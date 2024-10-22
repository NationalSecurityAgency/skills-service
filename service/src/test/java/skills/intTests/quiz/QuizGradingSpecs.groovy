/**
 * Copyright 2024 SkillTree
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

class QuizGradingSpecs extends DefaultIntSpec {

    def "grading information is returned from complete quiz endpoint"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createTextInputSurveyQuestion(1, 1)
        skillsService.createQuizQuestionDefs([questions])

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, [isSelected:true, answerText: 'This is user provided answer'])

        // quiz without text input
        def quiz1 = QuizDefFactory.createQuiz(2, "Fancy Description")
        skillsService.createQuizDef(quiz1)
        skillsService.createQuizQuestionDefs([QuizDefFactory.createChoiceQuestion(2, 1)])

        def quizInfo1 = skillsService.getQuizInfo(quiz1.quizId)
        def quizAttempt1 =  skillsService.startQuizAttempt(quiz1.quizId).body
        skillsService.reportQuizAnswer(quiz1.quizId, quizAttempt1.id, quizInfo1.questions[0].answerOptions[0].id)

        // quiz with multiple questions to grade
        def quiz2 = QuizDefFactory.createQuiz(3, "Fancy Description")
        skillsService.createQuizDef(quiz2)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputSurveyQuestion(3, 1),
                QuizDefFactory.createChoiceQuestion(3, 1),
                QuizDefFactory.createTextInputSurveyQuestion(3, 1),
        ])

        def quizInfo2 = skillsService.getQuizInfo(quiz2.quizId)
        def quizAttempt2 =  skillsService.startQuizAttempt(quiz2.quizId).body
        skillsService.reportQuizAnswer(quiz2.quizId, quizAttempt2.id, quizInfo2.questions[0].answerOptions[0].id, [isSelected:true, answerText: 'This is user provided answer'])
        skillsService.reportQuizAnswer(quiz2.quizId, quizAttempt2.id, quizInfo2.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz2.quizId, quizAttempt2.id, quizInfo2.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'This is user provided answer'])

        when:
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        def gradedQuizAttempt1 = skillsService.completeQuizAttempt(quiz1.quizId, quizAttempt1.id).body
        def gradedQuizAttempt2 = skillsService.completeQuizAttempt(quiz2.quizId, quizAttempt2.id).body
        then:
        gradedQuizAttempt.needsGrading == true
        gradedQuizAttempt.numQuestionsNeedGrading == 1

        gradedQuizAttempt1.needsGrading == false
        gradedQuizAttempt1.numQuestionsNeedGrading == 0

        gradedQuizAttempt2.needsGrading == true
        gradedQuizAttempt2.numQuestionsNeedGrading == 2
    }
}
