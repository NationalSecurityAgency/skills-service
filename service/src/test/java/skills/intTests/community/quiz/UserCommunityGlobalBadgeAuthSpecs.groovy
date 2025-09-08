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
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import skills.intTests.utils.*
import skills.storage.repos.UserPerformedSkillRepo

import static skills.intTests.utils.AdminGroupDefFactory.createAdminGroup
import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class UserCommunityGlobalBadgeAuthSpecs extends DefaultIntSpec {
    SkillsService rootSkillsService

    @Autowired
    UserPerformedSkillRepo performedSkillRepository

    def setup() {
        rootSkillsService = createRootSkillService()
        skillsService.getCurrentUser() // initialize skillsService user_attrs
    }

    def "cannot access badge endpoints with UC protection enabled if the user does not belong to the user community"() {

        when:
        rootSkillsService.saveUserTag(skillsService.userName, 'dragons', ['DivineDragon'])
        def proj1 = createProject(1)
        proj1.enableProtectedUserCommunity = true
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)
        def badge1 = createBadge(1)
        badge1.enableProtectedUserCommunity = true
        skillsService.createGlobalBadge(badge1)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj1.projectId, badgeId: badge1.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: badge1.badgeId, skillId: proj1_skills.get(0).skillId)
        badge1.enabled  = 'true'
        def badge2 = createBadge(1, 2)
        badge2.enableProtectedUserCommunity = false
        ClassPathResource resource = new ClassPathResource("/dot2.png")
        def file = resource.getFile()

        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService nonUserCommunityUser = createService(nonUserCommunityUserId)

        then:
        // api endpoints
        validateForbidden { nonUserCommunityUser.getCustomIconCssForGlobalBadge(badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.getBadgeSummary(null, proj1.projectId,  badge1.badgeId,-1, true) }
        validateForbidden { nonUserCommunityUser.getBadgeDescriptions(proj1.projectId, badge1.badgeId, true) }

        // admin endpoints
        validateForbidden { nonUserCommunityUser.updateBadge(badge1, badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.assignSkillToBadge(proj1.projectId, badge1.badgeId, proj1_skills.get(1).skillId) }
        validateForbidden { nonUserCommunityUser.assignSkillsToBadge(proj1.projectId, badge1.badgeId,[proj1_skills.get(1).skillId,  proj1_skills.get(2).skillId]) }
        validateForbidden { nonUserCommunityUser.removeSkillFromBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, skillId: proj1_skills.get(0).skillId]) }
        validateForbidden { nonUserCommunityUser.removeBadge([projectId: proj1.projectId, badgeId: badge1.badgeId]) }
        validateForbidden { nonUserCommunityUser.getBadge(proj1.projectId, badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.changeBadgeDisplayOrder(badge1, 0) }
        validateForbidden { nonUserCommunityUser.getBadgeUsers(proj1.projectId, badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.getBadgeSkills(proj1.projectId, badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.getSkillDependencyInfo(nonUserCommunityUserId, proj1.projectId, badge1.badgeId) }

        // global badge endpoints
        validateForbidden { nonUserCommunityUser.updateGlobalBadge(badge1, badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.assignSkillToGlobalBadge(proj1.projectId, badge1.badgeId, proj1_skills.get(1).skillId) }
        validateForbidden { nonUserCommunityUser.removeSkillFromGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, skillId: proj1_skills.get(0).skillId]) }
        validateForbidden { nonUserCommunityUser.deleteGlobalBadge(badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.getGlobalBadge(badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.changeGlobalBadgeDisplayOrder(badge1, 0) }
        validateForbidden { nonUserCommunityUser.getGlobalBadgeSkills(badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.getAvailableSkillsForGlobalBadge(badge1.badgeId, '') }
        validateForbidden { nonUserCommunityUser.getAvailableProjectsForGlobalBadge(badge1.badgeId, '') }
        validateForbidden { nonUserCommunityUser.assignProjectLevelToGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, level: '1']) }
        validateForbidden { nonUserCommunityUser.removeProjectLevelFromGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, level: '1']) }
        validateForbidden { nonUserCommunityUser.getCustomIconsForBadge([badgeId: badge1.badgeId]) }
        validateForbidden { nonUserCommunityUser.uploadGlobalIcon(badge1, file, true) }
        validateForbidden { nonUserCommunityUser.deleteGlobalIcon([badgeId:(badge1.badgeId), filename: "dot2.png"]) }
        validateForbidden { nonUserCommunityUser.changeProjectLevelOnGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, currentLevel: "1", newLevel: "2"]) }
        validateForbidden { nonUserCommunityUser.grantGlobalBadgeAdminRole(badge1.badgeId, 'user1') }
        validateForbidden { nonUserCommunityUser.getUserRolesForGlobalBadge(badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.removeGlobalBadgeAdminRole(badge1.badgeId, 'user1') }
        validateForbidden { nonUserCommunityUser.getAdminGroupsForGlobalBadge(badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.validateGlobalBadgeForEnablingCommunity(badge1.badgeId) }

        // check if badgeId exists and create new badge (w/o UC protection) should be ok
        !validateForbidden { nonUserCommunityUser.doesGlobalBadgeIdExists(badge1.badgeId) }
        !validateForbidden { nonUserCommunityUser.createGlobalBadge(badge2) }
    }

    def "can access badge endpoints with UC protection enabled when the user does belong to the user community"() {
        when:
        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def proj1 = createProject(1)
        proj1.enableProtectedUserCommunity = true
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)
        def badge1 = createBadge(1)
        badge1.enableProtectedUserCommunity = true
        skillsService.createGlobalBadge(badge1)
        skillsService.assignProjectLevelToGlobalBadge(projectId: proj1.projectId, badgeId: badge1.badgeId, level: "3")
        skillsService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: badge1.badgeId, skillId: proj1_skills.get(0).skillId)
        badge1.enabled  = 'true'
        skillsService.updateGlobalBadge(badge1, badge1.badgeId)
        def badge2 = createBadge(1, 2)
        badge2.enableProtectedUserCommunity = false
        ClassPathResource resource = new ClassPathResource("/dot2.png")
        def file = resource.getFile()

        def otherUserCommunityUserIds = getRandomUsers(2, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def otherUserCommunityUserId = otherUserCommunityUserIds[0]
        def otherUserCommunityUserId2 = otherUserCommunityUserIds[1]
        SkillsService otherUserCommunityUser = createService(otherUserCommunityUserId)
        createService(otherUserCommunityUserId2)
        rootSkillsService.saveUserTag(otherUserCommunityUserId, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(otherUserCommunityUserId2, 'dragons', ['DivineDragon'])
        skillsService.grantGlobalBadgeAdminRole(badge1.badgeId, otherUserCommunityUserId)
        skillsService.addProjectAdmin(proj1.projectId, otherUserCommunityUserId)

        then:
        // api endpoints
        !validateForbidden { otherUserCommunityUser.getCustomIconCssForGlobalBadge(badge1.badgeId) }
        !validateForbidden { otherUserCommunityUser.getBadgeSummary(null, proj1.projectId,  badge1.badgeId,-1, true) }
        !validateForbidden { otherUserCommunityUser.getBadgeDescriptions(proj1.projectId, badge1.badgeId, true) }

        // admin endpoints
        !validateForbidden { otherUserCommunityUser.getSkillDependencyInfo(otherUserCommunityUserId, proj1.projectId, badge1.badgeIId) }

        // global badge endpoints
        !validateForbidden { otherUserCommunityUser.updateGlobalBadge(badge1, badge1.badgeId) }
        !validateForbidden { otherUserCommunityUser.assignSkillToGlobalBadge(proj1.projectId, badge1.badgeId, proj1_skills.get(1).skillId) }
        !validateForbidden { otherUserCommunityUser.removeSkillFromGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, skillId: proj1_skills.get(0).skillId]) }
        !validateForbidden { otherUserCommunityUser.getGlobalBadge(badge1.badgeId) }
        !validateForbidden { otherUserCommunityUser.changeGlobalBadgeDisplayOrder(badge1, 0) }
        !validateForbidden { otherUserCommunityUser.getGlobalBadgeSkills(badge1.badgeId) }
        !validateForbidden { otherUserCommunityUser.getAvailableSkillsForGlobalBadge(badge1.badgeId, '') }
        !validateForbidden { otherUserCommunityUser.getAvailableProjectsForGlobalBadge(badge1.badgeId, '') }
        !validateForbidden { otherUserCommunityUser.removeProjectLevelFromGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, level: '3']) }
        !validateForbidden { otherUserCommunityUser.assignProjectLevelToGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, level: '1']) }
        !validateForbidden { otherUserCommunityUser.changeProjectLevelOnGlobalBadge([projectId: proj1.projectId, badgeId: badge1.badgeId, currentLevel: "1", newLevel: "2"]) }
        !validateForbidden { otherUserCommunityUser.getCustomIconsForBadge([badgeId: badge1.badgeId]) }
        !validateForbidden { otherUserCommunityUser.uploadGlobalIcon(badge1, file, true) }
        !validateForbidden { otherUserCommunityUser.deleteGlobalIcon([badgeId:(badge1.badgeId), filename: "dot2.png"]) }
        !validateForbidden { otherUserCommunityUser.grantGlobalBadgeAdminRole(badge1.badgeId, otherUserCommunityUserId2) }
        !validateForbidden { otherUserCommunityUser.getUserRolesForGlobalBadge(badge1.badgeId) }
        !validateForbidden { otherUserCommunityUser.removeGlobalBadgeAdminRole(badge1.badgeId, otherUserCommunityUserId2) }
        !validateForbidden { otherUserCommunityUser.getAdminGroupsForGlobalBadge(badge1.badgeId) }
        !validateForbidden { otherUserCommunityUser.validateGlobalBadgeForEnablingCommunity(badge1.badgeId) }
        !validateForbidden { otherUserCommunityUser.deleteGlobalBadge(badge1.badgeId) }

        // check if badgeId exists and create new badge are still allowed
        !validateForbidden { otherUserCommunityUser.doesGlobalBadgeIdExists(badge1.badgeId) }
        !validateForbidden { otherUserCommunityUser.createGlobalBadge(badge2) }
    }

    def "cannot add a global badge admin role to a community protected global badge if the user is not a member of that community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1)
        badge1.enableProtectedUserCommunity = true
        pristineDragonsUser.createGlobalBadge(badge1)

        when:
        pristineDragonsUser.grantGlobalBadgeAdminRole(badge1.badgeId, allDragonsUser.userName)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${allDragonsUser.userName} for display] is not allowed to be assigned [Admin] user role")
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

        def badge1 = createBadge(1)
        skillsService.createGlobalBadge(badge1)

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
        validateForbidden { nonUserCommunityUser.getAdminGroupGlobalBadges(adminGroup.adminGroupId) }
        validateForbidden { nonUserCommunityUser.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge1.badgeId) }
        validateForbidden { nonUserCommunityUser.deleteGlobalBadgeFromAdminGroup(adminGroup.adminGroupId, badge1.badgeId) }
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

        def badge1 = createBadge(1)
        skillsService.createGlobalBadge(badge1)

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
        !validateForbidden { skillsService.getAdminGroupGlobalBadges(adminGroup.adminGroupId) }
        !validateForbidden { skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge1.badgeId) }
        !validateForbidden { skillsService.deleteGlobalBadgeFromAdminGroup(adminGroup.adminGroupId, badge1.badgeId) }
        !validateForbidden { skillsService.updateAdminGroupDef(adminGroup) }
        !validateForbidden { skillsService.removeAdminGroupDef(adminGroup.adminGroupId) }
    }

    def "cannot add UC protected global badge to a non-UC admin group"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def badge1 = createBadge(1)
        badge1.enableProtectedUserCommunity = true
        skillsService.createGlobalBadge(badge1)

        when:

        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge1.badgeId)

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Global Badge [${badge1.name}] is not allowed to be assigned [${adminGroup.name}] Admin Group")
    }

    def "cannot enable UC protection on global badge if non-UC group is assigned to it already"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1)
        skillsService.createGlobalBadge(badge1)

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = false
        skillsService.createAdminGroupDef(adminGroup)

        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge1.badgeId)

        when:

        badge1.enableProtectedUserCommunity = true
        skillsService.updateGlobalBadge(badge1)

        then:

        true
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("This global badge is part of one or more Admin Groups that do no have Divine Dragon permission")
    }

    def "cannot enable UC protection on global badge if non-UC group is assigned to it already, multiple members in group"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def otherUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserCommunityUserId)

        def badge1 = createBadge(1)
        skillsService.createGlobalBadge(badge1)

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = false
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserCommunityUserId)

        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge1.badgeId)

        when:

        badge1.enableProtectedUserCommunity = true
        skillsService.updateGlobalBadge(badge1)

        then:

        true
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("This global badge is part of one or more Admin Groups that do no have Divine Dragon permission")
    }

    def "cannot enable UC protection for admin group if it contains a non UC global badge"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)

        def badge1 = createBadge(1)
        skillsService.createGlobalBadge(badge1)
        skillsService.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge1.badgeId)

        when:
        adminGroup.enableProtectedUserCommunity = true
        skillsService.updateAdminGroupDef(adminGroup)

        then:

        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Not Allowed to set [enableProtectedUserCommunity] to true for adminGroupId [${adminGroup.adminGroupId}]")
        e.message.contains("This Admin Group is connected to a global badge that is not compatible with user community protection")
    }

    def "cannot enable UC on a project that is linked to a non UC global badge via skill relationship"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        proj.enableProtectedUserCommunity = false
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def badge1 = createBadge(1)
        badge1.enableProtectedUserCommunity = false
        skillsService.createGlobalBadge(badge1)

        skillsService.assignSkillToGlobalBadge(proj.projectId, badge1.badgeId, skill.skillId)

        when:
        proj.enableProtectedUserCommunity = true
        skillsService.updateProject(proj)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Not Allowed to set [enableProtectedUserCommunity] to true")
        e.message.contains("This project is part of one or more Global Badges that has not enabled user community protection")
    }

    def "cannot enable UC on a project that is linked to a non UC global badge via level relationship"() {

        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skill = createSkill(1, 1)
        proj.enableProtectedUserCommunity = false
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])

        def badge1 = createBadge(1)
        badge1.enableProtectedUserCommunity = false
        skillsService.createGlobalBadge(badge1)

        skillsService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge1.badgeId, level: "1")

        when:
        proj.enableProtectedUserCommunity = true
        skillsService.updateProject(proj)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Not Allowed to set [enableProtectedUserCommunity] to true")
        e.message.contains("This project is part of one or more Global Badges that has not enabled user community protection")
    }

    private static boolean validateForbidden(Closure c) {
        try {
            c.call()
            return false
        } catch (SkillsClientException skillsClientException) {
            return skillsClientException.httpStatus == HttpStatus.FORBIDDEN
        } catch (Exception e) {
            return false
        }
    }
}

