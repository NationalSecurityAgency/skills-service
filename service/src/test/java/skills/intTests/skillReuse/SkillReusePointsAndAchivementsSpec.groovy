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

import skills.intTests.catalog.CatalogIntSpec
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.storage.model.SkillDef

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

    def "updating original skill increases user points and user's levels"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        def p1Skills_subj2 = createSkills(1, 1, 2, 100, 5)
        skillsService.createSkills(p1Skills_subj2)

        String user = getRandomUsers(1)[0]
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        skillsService.addSkill(p1Skills[0], user, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        when:
        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        p1Skills[0].pointIncrement = 1000
        p1Skills[0].numPerformToCompletion = 6
        skillsService.createSkill(p1Skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def proj_after = skillsService.getSkillSummary(user, p1.projectId)
        def subj2_after = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skill_after = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        then:
        proj.totalPoints == 2500
        proj.points == 600
        proj.skillsLevel == 1
        proj.subjects[0].todaysPoints == 100
        proj.subjects[0].points == 300
        proj.subjects[0].totalPoints == 1500
        proj.subjects[1].todaysPoints == 100
        proj.subjects[1].points == 300
        proj.subjects[1].totalPoints == 1000

        subj2.totalPoints == 1000
        subj2.points == 300
        subj2.todaysPoints == 100
        subj2.skillsLevel == 2
        subj2.skills.size() == 2
        subj2.skills[0].skillId == p1Skills_subj2[0].skillId
        subj2.skills[0].points == 0
        subj2.skills[0].totalPoints == 500
        subj2.skills[0].todaysPoints == 0
        subj2.skills[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[1].points == 300
        subj2.skills[1].totalPoints == 500
        subj2.skills[1].todaysPoints == 100

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.points == 300
        skill.totalPoints == 500
        skill.todaysPoints == 100

        // after
        proj_after.totalPoints == 13500
        proj_after.points == 6000
        proj_after.skillsLevel == 2
        proj_after.subjects[0].todaysPoints == 1000
        proj_after.subjects[0].points == 3000
        proj_after.subjects[0].totalPoints == 7000
        proj_after.subjects[1].todaysPoints == 1000
        proj_after.subjects[1].points == 3000
        proj_after.subjects[1].totalPoints == 6500

        subj2_after.totalPoints == 6000 + 500
        subj2_after.points == 3000
        subj2_after.todaysPoints == 1000
        subj2_after.skillsLevel == 3
        subj2_after.skills.size() == 2
        subj2_after.skills[0].skillId == p1Skills_subj2[0].skillId
        subj2_after.skills[0].points == 0
        subj2_after.skills[0].totalPoints == 500
        subj2_after.skills[0].todaysPoints == 0
        subj2_after.skills[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_after.skills[1].points == 3000
        subj2_after.skills[1].totalPoints == 6000
        subj2_after.skills[1].todaysPoints == 1000

        skill_after.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill_after.totalPoints == 6000
        skill_after.points == 3000
        skill_after.todaysPoints == 1000

        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, p1Skills[0].skillId) == 3000
        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)) == 3000
        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, p1subj1.subjectId) == 3000
        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, p1subj2.subjectId) == 3000
        userPointsRepo.findPointsByProjectIdAndUserId(p1Skills[0].projectId, user) == 6000
    }

    def "updating original skill's pointIncrement increases user points and user's levels"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        def p1Skills_subj2 = createSkills(1, 1, 2, 100, 5)
        skillsService.createSkills(p1Skills_subj2)

        String user = getRandomUsers(1)[0]
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        skillsService.addSkill(p1Skills[0], user, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        when:
        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        p1Skills[0].pointIncrement = 1000
        skillsService.createSkill(p1Skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def proj_after = skillsService.getSkillSummary(user, p1.projectId)
        def subj2_after = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skill_after = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        then:
        proj.totalPoints == 2500
        proj.points == 600
        proj.skillsLevel == 1
        proj.subjects[0].todaysPoints == 100
        proj.subjects[0].points == 300
        proj.subjects[0].totalPoints == 1500
        proj.subjects[1].todaysPoints == 100
        proj.subjects[1].points == 300
        proj.subjects[1].totalPoints == 1000

        subj2.totalPoints == 1000
        subj2.points == 300
        subj2.todaysPoints == 100
        subj2.skillsLevel == 2
        subj2.skills.size() == 2
        subj2.skills[0].skillId == p1Skills_subj2[0].skillId
        subj2.skills[0].points == 0
        subj2.skills[0].totalPoints == 500
        subj2.skills[0].todaysPoints == 0
        subj2.skills[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[1].points == 300
        subj2.skills[1].totalPoints == 500
        subj2.skills[1].todaysPoints == 100

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.points == 300
        skill.totalPoints == 500
        skill.todaysPoints == 100

        // after
        proj_after.totalPoints == 11500
        proj_after.points == 6000
        proj_after.skillsLevel == 3
        proj_after.subjects[0].todaysPoints == 1000
        proj_after.subjects[0].points == 3000
        proj_after.subjects[0].totalPoints == 6000
        proj_after.subjects[1].todaysPoints == 1000
        proj_after.subjects[1].points == 3000
        proj_after.subjects[1].totalPoints == 5500

        subj2_after.totalPoints == 5000 + 500
        subj2_after.points == 3000
        subj2_after.todaysPoints == 1000
        subj2_after.skillsLevel == 3
        subj2_after.skills.size() == 2
        subj2_after.skills[0].skillId == p1Skills_subj2[0].skillId
        subj2_after.skills[0].points == 0
        subj2_after.skills[0].totalPoints == 500
        subj2_after.skills[0].todaysPoints == 0
        subj2_after.skills[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_after.skills[1].points == 3000
        subj2_after.skills[1].totalPoints == 5000
        subj2_after.skills[1].todaysPoints == 1000

        skill_after.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill_after.totalPoints == 5000
        skill_after.points == 3000
        skill_after.todaysPoints == 1000

        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, p1Skills[0].skillId) == 3000
        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)) == 3000
        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, p1subj1.subjectId) == 3000
        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, p1subj2.subjectId) == 3000
        userPointsRepo.findPointsByProjectIdAndUserId(p1Skills[0].projectId, user) == 6000
    }

    def "updating original skill's numPerformToCompletion increases user's levels"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        def p1Skills_subj2 = createSkills(1, 1, 2, 100, 5)
        skillsService.createSkills(p1Skills_subj2)

        String user = getRandomUsers(1)[0]
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        skillsService.addSkill(p1Skills[0], user, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        when:
        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        p1Skills[0].numPerformToCompletion = 3
        skillsService.createSkill(p1Skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def proj_after = skillsService.getSkillSummary(user, p1.projectId)
        def subj2_after = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skill_after = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        then:
        proj.totalPoints == 2500
        proj.points == 600
        proj.skillsLevel == 1
        proj.subjects[0].todaysPoints == 100
        proj.subjects[0].points == 300
        proj.subjects[0].totalPoints == 1500
        proj.subjects[1].todaysPoints == 100
        proj.subjects[1].points == 300
        proj.subjects[1].totalPoints == 1000

        subj2.totalPoints == 1000
        subj2.points == 300
        subj2.todaysPoints == 100
        subj2.skillsLevel == 2
        subj2.skills.size() == 2
        subj2.skills[0].skillId == p1Skills_subj2[0].skillId
        subj2.skills[0].points == 0
        subj2.skills[0].totalPoints == 500
        subj2.skills[0].todaysPoints == 0
        subj2.skills[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[1].points == 300
        subj2.skills[1].totalPoints == 500
        subj2.skills[1].todaysPoints == 100

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.points == 300
        skill.totalPoints == 500
        skill.todaysPoints == 100

        // after
        proj_after.totalPoints == 2100
        proj_after.points == 600
        proj_after.skillsLevel == 2
        proj_after.subjects[0].todaysPoints == 100
        proj_after.subjects[0].points == 300
        proj_after.subjects[0].totalPoints == 1300
        proj_after.subjects[1].todaysPoints == 100
        proj_after.subjects[1].points == 300
        proj_after.subjects[1].totalPoints == 800

        subj2_after.totalPoints == 300 + 500
        subj2_after.points == 300
        subj2_after.todaysPoints == 100
        subj2_after.skillsLevel == 2
        subj2_after.skills.size() == 2
        subj2_after.skills[0].skillId == p1Skills_subj2[0].skillId
        subj2_after.skills[0].points == 0
        subj2_after.skills[0].totalPoints == 500
        subj2_after.skills[0].todaysPoints == 0
        subj2_after.skills[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_after.skills[1].points == 300
        subj2_after.skills[1].totalPoints == 300
        subj2_after.skills[1].todaysPoints == 100

        skill_after.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill_after.totalPoints == 300
        skill_after.points == 300
        skill_after.todaysPoints == 100

        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, p1Skills[0].skillId) == 300
        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)) == 300
        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, p1subj1.subjectId) == 300
        userPointsRepo.findPointsByProjectIdAndUserIdAndSkillId(p1Skills[0].projectId, user, p1subj2.subjectId) == 300
        userPointsRepo.findPointsByProjectIdAndUserId(p1Skills[0].projectId, user) == 600
    }

    def "skill events can be reported to a self reported skill - Honor System"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        }
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        String user = getRandomUsers(1)[0]
        when:
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user, new Date() - 2)
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user, new Date() - 1)
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillReused = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        def subj1 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def skillOrig = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
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

        skillReused.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused.points == 300
        skillReused.totalPoints == 500
        skillReused.todaysPoints == 100

        subj1.totalPoints == 1500
        subj1.points == 300
        subj1.todaysPoints == 100
        subj1.skillsLevel == 1
        subj1.skills.size() == 3
        def skill1 = subj1.skills.find { it.skillId == p1Skills[0].skillId }
        skill1.points == 300
        skill1.totalPoints == 500
        skill1.todaysPoints == 100
        subj1.skills.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1.skills.find { it.skillId == p1Skills[2].skillId }.points == 0

        skillOrig.skillId == p1Skills[0].skillId
        skillOrig.points == 300
        skillOrig.totalPoints == 500
        skillOrig.todaysPoints == 100
    }


    def "skill events can be reported to a self reported group skill - Honor System"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        }
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 22)
        skillsService.createSkill(p1subj2g2)

        String user = getRandomUsers(1)[0]
        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)

        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user, new Date() - 2)
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user, new Date() - 1)
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillReused = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        def subj1 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def skillOrig = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
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
        subj2.skills[0].children.size() == 1
        subj2.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[0].points == 300
        subj2.skills[0].children[0].totalPoints == 500
        subj2.skills[0].children[0].todaysPoints == 100

        skillReused.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused.points == 300
        skillReused.totalPoints == 500
        skillReused.todaysPoints == 100

        subj1.totalPoints == 1500
        subj1.points == 300
        subj1.todaysPoints == 100
        subj1.skillsLevel == 1
        subj1.skills.size() == 1
        subj1.skills[0].children.size() == 3
        def skill1 = subj1.skills[0].children.find { it.skillId == p1Skills[0].skillId }
        skill1.points == 300
        skill1.totalPoints == 500
        skill1.todaysPoints == 100
        subj1.skills[0].children.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1.skills[0].children.find { it.skillId == p1Skills[2].skillId }.points == 0

        skillOrig.skillId == p1Skills[0].skillId
        skillOrig.points == 300
        skillOrig.totalPoints == 500
        skillOrig.todaysPoints == 100
    }

    def "skill events can be reported to a self reported skill - Approval System"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        String user = getRandomUsers(1)[0]
        when:
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        def approve = {
            def approvalsEndpointRes = skillsService.getApprovals(p1.projectId, 5, 1, 'requestedOn', false)
            List<Integer> ids = approvalsEndpointRes.data.collect { it.id }
            assert ids
            skillsService.approve(p1.projectId, ids)
        }
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user, new Date() - 2)
        approve()
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user, new Date() - 1)
        approve()
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user)
        approve()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillReused = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        def subj1 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def skillOrig = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        then:
        skillOrig.skillId == p1Skills[0].skillId
        skillOrig.points == 300
        skillOrig.totalPoints == 500
        skillOrig.todaysPoints == 100

        skillReused.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused.points == 300
        skillReused.totalPoints == 500
        skillReused.todaysPoints == 100

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

        subj1.totalPoints == 1500
        subj1.points == 300
        subj1.todaysPoints == 100
        subj1.skillsLevel == 1
        subj1.skills.size() == 3
        def skill1 = subj1.skills.find { it.skillId == p1Skills[0].skillId }
        skill1.points == 300
        skill1.totalPoints == 500
        skill1.todaysPoints == 100
        subj1.skills.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1.skills.find { it.skillId == p1Skills[2].skillId }.points == 0


    }

    def "skill events can be reported to a self reported group skill - Approval System"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills.each {
            it.selfReportingType = SkillDef.SelfReportingType.Approval
        }
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 22)
        skillsService.createSkill(p1subj2g2)

        String user = getRandomUsers(1)[0]
        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)

        def approve = {
            def approvalsEndpointRes = skillsService.getApprovals(p1.projectId, 5, 1, 'requestedOn', false)
            List<Integer> ids = approvalsEndpointRes.data.collect { it.id }
            assert ids
            skillsService.approve(p1.projectId, ids)
        }
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user, new Date() - 2)
        approve()
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user, new Date() - 1)
        approve()
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], user)
        approve()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillReused = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        def subj1 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def skillOrig = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
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
        subj2.skills[0].children.size() == 1
        subj2.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[0].points == 300
        subj2.skills[0].children[0].totalPoints == 500
        subj2.skills[0].children[0].todaysPoints == 100

        skillReused.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused.points == 300
        skillReused.totalPoints == 500
        skillReused.todaysPoints == 100

        subj1.totalPoints == 1500
        subj1.points == 300
        subj1.todaysPoints == 100
        subj1.skillsLevel == 1
        subj1.skills.size() == 1
        subj1.skills[0].children.size() == 3
        def skill1 = subj1.skills[0].children.find { it.skillId == p1Skills[0].skillId }
        skill1.points == 300
        skill1.totalPoints == 500
        skill1.todaysPoints == 100
        subj1.skills[0].children.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1.skills[0].children.find { it.skillId == p1Skills[2].skillId }.points == 0

        skillOrig.skillId == p1Skills[0].skillId
        skillOrig.points == 300
        skillOrig.totalPoints == 500
        skillOrig.todaysPoints == 100
    }

    def "remove skill event from a skill under subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        String user = getRandomUsers(1)[0]

        List<Date> dates = [new Date() - 2, new Date() - 1, new Date()]
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: p1Skills[0].skillId], user, dates[0])
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: p1Skills[0].skillId], user, dates[1])
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: p1Skills[0].skillId], user, dates[2])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        when:
        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj1 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillOrig = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skillReused = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        skillsService.deleteSkillEvent([projectId: p1.projectId, skillId: p1Skills[0].skillId, userId: user, timestamp: dates[1].time])

        def proj_t1 = skillsService.getSkillSummary(user, p1.projectId)
        def subj1_t1 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def subj2_t1 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillOrig_t1 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skillReused_t1 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        skillsService.deleteSkillEvent([projectId: p1.projectId, skillId: p1Skills[0].skillId, userId: user, timestamp: dates[2].time])

        def proj_t2 = skillsService.getSkillSummary(user, p1.projectId)
        def subj1_t2 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def subj2_t2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillOrig_t2 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skillReused_t2 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        skillsService.deleteSkillEvent([projectId: p1.projectId, skillId: p1Skills[0].skillId, userId: user, timestamp: dates[0].time])

        def proj_t3 = skillsService.getSkillSummary(user, p1.projectId)
        def subj1_t3 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def subj2_t3 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillOrig_t3 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skillReused_t3 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

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
        subj2.skills.size() == 1
        subj2.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].points == 300
        subj2.skills[0].totalPoints == 500
        subj2.skills[0].todaysPoints == 100

        skillReused.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused.points == 300
        skillReused.totalPoints == 500
        skillReused.todaysPoints == 100

        subj1.totalPoints == 1500
        subj1.points == 300
        subj1.todaysPoints == 100
        subj1.skillsLevel == 1
        subj1.skills.size() == 3
        def skill1 = subj1.skills.find { it.skillId == p1Skills[0].skillId }
        skill1.points == 300
        skill1.totalPoints == 500
        skill1.todaysPoints == 100
        subj1.skills.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1.skills.find { it.skillId == p1Skills[2].skillId }.points == 0

        skillOrig.skillId == p1Skills[0].skillId
        skillOrig.points == 300
        skillOrig.totalPoints == 500
        skillOrig.todaysPoints == 100

        // after deletion
        proj_t1.points == 400
        proj_t1.todaysPoints == 200
        proj_t1.totalPoints == 2000
        proj_t1.skillsLevel == 1
        proj_t1.subjects[0].todaysPoints == 100
        proj_t1.subjects[0].points == 200
        proj_t1.subjects[0].totalPoints == 1500
        proj_t1.subjects[1].todaysPoints == 100
        proj_t1.subjects[1].points == 200
        proj_t1.subjects[1].totalPoints == 500

        subj2_t1.totalPoints == 500
        subj2_t1.points == 200
        subj2_t1.todaysPoints == 100
        subj2_t1.skillsLevel == 2
        subj2_t1.skills.size() == 1
        subj2_t1.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_t1.skills[0].points == 200
        subj2_t1.skills[0].totalPoints == 500
        subj2_t1.skills[0].todaysPoints == 100

        skillReused_t1.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused_t1.points == 200
        skillReused_t1.totalPoints == 500
        skillReused_t1.todaysPoints == 100

        subj1_t1.totalPoints == 1500
        subj1_t1.points == 200
        subj1_t1.todaysPoints == 100
        subj1_t1.skillsLevel == 1
        subj1_t1.skills.size() == 3
        def skill1_t1 = subj1_t1.skills.find { it.skillId == p1Skills[0].skillId }
        skill1_t1.points == 200
        skill1_t1.totalPoints == 500
        skill1_t1.todaysPoints == 100
        subj1_t1.skills.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1_t1.skills.find { it.skillId == p1Skills[2].skillId }.points == 0

        skillOrig_t1.skillId == p1Skills[0].skillId
        skillOrig_t1.points == 200
        skillOrig_t1.totalPoints == 500
        skillOrig_t1.todaysPoints == 100

        // after 2nd deletion
        proj_t2.points == 200
        proj_t2.todaysPoints == 0
        proj_t2.totalPoints == 2000
        proj_t2.skillsLevel == 1
        proj_t2.subjects[0].todaysPoints == 0
        proj_t2.subjects[0].points == 100
        proj_t2.subjects[0].totalPoints == 1500
        proj_t2.subjects[1].todaysPoints == 0
        proj_t2.subjects[1].points == 100
        proj_t2.subjects[1].totalPoints == 500

        subj2_t2.totalPoints == 500
        subj2_t2.points == 100
        subj2_t2.todaysPoints == 0
        subj2_t2.skillsLevel == 1
        subj2_t2.skills.size() == 1
        subj2_t2.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_t2.skills[0].points == 100
        subj2_t2.skills[0].totalPoints == 500
        subj2_t2.skills[0].todaysPoints == 0

        skillReused_t2.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused_t2.points == 100
        skillReused_t2.totalPoints == 500
        skillReused_t2.todaysPoints == 0

        subj1_t2.totalPoints == 1500
        subj1_t2.points == 100
        subj1_t2.todaysPoints == 0
        subj1_t2.skillsLevel == 0
        subj1_t2.skills.size() == 3
        def skill1_t2 = subj1_t2.skills.find { it.skillId == p1Skills[0].skillId }
        skill1_t2.points == 100
        skill1_t2.totalPoints == 500
        skill1_t2.todaysPoints == 0
        subj1_t2.skills.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1_t2.skills.find { it.skillId == p1Skills[2].skillId }.points == 0

        skillOrig_t2.skillId == p1Skills[0].skillId
        skillOrig_t2.points == 100
        skillOrig_t2.totalPoints == 500
        skillOrig_t2.todaysPoints == 0

        // after 3rd deletion
        proj_t3.points == 0
        proj_t3.todaysPoints == 0
        proj_t3.totalPoints == 2000
        proj_t3.skillsLevel == 0
        proj_t3.subjects[0].todaysPoints == 0
        proj_t3.subjects[0].points == 0
        proj_t3.subjects[0].totalPoints == 1500
        proj_t3.subjects[1].todaysPoints == 0
        proj_t3.subjects[1].points == 0
        proj_t3.subjects[1].totalPoints == 500

        subj2_t3.totalPoints == 500
        subj2_t3.points == 0
        subj2_t3.todaysPoints == 0
        subj2_t3.skillsLevel == 0
        subj2_t3.skills.size() == 1
        subj2_t3.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_t3.skills[0].points == 0
        subj2_t3.skills[0].totalPoints == 500
        subj2_t3.skills[0].todaysPoints == 0

        skillReused_t3.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused_t3.points == 0
        skillReused_t3.totalPoints == 500
        skillReused_t3.todaysPoints == 0

        subj1_t3.totalPoints == 1500
        subj1_t3.points == 0
        subj1_t3.todaysPoints == 0
        subj1_t3.skillsLevel == 0
        subj1_t3.skills.size() == 3
        def skill1_t3 = subj1_t3.skills.find { it.skillId == p1Skills[0].skillId }
        skill1_t3.points == 0
        skill1_t3.totalPoints == 500
        skill1_t3.todaysPoints == 0
        subj1_t3.skills.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1_t3.skills.find { it.skillId == p1Skills[2].skillId }.points == 0

        skillOrig_t3.skillId == p1Skills[0].skillId
        skillOrig_t3.points == 0
        skillOrig_t3.totalPoints == 500
        skillOrig_t3.todaysPoints == 0
    }

    def "remove skill event from a skill under group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 22)
        skillsService.createSkill(p1subj2g2)

        String user = getRandomUsers(1)[0]

        List<Date> dates = [new Date() - 2, new Date() - 1, new Date()]
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: p1Skills[0].skillId], user, dates[0])
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: p1Skills[0].skillId], user, dates[1])
        skillsService.addSkill([projectId: p1Skills[0].projectId, skillId: p1Skills[0].skillId], user, dates[2])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        when:
        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj1 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillOrig = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skillReused = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        skillsService.deleteSkillEvent([projectId: p1.projectId, skillId: p1Skills[0].skillId, userId: user, timestamp: dates[1].time])

        def proj_t1 = skillsService.getSkillSummary(user, p1.projectId)
        def subj1_t1 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def subj2_t1 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillOrig_t1 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skillReused_t1 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        skillsService.deleteSkillEvent([projectId: p1.projectId, skillId: p1Skills[0].skillId, userId: user, timestamp: dates[2].time])

        def proj_t2 = skillsService.getSkillSummary(user, p1.projectId)
        def subj1_t2 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def subj2_t2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillOrig_t2 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skillReused_t2 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

        skillsService.deleteSkillEvent([projectId: p1.projectId, skillId: p1Skills[0].skillId, userId: user, timestamp: dates[0].time])

        def proj_t3 = skillsService.getSkillSummary(user, p1.projectId)
        def subj1_t3 = skillsService.getSkillSummary(user, p1.projectId, p1subj1.subjectId)
        def subj2_t3 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skillOrig_t3 = skillsService.getSingleSkillSummary(user, p1.projectId, p1Skills[0].skillId)
        def skillReused_t3 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))

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
        subj2.skills[0].children.size() == 1
        subj2.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[0].points == 300
        subj2.skills[0].children[0].totalPoints == 500
        subj2.skills[0].children[0].todaysPoints == 100

        skillReused.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused.points == 300
        skillReused.totalPoints == 500
        skillReused.todaysPoints == 100

        subj1.totalPoints == 1500
        subj1.points == 300
        subj1.todaysPoints == 100
        subj1.skillsLevel == 1
        subj1.skills.size() == 1
        subj1.skills[0].children.size() == 3
        def skill1 = subj1.skills[0].children.find { it.skillId == p1Skills[0].skillId }
        skill1.points == 300
        skill1.totalPoints == 500
        skill1.todaysPoints == 100
        subj1.skills[0].children.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1.skills[0].children.find { it.skillId == p1Skills[2].skillId }.points == 0

        skillOrig.skillId == p1Skills[0].skillId
        skillOrig.points == 300
        skillOrig.totalPoints == 500
        skillOrig.todaysPoints == 100

        // after deletion
        proj_t1.points == 400
        proj_t1.todaysPoints == 200
        proj_t1.totalPoints == 2000
        proj_t1.skillsLevel == 1
        proj_t1.subjects[0].todaysPoints == 100
        proj_t1.subjects[0].points == 200
        proj_t1.subjects[0].totalPoints == 1500
        proj_t1.subjects[1].todaysPoints == 100
        proj_t1.subjects[1].points == 200
        proj_t1.subjects[1].totalPoints == 500

        subj2_t1.totalPoints == 500
        subj2_t1.points == 200
        subj2_t1.todaysPoints == 100
        subj2_t1.skillsLevel == 2
        subj2_t1.skills.size() == 1
        subj2_t1.skills[0].children.size() == 1
        subj2_t1.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_t1.skills[0].children[0].points == 200
        subj2_t1.skills[0].children[0].totalPoints == 500
        subj2_t1.skills[0].children[0].todaysPoints == 100

        skillReused_t1.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused_t1.points == 200
        skillReused_t1.totalPoints == 500
        skillReused_t1.todaysPoints == 100

        subj1_t1.totalPoints == 1500
        subj1_t1.points == 200
        subj1_t1.todaysPoints == 100
        subj1_t1.skillsLevel == 1
        subj1_t1.skills.size() == 1
        subj1_t1.skills[0].children.size() == 3
        def skill1_t1 = subj1_t1.skills[0].children.find { it.skillId == p1Skills[0].skillId }
        skill1_t1.points == 200
        skill1_t1.totalPoints == 500
        skill1_t1.todaysPoints == 100
        subj1_t1.skills[0].children.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1_t1.skills[0].children.find { it.skillId == p1Skills[2].skillId }.points == 0

        skillOrig_t1.skillId == p1Skills[0].skillId
        skillOrig_t1.points == 200
        skillOrig_t1.totalPoints == 500
        skillOrig_t1.todaysPoints == 100

        // after 2nd deletion
        proj_t2.points == 200
        proj_t2.todaysPoints == 0
        proj_t2.totalPoints == 2000
        proj_t2.skillsLevel == 1
        proj_t2.subjects[0].todaysPoints == 0
        proj_t2.subjects[0].points == 100
        proj_t2.subjects[0].totalPoints == 1500
        proj_t2.subjects[1].todaysPoints == 0
        proj_t2.subjects[1].points == 100
        proj_t2.subjects[1].totalPoints == 500

        subj2_t2.totalPoints == 500
        subj2_t2.points == 100
        subj2_t2.todaysPoints == 0
        subj2_t2.skillsLevel == 1
        subj2_t2.skills.size() == 1
        subj2_t2.skills[0].children.size() == 1
        subj2_t2.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_t2.skills[0].children[0].points == 100
        subj2_t2.skills[0].children[0].totalPoints == 500
        subj2_t2.skills[0].children[0].todaysPoints == 0

        skillReused_t2.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused_t2.points == 100
        skillReused_t2.totalPoints == 500
        skillReused_t2.todaysPoints == 0

        subj1_t2.totalPoints == 1500
        subj1_t2.points == 100
        subj1_t2.todaysPoints == 0
        subj1_t2.skillsLevel == 0
        subj1_t2.skills.size() == 1
        subj1_t2.skills[0].children.size() == 3
        def skill1_t2 = subj1_t2.skills[0].children.find { it.skillId == p1Skills[0].skillId }
        skill1_t2.points == 100
        skill1_t2.totalPoints == 500
        skill1_t2.todaysPoints == 0
        subj1_t2.skills[0].children.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1_t2.skills[0].children.find { it.skillId == p1Skills[2].skillId }.points == 0

        skillOrig_t2.skillId == p1Skills[0].skillId
        skillOrig_t2.points == 100
        skillOrig_t2.totalPoints == 500
        skillOrig_t2.todaysPoints == 0

        // after 3rd deletion
        proj_t3.points == 0
        proj_t3.todaysPoints == 0
        proj_t3.totalPoints == 2000
        proj_t3.skillsLevel == 0
        proj_t3.subjects[0].todaysPoints == 0
        proj_t3.subjects[0].points == 0
        proj_t3.subjects[0].totalPoints == 1500
        proj_t3.subjects[1].todaysPoints == 0
        proj_t3.subjects[1].points == 0
        proj_t3.subjects[1].totalPoints == 500

        subj2_t3.totalPoints == 500
        subj2_t3.points == 0
        subj2_t3.todaysPoints == 0
        subj2_t3.skillsLevel == 0
        subj2_t3.skills.size() == 1
        subj2_t3.skills[0].children.size() == 1
        subj2_t3.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_t3.skills[0].children[0].points == 0
        subj2_t3.skills[0].children[0].totalPoints == 500
        subj2_t3.skills[0].children[0].todaysPoints == 0

        skillReused_t3.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillReused_t3.points == 0
        skillReused_t3.totalPoints == 500
        skillReused_t3.todaysPoints == 0

        subj1_t3.totalPoints == 1500
        subj1_t3.points == 0
        subj1_t3.todaysPoints == 0
        subj1_t3.skillsLevel == 0
        subj1_t3.skills.size() == 1
        subj1_t3.skills[0].children.size() == 3
        def skill1_t3 = subj1_t3.skills[0].children.find { it.skillId == p1Skills[0].skillId }
        skill1_t3.points == 0
        skill1_t3.totalPoints == 500
        skill1_t3.todaysPoints == 0
        subj1_t3.skills[0].children.find { it.skillId == p1Skills[1].skillId }.points == 0
        subj1_t3.skills[0].children.find { it.skillId == p1Skills[2].skillId }.points == 0

        skillOrig_t3.skillId == p1Skills[0].skillId
        skillOrig_t3.points == 0
        skillOrig_t3.totalPoints == 500
        skillOrig_t3.todaysPoints == 0
    }
}
