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
import skills.intTests.utils.SkillsService
import skills.services.settings.Settings
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.SkillsFactory.createProject

class GetProjectsCommunitySpecs extends DefaultIntSpec {

    def "protected community project is only returned for users in that community, even if that user happened to assigned an admin"() {
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

        pristineDragonsUser.changeSettings(p1.projectId, [
                [projectId: p1.projectId, setting: Settings.USER_COMMUNITY_ONLY_PROJECT.settingName, value: "true"],
        ])
        when:
        def pristineDragonUserProjects = pristineDragonsUser.getProjects()
        def allDragonsUserProjects = allDragonsUser.getProjects()
        then:
        pristineDragonUserProjects.projectId == [p1.projectId, p2.projectId]
        pristineDragonUserProjects.userCommunity == ['Divine Dragon', 'All Dragons']
        allDragonsUserProjects.projectId == [p2.projectId]
        allDragonsUserProjects.userCommunity == [null]
    }

    def "get single project - community info is only returned for community members"() {
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

        pristineDragonsUser.changeSettings(p1.projectId, [
                [projectId: p1.projectId, setting: Settings.USER_COMMUNITY_ONLY_PROJECT.settingName, value: "true"],
        ])
        when:
        def pristineDragonUserP1 = pristineDragonsUser.getProject(p1.projectId)
        def pristineDragonUserP2 = pristineDragonsUser.getProject(p2.projectId)
        def allDragonsUserP2 = allDragonsUser.getProject(p2.projectId)
        then:
        pristineDragonUserP1.projectId == p1.projectId
        pristineDragonUserP1.userCommunity == 'Divine Dragon'

        pristineDragonUserP2.projectId == p2.projectId
        pristineDragonUserP2.userCommunity == 'All Dragons'

        allDragonsUserP2.projectId == p2.projectId
        allDragonsUserP2.userCommunity == null
    }

}
