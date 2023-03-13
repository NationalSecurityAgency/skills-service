/**
 * Copyright 2020 SkillTree
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


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory

class QuizMetricsSpecs extends DefaultIntSpec {

    def "load metrics"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(5, true)
        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index != 1)
        }

        when:
        def metrics = skillsService.getQuizMetrics(quiz.quizId)
        then:
        metrics
    }

    void runQuiz(String userId, def quiz, def quizInfo, boolean pass) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body
    }

}
