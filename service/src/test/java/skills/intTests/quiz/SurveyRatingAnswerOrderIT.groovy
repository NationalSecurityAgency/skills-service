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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizAnswerDef
import skills.storage.model.UserQuizQuestionAttempt
import skills.storage.repos.QuizAnswerDefRepo
import skills.storage.repos.UserQuizQuestionAttemptRepo

import static org.mockito.ArgumentMatchers.anyList
import static org.mockito.Mockito.doAnswer

class SurveyRatingAnswerOrderIT extends DefaultIntSpec {

    @MockitoSpyBean
    QuizAnswerDefRepo quizAnswerDefRepo

    @Autowired
    UserQuizQuestionAttemptRepo userQuizQuestionAttemptRepo

    def "survey rating options follow the star scale despite unordered database results - resume=#resume, randomizeAnswers=#randomizeAnswers"() {
        String quizId = createSurveyWithUnorderedRatingAnswers(randomizeAnswers)
        SkillsService learner = createService(getRandomUsers(1)[0])
        Map initialAttempt = resume ? learner.startQuizAttempt(quizId).body : null

        when:
        Map attempt = learner.startQuizAttempt(quizId).body

        then:
        attempt.questions.size() == 1
        attempt.questions[0].questionType == 'Rating'
        attempt.questions[0].answerOptions.answerOption == ['1', '2', '3', '4', '5']
        if (resume) {
            assert attempt.inProgressAlready
            assert attempt.id == initialAttempt.id
        }

        where:
        resume | randomizeAnswers
        false  | false
        true   | false
        false  | true
        true   | true
    }

    def "selecting the fifth star saves five stars despite unordered database results - resume=#resume, randomizeAnswers=#randomizeAnswers"() {
        String quizId = createSurveyWithUnorderedRatingAnswers(randomizeAnswers)
        SkillsService learner = createService(getRandomUsers(1)[0])
        if (resume) {
            learner.startQuizAttempt(quizId)
        }
        Map attempt = learner.startQuizAttempt(quizId).body

        when:
        // The survey UI maps a star selection to the answer at index (rating - 1).
        Integer fifthStarAnswerId = attempt.questions[0].answerOptions[4].id
        learner.reportQuizAnswer(quizId, attempt.id, fifthStarAnswerId)
        Map completed = learner.completeQuizAttempt(quizId, attempt.id).body
        Map savedAttempt = learner.getCurrentUserSingleQuizAttempt(attempt.id)

        then:
        completed.passed
        savedAttempt.status == 'PASSED'
        savedAttempt.questions[0].answers.findAll { Map answer -> answer.isSelected }.answer == ['5']

        where:
        resume | randomizeAnswers
        false  | false
        true   | false
        false  | true
        true   | true
    }

    def "non-randomized #questionType answers follow configured order on start and resume - resume=#resume"() {
        Map fixture = createQuizWithUnorderedAnswers(questionType)
        SkillsService learner = createService(getRandomUsers(1)[0])
        Map initialAttempt = resume ? learner.startQuizAttempt(fixture.quizId).body : null

        when:
        Map attempt = learner.startQuizAttempt(fixture.quizId).body

        then:
        attempt.questions[0].answerOptions.id == fixture.answers.id
        attempt.questions[0].answerOptions.answerOption == expectedOptions
        if (questionType == QuizQuestionType.Matching) {
            assert attempt.questions[0].matchingTerms.toSet() == ['value1', 'value2', 'value3'].toSet()
        }
        if (resume) {
            assert attempt.id == initialAttempt.id
            assert attempt.inProgressAlready
        }

        where:
        questionType                    | resume | expectedOptions
        QuizQuestionType.SingleChoice   | false  | ['Answer #4', 'Answer #3', 'Answer #2', 'Answer #1']
        QuizQuestionType.SingleChoice   | true   | ['Answer #4', 'Answer #3', 'Answer #2', 'Answer #1']
        QuizQuestionType.MultipleChoice | false  | ['Answer #4', 'Answer #3', 'Answer #2', 'Answer #1']
        QuizQuestionType.MultipleChoice | true   | ['Answer #4', 'Answer #3', 'Answer #2', 'Answer #1']
        QuizQuestionType.Matching       | false  | ['term3', 'term2', 'term1']
        QuizQuestionType.Matching       | true   | ['term3', 'term2', 'term1']
    }

    def "randomized #questionType answers keep their IDs associated with displayed options"() {
        Map fixture = createQuizWithUnorderedAnswers(questionType, true)
        SkillsService learner = createService(getRandomUsers(1)[0])
        Map expectedOptionsById = fixture.answers.collectEntries { Map answer ->
            [(answer.id): questionType == QuizQuestionType.Matching ? answer.multiPartAnswer.term : answer.answer]
        }

        when:
        Map attempt = learner.startQuizAttempt(fixture.quizId).body
        Map resumedAttempt = learner.startQuizAttempt(fixture.quizId).body

        then:
        [attempt, resumedAttempt].every { Map result ->
            List<Map> options = result.questions[0].answerOptions
            options.size() == fixture.answers.size() &&
                    options.collectEntries { Map answer -> [(answer.id): answer.answerOption] } == expectedOptionsById
        }
        if (questionType == QuizQuestionType.Matching) {
            assert attempt.questions[0].matchingTerms.toSet() == ['value1', 'value2', 'value3'].toSet()
            assert resumedAttempt.questions[0].matchingTerms.toSet() == ['value1', 'value2', 'value3'].toSet()
        }

        where:
        questionType << [QuizQuestionType.SingleChoice, QuizQuestionType.MultipleChoice, QuizQuestionType.Matching]
    }

    def "learner and admin history use configured answer order for #questionType"() {
        Map fixture = createQuizWithUnorderedAnswers(questionType)
        SkillsService learner = createService(getRandomUsers(1)[0])
        Map attempt = learner.startQuizAttempt(fixture.quizId).body
        reportAnswers(learner, fixture, attempt.id, questionType)

        when:
        learner.completeQuizAttempt(fixture.quizId, attempt.id)
        if (questionType == QuizQuestionType.TextInput) {
            // Learner history hides answers while a text-input submission awaits grading.
            skillsService.gradeAnswer(learner.userName, fixture.quizId, attempt.id, fixture.answers[0].id,
                    true, 'Correct answer', false, false)
        }
        Map learnerHistory = learner.getCurrentUserSingleQuizAttempt(attempt.id)
        Map adminHistory = skillsService.getQuizAttemptResult(fixture.quizId, attempt.id)

        then:
        [learnerHistory, adminHistory].every { Map history ->
            history.questions[0].answers.id == fixture.answers.id
        }
        if (questionType == QuizQuestionType.Matching) {
            assert learnerHistory.questions[0].answers.answer.term == ['term3', 'term2', 'term1']
            assert learnerHistory.questions[0].answers.answer.selectedMatch == ['value3', 'value2', 'value1']
        } else if (questionType == QuizQuestionType.FillInTheBlank) {
            assert learnerHistory.questions[0].answers.answer.answerText == ['Answer #1', 'Answer #2', 'Answer #3']
        } else if (questionType == QuizQuestionType.TextInput) {
            assert learnerHistory.questions[0].answers.answer == ['Learner answer']
        } else if (questionType == QuizQuestionType.Rating) {
            assert learnerHistory.questions[0].answers.answer == ['1', '2', '3', '4', '5']
            assert learnerHistory.questions[0].answers.findAll { Map answer -> answer.isSelected }.answer == ['5']
        } else {
            assert learnerHistory.questions[0].answers.answer == ['Answer #4', 'Answer #3', 'Answer #2', 'Answer #1']
            assert learnerHistory.questions[0].answers.findAll { Map answer -> answer.isSelected }.answer ==
                    (questionType == QuizQuestionType.SingleChoice ? ['Answer #1'] : ['Answer #3', 'Answer #1'])
        }

        where:
        questionType << QuizQuestionType.values().toList()
    }

    def "history question order survives definition changes - randomizeQuestions=#randomizeQuestions, legacy=#legacy"() {
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(1, 3, 2))
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.RandomizeQuestions.setting, value: randomizeQuestions],
        ])
        SkillsService learner = createService(getRandomUsers(1)[0])
        Map attempt = learner.startQuizAttempt(quiz.quizId).body
        attempt.questions.each { Map question ->
            learner.reportQuizAnswer(quiz.quizId, attempt.id, question.answerOptions.find { Map answer -> answer.answerOption == 'Answer #1' }.id)
        }
        learner.completeQuizAttempt(quiz.quizId, attempt.id)
        if (legacy) {
            List<UserQuizQuestionAttempt> questionAttempts = userQuizQuestionAttemptRepo.findAllByUserQuizAttemptRefId(attempt.id)
            questionAttempts.each { UserQuizQuestionAttempt questionAttempt -> questionAttempt.displayOrder = null }
            userQuizQuestionAttemptRepo.saveAllAndFlush(questionAttempts)
        }

        when:
        skillsService.changeQuizQuestionDisplayOrder(quiz.quizId, attempt.questions[0].id, 2)
        Map definitions = skillsService.getQuizQuestionDefs(quiz.quizId)
        Map learnerHistory = learner.getCurrentUserSingleQuizAttempt(attempt.id)
        Map adminHistory = skillsService.getQuizAttemptResult(quiz.quizId, attempt.id)

        then:
        definitions.questions.id != attempt.questions.id
        learnerHistory.questions.id == (legacy ? definitions.questions.id : attempt.questions.id)
        adminHistory.questions.id == learnerHistory.questions.id
        learnerHistory.questions.questionNum == [1, 2, 3]
        adminHistory.questions.questionNum == [1, 2, 3]

        where:
        randomizeQuestions | legacy
        false              | false
        true               | false
        false              | true
    }

    private Map createQuizWithUnorderedAnswers(QuizQuestionType questionType, boolean randomizeAnswers = false) {
        Map quiz = questionType == QuizQuestionType.Rating ? QuizDefFactory.createQuizSurvey(1) : QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        Map question
        if (questionType == QuizQuestionType.Matching) {
            question = QuizDefFactory.createMatchingQuestion(1, 1, 3)
        } else if (questionType == QuizQuestionType.FillInTheBlank) {
            question = QuizDefFactory.createFillInTheBlankQuestion(1, 1, 3)
        } else if (questionType == QuizQuestionType.TextInput) {
            question = QuizDefFactory.createTextInputQuestion(1, 1)
        } else if (questionType == QuizQuestionType.Rating) {
            question = QuizDefFactory.createRatingSurveyQuestion(1, 1)
            question.questionScale = 5
        } else {
            question = QuizDefFactory.createChoiceQuestion(1, 1, 4, questionType)
        }
        skillsService.createQuizQuestionDef(question)
        Map definition = skillsService.getQuizQuestionDefs(quiz.quizId).questions[0]
        if (questionType in [QuizQuestionType.SingleChoice, QuizQuestionType.MultipleChoice, QuizQuestionType.Matching]) {
            // Reorder through the editor endpoint so configured order differs from ID order.
            skillsService.updateQuizQuestionDef(question + [id: definition.id, answers: definition.answers.reverse()])
            definition = skillsService.getQuizQuestionDefs(quiz.quizId).questions[0]
        }
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.RandomizeAnswers.setting, value: randomizeAnswers],
        ])
        List<QuizAnswerDef> unorderedAnswers = quizAnswerDefRepo.findAllByQuizIdIgnoreCase(quiz.quizId)
                .sort { QuizAnswerDef answer -> answer.displayOrder }.reverse()
        doAnswer({ InvocationOnMock invocation -> new ArrayList<QuizAnswerDef>(unorderedAnswers) })
                .when(quizAnswerDefRepo).findAllByQuizIdIgnoreCase(quiz.quizId)
        doAnswer({ InvocationOnMock invocation ->
            List<Integer> questionIds = invocation.getArgument(0)
            unorderedAnswers.findAll { QuizAnswerDef answer -> questionIds.contains(answer.questionRefId) }
        }).when(quizAnswerDefRepo).findAllByQuestionRefIdIn(anyList())
        return [quizId: quiz.quizId, answers: definition.answers]
    }

    private void reportAnswers(SkillsService learner, Map fixture, Integer attemptId, QuizQuestionType questionType) {
        fixture.answers.each { Map answer ->
            if (questionType == QuizQuestionType.Matching) {
                learner.reportQuizAnswer(fixture.quizId, attemptId, answer.id, [isSelected: true, answerText: answer.multiPartAnswer.value])
            } else if (questionType == QuizQuestionType.FillInTheBlank) {
                learner.reportQuizAnswer(fixture.quizId, attemptId, answer.id, [isSelected: true, answerText: answer.answer])
            } else if (questionType == QuizQuestionType.TextInput) {
                learner.reportQuizAnswer(fixture.quizId, attemptId, answer.id, [isSelected: true, answerText: 'Learner answer'])
            } else if (answer.isCorrect || (questionType == QuizQuestionType.Rating && answer.answer == '5')) {
                learner.reportQuizAnswer(fixture.quizId, attemptId, answer.id)
            }
        }
    }

    private String createSurveyWithUnorderedRatingAnswers(boolean randomizeAnswers) {
        Map survey = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(survey)
        Map question = QuizDefFactory.createRatingSurveyQuestion(1, 1)
        question.questionScale = 5
        skillsService.createQuizQuestionDef(question)
        skillsService.saveQuizSettings(survey.quizId, [
                [setting: QuizSettings.RandomizeAnswers.setting, value: randomizeAnswers],
        ])

        List<QuizAnswerDef> orderedAnswers = quizAnswerDefRepo.findAllByQuizIdIgnoreCase(survey.quizId).sort { QuizAnswerDef answer -> answer.displayOrder }
        assert orderedAnswers.answer == ['1', '2', '3', '4', '5']
        List<QuizAnswerDef> unorderedAnswers = [orderedAnswers[4]] + orderedAnswers.take(4)

        // Keep real persisted answers and all other repository operations; only control
        // the unspecified lookup order. Return a fresh list for every start/resume.
        doAnswer({ InvocationOnMock invocation -> new ArrayList<QuizAnswerDef>(unorderedAnswers) })
                .when(quizAnswerDefRepo).findAllByQuizIdIgnoreCase(survey.quizId)
        return survey.quizId
    }
}
