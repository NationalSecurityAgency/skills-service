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

class SkillReuseManagementSpec extends CatalogIntSpec {

    def "reuse skill in another subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        when:
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        def projStat = skillsService.getProject(p1.projectId)
        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)
        def subjStats = skillsService.getSubject(p1subj2)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])

        then:
        projStat.numSubjects == 2
        projStat.numSkills == 4
        projStat.totalPoints == 400

        subjSkills.size() == 1
        subjSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subjSkills[0].name == p1Skills[0].name
        subjSkills[0].reusedSkill
        subjSkills[0].totalPoints == 100

        subjStats.numSkills == 1
        subjStats.totalPoints == 100

        skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillAdminInfo.name == p1Skills[0].name
    }

    def "reuse skill in multiple subjects"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p1subj4 = createSubject(1, 4)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.createSubject(p1subj3)
        def p1Skills_subj3 = createSkills(1, 1, 3, 100)
        skillsService.createSkills(p1Skills_subj3)
        skillsService.createSubject(p1subj4)

        when:
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj3.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj4.subjectId)

        def projStat = skillsService.getProject(p1.projectId)
        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)
        def subjStats2 = skillsService.getSubject(p1subj2)
        def subjStats3 = skillsService.getSubject(p1subj3)
        def subjStats4 = skillsService.getSubject(p1subj4)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])
        def skillAdminInfo1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj3.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)])
        def skillAdminInfo2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj4.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)])

        then:
        projStat.numSubjects == 4
        projStat.numSkills == 7
        projStat.totalPoints == 700

        subjSkills.size() == 1
        subjSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subjSkills[0].name == p1Skills[0].name
        subjSkills[0].reusedSkill
        subjSkills[0].totalPoints == 100

        subjStats2.numSkills == 1
        subjStats2.totalPoints == 100

        subjStats3.numSkills == 2
        subjStats3.totalPoints == 200

        subjStats4.numSkills == 1
        subjStats4.totalPoints == 100

        skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillAdminInfo.name == p1Skills[0].name

        skillAdminInfo1.reusedSkill
        skillAdminInfo1.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        skillAdminInfo1.name == p1Skills[0].name

        skillAdminInfo2.reusedSkill
        skillAdminInfo2.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        skillAdminInfo2.name == p1Skills[0].name
    }

    def "skills display shows reused skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        String user = getRandomUsers(1)[0]
        when:
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        then:
        proj.totalPoints == 400
        proj.subjects[0].totalPoints == 300
        proj.subjects[1].totalPoints == 100

        subj2.totalPoints == 100
        subj2.skills.size() == 1
        subj2.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].skill == p1Skills[0].name
        !subj2.skills[0].copiedFromProjectId
        !subj2.skills[0].copiedFromProjectName

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.skill == p1Skills[0].name
        !skill.copiedFromProjectId
        !skill.copiedFromProjectName
    }

    def "skills display shows reused skill - multiple subjects"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p1subj4 = createSubject(1, 4)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.createSubject(p1subj3)
        def p1Skills_subj3 = createSkills(1, 1, 3, 100)
        skillsService.createSkills(p1Skills_subj3)
        skillsService.createSubject(p1subj4)

        when:
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj3.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj4.subjectId)

        String user = getRandomUsers(1)[0]
        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def subj3 = skillsService.getSkillSummary(user, p1.projectId, p1subj3.subjectId)
        def subj4 = skillsService.getSkillSummary(user, p1.projectId, p1subj4.subjectId)
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        def skill1 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1))
        def skill2 = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2))

        then:
        proj.totalPoints == 700
        proj.subjects.size() == 4
        proj.subjects[0].totalPoints == 300
        proj.subjects[1].totalPoints == 100
        proj.subjects[2].totalPoints == 200
        proj.subjects[3].totalPoints == 100

        subj2.totalPoints == 100
        subj2.skills.size() == 1
        subj2.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].skill == p1Skills[0].name
        !subj2.skills[0].copiedFromProjectId
        !subj2.skills[0].copiedFromProjectName

        subj3.totalPoints == 200
        subj3.skills.size() == 2
        subj3.skills[0].skillId == p1Skills_subj3[0].skillId
        subj3.skills[0].skill == p1Skills_subj3[0].name
        !subj3.skills[0].copiedFromProjectId
        !subj3.skills[0].copiedFromProjectName
        subj3.skills[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        subj3.skills[1].skill == p1Skills[0].name
        !subj3.skills[1].copiedFromProjectId
        !subj3.skills[1].copiedFromProjectName

        subj4.totalPoints == 100
        subj4.skills.size() == 1
        subj4.skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        subj4.skills[0].skill == p1Skills[0].name
        !subj4.skills[0].copiedFromProjectId
        !subj4.skills[0].copiedFromProjectName

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.skill == p1Skills[0].name
        !skill.copiedFromProjectId
        !skill.copiedFromProjectName

        skill1.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        skill1.skill == p1Skills[0].name
        !skill1.copiedFromProjectId
        !skill1.copiedFromProjectName

        skill2.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        skill2.skill == p1Skills[0].name
        !skill2.copiedFromProjectId
        !skill2.copiedFromProjectName
    }

}
