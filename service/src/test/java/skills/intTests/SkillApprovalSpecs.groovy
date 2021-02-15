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

import groovy.json.JsonOutput
import groovy.lang.Closure
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.model.UserPerformedSkill
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.UserPerformedSkillRepo

class SkillApprovalSpecs extends DefaultIntSpec {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    void "getApprovals paging"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 200
        skills[0].selfReportType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = []
        List<String> users = getRandomUsers(7)
        7.times {
            Date date = new Date() - it
            dates << date
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[it], date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        when:
        def tableResultPg1 = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def tableResultPg2 = skillsService.getApprovals(proj.projectId, 5, 2, 'requestedOn', false)

        then:
        tableResultPg1.totalCount == 7
        tableResultPg1.count == 7
        tableResultPg1.data.size() == 5
        (0..4).each {Integer index ->
            assert tableResultPg1.data[index].id
            assert tableResultPg1.data[index].userId == users[index]
            assert tableResultPg1.data[index].skillId == "skill1"
            assert tableResultPg1.data[index].skillName == "Test Skill 1"
            assert tableResultPg1.data[index].requestedOn == dates[index].time
            assert tableResultPg1.data[index].requestMsg == "Please approve this ${index}!"
        }

        tableResultPg2.totalCount == 7
        tableResultPg2.count == 7
        tableResultPg2.data.size() == 2
        (0..1).each {Integer index ->
            assert tableResultPg2.data[index].id
            assert tableResultPg2.data[index].userId == users[index+5]
            assert tableResultPg2.data[index].skillId == "skill1"
            assert tableResultPg2.data[index].skillName == "Test Skill 1"
            assert tableResultPg2.data[index].requestedOn == dates[index+5].time
            assert tableResultPg2.data[index].requestMsg == "Please approve this ${index+5}!"
        }
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    void "getApprovals sorting"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 200
        skills[0].selfReportType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = []
        7.times {
            Date date = new Date() - it
            dates << date
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user${10 - it}", date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        when:
        def requestedOnDescPg1 = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def requestedOnDescPg2 = skillsService.getApprovals(proj.projectId, 5, 2, 'requestedOn', false)

        def requestedOnAscPg1 = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', true)
        def requestedOnAscPg2 = skillsService.getApprovals(proj.projectId, 5, 2, 'requestedOn', true)

        def userIdDescPg1 = skillsService.getApprovals(proj.projectId, 5, 1, 'userId', false)
        def userIdDescPg2 = skillsService.getApprovals(proj.projectId, 5, 2, 'userId', false)

        def userIdAscPg1 = skillsService.getApprovals(proj.projectId, 5, 1, 'userId', true)
        def userIdAscPg2 = skillsService.getApprovals(proj.projectId, 5, 2, 'userId', true)

        then:
        requestedOnDescPg1.data.collect { it.userId } == ["user10", "user9", "user8", "user7", "user6"]
        requestedOnDescPg2.data.collect { it.userId } == ["user5", "user4"]

        requestedOnAscPg1.data.collect { it.userId } == ["user4", "user5", "user6", "user7", "user8"]
        requestedOnAscPg2.data.collect { it.userId } == ["user9", "user10"]

        userIdAscPg1.data.collect { it.userId } == ["user10", "user4", "user5", "user6", "user7"]
        userIdAscPg2.data.collect { it.userId } == ["user8", "user9" ]

        userIdDescPg1.data.collect { it.userId } == ["user9", "user8", "user7", "user6", "user5"]
        userIdDescPg2.data.collect { it.userId } == ["user4", "user10"]
    }

    void "reject approval requests"() {
        String user = "user0"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve this!")
        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        List<Integer> ids = approvalsEndpointRes.data.collect { it.id }
        skillsService.rejectSkillApprovals(proj.projectId, ids, 'Just felt like it')

        List<SkillApproval> approvalsAfterRejection = skillApprovalRepo.findAll()

        def approvalsEndpointResAfter = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def skillEvents = skillsService.getPerformedSkills(user, proj.projectId)

        then:
        !res.body.skillApplied
        approvalsEndpointRes.data.size() == 1
        !skillEvents.data
        approvalsEndpointResAfter.data.size() == 0

        approvalsAfterRejection.size() == 1
        approvalsAfterRejection.get(0).requestMsg == "Please approve this!"
        approvalsAfterRejection.get(0).rejectedOn.format("yyyy-MM-dd") == new Date().format("yyyy-MM-dd")
        approvalsAfterRejection.get(0).rejectionMsg == "Just felt like it"

        approvalsAfterRejection.get(0).projectId == proj.projectId
        approvalsAfterRejection.get(0).skillRefId == skillDefRepo.findAll().find({it.skillId == skills[0].skillId}).id
        approvalsAfterRejection.get(0).userId == "user0"
        approvalsAfterRejection.get(0).requestedOn == date
    }

    void "getApprovals paging for different projects"() {

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 200
        skills[0].selfReportType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def proj1 = SkillsFactory.createProject(2)
        def subj1 = SkillsFactory.createSubject(2)
        def skills1 = SkillsFactory.createSkills(2,2)
        skills1[1].pointIncrement = 200
        skills1[1].numPerformToCompletion = 200
        skills1[1].selfReportType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills1)

        List<String> users = getRandomUsers(7)
        7.times {
            Date date = new Date() - it
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[it], date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        3.times {
            Date date = new Date() - it
            def res = skillsService.addSkill([projectId: proj1.projectId, skillId: skills1[1].skillId], users[it], date, "Other reason ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        when:
        def proj1Res = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        def proj2Res = skillsService.getApprovals(proj1.projectId, 10, 1, 'requestedOn', false)

        then:
        proj1Res.totalCount == 7
        proj1Res.count == 7

        proj2Res.totalCount == 3
        proj2Res.count == 3

        proj1Res.data.size() == 7
        (0..6).each {Integer index ->
            assert proj1Res.data[index].id
            assert proj1Res.data[index].userId == users[index]
            assert proj1Res.data[index].skillId == "skill1"
            assert proj1Res.data[index].skillName == "Test Skill 1"
            assert proj1Res.data[index].requestMsg == "Please approve this ${index}!"
        }

        proj2Res.data.size() == 3
        (0..2).each {Integer index ->
            assert proj2Res.data[index].id
            assert proj2Res.data[index].userId == users[index]
            assert proj2Res.data[index].skillId == "skill2"
            assert proj2Res.data[index].skillName == "Test Skill 2"
            assert proj2Res.data[index].requestMsg == "Other reason ${index}!"
        }
    }

    void "approval message is optional"() {
        String user = "user0"
        String user1 = "user1"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60
        when:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve this!")
        def res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user1, date)
        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        List approvals = approvalsEndpointRes.data.sort({ it.userId })
        then:
        !res.body.skillApplied
        !res1.body.skillApplied

        approvals.size() == 2
        approvals[0].userId == "user0"
        approvals[0].requestMsg == "Please approve this!"

        approvals[1].userId == "user1"
        !approvals[1].requestMsg
    }

    void "rejection message is optional"() {
        String user = "user0"
        String user1 = "user1"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60

        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve this!")
        def res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user1, date)
        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        List approvals = approvalsEndpointRes.data.sort({ it.userId })

        when:
        skillsService.rejectSkillApprovals(proj.projectId, [approvals[0].id], 'Just felt like it')
        skillsService.rejectSkillApprovals(proj.projectId, [approvals[1].id])

        List<SkillApproval> approvalsAfterRejection = skillApprovalRepo.findAll().sort { it.userId}
        then:
        approvalsAfterRejection.get(0).rejectionMsg == "Just felt like it"
        !approvalsAfterRejection.get(1).rejectionMsg
    }

    void "ignore ids that do not exist during approval or rejection"() {
        String user = "user0"
        String user1 = "user1"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve this!")
        skillsService.approve(proj.projectId, [-2])
        skillsService.rejectSkillApprovals(proj.projectId, [-3])
        then:
        // no error
        true
    }

    void "validate rejection message if 'paragraphValidationRegex' property is configured"() {
        String user = "user0"
        String user1 = "user1"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date() - 60

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], user, date, "Please approve this!")
        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)

        List approvals = approvalsEndpointRes.data.sort({ it.userId })

        skillsService.rejectSkillApprovals(proj.projectId, [approvals[0].id], 'Just jabberwocky felt like it')
        then:
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Custom validation failed: msg=[paragraphs may not contain jabberwocky], type=[skillApprovalRejection], rejectionMsg=[Just jabberwocky felt like it]")
    }

    void "get self reporting stats - all skills disabled"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4,)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        def res = skillsService.getSelfReportStats(proj.projectId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(res))
        then:
        res.size() == 1
        res[0].value == "Disabled"
        res[0].count == 4
    }

    void "get self reporting stats"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10,)
        skills[0].selfReportType = SkillDef.SelfReportingType.Approval
        skills[1].selfReportType = SkillDef.SelfReportingType.Approval
        skills[2].selfReportType = SkillDef.SelfReportingType.HonorSystem
        skills[3].selfReportType = SkillDef.SelfReportingType.HonorSystem
        skills[4].selfReportType = SkillDef.SelfReportingType.HonorSystem

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        when:
        def res = skillsService.getSelfReportStats(proj.projectId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(res))
        then:
        res.size() == 3
        res.find { it.value == "Disabled"}.count == 5
        res.find { it.value == "HonorSystem"}.count == 3
        res.find { it.value == "Approval"}.count == 2
    }

    void "remove existing approval requests if the skill's self approval type to be 'disabled'"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(5,)
        skills.each {
            it.pointIncrement = 200
            it.selfReportType = SkillDef.SelfReportingType.Approval
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def proj1 = SkillsFactory.createProject(2)
        def subj1 = SkillsFactory.createSubject(2)
        def skills1 = SkillsFactory.createSkills(3,2)
        skills1.each {
            it.pointIncrement = 200
            it.selfReportType = SkillDef.SelfReportingType.Approval
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills1)

        5.times {
            Date date = new Date() - it
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[it].skillId], "user${it}", date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
            def res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[it].skillId], "user${it+1}", date, "Please approve this ${it}!")
            assert res1.body.explanation == "Skill was submitted for approval"
        }

        3.times {
            Date date = new Date() - it
            def res = skillsService.addSkill([projectId: proj1.projectId, skillId: skills1[it].skillId], "user${it}", date, "Other reason ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        when:
        List<SkillApproval> approvalBefore = skillApprovalRepo.findAll()
        List<UserPerformedSkill> performedBefore = userPerformedSkillRepo.findAll()

        skills[1].selfReportType = null
        skillsService.createSkills([skills[1]])
        List<SkillApproval> approvalAfter1Delete = skillApprovalRepo.findAll()
        List<UserPerformedSkill> performedAfter = userPerformedSkillRepo.findAll()

        Closure<List<String>> getIds = { approvals->
            return approvals.collect {
                SkillDef skillDef = skillDefRepo.findById(it.skillRefId).get()
                assert skillDef
                return "${skillDef.projectId}-${skillDef.skillId}_${it.userId}"
            }.sort()
        }

        then:
        approvalBefore.collect {it.id }.size() == 13
        getIds.call(approvalBefore) == [
                "TestProject1-skill1_user0",
                "TestProject1-skill1_user1",
                "TestProject1-skill2_user1",
                "TestProject1-skill2_user2",
                "TestProject1-skill3_user2",
                "TestProject1-skill3_user3",
                "TestProject1-skill4_user3",
                "TestProject1-skill4_user4",
                "TestProject1-skill5_user4",
                "TestProject1-skill5_user5",

                "TestProject2-skill1_user0",
                "TestProject2-skill2_user1",
                "TestProject2-skill3_user2"]

        approvalAfter1Delete.collect {it.id }.size() == 11
        getIds.call(approvalAfter1Delete) == [
                "TestProject1-skill1_user0",
                "TestProject1-skill1_user1",
                "TestProject1-skill3_user2",
                "TestProject1-skill3_user3",
                "TestProject1-skill4_user3",
                "TestProject1-skill4_user4",
                "TestProject1-skill5_user4",
                "TestProject1-skill5_user5",

                "TestProject2-skill1_user0",
                "TestProject2-skill2_user1",
                "TestProject2-skill3_user2"]

        performedBefore.collect { it.id }.size() == 0
        performedAfter.collect { it.id }.size() == 0
    }

    void "apply existing approval requests if the skill's self approval type to be 'HonorSystem'"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(5,)
        skills.each {
            it.pointIncrement = 200
            it.selfReportType = SkillDef.SelfReportingType.Approval
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def proj1 = SkillsFactory.createProject(2)
        def subj1 = SkillsFactory.createSubject(2)
        def skills1 = SkillsFactory.createSkills(3,2)
        skills1.each {
            it.pointIncrement = 200
            it.selfReportType = SkillDef.SelfReportingType.Approval
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills1)

        5.times {
            Date date = new Date() - it
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[it].skillId], "user${it}", date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
            def res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[it].skillId], "user${it+1}", date, "Please approve this ${it}!")
            assert res1.body.explanation == "Skill was submitted for approval"
        }

        3.times {
            Date date = new Date() - it
            def res = skillsService.addSkill([projectId: proj1.projectId, skillId: skills1[it].skillId], "user${it}", date, "Other reason ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        when:
        List<SkillApproval> approvalBefore = skillApprovalRepo.findAll()
        List<UserPerformedSkill> performedBefore = userPerformedSkillRepo.findAll()

        skills[1].selfReportType = SkillDef.SelfReportingType.HonorSystem
        skillsService.createSkills([skills[1]])
        List<SkillApproval> approvalAfter1Delete = skillApprovalRepo.findAll()
        List<UserPerformedSkill> performedAfter = userPerformedSkillRepo.findAll()

        Closure<List<String>> getIds = { approvals->
            return approvals.collect {
                SkillDef skillDef = skillDefRepo.findById(it.skillRefId).get()
                assert skillDef
                return "${skillDef.projectId}-${skillDef.skillId}_${it.userId}"
            }.sort()
        }

        then:
        approvalBefore.collect {it.id }.size() == 13
        getIds.call(approvalBefore) == [
                "TestProject1-skill1_user0",
                "TestProject1-skill1_user1",
                "TestProject1-skill2_user1",
                "TestProject1-skill2_user2",
                "TestProject1-skill3_user2",
                "TestProject1-skill3_user3",
                "TestProject1-skill4_user3",
                "TestProject1-skill4_user4",
                "TestProject1-skill5_user4",
                "TestProject1-skill5_user5",

                "TestProject2-skill1_user0",
                "TestProject2-skill2_user1",
                "TestProject2-skill3_user2"]

        approvalAfter1Delete.collect {it.id }.size() == 11
        getIds.call(approvalAfter1Delete) == [
                "TestProject1-skill1_user0",
                "TestProject1-skill1_user1",
                "TestProject1-skill3_user2",
                "TestProject1-skill3_user3",
                "TestProject1-skill4_user3",
                "TestProject1-skill4_user4",
                "TestProject1-skill5_user4",
                "TestProject1-skill5_user5",

                "TestProject2-skill1_user0",
                "TestProject2-skill2_user1",
                "TestProject2-skill3_user2"]

        performedBefore.collect { it.id }.size() == 0
        performedAfter.collect { "${it.projectId}-${it.skillId}_${it.userId}" } == [
                "TestProject1-skill2_user1",
                "TestProject1-skill2_user2",
        ]
    }

    void "apply existing approval requests if the skill's self approval type to be 'HonorSystem' - only apply if request was NOT rejected"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(5,)
        skills.each {
            it.pointIncrement = 200
            it.selfReportType = SkillDef.SelfReportingType.Approval
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Date date = new Date()
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user1", date, "Please approve this")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user2", date, "Please approve this")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user3", date, "Please approve this")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user4", date, "Please approve this")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user5", date, "Please approve this")
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user6", date, "Please approve this")

        def approvalsEndpointRes = skillsService.getApprovals(proj.projectId, 50, 1, 'requestedOn', false)
        List approvalItems = approvalsEndpointRes.data.sort({ it.userId })
        skillsService.rejectSkillApprovals(proj.projectId, [approvalItems[1].id], 'Just felt like it')
        skillsService.rejectSkillApprovals(proj.projectId, [approvalItems[2].id], 'Just felt like it')

        when:
        List<SkillApproval> approvalBefore = skillApprovalRepo.findAll()
        List<UserPerformedSkill> performedBefore = userPerformedSkillRepo.findAll()

        skills[0].selfReportType = SkillDef.SelfReportingType.HonorSystem
        skillsService.createSkills([skills[0]])
        List<SkillApproval> approvalAfter1Delete = skillApprovalRepo.findAll()
        List<UserPerformedSkill> performedAfter = userPerformedSkillRepo.findAll()

        Closure<List<String>> getIds = { approvals->
            return approvals.collect {
                SkillDef skillDef = skillDefRepo.findById(it.skillRefId).get()
                assert skillDef
                return "${skillDef.projectId}-${skillDef.skillId}_${it.userId}"
            }.sort()
        }

        then:
        getIds.call(approvalBefore) == [
                "TestProject1-skill1_user1",
                "TestProject1-skill1_user2",
                "TestProject1-skill1_user3",
                "TestProject1-skill1_user4",
                "TestProject1-skill1_user5",
                "TestProject1-skill1_user6",
                ]

        !getIds.call(approvalAfter1Delete)

        performedBefore.collect { it.id }.size() == 0
        performedAfter.collect { "${it.projectId}-${it.skillId}_${it.userId}" } == [
                "TestProject1-skill1_user1",
                "TestProject1-skill1_user4",
                "TestProject1-skill1_user5",
                "TestProject1-skill1_user6",
        ]
    }
}
