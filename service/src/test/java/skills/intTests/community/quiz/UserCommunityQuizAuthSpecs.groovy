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
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.UserQuizAttempt
import skills.utils.GroovyToJavaByteUtils

@Slf4j
class UserCommunityQuizAuthSpecs extends DefaultIntSpec {
    SkillsService rootSkillsService

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

//    def "non-UC user cannot access quiz admin endpoints with UC protection enabled"() {
//        SkillsService allDragonsUser = createService(users[0])
//        SkillsService pristineDragonsUser = createService(users[1])
//        SkillsService rootUser = createRootSkillService()
//        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
//
//        when:
//
//        def q1 = QuizDefFactory.createQuiz(1)
//        skillsService.createQuizDef(q1)
//
//        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
//        SkillsService nonUserCommunityUser = createService(nonUserCommunityUserId)
//
//        q1.enableProtectedUserCommunity = true
//
//        then:
//        validateForbidden { nonUserCommunityUser.createQuizDef(QuizDefFactory.createQuiz(1)) }
//    }

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
//
//    def "cannot access group admin endpoints with UC protection enabled if the user does not belong to the user community"() {
//
//        when:
//        String userCommunityUserId =  skillsService.userName
//        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])
//
//        def adminGroup = createAdminGroup(1)
//        adminGroup.enableProtectedUserCommunity = true
//        skillsService.createAdminGroupDef(adminGroup)
//
//        def proj = createProject(1)
//        proj.enableProtectedUserCommunity = true
//        def subj = createSubject(1, 1)
//        def skill = SkillsFactory.createSkill(1, 1)
//        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
//
//        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
//        skillsService.createQuizDef(quiz)
//
//        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
//        SkillsService nonUserCommunityUser = createService(nonUserCommunityUserId)
//
//        then:
//
//        // get admin groups should not receive 403
//        !validateForbidden { nonUserCommunityUser.getAdminGroupDefs() }
//        nonUserCommunityUser.getAdminGroupDefs() == []
//
//        // all others should
//        validateForbidden { nonUserCommunityUser.getAdminGroupDef(adminGroup.adminGroupId) }
//        validateForbidden { nonUserCommunityUser.getAdminGroupMembers(adminGroup.adminGroupId) }
//        validateForbidden { nonUserCommunityUser.addAdminGroupOwner(adminGroup.adminGroupId, nonUserCommunityUserId) }
//        validateForbidden { nonUserCommunityUser.addAdminGroupMember(adminGroup.adminGroupId, nonUserCommunityUserId) }
//        validateForbidden { nonUserCommunityUser.deleteAdminGroupOwner(adminGroup.adminGroupId, nonUserCommunityUserId) }
//        validateForbidden { nonUserCommunityUser.deleteAdminGroupMember(adminGroup.adminGroupId, nonUserCommunityUserId) }
//        validateForbidden { nonUserCommunityUser.getAdminGroupQuizzesAndSurveys(adminGroup.adminGroupId) }
//        validateForbidden { nonUserCommunityUser.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId) }
//        validateForbidden { nonUserCommunityUser.deleteQuizFromAdminGroup(adminGroup.adminGroupId, quiz.quizId) }
//        validateForbidden { nonUserCommunityUser.getAdminGroupProjects(adminGroup.adminGroupId) }
//        validateForbidden { nonUserCommunityUser.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId) }
//        validateForbidden { nonUserCommunityUser.deleteProjectFromAdminGroup(adminGroup.adminGroupId, proj.projectId) }
//        validateForbidden { nonUserCommunityUser.updateAdminGroupDef(adminGroup) }
//        validateForbidden { nonUserCommunityUser.removeAdminGroupDef(adminGroup.adminGroupId) }
//    }
//
//    def "can access group admin endpoints with UC protection enabled if the user does belong to the user community"() {
//
//        when:
//        String userCommunityUserId =  skillsService.userName
//        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])
//
//        def adminGroup = createAdminGroup(1)
//        adminGroup.enableProtectedUserCommunity = true
//        skillsService.createAdminGroupDef(adminGroup)
//
//        def proj = createProject(1)
//        proj.enableProtectedUserCommunity = true
//        def subj = createSubject(1, 1)
//        def skill = SkillsFactory.createSkill(1, 1)
//        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
//
//        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
//        skillsService.createQuizDef(quiz)
//
//        def otherUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
//        rootSkillsService.saveUserTag(otherUserCommunityUserId, 'dragons', ['DivineDragon'])
//
//        then:
//
//        // get admin groups should not receive 403
//        !validateForbidden { skillsService.getAdminGroupDefs() }
//
//        // all others should
//        !validateForbidden { skillsService.getAdminGroupDef(adminGroup.adminGroupId) }
//        !validateForbidden { skillsService.getAdminGroupMembers(adminGroup.adminGroupId) }
//        !validateForbidden { skillsService.addAdminGroupOwner(adminGroup.adminGroupId, otherUserCommunityUserId) }
//        !validateForbidden { skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserCommunityUserId) }
//        !validateForbidden { skillsService.deleteAdminGroupOwner(adminGroup.adminGroupId, otherUserCommunityUserId) }
//        !validateForbidden { skillsService.deleteAdminGroupMember(adminGroup.adminGroupId, otherUserCommunityUserId) }
//        !validateForbidden { skillsService.getAdminGroupQuizzesAndSurveys(adminGroup.adminGroupId) }
//        !validateForbidden { skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId) }
//        !validateForbidden { skillsService.deleteQuizFromAdminGroup(adminGroup.adminGroupId, quiz.quizId) }
//        !validateForbidden { skillsService.getAdminGroupProjects(adminGroup.adminGroupId) }
//        !validateForbidden { skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId) }
//        !validateForbidden { skillsService.deleteProjectFromAdminGroup(adminGroup.adminGroupId, proj.projectId) }
//        !validateForbidden { skillsService.updateAdminGroupDef(adminGroup) }
//        !validateForbidden { skillsService.removeAdminGroupDef(adminGroup.adminGroupId) }
//    }
//
//    def "cannot add non-UC user as owner to UC protected admin group"() {
//
//        String userCommunityUserId =  skillsService.userName
//        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])
//
//        def adminGroup = createAdminGroup(1)
//        adminGroup.enableProtectedUserCommunity = true
//        skillsService.createAdminGroupDef(adminGroup)
//
//        def proj = createProject(1)
//        proj.enableProtectedUserCommunity = true
//        def subj = createSubject(1, 1)
//        def skill = SkillsFactory.createSkill(1, 1)
//        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
//
//        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
//        createService(nonUserCommunityUserId)
//
//        when:
//
//        skillsService.addAdminGroupOwner(adminGroup.adminGroupId, nonUserCommunityUserId)
//
//        then:
//
//        SkillsClientException e = thrown(SkillsClientException)
//        e.message.contains("User [${nonUserCommunityUserId} for display] is not allowed to be assigned [Admin Group Owner] user role for admin group [${adminGroup.adminGroupId}]")
//    }
//
//    def "cannot add a non-UC user as member to UC protected admin group"() {
//
//        String userCommunityUserId =  skillsService.userName
//        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])
//
//        def adminGroup = createAdminGroup(1)
//        adminGroup.enableProtectedUserCommunity = true
//        skillsService.createAdminGroupDef(adminGroup)
//
//        def proj = createProject(1)
//        def subj = createSubject(1, 1)
//        def skill = SkillsFactory.createSkill(1, 1)
//        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
//
//        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
//        createService(nonUserCommunityUserId)
//        when:
//
//        skillsService.addAdminGroupMember(adminGroup.adminGroupId, nonUserCommunityUserId)
//
//        then:
//
//        SkillsClientException e = thrown(SkillsClientException)
//        e.message.contains("User [${nonUserCommunityUserId} for display] is not allowed to be assigned [Admin Group Member] user role for admin group [${adminGroup.adminGroupId}]")
//    }
//
//    def "cannot add UC protected project to a non-UC admin group"() {
//
//        String userCommunityUserId =  skillsService.userName
//        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])
//
//        def adminGroup = createAdminGroup(1)
//        skillsService.createAdminGroupDef(adminGroup)
//
//        def proj = createProject(1)
//        proj.enableProtectedUserCommunity = true
//        def subj = createSubject(1, 1)
//        def skill = SkillsFactory.createSkill(1, 1)
//        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
//
//        when:
//
//        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)
//
//        then:
//
//        SkillsClientException e = thrown(SkillsClientException)
//        e.message.contains("Project [${proj.name}] is not allowed to be assigned [${adminGroup.name}] Admin Group")
//    }
//
//    def "cannot enable UC protection for admin group if it contains a non UC member"() {
//
//        String userCommunityUserId =  skillsService.userName
//        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])
//
//        def adminGroup = createAdminGroup(1)
//        skillsService.createAdminGroupDef(adminGroup)
//
//        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
//        createService(nonUserCommunityUserId)
//        skillsService.addAdminGroupMember(adminGroup.adminGroupId, nonUserCommunityUserId)
//
//        when:
//        adminGroup.enableProtectedUserCommunity = true
//        skillsService.updateAdminGroupDef(adminGroup)
//
//        then:
//
//        SkillsClientException e = thrown(SkillsClientException)
//        e.message.contains("Not Allowed to set [enableProtectedUserCommunity] to true for adminGroupId [${adminGroup.adminGroupId}]")
//        e.message.contains("Has existing ${nonUserCommunityUserId} for display user that is not authorized")
//    }
//
//    def "cannot disable UC protection for admin group after it has already been enabled"() {
//
//        String userCommunityUserId =  skillsService.userName
//        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])
//
//        def adminGroup = createAdminGroup(1)
//        adminGroup.enableProtectedUserCommunity = true
//        skillsService.createAdminGroupDef(adminGroup)
//
//        when:
//        adminGroup.enableProtectedUserCommunity = false
//        skillsService.updateAdminGroupDef(adminGroup)
//
//        then:
//
//        SkillsClientException e = thrown(SkillsClientException)
//        e.message.contains("Once admin group [enableProtectedUserCommunity=true] it cannot be flipped to false.  adminGroupId [${adminGroup.adminGroupId}]")
//    }
//
//    def "cannot enable UC protection on project if non-UC group is assigned to it already"() {
//
//        String userCommunityUserId =  skillsService.userName
//        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])
//
//        def proj = createProject(1)
//        def subj = createSubject(1, 1)
//        def skill = SkillsFactory.createSkill(1, 1)
//        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
//
//        def adminGroup = createAdminGroup(1)
//        adminGroup.enableProtectedUserCommunity = false
//        skillsService.createAdminGroupDef(adminGroup)
//
//        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)
//
//        when:
//
//        proj.enableProtectedUserCommunity = true
//        skillsService.updateProject(proj)
//
//        then:
//
//        true
//        SkillsClientException e = thrown(SkillsClientException)
//        e.message.contains("This project is part of one or more Admin Groups that has not enabled user community protection")
//    }
//
//    def "cannot enable UC protection on project if non-UC group is assigned to it already, multiple members in group"() {
//
//        String userCommunityUserId =  skillsService.userName
//        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])
//
//        def otherUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
//        SkillsService otherUserCommunityUser = createService(otherUserCommunityUserId)
//
//        def proj = createProject(1)
//        def subj = createSubject(1, 1)
//        def skill = SkillsFactory.createSkill(1, 1)
//        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
//
//        def adminGroup = createAdminGroup(1)
//        adminGroup.enableProtectedUserCommunity = false
//        skillsService.createAdminGroupDef(adminGroup)
//        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserCommunityUserId)
//
//        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)
//
//        when:
//
//        proj.enableProtectedUserCommunity = true
//        skillsService.updateProject(proj)
//
//        then:
//
//        true
//        SkillsClientException e = thrown(SkillsClientException)
//        e.message.contains("This project is part of one or more Admin Groups that has not enabled user community protection")
//    }
//
//    String extractInviteFromEmail(String emailBody) {
//        def regex = /join-project\/([^\/]+)\/([^?]+)/
//        def matcher = emailBody =~ regex
//        return matcher[0][2]
//    }

    private static boolean validateForbidden(Closure c) {
        try {
            c.call()
            return false
        } catch (SkillsClientException skillsClientException) {
            return skillsClientException.httpStatus == HttpStatus.FORBIDDEN
        }
    }

}

