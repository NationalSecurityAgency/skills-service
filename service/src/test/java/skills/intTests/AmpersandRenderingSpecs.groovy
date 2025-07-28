/**
 * Copyright 2022 SkillTree
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


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams
import skills.storage.model.SkillDef

class AmpersandRenderingSpecs extends DefaultIntSpec {

    def "ampersand in global badge name should be returned unescaped"() {
        def badge = SkillsFactory.createBadge()
        badge.name = "A & B"
        badge.badgeId = "b11"

        def badge2 = SkillsFactory.createBadge()
        badge2.name = "A &B"
        badge2.badgeId = "b22"

        def badge3 = SkillsFactory.createBadge()
        badge3.name = "A& B"
        badge3.badgeId = "b33"

        def badge4 = SkillsFactory.createBadge()
        badge4.name = "A&B"
        badge4.badgeId = "b44"

        skillsService.createGlobalBadge(badge)
        skillsService.createGlobalBadge(badge2)
        skillsService.createGlobalBadge(badge3)
        skillsService.createGlobalBadge(badge4)

        when:
        def b1 = skillsService.getGlobalBadge(badge.badgeId)
        def b2 = skillsService.getGlobalBadge(badge2.badgeId)
        def b3 = skillsService.getGlobalBadge(badge3.badgeId)
        def b4 = skillsService.getGlobalBadge(badge4.badgeId)

        then:
        b1.name == "A & B"
        b2.name == "A &B"
        b3.name == "A& B"
        b4.name == "A&B"
    }

    def "available global badge skills must render skill name with unescaped ampersands"() {
        def badge = SkillsFactory.createBadge()
        def proj = SkillsFactory.createProject()

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(1, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(1, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(1, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(1, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(1, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(1, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createGlobalBadge(badge)

        when:
        def availableSkills = skillsService.getAvailableSkillsForGlobalBadge(badge.badgeId, "")

        then:
        availableSkills.suggestedSkills.find { it.name == "A & B" && it.subjectName == "S & B"}
        availableSkills.suggestedSkills.find { it.name == "A &B" && it.subjectName == "S &B"}
        availableSkills.suggestedSkills.find { it.name == "A& B" && it.subjectName == "S& B"}
        availableSkills.suggestedSkills.find { it.name == "A&B" && it.subjectName == "S&B"}
    }

    def "available global badge levels must render skill name with unescaped ampersands"() {
        def badge = SkillsFactory.createBadge()
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"
        def proj2 = SkillsFactory.createProject(2)
        proj2.name = "P &B"
        def proj3 = SkillsFactory.createProject(3)
        proj3.name = "P& B"
        def proj4 = SkillsFactory.createProject(4)
        proj4.name = "P&B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(2, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(3, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(4, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(2, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(3, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(4, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createProject(proj3)
        skillsService.createProject(proj4)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createGlobalBadge(badge)

        when:

        def availableLevels = skillsService.getAvailableProjectsForGlobalBadge(badge.badgeId).projects

        then:
        availableLevels.find {it.name == "P & B"}
        availableLevels.find {it.name == "P &B"}
        availableLevels.find {it.name == "P& B"}
        availableLevels.find {it.name == "P&B"}
    }

    def "skills available for import from catalog should unescape ampersand in name fields"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"
        def proj2 = SkillsFactory.createProject(2)
        proj2.name = "P &B"
        def proj3 = SkillsFactory.createProject(3)
        proj3.name = "P& B"
        def proj4 = SkillsFactory.createProject(4)
        proj4.name = "P&B"

        def importer = SkillsFactory.createProject(5)

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(2, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(3, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(4, 4)
        subj4.name = "S&B"
        def importIntoSubject = SkillsFactory.createSubject(5, 5)

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(2, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(3, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(4, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createProject(proj3)
        skillsService.createProject(proj4)
        skillsService.createProject(importer)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSubject(importIntoSubject)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:
        skillsService.exportSkillToCatalog(proj.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(proj3.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(proj4.projectId, skill4.skillId)

        def catalogSkills = skillsService.getCatalogSkills(importer.projectId, 10, 1, "name")

        then:
        catalogSkills.data.find { it.name == "A & B" && it.subjectName == "S & B" && it.projectName == "P & B"}
        catalogSkills.data.find { it.name == "A &B" && it.subjectName == "S &B" && it.projectName == "P &B"}
        catalogSkills.data.find { it.name == "A& B" && it.subjectName == "S& B" && it.projectName == "P& B"}
        catalogSkills.data.find { it.name == "A&B" && it.subjectName == "S&B" && it.projectName == "P&B"}
    }

    def "skills exported by project should unescape ampersand in name fields"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(1, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(1, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(1, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(1, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(1, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(1, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:
        skillsService.exportSkillToCatalog(proj.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(proj.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(proj.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(proj.projectId, skill4.skillId)

        def catalogSkills = skillsService.getExportedSkills(proj.projectId, 10, 1, "exportedOn", true)

        then:
        catalogSkills.data.find { it.skillName == "A & B" && it.subjectName == "S & B"}
        catalogSkills.data.find { it.skillName == "A &B" && it.subjectName == "S &B"}
        catalogSkills.data.find { it.skillName == "A& B" && it.subjectName == "S& B"}
        catalogSkills.data.find { it.skillName == "A&B" && it.subjectName == "S&B"}
    }

    def "project skills should unescape ampersand in name" () {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(1, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(1, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(1, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(1, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(1, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(1, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:

        def projectSkills = skillsService.getSkillsForProject(proj.projectId)
        println projectSkills

        then:
        projectSkills.find { it.name == "A & B" && it.subjectName == "S & B"}
        projectSkills.find { it.name == "A &B" && it.subjectName == "S &B"}
        projectSkills.find { it.name == "A& B" && it.subjectName == "S& B"}
        projectSkills.find { it.name == "A&B" && it.subjectName == "S&B"}
    }

    def "skill self-report approvals requested should unescape ampersands in skill names"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(1, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(1, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(1, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        def skill2 = SkillsFactory.createSkill(1, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        def skill3 = SkillsFactory.createSkill(1, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        skill3.selfReportingType = SkillDef.SelfReportingType.Approval
        def skill4 = SkillsFactory.createSkill(1, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100
        skill4.selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:
        def user = getRandomUsers(1).first()
        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, new Date(), "Please approve this 1!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skill2.skillId], user, new Date(), "Please approve this 1!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skill3.skillId], user, new Date(), "Please approve this 1!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skill4.skillId], user, new Date(), "Please approve this 1!")

        def approvalsRequested = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)

        then:
        approvalsRequested.data.find {it.skillName == "A & B"}
        approvalsRequested.data.find {it.skillName == "A &B"}
        approvalsRequested.data.find {it.skillName == "A& B"}
        approvalsRequested.data.find {it.skillName == "A&B"}
    }

    def "self report approval history should unescape ampersand on name fields"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(1, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(1, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(1, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        def skill2 = SkillsFactory.createSkill(1, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        def skill3 = SkillsFactory.createSkill(1, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        skill3.selfReportingType = SkillDef.SelfReportingType.Approval
        def skill4 = SkillsFactory.createSkill(1, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100
        skill4.selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:
        def user = getRandomUsers(1).first()
        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user, new Date(), "Please approve this 1!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skill2.skillId], user, new Date(), "Please approve this 1!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skill3.skillId], user, new Date(), "Please approve this 1!")
        skillsService.addSkill([projectId: proj.projectId, skillId: skill4.skillId], user, new Date(), "Please approve this 1!")

        def approvalsRequested = skillsService.getApprovals(proj.projectId, 10, 1, 'requestedOn', false)
        approvalsRequested.data.each {
            skillsService.approve(proj.projectId, [it.id])
        }

        def approvalsHistory = skillsService.getApprovalsHistory(proj.projectId, 10, 1, 'requestedOn', false);

        then:
        approvalsHistory.data.find {it.skillName == "A & B"}
        approvalsHistory.data.find {it.skillName == "A &B"}
        approvalsHistory.data.find {it.skillName == "A& B"}
        approvalsHistory.data.find {it.skillName == "A&B"}
    }

    def "numUsersPerSubjectPerLevelChartBuilder should unescape ampersands in subject names"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(1, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(1, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(1, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(1, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(1, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(1, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:
        def users = getRandomUsers(4)
        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], users[0])
        skillsService.addSkill([projectId: proj.projectId, skillId: skill2.skillId], users[1])
        skillsService.addSkill([projectId: proj.projectId, skillId: skill3.skillId], users[2])
        skillsService.addSkill([projectId: proj.projectId, skillId: skill4.skillId], users[3])

        def res = skillsService.getMetricsData(proj.projectId, "numUsersPerSubjectPerLevelChartBuilder", [:])

        then:
        res.find {it.subject == "S & B"}
        res.find {it.subject == "S &B"}
        res.find {it.subject == "S& B"}
        res.find {it.subject == "S&B"}
    }

    def "skillUsageNavigatorChartBuilder should unescape ampersands in names"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(1, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(1, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(1, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(1, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(1, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(1, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:

        def users = getRandomUsers(4)
        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], users[0])
        skillsService.addSkill([projectId: proj.projectId, skillId: skill2.skillId], users[1])
        skillsService.addSkill([projectId: proj.projectId, skillId: skill3.skillId], users[2])
        skillsService.addSkill([projectId: proj.projectId, skillId: skill4.skillId], users[3])

        def res = skillsService.getMetricsData(proj.projectId, "skillUsageNavigatorChartBuilder", [:])

        then:
        res.skills.find {it.skillName == "A & B"}
        res.skills.find {it.skillName == "A &B"}
        res.skills.find {it.skillName == "A& B"}
        res.skills.find {it.skillName == "A&B"}
    }

    def "userAchievementsChartBuilder should unescape ampersands in names"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(1, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(1, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(1, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(1, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(1, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(1, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:

        def user = getRandomUsers(1).first()
        skillsService.addSkill([projectId: proj.projectId, skillId: skill.skillId], user)
        skillsService.addSkill([projectId: proj.projectId, skillId: skill2.skillId], user)
        skillsService.addSkill([projectId: proj.projectId, skillId: skill3.skillId], user)
        skillsService.addSkill([projectId: proj.projectId, skillId: skill4.skillId], user)

        Map props = [:]
        props[MetricsPagingParamsHelper.PROP_CURRENT_PAGE] = 1
        props[MetricsPagingParamsHelper.PROP_PAGE_SIZE] = 100
        props[MetricsPagingParamsHelper.PROP_SORT_DESC] = false
        props[MetricsPagingParamsHelper.PROP_SORT_BY] = "userName"
        props[MetricsParams.P_ACHIEVEMENT_TYPES] = "${MetricsParams.ACHIEVEMENT_TYPE_OVERALL},${SkillDef.ContainerType.Subject},${SkillDef.ContainerType.Skill},${SkillDef.ContainerType.Badge},${SkillDef.ContainerType.GlobalBadge}"
        def res = skillsService.getMetricsData(proj.projectId, "userAchievementsChartBuilder", props)

        then:
        res.items.find { it.name == "A & B"}
        res.items.find { it.name == "S & B"}
        res.items.find { it.name == "A &B"}
        res.items.find { it.name == "S &B"}
        res.items.find { it.name == "A& B"}
        res.items.find { it.name == "S& B"}
        res.items.find { it.name == "A&B"}
        res.items.find { it.name == "S&B"}
    }

    def "progress & ranking available projects should unescape ampersands in name"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"
        def proj2 = SkillsFactory.createProject(2)
        proj2.name = "P &B"
        def proj3 = SkillsFactory.createProject(3)
        proj3.name = "P& B"
        def proj4 = SkillsFactory.createProject(4)
        proj4.name = "P&B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(2, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(3, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(4, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(2, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(3, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(4, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createProject(proj3)
        skillsService.createProject(proj4)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        skillsService.enableProdMode(proj)
        skillsService.enableProdMode(proj2)
        skillsService.enableProdMode(proj3)
        skillsService.enableProdMode(proj4)

        when:
        def available = skillsService.getAvailableMyProjects()

        then:
        available.find {it.name == "P & B"}
        available.find {it.name == "P &B"}
        available.find {it.name == "P& B"}
        available.find {it.name == "P&B"}
    }

    def "my progress projects should unescape ampersands in names"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"
        def proj2 = SkillsFactory.createProject(2)
        proj2.name = "P &B"
        def proj3 = SkillsFactory.createProject(3)
        proj3.name = "P& B"
        def proj4 = SkillsFactory.createProject(4)
        proj4.name = "P&B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(2, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(3, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(4, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(2, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(3, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(4, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createProject(proj3)
        skillsService.createProject(proj4)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        skillsService.enableProdMode(proj)
        skillsService.enableProdMode(proj2)
        skillsService.enableProdMode(proj3)
        skillsService.enableProdMode(proj4)

        skillsService.addMyProject(proj.projectId)
        skillsService.addMyProject(proj2.projectId)
        skillsService.addMyProject(proj3.projectId)
        skillsService.addMyProject(proj4.projectId)

        when:
        def myProgressProjects = skillsService.getMyProgressSummary()

        then:
        myProgressProjects.projectSummaries.find {it.projectName == "P & B"}
        myProgressProjects.projectSummaries.find {it.projectName == "P &B"}
        myProgressProjects.projectSummaries.find {it.projectName == "P& B"}
        myProgressProjects.projectSummaries.find {it.projectName == "P&B"}
    }

    def "Skill dependencies should unescape ampersands in names"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(1, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(1, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(1, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(1, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(1, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(1, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        skillsService.addLearningPathPrerequisite(proj.projectId, skill4.skillId, skill3.skillId)
        skillsService.addLearningPathPrerequisite(proj.projectId, skill3.skillId, skill2.skillId)
        skillsService.addLearningPathPrerequisite(proj.projectId, skill2.skillId, skill.skillId)

        when:
        def projectGraph = skillsService.getDependencyGraph(proj.projectId)

        then:
        projectGraph.nodes.find {it.name == "A & B"}
        projectGraph.nodes.find {it.name == "A &B"}
        projectGraph.nodes.find {it.name == "A& B"}
        projectGraph.nodes.find {it.name == "A&B"}
    }

    def "summary for current user should escape ampersands in project name and subject names"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P & B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"
        def subj2 = SkillsFactory.createSubject(1, 2)
        subj2.name = "S &B"
        def subj3 = SkillsFactory.createSubject(1, 3)
        subj3.name = "S& B"
        def subj4 = SkillsFactory.createSubject(1, 4)
        subj4.name = "S&B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(1, 2, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(1, 3, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(1, 4, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        skillsService.createSubject(subj4)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:
        def summary = skillsService.getSkillsSummaryForCurrentUser(proj.projectId)

        then:
        summary.projectName == "P & B"
        summary.subjects.find {it.subject=='S & B'}
        summary.subjects.find {it.subject=='S &B'}
        summary.subjects.find {it.subject=='S& B'}
        summary.subjects.find {it.subject=='S&B'}
    }

    def "subject summary for current user should escape ampersands in skill names"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P &B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(1, 1, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(1, 1, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(1, 1, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:
        def summary = skillsService.getSubjectSummaryForCurrentUser(proj.projectId, subj.subjectId)

        then:
        summary.subject == "S & B"
        summary.skills.find {it.skill=='A & B' && it.projectName == "P &B"}
        summary.skills.find {it.skill=='A &B' && it.projectName == "P &B"}
        summary.skills.find {it.skill=='A& B' && it.projectName == "P &B"}
        summary.skills.find {it.skill=='A&B' && it.projectName == "P &B"}
    }

    def "skill summary for current user should escape ampersands in skill name"() {
        def proj = SkillsFactory.createProject()
        proj.name = "P& B"

        def subj = SkillsFactory.createSubject(1, 1)
        subj.name = "S & B"

        def skill = SkillsFactory.createSkill(1, 1, 1)
        skill.name = "A & B"
        skill.pointIncrement = 100
        def skill2 = SkillsFactory.createSkill(1, 1, 2)
        skill2.name = "A &B"
        skill2.pointIncrement = 100
        def skill3 = SkillsFactory.createSkill(1, 1, 3)
        skill3.name = "A& B"
        skill3.pointIncrement = 100
        def skill4 = SkillsFactory.createSkill(1, 1, 4)
        skill4.name = "A&B"
        skill4.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:
        def s1 = skillsService.getSingleSkillSummaryForCurrentUser(proj.projectId, skill.skillId)
        def s2 = skillsService.getSingleSkillSummaryForCurrentUser(proj.projectId, skill2.skillId)
        def s3 = skillsService.getSingleSkillSummaryForCurrentUser(proj.projectId, skill3.skillId)
        def s4 = skillsService.getSingleSkillSummaryForCurrentUser(proj.projectId, skill4.skillId)

        then:
        s1.projectName == "P& B"
        s1.skill == "A & B"

        s2.projectName == "P& B"
        s2.skill == "A &B"

        s3.projectName == "P& B"
        s3.skill == "A& B"

        s4.projectName == "P& B"
        s4.skill == "A&B"
    }
}
