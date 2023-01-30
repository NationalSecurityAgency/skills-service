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

import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizDefParent

@Slf4j
class QuizDefSurveyManagementSpecs extends DefaultIntSpec {

    def "get quiz definition summary"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3, QuizDefParent.QuizType.Survey),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4, QuizDefParent.QuizType.Survey),
                QuizDefFactory.createTextInputSurveyQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        when:
        def res = skillsService.getQuizQuestionDefs(quiz.quizId)
        then:
        res.quizType == quiz.type
        res.questions.questionType == [QuizQuestionType.MultipleChoice.toString(), QuizQuestionType.SingleChoice.toString(), QuizQuestionType.TextInput.toString()]
        res.questions.question == questions.question

        res.questions[0].answers.isCorrect == [false, false, false]
        res.questions[0].answers.answer == questions[0].answers.answer

        res.questions[1].answers.isCorrect == [false, false, false, false]
        res.questions[1].answers.answer == questions[1].answers.answer

        res.questions[2].answers.isCorrect == [false]
        res.questions[2].answers.answer == [null]
    }

}

