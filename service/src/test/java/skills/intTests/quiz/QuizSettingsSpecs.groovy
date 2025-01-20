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

import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicInteger

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSubject

class QuizSettingsSpecs extends DefaultIntSpec {

    def "concurrent request to settings produce consistent results"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createSkills(skills)

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)
        skillsService.addProjectAdmin(proj.projectId, otherUser.userName)

        def quiz2 = QuizDefFactory.createQuiz(2)
        otherUser.createQuizDef(quiz2)
        def questions2 = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        otherUser.createQuizQuestionDefs(questions2)

        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skillsService.createSkill(skills[1])

        int numTimes = 100
        AtomicInteger exceptioncount = new AtomicInteger()
        List<String> exceptions = Collections.synchronizedList([])
        List<Thread> threads = []
        when:
        assert otherUser.getQuizSettings(quiz1.quizId).find { it.setting == QuizSettings.QuizUserRole.setting }.value == RoleName.ROLE_QUIZ_READ_ONLY.toString()
        assert otherUser.getQuizSettings(quiz2.quizId).find { it.setting == QuizSettings.QuizUserRole.setting }.value == RoleName.ROLE_QUIZ_ADMIN.toString()
        threads << Thread.start {
            numTimes.times {
                try {
                    assert otherUser.getQuizSettings(quiz1.quizId).find { it.setting == QuizSettings.QuizUserRole.setting }.value == RoleName.ROLE_QUIZ_READ_ONLY.toString()
                } catch (Throwable t) {
                    exceptioncount.incrementAndGet()
                    t.printStackTrace()
                }
            }
        }
        threads << Thread.start {
            numTimes.times {
                try {
                    assert otherUser.getQuizSettings(quiz2.quizId).find { it.setting == QuizSettings.QuizUserRole.setting }.value == RoleName.ROLE_QUIZ_ADMIN.toString()
                } catch (Throwable t) {
                    exceptioncount.incrementAndGet()
                    t.printStackTrace()
                }
            }
        }
        threads.each {
            it.join(5000)
        }
        then:
        exceptioncount.get() == 0
    }

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
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '3'],
        ])
        def settings_t0 = skillsService.getQuizSettings(quiz.quizId)
        skillsService.deleteQuizQuestionDef(quiz.quizId,  quizAttempt.questions[0].id)
        def settings_t1 = skillsService.getQuizSettings(quiz.quizId)
        skillsService.deleteQuizQuestionDef(quiz.quizId,  quizAttempt.questions[1].id)
        def settings_t2 = skillsService.getQuizSettings(quiz.quizId)
        skillsService.deleteQuizQuestionDef(quiz.quizId,  quizAttempt.questions[2].id)
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

    def "QuizLength setting - when a question is removed adjust the QuizLength setting if needed"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body

        when:
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.QuizLength.setting, value: '3'],
        ])
        def settings_t0 = skillsService.getQuizSettings(quiz.quizId)
        skillsService.deleteQuizQuestionDef(quiz.quizId,  quizAttempt.questions[0].id)
        def settings_t1 = skillsService.getQuizSettings(quiz.quizId)
        skillsService.deleteQuizQuestionDef(quiz.quizId,  quizAttempt.questions[1].id)
        def settings_t2 = skillsService.getQuizSettings(quiz.quizId)
        skillsService.deleteQuizQuestionDef(quiz.quizId,  quizAttempt.questions[2].id)
        def settings_t3 = skillsService.getQuizSettings(quiz.quizId)
        then:
        settings_t0.setting == [QuizSettings.QuizLength.setting, QuizSettings.QuizUserRole.setting]
        settings_t0.value == ['3', RoleName.ROLE_QUIZ_ADMIN.toString()]

        settings_t1.setting == [QuizSettings.QuizLength.setting, QuizSettings.QuizUserRole.setting]
        settings_t1.value == ['2', RoleName.ROLE_QUIZ_ADMIN.toString()]

        settings_t2.setting == [QuizSettings.QuizLength.setting, QuizSettings.QuizUserRole.setting]
        settings_t2.value == ['1', RoleName.ROLE_QUIZ_ADMIN.toString()]

        settings_t3.setting == [QuizSettings.QuizUserRole.setting]
        settings_t3.value == [RoleName.ROLE_QUIZ_ADMIN.toString()]
    }

    def "Answers for a question can be randomized"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 100)
        skillsService.createQuizQuestionDefs(questions)

        def firstSortedQuizInfo = skillsService.startQuizAttempt(quiz.quizId).body
        def secondSortedQuizInfo = skillsService.startQuizAttempt(quiz.quizId).body
        def thirdSortedQuizInfo = skillsService.startQuizAttempt(quiz.quizId).body

        assert firstSortedQuizInfo.questions[0].answerOptions == secondSortedQuizInfo.questions[0].answerOptions
        assert secondSortedQuizInfo.questions[0].answerOptions == thirdSortedQuizInfo.questions[0].answerOptions
        assert thirdSortedQuizInfo.questions[0].answerOptions == firstSortedQuizInfo.questions[0].answerOptions

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.RandomizeAnswers.setting, value: 'true'],
        ])

        when:
        def firstQuizInfo = skillsService.startQuizAttempt(quiz.quizId).body
        def secondQuizInfo = skillsService.startQuizAttempt(quiz.quizId).body
        def thirdQuizInfo = skillsService.startQuizAttempt(quiz.quizId).body

        then:
        firstQuizInfo.questions[0].answerOptions != secondQuizInfo.questions[0].answerOptions
        secondQuizInfo.questions[0].answerOptions != thirdQuizInfo.questions[0].answerOptions
        thirdQuizInfo.questions[0].answerOptions != firstQuizInfo.questions[0].answerOptions
    }

    def "Question order can be randomized"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 100, 2)
        skillsService.createQuizQuestionDefs(questions)

        def firstSortedQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user1").body
        def secondSortedQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user2").body
        def thirdSortedQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user3").body

        assert firstSortedQuizInfo.questions == secondSortedQuizInfo.questions
        assert secondSortedQuizInfo.questions == thirdSortedQuizInfo.questions
        assert thirdSortedQuizInfo.questions == firstSortedQuizInfo.questions

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.RandomizeQuestions.setting, value: 'true'],
        ])

        when:
        def firstQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user4").body
        def secondQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user5").body
        def thirdQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user6").body

        then:
        firstQuizInfo.questions != secondQuizInfo.questions
        secondQuizInfo.questions != thirdQuizInfo.questions
        thirdQuizInfo.questions != firstQuizInfo.questions
    }

    def "Can select a subset of questions"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 100, 2)
        skillsService.createQuizQuestionDefs(questions)

        def fullQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user1").body

        assert fullQuizInfo.questions.size() == 100

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.QuizLength.setting, value: 30],
        ])

        when:
        def subsetQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user2").body

        then:
        subsetQuizInfo.questions.size() == 30
    }

    def "Can select a subset of randomized questions"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 100, 2)
        skillsService.createQuizQuestionDefs(questions)

        def fullQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user1").body

        assert fullQuizInfo.questions.size() == 100

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.QuizLength.setting, value: 10],
                [setting: QuizSettings.RandomizeQuestions.setting, value: 'true'],
        ])

        when:
        def firstSubsetQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user2").body
        def secondSubsetQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user3").body
        def thirdSubsetQuizInfo = skillsService.startQuizAttempt(quiz.quizId, "user4").body

        then:
        firstSubsetQuizInfo.questions.size() == 10
        secondSubsetQuizInfo.questions.size() == 10
        thirdSubsetQuizInfo.questions.size() == 10
        thirdSubsetQuizInfo.questions != secondSubsetQuizInfo.questions
        thirdSubsetQuizInfo.questions != firstSubsetQuizInfo.questions
        secondSubsetQuizInfo.questions != firstSubsetQuizInfo.questions
    }

    def "Can set a time limit on quizzes"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId, "user1")
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId, "user1").body

        assert quizInfo.quizTimeLimit == -1
        assert quizAttempt.deadline == null

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.QuizTimeLimit.setting, value: 300],
        ])

        when:
        def updatedQuizInfo = skillsService.getQuizInfo(quiz.quizId, "user1")
        def deadlineQuizAttempt = skillsService.startQuizAttempt(quiz.quizId, "user2").body

        then:
        updatedQuizInfo.quizTimeLimit == 300
        deadlineQuizAttempt.deadline != null
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

    def "run quiz - retaking a failed quiz gives only the failed questions if setting is enabled"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 4, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: 'quizRetakeIncorrectQuestions', value: 'true'],
        ])

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[3].answerOptions[1].id)
        assert quizAttempt.questions.size() == 4
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        def secondQuizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[1].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[2].answerOptions[1].id)
        assert secondQuizAttempt.questions.size() == 3
        skillsService.completeQuizAttempt(quiz.quizId, secondQuizAttempt.id).body

        def thirdQuizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, thirdQuizAttempt.id, thirdQuizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, thirdQuizAttempt.id, thirdQuizAttempt.questions[1].answerOptions[1].id)
        assert thirdQuizAttempt.questions.size() == 2
        skillsService.completeQuizAttempt(quiz.quizId, thirdQuizAttempt.id).body

        def fourthQuizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, fourthQuizAttempt.id, fourthQuizAttempt.questions[0].answerOptions[0].id)
        assert fourthQuizAttempt.questions.size() == 1
        skillsService.completeQuizAttempt(quiz.quizId, fourthQuizAttempt.id).body

        then:
        quizAttempt.questions.size() == 4
        secondQuizAttempt.questions.size() == 3
        thirdQuizAttempt.questions.size() == 2
        fourthQuizAttempt.questions.size() == 1
        secondQuizAttempt.questions[0].id == quizAttempt.questions[1].id
        thirdQuizAttempt.questions[0].id == quizAttempt.questions[2].id
        fourthQuizAttempt.questions[0].id == quizAttempt.questions[3].id

    }

    def "run quiz - full quiz is given when retake suggestion is disabled"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 4, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: 'quizRetakeIncorrectQuestions', value: 'true'],
        ])

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[3].answerOptions[1].id)
        assert quizAttempt.questions.size() == 4
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        def secondQuizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[1].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[2].answerOptions[1].id)
        assert secondQuizAttempt.questions.size() == 3
        skillsService.completeQuizAttempt(quiz.quizId, secondQuizAttempt.id).body

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: 'quizRetakeIncorrectQuestions', value: 'false'],
        ])

        def thirdQuizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, thirdQuizAttempt.id, thirdQuizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, thirdQuizAttempt.id, thirdQuizAttempt.questions[1].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, thirdQuizAttempt.id, thirdQuizAttempt.questions[2].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, thirdQuizAttempt.id, thirdQuizAttempt.questions[3].answerOptions[1].id)
        skillsService.completeQuizAttempt(quiz.quizId, thirdQuizAttempt.id).body

        then:
        quizAttempt.questions.size() == 4
        secondQuizAttempt.questions.size() == 3
        thirdQuizAttempt.questions.size() == 4

    }

    def "run quiz - retaking a failed quiz with a subset of questions"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 6, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
            [setting: QuizSettings.RetakeIncorrectQuestionsOnly.setting, value: 'true'],
            [setting: QuizSettings.QuizLength.setting, value: '4'],
        ])

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[3].answerOptions[1].id)
        assert quizAttempt.questions.size() == 4
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        def secondQuizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[1].answerOptions[0].id)
        skillsService.completeQuizAttempt(quiz.quizId, secondQuizAttempt.id).body

        then:
        quizAttempt.questions.size() == 4
        secondQuizAttempt.questions.size() == 2
        quizAttempt.questions.find{ question -> question.id == secondQuizAttempt.questions[0].id } != null
        quizAttempt.questions.find{ question -> question.id == secondQuizAttempt.questions[1].id } != null
    }

    def "multiple takes does not use the incorrect questions when quiz is passed"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 4, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.RetakeIncorrectQuestionsOnly.setting, value: 'true'],
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '1'],
                [setting: QuizSettings.MultipleTakes.setting, value: 'true'],
        ])

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[3].answerOptions[1].id)
        assert quizAttempt.questions.size() == 4
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        def secondQuizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[2].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[3].answerOptions[1].id)
        skillsService.completeQuizAttempt(quiz.quizId, secondQuizAttempt.id).body

        then:
        quizAttempt.questions.size() == 4
        secondQuizAttempt.questions.size() == 4

    }

    def "multiple takes does use the incorrect questions when quiz is failed"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 4, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.RetakeIncorrectQuestionsOnly.setting, value: 'true'],
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '3'],
                [setting: QuizSettings.MultipleTakes.setting, value: 'true'],
        ])

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[3].answerOptions[1].id)
        assert quizAttempt.questions.size() == 4
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        def secondQuizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, secondQuizAttempt.id, secondQuizAttempt.questions[1].answerOptions[0].id)
        skillsService.completeQuizAttempt(quiz.quizId, secondQuizAttempt.id).body

        then:
        quizAttempt.questions.size() == 4
        secondQuizAttempt.questions.size() == 2
        secondQuizAttempt.questions[0].id == quizAttempt.questions[2].id
        secondQuizAttempt.questions[1].id == quizAttempt.questions[3].id
    }
}