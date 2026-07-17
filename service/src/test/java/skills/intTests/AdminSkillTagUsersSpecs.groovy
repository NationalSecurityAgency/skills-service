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
package skills.intTests


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class AdminSkillTagUsersSpecs extends DefaultIntSpec {

    def "users with various achievements"() {
        String tagValue = "New Tag"
        String tagId = 'newtag'

        def proj1 = SkillsFactory.createProject(1)
        def proj1Subj1 = SkillsFactory.createSubject(1, 1)
        def proj1Subj1Skills = SkillsFactory.createSkills(8, 1, 1, 10, 2)
        proj1Subj1Skills.each {
            it.pointIncrementInterval = 0
        }
        skillsService.createProjectAndSubjectAndSkills(proj1, proj1Subj1, proj1Subj1Skills[0..3])
        def proj1Subj1Group = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createSkill(proj1Subj1Group)
        proj1Subj1Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj1Subj1Group.skillId, it)
        }
        def proj1Subj2 = SkillsFactory.createSubject(1, 2)
        def proj1Subj2Skills = SkillsFactory.createSkills(8, 1, 2, 5, 3)
        proj1Subj2Skills.each {
            it.pointIncrementInterval = 0
        }
        skillsService.createProjectAndSubjectAndSkills(null, proj1Subj2, proj1Subj2Skills[0..3])
        def proj1Subj2Group = SkillsFactory.createSkillsGroup(1, 2, 12)
        skillsService.createSkill(proj1Subj2Group)
        proj1Subj2Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj1Subj2Group.skillId, it)
        }

        skillsService.addTagToSkills(proj1.projectId, [
                proj1Subj1Skills[0].skillId, // 10 * 2
                proj1Subj1Skills[1].skillId, // 10 * 2
                proj1Subj1Skills[2].skillId, // 10 * 2
                proj1Subj1Skills[7].skillId, // 10 * 2
                // 80pts
                proj1Subj2Skills[0].skillId, // 5 * 3
                proj1Subj2Skills[1].skillId, // 5 * 3
                proj1Subj2Skills[2].skillId, // 5 * 3
                proj1Subj2Skills[6].skillId, // 5 * 3
                proj1Subj2Skills[7].skillId, // 5 * 3
                // 75 pts
                // total = 155pts
        ], tagValue, tagId)

        def proj2 = SkillsFactory.createProject(2)
        def proj2Subj1 = SkillsFactory.createSubject(2, 1)
        def proj2Subj1Skills = SkillsFactory.createSkills(8, 2, 1, 25, 4)
        proj2Subj1Skills.each {
            it.pointIncrementInterval = 0
        }
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2Subj1, proj2Subj1Skills[0..3])
        def proj2Subj1Group = SkillsFactory.createSkillsGroup(2, 1, 11)
        skillsService.createSkill(proj2Subj1Group)
        proj2Subj1Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj2Subj1Group.skillId, it)
        }
        def proj2Subj2 = SkillsFactory.createSubject(2, 2)
        def proj2Subj2Skills = SkillsFactory.createSkills(8, 2, 2, 15, 3)
        proj2Subj2Skills.each {
            it.pointIncrementInterval = 0
        }
        skillsService.createProjectAndSubjectAndSkills(null, proj2Subj2, proj2Subj2Skills[0..3])
        def proj2Subj2Group = SkillsFactory.createSkillsGroup(2, 2, 12)
        skillsService.createSkill(proj2Subj2Group)
        proj2Subj2Skills[4..7].each {
            skillsService.assignSkillToSkillsGroup(proj2Subj2Group.skillId, it)
        }
        skillsService.addTagToSkills(proj2.projectId, [
                proj2Subj1Skills[0].skillId,
                proj2Subj1Skills[4].skillId,
                proj2Subj2Skills[0].skillId,
                proj2Subj2Skills[4].skillId
        ], tagValue, tagId)

        List<SkillsService> users = getRandomUsers(5).collect { createService(it)}
        // Proj1 --------------------
        // user 1 achieved everything
        proj1Subj1Skills.each {Map skill -> 2.times {users[0].addSkill(skill) }}
        proj1Subj2Skills.each { Map skill -> 3.times { users[0].addSkill(skill) } }
        // user 2 some achievement
        proj1Subj1Skills.each {Map skill -> users[1].addSkill(skill) }
        proj1Subj2Skills.each { Map skill -> 2.times {users[1].addSkill(skill) }}
        // user 3 less achievements
        users[2].addSkill(proj1Subj1Skills[0])
        users[2].addSkill(proj1Subj1Skills[7])
        users[2].addSkill(proj1Subj2Skills[1])
        users[2].addSkill(proj1Subj2Skills[6])
        // user 4 achieved in non-tagged skills
        users[3].addSkill(proj1Subj1Skills[3])
        users[3].addSkill(proj1Subj2Skills[3])

        // Proj2 --------------------
        // user 1 achieved something
        users[0].addSkill(proj2Subj1Skills[1])
        users[0].addSkill(proj2Subj1Skills[4])
        users[0].addSkill(proj2Subj2Skills[2])
        users[0].addSkill(proj2Subj2Skills[4])

        // user 2 achieved everything
        proj2Subj1Skills.each {Map skill -> 4.times {users[1].addSkill(skill) }}
        proj2Subj2Skills.each { Map skill -> 3.times { users[1].addSkill(skill) } }

        // user 5 achieved some stuff too
        // user 1 achieved something
        users[4].addSkill(proj2Subj1Skills[2])
        users[4].addSkill(proj2Subj1Skills[4])
        users[4].addSkill(proj2Subj1Skills[4])
        users[4].addSkill(proj2Subj2Skills[3])
        users[4].addSkill(proj2Subj2Skills[4])

        when:
        def p1Res = skillsService.getSkillTagUsers(proj1.projectId, tagId, 10, 1, 'firstUpdated', false)
        def p2Res = skillsService.getSkillTagUsers(proj2.projectId, tagId, 10, 1, 'firstUpdated', false)
        then:
        p1Res.count == 3
        p1Res.totalPoints == 155
        def p1Data = p1Res.data
        p1Data[0].totalPoints == 30
        p1Data[0].userId == users[2].userName
        p1Data[1].totalPoints == 90
        p1Data[1].userId == users[1].userName
        p1Data[2].totalPoints == 155
        p1Data[2].userId == users[0].userName

        p2Res.count == 3
        p2Res.totalPoints == 290
        def p2Data = p2Res.data
        p2Data[0].totalPoints == 65
        p2Data[0].userId == users[4].userName
        p2Data[1].totalPoints == 290
        p2Data[1].userId == users[1].userName
        p2Data[2].totalPoints == 40
        p2Data[2].userId == users[0].userName

    }
}