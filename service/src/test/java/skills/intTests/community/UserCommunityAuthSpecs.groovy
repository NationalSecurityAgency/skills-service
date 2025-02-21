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
package skills.intTests.community

import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import skills.intTests.utils.*
import skills.storage.model.auth.RoleName
import skills.utils.GroovyToJavaByteUtils
import skills.utils.WaitFor

import static skills.intTests.utils.AdminGroupDefFactory.createAdminGroup
import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSubject


@Slf4j
class UserCommunityAuthSpecs extends DefaultIntSpec {
    SkillsService rootSkillsService

    def setup() {
        rootSkillsService = createRootSkillService()
        skillsService.getCurrentUser() // initialize skillsService user_attrs
    }

    def "cannot access project endpoints with UC protection enabled if the user does not belong to the user community"() {

        when:
        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def proj = createProject(1)
        proj.enableProtectedUserCommunity = true
        def subj = createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
        Map badge = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(proj.projectId, badge.badgeId, skill.skillId)

        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService nonUserCommunityUser = createService(nonUserCommunityUserId)

        then:
        validateForbidden { nonUserCommunityUser.apiGetUserLevelForProject(proj.projectId, null) }
        validateForbidden { nonUserCommunityUser.lookupMyProjectName(proj.projectId) }
        validateForbidden { nonUserCommunityUser.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.0.0") }
        validateForbidden { nonUserCommunityUser.contactProjectOwner(proj.projectId, "a message") }
        validateForbidden { nonUserCommunityUser.getSubjectDescriptions(proj.projectId, subj.subjectId) }
        validateForbidden { nonUserCommunityUser.getBadgeDescriptions(proj.projectId, badge.badgeId) }
        validateForbidden { nonUserCommunityUser.addSkill([projectId: proj.projectId, skillId: skill.skillId]) }
        validateForbidden { nonUserCommunityUser.getSkillSummary(null, proj.projectId, subj.subjectId) }
        validateForbidden { nonUserCommunityUser.documentVisitedSkillId(proj.projectId, skill.skillId) }
        validateForbidden { nonUserCommunityUser.getSkillsSummaryForCurrentUser(proj.projectId) }
        validateForbidden { nonUserCommunityUser.addMyProject(proj.projectId) }
        validateForbidden { nonUserCommunityUser.moveMyProject(proj.projectId, 1) }
        validateForbidden { nonUserCommunityUser.removeMyProject(proj.projectId) }
        validateForbidden { nonUserCommunityUser.getSingleSkillSummaryForCurrentUser(proj.projectId, skill.skillId) }
        validateForbidden { nonUserCommunityUser.getSubjectSummaryForCurrentUser(proj.projectId, subj.subjectId) }
        validateForbidden { nonUserCommunityUser.getBadgesSummary(null, proj.projectId) }
        validateForbidden { nonUserCommunityUser.getBadgeSummary(null, proj.projectId, badge.badgeId) }
        validateForbidden { nonUserCommunityUser.getUsersPerLevel(proj.projectId) }
        validateForbidden { nonUserCommunityUser.getRank(null, proj.projectId) }
        validateForbidden { nonUserCommunityUser.getRank(null, proj.projectId, subj.subjectId)}
        validateForbidden { nonUserCommunityUser.getLeaderboard(null, proj.projectId)}
        validateForbidden { nonUserCommunityUser.getLeaderboard(null, proj.projectId, subj.subjectId)}
        validateForbidden { nonUserCommunityUser.getRankDistribution(null, proj.projectId)}
        validateForbidden { nonUserCommunityUser.getRankDistribution(null, proj.projectId, subj.subjectId)}
        validateForbidden { nonUserCommunityUser.getPointHistory(null, proj.projectId)}
        validateForbidden { nonUserCommunityUser.getPointHistory(null, proj.projectId, subj.subjectId)}
    }

    def "can access project endpoints with UC protection enabled when the user does belong to the user community"() {
        when:
        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def proj = createProject(1)
        proj.enableProtectedUserCommunity = true
        def subj = createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
        Map badge = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(proj.projectId, badge.badgeId, skill.skillId)

        def otherUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUserCommunityUser = createService(otherUserCommunityUserId)
        rootSkillsService.saveUserTag(otherUserCommunityUserId, 'dragons', ['DivineDragon'])

        then:
        !validateForbidden { otherUserCommunityUser.apiGetUserLevelForProject(proj.projectId, null) }
        !validateForbidden { otherUserCommunityUser.lookupMyProjectName(proj.projectId) }
        !validateForbidden { otherUserCommunityUser.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.0.0") }
        !validateForbidden { otherUserCommunityUser.contactProjectOwner(proj.projectId, "a message") }
        !validateForbidden { otherUserCommunityUser.getSubjectDescriptions(proj.projectId, subj.subjectId) }
        !validateForbidden { otherUserCommunityUser.getBadgeDescriptions(proj.projectId, badge.badgeId) }
        !validateForbidden { otherUserCommunityUser.addSkill([projectId: proj.projectId, skillId: skill.skillId]) }
        !validateForbidden { otherUserCommunityUser.getSkillSummary(null, proj.projectId, subj.subjectId) }
        !validateForbidden { otherUserCommunityUser.documentVisitedSkillId(proj.projectId, skill.skillId) }
        !validateForbidden { otherUserCommunityUser.getSkillsSummaryForCurrentUser(proj.projectId) }
        !validateForbidden { otherUserCommunityUser.addMyProject(proj.projectId) }
        !validateForbidden { otherUserCommunityUser.moveMyProject(proj.projectId, 1) }
        !validateForbidden { otherUserCommunityUser.removeMyProject(proj.projectId) }
        !validateForbidden { otherUserCommunityUser.getSingleSkillSummaryForCurrentUser(proj.projectId, skill.skillId) }
        !validateForbidden { otherUserCommunityUser.getSubjectSummaryForCurrentUser(proj.projectId, subj.subjectId) }
        !validateForbidden { otherUserCommunityUser.getBadgesSummary(null, proj.projectId) }
        !validateForbidden { otherUserCommunityUser.getBadgeSummary(null, proj.projectId, badge.badgeId) }
        !validateForbidden { otherUserCommunityUser.getUsersPerLevel(proj.projectId) }
        !validateForbidden { otherUserCommunityUser.getRank(null, proj.projectId) }
        !validateForbidden { otherUserCommunityUser.getRank(null, proj.projectId, subj.subjectId)}
        !validateForbidden { otherUserCommunityUser.getLeaderboard(null, proj.projectId)}
        !validateForbidden { otherUserCommunityUser.getLeaderboard(null, proj.projectId, subj.subjectId)}
        !validateForbidden { otherUserCommunityUser.getRankDistribution(null, proj.projectId)}
        !validateForbidden { otherUserCommunityUser.getRankDistribution(null, proj.projectId, subj.subjectId)}
        !validateForbidden { otherUserCommunityUser.getPointHistory(null, proj.projectId)}
        !validateForbidden { otherUserCommunityUser.getPointHistory(null, proj.projectId, subj.subjectId)}
    }

    def "cannot add a project admin role to a community protected project if the user is not a member of that community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        when:
        pristineDragonsUser.addUserRole(allDragonsUser.userName, p1.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${allDragonsUser.userName} for display] is not allowed to be assigned [Admin] user role")
    }

    def "cannot add project approver role to a UC protected project if the user is not a member of that community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        when:
        pristineDragonsUser.addUserRole(allDragonsUser.userName, p1.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${allDragonsUser.userName} for display] is not allowed to be assigned [Approver] user role")
    }

    def "cannot accept invite to an invite only project for a UC protected project if the user is not a member of that community"() {
        startEmailServer()
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def proj = createProject(1)
        proj.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(proj)

        pristineDragonsUser.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        when:

        pristineDragonsUser.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        String invite = extractInviteFromEmail(email.html)

        allDragonsUser.joinProject(proj.projectId, invite)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.FORBIDDEN
    }

    def "cannot download attachments associated with a UC protected project if the user does not belong to the user community"() {
        when:
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        Map proj = createProject()
        proj.enableProtectedUserCommunity = true
        Map subject = createSubject()
        Map skill = SkillsFactory.createSkill()
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 2)
        def badge = SkillsFactory.createBadge()
        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)

        pristineDragonsUser.createProject(proj)
        pristineDragonsUser.createSubject(subject)
        pristineDragonsUser.createSkill(skill)
        pristineDragonsUser.createSkill(skillsGroup)
        pristineDragonsUser.createBadge(badge)
        def projAttachment = skillsService.uploadAttachment(resource, proj.projectId)
        def subjAttachment = skillsService.uploadAttachment(resource, proj.projectId, subject.subjectId)
        def skillAttachment = skillsService.uploadAttachment(resource, proj.projectId, skill.skillId)
        def skillsGroupAttachment = skillsService.uploadAttachment(resource, proj.projectId, skillsGroup.skillId)
        def badgeAttachment = skillsService.uploadAttachment(resource, proj.projectId, badge.badgeId)

        then:
        validateForbidden { allDragonsUser.downloadAttachment(projAttachment.href) }
        validateForbidden { allDragonsUser.downloadAttachment(subjAttachment.href) }
        validateForbidden { allDragonsUser.downloadAttachment(skillAttachment.href) }
        validateForbidden { allDragonsUser.downloadAttachment(skillsGroupAttachment.href) }
        validateForbidden { allDragonsUser.downloadAttachment(badgeAttachment.href) }
    }

    def "cannot access group admin endpoints with UC protection enabled if the user does not belong to the user community"() {

        when:
        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = true
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        proj.enableProtectedUserCommunity = true
        def subj = createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService nonUserCommunityUser = createService(nonUserCommunityUserId)

        then:

        // get admin groups should not receive 403
        !validateForbidden { nonUserCommunityUser.getAdminGroupDefs() }
        nonUserCommunityUser.getAdminGroupDefs() == []

        // all others should
        validateForbidden { nonUserCommunityUser.getAdminGroupDef(adminGroup.adminGroupId) }
        validateForbidden { nonUserCommunityUser.getAdminGroupMembers(adminGroup.adminGroupId) }
        validateForbidden { nonUserCommunityUser.addAdminGroupOwner(adminGroup.adminGroupId, nonUserCommunityUserId) }
        validateForbidden { nonUserCommunityUser.addAdminGroupMember(adminGroup.adminGroupId, nonUserCommunityUserId) }
        validateForbidden { nonUserCommunityUser.deleteAdminGroupOwner(adminGroup.adminGroupId, nonUserCommunityUserId) }
        validateForbidden { nonUserCommunityUser.deleteAdminGroupMember(adminGroup.adminGroupId, nonUserCommunityUserId) }
        validateForbidden { nonUserCommunityUser.getAdminGroupQuizzesAndSurveys(adminGroup.adminGroupId) }
        validateForbidden { nonUserCommunityUser.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId) }
        validateForbidden { nonUserCommunityUser.deleteQuizFromAdminGroup(adminGroup.adminGroupId, quiz.quizId) }
        validateForbidden { nonUserCommunityUser.getAdminGroupProjects(adminGroup.adminGroupId) }
        validateForbidden { nonUserCommunityUser.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId) }
        validateForbidden { nonUserCommunityUser.deleteProjectFromAdminGroup(adminGroup.adminGroupId, proj.projectId) }
        validateForbidden { nonUserCommunityUser.updateAdminGroupDef(adminGroup) }
        validateForbidden { nonUserCommunityUser.removeAdminGroupDef(adminGroup.adminGroupId) }
    }

    def "can access group admin endpoints with UC protection enabled if the user does belong to the user community"() {

        when:
        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = true
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        proj.enableProtectedUserCommunity = true
        def subj = createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def otherUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        rootSkillsService.saveUserTag(otherUserCommunityUserId, 'dragons', ['DivineDragon'])

        then:

        // get admin groups should not receive 403
        !validateForbidden { skillsService.getAdminGroupDefs() }

        // all others should
        !validateForbidden { skillsService.getAdminGroupDef(adminGroup.adminGroupId) }
        !validateForbidden { skillsService.getAdminGroupMembers(adminGroup.adminGroupId) }
        !validateForbidden { skillsService.addAdminGroupOwner(adminGroup.adminGroupId, otherUserCommunityUserId) }
        !validateForbidden { skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserCommunityUserId) }
        !validateForbidden { skillsService.deleteAdminGroupOwner(adminGroup.adminGroupId, otherUserCommunityUserId) }
        !validateForbidden { skillsService.deleteAdminGroupMember(adminGroup.adminGroupId, otherUserCommunityUserId) }
        !validateForbidden { skillsService.getAdminGroupQuizzesAndSurveys(adminGroup.adminGroupId) }
        !validateForbidden { skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId) }
        !validateForbidden { skillsService.deleteQuizFromAdminGroup(adminGroup.adminGroupId, quiz.quizId) }
        !validateForbidden { skillsService.getAdminGroupProjects(adminGroup.adminGroupId) }
        !validateForbidden { skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId) }
        !validateForbidden { skillsService.deleteProjectFromAdminGroup(adminGroup.adminGroupId, proj.projectId) }
        !validateForbidden { skillsService.updateAdminGroupDef(adminGroup) }
        !validateForbidden { skillsService.removeAdminGroupDef(adminGroup.adminGroupId) }
    }

    def "cannot add non-UC user as owner to UC protected admin group"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = true
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        proj.enableProtectedUserCommunity = true
        def subj = createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(nonUserCommunityUserId)

        when:

        skillsService.addAdminGroupOwner(adminGroup.adminGroupId, nonUserCommunityUserId)

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("User [${nonUserCommunityUserId} for display] is not allowed to be assigned [Admin Group Owner] user role for admin group [${adminGroup.adminGroupId}]")
    }

    def "cannot add a non-UC user as member to UC protected admin group"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = true
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(nonUserCommunityUserId)
        when:

        skillsService.addAdminGroupMember(adminGroup.adminGroupId, nonUserCommunityUserId)

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("User [${nonUserCommunityUserId} for display] is not allowed to be assigned [Admin Group Member] user role for admin group [${adminGroup.adminGroupId}]")
    }

    def "cannot add UC protected project to a non-UC admin group"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def proj = createProject(1)
        proj.enableProtectedUserCommunity = true
        def subj = createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        when:

        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Project [${proj.name}] is not allowed to be assigned [${adminGroup.name}] Admin Group")
    }

    def "cannot enable UC protection for admin group if it contains a non UC member"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(nonUserCommunityUserId)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, nonUserCommunityUserId)

        when:
        adminGroup.enableProtectedUserCommunity = true
        skillsService.updateAdminGroupDef(adminGroup)

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Not Allowed to set [enableProtectedUserCommunity] to true for adminGroupId [${adminGroup.adminGroupId}]")
        e.message.contains("This admin group has the user ${nonUserCommunityUserId} for display who is not authorized")
    }

    def "cannot disable UC protection for admin group after it has already been enabled"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = true
        skillsService.createAdminGroupDef(adminGroup)

        when:
        adminGroup.enableProtectedUserCommunity = false
        skillsService.updateAdminGroupDef(adminGroup)

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Once admin group [enableProtectedUserCommunity=true] it cannot be flipped to false.  adminGroupId [${adminGroup.adminGroupId}]")
    }

    def "cannot enable UC protection on project if non-UC group is assigned to it already"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = false
        skillsService.createAdminGroupDef(adminGroup)

        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        when:

        proj.enableProtectedUserCommunity = true
        skillsService.updateProject(proj)

        then:

        true
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("This project is part of one or more Admin Groups that has not enabled user community protection")
    }

    def "cannot enable UC protection on project if non-UC group is assigned to it already, multiple members in group"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def otherUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUserCommunityUser = createService(otherUserCommunityUserId)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = false
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserCommunityUserId)

        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        when:

        proj.enableProtectedUserCommunity = true
        skillsService.updateProject(proj)

        then:

        true
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("This project is part of one or more Admin Groups that has not enabled user community protection")
    }

    String extractInviteFromEmail(String emailBody) {
        def regex = /join-project\/([^\/]+)\/([^?]+)/
        def matcher = emailBody =~ regex
        return matcher[0][2]
    }

    private static boolean validateForbidden(Closure c) {
        try {
            c.call()
            return false
        } catch (SkillsClientException skillsClientException) {
            return skillsClientException.httpStatus == HttpStatus.FORBIDDEN
        }
    }

}

