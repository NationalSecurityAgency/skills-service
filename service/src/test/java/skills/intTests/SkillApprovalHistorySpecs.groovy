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
import skills.intTests.utils.SkillsService
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
        userAttrsRepo.findByUserIdIgnoreCase(userId).userIdForDisplay
    }

    void "get approvals history"() {

        // PROJECT 1
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

        // PROJECT 2
        def proj2 = SkillsFactory.createProject(2)
        def subj2 = SkillsFactory.createSubject(2, 1)
        def skills2 = SkillsFactory.createSkills(1, 2, 1)
        skills2[0].pointIncrement = 200
        skills2[0].numPerformToCompletion = 200
        skills2[0].selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj2)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skills2)

        List<Date> dates2 = []
        7.times {
            Date date = new Date() - it
            dates2 << date
            def res = skillsService.addSkill([projectId: proj2.projectId, skillId: skills2[0].skillId], users[it], date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }


        when:
        def approvals1 = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        def approvalsHistory1 = skillsService.getApprovalsHistory(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, [approvals1.data[0].id, approvals1.data[2].id])
        skillsService.rejectSkillApprovals(proj.projectId, [approvals1.data[4].id], 'This is a rejection message')

        def approvals2 = skillsService.getApprovals(proj.projectId, 5, 1, 'requestedOn', false)
        def approvalsHistory2 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false)


        def approvals1_proj2 = skillsService.getApprovals(proj2.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj2.projectId, [approvals1_proj2.data[0].id, approvals1_proj2.data[2].id])
        def approvalsHistory1_proj2 = skillsService.getApprovalsHistory(proj2.projectId, 7, 1, 'requestedOn', false)

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
        !approvalsHistory2.data[0].message
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
        !approvalsHistory2.data[1].message
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
        approvalsHistory2.data[2].message == 'This is a rejection message'
        approvalsHistory2.data[2].approverUserId == skillsService.userName
        approvalsHistory2.data[2].approverUserIdForDisplay == getUserIdForDisplay(skillsService.userName)


        // proj2 is just here to make sure that proj2's skills don't make into proj1
        approvals1_proj2.totalCount == 7
        approvals1_proj2.count == 7
        approvalsHistory1_proj2.totalCount == 2
        approvalsHistory1_proj2.count == 2
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
        def approvalsHistoryUser3 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false, '',  '1b', '')

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

        approvalsHistoryUser3.totalCount == 0
        approvalsHistoryUser3.count == 0
        !approvalsHistoryUser3.data
    }


    void "get approvals history - filter skill name"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(7,)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
            it.pointIncrement = 200
            it.numPerformToCompletion = 10
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = []
        String user = getRandomUsers(1).get(0)
        skills.size().times {
            Date date = new Date() - it
            dates << date
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[it].skillId], user, date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        when:
        def approvals1 = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals1.data.collect { it.id })

        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false, 'iLL 3',  '', '')
        def approvalsHistoryUser2 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false, 'sK',  '', '')
        def approvalsHistoryUser3 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false, 'bl',  '', '')

        then:
        approvalsHistoryUser1.totalCount == 1
        approvalsHistoryUser1.count == 1
        approvalsHistoryUser1.data.collect { it.skillName } == [skills[2].name]

        approvalsHistoryUser2.totalCount == 7
        approvalsHistoryUser2.count == 7

        approvalsHistoryUser3.totalCount == 0
        approvalsHistoryUser3.count == 0
        !approvalsHistoryUser3.data
    }


    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    void "get approvals history - filter by approver id"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(7,)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
            it.pointIncrement = 200
            it.numPerformToCompletion = 10
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = []
        String user = getRandomUsers(1).get(0)
        skills.size().times {
            Date date = new Date() - it
            dates << date
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[it].skillId], user, date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        when:
        def approvals1 = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)

        List<SkillsService> admins = [
                createService("ApPrOver1"),
                createService("CoolApprover")
        ]
        admins.each {
            skillsService.addProjectAdmin(proj.projectId, it.userName)
        }
        approvals1.data.eachWithIndex { item, int count ->
            SkillsService service = count < 3 ? admins[0] : admins[1]
            service.approve(proj.projectId, [item.id])
        }

        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false, '',  '', 'appROVER1')
        def approvalsHistoryUser2 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false, '',  '', 'olAPPROVE')
        def approvalsHistoryUser3 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false, '',  '', 'b')

        then:
        approvalsHistoryUser1.totalCount == 3
        approvalsHistoryUser1.count == 3
        approvalsHistoryUser1.data.collect { it.approverUserId }.unique() == [admins[0].userName.toLowerCase()]

        approvalsHistoryUser2.totalCount == 4
        approvalsHistoryUser2.count == 4
        approvalsHistoryUser1.data.collect { it.approverUserId }.unique() == [admins[0].userName.toLowerCase()]

        approvalsHistoryUser3.totalCount == 0
        approvalsHistoryUser3.count == 0
        !approvalsHistoryUser3.data
    }


    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    void "get approvals history - filter by all fields"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(7,)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
            it.pointIncrement = 200
            it.numPerformToCompletion = 10
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = []
        List<String> users = getRandomUsers(skills.size())
        skills.size().times {
            Date date = new Date() - it
            dates << date
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[it].skillId], users[it], date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        when:
        def approvals1 = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)

        List<SkillsService> admins = [
                createService("ApPrOver1"),
                createService("CoolApprover")
        ]
        admins.each {
            skillsService.addProjectAdmin(proj.projectId, it.userName)
        }
        approvals1.data.eachWithIndex { item, int count ->
            SkillsService service = count < 3 ? admins[0] : admins[1]
            service.approve(proj.projectId, [item.id])
        }

        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false, skills[1].name,  users[1], 'appROVER1')

        then:
        approvalsHistoryUser1.totalCount == 1
        approvalsHistoryUser1.count == 1
        approvalsHistoryUser1.data.collect { it.skillId } == [skills[1].skillId]
    }

    void "history of approvals and rejections for the same skill"() {
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
        List<String> users = getRandomUsers(2)

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date(), "approve 1")
        def approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date() - 2, "reject 1")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date() - 4, "approve 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.approve(proj.projectId, approvals.data.collect { it.id })

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date() - 6, "reject 2")
        approvals = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(proj.projectId, approvals.data.collect { it.id })


        def approvalsHistoryUser1 = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false, '',  '', '')

        then:
        approvalsHistoryUser1.totalCount == 4
        approvalsHistoryUser1.count == 4
        approvalsHistoryUser1.data.collect { it.requestMsg } == ["approve 1", "reject 1", "approve 2", "reject 2"]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    void "get approvals history - sort"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(7,)
        skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
            it.pointIncrement = 200
            it.numPerformToCompletion = 10
        }

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<Date> dates = []
        List<String> users = getRandomUsers(skills.size())
        skills.size().times {
            Date date = new Date() - it
            dates << date
            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[it].skillId], users[it], date, "Please approve this ${it}!")
            assert res.body.explanation == "Skill was submitted for approval"
        }

        when:
        def approvals1 = skillsService.getApprovals(proj.projectId, 7, 1, 'requestedOn', false)

        List<SkillsService> admins = [
                createService("ApPrOver1"),
                createService("CoolApprover")
        ]
        admins.each {
            skillsService.addProjectAdmin(proj.projectId, it.userName)
        }
        approvals1.data.eachWithIndex { item, int count ->
            SkillsService service = count < 3 ? admins[0] : admins[1]
            Thread.sleep(100) // so sorting works
            if ( count % 2 == 0) {
                service.approve(proj.projectId, [item.id])
            } else {
                service.rejectSkillApprovals(proj.projectId, [item.id])
            }
        }

        def skillNameAsc = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'skillName', true)
        def skillNameDesc = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'skillName', false)

        def rejectedOnAsc = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'rejectedOn', true)
        def rejectedOnDesc = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'rejectedOn', false)

        def requestedOnAsc = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', true)
        def requestedOnDesc = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'requestedOn', false)

        def approverActionTakenOnAsc = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'approverActionTakenOn', true)
        def approverActionTakenOnDesc = skillsService.getApprovalsHistory(proj.projectId, 5, 1, 'approverActionTakenOn', false)


        then:
        skillNameAsc.data.collect { it.skillId } == [skills[0].skillId, skills[1].skillId, skills[2].skillId, skills[3].skillId, skills[4].skillId]
        skillNameDesc.data.collect { it.skillId } == [skills[6].skillId, skills[5].skillId, skills[4].skillId, skills[3].skillId, skills[2].skillId]


        List rejectedOnAsc_rejectedOn = rejectedOnAsc.data.collect { it.rejectedOn }
        List rejectedOnDesc_rejectedOn = rejectedOnDesc.data.collect { it.rejectedOn }
        assert rejectedOnAsc_rejectedOn[0] != null
        assert rejectedOnAsc_rejectedOn[1] != null
        assert rejectedOnAsc_rejectedOn[2] != null
        assert rejectedOnAsc_rejectedOn[3] == null
        assert rejectedOnAsc_rejectedOn[4] == null

        assert rejectedOnDesc_rejectedOn[0] == null
        assert rejectedOnDesc_rejectedOn[1] == null
        assert rejectedOnDesc_rejectedOn[2] == null
        assert rejectedOnDesc_rejectedOn[3] == null
        assert rejectedOnDesc_rejectedOn[4] != null

        requestedOnAsc.data.collect { it.requestedOn } == requestedOnAsc.data.collect { it.requestedOn }.sort()
        requestedOnDesc.data.collect { it.requestedOn } == requestedOnDesc.data.collect { it.requestedOn }.sort().reverse()

        approverActionTakenOnAsc.data.collect { it.approverActionTakenOn } == approverActionTakenOnAsc.data.collect { it.approverActionTakenOn }.sort()
        approverActionTakenOnDesc.data.collect { it.approverActionTakenOn } == approverActionTakenOnDesc.data.collect { it.approverActionTakenOn }.sort().reverse()
    }

}
