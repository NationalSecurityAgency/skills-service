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

import org.springframework.beans.factory.annotation.Value
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName

class ApproverConfValidationSpecs extends DefaultIntSpec {

    @Value('#{"${skills.config.ui.maxTagValueLengthInApprovalWorkloadConfig}"}')
    int maxTagValueLengthInApprovalWorkloadConfig

    @Value('#{"${skills.config.ui.maxTagKeyLengthInApprovalWorkloadConfig}"}')
    int maxTagKeyLengthInApprovalWorkloadConfig

    def "cannot save duplicate user conf"() {
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

        // should be DN in case of pki
        String userIdConConf = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki' ? userAttrsRepo.findByUserIdIgnoreCase(users[2]).dn : users[2]
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, userIdConConf)
        when:
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, userIdConConf)

        then:
        SkillsClientException e = thrown()
        e.message.contains("exist for projectId=[${proj.projectId}], approverId=[${user1Service.userName}], userId=[${users[2]}] already exist.")
    }

    def "cannot save duplicate skill conf"() {
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

        skillsService.configureApproverForSkillId(proj.projectId, user2Service.userName, skills[0].skillId)
        when:
        skillsService.configureApproverForSkillId(proj.projectId, user2Service.userName, skills[0].skillId)

        then:
        SkillsClientException e = thrown()
        e.message.contains("exist for projectId=[${proj.projectId}], approverId=[${user2Service.userName}], skillId=[${skills[0].skillId}] already exist.")
    }

    def "cannot save duplicate tag conf"() {
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
        rootUser.saveUserTag(users[2], userTagKey, ["aBCd"])
        rootUser.saveUserTag(users[3], userTagKey, ["efGh"])

        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey.toLowerCase(), "Abc")
        when:
        skillsService.configureApproverForUserTag(proj.projectId, user1Service.userName, userTagKey, "aBc")

        then:
        SkillsClientException e = thrown()
        e.message.contains("exist for projectId=[${proj.projectId}], approverId=[${user1Service.userName}], userTagKey=[${userTagKey}], userTagValue=[aBc] already exist.")
    }

    def "can not assign bad skill id"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)


        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        skillsService.configureApproverForSkillId(proj.projectId, user1Service.userName, "noSkillId")

        then:
        SkillsClientException e = thrown()
        e.message.contains("Failed to find skillId [noSkillId]")
    }

    def "can not assign bad user id"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)


        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")

        when:
        skillsService.configureApproverForUser(proj.projectId, user1Service.userName, "notUser")

        then:
        SkillsClientException e = thrown()
        e.message.contains("Provided user id [notuser] does not exist")
    }

    def "only one config can be assigned at a time - userId + skillId"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)


        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")

        when:
        skillsService.wsHelper.adminPost("/projects/${proj.projectId}/approverConf/${user1Service.userName}", [userId: users[2], skillId: skills[0].skillId])
        then:
        SkillsClientException e = thrown()
        e.message.contains("Must provide only one of the config params")
    }

    def "only one config can be assigned at a time - userId + userTag"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)


        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")

        when:
        skillsService.wsHelper.adminPost("/projects/${proj.projectId}/approverConf/${user1Service.userName}", [userId: users[2], userTagKey: "key", userTagValue: "val"])
        then:
        SkillsClientException e = thrown()
        e.message.contains("Must provide only one of the config params")
    }

    def "only one config can be assigned at a time - skillId + userTAg"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)


        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[2], new Date(), "Please approve this!")

        when:
        skillsService.wsHelper.adminPost("/projects/${proj.projectId}/approverConf/${user1Service.userName}", [skillId: skills[0].skillId, userTagKey: "key", userTagValue: "val"])
        then:
        SkillsClientException e = thrown()
        e.message.contains("Must provide only one of the config params")
    }

    def "user tag key must be <= maxTagKeyLengthInApprovalWorkloadConfig"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)


        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())


        String key1 = (1..maxTagKeyLengthInApprovalWorkloadConfig).collect { "a" }.join("")
        skillsService.wsHelper.adminPost("/projects/${proj.projectId}/approverConf/${user1Service.userName}", [userTagKey: key1, userTagValue: "val"])

        when:
        skillsService.wsHelper.adminPost("/projects/${proj.projectId}/approverConf/${user1Service.userName}", [userTagKey: "${key1}1".toString(), userTagValue: "val"])
        then:
        SkillsClientException e = thrown()
        e.message.contains("userTagKey must be < ${maxTagKeyLengthInApprovalWorkloadConfig}")
    }

    def "user tag key must be <= maxTagValueLengthInApprovalWorkloadConfig"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)


        List<String> users = getRandomUsers(4, true)
        def user1Service = createService(users[0])
        skillsService.addUserRole(user1Service.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())


        String value = (1..maxTagValueLengthInApprovalWorkloadConfig).collect { "a" }.join("")
        skillsService.wsHelper.adminPost("/projects/${proj.projectId}/approverConf/${user1Service.userName}", [userTagKey: "key", userTagValue: value])

        when:
        skillsService.wsHelper.adminPost("/projects/${proj.projectId}/approverConf/${user1Service.userName}", [userTagKey: "key", userTagValue: "${value}1".toString()])
        then:
        SkillsClientException e = thrown()
        e.message.contains("userTagValue must be < ${maxTagValueLengthInApprovalWorkloadConfig}")
    }

}

