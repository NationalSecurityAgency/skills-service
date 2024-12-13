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

        skillsService.archiveUsers([users[2]], proj.projectId)
        def resAfterArchive = builder.build(proj.projectId, builder.id, props)

        then:
        result.totalLevels == 5
        result.data[0].value == [1: 1, 2: 1, 3: 1, 4: 0, 5: 1]
        result.data[0].tag == 'DEFGH'
        result.data[0].totalUsers == 4
        result.data[1].value == [1: 2, 2: 0, 3: 0, 4: 1]
        result.data[1].tag == 'ABCDE'
        result.data[1].totalUsers == 3

        resAfterArchive.totalLevels == 5
        resAfterArchive.data[0].value == [1: 1, 2: 1, 3: 1, 4: 0, 5: 1]
        resAfterArchive.data[0].tag == 'DEFGH'
        resAfterArchive.data[0].totalUsers == 4
        resAfterArchive.data[1].value == [1: 1, 2: 0, 3: 0, 4: 1]
        resAfterArchive.data[1].tag == 'ABCDE'
        resAfterArchive.data[1].totalUsers == 2
    }

    def "limits to top 20 results"() {
        def proj = SkillsFactory.createProject()
        def skill = SkillsFactory.createSkill(1, 1, 1, 0, 5,  )
        skill.pointIncrement = 150
        def subj = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        def users = getRandomUsers(50)
        def tags = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y']
        SkillsService rootUser = createRootSkillService()

        users[0..24].eachWithIndex{user, index ->
            createService(user)
            rootUser.saveUserTag(user, 'someTag', [tags[index]])
        }

        users[25..29].eachWithIndex{user, index ->
            createService(user)
            rootUser.saveUserTag(user, 'someTag', [tags[index]])
        }

        users[30..39].eachWithIndex{user, index ->
            createService(user)
            rootUser.saveUserTag(user, 'someTag', [tags[index]])
        }

        users[40..49].eachWithIndex{user, index ->
            createService(user)
            rootUser.saveUserTag(user, 'someTag', [tags[index]])
        }

        users.forEach( user -> {
            assert skillsService.addSkill(skill, user, new Date() - 4).body.skillApplied
            assert skillsService.addSkill(skill, user, new Date() - 3).body.skillApplied
            assert skillsService.addSkill(skill, user, new Date() - 2).body.skillApplied
            assert skillsService.addSkill(skill, user, new Date() - 1).body.skillApplied
        })

        when:
        def props = ["subjectId": subj.subjectId, "userTagKey": "someTag"]
        def result = builder.build(proj.projectId, builder.id, props)

        then:
        result.data.size() == 20
    }

    def "produces accurate achievement user list with multiple subjects"() {
        def proj = SkillsFactory.createProject()
        def skill = SkillsFactory.createSkill(1, 1, 1, 0, 5,  )
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 5,  )
        def skill3 = SkillsFactory.createSkill(1, 2, 3, 0, 5,  )
        def skill4 = SkillsFactory.createSkill(1, 2, 4, 0, 5,  )
        skill.pointIncrement = 150
        def subj = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(1, 2)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj2)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

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
        assert skillsService.addSkill(skill, users[0], new Date() - 1).body.skillApplied
        assert skillsService.addSkill(skill, users[1], new Date()).body.skillApplied
        assert skillsService.addSkill(skill, users[4], new Date() - 8).body.skillApplied
        assert skillsService.addSkill(skill, users[5], new Date() - 7).body.skillApplied
        assert skillsService.addSkill(skill, users[5], new Date() - 6).body.skillApplied
        assert skillsService.addSkill(skill, users[6], new Date() - 3).body.skillApplied
        assert skillsService.addSkill(skill2, users[0], new Date() - 2).body.skillApplied
        assert skillsService.addSkill(skill2, users[4], new Date() - 7).body.skillApplied
        assert skillsService.addSkill(skill2, users[4], new Date() - 6).body.skillApplied
        assert skillsService.addSkill(skill2, users[5], new Date() - 5).body.skillApplied
        assert skillsService.addSkill(skill3, users[2], new Date() - 2).body.skillApplied
        assert skillsService.addSkill(skill3, users[3], new Date() - 9).body.skillApplied
        assert skillsService.addSkill(skill3, users[5], new Date() - 4).body.skillApplied
        assert skillsService.addSkill(skill3, users[5], new Date() - 3).body.skillApplied
        assert skillsService.addSkill(skill4, users[6], new Date()).body.skillApplied

        when:
        def props = ["subjectId": subj.subjectId, "userTagKey": "someTag"]
        def subj1result = builder.build(proj.projectId, builder.id, props)
        def props2 = ["subjectId": subj2.subjectId, "userTagKey": "someTag"]
        def subj2result = builder.build(proj.projectId, builder.id, props2)

        skillsService.archiveUsers([users[0], users[5]], proj.projectId)
        def subj1resultAfterArchive = builder.build(proj.projectId, builder.id, props)
        def subj2resultAfterArchive = builder.build(proj.projectId, builder.id, props2)

        then:
        subj1result.totalLevels == 3
        subj1result.data[0].value == [1: 2, 2: 1]
        subj1result.data[0].tag == 'DEFGH'
        subj1result.data[0].totalUsers == 3
        subj1result.data[1].value == [1: 1, 2: 0, 3: 1]
        subj1result.data[1].tag == 'ABCDE'
        subj1result.data[1].totalUsers == 2
        subj2result.totalLevels == 1
        subj2result.data[0].value == [1: 3]
        subj2result.data[0].tag == 'DEFGH'
        subj2result.data[0].totalUsers == 3
        subj2result.data[1].value == [1: 1]
        subj2result.data[1].tag == 'ABCDE'
        subj2result.data[1].totalUsers == 1

        subj1resultAfterArchive.totalLevels == 1
        subj1resultAfterArchive.data[0].value == [1: 2]
        subj1resultAfterArchive.data[0].tag == 'DEFGH'
        subj1resultAfterArchive.data[0].totalUsers == 2
        subj1resultAfterArchive.data[1].value == [1: 1]
        subj1resultAfterArchive.data[1].tag == 'ABCDE'
        subj1resultAfterArchive.data[1].totalUsers == 1
        subj2resultAfterArchive.totalLevels == 1
        subj2resultAfterArchive.data[0].value == [1: 2]
        subj2resultAfterArchive.data[0].tag == 'DEFGH'
        subj2resultAfterArchive.data[0].totalUsers == 2
        subj2resultAfterArchive.data[1].value == [1: 1]
        subj2resultAfterArchive.data[1].tag == 'ABCDE'
        subj2resultAfterArchive.data[1].totalUsers == 1
    }
}
