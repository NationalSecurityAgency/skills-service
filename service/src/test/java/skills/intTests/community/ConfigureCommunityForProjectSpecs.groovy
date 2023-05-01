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

        def p1 = createProject(1, true)
        pristineDragonsUser.createProject(p1)

        def p2 = createProject(2, false)
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

        def p1 = createProject(1, true)
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

        def p1 = createProject(1, false)
        pristineDragonsUser.createProject(p1)

        def p2 = createProject(2, false)
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

        def p1 = createProject(1, true)
        pristineDragonsUser.createProject(p1)
        when:
        p1.enableProtectedUserCommunity = false
        pristineDragonsUser.updateProject(p1, p1.projectId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Once project [enableProtectedUserCommunity=true] it cannot be flipped to false")
        e.getMessage().contains("projectId:TestProject1")

    }
}
