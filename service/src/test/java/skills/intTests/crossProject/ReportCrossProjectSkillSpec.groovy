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
package skills.intTests.crossProject


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

import static skills.intTests.utils.SkillsFactory.createBadge

class ReportCrossProjectSkillSpec extends DefaultIntSpec {

    def "report cross-project Learning Path skill"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(1).skillId)

        when:
        SkillsService otherUser = createService(getRandomUsers(1)[0])
        def res = otherUser.reportCrossProjectSkill(proj2.projectId, proj1.projectId, proj1_skills.get(1).skillId).body
        then:
        res.projectId == proj1.projectId
        res.skillId == proj1_skills.get(1).skillId
        res.skillApplied == true
        res.pointsEarned == proj1_skills.get(1).pointIncrement
        res.totalPointsEarned == proj1_skills.get(1).pointIncrement
        res.numOccurrencesToCompletion == 1
        res.explanation == "Skill event was applied"
        res.completed.find { it.type == "Skill" }
        res.completed.find { it.type == "Subject" }
        res.completed.find { it.type == "Overall" }
    }

    def "not allowed to report cross-project skill unless it's part of the Learning Path for that project"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)

        def proj3 = SkillsFactory.createProject(3)
        def proj3_subj = SkillsFactory.createSubject(3, 3)
        List<Map> proj3_skills = SkillsFactory.createSkills(2, 3, 3, 100)
        skillsService.createProjectAndSubjectAndSkills(proj3, proj3_subj, proj3_skills)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(1).skillId, proj2.projectId)
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(1).skillId)

        skillsService.shareSkill(proj1.projectId, proj1_skills.get(0).skillId, proj3.projectId)
        skillsService.addLearningPathPrerequisite(proj3.projectId, proj3_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)

        List<SkillsService> otherUsers = getRandomUsers(2).collect { createService(it) }

        // this is ok because the learning path is for project 3
        otherUsers[0].reportCrossProjectSkill(proj3.projectId, proj1.projectId, proj1_skills.get(0).skillId).body

        when:
        otherUsers[1].reportCrossProjectSkill(proj2.projectId, proj1.projectId, proj1_skills.get(0).skillId).body
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("The provided crossProjectId and skillId must be associated with this project either through a Learning Path chain or via a Global Badge")
    }

    def "report cross-project Global Badge skill"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)

        def badge = createBadge()
        skillsService.createGlobalBadge(badge)
        skillsService.assignSkillToGlobalBadge([projectId: proj2.projectId, badgeId: badge.badgeId, skillId: proj2_skills.get(0).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: proj1_skills.get(1).skillId])

        when:
        SkillsService otherUser = createService(getRandomUsers(1)[0])
        def res = otherUser.reportCrossProjectSkill(proj2.projectId, proj1.projectId, proj1_skills.get(1).skillId).body
        then:
        res.projectId == proj1.projectId
        res.skillId == proj1_skills.get(1).skillId
        res.skillApplied == true
        res.pointsEarned == proj1_skills.get(1).pointIncrement
        res.totalPointsEarned == proj1_skills.get(1).pointIncrement
        res.numOccurrencesToCompletion == 1
        res.explanation == "Skill event was applied"
        res.completed.find { it.type == "Skill" }
        res.completed.find { it.type == "Subject" }
        res.completed.find { it.type == "Overall" }
    }

    def "not allowed to report cross-project skill unless it's part of the Global Badge and the project belongs to the same badge"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)

        def proj3 = SkillsFactory.createProject(3)
        def proj3_subj = SkillsFactory.createSubject(3, 3)
        List<Map> proj3_skills = SkillsFactory.createSkills(2, 3, 3, 100)
        skillsService.createProjectAndSubjectAndSkills(proj3, proj3_subj, proj3_skills)

        def proj4 = SkillsFactory.createProject(4)
        skillsService.createProject(proj4)

        def badge = createBadge(20, 1)
        skillsService.createGlobalBadge(badge)
        skillsService.assignSkillToGlobalBadge([projectId: proj2.projectId, badgeId: badge.badgeId, skillId: proj2_skills.get(0).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badge.badgeId, skillId: proj1_skills.get(1).skillId])

        def badge2 = createBadge(21, 2)
        skillsService.createGlobalBadge(badge2)
        skillsService.assignSkillToGlobalBadge([projectId: proj3.projectId, badgeId: badge2.badgeId, skillId: proj3_skills.get(0).skillId])
        skillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badge2.badgeId, skillId: proj1_skills.get(0).skillId])

        def badge3 = createBadge(22, 3)
        skillsService.createGlobalBadge(badge3)
        skillsService.assignProjectLevelToGlobalBadge([projectId: proj4.projectId, badgeId: badge3.badgeId, level: "1"])
        skillsService.assignSkillToGlobalBadge([projectId: proj1.projectId, badgeId: badge3.badgeId, skillId: proj1_skills.get(0).skillId])

        List<SkillsService> otherUsers = getRandomUsers(3).collect { createService(it) }
        // proj 3 associated by skill
        otherUsers[0].reportCrossProjectSkill(proj3.projectId, proj1.projectId, proj1_skills.get(0).skillId).body
        // proj 4 associated by level
        otherUsers[1].reportCrossProjectSkill(proj4.projectId, proj1.projectId, proj1_skills.get(0).skillId).body

        when:
        otherUsers[2].reportCrossProjectSkill(proj2.projectId, proj1.projectId, proj1_skills.get(0).skillId).body
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("The provided crossProjectId and skillId must be associated with this project either through a Learning Path chain or via a Global Badge")
    }
}
