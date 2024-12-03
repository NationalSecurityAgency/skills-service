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
import spock.lang.IgnoreIf

class ManageTheApproverRoleSpecs extends DefaultIntSpec {

    def "assign approver to user"() {
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        def user1Service = createService(getRandomUsers(1, true)[0])

        when:
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user1Roles = skillsService.getUserRolesForProjectAndUser(proj.projectId, user1Service.userName)
        then:
        user1Roles.userId == [user1Service.userName.toLowerCase()]
        user1Roles.roleName == [RoleName.ROLE_PROJECT_APPROVER.toString()]
    }

    def "only approver and admin roles can be assigned for a project"() {
        def proj = SkillsFactory.createProject()
        String userId = getRandomUsers(1)[0]
        when:
        skillsService.createProject(proj)
        then:
        userRoleException {
            skillsService.addUserRole(userId, proj.projectId, RoleName.ROLE_APP_USER.toString())
        }
    }

    boolean userRoleException(Closure c) {
        try {
            c.call()
        } catch (SkillsClientException sk) {
            if (sk.message.contains(" is not a project role")) {
                return true
            }
            throw sk
        }

        return false
    }

    // every valid user in pki mode can become an approver
    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "only valid dashboard users can have an approver role - reporting skill event is not enough"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 5
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(1, true)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date(), "Please approve this!")

        when:
        skillsService.addUserRole(users[0], proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        then:
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("User [${users[0].toLowerCase()}]  does not exist")
    }

    def "approver can view project's data"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 5
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(2, true)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date(), "Please approve this!")

        def user1Service = createService(users[1])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        def tableResultPg1 = user1Service.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def subjectsRes = user1Service.getSubjects(proj.projectId)
        def skillsRes = user1Service.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        tableResultPg1.data.skillId == [skills[0].skillId]
        tableResultPg1.data.userId == [users[0]]
        subjectsRes.subjectId == [subj.subjectId]
        skillsRes.skillId == [skills[0].skillId]
    }

    def "approver can view project's user client display data"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 5
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(2, true)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date())

        def user1Service = createService(users[1])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        def user0ProjSummary = user1Service.getSkillSummary(users[0], proj.projectId)
        def user0SubjSummary = user1Service.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        then:
        user0ProjSummary.points == 200
        user0SubjSummary.points == 200
    }

    def "approvers cannot report skills on behalf of other users"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 5
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        List<String> users = getRandomUsers(2, true)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date() - 2)

        def user1Service = createService(users[1])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        user1Service.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date())
        then:
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.FORBIDDEN
        e.message.contains("Access Denied")
    }

    def "approver can not mutate project's data"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 5
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(2, true)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date(), "Please approve this!")

        def user1Service = createService(users[1])

        when:
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        then:
        hasPermissionException {
            user1Service.createSubject(SkillsFactory.createSubject(1, 2))
        }
        skillsService.createSubject(SkillsFactory.createSubject(1, 2))

        hasPermissionException {
            user1Service.createSkill(SkillsFactory.createSkill(1, 1, 10))
        }
        skillsService.createSkill(SkillsFactory.createSkill(1, 1, 10))

        hasPermissionException {
            user1Service.createBadge(SkillsFactory.createBadge())
        }
        skillsService.createBadge(SkillsFactory.createBadge())

        hasPermissionException {
            user1Service.addOrUpdateProjectSetting(proj.projectId, "one", "two")
        }
        skillsService.addOrUpdateProjectSetting(proj.projectId, "one", "two")

        hasPermissionException {
            user1Service.addLearningPathPrerequisite(proj.projectId, skills[1].skillId, skills[0].skillId)
        }
        skillsService.addLearningPathPrerequisite(proj.projectId, skills[1].skillId, skills[0].skillId)

        hasPermissionException {
            user1Service.deleteLearningPathPrerequisite(proj.projectId, skills[1].skillId, skills[0].skillId)
        }
        skillsService.deleteLearningPathPrerequisite(proj.projectId, skills[1].skillId, skills[0].skillId)

        hasPermissionException {
            user1Service.archiveUsers([users[0]], proj.projectId)
        }
        skillsService.archiveUsers([users[0]], proj.projectId)
    }

    def "approver can approve self reporting requests"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(2)
        Date date = new Date() - 60

        def approverRoleUser = createService(users[1].toString())
        skillsService.addUserRole(approverRoleUser.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], date, "Please approve this!")
        def approvals_t0 = approverRoleUser.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def approvalHist_t0 = approverRoleUser.getApprovalsHistory(proj.projectId, 7, 1, 'requestedOn', false)

        approverRoleUser.approve(proj.projectId, approvals_t0.data.collect { it.id })

        def approvals_t1 = approverRoleUser.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def approvalHist_t1 = approverRoleUser.getApprovalsHistory(proj.projectId, 7, 1, 'requestedOn', false)

        then:
        res.body.explanation == "Skill was submitted for approval"

        !approvalHist_t0.data
        approvalHist_t1.data.collect { it.userId } == [users[0]]
        approvalHist_t1.data.collect { it.approverUserId } == [users[1]]
        approvalHist_t1.data[0].approverActionTakenOn
        !approvalHist_t1.data[0].rejectedOn

        !approvals_t1.data
    }

    def "approver can reject self reporting requests"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(2)
        Date date = new Date() - 60

        def approverRoleUser = createService(users[1].toString())
        skillsService.addUserRole(approverRoleUser.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], date, "Please approve this!")
        def approvals_t0 = approverRoleUser.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def approvalHist_t0 = approverRoleUser.getApprovalsHistory(proj.projectId, 7, 1, 'requestedOn', false)

        approverRoleUser.rejectSkillApprovals(proj.projectId, approvals_t0.data.collect { it.id })

        def approvals_t1 = approverRoleUser.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def approvalHist_t1 = approverRoleUser.getApprovalsHistory(proj.projectId, 7, 1, 'requestedOn', false)

        then:
        res.body.explanation == "Skill was submitted for approval"

        !approvalHist_t0.data
        approvalHist_t1.data.collect { it.userId } == [users[0]]
        approvalHist_t1.data.collect { it.approverUserId } == [users[1]]
        approvalHist_t1.data[0].approverActionTakenOn
        approvalHist_t1.data[0].rejectedOn

        !approvals_t1.data
    }
    boolean hasPermissionException(Closure c) {
        try {
            c.call()
        } catch (SkillsClientException sk) {
            // pki and pass seem to emit different messages
            if (sk.message.contains("You do not have permission to view/manage this Project") || sk.message.contains("HTTP Status 403 – Forbidden")) {
                return true
            }
            throw sk
        }

        return false
    }


}
