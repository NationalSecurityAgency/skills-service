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

import static skills.intTests.utils.SkillsFactory.*

class GroupSkillReusePointsAndAchivementsSpec extends CatalogIntSpec {

    def "skill events are propagated to the reused skills"() {
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
        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)

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
        subj2.skills[0].children.size() == 1
        subj2.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[0].points == 300
        subj2.skills[0].children[0].totalPoints == 500
        subj2.skills[0].children[0].todaysPoints == 100

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.points == 300
        skill.totalPoints == 500
        skill.todaysPoints == 100
    }

    def "skill events are propagated to the reused skills - multiple groups"() {
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

        def p1subj3 = createSubject(1, 3)
        skillsService.createSubject(p1subj3)
        def p1subj3g3 = createSkillsGroup(1, 3, 33)
        skillsService.createSkill(p1subj3g3)
        def p1Skills_subj3 = createSkills(1, 1, 3, 100, 5)
        p1Skills_subj3.each {
            skillsService.assignSkillToSkillsGroup(p1subj3g3.skillId, it)
        }

        def p1subj4 = createSubject(1, 4)
        skillsService.createSubject(p1subj4)
        def p1subj4g4 = createSkillsGroup(1, 4, 44)
        skillsService.createSkill(p1subj4g4)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj3.subjectId, p1subj3g3.skillId) // 1000
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj4.subjectId, p1subj4g4.skillId)

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
        subj2.skills[0].children.size() == 1
        subj2.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[0].points == 300
        subj2.skills[0].children[0].totalPoints == 500
        subj2.skills[0].children[0].todaysPoints == 100

        subj3.totalPoints == 1000
        subj3.points == 300
        subj3.todaysPoints == 100
        subj3.skillsLevel == 2
        subj3.skills.size() == 1
        subj3.skills[0].children.size() == 2
        subj3.skills[0].children[0].skillId == p1Skills_subj3[0].skillId
        subj3.skills[0].children[0].points == 0
        subj3.skills[0].children[0].totalPoints == 500
        subj3.skills[0].children[0].todaysPoints == 0
        subj3.skills[0].children[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        subj3.skills[0].children[1].points == 300
        subj3.skills[0].children[1].totalPoints == 500
        subj3.skills[0].children[1].todaysPoints == 100

        subj4.totalPoints == 500
        subj4.points == 300
        subj4.todaysPoints == 100
        subj4.skillsLevel == 3
        subj4.skills.size() == 1
        subj4.skills[0].children.size() == 1
        subj4.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        subj4.skills[0].children[0].points == 300
        subj4.skills[0].children[0].totalPoints == 500
        subj4.skills[0].children[0].todaysPoints == 100

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
        when:
        skillsService.addSkill(p1Skills[0], user, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)

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
        subj2.skills[0].children.size() == 1
        subj2.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[0].points == 300
        subj2.skills[0].children[0].totalPoints == 500
        subj2.skills[0].children[0].todaysPoints == 100

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.points == 300
        skill.totalPoints == 500
        skill.todaysPoints == 100
    }

    def "when a skill is reused, existing user points are migrated and achievements are created - multiple subjects"() {
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

        def p1subj3 = createSubject(1, 3)
        skillsService.createSubject(p1subj3)
        def p1subj3g3 = createSkillsGroup(1, 3, 33)
        skillsService.createSkill(p1subj3g3)
        def p1Skills_subj3 = createSkills(1, 1, 3, 100, 5)
        p1Skills_subj3.each {
            skillsService.assignSkillToSkillsGroup(p1subj3g3.skillId, it)
        }

        def p1subj4 = createSubject(1, 4)
        skillsService.createSubject(p1subj4)
        def p1subj4g4 = createSkillsGroup(1, 4, 44)
        skillsService.createSkill(p1subj4g4)


        String user = getRandomUsers(1)[0]
        skillsService.addSkill(p1Skills[0], user, new Date() - 2)
        skillsService.addSkill(p1Skills[0], user, new Date() - 1)
        skillsService.addSkill(p1Skills[0], user)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj3.subjectId, p1subj3g3.skillId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj4.subjectId, p1subj4g4.skillId)

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
        subj2.skills[0].children.size() == 1
        subj2.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[0].points == 300
        subj2.skills[0].children[0].totalPoints == 500
        subj2.skills[0].children[0].todaysPoints == 100

        subj3.totalPoints == 1000
        subj3.points == 300
        subj3.todaysPoints == 100
        subj3.skillsLevel == 2
        subj3.skills.size() == 1
        subj3.skills[0].children.size() == 2
        subj3.skills[0].children[0].skillId == p1Skills_subj3[0].skillId
        subj3.skills[0].children[0].points == 0
        subj3.skills[0].children[0].totalPoints == 500
        subj3.skills[0].children[0].todaysPoints == 0
        subj3.skills[0].children[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        subj3.skills[0].children[1].points == 300
        subj3.skills[0].children[1].totalPoints == 500
        subj3.skills[0].children[1].todaysPoints == 100

        subj4.totalPoints == 500
        subj4.points == 300
        subj4.todaysPoints == 100
        subj4.skillsLevel == 3
        subj4.skills.size() == 1
        subj4.skills[0].children.size() == 1
        subj4.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        subj4.skills[0].children[0].points == 300
        subj4.skills[0].children[0].totalPoints == 500
        subj4.skills[0].children[0].todaysPoints == 100

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
        def p1Skills_subj2 = createSkills(1, 1, 2, 100, 5)
        p1Skills_subj2.each {
            skillsService.assignSkillToSkillsGroup(p1subj2g2.skillId, it)
        }

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)

        String user = getRandomUsers(1)[0]
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
        skillsService.assignSkillToSkillsGroup(p1subj2g2.skillId, p1Skills[0])
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
        subj2.skills.size() == 1
        subj2.skills[0].children.size() == 2
        subj2.skills[0].children[0].skillId == p1Skills_subj2[0].skillId
        subj2.skills[0].children[0].points == 0
        subj2.skills[0].children[0].totalPoints == 500
        subj2.skills[0].children[0].todaysPoints == 0
        subj2.skills[0].children[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[1].points == 300
        subj2.skills[0].children[1].totalPoints == 500
        subj2.skills[0].children[1].todaysPoints == 100

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
        subj2_after.skills.size() == 1
        subj2_after.skills[0].children.size() == 2
        subj2_after.skills[0].children[0].skillId == p1Skills_subj2[0].skillId
        subj2_after.skills[0].children[0].points == 0
        subj2_after.skills[0].children[0].totalPoints == 500
        subj2_after.skills[0].children[0].todaysPoints == 0
        subj2_after.skills[0].children[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_after.skills[0].children[1].points == 3000
        subj2_after.skills[0].children[1].totalPoints == 6000
        subj2_after.skills[0].children[1].todaysPoints == 1000

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
        def p1Skills_subj2 = createSkills(1, 1, 2, 100, 5)
        p1Skills_subj2.each {
            skillsService.assignSkillToSkillsGroup(p1subj2g2.skillId, it)
        }


        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)

        String user = getRandomUsers(1)[0]
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
        subj2.skills.size() == 1
        subj2.skills[0].children.size() == 2
        subj2.skills[0].children[0].skillId == p1Skills_subj2[0].skillId
        subj2.skills[0].children[0].points == 0
        subj2.skills[0].children[0].totalPoints == 500
        subj2.skills[0].children[0].todaysPoints == 0
        subj2.skills[0].children[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[1].points == 300
        subj2.skills[0].children[1].totalPoints == 500
        subj2.skills[0].children[1].todaysPoints == 100

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
        subj2_after.skills.size() == 1
        subj2_after.skills[0].children.size() == 2
        subj2_after.skills[0].children[0].skillId == p1Skills_subj2[0].skillId
        subj2_after.skills[0].children[0].points == 0
        subj2_after.skills[0].children[0].totalPoints == 500
        subj2_after.skills[0].children[0].todaysPoints == 0
        subj2_after.skills[0].children[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_after.skills[0].children[1].points == 3000
        subj2_after.skills[0].children[1].totalPoints == 5000
        subj2_after.skills[0].children[1].todaysPoints == 1000

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
        def p1Skills_subj2 = createSkills(1, 1, 2, 100, 5)
        p1Skills_subj2.each {
            skillsService.assignSkillToSkillsGroup(p1subj2g2.skillId, it)
        }

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)

        String user = getRandomUsers(1)[0]
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
        subj2.skills.size() == 1
        subj2.skills[0].children.size() == 2
        subj2.skills[0].children[0].skillId == p1Skills_subj2[0].skillId
        subj2.skills[0].children[0].points == 0
        subj2.skills[0].children[0].totalPoints == 500
        subj2.skills[0].children[0].todaysPoints == 0
        subj2.skills[0].children[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[1].points == 300
        subj2.skills[0].children[1].totalPoints == 500
        subj2.skills[0].children[1].todaysPoints == 100

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
        subj2_after.skills.size() == 1
        subj2_after.skills[0].children.size() == 2
        subj2_after.skills[0].children[0].skillId == p1Skills_subj2[0].skillId
        subj2_after.skills[0].children[0].points == 0
        subj2_after.skills[0].children[0].totalPoints == 500
        subj2_after.skills[0].children[0].todaysPoints == 0
        subj2_after.skills[0].children[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2_after.skills[0].children[1].points == 300
        subj2_after.skills[0].children[1].totalPoints == 300
        subj2_after.skills[0].children[1].todaysPoints == 100

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
}
