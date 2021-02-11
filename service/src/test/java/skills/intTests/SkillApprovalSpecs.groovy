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
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.repos.SkillApprovalRepo
import spock.lang.IgnoreRest

class SkillApprovalSpecs extends DefaultIntSpec {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

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
        7.times {
            Date date = new Date() - it
            dates << date
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user${it}", date, "Please approve this ${it}!")
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
            assert tableResultPg1.data[index].userId == "user${index}"
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
            assert tableResultPg2.data[index].userId == "user${index+5}"
            assert tableResultPg2.data[index].skillId == "skill1"
            assert tableResultPg2.data[index].skillName == "Test Skill 1"
            assert tableResultPg2.data[index].requestedOn == dates[index+5].time
            assert tableResultPg2.data[index].requestMsg == "Please approve this ${index+5}!"
        }
    }

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

        7.times {
            Date date = new Date() - it
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], "user${it}", date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        3.times {
            Date date = new Date() - it
            def res = skillsService.addSkill([projectId: proj1.projectId, skillId: skills1[1].skillId], "user${it}", date, "Other reason ${it}!")
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
            assert proj1Res.data[index].userId == "user${index}"
            assert proj1Res.data[index].skillId == "skill1"
            assert proj1Res.data[index].skillName == "Test Skill 1"
            assert proj1Res.data[index].requestMsg == "Please approve this ${index}!"
        }

        proj2Res.data.size() == 3
        (0..2).each {Integer index ->
            assert proj2Res.data[index].id
            assert proj2Res.data[index].userId == "user${index}"
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
}
