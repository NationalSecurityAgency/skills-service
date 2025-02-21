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
import skills.services.settings.Settings
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.SkillsFactory.createProject

class ConfigureCommunityForProjectSpecs extends DefaultIntSpec {

    def "do not allow to enable community for a project through settings endpoint"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        pristineDragonsUser.createProject(p1)

        when:
        pristineDragonsUser.changeSettings(p1.projectId, [
                [projectId: p1.projectId, setting: Settings.USER_COMMUNITY_ONLY_PROJECT.settingName, value: "true"],
        ])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Not allowed to save [${Settings.USER_COMMUNITY_ONLY_PROJECT.settingName}] setting using this endpoint")
    }

    def "configure community when creating a project"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        def p2 = createProject(2)
        pristineDragonsUser.createProject(p2)

        when:
        def pristineDragonUserProjects = pristineDragonsUser.getProjects()
        then:
        pristineDragonUserProjects.projectId == [p1.projectId, p2.projectId]
        pristineDragonUserProjects.userCommunity == ['Divine Dragon', 'All Dragons']
    }

    def "only member of the community can enable project "() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        when:
        allDragonsUser.createProject(p1)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${allDragonsUser.userName}] is not allowed to set [enableProtectedUserCommunity] to true")
    }

    def "configure community when editing a project"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        pristineDragonsUser.createProject(p1)

        def p2 = createProject(2)
        pristineDragonsUser.createProject(p2)

        when:
        def pristineDragonUserProjects_before = pristineDragonsUser.getProjects()
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.updateProject(p1, p1.projectId)
        def pristineDragonUserProjects_after = pristineDragonsUser.getProjects()
        then:
        pristineDragonUserProjects_before.projectId == [p1.projectId, p2.projectId]
        pristineDragonUserProjects_before.userCommunity == ['All Dragons', 'All Dragons']

        pristineDragonUserProjects_after.projectId == [p1.projectId, p2.projectId]
        pristineDragonUserProjects_after.userCommunity == ['Divine Dragon', 'All Dragons']
    }

    def "once community is enabled it cannot be disabled"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)
        when:
        p1.enableProtectedUserCommunity = false
        pristineDragonsUser.updateProject(p1, p1.projectId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Once project [enableProtectedUserCommunity=true] it cannot be flipped to false")
        e.getMessage().contains("projectId:TestProject1")
    }

    def "cannot enable protected community for a project that has admin that does not belong to that community"() {
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

        p1.enableProtectedUserCommunity = true

        when:
        pristineDragonsUser.updateProject(p1, p1.projectId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Not Allowed to set [enableProtectedUserCommunity] to true")
        e.message.contains("This project has the user ${userAttrsRepo.findByUserIdIgnoreCase(allDragonsUser.userName).userIdForDisplay} who is not authorized")
    }

    def "cannot enable protected community for a project that has approver that does not belong to that community"() {
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

        p1.enableProtectedUserCommunity = true

        when:
        pristineDragonsUser.updateProject(p1, p1.projectId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Not Allowed to set [enableProtectedUserCommunity] to true")
        e.message.contains("This project has the user ${userAttrsRepo.findByUserIdIgnoreCase(allDragonsUser.userName).userIdForDisplay} who is not authorized")
    }

    def "run community specific paragraph validation for project's description - project creation with community"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        String notAllowedProtectedDesc = "has divinedragon"
        String notAllowedForNonProtectedDesc = "has jabberwocky"
        when:
        def protectedWithNotAllowedDesc  = createProject(1)
        protectedWithNotAllowedDesc.enableProtectedUserCommunity = true
        protectedWithNotAllowedDesc.description = notAllowedProtectedDesc

        def protectedWithAllowedDesc  = createProject(2)
        protectedWithAllowedDesc.enableProtectedUserCommunity = true
        protectedWithAllowedDesc.description = notAllowedForNonProtectedDesc

        def notProtectedWithNotAllowedDesc  = createProject(3)
        notProtectedWithNotAllowedDesc.description = notAllowedForNonProtectedDesc

        def notProtectedWithAllowedDesc  = createProject(4)
        notProtectedWithAllowedDesc.description = notAllowedProtectedDesc

        pristineDragonsUser.createProject(protectedWithAllowedDesc)
        pristineDragonsUser.createProject(notProtectedWithAllowedDesc)

        then:
        expectErrWithMsg ({pristineDragonsUser.createProject(protectedWithNotAllowedDesc) }, "May not contain divinedragon word")
        expectErrWithMsg ({pristineDragonsUser.createProject(notProtectedWithNotAllowedDesc) }, "paragraphs may not contain jabberwocky")
    }

    def expectErrWithMsg(Closure c, String msg) {
        boolean res = false
        try {
            c.call()
            res = false // should not get here
        } catch (SkillsClientException e) {
            res = e.message.contains(msg)
        }

        return res
    }

}
