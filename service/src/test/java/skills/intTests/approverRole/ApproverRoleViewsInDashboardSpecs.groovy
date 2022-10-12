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


import skills.intTests.utils.DefaultIntSpec
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

    def "approver role can only see approval history that were approved by that approver"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(10)
        Date date = new Date() - 60

        def approverRoleUser1 = createService(users[0].toString())
        skillsService.addUserRole(approverRoleUser1.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def approverRoleUser2 = createService(users[1].toString())
        skillsService.addUserRole(approverRoleUser2.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        (2..7).each {
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[it], date, "Please approve this!")
            assert res.body.explanation == "Skill was submitted for approval"
        }
        def approvals_t0 = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        approverRoleUser1.approve(proj.projectId, approvals_t0.data.findAll{ it.userId == users[2] }.collect { it.id })
        approverRoleUser1.rejectSkillApprovals(proj.projectId, approvals_t0.data.findAll{ it.userId == users[3] }.collect { it.id })
        approverRoleUser2.approve(proj.projectId, approvals_t0.data.findAll{ it.userId == users[4] }.collect { it.id })
        approverRoleUser2.rejectSkillApprovals(proj.projectId, approvals_t0.data.findAll{ it.userId == users[5] }.collect { it.id })
        approverRoleUser2.approve(proj.projectId, approvals_t0.data.findAll{ it.userId == users[6] }.collect { it.id })
        approverRoleUser2.approve(proj.projectId, approvals_t0.data.findAll{ it.userId == users[7] }.collect { it.id })

        def approvals_t1 = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvalHist_user1 = approverRoleUser1.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false)
        def approvalHist_user2 = approverRoleUser2.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false)
        def approvalHist_admin = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvalHist_admin.data.collect { it.userId }.sort() == (2..7).collect {users[it]}.sort()
        approvalHist_user1.data.collect { it.userId }.sort() == [users[2], users[3]].sort()
        approvalHist_user2.data.collect { it.userId }.sort() == [users[4], users[5], users[6], users[7]].sort()

        !approvals_t1.data
    }


}
