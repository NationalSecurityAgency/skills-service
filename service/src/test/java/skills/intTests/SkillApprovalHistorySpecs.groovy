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


import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillDef
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserPerformedSkillRepo
import spock.lang.IgnoreIf

class SkillApprovalHistorySpecs extends DefaultIntSpec {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    private getUserIdForDisplay(String userId) {
        userAttrsRepo.findByUserId(userId).userIdForDisplay
    }

    void "get approvals history"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

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
        def approvals1 = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        def approvalsHistory1 = skillsService.getApprovalsHistory(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, [approvals1.data[0].id, approvals1.data[2].id])
        skillsService.rejectSkillApprovals(proj.projectId, [approvals1.data[4].id], 'This is a rejection message')

        def approvals2 = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def approvalsHistory2 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false)

        then:
        approvals1.totalCount == 7
        approvals1.count == 7
        approvalsHistory1.totalCount == 0
        approvalsHistory1.count == 0

        approvals2.totalCount == 4
        approvals2.count == 4
        approvals2.data.collect { it.userId } == [users[1], users[3], users[5], users[6]]

        approvalsHistory2.totalCount == 3
        approvalsHistory2.count == 3

        approvalsHistory2.data[0].id
        approvalsHistory2.data[0].userId == users[0]
        approvalsHistory2.data[0].userIdForDisplay == getUserIdForDisplay(users[0])
        approvalsHistory2.data[0].skillId ==  skills[0].skillId
        approvalsHistory2.data[0].subjectId == subj.subjectId
        approvalsHistory2.data[0].projectId == proj.projectId
        approvalsHistory2.data[0].skillName ==  skills[0].name
        approvalsHistory2.data[0].requestedOn == dates[0].time
        approvalsHistory2.data[0].requestMsg == "Please approve this 0!"
        approvalsHistory2.data[0].approverActionTakenOn
        !approvalsHistory2.data[0].rejectedOn
        !approvalsHistory2.data[0].rejectionMsg
        approvalsHistory2.data[0].approverUserId == skillsService.userName
        approvalsHistory2.data[0].approverUserIdForDisplay == getUserIdForDisplay(skillsService.userName)

        approvalsHistory2.data[1].id
        approvalsHistory2.data[1].userId == users[2]
        approvalsHistory2.data[1].userIdForDisplay == getUserIdForDisplay(users[2])
        approvalsHistory2.data[1].skillId ==  skills[0].skillId
        approvalsHistory2.data[1].subjectId == subj.subjectId
        approvalsHistory2.data[1].projectId == proj.projectId
        approvalsHistory2.data[1].skillName ==  skills[0].name
        approvalsHistory2.data[1].requestedOn == dates[2].time
        approvalsHistory2.data[1].requestMsg == "Please approve this 2!"
        approvalsHistory2.data[1].approverActionTakenOn
        !approvalsHistory2.data[1].rejectedOn
        !approvalsHistory2.data[1].rejectionMsg
        approvalsHistory2.data[1].approverUserId == skillsService.userName
        approvalsHistory2.data[1].approverUserIdForDisplay == getUserIdForDisplay(skillsService.userName)

        approvalsHistory2.data[2].id
        approvalsHistory2.data[2].userId == users[4]
        approvalsHistory2.data[2].userIdForDisplay == getUserIdForDisplay(users[4])
        approvalsHistory2.data[2].skillId ==  skills[0].skillId
        approvalsHistory2.data[2].subjectId == subj.subjectId
        approvalsHistory2.data[2].projectId == proj.projectId
        approvalsHistory2.data[2].skillName ==  skills[0].name
        approvalsHistory2.data[2].requestedOn == dates[4].time
        approvalsHistory2.data[2].requestMsg == "Please approve this 4!"
        approvalsHistory2.data[2].approverActionTakenOn
        approvalsHistory2.data[2].rejectedOn
        approvalsHistory2.data[2].rejectionMsg == 'This is a rejection message'
        approvalsHistory2.data[2].approverUserId == skillsService.userName
        approvalsHistory2.data[2].approverUserIdForDisplay == getUserIdForDisplay(skillsService.userName)
    }

    void "get approvals history - paging"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

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
        def approvals1 = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals1.data.collect { it.id })

        def approvalsHistoryPg1 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false)
        def approvalsHistoryPg2 = skillsService.getApprovalsHistory(proj.projectId, 5, 2, 'requestedOn', false)

        then:
        approvalsHistoryPg1.totalCount == 7
        approvalsHistoryPg1.count == 7
        approvalsHistoryPg1.data.collect { it.userId } == [users[0], users[1], users[2], users[3], users[4]]

        approvalsHistoryPg2.totalCount == 7
        approvalsHistoryPg2.count == 7
        approvalsHistoryPg2.data.collect { it.userId } == [users[5], users[6]]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    void "get approvals history - filter user id"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].numPerformToCompletion = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = []
        List<String> users = ['User1', 'uSeR2', 'user3', 'UsEr12']
        users.size().times {
            Date date = new Date() - it
            dates << date
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[it], date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        when:
        def approvals1 = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals1.data.collect { it.id })

        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false, '',  'UsEr2', '')
        def approvalsHistoryUser2 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false, '',  users[3].toLowerCase(), '')
        def approvalsHistoryUsers = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false, '',  'Er1', '')

        then:
        approvalsHistoryUser1.totalCount == 1
        approvalsHistoryUser1.count == 1
        approvalsHistoryUser1.data.collect { it.userIdForDisplay } == [users[1]]

        approvalsHistoryUser2.totalCount == 1
        approvalsHistoryUser2.count == 1
        approvalsHistoryUser2.data.collect { it.userIdForDisplay } == [users[3]]

        approvalsHistoryUsers.totalCount == 2
        approvalsHistoryUsers.count == 2
        approvalsHistoryUsers.data.collect { it.userIdForDisplay } == [users[0], users[3]]
    }

}
