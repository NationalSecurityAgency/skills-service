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
import skills.metrics.builders.skill.NoUsagePostAchievementUsersBuilder
import skills.metrics.builders.skill.UsagePostAchievementUsersBuilder

class NoUsagePostAchievementUsersBuilderSpec extends DefaultIntSpec {

    @Autowired
    NoUsagePostAchievementUsersBuilder builder

    def "produces accurate post achievement user list"() {
        //simple case, not taking into account event compaction boundaries
        def proj = SkillsFactory.createProject()
        def skill = SkillsFactory.createSkill(1, 1, 1, 0, 2,  )
        skill.pointIncrement = 100

        skillsService.createProject(proj)
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkill(skill)

        def users = getRandomUsers(7).sort()
        def date = new Date() - 1

        // user 1 - achieved and used after
        assert skillsService.addSkill(skill, users[0], new Date() - 4).body.skillApplied
        assert skillsService.addSkill(skill, users[0], new Date() - 3).body.skillApplied
        assert !skillsService.addSkill(skill, users[0], new Date()).body.skillApplied

        // user 2 - did not achieve
        assert skillsService.addSkill(skill, users[1], new Date()).body.skillApplied

        // user 3 - achieved but did not use after
        assert skillsService.addSkill(skill, users[2], new Date() - 2).body.skillApplied
        assert skillsService.addSkill(skill, users[2], date).body.skillApplied

        // user 4 - achieved and used after
        assert skillsService.addSkill(skill, users[3], new Date() - 9).body.skillApplied
        assert skillsService.addSkill(skill, users[3], new Date() - 8).body.skillApplied
        assert !skillsService.addSkill(skill, users[3], new Date() - 7).body.skillApplied
        assert !skillsService.addSkill(skill, users[3], new Date() - 6).body.skillApplied
        assert !skillsService.addSkill(skill, users[3], new Date() - 5).body.skillApplied

        // user 5 - did not achieve
        assert skillsService.addSkill(skill, users[5], new Date()).body.skillApplied

        // user 6 - achieved but did not use after
        assert skillsService.addSkill(skill, users[6], new Date() - 2).body.skillApplied
        assert skillsService.addSkill(skill, users[6], new Date() - 1).body.skillApplied

        when:
        def props = ["skillId": skill.skillId, "page": 1, "pageSize": 5, "sortBy": "date", "sortDesc": true]
        def result = builder.build(proj.projectId, builder.id, props)

        skillsService.archiveUsers([users[2]], proj.projectId)

        def resultAfterArchive = builder.build(proj.projectId, builder.id, props)

        then:
        result
        result.totalCount == 2
        result.users[0].userId == users[2]
        result.users[0].count == 2
        result.users[0].date.toString() == date.format('YYYY-MM-dd 00:00:00.0')
        result.users[1].userId == users[6]
        result.users[1].count == 2
        result.users[1].date.toString() == date.format('YYYY-MM-dd 00:00:00.0')

        resultAfterArchive.totalCount == 1
        resultAfterArchive.users[0].userId == users[6]
        resultAfterArchive.users[0].count == 2
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
}
