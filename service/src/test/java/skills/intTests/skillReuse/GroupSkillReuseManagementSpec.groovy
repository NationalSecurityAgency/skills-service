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
import spock.lang.IgnoreRest

import static skills.intTests.utils.SkillsFactory.*

class GroupSkillReuseManagementSpec extends CatalogIntSpec {

    def "reuse skill in another group under a different subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        def p1subj2g1 = createSkillsGroup(1, 2, 11)
        skillsService.createSubject(p1subj2)
        skillsService.createSkill(p1subj2g1)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)

        def projStat = skillsService.getProject(p1.projectId)
        def groupSkills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)
        def subjStats = skillsService.getSubject(p1subj2)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])

        then:
        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 1
        projStat.totalPointsReused == 100

        groupSkills.size() == 1
        groupSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        groupSkills[0].name == p1Skills[0].name
        groupSkills[0].reusedSkill
        groupSkills[0].totalPoints == 100

        subjStats.numSkills == 0
        subjStats.totalPoints == 0
        subjStats.numSkillsReused == 1
        subjStats.totalPointsReused == 100

        skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillAdminInfo.name == p1Skills[0].name
    }

    def "reuse skill in another group under the SAME subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2g1 = createSkillsGroup(1, 1, 11)
        skillsService.createSkill(p1subj2g1)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId, p1subj2g1.skillId)

        def projStat = skillsService.getProject(p1.projectId)
        def groupSkills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)
        def subjStats = skillsService.getSubject(p1subj1)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])

        then:
        projStat.numSubjects == 1
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 1
        projStat.totalPointsReused == 100

        groupSkills.size() == 1
        groupSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        groupSkills[0].name == p1Skills[0].name
        groupSkills[0].reusedSkill
        groupSkills[0].totalPoints == 100

        subjStats.numSkills == 3
        subjStats.totalPoints == 300
        subjStats.numSkillsReused == 1
        subjStats.totalPointsReused == 100

        skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillAdminInfo.name == p1Skills[0].name
    }

    def "reuse group skill in another group under a different subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        def p1subj2g1 = createSkillsGroup(1, 2, 11)
        skillsService.createSubject(p1subj2)
        skillsService.createSkill(p1subj2g1)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)

        def projStat = skillsService.getProject(p1.projectId)
        def groupSkills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)
        def subjStats = skillsService.getSubject(p1subj2)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])

        then:
        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 1
        projStat.totalPointsReused == 100

        groupSkills.size() == 1
        groupSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        groupSkills[0].name == p1Skills[0].name
        groupSkills[0].reusedSkill
        groupSkills[0].totalPoints == 100

        subjStats.numSkills == 0
        subjStats.totalPoints == 0
        subjStats.numSkillsReused == 1
        subjStats.totalPointsReused == 100

        skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillAdminInfo.name == p1Skills[0].name
    }

    def "reuse group skill in another group under the SAME subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2g1 = createSkillsGroup(1, 1, 11)
        skillsService.createSkill(p1subj2g1)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId, p1subj2g1.skillId)

        def projStat = skillsService.getProject(p1.projectId)
        def groupSkills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)
        def subjStats = skillsService.getSubject(p1subj1)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])

        then:
        projStat.numSubjects == 1
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 1
        projStat.totalPointsReused == 100

        groupSkills.size() == 1
        groupSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        groupSkills[0].name == p1Skills[0].name
        groupSkills[0].reusedSkill
        groupSkills[0].totalPoints == 100

        subjStats.numSkills == 3
        subjStats.totalPoints == 300
        subjStats.numSkillsReused == 1
        subjStats.totalPointsReused == 100

        skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillAdminInfo.name == p1Skills[0].name
    }

    def "reuse group skill in its parent subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 8)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])
        skillsService.createSkill(p1subj1g1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId)

        def projStat = skillsService.getProject(p1.projectId)
        def subjectSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subjStats = skillsService.getSubject(p1subj1)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])

        then:
        projStat.numSubjects == 1
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 1
        projStat.totalPointsReused == 100

        subjectSkills.skillId == [p1subj1g1.skillId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)]

        subjStats.numSkills == 3
        subjStats.totalPoints == 300
        subjStats.numSkillsReused == 1
        subjStats.totalPointsReused == 100

        skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillAdminInfo.name == p1Skills[0].name
    }

    def "reuse skill in multiple groups"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p1subj4 = createSubject(1, 4)

        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 11)
        skillsService.createSkill(p1subj2g1)

        skillsService.createSubject(p1subj3)
        def p1subj3g2 = createSkillsGroup(1, 3, 12)
        skillsService.createSkill(p1subj3g2)

        def p1Skills_subj3 = createSkills(1, 1, 3, 100)
        skillsService.createSkills(p1Skills_subj3)
        skillsService.createSubject(p1subj4)
        def p1subj4g3 = createSkillsGroup(1, 4, 13)
        skillsService.createSkill(p1subj4g3)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj3.subjectId, p1subj3g2.skillId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj4.subjectId, p1subj4g3.skillId)

        def projStat = skillsService.getProject(p1.projectId)
        def groupSkills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)
        def subjStats2 = skillsService.getSubject(p1subj2)
        def subjStats3 = skillsService.getSubject(p1subj3)
        def subjStats4 = skillsService.getSubject(p1subj4)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])
        def skillAdminInfo1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj3.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)])
        def skillAdminInfo2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj4.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)])

        then:
        projStat.numSubjects == 4
        projStat.numSkills == 4
        projStat.totalPoints == 400
        projStat.numSkillsReused == 3
        projStat.totalPointsReused == 300

        groupSkills.size() == 1
        groupSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        groupSkills[0].name == p1Skills[0].name
        groupSkills[0].reusedSkill
        groupSkills[0].totalPoints == 100

        subjStats2.numSkills == 0
        subjStats2.totalPoints == 0
        subjStats2.numSkillsReused == 1
        subjStats2.totalPointsReused == 100

        subjStats3.numSkills == 1
        subjStats3.totalPoints == 100
        subjStats3.numSkillsReused == 1
        subjStats3.totalPointsReused == 100

        subjStats4.numSkills == 0
        subjStats4.totalPoints == 0
        subjStats4.numSkillsReused == 1
        subjStats4.totalPointsReused == 100

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

    def "skills display shows reused skill under a group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 11)
        skillsService.createSkill(p1subj2g1)

        String user = getRandomUsers(1)[0]
        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)

        def proj = skillsService.getSkillSummary(user, p1.projectId)
        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def skill = skillsService.getSingleSkillSummary(user, p1.projectId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        then:
        proj.totalPoints == 400
        proj.subjects[0].totalPoints == 300
        proj.subjects[1].totalPoints == 100

        subj2.totalPoints == 100
        subj2.skills.size() == 1
        subj2.skills[0].skillId == p1subj2g1.skillId
        subj2.skills[0].children.size() == 1
        subj2.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[0].skill == p1Skills[0].name
        !subj2.skills[0].children[0].copiedFromProjectId
        !subj2.skills[0].children[0].copiedFromProjectName

        skill.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skill.skill == p1Skills[0].name
        !skill.copiedFromProjectId
        !skill.copiedFromProjectName
    }

    def "skills display shows reused skill - multiple groups"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p1subj4 = createSubject(1, 4)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        def p1subj2g1 = createSkillsGroup(1, 2, 11)
        skillsService.createSkill(p1subj2g1)

        skillsService.createSubject(p1subj3)
        def p1subj3g2 = createSkillsGroup(1, 3, 12)
        skillsService.createSkill(p1subj3g2)
        def p1Skills_subj3 = createSkills(1, 1, 3, 100)
        p1Skills_subj3.each {
            skillsService.assignSkillToSkillsGroup(p1subj3g2.skillId, it)
        }
        skillsService.createSubject(p1subj4)
        def p1subj4g3 = createSkillsGroup(1, 4, 13)
        skillsService.createSkill(p1subj4g3)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj3.subjectId, p1subj3g2.skillId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj4.subjectId, p1subj4g3.skillId)

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
        subj2.skills[0].children.size() == 1
        subj2.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2.skills[0].children[0].skill == p1Skills[0].name
        !subj2.skills[0].children[0].copiedFromProjectId
        !subj2.skills[0].children[0].copiedFromProjectName

        subj3.totalPoints == 200
        subj3.skills.size() == 1
        subj3.skills[0].children.size() == 2
        subj3.skills[0].children[0].skillId == p1Skills_subj3[0].skillId
        subj3.skills[0].children[0].skill == p1Skills_subj3[0].name
        !subj3.skills[0].children[0].copiedFromProjectId
        !subj3.skills[0].children[0].copiedFromProjectName
        subj3.skills[0].children[1].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        subj3.skills[0].children[1].skill == p1Skills[0].name
        !subj3.skills[0].children[1].copiedFromProjectId
        !subj3.skills[0].children[1].copiedFromProjectName

        subj4.totalPoints == 100
        subj4.skills.size() == 1
        subj4.skills.children.size() == 1
        subj4.skills[0].children[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        subj4.skills[0].children[0].skill == p1Skills[0].name
        !subj4.skills[0].children[0].copiedFromProjectId
        !subj4.skills[0].children[0].copiedFromProjectName

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

    def "get reused skills for a group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)


        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 12)
        skillsService.createSkill(p1subj2g1)
        def p1SkillsGroup1 = createSkills(3, 1, 2, 100)
        p1SkillsGroup1.each {
            skillsService.assignSkillToSkillsGroup(p1subj2g1.skillId, it)
        }

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)

        def reusedGroup1 = skillsService.getReusedSkills(p1.projectId, p1subj2g1.skillId)
        def reusedSubj1 = skillsService.getReusedSkills(p1.projectId, p1subj1.subjectId)
        def reusedSubj2 = skillsService.getReusedSkills(p1.projectId, p1subj2.subjectId)

        then:
        !reusedSubj1
        !reusedSubj2
        reusedGroup1.name == ["Test Skill 1"]
    }

    def "skill modifications are propagated to the re-used skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 12)
        def p1Skills = createSkills(3, 1, 1, 100)

        p1Skills[0].description = "Original Desc"
        p1Skills[0].helpUrl = "http://veryOriginal.com"
        p1Skills[0].skillId = "originalSkillId"
        p1Skills[0].name = "Original Name"
        p1Skills[0].pointIncrement = 33
        p1Skills[0].numPerformToCompletion = 6
        p1Skills[0].pointIncrementInterval = 520
        p1Skills[0].numMaxOccurrencesIncrementInterval = 2
        p1Skills[0].iconClass = 'fa fa-icon-test'

        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 12)
        skillsService.createSkill(p1subj2g2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)
        String user = getRandomUsers(1)[0]
        when:
        def subj2_before = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def subj2_desc_before = skillsService.getSubjectDescriptions(p1.projectId, p1subj2.subjectId, user)
        def projStat_before = skillsService.getProject(p1.projectId)
        def subjStats_before = skillsService.getSubject(p1subj2)
        def groupSkills_before = skillsService.getSkillsForGroup(p1.projectId, p1subj2g2.skillId)

        String originalSkillId = p1Skills[0].skillId
        p1Skills[0].name = "New Name"
        p1Skills[0].description = "New Desc"
        p1Skills[0].helpUrl = "http://sonew.com"
        p1Skills[0].skillId = "newSkillId"
        p1Skills[0].pointIncrement = 22
        p1Skills[0].numPerformToCompletion = 10
        p1Skills[0].pointIncrementInterval = 600
        p1Skills[0].numMaxOccurrencesIncrementInterval = 1
        p1Skills[0].iconClass = 'fa fa-icon-new-test'
        p1Skills[0].selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.updateSkill(p1Skills[0], originalSkillId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def subj2 = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def subj2_desc = skillsService.getSubjectDescriptions(p1.projectId, p1subj2.subjectId, user)
        def projStat = skillsService.getProject(p1.projectId)
        def subjStats = skillsService.getSubject(p1subj2)
        def groupSkills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g2.skillId)

        then:
        subj2_before.skills[0].children[0].skill == "Original Name"
        subj2_before.skills[0].children[0].skillId == SkillReuseIdUtil.addTag("originalSkillId", 0)
        subj2_before.skills[0].children[0].totalPoints == 6 * 33
        subj2_before.skills[0].children[0].pointIncrementInterval == 520
        subj2_before.skills[0].children[0].maxOccurrencesWithinIncrementInterval == 2
        !subj2_before.skills[0].children[0].selfReporting.enabled
        def skillDescBefore = subj2_desc_before.find { it.skillId == SkillReuseIdUtil.addTag("originalSkillId", 0) }
        skillDescBefore.description == "Original Desc"
        skillDescBefore.href == "http://veryOriginal.com"

        projStat_before.numSubjects == 2
        projStat_before.numSkills == 3
        projStat_before.totalPoints == 200 + (6 * 33)
        projStat_before.numSkillsReused == 1
        projStat_before.totalPointsReused == 6 * 33

        subjStats_before.numSkills == 0
        subjStats_before.totalPoints == 0
        subjStats_before.numSkillsReused == 1
        subjStats_before.totalPointsReused == 6 * 33

        groupSkills_before.size() == 1
        groupSkills_before[0].skillId == SkillReuseIdUtil.addTag("originalSkillId", 0)
        groupSkills_before[0].name == "Original Name"
        groupSkills_before[0].reusedSkill
        groupSkills_before[0].totalPoints == 6 * 33
        groupSkills_before[0].pointIncrementInterval == 520
        groupSkills_before[0].numMaxOccurrencesIncrementInterval == 2
        groupSkills_before[0].iconClass == 'fa fa-icon-test'

        // after
        subj2.skills[0].children[0].skill == "New Name"
        subj2.skills[0].children[0].skillId == SkillReuseIdUtil.addTag("newSkillId", 0)
        subj2.skills[0].children[0].totalPoints == 10 * 22
        subj2.skills[0].children[0].pointIncrementInterval == 600
        subj2.skills[0].children[0].maxOccurrencesWithinIncrementInterval == 1
        subj2.skills[0].children[0].selfReporting.enabled
        subj2.skills[0].children[0].selfReporting.type == "Approval"
        def skillDescAfter = subj2_desc.find { it.skillId == SkillReuseIdUtil.addTag("newSkillId", 0) }
        skillDescAfter.description == "New Desc"
        skillDescAfter.href == "http://sonew.com"

        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 200 + (10 * 22)
        projStat.numSkillsReused == 1
        projStat.totalPointsReused == 10 * 22

        subjStats.numSkills == 0
        subjStats.totalPoints == 0
        subjStats.numSkillsReused == 1
        subjStats.totalPointsReused == 10 * 22

        groupSkills.size() == 1
        groupSkills[0].skillId == SkillReuseIdUtil.addTag("newSkillId", 0)
        groupSkills[0].name == "New Name"
        groupSkills[0].reusedSkill
        groupSkills[0].totalPoints == 10 * 22
        groupSkills[0].pointIncrementInterval == 600
        groupSkills[0].numMaxOccurrencesIncrementInterval == 1
        groupSkills[0].iconClass == 'fa fa-icon-new-test'
    }

    def "delete reused group skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p1subj4 = createSubject(1, 4)

        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 11)
        skillsService.createSkill(p1subj2g1)

        skillsService.createSubject(p1subj3)
        def p1subj3g2 = createSkillsGroup(1, 3, 12)
        skillsService.createSkill(p1subj3g2)

        def p1Skills_subj3 = createSkills(1, 1, 3, 100)
        skillsService.createSkills(p1Skills_subj3)
        skillsService.createSubject(p1subj4)
        def p1subj4g3 = createSkillsGroup(1, 4, 13)
        skillsService.createSkill(p1subj4g3)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj3.subjectId, p1subj3g2.skillId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj4.subjectId, p1subj4g3.skillId)

        def projStat = skillsService.getProject(p1.projectId)
        def groupSkills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)
        def subjStats2 = skillsService.getSubject(p1subj2)
        def subjStats3 = skillsService.getSubject(p1subj3)
        def subjStats4 = skillsService.getSubject(p1subj4)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])
        def skillAdminInfo1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj3.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)])
        def skillAdminInfo2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj4.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)])

        skillsService.deleteSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])

        def projStat_after = skillsService.getProject(p1.projectId)
        def groupSkills_after = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)
        def subjStats1_after = skillsService.getSubject(p1subj1)
        def subjStats2_after = skillsService.getSubject(p1subj2)
        def subjStats3_after = skillsService.getSubject(p1subj3)
        def subjStats4_after = skillsService.getSubject(p1subj4)
        def skillAdminInfo1_after = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj3.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)])
        def skillAdminInfo2_after = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj4.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)])

        then:
        projStat.numSubjects == 4
        projStat.numSkills == 4
        projStat.totalPoints == 400
        projStat.numSkillsReused == 3
        projStat.totalPointsReused == 300

        groupSkills.size() == 1
        groupSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        groupSkills[0].name == p1Skills[0].name
        groupSkills[0].reusedSkill
        groupSkills[0].totalPoints == 100

        subjStats2.numSkills == 0
        subjStats2.totalPoints == 0
        subjStats2.numSkillsReused == 1
        subjStats2.totalPointsReused == 100

        subjStats3.numSkills == 1
        subjStats3.totalPoints == 100
        subjStats3.numSkillsReused == 1
        subjStats3.totalPointsReused == 100

        subjStats4.numSkills == 0
        subjStats4.totalPoints == 0
        subjStats4.numSkillsReused == 1
        subjStats4.totalPointsReused == 100

        skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillAdminInfo.name == p1Skills[0].name

        skillAdminInfo1.reusedSkill
        skillAdminInfo1.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        skillAdminInfo1.name == p1Skills[0].name

        skillAdminInfo2.reusedSkill
        skillAdminInfo2.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        skillAdminInfo2.name == p1Skills[0].name

        // after
        projStat_after.numSubjects == 4
        projStat_after.numSkills == 4
        projStat_after.totalPoints == 400
        projStat_after.numSkillsReused == 2
        projStat_after.totalPointsReused == 200

        groupSkills_after.size() == 0

        subjStats1_after.numSkills == 3
        subjStats1_after.totalPoints == 300
        subjStats1_after.numSkillsReused == 0
        subjStats1_after.totalPointsReused == 0

        subjStats2_after.numSkills == 0
        subjStats2_after.totalPoints == 0

        subjStats3_after.numSkills == 1
        subjStats3_after.totalPoints == 100
        subjStats3_after.numSkillsReused == 1
        subjStats3_after.totalPointsReused == 100

        subjStats4_after.numSkills == 0
        subjStats4_after.totalPoints == 0
        subjStats4_after.numSkillsReused == 1
        subjStats4_after.totalPointsReused == 100

        skillAdminInfo1_after.reusedSkill
        skillAdminInfo1_after.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        skillAdminInfo1_after.name == p1Skills[0].name

        skillAdminInfo2_after.reusedSkill
        skillAdminInfo2_after.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        skillAdminInfo2_after.name == p1Skills[0].name
    }

    def "delete original group skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p1subj4 = createSubject(1, 4)

        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        def p1subj2g1 = createSkillsGroup(1, 2, 11)
        skillsService.createSkill(p1subj2g1)

        skillsService.createSubject(p1subj3)
        def p1subj3g2 = createSkillsGroup(1, 3, 12)
        skillsService.createSkill(p1subj3g2)

        def p1Skills_subj3 = createSkills(1, 1, 3, 100)
        skillsService.createSkills(p1Skills_subj3)
        skillsService.createSubject(p1subj4)
        def p1subj4g3 = createSkillsGroup(1, 4, 13)
        skillsService.createSkill(p1subj4g3)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g1.skillId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj3.subjectId, p1subj3g2.skillId)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj4.subjectId, p1subj4g3.skillId)

        def projStat = skillsService.getProject(p1.projectId)
        def groupSkills = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)
        def subjStats2 = skillsService.getSubject(p1subj2)
        def subjStats3 = skillsService.getSubject(p1subj3)
        def subjStats4 = skillsService.getSubject(p1subj4)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])
        def skillAdminInfo1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj3.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)])
        def skillAdminInfo2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj4.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)])

        skillsService.deleteSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])

        def projStat_after = skillsService.getProject(p1.projectId)
        def groupSkills_after = skillsService.getSkillsForGroup(p1.projectId, p1subj2g1.skillId)
        def subjStats1_after = skillsService.getSubject(p1subj1)
        def subjStats2_after = skillsService.getSubject(p1subj2)
        def subjStats3_after = skillsService.getSubject(p1subj3)
        def subjStats4_after = skillsService.getSubject(p1subj4)

        then:
        projStat.numSubjects == 4
        projStat.numSkills == 4
        projStat.totalPoints == 400
        projStat.numSkillsReused == 3
        projStat.totalPointsReused == 300

        groupSkills.size() == 1
        groupSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        groupSkills[0].name == p1Skills[0].name
        groupSkills[0].reusedSkill
        groupSkills[0].totalPoints == 100

        subjStats2.numSkills == 0
        subjStats2.totalPoints == 0
        subjStats2.numSkillsReused == 1
        subjStats2.totalPointsReused == 100

        subjStats3.numSkills == 1
        subjStats3.totalPoints == 100
        subjStats3.numSkillsReused == 1
        subjStats3.totalPointsReused == 100

        subjStats4.numSkills == 0
        subjStats4.totalPoints == 0
        subjStats4.numSkillsReused == 1
        subjStats4.totalPointsReused == 100

        skillAdminInfo.reusedSkill
        skillAdminInfo.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        skillAdminInfo.name == p1Skills[0].name

        skillAdminInfo1.reusedSkill
        skillAdminInfo1.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)
        skillAdminInfo1.name == p1Skills[0].name

        skillAdminInfo2.reusedSkill
        skillAdminInfo2.skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)
        skillAdminInfo2.name == p1Skills[0].name

        // after
        projStat_after.numSubjects == 4
        projStat_after.numSkills == 3
        projStat_after.totalPoints == 300
        projStat_after.numSkillsReused == 0
        projStat_after.totalPointsReused == 0

        groupSkills_after.size() == 0

        subjStats1_after.numSkills == 2
        subjStats1_after.totalPoints == 200
        subjStats1_after.numSkillsReused == 0
        subjStats1_after.totalPointsReused == 0

        subjStats2_after.numSkills == 0
        subjStats2_after.totalPoints == 0
        subjStats2_after.numSkillsReused == 0
        subjStats2_after.totalPointsReused == 0

        subjStats3_after.numSkills == 1
        subjStats3_after.totalPoints == 100
        subjStats3_after.numSkillsReused == 0
        subjStats3_after.totalPointsReused == 0

        subjStats4_after.numSkills == 0
        subjStats4_after.totalPoints == 0
        subjStats4_after.numSkillsReused == 0
        subjStats4_after.totalPointsReused == 0
    }
}
