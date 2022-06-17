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
package skills.intTests.skillReuse

import groovy.json.JsonOutput
import skills.intTests.catalog.CatalogIntSpec
import skills.services.admin.skillReuse.SkillReuseIdUtil
import spock.lang.IgnoreRest

import static skills.intTests.utils.SkillsFactory.*

class SkillReusePointsAndAchivementsSpec extends CatalogIntSpec {

    def "skill events are propagated to the reused skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        String user = getRandomUsers(1)[0]
        when:
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        skillsService.addSkill(p1Skills[0], user, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        then:
        proj.points == 600
        proj.todaysPoints == 200
        proj.totalPoints == 2000
        proj.skillsLevel == 2
        proj.subjects[0].todaysPoints == 100
        proj.subjects[0].points == 300
        proj.subjects[0].totalPoints == 1500
        proj.subjects[1].todaysPoints == 100
        proj.subjects[1].points == 300
        proj.subjects[1].totalPoints == 500

        subj2.totalPoints == 500
        subj2.points == 300
        subj2.todaysPoints == 100
        subj2.skillsLevel == 3
        subj2.skills.size() == 1
        subj2.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].points == 300
        subj2.skills[0].totalPoints == 500
        subj2.skills[0].todaysPoints == 100

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.points == 300
        skill.totalPoints == 500
        skill.todaysPoints == 100
    }


    def "skill events are propagated to the reused skills - multiple subjects"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p1subj4 = createSubject(1, 4)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.createSubject(p1subj3)
        def p1Skills_subj3 = createSkills(1, 1, 3, 100, 5)
        skillsService.createSkills(p1Skills_subj3)
        skillsService.createSubject(p1subj4)

        when:
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj3.subjectId) // 1000
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj4.subjectId)

        String user = getRandomUsers(1)[0]
        skillsService.addSkill(p1Skills[0], user, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def subj3 = skillsService.getSkillSummary(user, p1.projectId, p1subj3.subjectId)
        def subj4 = skillsService.getSkillSummary(user, p1.projectId, p1subj4.subjectId)
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        def skill1 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1))
        def skill2 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2))

        then:
        proj.points == 1200
        proj.todaysPoints == 400
        proj.totalPoints == 3500
        proj.skillsLevel == 2
        proj.subjects[0].todaysPoints == 100
        proj.subjects[0].points == 300
        proj.subjects[0].totalPoints == 1500
        proj.subjects[1].todaysPoints == 100
        proj.subjects[1].points == 300
        proj.subjects[1].totalPoints == 500
        proj.subjects[2].todaysPoints == 100
        proj.subjects[2].points == 300
        proj.subjects[2].totalPoints == 1000
        proj.subjects[3].todaysPoints == 100
        proj.subjects[3].points == 300
        proj.subjects[3].totalPoints == 500

        subj2.totalPoints == 500
        subj2.points == 300
        subj2.todaysPoints == 100
        subj2.skillsLevel == 3
        subj2.skills.size() == 1
        subj2.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].points == 300
        subj2.skills[0].totalPoints == 500
        subj2.skills[0].todaysPoints == 100

        subj3.totalPoints == 1000
        subj3.points == 300
        subj3.todaysPoints == 100
        subj3.skillsLevel == 2
        subj3.skills.size() == 2
        subj3.skills[0].skillId == p1Skills_subj3[0].skillId
        subj3.skills[0].points == 0
        subj3.skills[0].totalPoints == 500
        subj3.skills[0].todaysPoints == 0
        subj3.skills[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        subj3.skills[1].points == 300
        subj3.skills[1].totalPoints == 500
        subj3.skills[1].todaysPoints == 100

        subj4.totalPoints == 500
        subj4.points == 300
        subj4.todaysPoints == 100
        subj4.skillsLevel == 3
        subj4.skills.size() == 1
        subj4.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        subj4.skills[0].points == 300
        subj4.skills[0].totalPoints == 500
        subj4.skills[0].todaysPoints == 100

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.points == 300
        skill.totalPoints == 500
        skill.todaysPoints == 100

        skill1.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        skill1.points == 300
        skill1.totalPoints == 500
        skill1.todaysPoints == 100

        skill2.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        skill2.points == 300
        skill2.totalPoints == 500
        skill2.todaysPoints == 100
    }

    def "when a skill is reused, existing user points are migrated and achievements are created"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        String user = getRandomUsers(1)[0]
        when:
        skillsService.addSkill(p1Skills[0], user, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user)

        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        println JsonOutput.toJson(subj2)
        then:
        proj.totalPoints == 2000
        proj.points == 600
        proj.skillsLevel == 2
        proj.subjects[0].todaysPoints == 100
        proj.subjects[0].points == 300
        proj.subjects[0].totalPoints == 1500
        proj.subjects[1].todaysPoints == 100
        proj.subjects[1].points == 300
        proj.subjects[1].totalPoints == 500

        subj2.totalPoints == 500
        subj2.points == 300
        subj2.todaysPoints == 100
        subj2.skillsLevel == 3
        subj2.skills.size() == 1
        subj2.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].points == 300
        subj2.skills[0].totalPoints == 500
        subj2.skills[0].todaysPoints == 100

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.points == 300
        skill.totalPoints == 500
        skill.todaysPoints == 100
    }

    def "when a skill is reused, existing user points are migrated and achievements are created - multiple subjects"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p1subj4 = createSubject(1, 4)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.createSubject(p1subj3)
        def p1Skills_subj3 = createSkills(1, 1, 3, 100, 5)
        skillsService.createSkills(p1Skills_subj3)
        skillsService.createSubject(p1subj4)

        String user = getRandomUsers(1)[0]
        skillsService.addSkill(p1Skills[0], user, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user)

        when:
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj3.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj4.subjectId)

        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def subj3 = skillsService.getSkillSummary(user, p1.projectId, p1subj3.subjectId)
        def subj4 = skillsService.getSkillSummary(user, p1.projectId, p1subj4.subjectId)
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        def skill1 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1))
        def skill2 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2))

        then:
        proj.points == 1200
        proj.todaysPoints == 400
        proj.totalPoints == 3500
        proj.skillsLevel == 2
        proj.subjects[0].todaysPoints == 100
        proj.subjects[0].points == 300
        proj.subjects[0].totalPoints == 1500
        proj.subjects[1].todaysPoints == 100
        proj.subjects[1].points == 300
        proj.subjects[1].totalPoints == 500
        proj.subjects[2].todaysPoints == 100
        proj.subjects[2].points == 300
        proj.subjects[2].totalPoints == 1000
        proj.subjects[3].todaysPoints == 100
        proj.subjects[3].points == 300
        proj.subjects[3].totalPoints == 500

        subj2.totalPoints == 500
        subj2.points == 300
        subj2.todaysPoints == 100
        subj2.skillsLevel == 3
        subj2.skills.size() == 1
        subj2.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].points == 300
        subj2.skills[0].totalPoints == 500
        subj2.skills[0].todaysPoints == 100

        subj3.totalPoints == 1000
        subj3.points == 300
        subj3.todaysPoints == 100
        subj3.skillsLevel == 2
        subj3.skills.size() == 2
        subj3.skills[0].skillId == p1Skills_subj3[0].skillId
        subj3.skills[0].points == 0
        subj3.skills[0].totalPoints == 500
        subj3.skills[0].todaysPoints == 0
        subj3.skills[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        subj3.skills[1].points == 300
        subj3.skills[1].totalPoints == 500
        subj3.skills[1].todaysPoints == 100

        subj4.totalPoints == 500
        subj4.points == 300
        subj4.todaysPoints == 100
        subj4.skillsLevel == 3
        subj4.skills.size() == 1
        subj4.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        subj4.skills[0].points == 300
        subj4.skills[0].totalPoints == 500
        subj4.skills[0].todaysPoints == 100

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.points == 300
        skill.totalPoints == 500
        skill.todaysPoints == 100

        skill1.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        skill1.points == 300
        skill1.totalPoints == 500
        skill1.todaysPoints == 100

        skill2.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        skill2.points == 300
        skill2.totalPoints == 500
        skill2.todaysPoints == 100
    }
}
