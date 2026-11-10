/**
 * Copyright 2026 SkillTree
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
package skills.intTests.catalog

import groovy.json.JsonOutput
import org.springframework.beans.factory.annotation.Autowired
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillDefRepo

import static skills.intTests.utils.SkillsFactory.*

class CatalogSkillSelfReportingSpecs extends CatalogIntSpec {

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    SkillDefRepo skillDefRepo

    def "report skill event on imported skill not allowed if not self-report"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        skillsService.addSkill([projectId: project3.projectId, skillId: skill.skillId], user)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("explanation:Skills imported from the catalog can only be reported if the original skill is configured for Self Reporting")
    }


    def "report skill event on original exported skill when original project has insufficient points"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)

        def p2native = createSkill(2, 1, 3, 0, 1, 0, 100)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(p2native)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skill.pointIncrement = 10
        skill2.pointIncrement = 10
        skill3.pointIncrement = 10
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)

        then:
        def exc = thrown(Exception)
        exc.getMessage().contains("explanation:Insufficient project points, skill achievement is disallowed")
        exc.getMessage().contains("errorCode:InsufficientProjectPoints")
        skillsService.getUserStats(project2.projectId, user).userTotalPoints == 0
    }


    def "report self-report approval request on skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skill.pointIncrement = 200
        skill.numPerformToCompletion = 1
        skill.selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        participateInProject(project2.projectId, [user])
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)
        assert res.body.explanation == "Skill was submitted for approval"

        def p1StatsPre = skillsService.getUserStats(project1.projectId, user)
        def p2StatsPre = skillsService.getUserStats(project2.projectId, user)

        def p1Approvals = skillsService.getApprovals(project1.projectId, 7, 1, 'requestedOn', false)
        def p2Approvals = skillsService.getApprovals(project2.projectId, 7, 1, 'requestedOn', false)

        skillsService.approve(project1.projectId, [p1Approvals.data[0].id])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)

        then:
        p1StatsPre.numSkills == 0
        p1StatsPre.userTotalPoints == 0
        p2StatsPre.numSkills == 0
        p2StatsPre.userTotalPoints == 0
        p1Approvals.totalCount == 1
        p1Approvals.data.find { it.userId == user && it.projectId == project1.projectId && it.skillId == skill.skillId }
        p2Approvals.totalCount == 0
        p1Stats.numSkills == 1
        p1Stats.userTotalPoints == 200
        p2Stats.numSkills == 1
        p2Stats.userTotalPoints == 200
    }


    def "report self-report approval request on skill imported from catalog - skill already has pending approval request and then approved"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 2, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 2, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 2, 0, 10)

        skill.pointIncrement = 200
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        skill3.selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)

        def user = getRandomUsers(1)[0]
        when:
        def res1 = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        List<SkillApproval> approvals1 = skillApprovalRepo.findAll()

        def res2 = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)

        List<SkillApproval> approvals2 = skillApprovalRepo.findAll()
        skillsService.approve(project1.projectId, approvals2.collect { it.id })

        def res3 = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        println JsonOutput.prettyPrint(JsonOutput.toJson(res3))

        List<SkillApproval> approvals3 = skillApprovalRepo.findAll()
        skillsService.approve(project1.projectId, approvals3.find { it.approverActionTakenOn == null }.collect { it.id })

        def res4 = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        List<SkillApproval> approvals4 = skillApprovalRepo.findAll()
        then:
        !res1.body.skillApplied
        res1.body.explanation == "Skill was submitted for approval"
        res1.body.pointsEarned == 0
        approvals1.size() == 1
        approvals1[0].projectId == project1.projectId
        skillDefRepo.findById(approvals1[0].skillRefId).get().skillId == skill.skillId
        !approvals1[0].approverActionTakenOn

        !res2.body.skillApplied
        res2.body.pointsEarned == 0
        res2.body.explanation == "This skill was already submitted for approval and is still pending approval"
        approvals2.size() == 1
        approvals2[0].projectId == project1.projectId
        skillDefRepo.findById(approvals2[0].skillRefId).get().skillId == skill.skillId
        !approvals2[0].approverActionTakenOn

        !res3.body.skillApplied
        res3.body.explanation == "Skill was submitted for approval"
        res3.body.pointsEarned == 0
        approvals3.size() == 2
        def approved1 = approvals3.find { it.approverActionTakenOn != null }
        approved1.projectId == project1.projectId
        skillDefRepo.findById(approved1.skillRefId).get().skillId == skill.skillId

        def pending = approvals3.find { it.approverActionTakenOn == null }
        pending.projectId == project1.projectId
        skillDefRepo.findById(pending.skillRefId).get().skillId == skill.skillId

        !res4.body.skillApplied
        res4.body.pointsEarned == 0
        res4.body.explanation == "This skill reached its maximum points"
        approvals4.size() == 2
        approvals4.each { assert it.getApproverActionTakenOn() != null }
    }


    def "skills display summary endpoints must return self-report approval status for imported skills"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def proj2_skill4 = createSkill(2, 1, 4)
        def proj2_skill5 = createSkill(2, 1, 5)

        skill.pointIncrement = 200
        skill.numPerformToCompletion = 1
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        skill3.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill4.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill5.selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.createSkill(proj2_skill4)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)
        skillsService.createSkill(proj2_skill5)

        def user = getRandomUsers(1)[0]
        println skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        println skillsService.addSkill([projectId: project2.projectId, skillId: proj2_skill4.skillId], user)
        println skillsService.addSkill([projectId: project2.projectId, skillId: skill3.skillId], user)
        when:
        def skillsSummary = skillsService.getSkillSummary(user, project2.projectId, p2subj1.subjectId, -1, true).skills
        println JsonOutput.prettyPrint(JsonOutput.toJson(skillsSummary))
        then:
        skillsSummary.size() == 5
        def self1 = skillsSummary.find { it.skillId == skill.skillId }.selfReporting
        self1.enabled
        self1.type == "Approval"
        self1.requestedOn

        def self2 = skillsSummary.find { it.skillId == skill2.skillId }.selfReporting
        self2.enabled
        self2.type == "Approval"
        !self2.requestedOn

        def self3 = skillsSummary.find { it.skillId == skill3.skillId }.selfReporting
        self3.enabled
        self3.type == "Approval"
        self3.requestedOn

        def self4 = skillsSummary.find { it.skillId == proj2_skill4.skillId }.selfReporting
        self4.enabled
        self4.type == "Approval"
        self4.requestedOn

        def self5 = skillsSummary.find { it.skillId == proj2_skill5.skillId }.selfReporting
        self5.enabled
        self5.type == "Approval"
        !self5.requestedOn
    }


    def "single skill endpoint must return self-report approval status for imported skills"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def proj2_skill4 = createSkill(2, 1, 4)
        def proj2_skill5 = createSkill(2, 1, 5)

        skill.pointIncrement = 200
        skill.numPerformToCompletion = 1
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        skill3.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill4.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill5.selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.createSkill(proj2_skill4)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)
        skillsService.createSkill(proj2_skill5)

        def user = getRandomUsers(1)[0]
        skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        skillsService.addSkill([projectId: project2.projectId, skillId: proj2_skill4.skillId], user)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill3.skillId], user)
        when:
        def self1 = skillsService.getSingleSkillSummary(user, project2.projectId, skill.skillId).selfReporting
        def self2 = skillsService.getSingleSkillSummary(user, project2.projectId, skill2.skillId).selfReporting
        def self3 = skillsService.getSingleSkillSummary(user, project2.projectId, skill3.skillId).selfReporting
        def self4 = skillsService.getSingleSkillSummary(user, project2.projectId, proj2_skill4.skillId).selfReporting
        def self5 = skillsService.getSingleSkillSummary(user, project2.projectId, proj2_skill5.skillId).selfReporting
        then:
        self1.enabled
        self1.type == "Approval"
        self1.requestedOn

        self2.enabled
        self2.type == "Approval"
        !self2.requestedOn

        self3.enabled
        self3.type == "Approval"
        self3.requestedOn

        self4.enabled
        self4.type == "Approval"
        self4.requestedOn

        self5.enabled
        self5.type == "Approval"
        !self5.requestedOn
    }


    def "delete user skill event for skill in catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skill.pointIncrement = 20
        skill.numPerformToCompletion = 10

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:

        def user = getRandomUsers(1)[0]
        participateInProject(project2.projectId, [user])
        participateInProject(project3.projectId, [user])
        Date skillDate = new Date()
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user, skillDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)
        def p3Stats = skillsService.getUserStats(project3.projectId, user)

        assert p1Stats.userTotalPoints == 20
        assert p2Stats.userTotalPoints == 20
        assert p3Stats.userTotalPoints == 20

        skillsService.deleteSkillEvent([projectId: project1.projectId, skillId: skill.skillId, userId: user, timestamp: skillDate.time])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def postDeleteP1Stats = skillsService.getUserStats(project1.projectId, user)
        def postDeleteP2Stats = skillsService.getUserStats(project2.projectId, user)
        def postDeleteP3Stats = skillsService.getUserStats(project3.projectId, user)

        then:
        postDeleteP1Stats.userTotalPoints == 0
        postDeleteP2Stats.userTotalPoints == 0
        postDeleteP3Stats.userTotalPoints == 0
    }


    def "delete honor system user skill event for skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skill.pointIncrement = 20
        skill.numPerformToCompletion = 5
        skill.selfReportingType = SkillDef.SelfReportingType.HonorSystem

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        participateInProject(project2.projectId, [user])
        participateInProject(project3.projectId, [user])
        def timestamp = new Date()
        def res = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user, timestamp)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)
        def p3Stats = skillsService.getUserStats(project3.projectId, user)
        assert p1Stats.userTotalPoints == 20
        assert p2Stats.userTotalPoints == 20
        assert p3Stats.userTotalPoints == 20

        skillsService.deleteSkillEvent([projectId: project1.projectId, skillId: skill.skillId, userId: user, timestamp: timestamp.time])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def postDeleteP1Stats = skillsService.getUserStats(project1.projectId, user)
        def postDeleteP2Stats = skillsService.getUserStats(project2.projectId, user)
        def postDeleteP3Stats = skillsService.getUserStats(project3.projectId, user)

        then:
        postDeleteP1Stats.userTotalPoints == 0
        postDeleteP2Stats.userTotalPoints == 0
        postDeleteP3Stats.userTotalPoints == 0
    }


    def "acknowledging approval request rejection on importing project should not cause error"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skill.pointIncrement = 200
        skill.numPerformToCompletion = 1
        skill.selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)
        assert res.body.explanation == "Skill was submitted for approval"

        def p1Approvals = skillsService.getApprovals(project1.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(project1.projectId, [p1Approvals.data[0].id])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        skillsService.removeRejectionFromView(project2.projectId, p1Approvals.data[0].id, user)

        def p2Approvals = skillsService.getApprovals(project2.projectId, 7, 1, 'requestedOn', false)
        p1Approvals = skillsService.getApprovals(project1.projectId, 7, 1, 'requestedOn', false)

        then:
        p1Approvals.count == 0
        p2Approvals.count == 0
    }

}
