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
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSubject

class QuizSettingsSpecs extends DefaultIntSpec {

    def "save settings"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: 'name1', value: 'val1'],
                [setting: 'name2', value: 'val2'],
                [setting: 'name3', value: 'val3'],
        ])
        def settings = skillsService.getQuizSettings(quiz.quizId)

        then:
        settings.setting == ['name1', 'name2', 'name3', QuizSettings.QuizUserRole.setting]
        settings.value == ['val1', 'val2', 'val3', RoleName.ROLE_QUIZ_ADMIN.toString()]
    }

    def "existing settings are updated and new are inserted"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
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
        settings.setting == ['name1', 'name2', 'name3', 'name4', QuizSettings.QuizUserRole.setting]
        settings.value == ['val1', 'updated', 'val3', 'val4', RoleName.ROLE_QUIZ_ADMIN.toString()]
    }

    def "validation - setting has to be provided"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
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
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
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
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
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
        settings_t0.setting == [QuizSettings.MaxNumAttempts.setting, QuizSettings.QuizUserRole.setting]
        settings_t0.value == ['12', RoleName.ROLE_QUIZ_ADMIN.toString()]

        settings_t1.setting == [QuizSettings.MaxNumAttempts.setting, QuizSettings.QuizUserRole.setting]
        settings_t1.value == ['-1', RoleName.ROLE_QUIZ_ADMIN.toString()]
    }

    def "validation: valid MaxNumAttempts setting - must be > -1"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
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
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
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
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
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
        settings_t0.setting == [QuizSettings.MinNumQuestionsToPass.setting, QuizSettings.QuizUserRole.setting]
        settings_t0.value == ['2', RoleName.ROLE_QUIZ_ADMIN.toString()]

        settings_t1.setting == [QuizSettings.MinNumQuestionsToPass.setting, QuizSettings.QuizUserRole.setting]
        settings_t1.value == ['-1', RoleName.ROLE_QUIZ_ADMIN.toString()]
    }

    def "validation: valid MinNumQuestionsToPass setting - must be > -1"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
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
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
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
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
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
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
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
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
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
        settings_t0.setting == [QuizSettings.MinNumQuestionsToPass.setting, QuizSettings.QuizUserRole.setting]
        settings_t0.value == ['3', RoleName.ROLE_QUIZ_ADMIN.toString()]

        settings_t1.setting == [QuizSettings.MinNumQuestionsToPass.setting, QuizSettings.QuizUserRole.setting]
        settings_t1.value == ['2', RoleName.ROLE_QUIZ_ADMIN.toString()]

        settings_t2.setting == [QuizSettings.MinNumQuestionsToPass.setting, QuizSettings.QuizUserRole.setting]
        settings_t2.value == ['1', RoleName.ROLE_QUIZ_ADMIN.toString()]

        settings_t3.setting == [QuizSettings.QuizUserRole.setting]
        settings_t3.value == [RoleName.ROLE_QUIZ_ADMIN.toString()]
    }

    def "get user admin role"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        def settings = skillsService.getQuizSettings(quiz.quizId)
        then:
        settings.setting == [QuizSettings.QuizUserRole.setting]
        settings.value == [RoleName.ROLE_QUIZ_ADMIN.toString()]
    }

    def "get user read only role"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkills(skills)

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)
        skillsService.addProjectAdmin(proj.projectId, otherUser.userName)

        when:
        def settings = otherUser.getQuizSettings(quiz.quizId)
        then:
        settings.setting == [QuizSettings.QuizUserRole.setting]
        settings.value == [RoleName.ROLE_QUIZ_READ_ONLY.toString()]
    }

}

