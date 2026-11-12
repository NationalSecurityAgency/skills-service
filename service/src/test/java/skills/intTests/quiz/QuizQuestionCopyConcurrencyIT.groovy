/**
 * Copyright 2026 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.intTests.quiz

import org.mockito.invocation.InvocationOnMock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import skills.PublicProps
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizQuestionDef
import skills.storage.repos.QuizQuestionDefRepo
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import static org.mockito.Mockito.doAnswer

class QuizQuestionCopyConcurrencyIT extends DefaultIntSpec {

    @MockitoSpyBean
    QuizQuestionDefRepo quizQuestionDefRepo

    @Autowired
    JdbcTemplate jdbcTemplate

    @Autowired
    PublicProps publicProps

    def "copy preserves original question and answers during a concurrent source #mutation"() {
        setup:
        List<String> users = getRandomUsers(2)
        SkillsService copier = createService(users[0])
        SkillsService editor = createService(users[1])
        Map sourceQuiz = QuizDefFactory.createQuiz(1)
        Map destinationQuiz = QuizDefFactory.createQuiz(2)
        copier.createQuizDef(sourceQuiz)
        copier.createQuizDef(destinationQuiz)
        copier.addQuizUserRole(sourceQuiz.quizId, users[1], 'ROLE_QUIZ_ADMIN')

        Map question = QuizDefFactory.createChoiceQuestion(1, 1, 2, QuizQuestionType.SingleChoice)
        question.question = 'Original question'
        question.answerHint = 'Original answer hint'
        question.answers = [
                [answer: 'Original correct answer', isCorrect: true],
                [answer: 'Original incorrect answer', isCorrect: false],
        ]
        Map createdQuestion = copier.createQuizQuestionDef(question).body

        CountDownLatch copyPaused = new CountDownLatch(1)
        CountDownLatch resumeCopy = new CountDownLatch(1)
        AtomicInteger copyPid = new AtomicInteger()
        // Execute an equivalent scoped repository query, then pause before copy
        // can fetch answers. Spring Data query methods cannot use callRealMethod.
        doAnswer({ InvocationOnMock invocation ->
            List<QuizQuestionDef> questions = quizQuestionDefRepo.findAllByQuizIdIgnoreCase(sourceQuiz.quizId)
                    .findAll { QuizQuestionDef questionDef -> questionDef.id == createdQuestion.id }
            copyPid.set(jdbcTemplate.queryForObject('select pg_backend_pid()', Integer))
            copyPaused.countDown()
            assert resumeCopy.await(60, TimeUnit.SECONDS): 'Copy was not released by the test'
            return questions
        }).when(quizQuestionDefRepo).findAllByQuizIdAndIds(sourceQuiz.quizId, [createdQuestion.id])

        ExecutorService executor = Executors.newFixedThreadPool(2)
        Future<Object> copyRequest
        Future<Object> mutationRequest

        when:
        copyRequest = executor.submit({ ->
            copier.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [createdQuestion.id])
        } as Callable<Object>)
        assert copyPaused.await(10, TimeUnit.SECONDS): 'Copy did not reach the pause point'

        mutationRequest = executor.submit({ ->
            if (mutation == 'delete') {
                return editor.deleteQuizQuestionDef(sourceQuiz.quizId, createdQuestion.id)
            }
            return editor.updateQuizQuestionDef([
                    quizId: sourceQuiz.quizId,
                    id: createdQuestion.id,
                    question: 'Updated text-input question',
                    questionType: QuizQuestionType.TextInput.toString(),
                    answers: [],
            ])
        } as Callable<Object>)

        // Observe the actual database wait, not elapsed time. Without the source
        // lock, mutation completion is also terminal and fails the assertion below.
        // SQL is only used for synchronization; outcomes are checked through HTTP.
        new PollingConditions(timeout: 10).eventually {
            Boolean blockedByCopy = jdbcTemplate.queryForObject('''
                select exists (
                    select 1 from pg_locks where ? = any(pg_blocking_pids(pid))
                )
            ''', Boolean, copyPid.get())
            assert blockedByCopy || mutationRequest.isDone()
        }

        then: 'the source mutation waits for copying to finish'
        !mutationRequest.isDone()

        when:
        resumeCopy.countDown()
        copyRequest.get(30, TimeUnit.SECONDS)
        mutationRequest.get(30, TimeUnit.SECONDS)
        List<Map> destinationQuestions = copier.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then: 'the destination contains the complete original definition'
        destinationQuestions.size() == 1
        Map copiedQuestion = destinationQuestions.first()
        copiedQuestion.id != createdQuestion.id
        copiedQuestion.question == 'Original question'
        copiedQuestion.answerHint == 'Original answer hint'
        copiedQuestion.questionType == 'SingleChoice'
        copiedQuestion.displayOrder == 0
        copiedQuestion.answers*.answer == ['Original correct answer', 'Original incorrect answer']
        copiedQuestion.answers*.isCorrect == [true, false]
        copiedQuestion.answers*.displayOrder == [1, 2]

        and: 'the source mutation completed after copying'
        if (mutation == 'delete') {
            assert editor.getQuizQuestionDefs(sourceQuiz.quizId).questions.empty
        } else {
            Map updatedQuestion = editor.getQuizQuestionDef(sourceQuiz.quizId, createdQuestion.id)
            assert updatedQuestion.question == 'Updated text-input question'
            assert updatedQuestion.questionType == 'TextInput'
            assert updatedQuestion.answers.size() == 1
            assert updatedQuestion.answers.first().answer == null
            assert !updatedQuestion.answers.first().isCorrect
        }

        cleanup:
        // Release the request even when synchronization or outcome assertions fail.
        resumeCopy?.countDown()
        executor?.shutdown()
        if (executor) {
            assert executor.awaitTermination(70, TimeUnit.SECONDS): 'Concurrent requests did not finish during cleanup'
        }

        where:
        mutation << ['edit', 'delete']
    }

    def "concurrent copies from distinct sources append in source order to a populated destination"() {
        setup:
        List<String> users = getRandomUsers(2)
        SkillsService firstCopier = createService(users[0])
        SkillsService secondCopier = createService(users[1])
        Map firstSourceQuiz = QuizDefFactory.createQuiz(1)
        Map secondSourceQuiz = QuizDefFactory.createQuiz(2)
        Map destinationQuiz = QuizDefFactory.createQuiz(3)
        firstCopier.createQuizDef(firstSourceQuiz)
        secondCopier.createQuizDef(secondSourceQuiz)
        firstCopier.createQuizDef(destinationQuiz)
        firstCopier.addQuizUserRole(destinationQuiz.quizId, users[1], 'ROLE_QUIZ_ADMIN')

        List<Map> firstSourceQuestions = (1..3).collect { int number ->
            Map question = QuizDefFactory.createChoiceQuestion(1, number, 2)
            question.question = "First source question ${number}".toString()
            return firstCopier.createQuizQuestionDef(question).body as Map
        }
        List<Map> secondSourceQuestions = (1..3).collect { int number ->
            Map question = QuizDefFactory.createChoiceQuestion(2, number, 2)
            question.question = "Second source question ${number}".toString()
            return secondCopier.createQuizQuestionDef(question).body as Map
        }
        List<Map> existingQuestions = (1..2).collect { int number ->
            Map question = QuizDefFactory.createChoiceQuestion(3, number, 2)
            question.question = "Existing destination question ${number}".toString()
            return firstCopier.createQuizQuestionDef(question).body as Map
        }

        CountDownLatch firstCopyPaused = new CountDownLatch(1)
        CountDownLatch resumeFirstCopy = new CountDownLatch(1)
        AtomicInteger firstCopyPid = new AtomicInteger()
        AtomicBoolean pauseFirstCopy = new AtomicBoolean(true)
        // Read the real persisted display orders with an equivalent scoped query.
        // Pause the first copy after reading its append position, before inserting.
        // Different source quizzes ensure only the destination lock serializes copies.
        doAnswer({ InvocationOnMock invocation ->
            List<QuizQuestionDef> questions = quizQuestionDefRepo.findAllByQuizIdIgnoreCase(destinationQuiz.quizId)
            Integer maxDisplayOrder = questions.collect { QuizQuestionDef question -> question.displayOrder }.max()
            if (pauseFirstCopy.compareAndSet(true, false)) {
                firstCopyPid.set(jdbcTemplate.queryForObject('select pg_backend_pid()', Integer))
                firstCopyPaused.countDown()
                assert resumeFirstCopy.await(60, TimeUnit.SECONDS): 'First copy was not released by the test'
            }
            return maxDisplayOrder
        }).when(quizQuestionDefRepo).getMaxQuestionDisplayOrderByQuizId(destinationQuiz.quizId)

        ExecutorService executor = Executors.newFixedThreadPool(2)
        Future<Map> firstCopyRequest
        Future<Map> secondCopyRequest

        when:
        firstCopyRequest = executor.submit({ ->
            firstCopier.copyQuestions(firstSourceQuiz.quizId, destinationQuiz.quizId,
                    [firstSourceQuestions[2].id, firstSourceQuestions[0].id]).body as Map
        } as Callable<Map>)
        assert firstCopyPaused.await(10, TimeUnit.SECONDS): 'First copy did not reach the pause point'

        secondCopyRequest = executor.submit({ ->
            secondCopier.copyQuestions(secondSourceQuiz.quizId, destinationQuiz.quizId,
                    [secondSourceQuestions[2].id, secondSourceQuestions[0].id]).body as Map
        } as Callable<Map>)

        new PollingConditions(timeout: 10).eventually {
            Boolean blockedByFirstCopy = jdbcTemplate.queryForObject('''
                select exists (
                    select 1 from pg_locks where ? = any(pg_blocking_pids(pid))
                )
            ''', Boolean, firstCopyPid.get())
            assert blockedByFirstCopy || secondCopyRequest.isDone()
        }

        then: 'the second copy waits for the destination lock'
        !secondCopyRequest.isDone()

        when:
        resumeFirstCopy.countDown()
        Map firstCopyResponse = firstCopyRequest.get(30, TimeUnit.SECONDS)
        Map secondCopyResponse = secondCopyRequest.get(30, TimeUnit.SECONDS)
        List<Map> destinationQuestions = firstCopier.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then: 'both batches append after existing questions in their source order'
        firstCopyResponse.success
        secondCopyResponse.success
        destinationQuestions*.question == [
                'Existing destination question 1', 'Existing destination question 2',
                'First source question 1', 'First source question 3',
                'Second source question 1', 'Second source question 3',
        ]
        destinationQuestions*.id.take(2) == existingQuestions*.id
        destinationQuestions*.displayOrder == [0, 1, 2, 3, 4, 5]
        destinationQuestions*.id.toSet().size() == 6
        destinationQuestions.drop(2).each { Map question ->
            assert question.questionType == 'SingleChoice'
            assert question.answers*.answer == ['Answer #1', 'Answer #2']
            assert question.answers*.isCorrect == [true, false]
        }

        and: 'both source quizzes remain unchanged'
        firstCopier.getQuizQuestionDefs(firstSourceQuiz.quizId).questions*.id == firstSourceQuestions*.id
        secondCopier.getQuizQuestionDefs(secondSourceQuiz.quizId).questions*.id == secondSourceQuestions*.id

        cleanup:
        resumeFirstCopy?.countDown()
        executor?.shutdown()
        if (executor) {
            assert executor.awaitTermination(70, TimeUnit.SECONDS): 'Concurrent requests did not finish during cleanup'
        }
    }

    def "normal creation cannot exceed the question limit when copy fills the last destination slot"() {
        setup:
        List<String> users = getRandomUsers(2)
        SkillsService copier = createService(users[0])
        SkillsService creator = createService(users[1])
        Map sourceQuiz = QuizDefFactory.createQuiz(1)
        Map destinationQuiz = QuizDefFactory.createQuiz(2)
        copier.createQuizDef(sourceQuiz)
        copier.createQuizDef(destinationQuiz)
        copier.addQuizUserRole(destinationQuiz.quizId, users[1], 'ROLE_QUIZ_ADMIN')

        int maxQuestions = publicProps.getInt(PublicProps.UiProp.maxQuestionsPerQuiz)
        copier.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, maxQuestions - 1, 2))
        List<Map> existingQuestions = copier.getQuizQuestionDefs(destinationQuiz.quizId).questions
        Map sourceQuestion = QuizDefFactory.createChoiceQuestion(1, 1, 2)
        sourceQuestion.question = 'Copied question fills the last slot'
        Map createdSourceQuestion = copier.createQuizQuestionDef(sourceQuestion).body
        Map incomingQuestion = QuizDefFactory.createChoiceQuestion(2, maxQuestions, 2)
        incomingQuestion.question = 'Concurrent question must not be created'

        CountDownLatch copyPaused = new CountDownLatch(1)
        CountDownLatch resumeCopy = new CountDownLatch(1)
        AtomicInteger copyPid = new AtomicInteger()
        // This query occurs after copy checks destination capacity while holding
        // its lock, but before inserting the question that will fill the last slot.
        doAnswer({ InvocationOnMock invocation ->
            List<QuizQuestionDef> questions = quizQuestionDefRepo.findAllByQuizIdIgnoreCase(sourceQuiz.quizId)
                    .findAll { QuizQuestionDef questionDef -> questionDef.id == createdSourceQuestion.id }
            copyPid.set(jdbcTemplate.queryForObject('select pg_backend_pid()', Integer))
            copyPaused.countDown()
            assert resumeCopy.await(60, TimeUnit.SECONDS): 'Copy was not released by the test'
            return questions
        }).when(quizQuestionDefRepo).findAllByQuizIdAndIds(sourceQuiz.quizId, [createdSourceQuestion.id])

        ExecutorService executor = Executors.newFixedThreadPool(2)
        Future<Object> copyRequest
        Future<Map> creationRequest

        when:
        copyRequest = executor.submit({ ->
            copier.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [createdSourceQuestion.id])
        } as Callable<Object>)
        assert copyPaused.await(10, TimeUnit.SECONDS): 'Copy did not reach the pause point'

        creationRequest = executor.submit({ ->
            try {
                return [response: creator.createQuizQuestionDef(incomingQuestion).body]
            } catch (SkillsClientException creationError) {
                return [error: creationError]
            }
        } as Callable<Map>)

        // Before the fix creation validates against the old count, then waits.
        // With the fix it waits before reading the count. Observe the lock wait
        // in either case so releasing copy deterministically distinguishes them.
        new PollingConditions(timeout: 10).eventually {
            Boolean blockedByCopy = jdbcTemplate.queryForObject('''
                select exists (
                    select 1 from pg_locks where ? = any(pg_blocking_pids(pid))
                )
            ''', Boolean, copyPid.get())
            assert blockedByCopy || creationRequest.isDone()
        }

        then: 'normal creation waits for the destination lock'
        !creationRequest.isDone()

        when:
        resumeCopy.countDown()
        copyRequest.get(30, TimeUnit.SECONDS)
        Map creationResult = creationRequest.get(30, TimeUnit.SECONDS)
        List<Map> destinationQuestions = creator.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then: 'copy filled the last slot without allowing an extra question'
        destinationQuestions.size() == maxQuestions
        destinationQuestions*.id.take(maxQuestions - 1) == existingQuestions*.id
        destinationQuestions.last().question == 'Copied question fills the last slot'
        destinationQuestions.last().questionType == 'SingleChoice'
        destinationQuestions.last().answers*.answer == ['Answer #1', 'Answer #2']
        destinationQuestions.last().answers*.isCorrect == [true, false]
        !destinationQuestions.any { Map question -> question.question == incomingQuestion.question }

        and: 'normal creation reports the standard capacity validation error'
        creationResult.error instanceof SkillsClientException
        SkillsClientException error = creationResult.error
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains("[Number of Questions] must be <= [${maxQuestions}]")

        cleanup:
        resumeCopy?.countDown()
        executor?.shutdown()
        if (executor) {
            assert executor.awaitTermination(70, TimeUnit.SECONDS): 'Concurrent requests did not finish during cleanup'
        }
    }
}
