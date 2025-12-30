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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import spock.lang.IgnoreIf

import static skills.intTests.utils.AdminGroupDefFactory.createAdminGroup

class UserRoleSpecs extends DefaultIntSpec {

    static String projAdminRole = RoleName.ROLE_PROJECT_ADMIN.toString()

    def "get user roles for a project" () {
        String user = "UserRoleSpecsUser1"
        String user2 = "UserRoleSpecsUser2"
        SkillsService user1Seervice = createService(user, "passefeafeaef", "John", "Smith")
        createService(user2, "passefeafeaef", "Bob", "Cool")

        def proj = SkillsFactory.createProject(1)
        user1Seervice.createProject(proj)
        when:
        def res = user1Seervice.getUserRolesForProject(proj.projectId, [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER])
        user1Seervice.addUserRole(user2, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def res2 = user1Seervice.getUserRolesForProject(proj.projectId, [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER])
        then:
        res.count == 1
        res.data.size() == 1
        res.data.get(0).userId.contains(user.toLowerCase())
        res.data.get(0).userIdForDisplay.contains(user)
        res.data.get(0).firstName == "John"
        res.data.get(0).lastName == "Smith"
        res.data.get(0).projectId == proj.projectId
        res.data.get(0).roleName == projAdminRole

        res2.count == 2
        res2.data.size() == 2
        res2.data.get(0).userId.contains(user.toLowerCase())
        res2.data.get(0).userIdForDisplay.equalsIgnoreCase("$user for display")
        res2.data.get(0).firstName == "John"
        res2.data.get(0).lastName == "Smith"
        res2.data.get(0).projectId == proj.projectId
        res2.data.get(0).roleName == projAdminRole

        res2.data.get(1).userId == user2.toLowerCase()
        res2.data.get(1).userIdForDisplay.equalsIgnoreCase("$user2 for display")
        res2.data.get(1).firstName == "Bob"
        res2.data.get(1).lastName == "Cool"
        res2.data.get(1).projectId == proj.projectId
        res2.data.get(1).roleName == RoleName.ROLE_PROJECT_APPROVER.toString()
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "can sort by userIdForDisplay" () {
        String user = "jsmith"
        String user2 = "bcool"
        String user3 = "aaaronson"
        String user4 = "tjohnson"
        String user5 = "xjones"
        SkillsService user1Service = createService(user, "passefeafeaef", "John", "Smith")
        createService(user2, "passefeafeaef", "Bob", "Cool")
        createService(user3, "passefeafeaef", "Aaron", "Aaronson")
        createService(user4, "passefeafeaef", "Theodore", "Johnson")
        createService(user5, "passefeafeaef", "Xander", "Jones")

        def proj = SkillsFactory.createProject(1)
        user1Service.createProject(proj)
        when:
        user1Service.addUserRole(user2, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        user1Service.addUserRole(user3, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        user1Service.addUserRole(user4, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        user1Service.addUserRole(user5, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def res = user1Service.getUserRolesForProject(proj.projectId, [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER], 10, 1, "userIdForDisplay", true)
        def res2 = user1Service.getUserRolesForProject(proj.projectId, [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER], 10, 1, "userIdForDisplay", false)
        then:

        res.count == 5
        res.data.size() == 5
        res.data.get(0).userId.contains(user3.toLowerCase())
        res.data.get(0).userIdForDisplay.equalsIgnoreCase("$user3 for display")
        res.data.get(0).firstName == "Aaron"
        res.data.get(0).lastName == "Aaronson"
        res.data.get(0).projectId == proj.projectId
        res.data.get(0).roleName == RoleName.ROLE_PROJECT_APPROVER.toString()

        res.data.get(1).userId == user2.toLowerCase()
        res.data.get(1).userIdForDisplay.equalsIgnoreCase("$user2 for display")
        res.data.get(1).firstName == "Bob"
        res.data.get(1).lastName == "Cool"
        res.data.get(1).projectId == proj.projectId
        res.data.get(1).roleName == RoleName.ROLE_PROJECT_APPROVER.toString()

        res.data.get(2).userId == user.toLowerCase()
        res.data.get(2).userIdForDisplay.equalsIgnoreCase("$user for display")
        res.data.get(2).firstName == "John"
        res.data.get(2).lastName == "Smith"
        res.data.get(2).projectId == proj.projectId
        res.data.get(2).roleName == projAdminRole

        res.data.get(3).userId == user4.toLowerCase()
        res.data.get(3).userIdForDisplay.equalsIgnoreCase("$user4 for display")
        res.data.get(3).firstName == "Theodore"
        res.data.get(3).lastName == "Johnson"
        res.data.get(3).projectId == proj.projectId
        res.data.get(3).roleName == RoleName.ROLE_PROJECT_APPROVER.toString()

        res.data.get(4).userId == user5.toLowerCase()
        res.data.get(4).userIdForDisplay.equalsIgnoreCase("$user5 for display")
        res.data.get(4).firstName == "Xander"
        res.data.get(4).lastName == "Jones"
        res.data.get(4).projectId == proj.projectId
        res.data.get(4).roleName == RoleName.ROLE_PROJECT_APPROVER.toString()

        res2.count == 5
        res2.data.size() == 5
        res2.data.get(4).userId.contains(user3.toLowerCase())
        res2.data.get(4).userIdForDisplay.equalsIgnoreCase("$user3 for display")
        res2.data.get(4).firstName == "Aaron"
        res2.data.get(4).lastName == "Aaronson"
        res2.data.get(4).projectId == proj.projectId
        res2.data.get(4).roleName == RoleName.ROLE_PROJECT_APPROVER.toString()

        res2.data.get(3).userId == user2.toLowerCase()
        res2.data.get(3).userIdForDisplay.equalsIgnoreCase("$user2 for display")
        res2.data.get(3).firstName == "Bob"
        res2.data.get(3).lastName == "Cool"
        res2.data.get(3).projectId == proj.projectId
        res2.data.get(3).roleName == RoleName.ROLE_PROJECT_APPROVER.toString()

        res2.data.get(2).userId == user.toLowerCase()
        res2.data.get(2).userIdForDisplay.equalsIgnoreCase("$user for display")
        res2.data.get(2).firstName == "John"
        res2.data.get(2).lastName == "Smith"
        res2.data.get(2).projectId == proj.projectId
        res2.data.get(2).roleName == projAdminRole

        res2.data.get(1).userId == user4.toLowerCase()
        res2.data.get(1).userIdForDisplay.equalsIgnoreCase("$user4 for display")
        res2.data.get(1).firstName == "Theodore"
        res2.data.get(1).lastName == "Johnson"
        res2.data.get(1).projectId == proj.projectId
        res2.data.get(1).roleName == RoleName.ROLE_PROJECT_APPROVER.toString()

        res2.data.get(0).userId == user5.toLowerCase()
        res2.data.get(0).userIdForDisplay.equalsIgnoreCase("$user5 for display")
        res2.data.get(0).firstName == "Xander"
        res2.data.get(0).lastName == "Jones"
        res2.data.get(0).projectId == proj.projectId
        res2.data.get(0).roleName == RoleName.ROLE_PROJECT_APPROVER.toString()
    }

    def "project admin and approver roles are mutually exclusive" () {
        String user = "UserRoleSpecsUser1".toLowerCase()
        String user2 = "UserRoleSpecsUser2".toLowerCase()
        SkillsService user1Seervice = createService(user, "passefeafeaef", "John", "Smith")
        createService(user2, "passefeafeaef", "Bob", "Cool")

        def proj = SkillsFactory.createProject(1)
        user1Seervice.createProject(proj)
        user1Seervice.addUserRole(user2, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        when:
        def res_t0 = user1Seervice.getUserRolesForProject(proj.projectId, [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER])
        user1Seervice.addUserRole(user2, proj.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())
        def res_t1 = user1Seervice.getUserRolesForProject(proj.projectId, [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER])
        then:
        res_t0.count == 2
        res_t0.data.size() == 2
        res_t0.data.find { it.userId == user2 }.roleName == RoleName.ROLE_PROJECT_APPROVER.toString()

        res_t1.count == 2
        res_t1.data.size() == 2
        res_t1.data.find { it.userId == user2 }.roleName == RoleName.ROLE_PROJECT_ADMIN.toString()
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
        res.get(0).userId.equalsIgnoreCase(user)
        res.get(0).userIdForDisplay.equalsIgnoreCase("$user for display")
        res.get(0).firstName == "John"
        res.get(0).lastName == "Smith"
        res.get(0).projectId == "TestProject1"
        res.get(0).roleName == projAdminRole
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] != "pki" })
    def "dn is returned in pki mode" () {
        String user = getRandomUsers(1)[0]
        SkillsService user1Service = createService(user, "passefeafeaef", "John", "Smith")
        def proj = SkillsFactory.createProject(1)
        user1Service.createProject(proj)
        when:
        def res = user1Service.getUserRolesForProjectAndUser(proj.projectId, user)

        UserAttrs expectedAttrs = userAttrsRepo.findByUserIdIgnoreCase(user)
        then:
        expectedAttrs.dn
        res.size() == 1
        res.get(0).dn == expectedAttrs.dn
        res.get(0).userId == expectedAttrs.userId
        res.get(0).userIdForDisplay == expectedAttrs.userIdForDisplay
        res.get(0).firstName == expectedAttrs.firstName
        res.get(0).lastName == expectedAttrs.lastName
        res.get(0).projectId == "TestProject1"
        res.get(0).roleName == projAdminRole
    }

    def "cannot add user that belongs to admin group as local admin for this project"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)

        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(otherUserId)
        createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        when:
        skillsService.addUserRole(otherUserId, proj.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("User is already part of an Admin Group and cannot be added as a local admin")
    }

    def "cannot add user that belongs to admin group as local approver for this project"() {
        def proj = SkillsFactory.createProject(1)
        skillsService.createProject(proj)

        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(otherUserId)
        createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)
        skillsService.addProjectToAdminGroup(adminGroup.adminGroupId, proj.projectId)

        when:
        skillsService.addUserRole(otherUserId, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("User is already part of an Admin Group and cannot be added as a local admin")
    }
}
