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


import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException

class QuizSettingsSpecs extends DefaultIntSpec {

    def "save settings"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: 'name1', value: 'val1'],
                [setting: 'name2', value: 'val2'],
                [setting: 'name3', value: 'val3'],
        ])
        def settings = skillsService.getQuizSettings(quiz.quizId)

        then:
        settings.setting == ['name1', 'name2', 'name3']
        settings.value == ['val1', 'val2', 'val3']
    }

    def "existing settings are updated and new are inserted"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: 'name1', value: 'val1'],
                [setting: 'name2', value: 'val2'],
                [setting: 'name3', value: 'val3'],
        ])

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: 'name2', value: 'updated'],
                [setting: 'name4', value: 'val4'],
        ])
        def settings = skillsService.getQuizSettings(quiz.quizId)

        then:
        settings.setting == ['name1', 'name2', 'name3', 'name4']
        settings.value == ['val1', 'updated', 'val3', 'val4']
    }

    def "validation - setting has to be provided"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: 'name1', value: 'val1'],
                [setting: null, value: 'val2'],
                [setting: 'name3', value: 'val3'],
        ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("settings.setting was not provided")
        e.message.contains("quizId:${quiz.quizId}")
    }

    def "validation - setting's value has to be provided"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: 'name1', value: 'val1'],
                [setting: 'name2', value: null],
                [setting: 'name3', value: 'val3'],
        ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("settings.value was not provided")
        e.message.contains("quizId:${quiz.quizId}")
    }
}

