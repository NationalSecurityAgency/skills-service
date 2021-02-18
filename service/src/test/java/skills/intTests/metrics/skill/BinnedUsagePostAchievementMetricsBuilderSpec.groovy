/**
 * Copyright 2021 SkillTree
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
package skills.intTests.metrics.skill

import groovy.time.TimeCategory
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.skill.BinnedUsagePostAchievementMetricsBuilder
import skills.services.UserEventService
import skills.storage.repos.UserAchievedLevelRepo

class BinnedUsagePostAchievementMetricsBuilderSpec extends DefaultIntSpec {

    @Autowired
    BinnedUsagePostAchievementMetricsBuilder builder

    @Autowired
    UserAchievedLevelRepo temp

    @Autowired
    UserEventService userEventService

    def "produces accurate post achievement binned usage counts"() {
        //simple case, not taking into account event compaction boundaries
        def proj = SkillsFactory.createProject()
        def skill = SkillsFactory.createSkill(1, 1, 1, 0, 10,  )

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkill(skill)

        def users = getRandomUsers(10)

        use(TimeCategory){
            (60..0).each {
                Date date = it.days.ago
                if (it >= 50) {
                    users.each {
                        skillsService.addSkill(skill, it, date)
                    }
                } else if (it >= 45) {
                    users.subList(0, 8).each {
                        skillsService.addSkill(skill, it, date)
                    }
                } else if (it >= 30) {
                    users.subList(0, 5).each {
                        skillsService.addSkill(skill, it, date)
                    }
                } else {
                    skillsService.addSkill(skill, users[0], date)
                }
            }
        }

        when:
        def props = ["skillId": skill.skillId]
        def result = builder.build(proj.projectId, builder.id, props)

        then:
        result[0].label == "<5"
        result[0].count == 2
        result[1].label == ">=5 <20"
        result[1].count == 3
        result[2].label == ">=20 <50"
        result[2].count == 4
        result[3].label == ">=50"
        result[3].count == 1
    }


}
