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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.storage.model.QuizAnswerDef
import skills.storage.repos.QuizAnswerDefRepo

import static org.mockito.Mockito.doAnswer

class SurveyRatingAnswerOrderIT extends DefaultIntSpec {

    @MockitoSpyBean
    QuizAnswerDefRepo quizAnswerDefRepo

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
