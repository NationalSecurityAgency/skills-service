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


import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.metrics.builders.skill.UsagePostAchievementUsersBuilder

import java.time.Instant
import java.time.LocalDateTime

class UsagePostAchievementUsersBuilderSpec extends DefaultIntSpec {

    @Autowired
    UsagePostAchievementUsersBuilder builder

    def "produces accurate post achievement user list"() {
        //simple case, not taking into account event compaction boundaries
        def proj = SkillsFactory.createProject()
        def skill = SkillsFactory.createSkill(1, 1, 1, 0, 2,  )
        skill.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkill(skill)

        def users = getRandomUsers(7)
        Date date = Date.from(Instant.parse("2024-07-01T12:00:00.000Z"));
        def dateFiveDaysAgo = date - 5

        // user 1 - achieved and used after
        assert skillsService.addSkill(skill, users[0], date - 4).body.skillApplied
        assert skillsService.addSkill(skill, users[0], date - 3).body.skillApplied
        assert !skillsService.addSkill(skill, users[0], date).body.skillApplied

        // user 2 - did not achieve
        assert skillsService.addSkill(skill, users[1], date).body.skillApplied

        // user 3 - achieved but did not use after
        assert skillsService.addSkill(skill, users[2], date - 2).body.skillApplied
        assert skillsService.addSkill(skill, users[2], date - 1).body.skillApplied

        // user 4 - achieved and used after
        assert skillsService.addSkill(skill, users[3], date - 9).body.skillApplied
        assert skillsService.addSkill(skill, users[3], date - 8).body.skillApplied
        assert !skillsService.addSkill(skill, users[3], date - 7).body.skillApplied
        assert !skillsService.addSkill(skill, users[3], date - 6).body.skillApplied
        assert !skillsService.addSkill(skill, users[3], date - 5).body.skillApplied

        // user 5 and 6 - did not achieve
        assert skillsService.addSkill(skill, users[5], date).body.skillApplied
        assert skillsService.addSkill(skill, users[6], date).body.skillApplied

        when:
        def props = ["skillId": skill.skillId, "page": 1, "pageSize": 5, "sortBy": "date", "sortDesc": true]
        def result = builder.build(proj.projectId, builder.id, props)

        skillsService.archiveUsers([users[3]], proj.projectId)

        def resultAfterArchive = builder.build(proj.projectId, builder.id, props)

        then:
        result
        result.totalCount == 2
        result.users[0].userId == users[0]
        result.users[0].count == 1
        result.users[0].date.toString() == date.format('YYYY-MM-dd 00:00:00.0')
        result.users[1].userId == users[3]
        result.users[1].count == 3
        result.users[1].date.toString() == dateFiveDaysAgo.format('YYYY-MM-dd 00:00:00.0')

        resultAfterArchive
        resultAfterArchive.totalCount == 1
        resultAfterArchive.users[0].userId == users[0]
        resultAfterArchive.users[0].count == 1
        resultAfterArchive.users[0].date.toString() == date.format('YYYY-MM-dd 00:00:00.0')
    }

    def "pages appropriately"() {
        def proj = SkillsFactory.createProject()
        def skill = SkillsFactory.createSkill(1, 1, 1, 0, 2,  )
        skill.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkill(skill)

        def users = getRandomUsers(7)

        // user 1 - achieved and used after
        users.forEach( it -> {
            // user 1 - achieved and used after
            assert skillsService.addSkill(skill, it, new Date() - 4).body.skillApplied
            assert skillsService.addSkill(skill, it, new Date() - 3).body.skillApplied
            assert !skillsService.addSkill(skill, it, new Date()).body.skillApplied
        })

        when:
        def props = ["skillId": skill.skillId, "page": 1, "pageSize": 5, "sortBy": "userId", "sortDesc": false]
        def pageOne = builder.build(proj.projectId, builder.id, props)
        props.page = 2
        def pageTwo = builder.build(proj.projectId, builder.id, props)

        then:
        pageOne
        pageOne.totalCount == 7
        pageOne.users.size() == 5
        pageTwo
        pageTwo.totalCount == 7
        pageTwo.users.size() == 2
    }

    def "events on the same day are counted appropriately"() {
        //simple case, not taking into account event compaction boundaries
        def proj = SkillsFactory.createProject()
        def skill = SkillsFactory.createSkill(1, 1, 1, 0, 2, 0)
        skill.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkill(skill)

        def users = getRandomUsers(1)
        def date = new Date()

        assert skillsService.addSkill(skill, users[0], new Date() - 5).body.skillApplied
        assert skillsService.addSkill(skill, users[0], new Date() - 4).body.skillApplied
        for(def x = 0; x < 15; x++) {
            assert !skillsService.addSkill(skill, users[0], date).body.skillApplied
        }

        when:
        def props = ["skillId": skill.skillId, "page": 1, "pageSize": 5, "sortBy": "date", "sortDesc": true]
        def result = builder.build(proj.projectId, builder.id, props)

        then:
        result
        result.totalCount == 1
        result.users[0].userId == users[0]
        result.users[0].count == 15
        result.users[0].date.toString() == date.format('yyyy-MM-dd 00:00:00.0')
    }
}
