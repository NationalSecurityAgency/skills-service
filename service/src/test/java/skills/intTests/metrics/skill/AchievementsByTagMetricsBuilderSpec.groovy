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
import skills.metrics.builders.skill.AchievementsByTagMetricsBuilder

class AchievementsByTagMetricsBuilderSpec  extends DefaultIntSpec {
    @Autowired
    AchievementsByTagMetricsBuilder builder

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
        def props = ["skillId": skill.skillId, "userTagKey": "someTag"]
        def result = builder.build(proj.projectId, builder.id, props)

        skillsService.archiveUsers([users[2], users[5]], proj.projectId)
        def resultAfterArchive = builder.build(proj.projectId, builder.id, props)

        then:
        result['ABCDE'].numberAchieved == 0
        result['ABCDE'].numberInProgress == 3
        result['DEFGH'].numberAchieved == 1
        result['DEFGH'].numberInProgress == 3

        resultAfterArchive['ABCDE'].numberAchieved == 0
        resultAfterArchive['ABCDE'].numberInProgress == 2
        resultAfterArchive['DEFGH'].numberAchieved == 0
        resultAfterArchive['DEFGH'].numberInProgress == 3

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
        def props = ["skillId": skill.skillId, "userTagKey": "someTag"]
        def result = builder.build(proj.projectId, builder.id, props)

        then:
        result.size() == 20
    }

    def "produces accurate achievement user list with multiple skills"() {
        def proj = SkillsFactory.createProject()
        def skills = []
        for(def x = 1; x <= 5; x +=1) {
            skills.push(SkillsFactory.createSkill(1, 1, x, 0, 3,  ))
        }
        skills.each{ it -> it.pointIncrement = 150 }
        def subj = SkillsFactory.createSubject()
        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skills.each{ it -> skillsService.createSkill(it) }

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

        // ABCDE
        assert skillsService.addSkill(skills[0], users[0], new Date() - 4).body.skillApplied
        assert skillsService.addSkill(skills[0], users[0], new Date() - 3).body.skillApplied
        assert skillsService.addSkill(skills[0], users[0], new Date() - 2).body.skillApplied
        assert skillsService.addSkill(skills[1], users[0], new Date() - 2).body.skillApplied
        assert skillsService.addSkill(skills[2], users[0], new Date() - 1).body.skillApplied

        assert skillsService.addSkill(skills[2], users[1], new Date() - 3).body.skillApplied
        assert skillsService.addSkill(skills[2], users[1], new Date() - 2).body.skillApplied
        assert skillsService.addSkill(skills[2], users[1], new Date()).body.skillApplied

        assert skillsService.addSkill(skills[3], users[2], new Date() - 2).body.skillApplied

        // DEFGH
        assert skillsService.addSkill(skills[3], users[3], new Date() - 9).body.skillApplied
        assert skillsService.addSkill(skills[3], users[3], new Date() - 8).body.skillApplied
        assert skillsService.addSkill(skills[3], users[3], new Date() - 7).body.skillApplied

        assert skillsService.addSkill(skills[4], users[4], new Date() - 8).body.skillApplied
        assert skillsService.addSkill(skills[4], users[4], new Date() - 7).body.skillApplied
        assert skillsService.addSkill(skills[0], users[4], new Date() - 6).body.skillApplied

        assert skillsService.addSkill(skills[1], users[5], new Date() - 7).body.skillApplied
        assert skillsService.addSkill(skills[1], users[5], new Date() - 6).body.skillApplied
        assert skillsService.addSkill(skills[2], users[5], new Date() - 5).body.skillApplied
        assert skillsService.addSkill(skills[2], users[5], new Date() - 4).body.skillApplied
        assert skillsService.addSkill(skills[2], users[5], new Date() - 3).body.skillApplied
        assert skillsService.addSkill(skills[3], users[5], new Date() - 3).body.skillApplied

        assert skillsService.addSkill(skills[4], users[6], new Date() - 3).body.skillApplied
        assert skillsService.addSkill(skills[4], users[6], new Date()).body.skillApplied

        when:
        def resultList =[
            builder.build(proj.projectId, builder.id, ["skillId": skills[0].skillId, "userTagKey": "someTag"]),
            builder.build(proj.projectId, builder.id, ["skillId": skills[1].skillId, "userTagKey": "someTag"]),
            builder.build(proj.projectId, builder.id, ["skillId": skills[2].skillId, "userTagKey": "someTag"]),
            builder.build(proj.projectId, builder.id, ["skillId": skills[3].skillId, "userTagKey": "someTag"]),
            builder.build(proj.projectId, builder.id, ["skillId": skills[4].skillId, "userTagKey": "someTag"])
        ]

        then:
          resultList[0]['ABCDE'].numberAchieved == 1
          resultList[0]['ABCDE'].numberInProgress == 0
          resultList[0]['DEFGH'].numberAchieved == 0
          resultList[0]['DEFGH'].numberInProgress == 1

          resultList[1]['ABCDE'].numberInProgress == 1
          resultList[1]['ABCDE'].numberAchieved == 0
          resultList[1]['DEFGH'].numberInProgress == 1
          resultList[1]['DEFGH'].numberAchieved == 0

          resultList[2]['ABCDE'].numberInProgress == 1
          resultList[2]['ABCDE'].numberAchieved == 1
          resultList[2]['DEFGH'].numberInProgress == 0
          resultList[2]['DEFGH'].numberAchieved == 1

          resultList[3]['ABCDE'].numberInProgress == 1
          resultList[3]['ABCDE'].numberAchieved == 0
          resultList[3]['DEFGH'].numberInProgress == 1
          resultList[3]['DEFGH'].numberAchieved == 1

          resultList[4]['DEFGH'].numberInProgress == 2
          resultList[4]['DEFGH'].numberAchieved == 0
    }
}
