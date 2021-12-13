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
package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.storage.model.SkillDef
import skills.intTests.utils.SkillsFactory
import spock.lang.IgnoreRest

import static skills.intTests.utils.SkillsFactory.*

class CatalogSkillTests extends DefaultIntSpec {

    def "add skill to catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def subj1 = createSubject(1, 1)
        /* int projNumber = 1, int subjNumber = 1, int skillNumber = 1, int version = 0, int numPerformToCompletion = 1, pointIncrementInterval = 480, pointIncrement = 10, type="Skill" */
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(subj1)
        skillsService.createSkill(skill)

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        def res = skillsService.getCatalogSkills(project2.projectId, 5, 1)

        then:
        res
        res.totalCount == 1
        res.data[0].skillId == skill.skillId
        res.data[0].projectId == skill.projectId
        res.data[0].projectName == project1.name
        res.data[0].description == skill.description
        res.data[0].pointIncrement == skill.pointIncrement
        res.data[0].numPerformToCompletion == skill.numPerformToCompletion
    }

    def "bulk export skills to catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def subj1 = createSubject(1, 1)
        def subj2 = createSubject(1, 2)
        /* int projNumber = 1, int subjNumber = 1, int skillNumber = 1, int version = 0, int numPerformToCompletion = 1, pointIncrementInterval = 480, pointIncrement = 10, type="Skill" */
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 2, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        when:
        skillsService.bulkExportSkillsToCatalog(project1.projectId, [skill.skillId, skill2.skillId, skill3.skillId])
        def res = skillsService.getCatalogSkills(project2.projectId, 5, 1)

        then:
        res
        res.totalCount == 3
        res.data[0].skillId == skill.skillId
        res.data[1].skillId == skill2.skillId
        res.data[2].skillId == skill3.skillId
    }

    def "update skill that has been exported to catalog"() {
        //changes should be reflected across all copies
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        /* int projNumber = 1, int subjNumber = 1, int skillNumber = 1, int version = 0, int numPerformToCompletion = 1, pointIncrementInterval = 480, pointIncrement = 10, type="Skill" */
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)

        when:
        def preEdit = skillsService.getCatalogSkills(project2.projectId, 10, 1)
        def skillNamePreEdit = skill.name
        skill.name = "edited name"
        skill.numPerformToCompletion = 50
        skillsService.updateSkill(skill, skill.skillId)
        def postEdit = skillsService.getCatalogSkills(project3.projectId, 10, 1)

        then:
        preEdit.data[0].name == skillNamePreEdit
        preEdit.data[0].totalPoints == 250
        preEdit.data[0].numPerformToCompletion == 1
        postEdit.data[0].name == skill.name
        postEdit.data[0].totalPoints == 12500
        postEdit.data[0].numPerformToCompletion == 50
    }

    def "update skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def res = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def importedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])
        importedSkill.name = "a new name"
        importedSkill.subjectId = p2subj1.subjectId
        skillsService.updateSkill(importedSkill, importedSkill.skillId)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("errorCode:ReadOnlySkill")

    }

    def "remove skill from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def p2SkillsPreDelete = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def p3SkillsPreDelete = skillsService.getSkillsForSubject(project3.projectId, p3subj1.subjectId)
        skillsService.deleteSkill(skill)
        def p2Skills = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def p3Skills = skillsService.getSkillsForSubject(project3.projectId, p3subj1.subjectId)

        then:
        p2SkillsPreDelete.find {it.skillId == "skill1"}
        p3SkillsPreDelete.find {it.skillId == "skill1"}
        !p2Skills
        !p3Skills
    }

    def "bulk import skills from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        when:
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, [
                [projectId:project1.projectId, skillId: skill.skillId],
                [projectId: project1.projectId, skillId: skill2.skillId]
        ])

        def skills = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)

        then:
        skills.find { it.skillId == skill.skillId }
        skills.find { it.skillId == skill2.skillId }
        !skills.find { it.skillId == skill3.skillId }
    }

    def "import skill from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        when:
        def projectPreImport = skillsService.getProject(project2.projectId)
        def subjectPreImport = skillsService.getSubject(p2subj1)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def projectPostImport1 = skillsService.getProject(project2.projectId)
        def subjectPostImport1 = skillsService.getSubject(p2subj1)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        def projectPostImport2 = skillsService.getProject(project2.projectId)
        def subjectPostImport2 = skillsService.getSubject(p2subj1)

        then:
        projectPreImport.totalPoints == 0
        projectPostImport1.totalPoints == 250
        projectPostImport2.totalPoints == 500
        subjectPreImport.totalPoints == 0
        subjectPostImport1.totalPoints == 250
        subjectPostImport2.totalPoints == 500
    }

    def "import skill from catalog twice"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("explanation:Cannot import Skill from catalog, [skill1] already exists in Project")
    }

    def "remove imported skill, should have no impact on original skill" () {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)


        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.deleteSkill([projectId: project3.projectId, subjectId: p3subj1.subjectId, skillId: skill.skillId])

        def originalSkill = skillsService.getSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: skill.skillId])
        def p2Copy = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])

        then:
        originalSkill
        originalSkill.skillId == skill.skillId
        p2Copy
        p2Copy.skillId == skill.skillId
    }

    def "report skill event on imported skill not allowed if not self-report"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        skillsService.addSkill([projectId: project3.projectId, skillId: skill.skillId], user)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("explanation:Skills imported from the catalog can only be reported if the original skill is configured for Self Reporting")
    }

    def "report skill event on exported skill, should be reflected in all copies"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 250)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)
        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)
        def p3Stats = skillsService.getUserStats(project3.projectId, user)

        then:
        p1Stats.numSkills == 1
        p1Stats.userTotalPoints == 250
        p2Stats.numSkills == 1
        p2Stats.userTotalPoints == 250
        p3Stats.numSkills == 1
        p3Stats.userTotalPoints == 250
    }

    def "report skill event on original exported skill when original project has insufficient points"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)

        then:
        def exc = thrown(Exception)
        exc.getMessage().contains("explanation:Insufficient project points, skill achievement is disallowed, errorCode:InsufficientProjectPoints")
        skillsService.getUserStats(project2.projectId, user).userTotalPoints == 0
    }

    def "report self-report approval request on skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skill.pointIncrement = 200
        skill.numPerformToCompletion = 1
        skill.selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)
        assert res.body.explanation == "Skill was submitted for approval"

        def p1StatsPre = skillsService.getUserStats(project1.projectId, user)
        def p2StatsPre = skillsService.getUserStats(project2.projectId, user)

        def p1Approvals = skillsService.getApprovals(project1.projectId, 7, 1, 'requestedOn', false)
        def p2Approvals = skillsService.getApprovals(project2.projectId, 7, 1, 'requestedOn', false)

        skillsService.approve(project1.projectId, [p1Approvals.data[0].id])
        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)


        then:
        p1StatsPre.numSkills == 0
        p1StatsPre.userTotalPoints == 0
        p2StatsPre.numSkills == 0
        p2StatsPre.userTotalPoints == 0
        p1Approvals.totalCount == 1
        p1Approvals.data.find { it.userId == user && it.projectId == project1.projectId && it.skillId == skill.skillId }
        p2Approvals.totalCount == 0
        p1Stats.numSkills == 1
        p1Stats.userTotalPoints == 200
        p2Stats.numSkills == 1
        p2Stats.userTotalPoints == 200
    }

    def "delete user skill event for skill imported from catalog"() {
        //should be replicated to all copies
    }

    def "delete user skill event for skill exported to catalog"() {
        //should be replicated to all copies
    }

    def "get all skills exported by project"() {
        //test paging
    }

    def "get all skills imported to project"() {
        //test paging
    }

    def "get exported to catalog stats for project"() {

    }

    def "get imported from catalog stats for project"() {

    }

    def "get exported skill usage stats"() {

    }

    def "get all catalog skills available to project"() {
        //should not include skills exported to catalog by this project
        //or skills already imported from the catalog by this project
        //helpUrl and description should be populated
        //filtering on projectName, subjectName, skillName all need to be validated as well
        //paging needs to be tested

    }

    //TBD
    //skills imported from catalog should have readOnly attribute as true when loading skills or individual skill
    //that has been imported from the catalog
    //need to add tests to verify where and when copiedFromProjectId/copiedFromProjectName are set
    //need to add tests to verify the sharedToCatalog attribute is set
    //need to add tests to verify the subjectId and subjectName attributes are set
    //getting skills for subject should properly populate sharedToCatalogAttribute
    //getting skills for subject should properly populate projectName attribute when a skill has been imported from the catalog
    // --- copiedFrom attribute should also not be present
    //test that addresses exporting skills that have dependencies and what happens when reporting against the imported version
    //test case of deleting exported skill when a project importing the skill has added a local dependency on the imported skill (should work fine but ought to have a test for it specifically)
}

