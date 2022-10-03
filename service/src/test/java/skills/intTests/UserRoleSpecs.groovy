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
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName

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
}
