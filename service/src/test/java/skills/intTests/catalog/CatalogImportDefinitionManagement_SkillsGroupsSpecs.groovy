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
package skills.intTests.catalog

import groovy.json.JsonOutput
import skills.intTests.utils.SkillsFactory
import spock.lang.IgnoreRest

import static skills.intTests.utils.SkillsFactory.*

class CatalogImportDefinitionManagement_SkillsGroupsSpecs extends CatalogIntSpec {

    def "import group skill from catalog"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])
        def gSkill1 = createSkill(2, 1, 10, 0, 50)
        def gSkill2 = createSkill(2, 1, 11, 0, 50)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill1)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, gSkill2)

        when:
        def project_t0 = skillsService.getProject(p2.projectId)
        def projects_t0 = skillsService.getProjects()
        def subject_t0 = skillsService.getSubject(p2subj1)
        def subjects_t0 = skillsService.getSubjects(p2.projectId)
        def p2subj1Skills_t0 = skillsService.getSkillsForSubject(p2.projectId, p2subj1.subjectId)
        def p2skillsGroupSkills_t0 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)


        skillsService.bulkImportSkillsIntoGroupFromCatalog(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                [[projectId: p1.projectId, skillId: p1Skills[0].skillId]])

        def project_t1 = skillsService.getProject(p2.projectId)
        def projects_t1 = skillsService.getProjects()
        def subject_t1 = skillsService.getSubject(p2subj1)
        def subjects_t1 = skillsService.getSubjects(p2.projectId)
        def p2subj1Skills_t1 = skillsService.getSkillsForSubject(p2.projectId, p2subj1.subjectId)
        def p2skillsGroupSkills_t1 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(p2skillsGroupSkills_t1))

        skillsService.finalizeSkillsImportFromCatalog(p2.projectId)

        def project_t2 = skillsService.getProject(p2.projectId)
        def projects_t2 = skillsService.getProjects()
        def subject_t2 = skillsService.getSubject(p2subj1)
        def subjects_t2 = skillsService.getSubjects(p2.projectId)
        def p2subj1Skills_t2 = skillsService.getSkillsForSubject(p2.projectId, p2subj1.subjectId)
        def p2skillsGroupSkills_t2 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)

        then:
        project_t0.totalPoints == 1000
        projects_t0.totalPoints == [300, 1000]
        subject_t0.totalPoints == 1000
        subjects_t0.totalPoints == [1000]
        p2subj1Skills_t0.skillId == [p2skillsGroup.skillId]
        p2skillsGroupSkills_t0.skillId == [gSkill1.skillId, gSkill2.skillId]

        project_t1.totalPoints == 1000
        projects_t1.totalPoints == [300, 1000]
        subject_t1.totalPoints == 1000
        subjects_t1.totalPoints == [1000]
        p2subj1Skills_t1.skillId == [p2skillsGroup.skillId]
        p2skillsGroupSkills_t1.skillId == [gSkill1.skillId, gSkill2.skillId, p1Skills[0].skillId]
        p2skillsGroupSkills_t1.enabled == [true, true, false]

        subject_t2.totalPoints == 1100
        project_t2.totalPoints == 1100
        projects_t2.totalPoints == [300, 1100]
        subjects_t2.totalPoints == [1100]
        p2subj1Skills_t2.skillId == [p2skillsGroup.skillId]
        p2skillsGroupSkills_t2.skillId == [gSkill1.skillId, gSkill2.skillId, p1Skills[0].skillId]
        p2skillsGroupSkills_t2.enabled == [true, true, true]

        // validate that group was updated too
        skillDefRepo.findByProjectIdAndSkillId(p2.projectId, p2skillsGroup.skillId).totalPoints == 1100
    }

    def "Imported Group Skills: skill definition is synchronized"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])

        skillsService.bulkImportSkillsIntoGroupFromCatalog(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        when:
        def p2skillsGroupSkills_t0 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)

        p1Skills[0].name = "Other name"
        skillsService.createSkill(p1Skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def p2skillsGroupSkills_t1 = skillsService.getSkillsForGroup(p2.projectId, p2skillsGroup.skillId)
        then:
        p2skillsGroupSkills_t0.name == ["Test Skill 1", "Test Skill 2", "Test Skill 3"]
        p2skillsGroupSkills_t1.name == ["Other name", "Test Skill 2", "Test Skill 3"]
    }

    def "Imported Group Skills: skill events are propagated"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])

        String user = getRandomUsers(1)[0]
        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        when:
        def skill1_t0 = skillsService.getSingleSkillSummary(user, p2.projectId, p1Skills[1].skillId)

        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def skill1_t1 = skillsService.getSingleSkillSummary(user, p2.projectId, p1Skills[1].skillId)
        then:
        skill1_t0.points == 0
        skill1_t1.points == 100
    }

    def "Imported Group Skills: change point increment on the imported skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])

        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        when:
        def project_t0 = skillsService.getProject(p2.projectId)
        def subject_t0 = skillsService.getSubject(p2subj1)
        def skill_t0 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p1Skills[1].skillId])

        skillsService.updateImportedSkill(p2.projectId, p1Skills[1].skillId, 453)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def project_t1 = skillsService.getProject(p2.projectId)
        def subject_t1 = skillsService.getSubject(p2subj1)
        def skill_t1 = skillsService.getSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p1Skills[1].skillId])
        then:
        project_t0.totalPoints == 300
        subject_t0.totalPoints == 300
        skill_t0.totalPoints == 100

        project_t1.totalPoints == 653
        subject_t1.totalPoints == 653
        skill_t1.totalPoints == 453
    }

    def "Imported Group Skills: delete imported skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1Skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])

        String user = getRandomUsers(1)[0]

        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                p1Skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        def project_t0 = skillsService.getProject(p2.projectId)
        def subject_t0 = skillsService.getSubject(p2subj1)

        skillsService.addSkill([projectId: p1.projectId, skillId: p1Skills[1].skillId], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def user_skill1_t0 = skillsService.getSkillSummary(user, p2.projectId, p2subj1.subjectId)

        when:
        skillsService.deleteSkill([projectId: p2.projectId, subjectId: p2subj1.subjectId, skillId: p1Skills[1].skillId])

        def project_t1 = skillsService.getProject(p2.projectId)
        def subject_t1 = skillsService.getSubject(p2subj1)
        def user_skill1_t1 = skillsService.getSkillSummary(user, p2.projectId, p2subj1.subjectId)

        then:
        project_t0.totalPoints == 300
        subject_t0.totalPoints == 300
        user_skill1_t0.points == 100
        user_skill1_t0.todaysPoints == 100
        user_skill1_t0.skills[0].children.skill == ['Test Skill 1', 'Test Skill 2', 'Test Skill 3']

        project_t1.totalPoints == 200
        subject_t1.totalPoints == 200
        user_skill1_t1.points == 0
        user_skill1_t1.todaysPoints == 0
        user_skill1_t1.skills[0].children.skill == ['Test Skill 1', 'Test Skill 3']
    }
}

