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
import skills.intTests.utils.SkillsClientException
import skills.storage.model.SkillDef
import skills.intTests.utils.SkillsFactory
import spock.lang.IgnoreRest
import org.springframework.http.HttpStatus

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

    def "import skill that isn't shared to catalog"() {
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

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        then:
        def e = thrown(Exception)
        e.message.contains("explanation:Skill [skill1] from project [TestProject1] has not been shared to the catalog and may not be imported")
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

    def "delete user skill event for skill in catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skill.pointIncrement = 20
        skill.numPerformToCompletion = 10

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
        Date skillDate = new Date()
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user, skillDate)
        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)
        def p3Stats = skillsService.getUserStats(project3.projectId, user)

        assert p1Stats.userTotalPoints == 20
        assert p2Stats.userTotalPoints == 20
        assert p3Stats.userTotalPoints == 20

        skillsService.deleteSkillEvent([projectId: project1.projectId, skillId: skill.skillId, userId: user, timestamp: skillDate.time])

        def postDeleteP1Stats = skillsService.getUserStats(project1.projectId, user)
        def postDeleteP2Stats = skillsService.getUserStats(project2.projectId, user)
        def postDeleteP3Stats = skillsService.getUserStats(project3.projectId, user)

        then:
        postDeleteP1Stats.userTotalPoints == 0
        postDeleteP2Stats.userTotalPoints == 0
        postDeleteP3Stats.userTotalPoints == 0
    }

    def "delete honor system user skill event for skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skill.pointIncrement = 20
        skill.numPerformToCompletion = 5
        skill.selfReportingType = SkillDef.SelfReportingType.HonorSystem

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
        def timestamp = new Date()
        def res = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user, timestamp)
        def p1Stats = skillsService.getUserStats(project1.projectId, user)
        def p2Stats = skillsService.getUserStats(project2.projectId, user)
        def p3Stats = skillsService.getUserStats(project3.projectId, user)
        assert p1Stats.userTotalPoints == 20
        assert p2Stats.userTotalPoints == 20
        assert p3Stats.userTotalPoints == 20

        skillsService.deleteSkillEvent([projectId: project1.projectId, skillId: skill.skillId, userId: user, timestamp: timestamp.time])

        def postDeleteP1Stats = skillsService.getUserStats(project1.projectId, user)
        def postDeleteP2Stats = skillsService.getUserStats(project2.projectId, user)
        def postDeleteP3Stats = skillsService.getUserStats(project3.projectId, user)

        then:
        postDeleteP1Stats.userTotalPoints == 0
        postDeleteP2Stats.userTotalPoints == 0
        postDeleteP3Stats.userTotalPoints == 0
    }

    def "get all skills exported by project"() {
        def project1 = createProject(1)

        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)
        def skill4 = createSkill(1, 1, 4)
        def skill5 = createSkill(1, 2, 5)
        def skill6 = createSkill(1, 2, 6)

        skillsService.createProject(project1)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        Thread.sleep(1*1000)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        Thread.sleep(1*1000)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        Thread.sleep(1*1000)
        skillsService.exportSkillToCatalog(project1.projectId, skill4.skillId)
        Thread.sleep(1*1000)
        skillsService.exportSkillToCatalog(project1.projectId, skill5.skillId)
        Thread.sleep(1*1000)
        skillsService.exportSkillToCatalog(project1.projectId, skill6.skillId)
        Thread.sleep(1*1000)

        when:

        def pg1s1ExportedOnDesc = skillsService.getExportedSkills(project1.projectId, 1, 1, "exportedOn", false)
        def pg6s1ExportedOnDesc = skillsService.getExportedSkills(project1.projectId, 1, 6, "exportedOn", false)
        def pg1s1ExportedOnAsc = skillsService.getExportedSkills(project1.projectId, 1, 1, "exportedOn", true)
        def pg6s1ExportedOnAsc = skillsService.getExportedSkills(project1.projectId, 1, 6, "exportedOn", true)

        def pg6s6ExportedOnAsc = skillsService.getExportedSkills(project1.projectId, 1, 6, "exportedOn", false)
        def pg6s6ExportedOnDesc = skillsService.getExportedSkills(project1.projectId, 1, 6, "exportedOn", true)

        def pg1s1SkillNameAsc = skillsService.getExportedSkills(project1.projectId, 1, 1, "skillName", true)
        def pg6s1SkillNameAsc = skillsService.getExportedSkills(project1.projectId, 1, 6, "skillName", true)
        def pg1s1SkillNameDesc = skillsService.getExportedSkills(project1.projectId, 1, 1, "skillName", false)
        def pg6s1SkillNameDesc = skillsService.getExportedSkills(project1.projectId, 1, 6, "skillName", false)

        def pg1s6SkillNameAsc = skillsService.getExportedSkills(project1.projectId, 6, 1, "skillName", true)
        def pg1s6SkillNameDesc = skillsService.getExportedSkills(project1.projectId, 6, 1, "skillName", false)

        def pg1s1SubjectNameAsc = skillsService.getExportedSkills(project1.projectId, 1, 1, "subjectName", true)
        def pg6s1SubjectNameAsc = skillsService.getExportedSkills(project1.projectId, 1, 6, "subjectName", true)
        def pg1s1SubjectNameDesc = skillsService.getExportedSkills(project1.projectId, 1, 1, "subjectName", false)
        def pg6s1SubjectNameDesc = skillsService.getExportedSkills(project1.projectId, 1, 6, "subjectName", false)

        def pg1s6SubjectNameAsc = skillsService.getExportedSkills(project1.projectId, 6, 1, "subjectName", true)
        def pg1s6SubjectNameDesc = skillsService.getExportedSkills(project1.projectId, 6, 1, "subjectName", false)

        then:
        pg1s1ExportedOnDesc.data[0].skillId == skill6.skillId
        pg1s1ExportedOnDesc.totalCount == 6
        pg1s1ExportedOnDesc.count == 1
        pg6s1ExportedOnDesc.data[0].skillId == skill.skillId
        pg6s1ExportedOnDesc.totalCount == 6
        pg6s1ExportedOnDesc.count == 1

        pg1s1ExportedOnAsc.data[0].skillId == skill.skillId
        pg1s1ExportedOnAsc.totalCount == 6
        pg1s1ExportedOnAsc.count == 1
        pg6s1ExportedOnAsc.data[0].skillId == skill6.skillId
        pg6s1ExportedOnAsc.totalCount == 6
        pg6s1ExportedOnAsc.count == 1


        pg1s1SkillNameAsc.data[0].skillName == skill.name
        pg1s1SkillNameAsc.totalCount == 6
        pg1s1SkillNameAsc.count == 1
        pg6s1SkillNameAsc.data[0].skillName == skill6.name
        pg6s1SkillNameAsc.totalCount == 6
        pg6s1SkillNameAsc.count == 1

        pg1s1SkillNameDesc.data[0].skillName == skill6.name
        pg1s1SkillNameDesc.totalCount == 6
        pg1s1SkillNameDesc.count == 1
        pg6s1SkillNameDesc.data[0].skillName == skill.name
        pg6s1SkillNameDesc.totalCount == 6
        pg6s1SkillNameDesc.count == 1

        pg1s6SkillNameAsc.data[0].skillId == skill.skillId
        pg1s6SkillNameAsc.data[5].skillId == skill6.skillId
        pg1s6SkillNameAsc.totalCount == 6
        pg1s6SkillNameAsc.count == 6

        pg1s1SubjectNameAsc.data[0].subjectName == p1subj1.name
        pg1s1SubjectNameAsc.totalCount == 6
        pg1s1SubjectNameAsc.count == 1
        pg6s1SubjectNameAsc.data[0].subjectName == p1subj2.name
        pg6s1SubjectNameAsc.totalCount == 6
        pg6s1SubjectNameAsc.count == 1

        pg1s1SubjectNameDesc.data[0].subjectName == p1subj2.name
        pg1s1SubjectNameDesc.totalCount == 6
        pg1s1SubjectNameDesc.count == 1
        pg6s1SubjectNameDesc.data[0].subjectName == p1subj1.name
        pg6s1SubjectNameDesc.totalCount == 6
        pg6s1SubjectNameDesc.count == 1

        pg1s6SubjectNameAsc.data[0].subjectName == p1subj1.name
        pg1s6SubjectNameAsc.data[5].subjectName == p1subj2.name
        pg1s6SubjectNameAsc.totalCount == 6
        pg1s6SubjectNameAsc.count == 6

        pg1s6SubjectNameDesc.data[0].subjectName == p1subj2.name
        pg1s6SubjectNameDesc.data[5].subjectName == p1subj1.name
        pg1s6SubjectNameDesc.totalCount == 6
        pg1s6SubjectNameDesc.count == 6
    }

    def "get all skills imported to project"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(2, 2, 4)
        def skill5 = createSkill(2, 2, 5)
        def skill6 = createSkill(2, 2, 6)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        when:
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)

        def skills = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def skillsForProject = skillsService.getSkillsForProject(project2.projectId)

        then:
        skills.findAll { it.readOnly == true && it.copiedFromProjectId == project1.projectId && it.copiedFromProjectName == project1.name }.size() == 3
        //copiedFromProjectId, copiedFromProjectName, and readOnly are not populated by this endpoint
        skillsForProject.findAll { it.readOnly && it.copiedFromProjectId && it.copiedFromProjectName }.size() == 0
    }

    def "get exported to catalog stats for project"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(1, 1, 4)
        def skill5 = createSkill(1, 1, 5)
        def skill6 = createSkill(1, 1, 6)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)

        when:

        def zeroStats = skillsService.getExportedSkillsForProjectStats(project1.projectId)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)

        def statsOneSkill = skillsService.getExportedSkillsForProjectStats(project1.projectId)

        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        def stats = skillsService.getExportedSkillsForProjectStats(project1.projectId)

        then:
        zeroStats.numberOfProjectsUsing == 0
        zeroStats.numberOfSkillsExported == 0

        statsOneSkill.numberOfProjectsUsing == 0
        statsOneSkill.numberOfSkillsExported == 1

        stats.numberOfProjectsUsing == 1
        stats.numberOfSkillsExported == 3
    }

    def "get imported from catalog stats for project"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)
        def p2subj2 = createSubject(2, 3)
        def p4subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(1, 1, 4)
        def skill5 = createSkill(1, 1, 5)
        def skill6 = createSkill(1, 1, 6)

        def skill7 = createSkill(3, 1, 7)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p2subj2)
        skillsService.createSubject(p4subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createSkill(skill7)

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, skill7.skillId)
        def noImports = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def oneImport = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj2.subjectId, project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)
        def threeImportsDifferentSubjects = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj2.subjectId, project3.projectId, skill7.skillId)
        def fourImporstTwoProjectsTwoSubjects = skillsService.getImportedSkillsStats(project2.projectId)

        then:
        noImports.numberOfProjectsImportedFrom == 0
        noImports.numberOfSkillsImported == 0
        oneImport.numberOfProjectsImportedFrom == 1
        oneImport.numberOfSkillsImported == 1
        threeImportsDifferentSubjects.numberOfProjectsImportedFrom == 1
        threeImportsDifferentSubjects.numberOfSkillsImported == 3
        fourImporstTwoProjectsTwoSubjects.numberOfProjectsImportedFrom == 2
        fourImporstTwoProjectsTwoSubjects.numberOfSkillsImported == 4
    }

    def "get exported skill usage stats"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)
        def p2subj2 = createSubject(2, 3)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(1, 1, 4)
        def skill5 = createSkill(1, 1, 5)
        def skill6 = createSkill(1, 1, 6)

        def skill7 = createSkill(3, 1, 7)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p2subj2)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createSkill(skill7)

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, skill7.skillId)

        def noImports = skillsService.getExportedSkillStats(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def oneImport = skillsService.getExportedSkillStats(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)
        def twoImports = skillsService.getExportedSkillStats(project1.projectId, skill.skillId)

        then:
        noImports.projectId == project1.projectId
        noImports.skillId == skill.skillId
        !noImports.users

        oneImport.projectId == project1.projectId
        oneImport.skillId == skill.skillId
        oneImport.users.size() == 1
        oneImport.users.find { it.importingProjectId == project2.projectId && it.importedIntoSubjectId == p2subj1.subjectId }

        twoImports.projectId == project1.projectId
        twoImports.skillId == skill.skillId
        twoImports.users.size() == 2
        twoImports.users.find { it.importingProjectId == project2.projectId && it.importedIntoSubjectId == p2subj1.subjectId }
        twoImports.users.find { it.importingProjectId == project3.projectId && it.importedIntoSubjectId == p3subj1.subjectId }
    }

    def "get all catalog skills available to project"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        p1subj1.name = "p1 test subject #1"
        def p1subj2 = createSubject(1, 2)
        def p2subj1 = createSubject(2, 2)
        def p2subj2 = createSubject(2, 3)
        def p4subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 2, 0, 20)
        def skill3 = createSkill(1, 2, 3, 0, 3, 0, 30)
        def skill4 = createSkill(1, 2, 4)
        def skill5 = createSkill(1, 1, 5)
        def skill6 = createSkill(1, 1, 6)

        def skill7 = createSkill(3, 1, 7)

        def skill9 = createSkill(2, 2, 9)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p2subj2)
        skillsService.createSubject(p4subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createSkill(skill7)
        skillsService.createSkill(skill9)

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project1.projectId, skill4.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project1.projectId, skill5.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project3.projectId, skill7.skillId)
        Thread.sleep(50)
        skillsService.exportSkillToCatalog(project2.projectId, skill9.skillId)

        //numPerformToCompletion is a synthetic field, it can't be sorted on
        def availableCatalogSkills = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name")
        def availableCatalogP1 = skillsService.getCatalogSkills(project2.projectId, 2, 1, "exportedOn")
        def availableCatalogP2 = skillsService.getCatalogSkills(project2.projectId, 2, 2, "exportedOn")
        def availableCatalogP3 = skillsService.getCatalogSkills(project2.projectId, 2, 3, "exportedOn")

        def sortedByPointIncrementAsc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "pointIncrement", true)
        def sortedByPointIncrementDesc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "pointIncrement", false)
        def sortedBySubjectNameAsc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "subjectName", true)
        def sortedBySubjectNameDesc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "subjectName", false)
        def sortedByProjectNameAsc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "projectName", true)
        def sortedByProjectNameDesc = skillsService.getCatalogSkills(project2.projectId, 10, 1, "projectName", false)

        // import skill from project1, catalog should have 5 total skills remaining available after this
        //4: project1, 1: project3
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        def postImportAvailableCatalogSkills = skillsService.getCatalogSkills(project2.projectId, 10, 1, "exportedOn")

        //search on project1.name, should result in 4 skills
        def searchOnProjectName = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, project1.name)

        def searchWithPagingP1 = skillsService.getCatalogSkills(project2.projectId, 3, 1, "exportedOn", true, project1.name)
        def searchWithPagingP2 = skillsService.getCatalogSkills(project2.projectId, 3, 2, "exportedOn", true, project1.name)

        def searchOnProjectNameAndSubjectName = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, project1.name, p1subj2.name)
        def searchOnProjectNameAndSubjectNameAndSkillName = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, project1.name, p1subj2.name, skill3.name)

        def searchOnSubjectName = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, "", p1subj1.name)

        def searchOnSkillName = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, "", "", skill5.name)
        def searchOnSkillNameBroad = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name", true, "", "", "1")

        then:
        //project2 skill should be excluded as it is the project id specified in the request
        availableCatalogSkills.totalCount == 6
        availableCatalogSkills.data.size() == 6
        //description, helpUrl, projectName
        availableCatalogSkills.data.find { it.projectId == project1.projectId && it.skillId == skill.skillId && it.subjectName == p1subj1.name && it.description && it.helpUrl && it.projectName == project1.name}
        availableCatalogSkills.count == 6
        availableCatalogSkills.data.size() == 6
        availableCatalogSkills.data[0].name == skill.name
        availableCatalogSkills.data[5].name == skill7.name

        availableCatalogP1.totalCount == 6
        availableCatalogP1.count == 2
        availableCatalogP1.data.size() == 2
        availableCatalogP1.data[0].skillId == skill.skillId
        availableCatalogP1.data[0].projectId == project1.projectId
        availableCatalogP1.data[1].skillId == skill2.skillId
        availableCatalogP1.data[1].projectId == project1.projectId

        availableCatalogP2.totalCount == 6
        availableCatalogP2.count == 2
        availableCatalogP2.data.size() == 2
        availableCatalogP2.data[0].skillId == skill3.skillId
        availableCatalogP2.data[0].projectId == project1.projectId
        availableCatalogP2.data[1].skillId == skill4.skillId
        availableCatalogP2.data[1].projectId == project1.projectId

        availableCatalogP3.totalCount == 6
        availableCatalogP3.count == 2
        availableCatalogP3.data.size() == 2
        availableCatalogP3.data[0].skillId == skill5.skillId
        availableCatalogP3.data[0].projectId == project1.projectId
        availableCatalogP3.data[1].skillId == skill7.skillId
        availableCatalogP3.data[1].projectId == project3.projectId

        sortedByPointIncrementAsc.data[5].skillId == skill3.skillId
        sortedByPointIncrementAsc.data[5].pointIncrement == 30
        sortedByPointIncrementDesc.data[0].skillId == skill3.skillId
        sortedByPointIncrementDesc.data[0].pointIncrement == 30
        sortedByProjectNameAsc.data[0].projectName == project1.name
        sortedByProjectNameAsc.data[5].projectName == project3.name
        sortedByProjectNameDesc.data[0].projectName == project3.name
        sortedByProjectNameDesc.data[5].projectName == project1.name
        postImportAvailableCatalogSkills.totalCount == 5
        postImportAvailableCatalogSkills.count == 5
        !postImportAvailableCatalogSkills.data.find { it.projectId == project1.projectId && it.skillId == skill.skillId && it.subjectName == p1subj1.name && it.description && it.helpUrl && it.projectName == project1.name}
        searchOnProjectName.count == 4
        searchOnProjectName.totalCount == 4
        searchOnProjectName.data.findAll { it.projectId == project1.projectId }.size() == 4
        searchWithPagingP1.count == 3
        searchWithPagingP1.totalCount == 4
        searchWithPagingP1.data.size() == 3 //2,3,4,5
        searchWithPagingP1.data[0].skillId == skill2.skillId
        searchWithPagingP1.data[0].projectId == project1.projectId
        searchWithPagingP1.data[1].skillId == skill3.skillId
        searchWithPagingP1.data[1].projectId == project1.projectId
        searchWithPagingP1.data[2].skillId == skill4.skillId
        searchWithPagingP1.data[2].projectId == project1.projectId
        searchWithPagingP2.count == 1
        searchWithPagingP2.totalCount == 4
        searchWithPagingP2.data.size() == 1
        searchWithPagingP2.data[0].skillId == skill5.skillId
        searchWithPagingP2.data[0].projectId == project1.projectId
        searchOnProjectNameAndSubjectName.count == 2
        searchOnProjectNameAndSubjectName.totalCount == 2
        searchOnProjectNameAndSubjectName.data.size() == 2
        searchOnProjectNameAndSubjectName.data.findAll { it.projectName == project1.name && it.subjectName == p1subj2.name }.size() == 2
        searchOnProjectNameAndSubjectNameAndSkillName.count == 1
        searchOnProjectNameAndSubjectNameAndSkillName.totalCount == 1
        searchOnProjectNameAndSubjectNameAndSkillName.data.size() == 1
        searchOnProjectNameAndSubjectNameAndSkillName.data[0].skillId == skill3.skillId
        searchOnSubjectName.count == 2
        searchOnSubjectName.totalCount == 2
        searchOnSubjectName.data.size() == 2
        searchOnSubjectName.data[0].skillId == skill2.skillId
        searchOnSubjectName.data[1].skillId == skill5.skillId
        searchOnSkillName.count == 1
        searchOnSkillName.totalCount == 1
        searchOnSkillName.data.size() == 1
        searchOnSkillName.data[0].skillId == skill5.skillId
    }

    def "get skill details should populate imported from catalog attributes"() {
        //readOnly, copiedFromProjectId, copiedFromProjectName, copiedFromSubjectName??
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
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def importedSkillDetails = skillsService.getSkill(["projectId": project2.projectId, "subjectId": p2subj1.subjectId, "skillId": skill.skillId])

        then:
        importedSkillDetails.readOnly == true
        importedSkillDetails.copiedFromProjectId == project1.projectId
        importedSkillDetails.copiedFromProjectName == project1.name
    }

    def "skills exported to the catalog should have sharedToCatalog populated"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def skill4 = createSkill(1, 1, 4)
        def skill5 = createSkill(1, 1, 5)
        def skill6 = createSkill(1, 1, 6)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        when:
        def skillDetails = skillsService.getSkill(skill)
        def subjectSkills = skillsService.getSkillsForSubject(project1.projectId, p1subj1.subjectId)
        def projectSkills = skillsService.getSkillsForProject(project1.projectId)

        then:
        skillDetails.sharedToCatalog == true
        subjectSkills.findAll { it.sharedToCatalog == true }.size() == 2
        // not populated by all project skills endpoint
        projectSkills.findAll { it.sharedToCatalog == true }.size() == 0
    }

    def "export a skill with dependencies"() {
        // create skills, add dependencies, try to export top of chain
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        skillsService.assignDependency([projectId: project1.projectId, skillId: skill2.skillId, dependentSkillId: skill.skillId])

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("Skill [skill2] has dependencies. Skills with dependencies may not be exported to the catalog, errorCode:ExportToCatalogNotAllowed")
    }

    def "export skill that other skills depend on"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        skillsService.assignDependency([projectId: project1.projectId, skillId: skill2.skillId, dependentSkillId: skill.skillId])

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        def exportedSkills = skillsService.getExportedSkills(project1.projectId, 10, 1, "exportedOn", true)

        then:
        exportedSkills.totalCount == 1
        exportedSkills.count == 1
        exportedSkills.data.size() == 1
        exportedSkills.data[0].skillId == skill.skillId
    }

    def "delete a skill exported to the catalog that has been imported and added as a dependency to other skills"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)
        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 10)

        def p2skill1 = createSkill(2, 1, 11)
        def p2skill2 = createSkill(2, 1, 12)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p2skill2)

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.assignDependency([projectId: project1.projectId, skillId: skill2.skillId, dependentSkillId: skill.skillId])

        skillsService.deleteSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: skill.skillId])

        def importedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])
        then:
        def e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.NOT_FOUND
    }

}


