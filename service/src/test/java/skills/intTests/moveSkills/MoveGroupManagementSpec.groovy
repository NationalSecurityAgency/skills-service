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
package skills.intTests.moveSkills


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException

import static skills.intTests.utils.SkillsFactory.*

class MoveGroupManagementSpec extends DefaultIntSpec {

    def "move a single group into another subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(4, 1, 1, 100)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1Skills[0], p1subj1g1])
        p1Skills[1..3].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        when:
        def subj1GroupSkillsBefore = skillsService.getSkillSummary(null, p1.projectId, p1subj1.subjectId)
        def subj2GroupSkillsBefore = skillsService.getSkillSummary(null, p1.projectId, p1subj2.subjectId)
        def groupSkillsBefore = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)
        skillsService.moveSkills(p1.projectId, [p1subj1g1.skillId], p1subj2.subjectId)

        def subj1GroupSkills = skillsService.getSkillSummary(null, p1.projectId, p1subj1.subjectId)
        def subj2GroupSkills = skillsService.getSkillSummary(null, p1.projectId, p1subj2.subjectId)
        def groupSkills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)
        then:
        subj1GroupSkillsBefore.skills.skillId == [p1Skills[0].skillId, p1subj1g1.skillId]
        def groupBefore = subj1GroupSkillsBefore.skills.find { it.skillId == p1subj1g1.skillId }
        groupBefore.children.skillId == p1Skills[1..3].skillId
        !subj2GroupSkillsBefore.skills
        groupSkillsBefore.skillId == p1Skills[1..3].skillId
        groupSkillsBefore.groupId == [p1subj1g1.skillId, p1subj1g1.skillId, p1subj1g1.skillId]

        subj1GroupSkills.skills.skillId == [p1Skills[0].skillId]
        subj2GroupSkills.skills.skillId == [p1subj1g1.skillId].sort()
        subj2GroupSkills.skills[0].children.skillId == p1Skills[1..3].skillId
        groupSkills.skillId == p1Skills[1..3].skillId
        groupSkills.groupId == [p1subj1g1.skillId, p1subj1g1.skillId, p1subj1g1.skillId]
    }

    def "move multiple skills and a single group from subject with multiple skills and 1 group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        
        // Create subject with 5 skills and 1 group containing 2 of the skills
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1Skills[0], p1Skills[1], p1subj1g1])
        p1Skills[2..3].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        skillsService.createSkill(p1Skills[4])

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        when:
        def subj1GroupSkillsBefore = skillsService.getSkillSummary(null, p1.projectId, p1subj1.subjectId)
        def subj2GroupSkillsBefore = skillsService.getSkillSummary(null, p1.projectId, p1subj2.subjectId)
        def groupSkillsBefore = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)
        
        // Move 2 individual skills and 1 group to subject 2
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId, p1Skills[1].skillId, p1subj1g1.skillId], p1subj2.subjectId)

        def subj1GroupSkills = skillsService.getSkillSummary(null, p1.projectId, p1subj1.subjectId)
        def subj2GroupSkills = skillsService.getSkillSummary(null, p1.projectId, p1subj2.subjectId)
        def groupSkills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)
        
        then:
        // Verify initial state: subject 1 has 5 skills total (3 individual + 1 group with 2 skills)
        subj1GroupSkillsBefore.skills.skillId.containsAll([p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[4].skillId, p1subj1g1.skillId])
        def groupBefore = subj1GroupSkillsBefore.skills.find { it.skillId == p1subj1g1.skillId }
        groupBefore.children.skillId == p1Skills[2..3].skillId
        !subj2GroupSkillsBefore.skills
        groupSkillsBefore.skillId == p1Skills[2..3].skillId
        groupSkillsBefore.groupId == [p1subj1g1.skillId, p1subj1g1.skillId]

        // Verify after move: subject 1 has only 1 remaining skill
        subj1GroupSkills.skills.skillId == [p1Skills[4].skillId]
        
        // Verify subject 2 has the moved skills and group
        subj2GroupSkills.skills.skillId.containsAll([p1Skills[0].skillId, p1Skills[1].skillId, p1subj1g1.skillId])
        def movedGroup = subj2GroupSkills.skills.find { it.skillId == p1subj1g1.skillId }
        movedGroup.children.skillId == p1Skills[2..3].skillId
        
        // Verify group skills are still intact
        groupSkills.skillId == p1Skills[2..3].skillId
        groupSkills.groupId == [p1subj1g1.skillId, p1subj1g1.skillId]
    }

    def "move some skills and groups from subject with multiple skills and groups while others remain"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(9, 1, 1, 100)
        def p1subj1g1 = createSkillsGroup(1, 1, 20)
        def p1subj1g2 = createSkillsGroup(1, 1, 21)
        def p1subj1g3 = createSkillsGroup(1, 1, 22)
        
        // Create subject with 9 skills and 3 groups:
        // - Individual skills: skill1, skill2, skill3, skill9
        // - Group 1: skill4, skill5
        // - Group 2: skill6
        // - Group 3: skill7, skill8
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1Skills[0], p1Skills[1], p1Skills[2], p1Skills[8], p1subj1g1, p1subj1g2, p1subj1g3])
        p1Skills[3..4].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }
        p1Skills[5..5].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g2.skillId, it)
        }
        p1Skills[6..7].each {
            skillsService.assignSkillToSkillsGroup(p1subj1g3.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        when:
        def subj1GroupSkillsBefore = skillsService.getSkillSummary(null, p1.projectId, p1subj1.subjectId)
        def subj2GroupSkillsBefore = skillsService.getSkillSummary(null, p1.projectId, p1subj2.subjectId)
        def group1SkillsBefore = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)
        def group2SkillsBefore = skillsService.getSkillsForGroup(p1.projectId, p1subj1g2.skillId)
        def group3SkillsBefore = skillsService.getSkillsForGroup(p1.projectId, p1subj1g3.skillId)
        
        // Move skill1, skill3, skill9, group1 (containing skill4, skill5), and group3 (containing skill7, skill8) to subject 2
        // Leave skill2 and group2 (containing skill6) in subject 1
        skillsService.moveSkills(p1.projectId, [p1Skills[0].skillId, p1Skills[2].skillId, p1Skills[8].skillId, p1subj1g1.skillId, p1subj1g3.skillId], p1subj2.subjectId)

        def subj1GroupSkills = skillsService.getSkillSummary(null, p1.projectId, p1subj1.subjectId)
        def subj2GroupSkills = skillsService.getSkillSummary(null, p1.projectId, p1subj2.subjectId)
        def group1Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g1.skillId)
        def group2Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g2.skillId)
        def group3Skills = skillsService.getSkillsForGroup(p1.projectId, p1subj1g3.skillId)
        
        then:
        // Verify initial state: subject 1 has all skills and groups
        subj1GroupSkillsBefore.skills.skillId.containsAll([p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId, p1Skills[8].skillId, p1subj1g1.skillId, p1subj1g2.skillId, p1subj1g3.skillId])
        def group1Before = subj1GroupSkillsBefore.skills.find { it.skillId == p1subj1g1.skillId }
        group1Before.children.skillId == p1Skills[3..4].skillId
        def group2Before = subj1GroupSkillsBefore.skills.find { it.skillId == p1subj1g2.skillId }
        group2Before.children.skillId == [p1Skills[5].skillId]
        def group3Before = subj1GroupSkillsBefore.skills.find { it.skillId == p1subj1g3.skillId }
        group3Before.children.skillId == p1Skills[6..7].skillId
        !subj2GroupSkillsBefore.skills
        group1SkillsBefore.skillId == p1Skills[3..4].skillId
        group2SkillsBefore.skillId == [p1Skills[5].skillId]
        group3SkillsBefore.skillId == p1Skills[6..7].skillId

        // Verify after move: subject 1 has skill2 and group2 (with skill6)
        subj1GroupSkills.skills.skillId.containsAll([p1Skills[1].skillId, p1subj1g2.skillId])
        !subj1GroupSkills.skills.skillId.contains(p1Skills[0].skillId)
        !subj1GroupSkills.skills.skillId.contains(p1Skills[2].skillId)
        !subj1GroupSkills.skills.skillId.contains(p1Skills[8].skillId)
        !subj1GroupSkills.skills.skillId.contains(p1subj1g1.skillId)
        !subj1GroupSkills.skills.skillId.contains(p1subj1g3.skillId)
        
        // Verify subject 2 has skill1, skill3, skill9, group1 (with skill4, skill5), and group3 (with skill7, skill8)
        subj2GroupSkills.skills.skillId.containsAll([p1Skills[0].skillId, p1Skills[2].skillId, p1Skills[8].skillId, p1subj1g1.skillId, p1subj1g3.skillId])
        def movedGroup1 = subj2GroupSkills.skills.find { it.skillId == p1subj1g1.skillId }
        movedGroup1.children.skillId == p1Skills[3..4].skillId
        def movedGroup3 = subj2GroupSkills.skills.find { it.skillId == p1subj1g3.skillId }
        movedGroup3.children.skillId == p1Skills[6..7].skillId
        
        // Verify group skills are still intact
        group1Skills.skillId == p1Skills[3..4].skillId
        group2Skills.skillId == [p1Skills[5].skillId]
        group3Skills.skillId == p1Skills[6..7].skillId
    }

    def "attempting to move a group into another group throws an exception"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(4, 1, 1, 100)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        def p1subj1g2 = createSkillsGroup(1, 1, 9)
        
        // Create subject with 2 individual skills and 2 groups
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1Skills[0], p1Skills[1], p1subj1g1, p1subj1g2])
        skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, p1Skills[2])
        skillsService.assignSkillToSkillsGroup(p1subj1g2.skillId, p1Skills[3])

        when:
        // Attempt to move group1 into group2 - this should fail
        skillsService.moveSkills(p1.projectId, [p1subj1g1.skillId], p1subj1.subjectId, p1subj1g2.skillId)
        
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot move a group into another group")
    }

}
