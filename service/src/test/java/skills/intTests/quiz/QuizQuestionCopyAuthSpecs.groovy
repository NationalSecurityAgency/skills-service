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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import skills.storage.repos.QuizQuestionDefRepo

class QuizQuestionCopyAuthSpecs extends DefaultIntSpec {

    @Autowired
    QuizQuestionDefRepo quizQuestionDefRepo

    def "copying questions requires destination quiz admin access"() {
        SkillsService sourceAdmin = createService(getRandomUsers(1)[0])
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        sourceAdmin.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestion = sourceAdmin.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body

        when: 'the source quiz admin tries to copy into a quiz they do not administer'
        sourceAdmin.wsHelper.adminPost("/quiz-definitions/${sourceQuiz.quizId}/questions/copy/${destinationQuiz.quizId}".toString(), [sourceQuestion.id])

        then:
        SkillsClientException denied = thrown()
        denied.httpStatus == HttpStatus.FORBIDDEN
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty

        when: 'the source admin is also granted access to the destination'
        skillsService.addQuizUserRole(destinationQuiz.quizId, sourceAdmin.userName, 'ROLE_QUIZ_ADMIN')
        def copied = sourceAdmin.wsHelper.adminPost("/quiz-definitions/${sourceQuiz.quizId}/questions/copy/${destinationQuiz.quizId}".toString(), [sourceQuestion.id])

        then:
        copied.body.success
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.size() == 1
    }

    def "community-protected questions cannot be copied into an unrestricted quiz"() {
        SkillsService communityAdmin = createService(getRandomUsers(1)[0])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(communityAdmin.userName, 'dragons', ['DivineDragon'])

        def sourceQuiz = QuizDefFactory.createQuiz(1)
        sourceQuiz.enableProtectedUserCommunity = true
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        communityAdmin.createQuizDef(sourceQuiz)
        communityAdmin.createQuizDef(destinationQuiz)
        def sourceQuestion = communityAdmin.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body

        when:
        communityAdmin.wsHelper.adminPost(
                "/quiz-definitions/${sourceQuiz.quizId}/questions/copy/${destinationQuiz.quizId}".toString(),
                [sourceQuestion.id])

        then:
        SkillsClientException denied = thrown()
        denied.httpStatus == HttpStatus.BAD_REQUEST
        denied.message.contains('errorCode:AccessDenied')
        denied.message.contains('Can not copy questions')
        communityAdmin.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty
        communityAdmin.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == [sourceQuestion.id]
    }

    def "community-protected questions can be copied into another protected quiz"() {
        SkillsService communityAdmin = createService(getRandomUsers(1)[0])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(communityAdmin.userName, 'dragons', ['DivineDragon'])

        def sourceQuiz = QuizDefFactory.createQuiz(1)
        sourceQuiz.enableProtectedUserCommunity = true
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        destinationQuiz.enableProtectedUserCommunity = true
        communityAdmin.createQuizDef(sourceQuiz)
        communityAdmin.createQuizDef(destinationQuiz)
        def sourceQuestion = communityAdmin.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body

        when:
        def response = communityAdmin.wsHelper.adminPost(
                "/quiz-definitions/${sourceQuiz.quizId}/questions/copy/${destinationQuiz.quizId}".toString(),
                [sourceQuestion.id])
        def copiedQuestions = communityAdmin.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then:
        response.body.success
        copiedQuestions.size() == 1
        copiedQuestions[0].id != sourceQuestion.id
        copiedQuestions[0].question == sourceQuestion.question
        copiedQuestions[0].answers*.answer == sourceQuestion.answers*.answer
        communityAdmin.getQuizDefSummary(destinationQuiz.quizId).userCommunity == 'Divine Dragon'
    }

    def "copy selected question resolves the source quiz id case-insensitively"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body

        when:
        def copied = skillsService.wsHelper.adminPost(
                "/quiz-definitions/${sourceQuiz.quizId.toUpperCase()}/questions/copy/${destinationQuiz.quizId}".toString(),
                [sourceQuestion.id])

        then:
        copied.body.success
        def destinationQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions
        destinationQuestions.size() == 1
        destinationQuestions[0].question == sourceQuestion.question
    }

    def "empty selected question ids cannot copy the entire quiz"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2))

        when:
        skillsService.wsHelper.adminPost(
                "/quiz-definitions/${sourceQuiz.quizId}/questions/copy/${destinationQuiz.quizId}".toString(), [])

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains('At least one question id must be provided')
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions.size() == 1
    }

    def "selected question ids must all belong to the source quiz"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        def otherQuiz = QuizDefFactory.createQuiz(3)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        skillsService.createQuizDef(otherQuiz)
        def sourceQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body
        def otherQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(3, 1, 2)).body

        when: 'a request includes an id from another quiz'
        skillsService.wsHelper.adminPost(
                "/quiz-definitions/${sourceQuiz.quizId}/questions/copy/${destinationQuiz.quizId}".toString(),
                [sourceQuestion.id, otherQuestion.id])

        then:
        SkillsClientException foreignId = thrown()
        foreignId.httpStatus == HttpStatus.BAD_REQUEST
        foreignId.message.contains("Question ids [${otherQuestion.id}] do not belong to quiz [${sourceQuiz.quizId}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty

        when: 'a request includes a nonexistent id'
        Integer nonexistentId = Math.max(sourceQuestion.id, otherQuestion.id) + 1_000_000
        skillsService.wsHelper.adminPost(
                "/quiz-definitions/${sourceQuiz.quizId}/questions/copy/${destinationQuiz.quizId}".toString(),
                [sourceQuestion.id, nonexistentId])

        then:
        SkillsClientException missingId = thrown()
        missingId.httpStatus == HttpStatus.BAD_REQUEST
        missingId.message.contains("Question ids [${nonexistentId}] do not belong to quiz [${sourceQuiz.quizId}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty
    }

    def "selected questions append in source order after existing destination questions"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestions = (1..4).collect { number ->
            skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, number, 2)).body
        }
        def existingQuestions = (1..2).collect { number ->
            skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(2, number, 2)).body
        }

        when:
        def copied = skillsService.wsHelper.adminPost(
                "/quiz-definitions/${sourceQuiz.quizId}/questions/copy/${destinationQuiz.quizId}".toString(),
                [sourceQuestions[3].id, sourceQuestions[1].id])
        def destinationQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then:
        copied.body.success
        destinationQuestions*.question == [existingQuestions[0].question, existingQuestions[1].question,
                                           sourceQuestions[1].question, sourceQuestions[3].question]
        destinationQuestions*.displayOrder == [0, 1, 2, 3]
    }

    def "selected questions copied to an empty quiz preserve their definitions and answer order"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 3, QuizQuestionType.SingleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.MultipleChoice),
                QuizDefFactory.createChoiceQuestion(1, 3, 2, QuizQuestionType.SingleChoice),
        ].collect { skillsService.createQuizQuestionDef(it).body }

        when:
        def copied = skillsService.wsHelper.adminPost(
                "/quiz-definitions/${sourceQuiz.quizId}/questions/copy/${destinationQuiz.quizId}".toString(),
                [sourceQuestions[2].id, sourceQuestions[0].id])
        def destinationQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then:
        copied.body.success
        destinationQuestions.size() == 2
        destinationQuestions*.displayOrder == [0, 1]
        [sourceQuestions[0], sourceQuestions[2]].eachWithIndex { source, index ->
            def copiedQuestion = destinationQuestions[index]
            assert copiedQuestion.id != source.id
            assert copiedQuestion.question == source.question
            assert copiedQuestion.answerHint == source.answerHint
            assert copiedQuestion.questionType == source.questionType
            assert copiedQuestion.answers*.answer == source.answers*.answer
            assert copiedQuestion.answers*.isCorrect == source.answers*.isCorrect
            assert copiedQuestion.answers*.displayOrder == source.answers*.displayOrder
            assert !copiedQuestion.answers*.id.intersect(source.answers*.id)
        }
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == sourceQuestions*.id
    }

    def "validation failure in a selected question prevents all questions from being copied"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def validQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body
        def invalidQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 2)).body
        // Existing questions can become invalid after the paragraph validation rules change.
        runInTransaction { quizQuestionDefRepo.updateQuestion(invalidQuestion.id, 'jabberwocky') }

        when:
        skillsService.wsHelper.adminPost(
                "/quiz-definitions/${sourceQuiz.quizId}/questions/copy/${destinationQuiz.quizId}".toString(),
                [validQuestion.id, invalidQuestion.id])

        then:
        SkillsClientException error = thrown()
        error.message.contains("Validation failed for questionId=[${invalidQuestion.id}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty
    }
}
