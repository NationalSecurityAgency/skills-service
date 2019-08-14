package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import spock.lang.Specification

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

        assert currentUser.first != newFirstName
        assert currentUser.last != newLastName
        assert currentUser.nickname != newNickname

        currentUser.first = newFirstName
        currentUser.last = newLastName
        currentUser.nickname = newNickname

        when:
        skillsService.updateUserInfo(currentUser)

        then:

        currentUser.first == newFirstName
        currentUser.last == newLastName
        currentUser.nickname == newNickname
    }

    def 'nickname is optional'() {

        if (!currentUser.nickname) {
            currentUser.nickname = 'nickname'
            skillsService.updateUserInfo(currentUser)
        }

        assert currentUser.nickname
        currentUser.nickname = null

        when:
        skillsService.updateUserInfo(currentUser)

        then:

        currentUser
        !currentUser.nickname
    }

    def 'user first name cannot exceed 30 characters'() {

        String nameOver30Chars = "1234567890123456789012345678901"
        assert currentUser.first && currentUser.first != nameOver30Chars
        currentUser.first = nameOver30Chars

        when:
        skillsService.updateUserInfo(currentUser)

        then:

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:First Name is required and can be no longer than 30 characters")
        exception.message.contains("errorCode:BadParam")
    }

    def 'user last name cannot exceed 30 characters'() {

        String nameOver30Chars = "1234567890123456789012345678901"
        assert currentUser.last && currentUser.last != nameOver30Chars
        currentUser.last = nameOver30Chars

        when:
        skillsService.updateUserInfo(currentUser)

        then:

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Last Name is required and can be no longer than 30 characters")
        exception.message.contains("errorCode:BadParam")
    }

    def 'user nick cannot exceed 30 characters'() {

        String nameOver30Chars = "1234567890123456789012345678901"
        assert currentUser.nickname != nameOver30Chars
        currentUser.nickname = nameOver30Chars

        when:
        skillsService.updateUserInfo(currentUser)

        then:

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Nickname cannot be over 30 characters")
        exception.message.contains("errorCode:BadParam")
    }

    def 'project sort - move project up'(){
        skillsService.createProject([projectId: userProj1, name: 'proj1'])
        skillsService.createProject([projectId: userProj2, name: 'proj2'])
        skillsService.createProject([projectId: userProj3, name: 'proj3'])
        def result = skillsService.getProjects()

        def preMoveProj2 = result.find{ it.projectId == userProj2 }
        def preMoveProj3 = result.find{ it.projectId == userProj3 }

        when:
        skillsService.moveProjectUp([projectId: userProj3])
        def projects = skillsService.getProjects()
        def postMoveProj2 = projects.find{ it.projectId == userProj2 }
        def postMoveProj3 = projects.find{ it.projectId == userProj3 }

        then:
        postMoveProj3.displayOrder == preMoveProj2.displayOrder
        postMoveProj2.displayOrder == preMoveProj3.displayOrder
    }

    def 'project sort - move project down'(){
        skillsService.createProject([projectId: userProj1, name: 'proj1'])
        skillsService.createProject([projectId: userProj2, name: 'proj2'])
        skillsService.createProject([projectId: userProj3, name: 'proj3'])
        def result = skillsService.getProjects()

        def preMoveProj2 = result.find{ it.projectId == userProj2 }
        def preMoveProj3 = result.find{ it.projectId == userProj3 }

        when:
        skillsService.moveProjectDown([projectId: userProj2])
        def projects = skillsService.getProjects()
        def postMoveProj2 = projects.find{ it.projectId == userProj2 }
        def postMoveProj3 = projects.find{ it.projectId == userProj3 }

        then:
        postMoveProj2.displayOrder == preMoveProj3.displayOrder
        postMoveProj3.displayOrder == preMoveProj2.displayOrder
    }
}
