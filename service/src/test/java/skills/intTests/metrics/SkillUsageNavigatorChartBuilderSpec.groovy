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
package skills.intTests.metrics

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class SkillUsageNavigatorChartBuilderSpec extends DefaultIntSpec {

    def "skill usage navigator"() {
        String userId = "user1"
        List<Map> skills = SkillsFactory.createSkills(10)
//        skills.each { it.numPerformToCompletion = 1 }

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)

        println skillsService.addSkill([projectId: skills[0].projectId, skillId: skills[0].skillId], userId, new Date())
        println skillsService.addSkill([projectId: skills[0].projectId, skillId: skills[1].skillId], userId, new Date())

        when:
        def res = skillsService.getMetricsData(skills[0].projectId, "skillUsageNavigatorChartBuilder")
        res.each {
            println it
        }
        then:
        res
    }
}
