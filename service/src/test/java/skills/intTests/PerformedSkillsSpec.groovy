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
package skills.intTests


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class PerformedSkillsSpec extends DefaultIntSpec {

    def "get performed skills"() {
        List<String> users = getRandomUsers(3)
        def proj1 = SkillsFactory.createProject(5)
        def proj1_subj1 = SkillsFactory.createSubject(5, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 5, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)

        List<Date> dates = (1..3).collect { new Date() - it }
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[0], dates[0]).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId],  users[0], dates[1] ).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId],  users[0], dates[2] ).body


        when:
        def events = skillsService.getPerformedSkills(users[0], proj1.projectId, "", "skillId")

        then:
        events.data.size() == 3
        events.data[0].skillName == proj1_skills[2].name
        events.data[0].skillId == proj1_skills[2].skillId
        events.data[0].subjectId == proj1_subj1.subjectId
        events.data[1].skillName == proj1_skills[1].name
        events.data[1].skillId == proj1_skills[1].skillId
        events.data[1].subjectId == proj1_subj1.subjectId
        events.data[2].skillName == proj1_skills[0].name
        events.data[2].skillId == proj1_skills[0].skillId
        events.data[2].subjectId == proj1_subj1.subjectId
    }

    def "filter by skill name"() {
        List<String> users = getRandomUsers(3)
        def proj1 = SkillsFactory.createProject(5)
        def proj1_subj1 = SkillsFactory.createSubject(5, 1)

        List<Map> proj1_skills = SkillsFactory.createSkills(3, 5, 1)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        proj1_skills[0].name = "one ok find"
        proj1_skills[1].name = "ool ok find"
        proj1_skills[2].name = "grr 5 find"

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)

        List<Date> dates = (1..3).collect { new Date() - it }
        when:
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[0], dates[0]).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId],  users[0], dates[1] ).body
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId],  users[0], dates[2] ).body

        then:
        skillsService.getPerformedSkills(users[0], proj1.projectId, "", "skillId").data.collect { it.skillName } == ["grr 5 find", "ool ok find", "one ok find"]
        skillsService.getPerformedSkills(users[0], proj1.projectId, "ok find", "skillId").data.collect { it.skillName } == ["ool ok find", "one ok find"]
        skillsService.getPerformedSkills(users[0], proj1.projectId, "ok fi", "skillId").data.collect { it.skillName } == ["ool ok find", "one ok find"]
        skillsService.getPerformedSkills(users[0], proj1.projectId, "gr", "skillId").data.collect { it.skillName } == ["grr 5 find"]
    }

    def "get performed skills in groups"() {
        List<String> users = getRandomUsers(3)
        def proj1 = SkillsFactory.createProject(5)
        def proj1_subj1 = SkillsFactory.createSubject(5, 1)
        def proj1_subj2 = SkillsFactory.createSubject(5, 2)

        List<Map> proj1_skills = SkillsFactory.createSkills(5, 5, 1)
        List<Map> proj1_skills2 = SkillsFactory.createSkills(5, 5, 2)
        proj1_skills.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        proj1_skills2.each {
            it.pointIncrement = 100
            it.numPerformToCompletion = 2
            it.pointIncrementInterval = 0 // ability to achieve right away
        }
        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSkills(proj1_skills)
        skillsService.createSubject(proj1_subj2)
        skillsService.createSkills(proj1_skills2)
        def skillsGroup = SkillsFactory.createSkillsGroup(5, 1, 6)
        skillsService.createSkill(skillsGroup)
        skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, proj1_skills[3])
        skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, proj1_skills[4])

        List<Date> dates = (1..5).collect { new Date() - it }
        dates.eachWithIndex { it, index ->
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(index).skillId], users[0], it).body
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills2.get(index).skillId], users[0], it).body
        }

        when:
        def events = skillsService.getPerformedSkills(users[0], proj1.projectId, "", "skillId")

        then:
        events.data.size() == 10
        events.data[0].skillName == proj1_skills2[4].name
        events.data[0].skillId == proj1_skills2[4].skillId
        events.data[0].subjectId == proj1_subj2.subjectId
        events.data[1].skillName == proj1_skills[4].name
        events.data[1].skillId == proj1_skills[4].skillId
        events.data[1].subjectId == proj1_subj1.subjectId

        events.data[2].skillName == proj1_skills2[3].name
        events.data[2].skillId == proj1_skills2[3].skillId
        events.data[2].subjectId == proj1_subj2.subjectId
        events.data[3].skillName == proj1_skills[3].name
        events.data[3].skillId == proj1_skills[3].skillId
        events.data[3].subjectId == proj1_subj1.subjectId

        events.data[4].skillName == proj1_skills2[2].name
        events.data[4].skillId == proj1_skills2[2].skillId
        events.data[4].subjectId == proj1_subj2.subjectId
        events.data[5].skillName == proj1_skills[2].name
        events.data[5].skillId == proj1_skills[2].skillId
        events.data[5].subjectId == proj1_subj1.subjectId

        events.data[6].skillName == proj1_skills2[1].name
        events.data[6].skillId == proj1_skills2[1].skillId
        events.data[6].subjectId == proj1_subj2.subjectId
        events.data[7].skillName == proj1_skills[1].name
        events.data[7].skillId == proj1_skills[1].skillId
        events.data[7].subjectId == proj1_subj1.subjectId

        events.data[8].skillName == proj1_skills2[0].name
        events.data[8].skillId == proj1_skills2[0].skillId
        events.data[8].subjectId == proj1_subj2.subjectId
        events.data[9].skillName == proj1_skills[0].name
        events.data[9].skillId == proj1_skills[0].skillId
        events.data[9].subjectId == proj1_subj1.subjectId
    }
}
