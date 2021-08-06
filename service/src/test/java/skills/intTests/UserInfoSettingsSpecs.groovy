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
package skills.intTests

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import spock.lang.IgnoreIf
import spock.lang.IgnoreRest

class UserInfoSettingsSpecs extends DefaultIntSpec {

    def currentUser

    def userProj1 = "up1"
    def userProj2 = "up2"
    def userProj3 = "up3"

    def setup() {
        currentUser = skillsService.getCurrentUser()
        skillsService.deleteProjectIfExist(userProj1)
        skillsService.deleteProjectIfExist(userProj2)
        skillsService.deleteProjectIfExist(userProj3)
    }

    def 'user can update their user info properties'() {

        String newFirstName = "newFirstName-${System.currentTimeMillis()}"
        String newLastName = "newLastName-${System.currentTimeMillis()}"
        String newNickname = "newNickname-${System.currentTimeMillis()}"

        // verify that the landing page setting is included in the userInfo
        currentUser.landingPage == 'progress'

        assert currentUser.first != newFirstName
        assert currentUser.last != newLastName
        assert currentUser.nickname != newNickname

        currentUser.first = newFirstName
        currentUser.last = newLastName
        currentUser.nickname = newNickname

        when:
        skillsService.updateUserInfo(currentUser)
        def updatedCurrentUser = skillsService.getCurrentUser()

        then:

        // cannot change first or last name in PKI mode
        if (System.getenv("SPRING_PROFILES_ACTIVE") != 'pki') {
            updatedCurrentUser.first == newFirstName
            updatedCurrentUser.last == newLastName
        }
        updatedCurrentUser.nickname == newNickname
    }

    def 'nickname is optional'() {

        if (!currentUser.nickname) {
            currentUser.nickname = 'nickname'
            skillsService.updateUserInfo(currentUser)
        }

        assert currentUser.nickname
        currentUser.nickname = ""

        when:
        skillsService.updateUserInfo(currentUser)
        def updatedCurrentUser = skillsService.getCurrentUser()

        then:

        updatedCurrentUser
        !updatedCurrentUser.nickname
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def 'user first name cannot exceed 30 characters'() {

        String nameOver30Chars = "1234567890123456789012345678901"
        assert currentUser.first && currentUser.first != nameOver30Chars
        currentUser.first = nameOver30Chars

        when:
        skillsService.updateUserInfo(currentUser)

        then:

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:First Name is required and can be no longer than 30 characters; provided firstName=[${nameOver30Chars}]")
        exception.message.contains("errorCode:BadParam")
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def 'user last name cannot exceed 30 characters'() {

        String nameOver30Chars = "1234567890123456789012345678901"
        assert currentUser.last && currentUser.last != nameOver30Chars
        currentUser.last = nameOver30Chars

        when:
        skillsService.updateUserInfo(currentUser)

        then:

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Last Name is required and can be no longer than 30 characters; provided lastName=[${nameOver30Chars}]")
        exception.message.contains("errorCode:BadParam")
    }

    def 'user nick cannot exceed 70 characters'() {
        String nameOver70Chars = (0..71).collect({"a"}).join("")
        assert currentUser.nickname != nameOver70Chars
        currentUser.nickname = nameOver70Chars

        when:
        skillsService.updateUserInfo(currentUser)

        then:

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Nickname cannot be over 70 characters; provided nickname=[${nameOver70Chars}]")
        exception.message.contains("errorCode:BadParam")
    }

    def 'project sort - move project up'(){
        skillsService.createProject([projectId: userProj1, name: 'proj1'])
        skillsService.createProject([projectId: userProj2, name: 'proj2'])
        skillsService.createProject([projectId: userProj3, name: 'proj3'])
        def result = skillsService.getProjects()

        when:
        def beforeMove = skillsService.getProjects()
        skillsService.changeProjectDisplayOrder([projectId: userProj3], 1)
        def afterMove = skillsService.getProjects()

        then:
        beforeMove.collect { it.projectId } == [userProj1, userProj2, userProj3]
        afterMove.collect { it.projectId } == [userProj1, userProj3, userProj2]
    }

    def 'project sort - move project down'(){
        skillsService.createProject([projectId: userProj1, name: 'proj1'])
        skillsService.createProject([projectId: userProj2, name: 'proj2'])
        skillsService.createProject([projectId: userProj3, name: 'proj3'])
        def beforeMove = skillsService.getProjects()

        when:
        skillsService.changeProjectDisplayOrder([projectId: userProj2], 2)

        def afterMove = skillsService.getProjects()

        skillsService.changeProjectDisplayOrder([projectId: userProj1], 1)
        skillsService.changeProjectDisplayOrder([projectId: userProj1], 2)
        def afterMove1 = skillsService.getProjects()

        then:
        beforeMove.collect({it.name}) == ['proj1', 'proj2', 'proj3']
        afterMove.collect({it.name}) == ['proj1', 'proj3', 'proj2']
        afterMove1.collect({it.name}) == ['proj3', 'proj2', 'proj1']
    }

    def "sequence of display order operations"() {
        skillsService.createProject([projectId: userProj1, name: 'proj1'])
        skillsService.createProject([projectId: userProj2, name: 'proj2'])
        skillsService.createProject([projectId: userProj3, name: 'proj3'])
        String userProj4 = "up4"
        String userProj5 = "up5"
        skillsService.createProject([projectId: userProj4, name: 'proj4'])
        skillsService.createProject([projectId: userProj5, name: 'proj5'])

        when:
        def beforeMove = skillsService.getProjects()
        skillsService.changeProjectDisplayOrder([projectId: userProj1], 3)
        def move1 = skillsService.getProjects()

        skillsService.changeProjectDisplayOrder([projectId: userProj5], 0)
        def move2 = skillsService.getProjects()

        skillsService.changeProjectDisplayOrder([projectId: userProj2], 4)
        def move3 = skillsService.getProjects()

        skillsService.changeProjectDisplayOrder([projectId: userProj3], 2)
        def move4 = skillsService.getProjects()
        then:
        beforeMove.collect({it.name}) == ["proj1", "proj2", "proj3", "proj4", "proj5"]
        move1.collect({it.name}) == ["proj2", "proj3", "proj4", "proj1", "proj5"]
        move2.collect({it.name}) == ["proj5", "proj2", "proj3", "proj4", "proj1"]
        move3.collect({it.name}) == ["proj5", "proj3", "proj4", "proj1", "proj2"]
        move4.collect({it.name}) == ["proj5", "proj4", "proj3", "proj1", "proj2"]
    }

    def "move subject to out of the max bound - should be placed last"() {
        skillsService.createProject([projectId: userProj1, name: 'proj1'])
        skillsService.createProject([projectId: userProj2, name: 'proj2'])
        skillsService.createProject([projectId: userProj3, name: 'proj3'])
        String userProj4 = "up4"
        String userProj5 = "up5"
        skillsService.createProject([projectId: userProj4, name: 'proj4'])
        skillsService.createProject([projectId: userProj5, name: 'proj5'])


        when:
        def beforeMove = skillsService.getProjects()
        skillsService.changeProjectDisplayOrder([projectId: userProj1], 10)
        def afterMove = skillsService.getProjects()
        then:
        beforeMove.collect({it.name}) == ["proj1", "proj2", "proj3", "proj4", "proj5"]
        afterMove.collect({it.name}) == ["proj2", "proj3", "proj4", "proj5", "proj1"]
    }

    def "new display index must be >=0 "() {
        skillsService.createProject([projectId: userProj1, name: 'proj1'])
        skillsService.createProject([projectId: userProj2, name: 'proj2'])
        skillsService.createProject([projectId: userProj3, name: 'proj3'])
        String userProj4 = "up4"
        String userProj5 = "up5"
        skillsService.createProject([projectId: userProj4, name: 'proj4'])
        skillsService.createProject([projectId: userProj5, name: 'proj5'])

        when:
        skillsService.changeProjectDisplayOrder([projectId: userProj3], 0)
        def afterMove = skillsService.getProjects()
        skillsService.changeProjectDisplayOrder([projectId: userProj3], -1)
        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('[newDisplayOrderIndex] param must be >=0 but received [-1]')

        afterMove.collect({it.name}) == ["proj3", "proj1", "proj2", "proj4", "proj5"]

    }

    def 'validate project sort is per user'(){
        SkillsService user1Service = createService('user1')
        SkillsService user2Service = createService('user2')

        user1Service.createProject([projectId: userProj1, name: 'proj1'])
        user1Service.createProject([projectId: userProj2, name: 'proj2'])
        user1Service.createProject([projectId: userProj3, name: 'proj3'])

        user1Service.addUserRole('user2', userProj1, 'ROLE_PROJECT_ADMIN')
        user1Service.addUserRole('user2', userProj2, 'ROLE_PROJECT_ADMIN')
        user1Service.addUserRole('user2', userProj3, 'ROLE_PROJECT_ADMIN')

        def u1Results = user1Service.getProjects()
        def u2Results = user2Service.getProjects()

        when:
        user1Service.changeProjectDisplayOrder([projectId: userProj2], 2)
        user2Service.changeProjectDisplayOrder([projectId: userProj2], 0)

        def projectsU1 = user1Service.getProjects()
        def projectsU2 = user2Service.getProjects()

        user1Service.deleteAllMyProjects()
        user2Service.deleteAllMyProjects()

        then:
        u1Results.collect { it.projectId } == [userProj1, userProj2, userProj3]
        projectsU1.collect { it.projectId } == [userProj1, userProj3, userProj2]

        u2Results.collect { it.projectId } == [userProj1, userProj2, userProj3]
        projectsU2.collect { it.projectId } == [userProj2, userProj1, userProj3]
    }
}
