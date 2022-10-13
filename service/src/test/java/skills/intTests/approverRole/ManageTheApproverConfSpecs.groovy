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
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName

class ManageTheApproverConfSpecs extends DefaultIntSpec {

   def "assign approvers to user"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[3], new Date(), "Please approve this!")

        when:
        def approvals_t0 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, users[2])
        skillsService.configureApproverForUser(proj.projectId, user2Service.userName, users[2])
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t1.count == 1
        approvals_t1.data.userId == [users[2]]

        approvals_t1_u2.count == 1
        approvals_t1_u2.data.userId == [users[2]]
    }

    def "assign approvers to skill"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[3], new Date(), "Please approve this!")

        when:
        def approvals_t0 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureApproverForSkillId(proj.projectId, user1Service.userName, skills[0].skillId)
        skillsService.configureApproverForSkillId(proj.projectId, user2Service.userName, skills[1].skillId)
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t1.count == 1
        approvals_t1.data.userId == [users[2]]

        approvals_t1_u2.count == 1
        approvals_t1_u2.data.userId == [users[3]]
    }

    def "assign approvers by user tag"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[3], new Date(), "Please approve this!")

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(users[2], userTagKey, ["abcd"])
        rootUser.saveUserTag(users[3], userTagKey, ["efgh"])

        when:
        def approvals_t0 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey, "abc")
        skillsService.configureApproverForUserTag(proj.projectId, user2Service.userName, userTagKey, "efgh")
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t1.count == 1
        approvals_t1.data.userId == [users[2]]

        approvals_t1_u2.count == 1
        approvals_t1_u2.data.userId == [users[3]]
    }

    def "approver matches multiple ways based on the conf"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[3], new Date(), "Please approve this!")

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(users[2], userTagKey, ["abcd"])
        rootUser.saveUserTag(users[3], userTagKey, ["efgh"])

        when:
        def approvals_t0 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, users[2])
        skillsService.configureApproverForSkillId(proj.projectId, user1Service.userName, skills[0].skillId)
        skillsService.configureApproverForUser(proj.projectId, user2Service.userName, users[2])
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey, "abc")
        skillsService.configureApproverForUserTag(proj.projectId, user2Service.userName, userTagKey, "nomatch")
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvals_t0.data.userId == [users[3],  users[2]]
        approvals_t1.data.userId == [users[2]]
        approvals_t1_u2.data.userId == [users[2]]
    }

    def "project admin of a different project should be rejected"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(2, true)
        def user1Service = createService(users[0])
        user1Service.createProject(SkillsFactory.createProject(2))

        when:
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, users[1])

        then:
        SkillsClientException e = thrown()
        e.message.contains("Approver [${user1Service.userName}] does not have permission to approve for the project [${proj.projectId}]")
    }

    def "project approver of a different project should be rejected"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(2, true)
        def user1Service = createService(users[0])
        def proj2 = SkillsFactory.createProject(2)
        skillsService.createProject(proj2)
        skillsService.addUserRole(user1Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, users[1])

        then:
        SkillsClientException e = thrown()
        e.message.contains("Approver [${user1Service.userName}] does not have permission to approve for the project [${proj.projectId}]")
    }



}
