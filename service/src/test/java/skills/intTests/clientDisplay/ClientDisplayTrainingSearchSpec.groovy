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


import org.apache.commons.lang3.StringUtils
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.*

class ClientDisplayTrainingSearchSpec extends DefaultIntSpec {

    def "get skills, subjects and badges"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100, 2)
        def subj1Group1 = createSkillsGroup(1, 1, 11)
        def childSkills = createSkills(3, 1, 1, 100)
        def p1subj2 = createSubject(1, 2)
        def p1SkillsSubj2 = createSkills(10, 1, 2, 100, 2)

        (2..6).each {
            p1Skills[it].name = StringUtils.leftPad((it-1).toString(), 3, "0")
            p1Skills[it].pointIncrement = it
        }

        (3..7).each {
            p1SkillsSubj2[it].name = StringUtils.leftPad((it+3).toString(), 3, "0")
            p1SkillsSubj2[it].pointIncrement = it
        }
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1SkillsSubj2)
        skillsService.createSkill(subj1Group1)
        childSkills.eachWithIndex { it, index ->
            it.name = "child skill $index".toString()
            it.skillId = "child$index".toString()
            skillsService.assignSkillToSkillsGroup(subj1Group1.skillId, it)
        }

        skillsService.addSkill(p1Skills[3], skillsService.userName, new Date() - 1)
        skillsService.addSkill(p1Skills[3], skillsService.userName, new Date())
        skillsService.addSkill(p1Skills[4], skillsService.userName, new Date())

        String otherUser = getRandomUsers(1)[0]
        skillsService.addSkill(p1Skills[5], otherUser, new Date()-1)
        skillsService.addSkill(p1Skills[5], otherUser, new Date())

        Map badge = createBadge(1, 1)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[3].skillId])
        badge.enabled = 'true'
        skillsService.createBadge(badge)

        when:
        def skills = skillsService.getApiAllSubjectsBadgesAndSkills(p1.projectId)
        then:
        skills
        skills.size() == 26
        skills.skillId.sort() == ["child0", "child1", "child2", "skill3", "skill4", "skill5", "skill6", "skill7", "skill4subj2", "skill5subj2", "skill6subj2", "skill7subj2", "skill8subj2", "badge1", "skill1", "skill1subj2", "skill10", "skill10subj2", "skill2", "skill2subj2", "skill3subj2", "skill8", "skill9", "skill9subj2", "TestSubject1", "TestSubject2"].sort()
        skills.skillName.sort() == ["child skill 0", "child skill 1", "child skill 2", "001", "002", "003", "004", "005", "006", "007", "008", "009", "010", "Test Badge 1", "Test Skill 1", "Test Skill 1 Subject2", "Test Skill 10", "Test Skill 10 Subject2", "Test Skill 2", "Test Skill 2 Subject2", "Test Skill 3 Subject2", "Test Skill 8", "Test Skill 9", "Test Skill 9 Subject2", "Test Subject #1", "Test Subject #2"].sort()
        skills.pointIncrement.sort() == [2, 3, 4, 5, 6, 3, 4, 5, 6, 7, 0, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0].sort()
        skills.totalPoints.sort() == [4, 6, 8, 10, 12, 6, 8, 10, 12, 14, 0, 100, 100, 100, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 1340, 1050].sort()
        skills.userCurrentPoints.sort() == [0, 6, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0].sort()
        skills.userAchieved == [false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false]
        skills.subjectId.sort() == ["TestSubject1", "TestSubject1", "TestSubject1", "TestSubject1", "TestSubject1", "TestSubject1", "TestSubject1", "TestSubject1", "TestSubject2", "TestSubject2", "TestSubject2", "TestSubject2", "TestSubject2", null, "TestSubject1", "TestSubject2", "TestSubject1", "TestSubject2", "TestSubject1", "TestSubject2", "TestSubject2", "TestSubject1", "TestSubject1", "TestSubject2", null, null].sort()
        skills.findAll { it.skillType == "Skill" }.skillId.sort() == ["child0", "child1", "child2", "skill3", "skill4", "skill5", "skill6", "skill7", "skill4subj2", "skill5subj2", "skill6subj2", "skill7subj2", "skill8subj2", "skill1", "skill1subj2", "skill10", "skill10subj2", "skill2", "skill2subj2", "skill3subj2", "skill8", "skill9", "skill9subj2"].sort()
        skills.findAll { it.skillType == "Subject" }.skillId.sort() == ["TestSubject1", "TestSubject2"].sort()
        skills.find { it.skillId == "TestSubject1" }.childAchievementCount == 1
        skills.find { it.skillId == "TestSubject1" }.totalChildCount == 13
        skills.find { it.skillId == "TestSubject2" }.childAchievementCount == 0
        skills.find { it.skillId == "TestSubject2" }.totalChildCount == 10
        skills.findAll { it.skillType == "Badge" }.skillId == ["badge1"]
        skills.find { it.skillId == "badge1" }.childAchievementCount == 1
        skills.find { it.skillId == "badge1" }.totalChildCount == 1
        skills.find { it.skillId == "badge1" }.userAchieved  == true
    }

    def "get skills, subjects and badges - multiple badges with dependencies"() {
        def p1 = createProject(1)
        def p1Subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1Subj1, p1Subj1Skills)

        def badge1 = createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Subj1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Subj1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Subj1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Subj1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, badge1.badgeId)

        skillsService.addSkill([projectId: p1.projectId, skillId: p1Subj1Skills.get(0).skillId], skillsService.userName, new Date())
        when:
        def skills = skillsService.getApiAllSubjectsBadgesAndSkills(p1.projectId)
        then:
        skills
        skills.size() == 8
        skills.findAll { it.skillType == "Skill" }.skillId.sort() == ["skill1", "skill2", "skill3", "skill4", "skill5"].sort()
        skills.findAll { it.skillType == "Subject" }.skillId.sort() == ["TestSubject1"].sort()
        skills.find { it.skillId == "TestSubject1" }.childAchievementCount == 1
        skills.find { it.skillId == "TestSubject1" }.totalChildCount == 5
        skills.findAll { Boolean.valueOf(it.userAchieved) }.skillId  == ['skill1']
        skills.findAll { it.skillType == "Badge" }.skillId.sort() == ["badge1", "badge2"].sort()
        skills.find { it.skillId == "badge1" }.childAchievementCount == 1
        skills.find { it.skillId == "badge1" }.totalChildCount == 2
        skills.find { it.skillId == "badge1" }.userAchieved  == false
        skills.find { it.skillId == "badge2" }.childAchievementCount == 0
        skills.find { it.skillId == "badge2" }.totalChildCount == 2
        skills.find { it.skillId == "badge1" }.userAchieved  == false
    }

    def "empty projects"() {
        def p1 = createProject(1)
        skillsService.createProjectAndSubjectAndSkills(p1, null, [])

        when:
        def skills = skillsService.getApiAllSubjectsBadgesAndSkills(p1.projectId)
        then:
        !skills
    }

}
