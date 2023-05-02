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

import static skills.intTests.utils.SkillsFactory.createProject

class GetProjectsCommunitySpecs extends DefaultIntSpec {

    def "get single project - community info is only returned for community members"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        def p2 = createProject(2)
        allDragonsUser.createProject(p2)

        allDragonsUser.addUserRole(pristineDragonsUser.userName, p2.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())

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

    def "user community project is not included in availableForMyProjects when user is not a member of the UC"() {
        List<String> users = getRandomUsers(2)
        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..3).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj, numProj == 3)  // 3rd project has user community protection enabled
            pristineDragonsUser.createProject(proj)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            return proj;
        }
        // 2nd project is NOT in the production mode, 3rd project has user community protection enabled
        pristineDragonsUser.enableProdMode(projs[0])
        pristineDragonsUser.enableProdMode(projs[2])

        when:
        def forMyProjects = allDragonsUser.getAvailableMyProjects()

        then:
        forMyProjects.size() == 1

        forMyProjects[0].projectId == projs[0].projectId
        forMyProjects[0].name == projs[0].name
        forMyProjects[0].totalPoints == 10
        forMyProjects[0].numSubjects == 1
        forMyProjects[0].numSkills == 1
        forMyProjects[0].numBadges == 3
        !forMyProjects[0].isMyProject
    }

    def "user community project is included in availableForMyProjects when user is a member of the UC"() {
        List<String> users = getRandomUsers(2)
        SkillsService otherPristineDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(otherPristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..3).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj, numProj == 3)  // 3rd project has user community protection enabled
            pristineDragonsUser.createProject(proj)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            return proj;
        }
        // 2nd project is NOT in the production mode
        pristineDragonsUser.enableProdMode(projs[0])
        pristineDragonsUser.enableProdMode(projs[2])

        when:
        def forMyProjects = otherPristineDragonsUser.getAvailableMyProjects()

        then:
        forMyProjects.size() == 2

        forMyProjects[0].projectId == projs[0].projectId
        forMyProjects[0].name == projs[0].name
        forMyProjects[0].totalPoints == 10
        forMyProjects[0].numSubjects == 1
        forMyProjects[0].numSkills == 1
        forMyProjects[0].numBadges == 3
        !forMyProjects[0].isMyProject

        forMyProjects[1].projectId == projs[2].projectId
        forMyProjects[1].name == projs[2].name
        forMyProjects[1].totalPoints == 30
        forMyProjects[1].numSubjects == 3
        forMyProjects[1].numSkills == 3
        forMyProjects[1].numBadges == 1
        !forMyProjects[1].isMyProject
    }
}
