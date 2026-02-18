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

        // should be DN in case of pki
        String userIdConConf = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[2]).dn : users[2]
        when:
        def approvals_t0 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, userIdConConf)
        skillsService.configureApproverForUser(proj.projectId, user2Service.userName, userIdConConf)
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_default = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t1.count == 1
        approvals_t1.data.userId == [users[2]]

        approvals_t1_u2.count == 1
        approvals_t1_u2.data.userId == [users[2]]

        approvals_t1_default.count == 1
        approvals_t1_default.data.userId == [users[3]]
    }

    def "assign approvers to user - 2 projects should not interfere"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(5, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "1. Please approve ${users[2]} request!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[3], new Date(), "2. Please approve ${users[2]} request!")

        // should be DN in case of pki
        String user1IdConConf = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[2]).dn : users[2]
        String user2IdConConf = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[3]).dn : users[3]

        def user4Service = createService(users[4])
        skillsService.addUserRole(user4Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        // other project
        def proj2 = SkillsFactory.createProject(2)
        skillsService.createProject(proj2)
        skillsService.addUserRole(user1Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.addUserRole(user2Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.addUserRole(user4Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.configureApproverForUser(proj2.projectId, user4Service.userName, user1IdConConf)
        skillsService.configureApproverForUser(proj2.projectId, user4Service.userName, user2IdConConf)

        when:
        def approvals_t0 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, user1IdConConf)
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, user2IdConConf)
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u4 = user4Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_default = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t1.count == 2
        approvals_t1.data.userId == [users[3], users[2]]

        approvals_t1_u2.count == 0
        !approvals_t1_u2.data

        approvals_t1_default.count == 0
        !approvals_t1_default.data

        approvals_t1_u4.count == 0
        !approvals_t1_u4.data
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
        def approvals_t1_default = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t1.count == 1
        approvals_t1.data.userId == [users[2]]

        approvals_t1_u2.count == 1
        approvals_t1_u2.data.userId == [users[3]]

        approvals_t1_default.count == 0
        !approvals_t1_default.data
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
        String userTagKey = "KeY1"
        rootUser.saveUserTag(users[2], userTagKey, ["aBcD"])
        rootUser.saveUserTag(users[3], userTagKey, ["EfGh"])

        when:
        def approvals_t0 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey.toLowerCase(), "AbC")
        skillsService.configureApproverForUserTag(proj.projectId, user2Service.userName, userTagKey.toLowerCase(), "eFgH")
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_default = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t1.count == 1
        approvals_t1.data.userId == [users[2]]

        approvals_t1_u2.count == 1
        approvals_t1_u2.data.userId == [users[3]]

        approvals_t1_default.count == 0
        !approvals_t1_default.data
    }

    def "assign approvers by user tag - 2 projects should not interfere"() {
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
        String userTagKey = "KeY1"
        rootUser.saveUserTag(users[2], userTagKey, ["aBcD"])
        rootUser.saveUserTag(users[3], userTagKey, ["EfGh"])

        // other project
        def proj2 = SkillsFactory.createProject(2)
        skillsService.createProject(proj2)
        skillsService.addUserRole(user1Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.addUserRole(user2Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.configureApproverForUserTag(proj2.projectId, user1Service.userName, userTagKey.toLowerCase(), "AbC")
        skillsService.configureApproverForUserTag(proj2.projectId, user1Service.userName, userTagKey.toLowerCase(), "eFgH")
        skillsService.configureApproverForUserTag(proj2.projectId, user2Service.userName, userTagKey.toLowerCase(), "AbC")
        skillsService.configureApproverForUserTag(proj2.projectId, user2Service.userName, userTagKey.toLowerCase(), "eFgH")

        when:
        def approvals_t0 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey.toLowerCase(), "AbC")
        skillsService.configureApproverForUserTag(proj.projectId, user2Service.userName, userTagKey.toLowerCase(), "eFgH")
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_default = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t1.data.userId == [users[2]]
        approvals_t1.count == 1

        approvals_t1_u2.count == 1
        approvals_t1_u2.data.userId == [users[3]]

        approvals_t1_default.count == 0
        !approvals_t1_default.data
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

        String userIdConConf = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[2]).dn : users[2]
        when:
        def approvals_t0 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, userIdConConf)
        skillsService.configureApproverForSkillId(proj.projectId, user1Service.userName, skills[0].skillId)
        skillsService.configureApproverForUser(proj.projectId, user2Service.userName, userIdConConf)
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey, "ABc")
        skillsService.configureApproverForUserTag(proj.projectId, user2Service.userName, userTagKey, "noMatch")
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_default = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvals_t0.data.userId == [users[3],  users[2]]
        approvals_t1.data.userId == [users[2]]
        approvals_t1_u2.data.userId == [users[2]]
        approvals_t1_default.data.userId == [users[3]]
    }

    def "conf is returned after being saved"() {
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

        String user2ForDisplay = userAttrsRepo.findByUserIdIgnoreCase(users[2]).userIdForDisplay

        when:
        def forUserConf = skillsService.configureApproverForUser(proj.projectId, user1Service.userName, users[2]).body
        def forSkillConf = skillsService.configureApproverForSkillId(proj.projectId, user2Service.userName, skills[0].skillId).body
        def forTagConf = skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey, "ABC").body

        then:
        forUserConf.id
        forUserConf.approverUserId == user1Service.userName
        forUserConf.userId == users[2]
        forUserConf.userIdForDisplay == user2ForDisplay
        !forUserConf.userTagKey
        !forUserConf.userTagValue
        !forUserConf.skillName
        !forUserConf.skillId

        forSkillConf.id
        forSkillConf.approverUserId == user2Service.userName
        !forSkillConf.userId
        !forSkillConf.userIdForDisplay
        !forSkillConf.userTagKey
        !forSkillConf.userTagValue
        forSkillConf.skillName == skills[0].name
        forSkillConf.skillId == skills[0].skillId

        forTagConf.id
        forTagConf.approverUserId == user1Service.userName
        !forTagConf.userId
        !forTagConf.userIdForDisplay
        forTagConf.userTagKey == userTagKey
        forTagConf.userTagValue == "ABC"
        !forTagConf.skillName
        !forTagConf.skillId
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

    def "get approver conf"() {
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
        rootUser.saveUserTag(users[2], userTagKey, ["ABCD"])
        rootUser.saveUserTag(users[3], userTagKey, ["EFGH"])

        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, users[2])
        skillsService.configureApproverForSkillId(proj.projectId, user1Service.userName, skills[0].skillId)
        skillsService.configureApproverForUser(proj.projectId, user2Service.userName, users[2])
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey, "abc")
        skillsService.configureApproverForUserTag(proj.projectId, user2Service.userName, userTagKey, "nomatch")

        String user2ForDisplay = userAttrsRepo.findByUserIdIgnoreCase(users[2]).userIdForDisplay

        when:
        def approverConf = skillsService.getApproverConf(proj.projectId)

        then:
        approverConf.approverUserId == [user1Service.userName, user1Service.userName, user2Service.userName, user1Service.userName, user2Service.userName]
        approverConf.userId == [users[2], null, users[2], null, null]
        approverConf.userIdForDisplay == [user2ForDisplay, null, user2ForDisplay, null, null]
        approverConf.userTagKey == [null, null, null, userTagKey, userTagKey]
        approverConf.userTagValue == [null, null, null, "abc", "nomatch"]
        approverConf.skillName == [null, skills[0].name, null, null, null]
        approverConf.skillId == [null, skills[0].skillId, null, null, null]
    }

    def "remove approver conf"() {
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

        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, users[2])
        skillsService.configureApproverForSkillId(proj.projectId, user1Service.userName, skills[0].skillId)
        skillsService.configureApproverForUser(proj.projectId, user2Service.userName, users[2])
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey, "Abc")
        skillsService.configureApproverForUserTag(proj.projectId, user2Service.userName, userTagKey, "nomatch")

        String user2ForDisplay = userAttrsRepo.findByUserIdIgnoreCase(users[2]).userIdForDisplay

        when:
        def approverConf = skillsService.getApproverConf(proj.projectId)
        skillsService.deleteApproverConf(proj.projectId, approverConf[1].id)
        def approverConf_t1 = skillsService.getApproverConf(proj.projectId)
        skillsService.deleteApproverConf(proj.projectId, approverConf[4].id)
        def approverConf_t2 = skillsService.getApproverConf(proj.projectId)

        then:
        approverConf.approverUserId == [user1Service.userName, user1Service.userName, user2Service.userName, user1Service.userName, user2Service.userName]
        approverConf.userId == [users[2], null, users[2], null, null]
        approverConf.userIdForDisplay == [user2ForDisplay, null, user2ForDisplay, null, null]
        approverConf.userTagKey == [null, null, null, userTagKey, userTagKey]
        approverConf.userTagValue == [null, null, null, "Abc", "nomatch"]
        approverConf.skillName == [null, skills[0].name, null, null, null]
        approverConf.skillId == [null, skills[0].skillId, null, null, null]

        approverConf_t1.approverUserId == [user1Service.userName, user2Service.userName, user1Service.userName, user2Service.userName]
        approverConf_t1.userId == [users[2], users[2], null, null]
        approverConf_t1.userIdForDisplay == [user2ForDisplay, user2ForDisplay, null, null]
        approverConf_t1.userTagKey == [null, null, userTagKey, userTagKey]
        approverConf_t1.userTagValue == [null, null, "Abc", "nomatch"]
        approverConf_t1.skillName == [null, null, null, null]
        approverConf_t1.skillId == [null, null, null, null]

        approverConf_t2.approverUserId == [user1Service.userName, user2Service.userName, user1Service.userName]
        approverConf_t2.userId == [users[2], users[2], null]
        approverConf_t2.userIdForDisplay == [user2ForDisplay, user2ForDisplay, null]
        approverConf_t2.userTagKey == [null, null, userTagKey]
        approverConf_t2.userTagValue == [null, null, "Abc"]
        approverConf_t2.skillName == [null, null, null]
        approverConf_t2.skillId == [null, null, null]
    }

    def "deleted conf must exist under the requested project"() {
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

        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.configureApproverForSkillId(proj.projectId, user2Service.userName, skills[0].skillId)

        def approverConf = skillsService.getApproverConf(proj.projectId)
        when:
        skillsService.deleteApproverConf(proj2.projectId, approverConf[0].id)

        then:
        SkillsClientException e = thrown()
        e.message.contains("You are not authorized to delete approval with id [${approverConf[0].id}]")
    }

    def "approver conf can only be viewed by admins"() {
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

        when:
        user1Service.getApproverConf(proj.projectId)

        then:
        SkillsClientException e = thrown()
        e.resBody.contains("You do not have permission to view/manage this Project") || e.resBody.contains("HTTP Status 403 – Forbidden")
    }

    def "explicitly designate user to catch all unmatched requests"() {
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
        def approvals_t0_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureFallbackApprover(proj.projectId, user1Service.userName)
        def approverConf = skillsService.getApproverConf(proj.projectId)
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approverConf.size() == 1
        def approver = approverConf[0]
        approver.approverUserId == user1Service.userName
        !approver.userId
        !approver.userId
        !approver.userTagValue
        !approver.skillName
        !approver.skillId

        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t0_u2.count == 2
        approvals_t0_u2.data.userId == [users[3],  users[2]]

        approvals_t1.count == 2
        approvals_t1.data.userId == [users[3],  users[2]]

        approvals_t1_u2.count == 0
        !approvals_t1_u2.data
    }

    def "explicitly designate user to catch all unmatched requests - 2 projects should not interfere with each other - tag conf"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(5, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user5Service = createService(users[4])
        skillsService.addUserRole(user5Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[3], new Date(), "Please approve this!")

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "KeY1"
        rootUser.saveUserTag(users[2], userTagKey, ["aBcD"])
        rootUser.saveUserTag(users[3], userTagKey, ["EfGh"])
        // other project
        def proj2 = SkillsFactory.createProject(2)
        skillsService.createProject(proj2)
        skillsService.addUserRole(user1Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.addUserRole(user2Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.addUserRole(user5Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.configureApproverForUserTag(proj2.projectId, user1Service.userName, userTagKey.toLowerCase(), "AbC")
        skillsService.configureApproverForUserTag(proj2.projectId, user1Service.userName, userTagKey.toLowerCase(), "eFgH")
        skillsService.configureApproverForUserTag(proj2.projectId, user2Service.userName, userTagKey.toLowerCase(), "AbC")
        skillsService.configureApproverForUserTag(proj2.projectId, user2Service.userName, userTagKey.toLowerCase(), "eFgH")
        skillsService.configureApproverForUserTag(proj2.projectId, user5Service.userName, userTagKey.toLowerCase(), "AbC")
        skillsService.configureApproverForUserTag(proj2.projectId, user5Service.userName, userTagKey.toLowerCase(), "eFgH")

        when:
        def approvals_t0 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t0_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureFallbackApprover(proj.projectId, user1Service.userName)
        def approverConf = skillsService.getApproverConf(proj.projectId)
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u5 = user5Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approverConf.size() == 1
        def approver = approverConf[0]
        approver.approverUserId == user1Service.userName
        !approver.userId
        !approver.userId
        !approver.userTagValue
        !approver.skillName
        !approver.skillId

        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t0_u2.count == 2
        approvals_t0_u2.data.userId == [users[3],  users[2]]

        approvals_t1.count == 2
        approvals_t1.data.userId == [users[3],  users[2]]

        approvals_t1_u2.count == 0
        !approvals_t1_u2.data

        approvals_t1_u5.count == 0
        !approvals_t1_u5.data
    }

    def "explicitly designate user to catch all unmatched requests - 2 projects should not interfere with each other - user conf"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(5, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user5Service = createService(users[4])
        skillsService.addUserRole(user5Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[3], new Date(), "Please approve this!")

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "KeY1"
        rootUser.saveUserTag(users[2], userTagKey, ["aBcD"])
        rootUser.saveUserTag(users[3], userTagKey, ["EfGh"])
        // other project
        def proj2 = SkillsFactory.createProject(2)
        skillsService.createProject(proj2)
        skillsService.addUserRole(user1Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.addUserRole(user2Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.addUserRole(user5Service.userName, proj2.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        String user1IdConConf = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[2]).dn : users[2]
        String user2IdConConf = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[3]).dn : users[3]
        skillsService.configureApproverForUser(proj2.projectId, user1Service.userName, user1IdConConf)
        skillsService.configureApproverForUser(proj2.projectId, user1Service.userName, user2IdConConf)
        skillsService.configureApproverForUser(proj2.projectId, user2Service.userName, user1IdConConf)
        skillsService.configureApproverForUser(proj2.projectId, user2Service.userName, user2IdConConf)
        skillsService.configureApproverForUser(proj2.projectId, user5Service.userName, user1IdConConf)
        skillsService.configureApproverForUser(proj2.projectId, user5Service.userName, user2IdConConf)

        when:
        def approvals_t0 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t0_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureFallbackApprover(proj.projectId, user1Service.userName)
        def approverConf = skillsService.getApproverConf(proj.projectId)
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u5 = user5Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approverConf.size() == 1
        def approver = approverConf[0]
        approver.approverUserId == user1Service.userName
        !approver.userId
        !approver.userId
        !approver.userTagValue
        !approver.skillName
        !approver.skillId

        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t0_u2.count == 2
        approvals_t0_u2.data.userId == [users[3],  users[2]]

        approvals_t1.count == 2
        approvals_t1.data.userId == [users[3],  users[2]]

        approvals_t1_u2.count == 0
        !approvals_t1_u2.data

        approvals_t1_u5.count == 0
        !approvals_t1_u5.data
    }

    def "when there is no explicit fallback strategy then users without config will handle unmatched requests"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2,)
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
        def approvals_t0_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        skillsService.configureApproverForSkillId(proj.projectId, user2Service.userName, skills[1].skillId)
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvals_t0.count == 2
        approvals_t0.data.userId == [users[3],  users[2]]

        approvals_t0_u2.count == 2
        approvals_t0_u2.data.userId == [users[3],  users[2]]

        approvals_t1.count == 2
        approvals_t1.data.userId == [users[3],  users[2]]

        approvals_t1_u2.count == 0
        !approvals_t1_u2.data
    }

    def "must be admin or approver of the project in order to be designated for approval fallback"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(2, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        def proj2 = SkillsFactory.createProject(2)
        skillsService.createProject(proj2)

        when:
        skillsService.configureFallbackApprover(proj2.projectId, user1Service.userName)
        then:
        SkillsClientException e = thrown()
        e.message.contains("Approver [${user1Service.userName}] does not have permission to approve for the project [${proj2.projectId}]")
    }

    def "can only configure fallback once"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(2, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.configureFallbackApprover(proj.projectId, user1Service.userName)
        when:
        skillsService.configureFallbackApprover(proj.projectId, user1Service.userName)
        then:
        SkillsClientException e = thrown()
        e.message.contains("[${user1Service.userName}] is already a fallback approver")
    }

    def "once fallback is configured approver cannot be assigned other conf"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(2, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.configureFallbackApprover(proj.projectId, user1Service.userName)
        when:
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, "key", "val")
        then:
        SkillsClientException e = thrown()
        e.message.contains("[${user1Service.userName}] is already a fallback approver")
    }

    def "once conf is configured approver cannot be assigned fallback"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(2, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, "key", "val")
        when:
        skillsService.configureFallbackApprover(proj.projectId, user1Service.userName)

        then:
        SkillsClientException e = thrown()
        e.message.contains("Cannot configure fallback approver since this approver already has existing workload config")
    }

    def "there must be 1 implicit or explicit fallback approver"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(2, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, "key", "val")
        when:
        skillsService.configureApproverForUserTag(proj.projectId, skillsService.userName, "key", "val")

        then:
        SkillsClientException e = thrown()
        e.message.contains("This operation will assign the last approver [${skillsService.userName}] away from fallback duties, which is sadly not allowed")
    }

    def "there must be 1 implicit or explicit fallback approver - explicit fallback conf present so no error"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(3, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, "key", "val")
        skillsService.configureFallbackApprover(proj.projectId, user2Service.userName)
        when:
        skillsService.configureApproverForUserTag(proj.projectId, skillsService.userName, "key", "val")

        then:
        true
    }

    def "when approver is removed its conf is also removed"() {
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

        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, users[2])
        skillsService.configureApproverForSkillId(proj.projectId, user1Service.userName, skills[0].skillId)
        skillsService.configureApproverForUser(proj.projectId, user2Service.userName, users[2])
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey, "aBC")
        skillsService.configureApproverForUserTag(proj.projectId, user2Service.userName, userTagKey, "nomatch")

        when:
        def conf_t0 = skillsService.getApproverConf(proj.projectId)
        skillsService.deleteUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def conf_t1 = skillsService.getApproverConf(proj.projectId)
        then:
        conf_t0.collect { it.approverUserId } == [user1Service.userName, user1Service.userName, user2Service.userName, user1Service.userName, user2Service.userName]
        conf_t1.collect { it.approverUserId } == [user2Service.userName, user2Service.userName]
    }

    def "when admin is removed its conf is also removed"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3, 1, 1, 100)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())
        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[3], new Date(), "Please approve this!")

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(users[2], userTagKey, ["abcd"])
        rootUser.saveUserTag(users[3], userTagKey, ["efgh"])

        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, users[2])
        skillsService.configureApproverForSkillId(proj.projectId, user1Service.userName, skills[0].skillId)
        skillsService.configureApproverForUser(proj.projectId, user2Service.userName, users[2])
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey, "Abc")
        skillsService.configureApproverForUserTag(proj.projectId, user2Service.userName, userTagKey, "nomatch")

        when:
        def conf_t0 = skillsService.getApproverConf(proj.projectId)
        skillsService.deleteUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())
        def conf_t1 = skillsService.getApproverConf(proj.projectId)
        then:
        conf_t0.collect { it.approverUserId } == [user1Service.userName, user1Service.userName, user2Service.userName, user1Service.userName, user2Service.userName]
        conf_t1.collect { it.approverUserId } == [user2Service.userName, user2Service.userName]
    }

    def "counting configs works as expected"() {
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
        def user3Service = createService(users[2])
        skillsService.addUserRole(user3Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[3], new Date(), "Please approve this!")

        SkillsService rootUser = createRootSkillService()
        String userTagKey = "key1"
        rootUser.saveUserTag(users[0], userTagKey, ["abcd"])
        rootUser.saveUserTag(users[1], userTagKey, ["efgh"])

        // should be DN in case of pki
        String userIdConConf = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[2]).dn : users[2]
        when:
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, userIdConConf)
        def oneApproval = skillsService.countApproverConf(proj.projectId)
        skillsService.configureApproverForSkillId(proj.projectId, user2Service.userName, skills[0].skillId)
        def twoApprovals = skillsService.countApproverConf(proj.projectId)
        skillsService.configureApproverForUserTag(proj.projectId, user3Service.userName, userTagKey, 'abcd')
        def threeApprovals = skillsService.countApproverConf(proj.projectId)

        skillsService.configureApproverForUser(proj.projectId, user3Service.userName, userIdConConf)
        skillsService.configureApproverForSkillId(proj.projectId, user1Service.userName, skills[0].skillId)
        skillsService.configureApproverForUserTag(proj.projectId, user2Service.userName, userTagKey, 'abcd')
        def unchangedApprovals = skillsService.countApproverConf(proj.projectId)

        then:
        oneApproval == 1
        twoApprovals == 2
        threeApprovals == 3
        unchangedApprovals == 3
    }

    def "approvers can filter by user and skill"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skills[1].pointIncrement = 200
        skills[1].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(8, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        def user2Service = createService(users[1])
        skillsService.addUserRole(user2Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[3], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[4], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[5], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[6], new Date(), "Please approve this!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[7], new Date(), "Please approve this!")

        // should be DN in case of pki
        String userIdConConf2 = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[2]).dn : users[2]
        String userIdConConf3 = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[3]).dn : users[3]
        String userIdConConf4 = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[4]).dn : users[4]
        String userIdConConf5 = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[5]).dn : users[5]
        String userIdConConf6 = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[6]).dn : users[6]
        String userIdConConf7 = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[7]).dn : users[7]

        when:
        skillsService.configureApproverForUser(proj.projectId, user2Service.userName, userIdConConf2)
        skillsService.configureApproverForUser(proj.projectId, user2Service.userName, userIdConConf3)
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, userIdConConf4)
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, userIdConConf5)
        def approvals_t1 = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_u2 = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_user_filter = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false, userIdConConf4)
        def approvals_t1_skill_filter = user1Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false, '', skills[1].name)
        def approvals_t1_u2_user_filter = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false, userIdConConf2)
        def approvals_t1_u2_skill_filter = user2Service.getApprovals(proj.projectId, 10, 1, 'requestedOn', false, '', skills[1].name)
        def approvals_t1_default = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def approvals_t1_default_user_filter = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false, userIdConConf6)
        def approvals_t1_default_skill_filter = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false, '', skills[1].name)

        then:
        approvals_t1.count == 2
        approvals_t1.data.userId == [userIdConConf5, userIdConConf4]
        approvals_t1_user_filter.count == 1
        approvals_t1_user_filter.data.userId == [userIdConConf4]
        approvals_t1_skill_filter.count == 1
        approvals_t1_skill_filter.data.userId == [userIdConConf5]
        approvals_t1_skill_filter.data.skillName == [skills[1].name]

        approvals_t1_u2.count == 2
        approvals_t1_u2.data.userId == [userIdConConf3, userIdConConf2]
        approvals_t1_u2_user_filter.count == 1
        approvals_t1_u2_user_filter.data.userId == [userIdConConf2]
        approvals_t1_u2_skill_filter.count == 1
        approvals_t1_u2_skill_filter.data.userId == [userIdConConf3]
        approvals_t1_u2_skill_filter.data.skillName == [skills[1].name]

        approvals_t1_default.count == 2
        approvals_t1_default.data.userId == [userIdConConf7, userIdConConf6]
        approvals_t1_default_user_filter.count == 1
        approvals_t1_default_user_filter.data.userId == [userIdConConf6]
        approvals_t1_default_skill_filter.count == 1
        approvals_t1_default_skill_filter.data.userId == [userIdConConf7]
        approvals_t1_default_skill_filter.data.skillName == [skills[1].name]
    }
}
