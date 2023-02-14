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
import skills.intTests.utils.SkillsService
import skills.metrics.builders.subjects.AchievementsByTagPerLevelMetricsBuilder

class AchievementsByTagPerLevelMetricsBuilderSpec extends DefaultIntSpec {

    @Autowired
    AchievementsByTagPerLevelMetricsBuilder builder

    def "produces accurate achievement user list"() {
        //simple case, not taking into account event compaction boundaries
        def proj = SkillsFactory.createProject()
        def skill = SkillsFactory.createSkill(1, 1, 1, 0, 5,  )
        skill.pointIncrement = 150
        def subj = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        def users = getRandomUsers(7)

        SkillsService rootUser = createRootSkillService()

        users[0..2].forEach(user -> {
            createService(user)
            rootUser.saveUserTag(user, 'someTag', ['ABCDE'])
        })

        users[3..6].forEach(user -> {
            createService(user)
            rootUser.saveUserTag(user, 'someTag', ['DEFGH'])
        })

        assert skillsService.addSkill(skill, users[0], new Date() - 4).body.skillApplied
        assert skillsService.addSkill(skill, users[0], new Date() - 3).body.skillApplied
        assert skillsService.addSkill(skill, users[0], new Date() - 2).body.skillApplied
        assert skillsService.addSkill(skill, users[0], new Date() - 1).body.skillApplied
        assert skillsService.addSkill(skill, users[1], new Date()).body.skillApplied
        assert skillsService.addSkill(skill, users[2], new Date() - 2).body.skillApplied
        assert skillsService.addSkill(skill, users[3], new Date() - 9).body.skillApplied
        assert skillsService.addSkill(skill, users[4], new Date() - 8).body.skillApplied
        assert skillsService.addSkill(skill, users[4], new Date() - 7).body.skillApplied
        assert skillsService.addSkill(skill, users[4], new Date() - 6).body.skillApplied
        assert skillsService.addSkill(skill, users[5], new Date() - 7).body.skillApplied
        assert skillsService.addSkill(skill, users[5], new Date() - 6).body.skillApplied
        assert skillsService.addSkill(skill, users[5], new Date() - 5).body.skillApplied
        assert skillsService.addSkill(skill, users[5], new Date() - 4).body.skillApplied
        assert skillsService.addSkill(skill, users[5], new Date() - 3).body.skillApplied
        assert skillsService.addSkill(skill, users[6], new Date() - 3).body.skillApplied
        assert skillsService.addSkill(skill, users[6], new Date()).body.skillApplied

        when:
        def props = ["subjectId": subj.subjectId, "userTagKey": "someTag"]
        def result = builder.build(proj.projectId, builder.id, props)

        then:
        result.totalLevels == 5
        result.data[0].value == [1: 1, 2: 1, 3: 1, 4: 0, 5: 1]
        result.data[0].tag == 'DEFGH'
        result.data[0].totalUsers == 4
        result.data[1].value == [1: 2, 2: 0, 3: 0, 4: 1]
        result.data[1].tag == 'ABCDE'
        result.data[1].totalUsers == 3
    }
}
