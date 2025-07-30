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
        projStat.numSkills == 3
        projStat.totalPoints == 300
        projStat.numSkillsReused == 1
        projStat.totalPointsReused == 100

        subjSkills.size() == 1
        subjSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subjSkills[0].name == p1Skills[0].name
        subjSkills[0].reusedSkill
        subjSkills[0].totalPoints == 100

        subjStats.numSkills == 0
        subjStats.totalPoints == 0
        subjStats.numSkillsReused == 1
        subjStats.totalPointsReused == 100

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
        projStat.numSkills == 4
        projStat.totalPoints == 400
        projStat.numSkillsReused == 3
        projStat.totalPointsReused == 300


        subjSkills.size() == 1
        subjSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subjSkills[0].name == p1Skills[0].name
        subjSkills[0].reusedSkill
        subjSkills[0].totalPoints == 100

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

    def "get reused skills for a subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1SkillsSubj2 = createSkills(3, 1, 2, 100)
        skillsService.createSubject(p1subj2)
        skillsService.createSkills(p1SkillsSubj2)

        when:
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        def reusedSubj1 = skillsService.getReusedSkills(p1.projectId, p1subj1.subjectId)
        def reusedSubj2 = skillsService.getReusedSkills(p1.projectId, p1subj2.subjectId)

        then:
        !reusedSubj1
        reusedSubj2.name == ["Test Skill 1"]
    }

    def "skill modifications are propagated to the re-used skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
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

        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        String user = getRandomUsers(1)[0]
        when:
        def subj2_before = skillsService.getSkillSummary(user, p1.projectId, p1subj2.subjectId)
        def subj2_desc_before = skillsService.getSubjectDescriptions(p1.projectId, p1subj2.subjectId, user)
        def projStat_before = skillsService.getProject(p1.projectId)
        def subjStats_before = skillsService.getSubject(p1subj2)
        def subjSkills_before = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)

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
        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)

        then:
        subj2_before.skills[0].skill == "Original Name"
        subj2_before.skills[0].skillId == SkillReuseIdUtil.addTag("originalSkillId", 0)
        subj2_before.skills[0].totalPoints == 6 * 33
        subj2_before.skills[0].pointIncrementInterval == 520
        subj2_before.skills[0].maxOccurrencesWithinIncrementInterval == 2
        !subj2_before.skills[0].selfReporting.enabled
        subj2_desc_before[0].description == "Original Desc"
        subj2_desc_before[0].href == "http://veryOriginal.com"

        projStat_before.numSubjects == 2
        projStat_before.numSkills == 3
        projStat_before.totalPoints == 200 + (6 * 33)
        projStat_before.numSkillsReused == 1
        projStat_before.totalPointsReused == (6 * 33)

        subjStats_before.numSkills == 0
        subjStats_before.totalPoints == 0
        subjStats_before.numSkillsReused == 1
        subjStats_before.totalPointsReused == (6 * 33)

        subjSkills_before.size() == 1
        subjSkills_before[0].skillId == SkillReuseIdUtil.addTag("originalSkillId", 0)
        subjSkills_before[0].name == "Original Name"
        subjSkills_before[0].reusedSkill
        subjSkills_before[0].totalPoints == 6 * 33
        subjSkills_before[0].pointIncrementInterval == 520
        subjSkills_before[0].numMaxOccurrencesIncrementInterval == 2
        subjSkills_before[0].iconClass == 'fa fa-icon-test'

        // after
        subj2.skills[0].skill == "New Name"
        subj2.skills[0].skillId == SkillReuseIdUtil.addTag("newSkillId", 0)
        subj2.skills[0].totalPoints == 10 * 22
        subj2.skills[0].pointIncrementInterval == 600
        subj2.skills[0].maxOccurrencesWithinIncrementInterval == 1
        subj2.skills[0].selfReporting.enabled
        subj2.skills[0].selfReporting.type == "Approval"
        subj2.skills[0].iconClass == 'fa fa-icon-new-test'
        subj2_desc[0].description == "New Desc"
        subj2_desc[0].href == "http://sonew.com"

        projStat.numSubjects == 2
        projStat.numSkills == 3
        projStat.totalPoints == 200 + (10 * 22)
        projStat.numSkillsReused == 1
        projStat.totalPointsReused == (10 * 22)

        subjStats.numSkills == 0
        subjStats.totalPoints == 0
        subjStats.numSkillsReused == 1
        subjStats.totalPointsReused == (10 * 22)

        subjSkills.size() == 1
        subjSkills[0].skillId == SkillReuseIdUtil.addTag("newSkillId", 0)
        subjSkills[0].name == "New Name"
        subjSkills[0].reusedSkill
        subjSkills[0].totalPoints == 10 * 22
        subjSkills[0].pointIncrementInterval == 600
        subjSkills[0].numMaxOccurrencesIncrementInterval == 1
    }

    def "delete reused skill"() {
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

        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj3.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj4.subjectId)

        when:
        def projStat = skillsService.getProject(p1.projectId)
        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)
        def subjStats1 = skillsService.getSubject(p1subj1)
        def subjStats2 = skillsService.getSubject(p1subj2)
        def subjStats3 = skillsService.getSubject(p1subj3)
        def subjStats4 = skillsService.getSubject(p1subj4)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])
        def skillAdminInfo1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj3.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)])
        def skillAdminInfo2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj4.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)])

        skillsService.deleteSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])

        def projStat_after = skillsService.getProject(p1.projectId)
        def subjSkills_after = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)
        def subjStats1_after = skillsService.getSubject(p1subj1)
        def subjStats2_after = skillsService.getSubject(p1subj2)
        def subjStats3_after = skillsService.getSubject(p1subj3)
        def subjStats4_after = skillsService.getSubject(p1subj4)
        def skillAdminInfo1_after = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj3.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)])
        def skillAdminInfo2_after = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj4.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)])

        then:
        projStat.numSubjects == 4
        projStat.numSkills == 4
        projStat.numSkillsReused == 3
        projStat.totalPoints == 400
        projStat.totalPointsReused == 300

        subjSkills.size() == 1
        subjSkills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subjSkills[0].name == p1Skills[0].name
        subjSkills[0].reusedSkill
        subjSkills[0].totalPoints == 100

        subjStats1.numSkills == 3
        subjStats1.totalPoints == 300

        subjStats2.numSkills == 0
        subjStats2.numSkillsReused == 1
        subjStats2.totalPoints == 0
        subjStats2.totalPointsReused == 100

        subjStats3.numSkills == 1
        subjStats3.numSkillsReused == 1
        subjStats3.totalPoints == 100
        subjStats3.totalPointsReused == 100

        subjStats4.numSkills == 0
        subjStats4.numSkillsReused == 1
        subjStats4.totalPoints == 0
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
        projStat_after.numSkillsReused == 2
        projStat_after.totalPoints == 400
        projStat_after.totalPointsReused == 200

        subjSkills_after.size() == 0

        subjStats1_after.numSkills == 3
        subjStats1_after.totalPoints == 300

        subjStats2_after.numSkills == 0
        subjStats2_after.totalPoints == 0
        subjStats2_after.numSkillsReused == 0
        subjStats2_after.totalPointsReused == 0

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

    def "delete the original skill"() {
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

        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj3.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj4.subjectId)

        when:
        def projStat = skillsService.getProject(p1.projectId)
        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)
        def subjStats1 = skillsService.getSubject(p1subj1)
        def subjStats2 = skillsService.getSubject(p1subj2)
        def subjStats3 = skillsService.getSubject(p1subj3)
        def subjStats4 = skillsService.getSubject(p1subj4)
        def skillAdminInfo = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj2.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)])
        def skillAdminInfo1 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj3.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 1)])
        def skillAdminInfo2 = skillsService.getSkill([projectId: p1.projectId, subjectId: p1subj4.subjectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 2)])

        skillsService.deleteSkill([projectId: p1.projectId, subjectId: p1subj1.subjectId, skillId: p1Skills[0].skillId])

        def projStat_after = skillsService.getProject(p1.projectId)
        def subj1Skills_after = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj2Skills_after = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)
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

        subj1Skills.skillId == [p1Skills[0].skillId, p1Skills[1].skillId, p1Skills[2].skillId]

        subj2Skills.size() == 1
        subj2Skills[0].skillId == SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)
        subj2Skills[0].name == p1Skills[0].name
        subj2Skills[0].reusedSkill
        subj2Skills[0].totalPoints == 100

        subjStats1.numSkills == 3
        subjStats1.totalPoints == 300
        subjStats1.numSkillsReused == 0
        subjStats1.totalPointsReused == 0

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

        subj1Skills_after.skillId == [p1Skills[1].skillId, p1Skills[2].skillId]

        subj2Skills_after.size() == 0

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
