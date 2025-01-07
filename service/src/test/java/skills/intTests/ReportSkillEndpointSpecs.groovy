/**
 * Copyright 2024 SkillTree
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

class ReportSkillEndpointSpecs extends DefaultIntSpec {

    def "skill points earned with multiple iterations for multiple users"() {
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        def skills = SkillsFactory.createSkills(10, 1, 1, 10, 3)
        skills.each { it.pointIncrementInterval = 0 }
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> userIds = getRandomUsers(3)
        List<SkillsService> users = userIds.collect { userId -> createService(userId) }

        when:
        def user1Res1 = users[0].addSkill([projectId: proj.projectId, skillId: skills[0].skillId]).body
        def user2Res1 = users[1].addSkill([projectId: proj.projectId, skillId: skills[0].skillId]).body
        def user1Res2 = users[0].addSkill([projectId: proj.projectId, skillId: skills[0].skillId]).body
        def user2Res2 = users[1].addSkill([projectId: proj.projectId, skillId: skills[0].skillId]).body
        def user1Res3 = users[0].addSkill([projectId: proj.projectId, skillId: skills[0].skillId]).body
        def user2Res3 = users[1].addSkill([projectId: proj.projectId, skillId: skills[0].skillId]).body

        def user3Res1 = users[2].addSkill([projectId: proj.projectId, skillId: skills[0].skillId]).body
        def user3Res2 = users[2].addSkill([projectId: proj.projectId, skillId: skills[0].skillId]).body
        def user3Res3 = users[2].addSkill([projectId: proj.projectId, skillId: skills[0].skillId]).body


        def checkUserRes = { def res, int totalPointsEarned, List<String> completedNames ->
            assert res.skillApplied == true
            assert res.totalPoints == 30
            assert res.numOccurrencesToCompletion == 3
            assert res.pointsEarned == 10
            assert res.totalPointsEarned == totalPointsEarned
            assert res.projectId == proj.projectId
            assert res.name == skills[0].name
            assert res.skillId == skills[0].skillId
            assert res.explanation == "Skill event was applied"
            if (completedNames) {
                assert res.completed.name.sort() == completedNames.sort()
            } else {
                assert !res.completed
            }

            return true
        }

        then:
        checkUserRes(user1Res1, 10, null)
        checkUserRes(user1Res2, 20, null)
        checkUserRes(user1Res3, 30, [skills[0].name, "OVERALL", subj.name])

        checkUserRes(user2Res1, 10, null)
        checkUserRes(user2Res2, 20, null)
        checkUserRes(user2Res3, 30, [skills[0].name, "OVERALL", subj.name])

        checkUserRes(user3Res1, 10, null)
        checkUserRes(user3Res2, 20, null)
        checkUserRes(user3Res3, 30, [skills[0].name, "OVERALL", subj.name])
    }
}
