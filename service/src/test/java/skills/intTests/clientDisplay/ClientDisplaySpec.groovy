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

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

class ClientDisplaySpec extends DefaultIntSpec {

    def "summary for an empty subject"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        when:
        def res = skillsService.getSkillSummary("user1", proj1.projectId)
        then:
        res
        res.subjects.size() == 1
        res.subjects.first().subjectId == subj1.subjectId
        res.subjects.first().totalPoints == 0
    }

    def "attempt to get proj summary for project that does not exist"() {
        when:
        skillsService.getSkillSummary("user1", "notaproject")
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def "attempt to get subject summary for subject that does not exist"() {
        def proj1 = SkillsFactory.createProject()

        skillsService.createProject(proj1)
        when:
        skillsService.getSkillSummary("user1", proj1.projectId, "notasubject")
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
    }

    def "disabled project badge should not result in summary badges enabled being true"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skill1 = SkillsFactory.createSkill()
        def badge = SkillsFactory.createBadge()
        badge.enabled = 'false'

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkill(skill1)
        skillsService.createBadge(badge)

        when:
        def summaryOneDisabledBadge = skillsService.getSkillSummary("user1", proj1.projectId)

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill1.skillId])
        badge.enabled = 'true'
        skillsService.createBadge(badge)
        def summaryOneEnabledBadge = skillsService.getSkillSummary("user1", proj1.projectId)

        then:
        !summaryOneDisabledBadge.badges.enabled
        summaryOneEnabledBadge.badges.enabled
    }

    def "disabled global badge should not result in summary badges enabled being true"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skill1 = SkillsFactory.createSkill()

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkill(skill1)

        def supervisorService = createSupervisor()
        def globalBadge = [badgeId: "globalBadge", name: 'Test Global Badge 1', enabled: 'false']
        supervisorService.createGlobalBadge(globalBadge)

        when:
        def summaryOneDisabledBadge = skillsService.getSkillSummary("user1", proj1.projectId)

        supervisorService.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge.badgeId, skillId: skill1.skillId)
        globalBadge.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge)
        def summaryOneEnabledBadge = skillsService.getSkillSummary("user1", proj1.projectId)

        then:
        !summaryOneDisabledBadge.badges.enabled
        summaryOneEnabledBadge.badges.enabled
    }

    def "Disabled badges should not result in summary badges enabled being true"() {
        def proj1 = SkillsFactory.createProject()
        def subj1 = SkillsFactory.createSubject()
        def skill1 = SkillsFactory.createSkill()
        def badge = SkillsFactory.createBadge()
        badge.enabled = 'false'

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkill(skill1)
        skillsService.createBadge(badge)

        when:
        def summaryOneDisabledBadge = skillsService.getSkillSummary("user1", proj1.projectId)

        def service = createSupervisor()
        def globalBadge = [badgeId: "globalBadge", name: 'Test Global Badge 1', enabled: 'false']
        service.createGlobalBadge(globalBadge)

        def summaryOneDisabledBadgeOneDisabledGlobalBadge = skillsService.getSkillSummary("user1", proj1.projectId)
        service.assignSkillToGlobalBadge(projectId: proj1.projectId, badgeId: globalBadge.badgeId, skillId: skill1.skillId)
        globalBadge.enabled = 'true'
        service.createGlobalBadge(globalBadge)

        def summaryOneDisabledBadgeOneEnabledGlobalBadge = skillsService.getSkillSummary("user1", proj1.projectId)

        skillsService.assignSkillToBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: skill1.skillId])
        badge.enabled = 'true'
        skillsService.createBadge(badge)

        def summaryBothBadgesEnabled = skillsService.getSkillSummary("user1", proj1.projectId)

        then:
        !summaryOneDisabledBadge.badges.enabled
        !summaryOneDisabledBadgeOneDisabledGlobalBadge.badges.enabled
        summaryOneDisabledBadgeOneEnabledGlobalBadge.badges.enabled
        summaryBothBadgesEnabled.badges.enabled
    }

    def "disabled skills group do not contribute to the summary"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def group1Children = allSkills[1..2]
        group1Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }

        when:
        def projectSummary = skillsService.getSkillSummary('user1', proj.projectId)

        then:
        projectSummary.skillsLevel == 0
        projectSummary.totalPoints == 0
        projectSummary.subjects
        projectSummary.subjects[0].skillsLevel == 0
        projectSummary.subjects[0].totalPoints == 0
    }

    def "enabled skills group do contribute to the summary"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def group1Children = allSkills[1..2]
        group1Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        when:
        def projectSummary = skillsService.getSkillSummary('user1', proj.projectId)

        then:
        projectSummary.skillsLevel == 0
        projectSummary.totalPoints == 20
        projectSummary.subjects
        projectSummary.subjects[0].skillsLevel == 0
        projectSummary.subjects[0].totalPoints == 20
    }

    def "enabled skills group calculate totalPoints based on numSkillsRequired"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def allSkills = SkillsFactory.createSkills(3)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        def skillsGroup = allSkills[0]
        skillsGroup.type = 'SkillsGroup'
        skillsService.createSkill(skillsGroup)
        String skillsGroupId = skillsGroup.skillId
        def group1Children = allSkills[1..2]
        group1Children.each { skill ->
            skillsService.assignSkillToSkillsGroup(skillsGroupId, skill)
        }
        skillsGroup.numSkillsRequired = 1
        skillsGroup.enabled = 'true'
        skillsService.updateSkill(skillsGroup, null)

        when:
        def projectSummary = skillsService.getSkillSummary('user1', proj.projectId)

        then:
        projectSummary.skillsLevel == 0
        projectSummary.totalPoints == 10
        projectSummary.subjects
        projectSummary.subjects[0].skillsLevel == 0
        projectSummary.subjects[0].totalPoints == 10
    }
}
