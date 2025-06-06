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
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.settings.Settings

import static skills.intTests.utils.SkillsFactory.*

class ClientDisplaySpec extends DefaultIntSpec {

    def "summary for an empty subject"() {
        def proj1 = createProject()
        def subj1 = createSubject()

        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        when:
        def res = skillsService.getSkillSummary("user1", proj1.projectId)
        then:
        res
        res.subjects.size() == 1
        res.subjects.first().subjectId == subj1.subjectId
        res.subjects.first().totalPoints == 0
        res.totalSkills == 0
    }

    def "only return project description if setting show_project_description_everywhere=true"() {
        def proj1 = createProject(1)
        proj1.description = "desc1"
        def proj2 = createProject(2)
        proj2.description = "desc2"

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.addOrUpdateProjectSetting(proj1.projectId, Settings.SHOW_PROJECT_DESCRIPTION_EVERYWHERE.settingName, "true")
        when:
        def resProj1 = skillsService.getSkillSummary("user1", proj1.projectId)
        def resProj2 = skillsService.getSkillSummary("user1", proj2.projectId)
        then:
        resProj1.projectDescription == "desc1"
        !resProj2.projectDescription
    }

    def "global badge with no dependencies on requested project should not be included in count of completed badges"() {
        def proj1 = createProject()
        def subj1 = createSubject()
        def skills = createSkills(2, 1, 1, 100)
        skillsService.createProject(proj1)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills)

        def proj2 = createProject(2)
        def subj21 = createSubject(2, 1)
        def skills2 = createSkills(2, 2, 1, 100)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj21)
        skillsService.createSkills(skills2)

        def globalBadge = createBadge(1, 1)
        SkillsService supervisorService = createSupervisor()
        supervisorService.createGlobalBadge(globalBadge)
        supervisorService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: globalBadge.badgeId, skillId: skills[0].skillId])
        supervisorService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: globalBadge.badgeId, skillId: skills[1].skillId])
        globalBadge.enabled = 'true'
        supervisorService.createGlobalBadge(globalBadge)

        def user = getRandomUsers(1)[0]
        skillsService.addSkill(skills[0], user)
        skillsService.addSkill(skills[1], user)
        skillsService.addSkill(skills2[0], user)
        skillsService.addSkill(skills2[1], user)

        when:
        def summaryProj1 = skillsService.getSkillSummary(user, proj1.projectId)
        def summaryProj2 = skillsService.getSkillSummary(user, proj2.projectId)

        then:
        summaryProj1.badges.numTotalBadges == 1
        summaryProj1.badges.numBadgesCompleted == 1
        summaryProj2.badges.numTotalBadges == 0
        summaryProj2.badges.numBadgesCompleted == 0
    }

    def "attempt to get proj summary for project that does not exist"() {
        when:
        skillsService.getSkillSummary("user1", "notaproject")
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    def "attempt to get subject summary for subject that does not exist"() {
        def proj1 = createProject()

        skillsService.createProject(proj1)
        when:
        skillsService.getSkillSummary("user1", proj1.projectId, "notasubject")
        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
    }

    def "disabled project badge should not result in summary badges enabled being true"() {
        def proj1 = createProject()
        def subj1 = createSubject()
        def skill1 = createSkill()
        def badge = createBadge()
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
        def proj1 = createProject()
        def subj1 = createSubject()
        def skill1 = createSkill()

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
        def proj1 = createProject()
        def subj1 = createSubject()
        def skill1 = createSkill()
        def badge = createBadge()
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

    def "enabled skills group do contribute to the summary"() {
        def proj = createProject()
        def subj = createSubject()
        def allSkills = createSkills(3)
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
        projectSummary.totalSkills == 2
    }

    def "skills group calculate totalPoints based on all skills regardless of numSkillsRequired"() {
        def proj = createProject()
        def subj = createSubject()
        def allSkills = createSkills(3)
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
        skillsService.updateSkill(skillsGroup, null)

        when:
        def projectSummary = skillsService.getSkillSummary('user1', proj.projectId)

        then:
        projectSummary.skillsLevel == 0
        projectSummary.totalPoints == 20
        projectSummary.subjects
        projectSummary.subjects[0].skillsLevel == 0
        projectSummary.subjects[0].totalPoints == 20
        projectSummary.totalSkills == 2
    }

    def "project summary includes description if set"() {
        def proj1 = createProject()
        def desc = "description descraption despaption"
        proj1.description = desc

        skillsService.createProject(proj1)
        skillsService.addOrUpdateProjectSetting(proj1.projectId, "show_project_description_everywhere", "true")

        when:
        def summary = skillsService.getSkillSummary("user1", proj1.projectId)
        then:
        summary.projectDescription == desc
    }

    def "project summary includes skill counts"() {
        def proj = createProject()
        def subj = createSubject()
        def skills = createSkills(3, 1, 1, 100)

        def subj2 = createSubject(1, 2)
        def skillsSubj2 = createSkills(2, 1, 2, 100)

        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skillsSubj2)

        def users = getRandomUsers(3)
        skillsService.addSkill(skills[0], users[0])
        skillsService.addSkill(skills[1], users[0])
        skillsService.addSkill(skillsSubj2[0], users[0])

        3.times { Integer index -> skillsService.addSkill(skills[index], users[1]) }
        2.times { Integer index -> skillsService.addSkill(skillsSubj2[index], users[1]) }

        when:
        def user1Summary = skillsService.getSkillSummary(users[0], proj.projectId)
        def user2Summary = skillsService.getSkillSummary(users[1], proj.projectId)
        def user3Summary = skillsService.getSkillSummary(users[2], proj.projectId)

        def user1Subj1Summary = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def user2Subj1Summary = skillsService.getSkillSummary(users[1], proj.projectId, subj.subjectId)
        def user3Subj1Summary = skillsService.getSkillSummary(users[2], proj.projectId, subj.subjectId)

        def user1Subj2Summary = skillsService.getSkillSummary(users[0], proj.projectId, subj2.subjectId)
        def user2Subj2Summary = skillsService.getSkillSummary(users[1], proj.projectId, subj2.subjectId)
        def user3Subj2Summary = skillsService.getSkillSummary(users[2], proj.projectId, subj2.subjectId)

        def user1Subj1SummaryWithoutSkills = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId, -1, false)
        def user2Subj1SummaryWithoutSkills = skillsService.getSkillSummary(users[1], proj.projectId, subj.subjectId, -1, false)
        def user3Subj1SummaryWithoutSkills = skillsService.getSkillSummary(users[2], proj.projectId, subj.subjectId, -1, false)

        def user1Subj2SummaryWithoutSkills = skillsService.getSkillSummary(users[0], proj.projectId, subj2.subjectId, -1, false)
        def user2Subj2SummaryWithoutSkills = skillsService.getSkillSummary(users[1], proj.projectId, subj2.subjectId, -1, false)
        def user3Subj2SummaryWithoutSkills = skillsService.getSkillSummary(users[2], proj.projectId, subj2.subjectId, -1, false)


        then:
        user1Summary.skillsAchieved == 3
        user1Summary.totalSkills == 5
        user2Summary.skillsAchieved == 5
        user2Summary.totalSkills == 5
        user3Summary.skillsAchieved == 0
        user3Summary.totalSkills == 5

        user1Subj1Summary.skillsAchieved == 2
        user1Subj1Summary.totalSkills == 3
        user2Subj1Summary.skillsAchieved == 3
        user2Subj1Summary.totalSkills == 3
        user3Subj1Summary.skillsAchieved == 0
        user3Subj1Summary.totalSkills == 3

        user1Subj2Summary.skillsAchieved == 1
        user1Subj2Summary.totalSkills == 2
        user2Subj2Summary.skillsAchieved == 2
        user2Subj2Summary.totalSkills == 2
        user3Subj2Summary.skillsAchieved == 0
        user3Subj2Summary.totalSkills == 2

        user1Subj1SummaryWithoutSkills.skillsAchieved == 2
        user1Subj1SummaryWithoutSkills.totalSkills == 3
        user2Subj1SummaryWithoutSkills.skillsAchieved == 3
        user2Subj1SummaryWithoutSkills.totalSkills == 3
        user3Subj1SummaryWithoutSkills.skillsAchieved == 0
        user3Subj1SummaryWithoutSkills.totalSkills == 3

        user1Subj2SummaryWithoutSkills.skillsAchieved == 1
        user1Subj2SummaryWithoutSkills.totalSkills == 2
        user2Subj2SummaryWithoutSkills.skillsAchieved == 2
        user2Subj2SummaryWithoutSkills.totalSkills == 2
        user3Subj2SummaryWithoutSkills.skillsAchieved == 0
        user3Subj2SummaryWithoutSkills.totalSkills == 2
    }

    def "project summary includes skill counts for skills under groups"() {
        def proj = createProject()
        def subj = createSubject()
        def subjGroup1 = createSkillsGroup(1, 1, 11)
        def subjGroup2 = createSkillsGroup(1, 1, 12)
        def skills = createSkills(3, 1, 1, 100)


        def subj2 = createSubject(1, 2)
        def subj2Group = createSkillsGroup(1, 2, 22)
        def skillsSubj2 = createSkills(2, 1, 2, 100)

        skillsService.createProjectAndSubjectAndSkills(proj, subj, [subjGroup1, subjGroup2])
        skillsService.assignSkillToSkillsGroup(subjGroup1.skillId, skills[0])
        skillsService.assignSkillToSkillsGroup(subjGroup2.skillId, skills[1])
        skillsService.assignSkillToSkillsGroup(subjGroup2.skillId, skills[2])

        skillsService.createSubject(subj2)
        skillsService.createSkills([subj2Group])
        skillsSubj2.each {
            skillsService.assignSkillToSkillsGroup(subj2Group.skillId, it)
        }

        def users = getRandomUsers(3)
        skillsService.addSkill(skills[0], users[0])
        skillsService.addSkill(skills[1], users[0])
        skillsService.addSkill(skillsSubj2[0], users[0])

        3.times { Integer index -> skillsService.addSkill(skills[index], users[1]) }
        2.times { Integer index -> skillsService.addSkill(skillsSubj2[index], users[1]) }

        when:
        def user1Summary = skillsService.getSkillSummary(users[0], proj.projectId)
        def user2Summary = skillsService.getSkillSummary(users[1], proj.projectId)
        def user3Summary = skillsService.getSkillSummary(users[2], proj.projectId)

        def user1Subj1Summary = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def user2Subj1Summary = skillsService.getSkillSummary(users[1], proj.projectId, subj.subjectId)
        def user3Subj1Summary = skillsService.getSkillSummary(users[2], proj.projectId, subj.subjectId)

        def user1Subj2Summary = skillsService.getSkillSummary(users[0], proj.projectId, subj2.subjectId)
        def user2Subj2Summary = skillsService.getSkillSummary(users[1], proj.projectId, subj2.subjectId)
        def user3Subj2Summary = skillsService.getSkillSummary(users[2], proj.projectId, subj2.subjectId)

        def user1Subj1SummaryWithoutSkills = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId, -1, false)
        def user2Subj1SummaryWithoutSkills = skillsService.getSkillSummary(users[1], proj.projectId, subj.subjectId, -1, false)
        def user3Subj1SummaryWithoutSkills = skillsService.getSkillSummary(users[2], proj.projectId, subj.subjectId, -1, false)

        def user1Subj2SummaryWithoutSkills = skillsService.getSkillSummary(users[0], proj.projectId, subj2.subjectId, -1, false)
        def user2Subj2SummaryWithoutSkills = skillsService.getSkillSummary(users[1], proj.projectId, subj2.subjectId, -1, false)
        def user3Subj2SummaryWithoutSkills = skillsService.getSkillSummary(users[2], proj.projectId, subj2.subjectId, -1, false)


        then:
        user1Summary.skillsAchieved == 3
        user1Summary.totalSkills == 5
        user2Summary.skillsAchieved == 5
        user2Summary.totalSkills == 5
        user3Summary.skillsAchieved == 0
        user3Summary.totalSkills == 5

        user1Subj1Summary.skillsAchieved == 2
        user1Subj1Summary.totalSkills == 3
        user2Subj1Summary.skillsAchieved == 3
        user2Subj1Summary.totalSkills == 3
        user3Subj1Summary.skillsAchieved == 0
        user3Subj1Summary.totalSkills == 3

        user1Subj2Summary.skillsAchieved == 1
        user1Subj2Summary.totalSkills == 2
        user2Subj2Summary.skillsAchieved == 2
        user2Subj2Summary.totalSkills == 2
        user3Subj2Summary.skillsAchieved == 0
        user3Subj2Summary.totalSkills == 2

        user1Subj1SummaryWithoutSkills.skillsAchieved == 2
        user1Subj1SummaryWithoutSkills.totalSkills == 3
        user2Subj1SummaryWithoutSkills.skillsAchieved == 3
        user2Subj1SummaryWithoutSkills.totalSkills == 3
        user3Subj1SummaryWithoutSkills.skillsAchieved == 0
        user3Subj1SummaryWithoutSkills.totalSkills == 3

        user1Subj2SummaryWithoutSkills.skillsAchieved == 1
        user1Subj2SummaryWithoutSkills.totalSkills == 2
        user2Subj2SummaryWithoutSkills.skillsAchieved == 2
        user2Subj2SummaryWithoutSkills.totalSkills == 2
        user3Subj2SummaryWithoutSkills.skillsAchieved == 0
        user3Subj2SummaryWithoutSkills.totalSkills == 2
    }

    def "project summary returns latest level achievement date"() {
        def proj = createProject()
        def subj = createSubject()
        def skills = createSkills(5, 1, 1, 100)

        def subj2 = createSubject(1, 2)
        def skillsSubj2 = createSkills(5, 1, 2, 100)

        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(skillsSubj2)

        def users = getRandomUsers(3)
        Date oneDayAgo = null
        Date twoDaysAgo = null
        Date threeDaysAgo = null
        Date fourDaysAgo = null
        Date fiveDaysAgo = null
        use(TimeCategory) {
            oneDayAgo = 1.day.ago
            twoDaysAgo = 2.days.ago
            threeDaysAgo = 3.days.ago
            fourDaysAgo = 4.days.ago
            fiveDaysAgo = 5.days.ago

            skillsService.addSkill(skills[0], users[0], threeDaysAgo)
            skillsService.addSkill(skills[1], users[0], twoDaysAgo)
            skillsService.addSkill(skillsSubj2[0], users[0], oneDayAgo)

            3.times { Integer index -> skillsService.addSkill(skills[index], users[1], fourDaysAgo) }
            2.times { Integer index -> skillsService.addSkill(skillsSubj2[index], users[1], fiveDaysAgo) }
        }

        when:
        def user1Summary = skillsService.getSkillSummary(users[0], proj.projectId)
        def user2Summary = skillsService.getSkillSummary(users[1], proj.projectId)
        def user3Summary = skillsService.getSkillSummary(users[2], proj.projectId)

        def user1Subj1Summary = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def user2Subj1Summary = skillsService.getSkillSummary(users[1], proj.projectId, subj.subjectId)
        def user3Subj1Summary = skillsService.getSkillSummary(users[2], proj.projectId, subj.subjectId)

        def user1Subj2Summary = skillsService.getSkillSummary(users[0], proj.projectId, subj2.subjectId)
        def user2Subj2Summary = skillsService.getSkillSummary(users[1], proj.projectId, subj2.subjectId)
        def user3Subj2Summary = skillsService.getSkillSummary(users[2], proj.projectId, subj2.subjectId)

        then:
        user1Summary.skillsLevel == 2
        parseDate(user1Summary.lastLevelAchieved) == oneDayAgo
        user1Summary.subjects.find { it.subjectId == subj.subjectId }.skillsLevel == 2
        parseDate(user1Summary.subjects.find { it.subjectId == subj.subjectId }.lastLevelAchieved) == twoDaysAgo

        user1Summary.subjects.find { it.subjectId == subj2.subjectId }.skillsLevel == 1
        parseDate(user1Summary.subjects.find { it.subjectId == subj2.subjectId }.lastLevelAchieved) == oneDayAgo

        user1Subj1Summary.skillsLevel == 2
        parseDate(user1Subj1Summary.lastLevelAchieved) == twoDaysAgo

        user1Subj2Summary.skillsLevel == 1
        parseDate(user1Subj2Summary.lastLevelAchieved) == oneDayAgo

        // user2
        user2Summary.skillsLevel == 3
        parseDate(user2Summary.lastLevelAchieved) == fourDaysAgo
        user2Summary.subjects.find { it.subjectId == subj.subjectId }.skillsLevel == 3
        parseDate(user2Summary.subjects.find { it.subjectId == subj.subjectId }.lastLevelAchieved) == fourDaysAgo

        user2Summary.subjects.find { it.subjectId == subj2.subjectId }.skillsLevel == 2
        parseDate(user2Summary.subjects.find { it.subjectId == subj2.subjectId }.lastLevelAchieved) == fiveDaysAgo

        user2Subj1Summary.skillsLevel == 3
        parseDate(user2Subj1Summary.lastLevelAchieved) == fourDaysAgo

        user2Subj2Summary.skillsLevel == 2
        parseDate(user2Subj2Summary.lastLevelAchieved) == fiveDaysAgo

        // user3
        user3Summary.skillsLevel == 0
        user3Summary.lastLevelAchieved == null
        user3Summary.subjects.find { it.subjectId == subj.subjectId }.skillsLevel == 0
        user3Summary.subjects.find { it.subjectId == subj.subjectId }.lastLevelAchieved == null

        user3Summary.subjects.find { it.subjectId == subj2.subjectId }.skillsLevel == 0
        user3Summary.subjects.find { it.subjectId == subj2.subjectId }.lastLevelAchieved == null

        user3Subj1Summary.skillsLevel == 0
        user3Subj1Summary.lastLevelAchieved == null

        user3Subj2Summary.skillsLevel == 0
        user3Subj2Summary.lastLevelAchieved == null
    }

    def "project summary properly hides disabled skills"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def res = skillsService.getSkillSummary("user1", proj1.projectId)

        proj1_skills[0].enabled = true
        skillsService.updateSkill(proj1_skills[0], proj1_skills[0].skillId)
        def resAfterEnabled = skillsService.getSkillSummary("user1", proj1.projectId)

        then:
        res
        res.totalSkills == 2
        res.totalPoints == 20
        res.subjects.size() == 1
        res.subjects[0].totalSkills == 2
        res.subjects[0].totalPoints == 20

        resAfterEnabled
        resAfterEnabled.totalSkills == 3
        resAfterEnabled.totalPoints == 30
        resAfterEnabled.subjects.size() == 1
        resAfterEnabled.subjects[0].totalSkills == 3
        resAfterEnabled.subjects[0].totalPoints == 30
    }

    def "project summary properly hides disabled subjects"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = false
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills.each { it.enabled = false }

        def proj1Subj2 = createSubject(1, 2)
        List<Map> proj1Subj2Skills = createSkills(3, 1, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)
        skillsService.createSubject(proj1Subj2)
        skillsService.createSkills(proj1Subj2Skills)

        when:
        def res = skillsService.getSkillSummary("user1", proj1.projectId)

        proj1_subj.enabled = true
        skillsService.updateSubject(proj1_subj, proj1_subj.subjectId)
        def resAfterEnabled = skillsService.getSkillSummary("user1", proj1.projectId)

        then:
        res
        res.totalSkills == 3
        res.totalPoints == 30
        res.subjects.size() == 1
        res.subjects[0].totalSkills == 3
        res.subjects[0].totalPoints == 30

        resAfterEnabled
        resAfterEnabled.totalSkills == 6
        resAfterEnabled.totalPoints == 60
        resAfterEnabled.subjects.size() == 2
        resAfterEnabled.subjects[0].totalSkills == 3
        resAfterEnabled.subjects[0].totalPoints == 30
        resAfterEnabled.subjects[1].totalSkills == 3
        resAfterEnabled.subjects[1].totalPoints == 30
    }

    def "subject summary properly hides disabled skills"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def res = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj.subjectId)

        proj1_skills[0].enabled = true
        skillsService.updateSkill(proj1_skills[0], proj1_skills[0].skillId)
        def resAfterEnabled = skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj.subjectId as String)

        then:
        res
        res.totalSkills == 2
        res.totalPoints == 20
        res.skills.size() == 2

        resAfterEnabled
        resAfterEnabled.totalSkills == 3
        resAfterEnabled.totalPoints == 30
        resAfterEnabled.skills.size() == 3
    }

    def "subject summary properly returns an error for a disabled subject"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = false
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills.each { it.enabled = false }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        skillsService.getSkillSummary("user1", proj1.projectId, proj1_subj.subjectId as String)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.resBody.contains("Skill with id [TestSubject1] is not enabled")
    }

    def "single summary properly returns an error for a disabled skill"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        skillsService.getSingleSkillSummary("user1", proj1.projectId, proj1_skills.get(0).skillId as String)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.resBody.contains("Skill with id [skill1] is not enabled")
    }

    def "single summary with subject properly for an enabled skill does not consider disabled skills for next/previous and total skill count"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = false
        proj1_skills[2].enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def res = skillsService.getSingleSkillSummaryWithSubject("user1", proj1.projectId, proj1_subj.subjectId as String, proj1_skills.get(1).skillId as String)

        then:
        res
        res.totalSkills == 1
        res.prevSkillId == null
        res.nextSkillId == null
    }

    private Date parseDate(String str) {
        Date.parse("yyyy-MM-dd'T'HH:mm:ss", str)
    }

}

