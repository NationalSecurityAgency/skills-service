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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.AdminGroupDefFactory.createAdminGroup
import static skills.intTests.utils.SkillsFactory.*

class EnableCommunityValidationSpecs extends DefaultIntSpec {

    def "validation endpoint - able to enable community"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        when:
        def res = pristineDragonsUser.validateProjectForEnablingCommunity(p1.projectId)
        then:
        res.isAllowed == true
        !res.unmetRequirements
    }

    def "validation endpoint - cannot enable community for a project if project has exported skills"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        pristineDragonsUser.exportSkillToCatalog(p1.projectId, p1Skills[0].skillId)

        when:
        def res = pristineDragonsUser.validateProjectForEnablingCommunity(p1.projectId)
        then:
        res.isAllowed == false
        res.unmetRequirements == ["Has skill(s) that have been exported to the Skills Catalog"]
    }

    def "validation endpoint - cannot enable community if project has admin that doesn't belong to the community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        allDragonsUser.createProject(p1)

        def p2 = createProject(2)
        allDragonsUser.createProject(p2)

        allDragonsUser.addUserRole(pristineDragonsUser.userName, p1.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())
        allDragonsUser.addUserRole(pristineDragonsUser.userName, p2.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())

        when:
        def res = pristineDragonsUser.validateProjectForEnablingCommunity(p1.projectId)
        then:
        res.isAllowed == false
        res.unmetRequirements == ["This project has the user ${userAttrsRepo.findByUserIdIgnoreCase(allDragonsUser.userName).userIdForDisplay} who is not authorized"]

    }

    def "validation endpoint - cannot enable community if project has approver that doesn't belong to the community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        pristineDragonsUser.createProject(p1)

        def p2 = createProject(2)
        pristineDragonsUser.createProject(p2)

        pristineDragonsUser.addUserRole(allDragonsUser.userName, p1.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        pristineDragonsUser.addUserRole(allDragonsUser.userName, p2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        def res = pristineDragonsUser.validateProjectForEnablingCommunity(p1.projectId)
        then:
        res.isAllowed == false
        res.unmetRequirements == ["This project has the user ${userAttrsRepo.findByUserIdIgnoreCase(allDragonsUser.userName).userIdForDisplay} who is not authorized"]
    }

    def "validation endpoint - cannot enable community because number of requirements are not met"() {
        List<String> users = getRandomUsers(3)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService allDragonsUser1 = createService(users[2])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        pristineDragonsUser.exportSkillToCatalog(p1.projectId, p1Skills[0].skillId)

        pristineDragonsUser.addUserRole(allDragonsUser.userName, p1.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())
        pristineDragonsUser.addUserRole(allDragonsUser1.userName, p1.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        def res = pristineDragonsUser.validateProjectForEnablingCommunity(p1.projectId)
        then:
        res.isAllowed == false
        res.unmetRequirements.sort() == [
                "This project has the user ${userAttrsRepo.findByUserIdIgnoreCase(allDragonsUser.userName).userIdForDisplay} who is not authorized",
                "This project has the user ${userAttrsRepo.findByUserIdIgnoreCase(allDragonsUser1.userName).userIdForDisplay} who is not authorized",
                "Has skill(s) that have been exported to the Skills Catalog"
        ].sort()
    }

    def "validation endpoint - projects with a protected community are not allowed to share skills for dependencies"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(3, 2, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        pristineDragonsUser.shareSkill(p1.projectId, p1Skills[0].skillId, p2.projectId)

        when:
        def res = pristineDragonsUser.validateProjectForEnablingCommunity(p1.projectId)
        then:
        res.isAllowed == false
        res.unmetRequirements == ["Has skill(s) that have been shared for cross-project dependencies"]
    }

    def "validation endpoint - projects with a protected community are not allowed to be part of the global badge"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(3, 2, 1, 100, 5)
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, p2subj1, p2Skills)

        def badge1 = SkillsFactory.createBadge(1)
        def badge2 = SkillsFactory.createBadge(2)
        rootUser.createGlobalBadge(badge1)
        rootUser.assignSkillToGlobalBadge(projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId)

        rootUser.createGlobalBadge(badge2)
        rootUser.assignProjectLevelToGlobalBadge(projectId: p2.projectId, badgeId: badge2.badgeId, level: "1")

        when:
        def res = pristineDragonsUser.validateProjectForEnablingCommunity(p1.projectId)
        def res1 = pristineDragonsUser.validateProjectForEnablingCommunity(p2.projectId)
        then:
        res.isAllowed == false
        res.unmetRequirements == ["This project is part of one or more Global Badges"]

        res1.isAllowed == false
        res1.unmetRequirements == ["This project is part of one or more Global Badges"]
    }

    def "cannot enable UC protection on admin group if it contains a non-UC member"() {
        SkillsService rootUser = createRootSkillService()
        String userCommunityUserId =  skillsService.userName
        rootUser.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = false
        rootUser.createAdminGroupDef(adminGroup)

        when:

        def res = rootUser.validateAdminGroupForEnablingCommunity(adminGroup.adminGroupId)

        then:
        res.isAllowed == false
        res.unmetRequirements == ["This admin group has the user rootUser for display who is not authorized"]
    }


    def "cannot enable UC protection on project if non-UC group is assigned to it already"() {
        SkillsService rootUser = createRootSkillService()
        String userCommunityUserId =  skillsService.userName
        rootUser.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

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
        def res = skillsService.validateProjectForEnablingCommunity(proj.projectId)

        then:
        res.isAllowed == false
        res.unmetRequirements == ["This project is part of one or more Admin Groups that has not enabled user community protection"]
    }

    def "cannot enable UC protection on project if non-UC group is assigned to it already, multiple members in group"() {
        SkillsService rootUser = createRootSkillService()
        String userCommunityUserId =  skillsService.userName
        rootUser.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon'])

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
        def res = skillsService.validateProjectForEnablingCommunity(proj.projectId)

        then:
        res.isAllowed == false
        res.unmetRequirements == ["This project has the user user3 for display who is not authorized", "This project is part of one or more Admin Groups that has not enabled user community protection"]
    }
}
