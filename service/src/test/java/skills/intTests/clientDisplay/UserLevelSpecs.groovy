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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import spock.lang.IgnoreRest

class UserLevelSpecs extends DefaultIntSpec {

    def "get user level when there are not levels achieved"() {
        when:
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        then:
        skillsService.getUserLevel(proj1.projectId, userId) == 0
    }

    def "get user level when there are achievements"() {
        String userId = "user1"

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(10, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)


        when:
        List<Integer> levels = []
        (0..9).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], userId, new Date())
            levels.add(skillsService.getUserLevel(proj1.projectId, userId))
        }

        then:
        levels == [1, 1, 2, 2, 3, 3, 4, 4, 4, 5]
    }

    def "get levels - multiple subjects"() {
        String userId1 = "user1"

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def subj1 = SkillsFactory.createSubject(1, 2)
        def skills = SkillsFactory.createSkills(2, 1, 1)
        def skills_subj1 = SkillsFactory.createSkills(1, 1, 2)

        skills.each {
            it.pointIncrement = 10
            it.numPerformToCompletion = 5
        }
        skills_subj1.each {
            it.pointIncrement = 10
            it.numPerformToCompletion = 5
        }
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills)
        skillsService.createSkills(skills_subj1)

        /**
         Level 1 => 15
         Level 2 => 37
         Level 3 => 67
         Level 4 => 100
         Level 5 => 138
         */

        when:
        def user1_level_call1 = skillsService.getUserLevel(proj.projectId, userId1)
        def user1_summary1 = skillsService.getSkillSummary(userId1, proj.projectId)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId1, new Date())
        def user1_summary2 = skillsService.getSkillSummary(userId1, proj.projectId)
        def user1_level_call2 = skillsService.getUserLevel(proj.projectId, userId1)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], userId1, new Date())
        def user1_summary3 = skillsService.getSkillSummary(userId1, proj.projectId)
        def user1_level_call3 = skillsService.getUserLevel(proj.projectId, userId1)

        then:
        user1_summary1.skillsLevel == 0
        user1_summary1.points == 0
        user1_level_call1 == 0
        user1_summary2.points == 10
        user1_summary2.skillsLevel == 0
        user1_level_call2 == 0
        user1_summary3.skillsLevel == 1
        user1_summary3.points == 20
        user1_level_call3 == 1
    }
}
