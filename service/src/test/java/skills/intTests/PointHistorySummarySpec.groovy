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
package skills.intTests

import groovy.time.TimeCategory
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.skillLoading.SkillsLoader
import skills.skillLoading.model.UserPointHistorySummary

class PointHistorySummarySpec extends DefaultIntSpec{

    @Autowired
    SkillsLoader skillsLoader

    def "limits results to the supplied showHistoryForNumDays"(){
        //create skills, achieve skil
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        List<String> randos = getRandomUsers(2)

        use(TimeCategory) {
            skillsService.addSkill(skills[0], randos[0], 10.days.ago)
            skillsService.addSkill(skills[0], randos[1], 10.days.ago)

            skillsService.addSkill(skills[1], randos[0], 8.days.ago)
            skillsService.addSkill(skills[1], randos[1], 8.days.ago)

            skillsService.addSkill(skills[2], randos[0], 6.days.ago)
            skillsService.addSkill(skills[2], randos[1], 6.days.ago)

            skillsService.addSkill(skills[3], randos[0], 5.days.ago)
            skillsService.addSkill(skills[3], randos[1], 5.days.ago)

            skillsService.addSkill(skills[4], randos[0], 1.days.ago)
            skillsService.addSkill(skills[4], randos[1], 1.days.ago)
            skillsService.addSkill(skills[5], randos[0], 1.days.ago)
            skillsService.addSkill(skills[5], randos[1], 1.days.ago)
            skillsService.addSkill(skills[6], randos[0], 1.days.ago)
            skillsService.addSkill(skills[6], randos[1], 1.days.ago)
        }

        when:
        UserPointHistorySummary pointHistorySummary = skillsLoader.loadPointHistorySummary(proj.projectId, randos[0], 2)
        Date twoDaysAgo
        use(TimeCategory) {
            twoDaysAgo = 2.days.ago.clearTime()
        }

        then:
        pointHistorySummary
        pointHistorySummary.achievements.size() == 1
        pointHistorySummary.achievements.each {
            assert it.achievedOn >= twoDaysAgo
        }
        pointHistorySummary.pointsHistory.each {
            assert it.dayPerformed >= twoDaysAgo
        }

    }
}
