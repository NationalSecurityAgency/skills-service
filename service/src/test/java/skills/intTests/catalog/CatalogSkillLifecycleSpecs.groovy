/**
 * Copyright 2026 SkillTree
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

import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.*

class CatalogSkillLifecycleSpecs extends CatalogIntSpec {

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
        def res = skillsService.getCatalogSkills(project2.projectId, 5, 1, "name")

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


    def "catalog skill id already exist locally"() {
        def project1 = createProject(1)
        def subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skillA = createSkill(1, 1, 2, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(project1, subj1, [skill, skillA])

        def project2 = createProject(2)
        def subj2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1, 1, 0, 1, 0, 250)
        skill2.skillId = skill.skillId.toUpperCase()
        skillsService.createProjectAndSubjectAndSkills(project2, subj2, [skill2])

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skillA.skillId)
        def res = skillsService.getCatalogSkills(project2.projectId, 5, 1).data.sort { it.skillId }
        def resWithSearch = skillsService.getCatalogSkills(project2.projectId, 5, 1, "exportedOn", true, project1.name).data.sort { it.skillId }

        then:
        res.skillId == [skill.skillId, skillA.skillId]
        res.skillIdAlreadyExist == [true, false]
        res.skillNameAlreadyExist == [true, false]

        resWithSearch.skillId == [skill.skillId, skillA.skillId]
        resWithSearch.skillIdAlreadyExist == [true, false]
        resWithSearch.skillNameAlreadyExist == [true, false]
    }


    def "catalog skill name already exist locally"() {
        def project1 = createProject(1)
        def subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 10, 0, 1, 0, 250)
        def skillA = createSkill(1, 1, 11, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(project1, subj1, [skill, skillA])

        def project2 = createProject(2)
        def subj2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1, 1, 0, 1, 0, 250)
        skill2.name = skill.name.toUpperCase()
        skillsService.createProjectAndSubjectAndSkills(project2, subj2, [skill2])

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skillA.skillId)
        def res = skillsService.getCatalogSkills(project2.projectId, 5, 1).data.sort { it.skillId }
        def resWithSearch = skillsService.getCatalogSkills(project2.projectId, 5, 1, "exportedOn", true, project1.name).data.sort { it.skillId }

        then:
        res.skillId == [skill.skillId, skillA.skillId]
        res.skillIdAlreadyExist == [false, false]
        res.skillNameAlreadyExist == [true, false]

        resWithSearch.skillId == [skill.skillId, skillA.skillId]
        resWithSearch.skillIdAlreadyExist == [false, false]
        resWithSearch.skillNameAlreadyExist == [true, false]
    }


    def "catalog skill name and id already exist locally"() {
        def project1 = createProject(1)
        def subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 10, 0, 1, 0, 250)
        def skillA = createSkill(1, 1, 11, 0, 1, 0, 250)
        def skillB = createSkill(1, 1, 12, 0, 1, 0, 250)
        def skillC = createSkill(1, 1, 13, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(project1, subj1, [skill, skillA, skillB, skillC])

        def project2 = createProject(2)
        def subj2 = createSubject(2, 1)
        def skill2 = createSkill(2, 1, 1, 0, 1, 0, 250)
        def skill3 = createSkill(2, 1, 2, 0, 1, 0, 250)
        def skill4 = createSkill(2, 1, 3, 0, 1, 0, 250)
        skill2.name = skill.name.toUpperCase()
        skill3.skillId = skillA.skillId.toUpperCase()
        skill4.skillId = skillB.skillId.toUpperCase()
        skill4.name = skillB.name.toUpperCase()
        skillsService.createProjectAndSubjectAndSkills(project2, subj2, [skill2, skill3, skill4])

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skillA.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skillB.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skillC.skillId)
        def res = skillsService.getCatalogSkills(project2.projectId, 5, 1).data.sort { it.skillId }
        def resWithSearch = skillsService.getCatalogSkills(project2.projectId, 5, 1, "exportedOn", true, project1.name).data.sort { it.skillId }

        then:
        res.skillId == [skill.skillId, skillA.skillId, skillB.skillId, skillC.skillId]
        res.skillIdAlreadyExist == [false, true, true, false]
        res.skillNameAlreadyExist == [true, false, true, false]

        resWithSearch.skillId == [skill.skillId, skillA.skillId, skillB.skillId, skillC.skillId]
        res.skillIdAlreadyExist == [false, true, true, false]
        resWithSearch.skillNameAlreadyExist == [true, false, true, false]
    }


    def "catalog skill name and id already exist locally within group skills"() {
        def project1 = createProject(1)
        def subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 10, 0, 1, 0, 250)
        def skillA = createSkill(1, 1, 11, 0, 1, 0, 250)
        def skillB = createSkill(1, 1, 12, 0, 1, 0, 250)
        def skillC = createSkill(1, 1, 13, 0, 1, 0, 250)
        skillsService.createProjectAndSubjectAndSkills(project1, subj1, [skill, skillA, skillB, skillC])

        def project2 = createProject(2)
        def subj2 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 5)
        skillsService.createProjectAndSubjectAndSkills(project2, subj2, [p2skillsGroup])
        def skill2 = createSkill(2, 1, 1, 0, 1, 0, 250)
        def skill3 = createSkill(2, 1, 2, 0, 1, 0, 250)
        def skill4 = createSkill(2, 1, 3, 0, 1, 0, 250)
        skill2.name = skill.name.toUpperCase()
        skill3.skillId = skillA.skillId.toUpperCase()
        skill4.skillId = skillB.skillId.toUpperCase()
        skill4.name = skillB.name.toUpperCase()
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, skill2)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, skill3)
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, skill4)

        when:
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skillA.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skillB.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skillC.skillId)
        def res = skillsService.getCatalogSkills(project2.projectId, 5, 1).data.sort { it.skillId }
        def resWithSearch = skillsService.getCatalogSkills(project2.projectId, 5, 1, "exportedOn", true, project1.name).data.sort { it.skillId }

        then:
        res.skillId == [skill.skillId, skillA.skillId, skillB.skillId, skillC.skillId]
        res.skillIdAlreadyExist == [false, true, true, false]
        res.skillNameAlreadyExist == [true, false, true, false]

        resWithSearch.skillId == [skill.skillId, skillA.skillId, skillB.skillId, skillC.skillId]
        res.skillIdAlreadyExist == [false, true, true, false]
        resWithSearch.skillNameAlreadyExist == [true, false, true, false]
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
        def res = skillsService.getCatalogSkills(project2.projectId, 5, 1, "name")

        then:
        res
        res.totalCount == 3
        res.data[0].skillId == skill.skillId
        res.data[1].skillId == skill3.skillId
        res.data[2].skillId == skill2.skillId
    }


    def "bulk export skills to catalog - 1 skill id does not exist"() {
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
        skillsService.bulkExportSkillsToCatalog(project1.projectId, [skill.skillId, "haaaaaa", skill2.skillId, skill3.skillId])

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Skill [haaaaaa] doesn't exist")
    }


    def "bulk export skills to catalog - do not allow to export groups"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def subj1 = createSubject(1, 1)
        def subj2 = createSubject(1, 2)
        /* int projNumber = 1, int subjNumber = 1, int skillNumber = 1, int version = 0, int numPerformToCompletion = 1, pointIncrementInterval = 480, pointIncrement = 10, type="Skill" */
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 250)
        def skill3 = createSkill(1, 2, 1, 0, 1, 0, 250)
        def skillsGroup = SkillsFactory.createSkillsGroup(1, 1, 4)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skillsGroup)

        when:
        skillsService.bulkExportSkillsToCatalog(project1.projectId, [skill.skillId, skillsGroup.skillId, skill2.skillId, skill3.skillId])

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("explanation:Only type=[Skill] is supported but provided type=[SkillsGroup] for skillId=[skill4]")
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
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

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
        skillsService.bulkImportSkillsFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, [
                [projectId:project1.projectId, skillId: skill.skillId],
                [projectId: project1.projectId, skillId: skill2.skillId]
        ])

        def skills = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)

        then:
        skills.find { it.skillId == skill.skillId }
        skills.find { it.skillId == skill2.skillId }
        !skills.find { it.skillId == skill3.skillId }
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
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

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
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

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
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.deleteSkill([projectId: project3.projectId, subjectId: p3subj1.subjectId, skillId: skill.skillId])

        def originalSkill = skillsService.getSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: skill.skillId])
        def p2Copy = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])

        then:
        originalSkill
        originalSkill.skillId == skill.skillId
        p2Copy
        p2Copy.skillId == skill.skillId
    }


    def "cannot export a disabled skill"() {
        // create skills, add dependencies, try to export top of chain
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        skill2.enabled = false
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("Skill [skill2] is disabled. Disabled skills may not be exported to the catalog")
        e.getMessage().contains("errorCode:ExportToCatalogNotAllowed")
    }


    def "cannot import skill from catalog with same name as skill already existing in destination project"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)

        def p2skill1 = createSkill(2, 1, 1, 0, 1, 0, 100)
        p2skill1.skillId = "foo"
        p2skill1.name = skill.name

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)

        skillsService.exportSkillToCatalog(skill.projectId, skill.skillId)

        when:
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        then:
        def e = thrown(SkillsClientException)
        e.getMessage().contains("Cannot import Skill from catalog, [${p2skill1.name}] already exists in Project")
    }


    def "cannot export skill to catalog if there is already a skill with the same id in the catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)

        def p2skill1 = createSkill(2, 1, 1, 0, 1, 0, 100)
        p2skill1.skillId = skill.skillId

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)

        skillsService.exportSkillToCatalog(skill.projectId, skill.skillId)

        when:
        skillsService.exportSkillToCatalog(p2skill1.projectId, p2skill1.skillId)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("Skill id [${p2skill1.skillId}] already exists in the catalog. Duplicated skill ids are not allowed")
    }


    def "cannot export skill to catalog if there is already a skill with the same name in the catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)

        def p2skill1 = createSkill(2, 1, 1, 0, 1, 0, 100)
        p2skill1.skillId = "rando"
        p2skill1.name = skill.name

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)

        skillsService.exportSkillToCatalog(skill.projectId, skill.skillId)

        when:
        skillsService.exportSkillToCatalog(p2skill1.projectId, p2skill1.skillId)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("Skill name [${p2skill1.name}] already exists in the catalog. Duplicate skill names are not allowed")
    }


    def "check if skill is already exported to the catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def p2skill1 = createSkill(2, 1, 1, 0, 1, 0, 100)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)

        skillsService.exportSkillToCatalog(skill.projectId, skill.skillId)

        when:
        def res1 = skillsService.doesSkillExistInCatalog(skill.projectId, skill.skillId)
        def res2 = skillsService.doesSkillExistInCatalog(skill2.projectId, skill2.skillId)

        then:
        res1.skillAlreadyInCatalog
        !res1.skillIdConflictsWithExistingCatalogSkill
        !res1.skillNameConflictsWithExistingCatalogSkill

        !res2.skillAlreadyInCatalog
        !res2.skillIdConflictsWithExistingCatalogSkill
        !res2.skillNameConflictsWithExistingCatalogSkill
    }


    def "check catalog status of multiple skillids"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)

        def skill4 = createSkill(2, 1, 4, 0, 1, 0, 100)
        def skill5 = createSkill(2, 1, 5, 0, 1, 0, 100)
        def skill6 = createSkill(2, 1, 6, 0, 1, 0, 100)
        def skill7 = createSkill(2, 1, 7, 0, 1, 0, 100)
        def skill8 = createSkill(2, 1, 8, 0, 1, 0, 100)

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
        skillsService.createSkill(skill7)
        skillsService.createSkill(skill8)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        when:

        def result = skillsService.doSkillsExistInCatalog(project1.projectId, [skill.skillId, skill2.skillId, skill3.skillId, skill4.skillId, skill5.skillId])

        then:
        result
        result[skill.skillId] == true
        result[skill2.skillId] == true
        result[skill3.skillId] == false
        result[skill4.skillId] == false
        result[skill5.skillId] == false
    }


    def "validate exportability of multiple skillIds"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)

        def skill4 = createSkill(2, 1, 4, 0, 1, 0, 100)
        def skill5 = createSkill(2, 1, 5, 0, 1, 0, 100)
        def skill6 = createSkill(2, 1, 6, 0, 1, 0, 100)
        def skill7 = createSkill(2, 1, 7, 0, 1, 0, 100)
        def skill8 = createSkill(2, 1, 8, 0, 1, 0, 100)

        def p3skill1 = createSkill(3, 1, 1, 0, 1, 0, 100)
        p3skill1.skillId = skill.skillId
        p3skill1.name = 'p3skill1 name'
        def p3skill2 = createSkill(3, 1, 2, 0, 1, 0, 100)
        p3skill2.skillId = 'p3skill2_skillId'
        p3skill2.name = skill2.name
        def p3skill3 = createSkill(3, 1, 3, 0, 1, 0, 100)
        p3skill3.skillId = "p3skill3_skillId"
        p3skill3.name = "p3skill3 name"
        def p3skill4 = createSkill(3, 1, 4, 0, 1, 0, 100)
        p3skill4.skillId = "p3skill4_skillId"
        p3skill4.name = "p3skill4 name"
        def p3skill5 = createSkill(3, 1, 5, 0, 1, 0, 100)
        p3skill5.skillId = "p3skill5_skillId"
        p3skill5.name = "p3skill5 name"
        def p3skill6 = createSkill(3, 1, 6, 0, 1, 0, 100)
        p3skill6.skillId = "p3skill6_skillId"
        p3skill6.name = "p3skill6 name"
        def p3skill7 = createSkill(3, 1, 7, 0, 1, 0, 100)
        p3skill7.skillId = "p3skill7_skillId"
        p3skill7.name = "p3skill7 name"

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createSkill(skill7)
        skillsService.createSkill(skill8)

        skillsService.createSkill(p3skill1)
        skillsService.createSkill(p3skill2)
        skillsService.createSkill(p3skill3)
        skillsService.createSkill(p3skill4)
        skillsService.createSkill(p3skill5)
        skillsService.createSkill(p3skill6)
        skillsService.createSkill(p3skill7)

        skillsService.addLearningPathPrerequisite(p3skill6.projectId, p3skill6.skillId, p3skill5.skillId)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project2.projectId, skill5.skillId)
        skillsService.exportSkillToCatalog(project2.projectId, skill6.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, p3skill7.skillId)


        when:
        def validationResult = skillsService.areSkillIdsExportable(project3.projectId, [p3skill1.skillId, p3skill2.skillId, p3skill3.skillId, p3skill4.skillId, p3skill5.skillId, p3skill6.skillId, p3skill7.skillId])

        then:
        validationResult.skillsValidationRes[p3skill1.skillId].skillId == p3skill1.skillId
        validationResult.skillsValidationRes[p3skill1.skillId].skillAlreadyInCatalog == false
        validationResult.skillsValidationRes[p3skill1.skillId].skillIdConflictsWithExistingCatalogSkill == true
        validationResult.skillsValidationRes[p3skill1.skillId].skillNameConflictsWithExistingCatalogSkill == false
        validationResult.skillsValidationRes[p3skill1.skillId].hasDependencies == false

        validationResult.skillsValidationRes[p3skill2.skillId].skillId == p3skill2.skillId
        validationResult.skillsValidationRes[p3skill2.skillId].skillAlreadyInCatalog == false
        validationResult.skillsValidationRes[p3skill2.skillId].skillIdConflictsWithExistingCatalogSkill == false
        validationResult.skillsValidationRes[p3skill2.skillId].skillNameConflictsWithExistingCatalogSkill == true
        validationResult.skillsValidationRes[p3skill2.skillId].hasDependencies == false

        validationResult.skillsValidationRes[p3skill3.skillId].skillId == p3skill3.skillId
        validationResult.skillsValidationRes[p3skill3.skillId].skillAlreadyInCatalog == false
        validationResult.skillsValidationRes[p3skill3.skillId].skillIdConflictsWithExistingCatalogSkill == false
        validationResult.skillsValidationRes[p3skill3.skillId].skillNameConflictsWithExistingCatalogSkill == false
        validationResult.skillsValidationRes[p3skill3.skillId].hasDependencies == false

        validationResult.skillsValidationRes[p3skill4.skillId].skillId == p3skill4.skillId
        validationResult.skillsValidationRes[p3skill4.skillId].skillAlreadyInCatalog == false
        validationResult.skillsValidationRes[p3skill4.skillId].skillIdConflictsWithExistingCatalogSkill == false
        validationResult.skillsValidationRes[p3skill4.skillId].skillNameConflictsWithExistingCatalogSkill == false
        validationResult.skillsValidationRes[p3skill4.skillId].hasDependencies == false

        validationResult.skillsValidationRes[p3skill5.skillId].skillId == p3skill5.skillId
        validationResult.skillsValidationRes[p3skill5.skillId].skillAlreadyInCatalog == false
        validationResult.skillsValidationRes[p3skill5.skillId].skillIdConflictsWithExistingCatalogSkill == false
        validationResult.skillsValidationRes[p3skill5.skillId].skillNameConflictsWithExistingCatalogSkill == false
        validationResult.skillsValidationRes[p3skill5.skillId].hasDependencies == false

        validationResult.skillsValidationRes[p3skill6.skillId].skillId == p3skill6.skillId
        validationResult.skillsValidationRes[p3skill6.skillId].skillAlreadyInCatalog == false
        validationResult.skillsValidationRes[p3skill6.skillId].skillIdConflictsWithExistingCatalogSkill == false
        validationResult.skillsValidationRes[p3skill6.skillId].skillNameConflictsWithExistingCatalogSkill == false
        validationResult.skillsValidationRes[p3skill6.skillId].hasDependencies == true

        validationResult.skillsValidationRes[p3skill7.skillId].skillId == p3skill7.skillId
        validationResult.skillsValidationRes[p3skill7.skillId].skillAlreadyInCatalog == true
        validationResult.skillsValidationRes[p3skill7.skillId].skillIdConflictsWithExistingCatalogSkill == true
        validationResult.skillsValidationRes[p3skill7.skillId].skillNameConflictsWithExistingCatalogSkill == true
        validationResult.skillsValidationRes[p3skill7.skillId].hasDependencies == false
    }


    def "finalization of imports on project with insufficient points should fail"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 5)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 5)
        def skill3 = createSkill(1, 1, 30, 0, 1, 0, 500)

        def p2skill1 = createSkill(2, 1, 3, 0, 1, 0, 10)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(p2skill1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)

        when:
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        def e = thrown(Exception)
        e.message.contains("errorCode:InsufficientProjectFinalizationPoints")


    }


    def "finalization of imports where subjects with imported skills have insufficient points should fail"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p2subj2 = createSubject(2, 2)
        def p2subj3 = createSubject(2, 3)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 5)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 5)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 5)
        def skill4 = createSkill(1, 1, 40, 0, 1, 0, 500)

        def p2skill1 = createSkill(2, 2, 3, 0, 1, 0, 200)
        def p2skill2 = createSkill(2, 1, 33, 0, 1, 0, 90)
        def p2skill3 = createSkill(2, 3, 11, 0, 1, 0, 5)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p2subj2)
        skillsService.createSubject(p2subj3)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p2skill2)
        skillsService.createSkill(p2skill3)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)


        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj3.subjectId, project1.projectId, skill3.skillId)

        when:
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        def e = thrown(Exception)
        e.message.contains("errorCode:InsufficientSubjectFinalizationPoints")
    }

}
