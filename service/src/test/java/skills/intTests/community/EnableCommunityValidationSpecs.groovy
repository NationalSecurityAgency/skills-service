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
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName

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
        res.unmetRequirements == ["Has existing ${userAttrsRepo.findByUserId(allDragonsUser.userName).userIdForDisplay} user that is not authorized"]

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
        res.unmetRequirements == ["Has existing ${userAttrsRepo.findByUserId(allDragonsUser.userName).userIdForDisplay} user that is not authorized"]
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
                "Has existing ${userAttrsRepo.findByUserId(allDragonsUser.userName).userIdForDisplay} user that is not authorized",
                "Has existing ${userAttrsRepo.findByUserId(allDragonsUser1.userName).userIdForDisplay} user that is not authorized",
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
}
