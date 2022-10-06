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
package skills.intTests.approverRole

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName

class ApproverRoleViewsInDashboardSpecs extends DefaultIntSpec {

    def "get projects should return all projects where user is an approver or admin"() {
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        def user1Service = createService(getRandomUsers(1, true)[0])

        def proj2 = SkillsFactory.createProject(2)
        user1Service.createProject(proj2)
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        def projects = user1Service.getProjects()

        then:
        projects[0].projectId == proj2.projectId
        projects[0].userRole == RoleName.ROLE_PROJECT_ADMIN.toString()

        projects[1].projectId == proj.projectId
        projects[1].userRole == RoleName.ROLE_PROJECT_APPROVER.toString()
    }


}
