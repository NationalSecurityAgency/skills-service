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
package skills.intTests.community.globalBadge

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService

import static skills.intTests.utils.AdminGroupDefFactory.createAdminGroup
import static skills.intTests.utils.SkillsFactory.*

class EnableCommunityForGlobalBadgeValidationSpecs extends DefaultIntSpec {

    def "validation endpoint - able to enable community"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1, 1)
        pristineDragonsUser.createGlobalBadge(badge1)

        when:
        def res = pristineDragonsUser.validateGlobalBadgeForEnablingCommunity(badge1.badgeId)

        then:
        res.isAllowed == true
        !res.unmetRequirements
    }

    def "cannot enable community if global badge has admin that doesn't belong to the community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1, 1)
        pristineDragonsUser.createGlobalBadge(badge1)

        pristineDragonsUser.grantGlobalBadgeAdminRole(badge1.badgeId, allDragonsUser.userName)

        when:
        badge1.enableProtectedUserCommunity = true
        pristineDragonsUser.updateGlobalBadge(badge1, badge1.badgeId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("This global badge has the user ${userAttrsRepo.findByUserIdIgnoreCase(allDragonsUser.userName).userIdForDisplay} who is not authorized")
    }

    def "validation endpoint - cannot enable community if global badge has admin that doesn't belong to the community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1, 1)
        pristineDragonsUser.createGlobalBadge(badge1)

        def badge2 = createBadge(1, 2)
        pristineDragonsUser.createGlobalBadge(badge2)

        pristineDragonsUser.grantGlobalBadgeAdminRole(badge1.badgeId, allDragonsUser.userName)

        when:
        assert pristineDragonsUser.validateGlobalBadgeForEnablingCommunity(badge2.badgeId).isAllowed == true
        def res = pristineDragonsUser.validateGlobalBadgeForEnablingCommunity(badge1.badgeId)
        then:
        res.isAllowed == false
        res.unmetRequirements == ["This global badge has the user ${userAttrsRepo.findByUserIdIgnoreCase(allDragonsUser.userName).userIdForDisplay} who is not authorized"]
    }

    def "cannot enable community if global badge has an admin group with an admin that doesn't belong to the community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1, 1)
        pristineDragonsUser.createGlobalBadge(badge1)

        def badge2 = createBadge(1, 2)
        pristineDragonsUser.createGlobalBadge(badge2)

        def adminGroup = createAdminGroup(1)
        pristineDragonsUser.createAdminGroupDef(adminGroup)
        pristineDragonsUser.addAdminGroupOwner(adminGroup.adminGroupId, allDragonsUser.userName)
        pristineDragonsUser.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge1.badgeId)

        when:
        badge1.enableProtectedUserCommunity = true
        pristineDragonsUser.updateGlobalBadge(badge1, badge1.badgeId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("This global badge is part of one or more Admin Groups that do no have Divine Dragon permission")
    }

    def "validation endpoint - cannot enable community if global badge has an admin group with an admin that doesn't belong to the community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1, 1)
        pristineDragonsUser.createGlobalBadge(badge1)

        def badge2 = createBadge(2, 2)
        pristineDragonsUser.createGlobalBadge(badge2)

        def adminGroup = createAdminGroup(1)
        pristineDragonsUser.createAdminGroupDef(adminGroup)
        pristineDragonsUser.addAdminGroupOwner(adminGroup.adminGroupId, allDragonsUser.userName)
        pristineDragonsUser.addGlobalBadgeToAdminGroup(adminGroup.adminGroupId, badge1.badgeId)

        when:
        assert pristineDragonsUser.validateGlobalBadgeForEnablingCommunity(badge2.badgeId).isAllowed == true
        def res = pristineDragonsUser.validateGlobalBadgeForEnablingCommunity(badge1.badgeId)
        then:
        res.isAllowed == false
        res.unmetRequirements.find { it == "This global badge is part of one or more Admin Groups that do no have Divine Dragon permission" }
    }

    def "cannot enable community if global badge associated to skills of a non-community project"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1, 1)
        pristineDragonsUser.createGlobalBadge(badge1)

        def badge2 = createBadge(1, 2)
        pristineDragonsUser.createGlobalBadge(badge2)

        def p1 = createProject(1)
        def p1Skill = createSkill(1, 1, 1, 1, 1, 480, 200)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, createSubject(1, 1), [p1Skill])
        pristineDragonsUser.assignSkillToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skill.skillId)

        when:
        badge1.enableProtectedUserCommunity = true
        pristineDragonsUser.updateGlobalBadge(badge1, badge1.badgeId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("This global badge is linked to the following project(s) that do not have Divine Dragon permission: ${p1.projectId}".toString())
    }

    def "validation endpoint - cannot enable community if global badge associated to skills of a non-community project"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1, 1)
        pristineDragonsUser.createGlobalBadge(badge1)

        def badge2 = createBadge(1, 2)
        pristineDragonsUser.createGlobalBadge(badge2)

        def p1 = createProject(1)
        def p1Skill = createSkill(1, 1, 1, 1, 1, 480, 200)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, createSubject(1, 1), [p1Skill])
        pristineDragonsUser.assignSkillToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skill.skillId)

        when:
        assert pristineDragonsUser.validateGlobalBadgeForEnablingCommunity(badge2.badgeId).isAllowed == true
        def res = pristineDragonsUser.validateGlobalBadgeForEnablingCommunity(badge1.badgeId)
        then:
        res.isAllowed == false
        res.unmetRequirements == ["This global badge is linked to the following project(s) that do not have Divine Dragon permission: ${p1.projectId}".toString()]
    }

    def "cannot enable community because number of requirements are not met"() {
        List<String> users = getRandomUsers(2)
        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1)
        pristineDragonsUser.createGlobalBadge(badge1)

        def badge2 = createBadge(1, 2)
        pristineDragonsUser.createGlobalBadge(badge2)

        pristineDragonsUser.grantGlobalBadgeAdminRole(badge1.badgeId, allDragonsUser.userName)

        def p1 = createProject(1)
        def p1Skill = createSkill(1, 1, 1, 1, 1, 480, 200)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, createSubject(1, 1), [p1Skill])
        pristineDragonsUser.assignSkillToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skill.skillId)

        when:
        badge1.enableProtectedUserCommunity = true
        pristineDragonsUser.updateGlobalBadge(badge1, badge1.badgeId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("This global badge has the user ${allDragonsUser.userName} for display who is not authorized".toString()) ||
                e.getMessage().contains("This global badge is linked to the following project(s) that do not have Divine Dragon permission: ${p1.projectId}")
    }

    def "validation endpoint - cannot enable community because number of requirements are not met"() {
        List<String> users = getRandomUsers(2)
        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def badge1 = createBadge(1)
        pristineDragonsUser.createGlobalBadge(badge1)

        def badge2 = createBadge(1, 2)
        pristineDragonsUser.createGlobalBadge(badge2)

        pristineDragonsUser.grantGlobalBadgeAdminRole(badge1.badgeId, allDragonsUser.userName)

        def p1 = createProject(1)
        def p1Skill = createSkill(1, 1, 1, 1, 1, 480, 200)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, createSubject(1, 1), [p1Skill])
        pristineDragonsUser.assignSkillToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skill.skillId)

        when:
        assert pristineDragonsUser.validateGlobalBadgeForEnablingCommunity(badge2.badgeId).isAllowed == true
        def res = pristineDragonsUser.validateGlobalBadgeForEnablingCommunity(badge1.badgeId)
        then:
        res.isAllowed == false
        res.unmetRequirements.sort() == [
                "This global badge has the user ${allDragonsUser.userName} for display who is not authorized".toString(),
                "This global badge is linked to the following project(s) that do not have Divine Dragon permission: ${p1.projectId}".toString()
        ].sort()
    }
}
