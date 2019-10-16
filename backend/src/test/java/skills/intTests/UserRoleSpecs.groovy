package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class UserRoleSpecs extends DefaultIntSpec {

    static String projAdminRole = "ROLE_PROJECT_ADMIN"

    def "get user roles for a project" () {
        String user = "UserRoleSpecsUser1"
        String user2 = "UserRoleSpecsUser2"
        SkillsService user1Seervice = createService(user, "passefeafeaef", "John", "Smith")
        createService(user2, "passefeafeaef", "Bob", "Cool")

        def proj = SkillsFactory.createProject(1)
        user1Seervice.createProject(proj)
        when:
        def res = user1Seervice.getUserRolesForProject(proj.projectId)
        user1Seervice.addUserRole(user2, proj.projectId, projAdminRole)
        def res2 = user1Seervice.getUserRolesForProject(proj.projectId).sort { it.userId }
        then:
        res.size() == 1
        res.get(0).userId == user.toLowerCase()
        res.get(0).userIdForDisplay == user
        res.get(0).firstName == "John"
        res.get(0).lastName == "Smith"
        res.get(0).projectId == proj.projectId
        res.get(0).roleName == projAdminRole

        res2.size() == 2
        res2.get(0).userId == user.toLowerCase()
        res2.get(0).userIdForDisplay == user
        res2.get(0).firstName == "John"
        res2.get(0).lastName == "Smith"
        res2.get(0).projectId == proj.projectId
        res2.get(0).roleName == projAdminRole

        res2.get(1).userId == user2.toLowerCase()
        res2.get(1).userIdForDisplay == user2
        res2.get(1).firstName == "Bob"
        res2.get(1).lastName == "Cool"
        res2.get(1).projectId == proj.projectId
        res2.get(1).roleName == projAdminRole
    }

    def "get user roles for a project and user" () {
        String user = "UserRoleSpecsUser3"
        SkillsService user1Service = createService(user, "passefeafeaef", "John", "Smith")
        def proj = SkillsFactory.createProject(1)
        user1Service.createProject(proj)
        when:
        def res = user1Service.getUserRolesForProjectAndUser(proj.projectId, user)
        then:
        res.size() == 1
        res.get(0).userId == user.toLowerCase()
        res.get(0).userIdForDisplay == user
        res.get(0).firstName == "John"
        res.get(0).lastName == "Smith"
        res.get(0).projectId == "TestProject1"
        res.get(0).roleName == projAdminRole
    }
}
