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
package skills.intTests.clientDisplay

import groovy.time.TimeCategory
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillDefRepo

class SingleSkillSummarySpec extends DefaultIntSpec {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    SkillDefRepo skillDefRepo

    def "load single skill summary"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSingleSkillSummary("user1", proj1.projectId, proj1_skills.get(1).skillId)

        then:
        !summary.crossProject
        summary.projectId == proj1.projectId
        summary.projectName == proj1.name
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
        summary.pointIncrement == proj1_skills.get(1).pointIncrement
        summary.maxOccurrencesWithinIncrementInterval == proj1_skills.get(1).numMaxOccurrencesIncrementInterval
        summary.totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion
        summary.points == 0
        summary.todaysPoints == 0
        summary.description.description == "This skill [skill2] belongs to project [TestProject1]"
        !summary.groupName
    }

    def "load single skill summary with some users points"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String userId = "user1"
        Date yesterday = use(TimeCategory) { return 1.day.ago }
        when:
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, yesterday)
        def summary = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        !summary.crossProject
        summary.projectId == proj1.projectId
        summary.projectName == proj1.name
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
        summary.pointIncrement == proj1_skills.get(1).pointIncrement
        summary.totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion
        summary.points == 20
        summary.todaysPoints == 10
        summary.description.description == "This skill [skill2] belongs to project [TestProject1]"
        !summary.dependencyInfo
    }


    def "load single skill summary with dependencies"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String userId = "user1"
        when:
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills.get(1).skillId, proj1_skills.get(0).skillId)
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills.get(1).skillId, proj1_skills.get(2).skillId)
        def summary = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.projectId == proj1.projectId
        summary.skillId == proj1_skills.get(1).skillId
        summary.dependencyInfo.numDirectDependents == 2
        !summary.dependencyInfo.achieved
    }

    def "load single skill summary with all dependencies achieved"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 40
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String userId = "user1"
        when:
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills.get(1).skillId, proj1_skills.get(0).skillId)
        skillsService.addLearningPathPrerequisite(proj1.projectId, proj1_skills.get(1).skillId, proj1_skills.get(2).skillId)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], userId, new Date())

        def summary = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.projectId == proj1.projectId
        summary.skillId == proj1_skills.get(1).skillId
        summary.dependencyInfo.numDirectDependents == 2
        summary.dependencyInfo.achieved
    }

    def "if [help.url.root] property was set then must be used as a root for help url"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.get(0).helpUrl = "/some/path"
        proj1_skills.get(1).helpUrl = "https://keepMe.com/some/path"

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.changeSetting(proj1.projectId, "help.url.root", [projectId: proj1.projectId, setting: "help.url.root", value: "http://www.root.com/"])

        when:
        def summary = skillsService.getSingleSkillSummary("user1", proj1.projectId, proj1_skills.get(0).skillId)
        def summary1 = skillsService.getSingleSkillSummary("user1", proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.description.href == "http://www.root.com/some/path"
        summary1.description.href == "https://keepMe.com/some/path"
    }

    DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    def "achieved date"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills[1].numPerformToCompletion = 2
        proj1_skills.each {
            it.pointIncrement = 100
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Date date = new Date()
        String userId = "user1"
        when:
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, date)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, date)

        def summary = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(0).skillId)
        def summary1 = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(1).skillId)
        def summary2 = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(2).skillId)

        then:
        formatter.parseDateTime(summary.achievedOn).getMillis() == date.time
        !summary1.achievedOn
        !summary2.achievedOn
    }

    def "load with different self-reporting types and status"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[2].selfReportingType = SkillDef.SelfReportingType.Approval
        skills[3].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj1)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = "user0"
        Date date = new Date()
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(1).skillId], userId, date)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(2).skillId], userId, date)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(3).skillId], userId, date)
        Integer approvalIdToReject = skillsService.getApprovals(proj1.projectId, 5, 1, 'requestedOn', false).data.find { it.skillId == skills.get(3).skillId }.id
        skillsService.rejectSkillApprovals(proj1.projectId, [approvalIdToReject], 'Good rejection message')

        when:
        def summary1 = skillsService.getSingleSkillSummary(userId, proj1.projectId, skills.get(0).skillId)
        def summary2 = skillsService.getSingleSkillSummary(userId, proj1.projectId, skills.get(1).skillId)
        def summary3 = skillsService.getSingleSkillSummary(userId, proj1.projectId, skills.get(2).skillId)
        def summary4 = skillsService.getSingleSkillSummary(userId, proj1.projectId, skills.get(3).skillId)
        def summary5 = skillsService.getSingleSkillSummary(userId, proj1.projectId, skills.get(4).skillId)

        then:
        summary1.selfReporting.enabled
        summary1.selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        !summary1.selfReporting.requestedOn
        !summary1.selfReporting.rejectedOn
        !summary1.selfReporting.rejectionMsg

        summary2.selfReporting.enabled
        summary2.selfReporting.type == SkillDef.SelfReportingType.HonorSystem.toString()
        !summary2.selfReporting.requestedOn
        !summary2.selfReporting.rejectedOn
        !summary2.selfReporting.rejectionMsg

        summary3.selfReporting.enabled
        summary3.selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        summary3.selfReporting.requestedOn == date.time
        !summary3.selfReporting.rejectedOn
        !summary3.selfReporting.rejectionMsg

        summary4.selfReporting.enabled
        summary4.selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        summary4.selfReporting.requestedOn == date.time
        new Date(summary4.selfReporting.rejectedOn).format('yyyy-MM-dd') == date.format('yyyy-MM-dd')
        summary4.selfReporting.rejectionMsg == 'Good rejection message'

        !summary5.selfReporting.enabled
        !summary5.selfReporting.type
        !summary5.selfReporting.requestedOn
        !summary5.selfReporting.rejectedOn
        !summary5.selfReporting.rejectionMsg
    }

    def "when a self-reporting skill has a history of approvals only load the latest approval info - latest rejection"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = (0..5).collect { new Date() - it }
        List<String> users = getRandomUsers(2)

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[3], "approve 1")
        def approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[2], "reject 1")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[1], "approve 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[0], "reject 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id }, 'last rejection')


        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '', '', '')

        def summary1 = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills.get(0).skillId)

        then:
        approvalsHistoryUser1.totalCount == 4

        summary1.selfReporting.enabled
        summary1.selfReporting.rejectionMsg == 'last rejection'
        summary1.selfReporting.requestedOn == dates[0].time
        summary1.selfReporting.rejectedOn
    }


    def "when a self-reporting skill has a history of approvals only load the latest approval info - latest approval request"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = (0..5).collect { new Date() - it }
        List<String> users = getRandomUsers(2)

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[3], "approve 1")
        def approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[2], "reject 1")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[1], "approve 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[0], "approve 3")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)


        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '', '', '')
        def summary1 = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills.get(0).skillId)

        then:
        approvals.totalCount == 1
        approvalsHistoryUser1.totalCount == 3

        summary1.selfReporting.enabled
        !summary1.selfReporting.rejectionMsg
        summary1.selfReporting.requestedOn == dates[0].time
        !summary1.selfReporting.rejectedOn
    }

    def "when a self-reporting skill has a history of approvals only load the latest approval info - latest approval request was approved"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = (0..5).collect { new Date() - it }
        List<String> users = getRandomUsers(2)

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[3], "approve 1")
        def approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[2], "reject 1")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[1], "approve 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[0], "approve 3")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })


        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '', '', '')

        def summary1 = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills.get(0).skillId)

        then:
        approvalsHistoryUser1.totalCount == 4

        summary1.selfReporting.enabled
        !summary1.selfReporting.rejectionMsg
        !summary1.selfReporting.requestedOn
        !summary1.selfReporting.rejectedOn
    }

    def "user can remove approval rejection from their view"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[2].selfReportingType = SkillDef.SelfReportingType.Approval
        skills[3].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj1)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(1).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(2).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(3).skillId])
        List<SkillApproval> approvalsBefore = skillApprovalRepo.findAll()
        approvalsBefore.each {
            skillsService.rejectSkillApprovals(proj1.projectId, [it.id], 'Good rejection message')
        }

        when:
        skillsService.removeRejectionFromView(proj1.projectId, approvalsBefore.find { getSkillId(it.skillRefId) == skills.get(3).skillId }.id)
        List<SkillApproval> approvalsAfterFirstRemoval = skillApprovalRepo.findAll()

        skillsService.removeRejectionFromView(proj1.projectId, approvalsBefore.find { getSkillId(it.skillRefId) == skills.get(2).skillId }.id)
        List<SkillApproval> approvalsAfter = skillApprovalRepo.findAll()

        then:
        approvalsBefore.size() == 2
        approvalsAfterFirstRemoval.size() == 2
        approvalsAfter.size() == 2

        !approvalsBefore.find({ getSkillId(it.skillRefId) == skills.get(3).skillId }).rejectionAcknowledgedOn
        !approvalsBefore.find({ getSkillId(it.skillRefId) == skills.get(2).skillId }).rejectionAcknowledgedOn

        approvalsAfterFirstRemoval.find({ getSkillId(it.skillRefId) == skills.get(3).skillId }).rejectionAcknowledgedOn
        !approvalsAfterFirstRemoval.find({ getSkillId(it.skillRefId) == skills.get(2).skillId }).rejectionAcknowledgedOn

        approvalsAfter.find({ getSkillId(it.skillRefId) == skills.get(3).skillId }).rejectionAcknowledgedOn
        approvalsAfter.find({ getSkillId(it.skillRefId) == skills.get(2).skillId }).rejectionAcknowledgedOn
    }

    def "user can only delete his/her own approval rejection"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[2].selfReportingType = SkillDef.SelfReportingType.Approval
        skills[3].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj1)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = "user0"
        Date date = new Date()
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(1).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(2).skillId], userId, date)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(3).skillId])
        List<SkillApproval> approvalsBefore = skillApprovalRepo.findAll()
        approvalsBefore.each {
            skillsService.rejectSkillApprovals(proj1.projectId, [it.id], 'Good rejection message')
        }

        when:
        skillsService.removeRejectionFromView(proj1.projectId, approvalsBefore.find { getSkillId(it.skillRefId) == skills.get(2).skillId }.id)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("has userId that does not match provided userId. Provided userId=[skills@skills.org]")
    }

    def "user can only delete rejected approvals"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[2].selfReportingType = SkillDef.SelfReportingType.Approval
        skills[3].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj1)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(1).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(2).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(3).skillId])
        List<SkillApproval> approvalsBefore = skillApprovalRepo.findAll()
        when:
        skillsService.removeRejectionFromView(proj1.projectId, approvalsBefore.find { getSkillId(it.skillRefId) == skills.get(2).skillId }.id)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("user can only remove rejected SkillApproval record")
    }

    def "when deleting rejected approvals project id must match project id in the approval record"() {

        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[2].selfReportingType = SkillDef.SelfReportingType.Approval
        skills[3].selfReportingType = SkillDef.SelfReportingType.Approval

        def proj2 = SkillsFactory.createProject(2)

        skillsService.createProject(proj1)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createProject(proj2)

        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(1).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(2).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(3).skillId])
        List<SkillApproval> approvalsBefore = skillApprovalRepo.findAll()
        when:
        skillsService.removeRejectionFromView(proj2.projectId, approvalsBefore.find { getSkillId(it.skillRefId) == skills.get(2).skillId }.id)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("has projectId that does not match provided projectId")
    }

    def "latest approval request was rejected - user accepts the rejection - then submits another request which is also rejected"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = (0..7).collect { new Date() - it }
        List<String> users = getRandomUsers(2)

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[5], "approve 1")
        def approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[4], "reject 1")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[3], "approve 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        // (1) rejected request
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[2], "approve 3")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id }, 'sorry but rejected 1')
        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '', '', '')
        def summary1 = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills.get(0).skillId)

        Integer rejectionId = approvals.data[0].id

        // (2) user accepts the rejection - should disappear from the summary
        skillsService.removeRejectionFromView(proj.projectId, rejectionId, users[0])
        def approvalsHistoryUser2 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '', '', '')
        def summary2 = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills.get(0).skillId)


        // (3) user submits another request
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], dates[1], "approve 4")
        def approvalsHistoryUser3 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '', '', '')
        def summary3 = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills.get(0).skillId)

        // (4) reject request
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id }, 'sorry but rejected 2')
        def approvalsHistoryUser4 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '', '', '')
        def summary4 = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills.get(0).skillId)


        then:
        approvalsHistoryUser1.totalCount == 4
        summary1.selfReporting.enabled
        summary1.selfReporting.rejectionMsg == 'sorry but rejected 1'
        summary1.selfReporting.requestedOn == dates[2].time
        summary1.selfReporting.rejectedOn


        approvalsHistoryUser2.totalCount == 4
        summary2.selfReporting.enabled
        !summary2.selfReporting.rejectionMsg
        !summary2.selfReporting.requestedOn
        !summary2.selfReporting.rejectedOn

        approvalsHistoryUser3.totalCount == 4
        summary3.selfReporting.enabled
        !summary3.selfReporting.rejectionMsg
        summary3.selfReporting.requestedOn == dates[1].time
        !summary3.selfReporting.rejectedOn

        approvalsHistoryUser4.totalCount == 5
        summary4.selfReporting.enabled
        summary4.selfReporting.rejectionMsg == 'sorry but rejected 2'
        summary4.selfReporting.requestedOn == dates[1].time
        summary4.selfReporting.rejectedOn

    }

    def "return extra fields for the catalog imported skill"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.pointIncrement = 100
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)
        skillsService.exportSkillToCatalog(proj1.projectId, proj1_skills[0].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, proj1_skills[1].skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, proj1_skills[2].skillId)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.importSkillFromCatalog(proj2.projectId, proj2_subj.subjectId, proj1.projectId, proj1_skills[0].skillId)
        skillsService.createSkills(proj2_skills)
        skillsService.importSkillFromCatalog(proj2.projectId, proj2_subj.subjectId, proj1.projectId, proj1_skills[1].skillId)

        def proj3 = SkillsFactory.createProject(3)
        def proj3_subj = SkillsFactory.createSubject(3, 3)
        List<Map> proj3_skills = SkillsFactory.createSkills(2, 3, 3, 100)

        skillsService.createProject(proj3)
        skillsService.createSubject(proj3_subj)
        skillsService.createSkills(proj3_skills)
        skillsService.exportSkillToCatalog(proj3.projectId, proj3_skills[0].skillId)
        skillsService.exportSkillToCatalog(proj3.projectId, proj3_skills[1].skillId)

        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj2.projectId, proj2_subj.subjectId, [
                [projectId: proj3.projectId, skillId: proj3_skills[0].skillId],
                [projectId: proj1.projectId, skillId: proj1_skills[2].skillId],
                [projectId: proj3.projectId, skillId: proj3_skills[1].skillId],
        ])

        when:
        def imported1 = skillsService.getSingleSkillSummary("user1", proj2.projectId, proj1_skills[0].skillId)
        def imported2 = skillsService.getSingleSkillSummary("user1", proj2.projectId, proj1_skills[1].skillId)
        def imported3 = skillsService.getSingleSkillSummary("user1", proj2.projectId, proj1_skills[2].skillId)
        def imported4 = skillsService.getSingleSkillSummary("user1", proj2.projectId, proj3_skills[0].skillId)
        def imported5 = skillsService.getSingleSkillSummary("user1", proj2.projectId, proj3_skills[1].skillId)
        def local1 = skillsService.getSingleSkillSummary("user1", proj2.projectId, proj2_skills[0].skillId)
        def local2 = skillsService.getSingleSkillSummary("user1", proj2.projectId, proj2_skills[0].skillId)

        then:
        imported1.copiedFromProjectId == "TestProject1"
        imported1.copiedFromProjectName == "Test Project#1"
        imported2.copiedFromProjectId == "TestProject1"
        imported2.copiedFromProjectName == "Test Project#1"
        imported3.copiedFromProjectId == "TestProject1"
        imported3.copiedFromProjectName == "Test Project#1"
        imported4.copiedFromProjectId == "TestProject3"
        imported4.copiedFromProjectName == "Test Project#3"
        imported5.copiedFromProjectId == "TestProject3"
        imported5.copiedFromProjectName == "Test Project#3"

        !local1.copiedFromProjectId
        !local1.copiedFromProjectName

        !local2.copiedFromProjectId
        !local2.copiedFromProjectName
    }

    def "load single skill summary with subject"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId,proj1_subj.subjectId, proj1_skills.get(1).skillId)

        then:
        !summary.crossProject
        summary.projectId == proj1.projectId
        summary.projectName == proj1.name
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
        summary.pointIncrement == proj1_skills.get(1).pointIncrement
        summary.maxOccurrencesWithinIncrementInterval == proj1_skills.get(1).numMaxOccurrencesIncrementInterval
        summary.totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion
        summary.points == 0
        summary.todaysPoints == 0
        summary.description.description == "This skill [skill2] belongs to project [TestProject1]"
    }

    def "loading the first skill sets nextSkillId but not prevSkillId"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId,proj1_subj.subjectId, proj1_skills.get(0).skillId)

        then:
        summary.prevSkillId == null
        summary.nextSkillId == 'skill2'
        summary.skillId == proj1_skills.get(0).skillId
        summary.skill == proj1_skills.get(0).name
    }

    def "loading a skill in the middle appropriately sets prev and next skill Ids"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId,proj1_subj.subjectId, proj1_skills.get(1).skillId)

        then:
        summary.prevSkillId == 'skill1'
        summary.nextSkillId == 'skill3'
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
    }

    def "loading the last skill sets prevSkillId but not nextSkillId"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId,proj1_subj.subjectId, proj1_skills.get(2).skillId)

        then:
        summary.prevSkillId == 'skill2'
        summary.nextSkillId == null
        summary.skillId == proj1_skills.get(2).skillId
        summary.skill == proj1_skills.get(2).name
    }

    def "loading a skill with a broken next display works correctly"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillDefRepo.setSkillDisplayOrder(proj1_skills[1].skillId, 3)
        skillDefRepo.setSkillDisplayOrder(proj1_skills[2].skillId, 5)

        when:
        def firstSkill = skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId,proj1_subj.subjectId, proj1_skills.get(0).skillId)
        def secondSkill = skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId,proj1_subj.subjectId, proj1_skills.get(1).skillId)
        def lastSkill = skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId,proj1_subj.subjectId, proj1_skills.get(2).skillId)

        then:
        firstSkill.prevSkillId == null
        firstSkill.nextSkillId == 'skill2'
        firstSkill.skillId == proj1_skills.get(0).skillId
        firstSkill.skill == proj1_skills.get(0).name
        secondSkill.prevSkillId == 'skill1'
        secondSkill.nextSkillId == 'skill3'
        secondSkill.skillId == proj1_skills.get(1).skillId
        secondSkill.skill == proj1_skills.get(1).name
        lastSkill.prevSkillId == 'skill2'
        lastSkill.nextSkillId == null
        lastSkill.skillId == proj1_skills.get(2).skillId
        lastSkill.skill == proj1_skills.get(2).name
    }

    def "skills with groups - loading prev/last skillIds with broken displayOrder"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(1, 1, 22)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills[0..2])
        skillsService.createSkill(p1subj1g1)
        proj1_skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        skillsService.createSkills(proj1_skills[5..9])

        skillDefRepo.setSkillDisplayOrder(p1subj1g1.skillId, 10)
        proj1_skills[0..9].eachWithIndex { it, index ->
            def newIndex = (index + 1) * 2
            skillDefRepo.setSkillDisplayOrder(it.skillId, newIndex)
        }

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId, proj1_subj.subjectId, it.skillId)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                null,
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
                null
        ]
        summaries.orderInGroup == [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10
        ]
    }

    def "skills with groups - loading prev/last skillIds"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(1, 1, 22)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills[0..2])
        skillsService.createSkill(p1subj1g1)
        proj1_skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        skillsService.createSkills(proj1_skills[5..9])

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId, proj1_subj.subjectId, it.skillId)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                null,
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
                null
        ]
        summaries.orderInGroup == [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10
        ]
    }

    def "skills with multiple groups (adjacent) - loading prev/last skillIds"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(1, 1, 22)
        def p1subj1g2 = SkillsFactory.createSkillsGroup(1, 1, 25)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills[0..2])
        skillsService.createSkill(p1subj1g1)
        skillsService.createSkill(p1subj1g2)
        proj1_skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        proj1_skills[5..7].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g2.skillId, it)
        }
        skillsService.createSkills(proj1_skills[8..9])

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId, proj1_subj.subjectId, it.skillId)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                null,
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
                null
        ]
        summaries.orderInGroup == [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10
        ]
    }

    def "skills with multiple groups (not adjacent) - loading prev/last skillIds"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(1, 1, 22)
        def p1subj1g2 = SkillsFactory.createSkillsGroup(1, 1, 25)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills[0..2])
        skillsService.createSkill(p1subj1g1)
        skillsService.createSkill(proj1_skills[5])
        skillsService.createSkill(p1subj1g2)
        skillsService.createSkill(proj1_skills[9])
        proj1_skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        proj1_skills[6..8].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g2.skillId, it)
        }

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId, proj1_subj.subjectId, it.skillId)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                null,
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
                null
        ]
        summaries.orderInGroup == [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10
        ]
    }

    def "skills are all in groups - loading prev/last skillIds"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(1, 1, 22)
        def p1subj1g2 = SkillsFactory.createSkillsGroup(1, 1, 25)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkill(p1subj1g1)
        skillsService.createSkill(p1subj1g2)
        proj1_skills[0..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        proj1_skills[5..9].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g2.skillId, it)
        }

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId, proj1_subj.subjectId, it.skillId)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                null,
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
                null
        ]
        summaries.orderInGroup == [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10
        ]
    }

    def "skills organized correctly after display orders changed - loading prev/last skillIds"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)
        def p1subj1g1 = SkillsFactory.createSkillsGroup(1, 1, 22)
        def p1subj1g2 = SkillsFactory.createSkillsGroup(1, 1, 25)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills[0..2])
        skillsService.createSkill(p1subj1g1)
        skillsService.createSkill(proj1_skills[5])
        skillsService.createSkill(p1subj1g2)
        skillsService.createSkill(proj1_skills[9])
        proj1_skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        proj1_skills[6..8].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g2.skillId, it)
        }

        skillsService.moveSkillDown(proj1_skills[0])
        skillsService.moveSkillDown(proj1_skills[0])
        skillsService.moveSkillDown(proj1_skills[0])
        skillsService.moveSkillUp(proj1_skills[8])
        skillsService.moveSkillUp(proj1_skills[8])

        when:
        def summaries = proj1_skills.collect {
            skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId, proj1_subj.subjectId, it.skillId)
        }

        then:
        summaries.skillId == [
                proj1_skills[0].skillId,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[5].skillId,
                proj1_skills[6].skillId,
                proj1_skills[7].skillId,
                proj1_skills[8].skillId,
                proj1_skills[9].skillId,
        ]
        summaries.prevSkillId == [
                proj1_skills[4].skillId,
                null,
                proj1_skills[1].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[0].skillId,
                proj1_skills[8].skillId,
                proj1_skills[6].skillId,
                proj1_skills[5].skillId,
                proj1_skills[7].skillId,
        ]
        summaries.nextSkillId == [
                proj1_skills[5].skillId,
                proj1_skills[2].skillId,
                proj1_skills[3].skillId,
                proj1_skills[4].skillId,
                proj1_skills[0].skillId,
                proj1_skills[8].skillId,
                proj1_skills[7].skillId,
                proj1_skills[9].skillId,
                proj1_skills[6].skillId,
                null
        ]
        summaries.orderInGroup == [
                5,
                1,
                2,
                3,
                4,
                6,
                8,
                9,
                7,
                10
        ]
    }

    def "skills order is not supported for cross-project skills - loading prev/last skillIds"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.createProject(proj2)
        skillsService.createSubject(proj2_subj)
        skillsService.createSkills(proj2_skills)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(1).skillId)

        when:
        def res = skillsService.getCrossProjectSkillSummaryWithSubject("user1", proj2.projectId, proj1.projectId, proj2_subj.subjectId, proj1_skills.get(1).skillId)

        then:
        res
        res.prevSkillId == null
        res.nextSkillId == null
    }

    def "load skill summary with badges"(){
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        proj1_subj.helpUrl = "http://foo.org"
        proj1_subj.description = "This is a description"
        List<Map> allSkills = SkillsFactory.createSkills(3, 1, 1)
        SkillsService supervisorService = createSupervisor()

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(allSkills)

        Map badge2 = SkillsFactory.createBadge(1, 2)
        badge2.enabled = true
        supervisorService.createGlobalBadge(badge2)
        supervisorService.assignSkillToGlobalBadge(proj1.projectId, badge2.badgeId, allSkills[0].skillId)
        supervisorService.updateGlobalBadge(badge2, badge2.badgeId)

        Map badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        allSkills.each {
            skillsService.assignSkillToBadge(proj1.projectId, badge1.badgeId, it.skillId)
        }
        badge1.enabled = true
        skillsService.updateBadge(badge1, badge1.badgeId)

        when:
        def summary = skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId, proj1_subj.subjectId, allSkills[0].skillId)
        then:
        summary.badges.size() == 2
        summary.badges[0].badgeId == badge1.badgeId
        summary.badges[1].badgeId == badge2.badgeId

    }

    private String getSkillId(Integer skillRefId) {
        skillDefRepo.findById(skillRefId).get().skillId
    }

    def "load single skill summary with group information"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        def summary = skillsService.getSingleSkillSummaryWithSubject("user1", proj.projectId, subj.subjectId, skills.get(1).skillId)

        then:
        !summary.crossProject
        summary.projectId == proj.projectId
        summary.projectName == proj.name
        summary.skillId == skills.get(1).skillId
        summary.skill == skills.get(1).name
        summary.pointIncrement == skills.get(1).pointIncrement
        summary.maxOccurrencesWithinIncrementInterval == skills.get(1).numMaxOccurrencesIncrementInterval
        summary.totalPoints == skills.get(1).pointIncrement * skills.get(1).numPerformToCompletion
        summary.points == 0
        summary.todaysPoints == 0
        summary.description.description == "This skill [skill2] belongs to project [TestProject1]"
        summary.groupName == skillsGroup.name
        summary.groupSkillId == skillsGroup.skillId
    }

    def "load description for group"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(3)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 5)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        skills.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        String description = skillsService.getSkillDescription(proj.projectId, subj.subjectId, skillsGroup.skillId).description

        then:
        description == "This skill [skill5] belongs to project [TestProject1]"
    }
}
