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
import skills.quizLoading.QuizSettings

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

    def "valid MaxNumAttempts setting"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: '12'],
        ])
        def settings_t0 = skillsService.getQuizSettings(quiz.quizId)
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: '-1'],
        ])
        def settings_t1 = skillsService.getQuizSettings(quiz.quizId)

        then:
        settings_t0.setting == [QuizSettings.MaxNumAttempts.setting]
        settings_t0.value == ['12']

        settings_t1.setting == [QuizSettings.MaxNumAttempts.setting]
        settings_t1.value == ['-1']
    }

    def "validation: valid MaxNumAttempts setting - must be > -1"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: '-2'],
        ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided value [-2] for [${QuizSettings.MaxNumAttempts.setting}] setting must be >= -1")
        e.message.contains("quizId:${quiz.quizId}")
    }

    def "validation: valid MaxNumAttempts setting - must be a number"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: '1a'],
        ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided value [1a] for [${QuizSettings.MaxNumAttempts.setting}] setting must be numeric")
        e.message.contains("quizId:${quiz.quizId}")
    }

    def "valid MinNumQuestionsToPass setting"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '2'],
        ])
        def settings_t0 = skillsService.getQuizSettings(quiz.quizId)
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '-1'],
        ])
        def settings_t1 = skillsService.getQuizSettings(quiz.quizId)

        then:
        settings_t0.setting == [QuizSettings.MinNumQuestionsToPass.setting]
        settings_t0.value == ['2']

        settings_t1.setting == [QuizSettings.MinNumQuestionsToPass.setting]
        settings_t1.value == ['-1']
    }

    def "validation: valid MinNumQuestionsToPass setting - must be > -1"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '-2'],
        ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided value [-2] for [${QuizSettings.MinNumQuestionsToPass.setting}] setting must be >= -1")
        e.message.contains("quizId:${quiz.quizId}")
    }

    def "validation: valid MinNumQuestionsToPass setting - must be a number"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '1a'],
        ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided value [1a] for [${QuizSettings.MinNumQuestionsToPass.setting}] setting must be numeric")
        e.message.contains("quizId:${quiz.quizId}")
    }

    def "validation: valid MinNumQuestionsToPass setting - not allowed to set when there are 0 declared questions"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '1'],
        ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Cannot modify [${QuizSettings.MinNumQuestionsToPass.setting}] becuase there is 0 declared questions")
        e.message.contains("quizId:${quiz.quizId}")
    }

    def "validation: valid MinNumQuestionsToPass setting - must be less than the declared questions"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '3'],
        ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided [${QuizSettings.MinNumQuestionsToPass.setting}] setting [3] must be less than [2] declared questions")
        e.message.contains("quizId:${quiz.quizId}")
    }

    def "MinNumQuestionsToPass setting - must be less than the declared questions"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '3'],
        ])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided [${QuizSettings.MinNumQuestionsToPass.setting}] setting [3] must be less than [2] declared questions")
        e.message.contains("quizId:${quiz.quizId}")
    }

    def "MinNumQuestionsToPass setting - when a question is removed adjust the MinNumQuestionsToPass setting if needed"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '3'],
        ])
        def settings_t0 = skillsService.getQuizSettings(quiz.quizId)
        skillsService.deleteQuizQuestionDef(quiz.quizId,  quizInfo.questions[0].id)
        def settings_t1 = skillsService.getQuizSettings(quiz.quizId)
        skillsService.deleteQuizQuestionDef(quiz.quizId,  quizInfo.questions[1].id)
        def settings_t2 = skillsService.getQuizSettings(quiz.quizId)
        skillsService.deleteQuizQuestionDef(quiz.quizId,  quizInfo.questions[2].id)
        def settings_t3 = skillsService.getQuizSettings(quiz.quizId)
        then:
        settings_t0.setting == [QuizSettings.MinNumQuestionsToPass.setting]
        settings_t0.value == ['3']

        settings_t1.setting == [QuizSettings.MinNumQuestionsToPass.setting]
        settings_t1.value == ['2']

        settings_t2.setting == [QuizSettings.MinNumQuestionsToPass.setting]
        settings_t2.value == ['1']

        !settings_t3
    }
}

