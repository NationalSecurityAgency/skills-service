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
import skills.intTests.TestDates
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.skill.UsagePostAchievementMetricsBuilder
import skills.services.LockingService
import skills.services.UserEventService
import skills.storage.repos.UserAchievedLevelRepo
import spock.lang.IgnoreRest

class UsagePostAchievementMetricsBuilderSpec extends DefaultIntSpec {

    @Autowired
    UsagePostAchievementMetricsBuilder builder

    @Autowired
    UserAchievedLevelRepo temp

    @Autowired
    UserEventService userEventService

    @IgnoreRest
    def "produces accurate post achievement usage counts"() {
        //simple case, not taking into account event compaction boundaries
        def proj = SkillsFactory.createProject()
        def skill = SkillsFactory.createSkill(1, 1, 1, 0, 2,  )
        skill.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkill(skill)

        def users = getRandomUsers(7)

        // user 1 - achieved and used after
        assert skillsService.addSkill(skill, users[0], new Date() - 4).body.skillApplied
        assert skillsService.addSkill(skill, users[0], new Date() - 3).body.skillApplied
        assert !skillsService.addSkill(skill, users[0], new Date()).body.skillApplied

        // user 2 - did not achieve
        assert skillsService.addSkill(skill, users[1], new Date()).body.skillApplied

        // user 3 - achieved but did not use after
        assert skillsService.addSkill(skill, users[2], new Date() - 2).body.skillApplied
        assert skillsService.addSkill(skill, users[2], new Date() - 1).body.skillApplied

        // user 4 - achieved and used after
        assert skillsService.addSkill(skill, users[3], new Date() - 9).body.skillApplied
        assert skillsService.addSkill(skill, users[3], new Date() - 8).body.skillApplied
        assert !skillsService.addSkill(skill, users[3], new Date() - 7).body.skillApplied
        assert !skillsService.addSkill(skill, users[3], new Date() - 6).body.skillApplied
        assert !skillsService.addSkill(skill, users[3], new Date() - 5).body.skillApplied

        // user 5 and 6 - did not achieve
        assert skillsService.addSkill(skill, users[5], new Date()).body.skillApplied
        assert skillsService.addSkill(skill, users[6], new Date()).body.skillApplied

        when:
        def props = ["skillId": skill.skillId]
        def result = builder.build(proj.projectId, builder.id, props)

        then:
        result
        result.totalUsersAchieved == 3
        result.usersPostAchievement == 2
    }

    def "skill usage post achievement crossing event compaction boundaries"() {
        //figure out how to construct the timeline to satisfy this test
        LockingService mock = Mock()

        def proj = SkillsFactory.createProject()
        def skill = SkillsFactory.createSkill(1, 1, 1, 0, 10  )
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 10 )

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)

        def allRandom = getRandomUsers(4)
        def users = allRandom.subList(0, 3)

        TestDates testDates = new TestDates()
        Date last = null
        (13..0).each {
            if (it > 3) {
                Date date = testDates.getStartOfPreviousWeek().minusDays(it).toDate()
                def lastRes
                users.each {
                    lastRes = skillsService.addSkill(skill, it, date)
                }
                skillsService.addSkill(skill2, allRandom.last(), date)
                last = date
                println "${date} ${lastRes}"
            } else {
                //we need to ensure that these fall into the same week that the achievement occurred
                //ideally after the achievement when the day week permits
                Date date = testDates.getDateWithinWeek(last, true).toDate()
                def res = skillsService.addSkill(skill, users[0], date)
                skillsService.addSkill(skill2, allRandom.last(), date)
                println "$date $res"
            }
        }

        userEventService.lockingService = mock
        userEventService.compactDailyEvents()

        when:
        def props = ["skillId": skill.skillId]
        def result = builder.build(proj.projectId, builder.id, props)

        then:
        result
        result.totalUsersAchieved == 3
        //because the post achievement events were compacted into a weekly event whose start of week date
        //falls before the achievement
        result.usersPostAchievement == 0
    }


}
