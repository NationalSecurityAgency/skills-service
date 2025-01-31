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
package skills.intTests.community.quiz


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.transaction.support.TransactionTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.quizLoading.QuizUserPreferences
import skills.services.quiz.QuizQuestionType
import skills.storage.model.SkillDef
import skills.storage.model.UserQuizAttempt
import skills.storage.model.auth.RoleName
import skills.storage.repos.UserPerformedSkillRepo
import skills.utils.GroovyToJavaByteUtils

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkill
import static skills.intTests.utils.SkillsFactory.createSubject

@Slf4j
class UserCommunityQuizAuthSpecs extends DefaultIntSpec {
    SkillsService rootSkillsService

    @Autowired
    UserPerformedSkillRepo performedSkillRepository

    def setup() {
        rootSkillsService = createRootSkillService()
        skillsService.getCurrentUser() // initialize skillsService user_attrs
    }

    def "non-UC user cannot access quiz trainee/api endpoints with UC protection enabled"() {
        when:
        rootSkillsService.saveUserTag(skillsService.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        skillsService.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService nonUserCommunityUser = createService(nonUserCommunityUserId)

        then:
        validateForbidden { nonUserCommunityUser.getQuizInfo(q1.quizId) }
        validateForbidden { nonUserCommunityUser.startQuizAttempt(q1.quizId) }
        validateForbidden { nonUserCommunityUser.reportQuizAnswer(q1.quizId, 1, 1) }
        validateForbidden { nonUserCommunityUser.completeQuizAttempt(q1.quizId, 1 ) }
        validateForbidden { nonUserCommunityUser.failQuizAttempt(q1.quizId, 1 ) }
    }

    def "non-UC user cannot access quiz trainee/api endpoints with UC protection enabled - attempt was started prior quiz was elevated"() {
        when:
        rootSkillsService.saveUserTag(skillsService.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService nonUserCommunityUser = createService(nonUserCommunityUserId)

        def quizAttempt = nonUserCommunityUser.startQuizAttempt(q1.quizId).body
        q1.enableProtectedUserCommunity = true
        skillsService.createQuizDef(q1, q1.quizId)
        then:
        validateForbidden { nonUserCommunityUser.reportQuizAnswer(q1.quizId, quizAttempt.id, 1) }
        validateForbidden { nonUserCommunityUser.completeQuizAttempt(q1.quizId, quizAttempt.id ) }
        validateForbidden { nonUserCommunityUser.failQuizAttempt(q1.quizId, quizAttempt.id ) }
    }

    def "user was UC but then became non-UC - cannot access quiz trainee/api endpoints with UC protection enabled"() {
        when:
        rootSkillsService.saveUserTag(skillsService.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        skillsService.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService nonUserCommunityUser = createService(nonUserCommunityUserId)
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(nonUserCommunityUser.userName, 'dragons', ['DivineDragon'])

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
        nonUserCommunityUser.getQuizInfo(q1.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(q1.quizId).body
        transactionTemplate.execute({
            userTagRepo.deleteByUserId(nonUserCommunityUser.userName)
        })
        then:
        validateForbidden { nonUserCommunityUser.getQuizInfo(q1.quizId) }
        validateForbidden { nonUserCommunityUser.reportQuizAnswer(q1.quizId, 1, 1) }
        validateForbidden { nonUserCommunityUser.completeQuizAttempt(q1.quizId, 1 ) }
        validateForbidden { nonUserCommunityUser.failQuizAttempt(q1.quizId, 1 ) }
    }

    def "for non-UC user quiz attempt history only contains non-UC projects"() {
        // this can happen if a non-uc user takes a quiz and the quiz is elevated to UC at a later time
        List<String> users = getRandomUsers(3)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService adminUser = createService(users[2])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(adminUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        adminUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        adminUser.createQuizQuestionDefs(questions)

        def runQuiz = { SkillsService userService, boolean pass ->
            def quizAttempt = userService.startQuizAttempt(q1.quizId).body
            int answerIndex = pass ? 0 : 1
            userService.reportQuizAnswer(q1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[answerIndex].id)
            def gradedQuizAttempt = userService.completeQuizAttempt(q1.quizId, quizAttempt.id).body
            return gradedQuizAttempt
        }

        runQuiz(allDragonsUser, false)
        runQuiz(allDragonsUser, false)
        runQuiz(allDragonsUser, true)

        runQuiz(pristineDragonsUser, false)
        runQuiz(pristineDragonsUser, false)
        runQuiz(pristineDragonsUser, true)

        when:
        def allDragonsAttempts_t1 = allDragonsUser.getCurrentUserQuizAttempts()
        def pristineDragonAttempts_t1 = pristineDragonsUser.getCurrentUserQuizAttempts()

        q1.enableProtectedUserCommunity = true
        adminUser.createQuizDef(q1, q1.quizId)
        def allDragonsAttempts_t2 = allDragonsUser.getCurrentUserQuizAttempts()
        def pristineDragonAttempts_t2 = pristineDragonsUser.getCurrentUserQuizAttempts()

        then:
        allDragonsAttempts_t1.data.size() == 3
        allDragonsAttempts_t1.data.quizId == [q1.quizId, q1.quizId, q1.quizId]
        allDragonsAttempts_t1.data.status == [
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        ]

        pristineDragonAttempts_t1.data.size() == 3
        pristineDragonAttempts_t1.data.quizId == [q1.quizId, q1.quizId, q1.quizId]
        pristineDragonAttempts_t1.data.status == [
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        ]

        pristineDragonAttempts_t2.data.size() == 3
        pristineDragonAttempts_t2.data.quizId == [q1.quizId, q1.quizId, q1.quizId]
        pristineDragonAttempts_t2.data.status == [
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        ]

        allDragonsAttempts_t2.data.size() == 0
        allDragonsAttempts_t2.data == []
    }

    def "for non-UC user quiz attempt history only contains non-UC projects - multiple quizzes"() {
        // this can happen if a non-uc user takes a quiz and the quiz is elevated to UC at a later time
        List<String> users = getRandomUsers(3)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService adminUser = createService(users[2])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(adminUser.userName, 'dragons', ['DivineDragon'])

        def createQuiz = { int num ->
            def theQuiz = QuizDefFactory.createQuiz(num)
            adminUser.createQuizDef(theQuiz)
            def questions = QuizDefFactory.createChoiceQuestions(num, 1, 2)
            adminUser.createQuizQuestionDefs(questions)
            return theQuiz
        }
        def runQuiz = { def quiz, SkillsService userService, boolean pass ->
            def quizAttempt = userService.startQuizAttempt(quiz.quizId).body
            int answerIndex = pass ? 0 : 1
            userService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[answerIndex].id)
            def gradedQuizAttempt = userService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
            return gradedQuizAttempt
        }


        def q1 = createQuiz(1)
        def q2 = createQuiz(2)

        runQuiz(q1, allDragonsUser, false)
        runQuiz(q2, allDragonsUser, false)

        runQuiz(q1, allDragonsUser, false)
        runQuiz(q2, allDragonsUser, false)

        runQuiz(q1, allDragonsUser, true)
        runQuiz(q2, allDragonsUser, true)

        runQuiz(q1, pristineDragonsUser, false)
        runQuiz(q2, pristineDragonsUser, false)

        runQuiz(q1, pristineDragonsUser, false)
        runQuiz(q2, pristineDragonsUser, false)

        runQuiz(q1, pristineDragonsUser, true)
        runQuiz(q2, pristineDragonsUser, true)

        when:
        def allDragonsAttempts_t1 = allDragonsUser.getCurrentUserQuizAttempts()
        def pristineDragonAttempts_t1 = pristineDragonsUser.getCurrentUserQuizAttempts()

        q1.enableProtectedUserCommunity = true
        adminUser.createQuizDef(q1, q1.quizId)
        def allDragonsAttempts_t2 = allDragonsUser.getCurrentUserQuizAttempts()
        def pristineDragonAttempts_t2 = pristineDragonsUser.getCurrentUserQuizAttempts()

        then:
        allDragonsAttempts_t1.data.size() == 6
        allDragonsAttempts_t1.data.quizId == [q1.quizId, q2.quizId, q1.quizId, q2.quizId, q1.quizId, q2.quizId]

        allDragonsAttempts_t1.data.status == [
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        ]
        pristineDragonAttempts_t1.data.size() == 6
        pristineDragonAttempts_t1.data.quizId == [q1.quizId, q2.quizId, q1.quizId, q2.quizId, q1.quizId, q2.quizId]
        pristineDragonAttempts_t1.data.status == [
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        ]

        pristineDragonAttempts_t2.data.size() == 6
        pristineDragonAttempts_t2.data.quizId == [q1.quizId, q2.quizId, q1.quizId, q2.quizId, q1.quizId, q2.quizId]
        pristineDragonAttempts_t2.data.status == [
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        ]

        allDragonsAttempts_t2.data.size() == 3
        allDragonsAttempts_t2.data.quizId == [q2.quizId, q2.quizId, q2.quizId]
        allDragonsAttempts_t2.data.status == [
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.FAILED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        ]
    }

    def "not allowed to get uc quiz attempts for non-uc user"() {
        // this can happen if a non-uc user takes a quiz and the quiz is elevated to UC at a later time
        List<String> users = getRandomUsers(3)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService adminUser = createService(users[2])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(adminUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        adminUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        adminUser.createQuizQuestionDefs(questions)

        def runQuiz = { SkillsService userService, boolean pass ->
            def quizAttempt = userService.startQuizAttempt(q1.quizId).body
            int answerIndex = pass ? 0 : 1
            userService.reportQuizAnswer(q1.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[answerIndex].id)
            def gradedQuizAttempt = userService.completeQuizAttempt(q1.quizId, quizAttempt.id).body
            return quizAttempt
        }
        def allDragonsUserAttempt = runQuiz(allDragonsUser, false)
        def pristineDragonAttempt = runQuiz(pristineDragonsUser, false)
        q1.enableProtectedUserCommunity = true
        adminUser.createQuizDef(q1, q1.quizId)
        def pristineDragonAttemptInfo = pristineDragonsUser.getCurrentUserSingleQuizAttempt(pristineDragonAttempt.id)
        when:
        allDragonsUser.getCurrentUserSingleQuizAttempt(allDragonsUserAttempt.id)
        then:
        pristineDragonAttemptInfo.quizName == q1.name
        SkillsClientException e = thrown(SkillsClientException)
        e.resBody.contains("User [${allDragonsUser.userName}] does not have access to quiz [${allDragonsUserAttempt.id}]")
    }

    def "non-UC user cannot cannot create UC protected quiz"() {
        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true

        when:
        skillsService.createQuizDef(q1)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${skillsService.userName}] is not allowed to set [enableProtectedUserCommunity] to true")
    }

    def "non-UC user cannot copy non-uc quiz to uc"() {
        def q1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(q1)

        when:
        def copy = [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: q1.type, enableProtectedUserCommunity: true]
        skillsService.copyQuiz(q1.quizId, copy)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${skillsService.userName}] is not allowed to set [enableProtectedUserCommunity] to true")
    }

    def "non-UC user cannot access quiz admin endpoints with UC protection enabled"() {
        List<String> users = getRandomUsers(2)
        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true

        when:
        pristineDragonsUser.createQuizDef(q1)

        then:
        validateForbidden { allDragonsUser.createQuizDef(q1, q1.quizId) }
        validateForbidden { allDragonsUser.removeQuizDef(q1.quizId) }
        validateForbidden { allDragonsUser.validateQuizForEnablingCommunity(q1.quizId) }
        validateForbidden { allDragonsUser.getQuizDef(q1.quizId) }
        validateForbidden { allDragonsUser.countSkillsForQuiz(q1.quizId) }
        validateForbidden { allDragonsUser.getSkillsForQuiz(q1.quizId) }
        validateForbidden { allDragonsUser.getQuizDefSummary(q1.quizId) }
        validateForbidden { allDragonsUser.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.SingleChoice)) }
        validateForbidden { allDragonsUser.updateQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.SingleChoice)) }
        validateForbidden { allDragonsUser.deleteQuizQuestionDef(q1.quizId, 1) }
        validateForbidden { allDragonsUser.changeQuizQuestionDisplayOrder(q1.quizId, 1, 1) }
        validateForbidden { allDragonsUser.getQuizQuestionDefs(q1.quizId) }
        validateForbidden { allDragonsUser.getQuizQuestionDef(q1.quizId, 1) }
        validateForbidden { allDragonsUser.getUserQuizAnswers(q1.quizId, 1) }
        validateForbidden { allDragonsUser.getQuizMetrics(q1.quizId) }
        validateForbidden { allDragonsUser.getQuizRuns(q1.quizId) }
        validateForbidden { allDragonsUser.deleteQuizRun(q1.quizId, 1) }
        validateForbidden { allDragonsUser.getQuizAttemptResult(q1.quizId, 1) }
        validateForbidden { allDragonsUser.startQuizAttempt(q1.quizId) }
        validateForbidden { allDragonsUser.reportQuizAnswer(q1.quizId, 1,1) }
        validateForbidden { allDragonsUser.gradeAnswer(skillsService.userName, q1.quizId, 1, 1, true) }
        validateForbidden { allDragonsUser.failQuizAttempt(q1.quizId, 1) }
        validateForbidden { allDragonsUser.completeQuizAttempt(q1.quizId, 1) }
        validateForbidden { allDragonsUser.saveQuizSettings(q1.quizId, [[setting: QuizSettings.QuizLength.setting, value: '1']]) }
        validateForbidden { allDragonsUser.getQuizSettings(q1.quizId) }
        validateForbidden { allDragonsUser.saveQuizUserPreference(q1.quizId, QuizUserPreferences.DisableGradingRequestNotification.preference, true) }
        validateForbidden { allDragonsUser.getCurrentUserQuizPreferences(q1.quizId) }
        validateForbidden { allDragonsUser.addQuizUserRole(q1.quizId, allDragonsUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString()) }
        validateForbidden { allDragonsUser.getQuizUserRoles(q1.quizId) }
        validateForbidden { allDragonsUser.deleteQuizUserRole(q1.quizId, allDragonsUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString()) }
        validateForbidden { allDragonsUser.getQuizUserTagCounts(q1.quizId, "tag") }
        validateForbidden { allDragonsUser.getQuizUsageOverTime(q1.quizId) }
        validateForbidden { allDragonsUser.getUserActionsForQuiz(q1.quizId) }
        validateForbidden { allDragonsUser.getUserActionFilterOptionsForQuiz(q1.quizId) }
        validateForbidden { allDragonsUser.getQuizUserActionAttributes(q1.quizId, 1) }
        validateForbidden { allDragonsUser.getAdminGroupsForQuiz(q1.quizId) }
    }

    def "user was UC but then became non-UC - cannot access quiz admin endpoints with UC protection enabled"() {
        List<String> users = getRandomUsers(2)
        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)

        when:
        rootUser.saveUserTag(allDragonsUser.userName, 'dragons', ['DivineDragon'])
        allDragonsUser.createQuizDef(q1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        allDragonsUser.createQuizQuestionDefs(questions)
        def quizAttempt =  allDragonsUser.startQuizAttempt(q1.quizId).body
        transactionTemplate.execute({
            userTagRepo.deleteByUserId(allDragonsUser.userName)
        })

        then:
        validateForbidden { allDragonsUser.validateQuizForEnablingCommunity(q1.quizId) }
        validateForbidden { allDragonsUser.getQuizDef(q1.quizId) }
        validateForbidden { allDragonsUser.countSkillsForQuiz(q1.quizId) }
        validateForbidden { allDragonsUser.getSkillsForQuiz(q1.quizId) }
        validateForbidden { allDragonsUser.getQuizDefSummary(q1.quizId) }
        validateForbidden { allDragonsUser.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.SingleChoice)) }
        validateForbidden { allDragonsUser.updateQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.SingleChoice)) }
        validateForbidden { allDragonsUser.deleteQuizQuestionDef(q1.quizId, 1) }
        validateForbidden { allDragonsUser.changeQuizQuestionDisplayOrder(q1.quizId, 1, 1) }
        validateForbidden { allDragonsUser.getQuizQuestionDefs(q1.quizId) }
        validateForbidden { allDragonsUser.getQuizQuestionDef(q1.quizId, 1) }
        validateForbidden { allDragonsUser.getUserQuizAnswers(q1.quizId, 1) }
        validateForbidden { allDragonsUser.getQuizMetrics(q1.quizId) }
        validateForbidden { allDragonsUser.getQuizRuns(q1.quizId) }
        validateForbidden { allDragonsUser.deleteQuizRun(q1.quizId, quizAttempt.id) }
        validateForbidden { allDragonsUser.getQuizAttemptResult(q1.quizId, 1) }
        validateForbidden { allDragonsUser.startQuizAttempt(q1.quizId) }
        validateForbidden { allDragonsUser.reportQuizAnswer(q1.quizId, quizAttempt.id,1) }
        validateForbidden { allDragonsUser.gradeAnswer(skillsService.userName, q1.quizId, 1, 1, true) }
        validateForbidden { allDragonsUser.failQuizAttempt(q1.quizId, quizAttempt.id) }
        validateForbidden { allDragonsUser.completeQuizAttempt(q1.quizId, quizAttempt.id) }
        validateForbidden { allDragonsUser.saveQuizSettings(q1.quizId, [[setting: QuizSettings.QuizLength.setting, value: '1']]) }
        validateForbidden { allDragonsUser.getQuizSettings(q1.quizId) }
        validateForbidden { allDragonsUser.saveQuizUserPreference(q1.quizId, QuizUserPreferences.DisableGradingRequestNotification.preference, true) }
        validateForbidden { allDragonsUser.getCurrentUserQuizPreferences(q1.quizId) }
        validateForbidden { allDragonsUser.addQuizUserRole(q1.quizId, allDragonsUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString()) }
        validateForbidden { allDragonsUser.getQuizUserRoles(q1.quizId) }
        validateForbidden { allDragonsUser.deleteQuizUserRole(q1.quizId, allDragonsUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString()) }
        validateForbidden { allDragonsUser.getQuizUserTagCounts(q1.quizId, "tag") }
        validateForbidden { allDragonsUser.getQuizUsageOverTime(q1.quizId) }
        validateForbidden { allDragonsUser.getUserActionsForQuiz(q1.quizId) }
        validateForbidden { allDragonsUser.getUserActionFilterOptionsForQuiz(q1.quizId) }
        validateForbidden { allDragonsUser.getQuizUserActionAttributes(q1.quizId, 1) }
        validateForbidden { allDragonsUser.getAdminGroupsForQuiz(q1.quizId) }
        validateForbidden { allDragonsUser.removeQuizDef(q1.quizId) }
    }

    def "UC user can access quiz admin endpoints with UC protection enabled"() {
        List<String> users = getRandomUsers(2)
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true

        when:
        pristineDragonsUser.createQuizDef(q1)

        then:
        !validateForbidden { pristineDragonsUser.createQuizDef(q1, q1.quizId) }
        !validateForbidden { pristineDragonsUser.getQuizDef(q1.quizId) }
        !validateForbidden { pristineDragonsUser.countSkillsForQuiz(q1.quizId) }
        !validateForbidden { pristineDragonsUser.getSkillsForQuiz(q1.quizId) }
        !validateForbidden { pristineDragonsUser.getQuizDefSummary(q1.quizId) }
        !validateForbidden { pristineDragonsUser.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.SingleChoice)) }
        !validateForbidden { pristineDragonsUser.updateQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.SingleChoice)) }
        !validateForbidden { pristineDragonsUser.deleteQuizQuestionDef(q1.quizId, 1) }
        !validateForbidden { pristineDragonsUser.changeQuizQuestionDisplayOrder(q1.quizId, 1, 1) }
        !validateForbidden { pristineDragonsUser.getQuizQuestionDefs(q1.quizId) }
        !validateForbidden { pristineDragonsUser.getQuizQuestionDef(q1.quizId, 1) }
        !validateForbidden { pristineDragonsUser.getUserQuizAnswers(q1.quizId, 1) }
        !validateForbidden { pristineDragonsUser.getQuizMetrics(q1.quizId) }
        !validateForbidden { pristineDragonsUser.getQuizRuns(q1.quizId) }
        !validateForbidden { pristineDragonsUser.deleteQuizRun(q1.quizId, 1) }
        !validateForbidden { pristineDragonsUser.getQuizAttemptResult(q1.quizId, 1) }
        !validateForbidden { pristineDragonsUser.startQuizAttempt(q1.quizId) }
        !validateForbidden { pristineDragonsUser.reportQuizAnswer(q1.quizId, 1,1) }
        !validateForbidden { pristineDragonsUser.gradeAnswer(skillsService.userName, q1.quizId, 1, 1, true) }
        !validateForbidden { pristineDragonsUser.failQuizAttempt(q1.quizId, 1) }
        !validateForbidden { pristineDragonsUser.completeQuizAttempt(q1.quizId, 1) }
        !validateForbidden { pristineDragonsUser.saveQuizSettings(q1.quizId, [[setting: QuizSettings.QuizLength.setting, value: '1']]) }
        !validateForbidden { pristineDragonsUser.getQuizSettings(q1.quizId) }
        !validateForbidden { pristineDragonsUser.saveQuizUserPreference(q1.quizId, QuizUserPreferences.DisableGradingRequestNotification.preference, true) }
        !validateForbidden { pristineDragonsUser.getCurrentUserQuizPreferences(q1.quizId) }
        !validateForbidden { pristineDragonsUser.getQuizUserRoles(q1.quizId) }
        !validateForbidden { pristineDragonsUser.getQuizUserTagCounts(q1.quizId, "tag") }
        !validateForbidden { pristineDragonsUser.getQuizUsageOverTime(q1.quizId) }
        !validateForbidden { pristineDragonsUser.getUserActionsForQuiz(q1.quizId) }
        !validateForbidden { pristineDragonsUser.getUserActionFilterOptionsForQuiz(q1.quizId) }
        !validateForbidden { pristineDragonsUser.getQuizUserActionAttributes(q1.quizId, 1) }
        !validateForbidden { pristineDragonsUser.validateQuizForEnablingCommunity(q1.quizId) }
        !validateForbidden { pristineDragonsUser.getAdminGroupsForQuiz(q1.quizId) }
        !validateForbidden { pristineDragonsUser.removeQuizDef(q1.quizId) }
    }

    def "cannot download attachments associated with a UC protected project if the user does not belong to the user community"() {
        when:
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1)

        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = (Resource)GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        def quizAttachment = skillsService.uploadAttachment(resource, (String)null, (String)null, q1.quizId)

        then:
        pristineDragonsUser.downloadAttachmentAsText(quizAttachment.href) == "Test is a test"
        println quizAttachment.href
        validateForbidden { allDragonsUser.downloadAttachment(quizAttachment.href) }
    }

    def "non-UC user is not given credit for non-UC quiz associated with UC protected skill"() {
        when:
        List<String> users = getRandomUsers(3)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService adminUser = createService(users[2])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(adminUser.userName, 'dragons', ['DivineDragon'])

        def quiz = QuizDefFactory.createQuiz(1)
        adminUser.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        adminUser.createQuizQuestionDefs(questions)

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        def subjP1 = createSubject(1, 1)

        def skillWithQuizP1 = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuizP1.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuizP1.quizId = quiz.quizId
        adminUser.createProjectAndSubjectAndSkills(p1, subjP1, [skillWithQuizP1])

        def p2 = createProject(2)
        p2.enableProtectedUserCommunity = false
        def subjP2 = createSubject(2, 1)

        def skillWithQuizP2 = createSkill(2, 1, 1, 1, 1, 480, 200)
        skillWithQuizP2.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuizP2.quizId = quiz.quizId
        adminUser.createProjectAndSubjectAndSkills(p2, subjP2, [skillWithQuizP2])

        def runQuiz = { SkillsService userService, boolean pass ->
            def quizAttempt = userService.startQuizAttempt(quiz.quizId).body
            int answerIndex = pass ? 0 : 1
            userService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[answerIndex].id)
            return userService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        }
        def allDragonsUserAttempt = runQuiz(allDragonsUser, true)
        def pristineDragonAttempt = runQuiz(pristineDragonsUser, true)

        def allDragonsUserPerformedSkillsP1 = getPerformedSkillsForUser(allDragonsUser.userName, p1.projectId)
        def pristineDragonsUserPerformedSkillsP1 = getPerformedSkillsForUser(pristineDragonsUser.userName, p1.projectId)

        def allDragonsUserPerformedSkillsP2 = getPerformedSkillsForUser(allDragonsUser.userName, p2.projectId)
        def pristineDragonsUserPerformedSkillsP2 = getPerformedSkillsForUser(pristineDragonsUser.userName, p2.projectId)

        then:
        allDragonsUserAttempt.passed
        pristineDragonAttempt.passed

        !allDragonsUserPerformedSkillsP1
        pristineDragonsUserPerformedSkillsP1
        pristineDragonsUserPerformedSkillsP1.size() == 1
        pristineDragonsUserPerformedSkillsP1[0].skillId == skillWithQuizP1.skillId

        allDragonsUserPerformedSkillsP2
        allDragonsUserPerformedSkillsP2.size() == 1
        allDragonsUserPerformedSkillsP2
        pristineDragonsUserPerformedSkillsP2[0].skillId == skillWithQuizP1.skillId
        pristineDragonsUserPerformedSkillsP2.size() == 1
        pristineDragonsUserPerformedSkillsP2[0].skillId == skillWithQuizP1.skillId
    }

    def getPerformedSkillsForUser(String userId, String projectId) {
        PageRequest allPlease = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "performedOn"))
        List<UserPerformedSkillRepo.PerformedSkillQRes> performedSkills = performedSkillRepository.findByUserIdAndProjectIdAndSkillIdIgnoreCaseContaining(userId, projectId, '', allPlease)
        return performedSkills
    }

    private static boolean validateForbidden(Closure c) {
        try {
            def res = c.call()
            return false
        } catch (SkillsClientException skillsClientException) {
            return skillsClientException.httpStatus == HttpStatus.FORBIDDEN
        }
    }

}

