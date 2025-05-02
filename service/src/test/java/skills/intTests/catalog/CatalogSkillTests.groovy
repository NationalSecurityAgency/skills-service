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
package skills.intTests.catalog

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.controller.result.model.LevelDefinitionRes
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.services.LevelDefinitionStorageService
import skills.services.UserEventService
import skills.storage.model.SkillApproval
import skills.storage.model.SkillDef
import skills.storage.repos.SkillApprovalRepo
import skills.storage.repos.SkillDefRepo

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class CatalogSkillTests extends CatalogIntSpec {

    @Autowired
    UserEventService userEventService

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").withZoneUTC()

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

    def "update skill that has been exported to catalog"() {
        //changes should be reflected across all copies
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

        when:
        def preEdit = skillsService.getCatalogSkills(project2.projectId, 10, 1, "name")
        def skillNamePreEdit = skill.name
        def skillDescriptionPreEdit = skill.description
        def skillHelpUrlPreEdit = skill.helpUrl

        skill.name = "edited name"
        skill.numPerformToCompletion = 50
        skill.helpUrl = "http://newHelpUrl"
        skill.description = "updated description"
        skill.selfReportingType = SkillDef.SelfReportingType.Approval.toString()

        skillsService.updateSkill(skill, skill.skillId)
        def postEdit = skillsService.getCatalogSkills(project3.projectId, 10, 1, "name")

        then:
        preEdit.data[0].name == skillNamePreEdit
        preEdit.data[0].totalPoints == 250
        preEdit.data[0].numPerformToCompletion == 1
        preEdit.data[0].description == skillDescriptionPreEdit
        preEdit.data[0].helpUrl == skillHelpUrlPreEdit
        !preEdit.data[0].selfReportingType
        postEdit.data[0].name == skill.name
        postEdit.data[0].totalPoints == 12500
        postEdit.data[0].numPerformToCompletion == 50
        postEdit.data[0].description == skill.description
        postEdit.data[0].helpUrl == skill.helpUrl
        postEdit.data[0].selfReportingType == SkillDef.SelfReportingType.Approval.toString()
    }

    def "update skill that has been exported to catalog, edits should be reflected on imported copies"() {
        //changes should be reflected across all copies
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
        def preEdit = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])
        def skillNamePreEdit = skill.name
        def skillDescriptionPreEdit = skill.description
        def skillHelpUrlPreEdit = skill.helpUrl

        skill.name = "edited name"
        skill.numPerformToCompletion = 50
        skill.helpUrl = "http://newHelpUrl"
        skill.description = "updated description"
        skill.selfReportingType = SkillDef.SelfReportingType.Approval.toString()

        skillsService.updateSkill(skill, skill.skillId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def postEdit = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])

        then:
        preEdit.name == skillNamePreEdit
        preEdit.totalPoints == 250
        preEdit.numPerformToCompletion == 1
        preEdit.description == skillDescriptionPreEdit
        preEdit.helpUrl == skillHelpUrlPreEdit
        !preEdit.selfReportingType
        postEdit.name == skill.name
        postEdit.totalPoints == 12500
        postEdit.numPerformToCompletion == 50
        postEdit.description == skill.description
        postEdit.helpUrl == skill.helpUrl
        postEdit.selfReportingType == SkillDef.SelfReportingType.Approval.toString()
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
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

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

    def "description, helpUrl, selfReportingType fields present on skill imported from catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 250)
        skill.name = "foo name"
        skill.numPerformToCompletion = 50
        skill.helpUrl = "http://newHelpUrl"
        skill.description = "updated description"
        skill.selfReportingType = SkillDef.SelfReportingType.Approval.toString()

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def res = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def importedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])

        then:
        importedSkill.name == skill.name
        importedSkill.numPerformToCompletion == skill.numPerformToCompletion
        importedSkill.helpUrl == skill.helpUrl
        importedSkill.description == skill.description
        importedSkill.selfReportingType == skill.selfReportingType
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

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        skillsService.addSkill([projectId: project3.projectId, skillId: skill.skillId], user)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("explanation:Skills imported from the catalog can only be reported if the original skill is configured for Self Reporting")
    }

    def "report skill event on original exported skill when original project has insufficient points"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)

        def p2native = createSkill(2, 1, 3, 0, 1, 0, 100)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(p2native)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        skill.pointIncrement = 10
        skill2.pointIncrement = 10
        skill3.pointIncrement = 10
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

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

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)
        assert res.body.explanation == "Skill was submitted for approval"

        def p1StatsPre = skillsService.getUserStats(project1.projectId, user)
        def p2StatsPre = skillsService.getUserStats(project2.projectId, user)

        def p1Approvals = skillsService.getApprovals(project1.projectId, 7, 1, 'requestedOn', false)
        def p2Approvals = skillsService.getApprovals(project2.projectId, 7, 1, 'requestedOn', false)

        skillsService.approve(project1.projectId, [p1Approvals.data[0].id])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
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

    def "report self-report approval request on skill imported from catalog - skill already has pending approval request and then approved"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 2, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 2, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 2, 0, 10)

        skill.pointIncrement = 200
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        skill3.selfReportingType = SkillDef.SelfReportingType.Approval

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

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)

        def user = getRandomUsers(1)[0]
        when:
        def res1 = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        List<SkillApproval> approvals1 = skillApprovalRepo.findAll()

        def res2 = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)

        List<SkillApproval> approvals2 = skillApprovalRepo.findAll()
        skillsService.approve(project1.projectId, approvals2.collect { it.id })

        def res3 = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        println JsonOutput.prettyPrint(JsonOutput.toJson(res3))

        List<SkillApproval> approvals3 = skillApprovalRepo.findAll()
        skillsService.approve(project1.projectId, approvals3.find { it.approverActionTakenOn == null }.collect { it.id })

        def res4 = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        List<SkillApproval> approvals4 = skillApprovalRepo.findAll()
        then:
        !res1.body.skillApplied
        res1.body.explanation == "Skill was submitted for approval"
        res1.body.pointsEarned == 0
        approvals1.size() == 1
        approvals1[0].projectId == project1.projectId
        skillDefRepo.findById(approvals1[0].skillRefId).get().skillId == skill.skillId
        !approvals1[0].approverActionTakenOn

        !res2.body.skillApplied
        res2.body.pointsEarned == 0
        res2.body.explanation == "This skill was already submitted for approval and is still pending approval"
        approvals2.size() == 1
        approvals2[0].projectId == project1.projectId
        skillDefRepo.findById(approvals2[0].skillRefId).get().skillId == skill.skillId
        !approvals2[0].approverActionTakenOn

        !res3.body.skillApplied
        res3.body.explanation == "Skill was submitted for approval"
        res3.body.pointsEarned == 0
        approvals3.size() == 2
        def approved1 = approvals3.find { it.approverActionTakenOn != null }
        approved1.projectId == project1.projectId
        skillDefRepo.findById(approved1.skillRefId).get().skillId == skill.skillId

        def pending = approvals3.find { it.approverActionTakenOn == null }
        pending.projectId == project1.projectId
        skillDefRepo.findById(pending.skillRefId).get().skillId == skill.skillId

        !res4.body.skillApplied
        res4.body.pointsEarned == 0
        res4.body.explanation == "This skill reached its maximum points"
        approvals4.size() == 2
        approvals4.each { assert it.getApproverActionTakenOn() != null }
    }

    def "skills display summary endpoints must return self-report approval status for imported skills"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def proj2_skill4 = createSkill(2, 1, 4)
        def proj2_skill5 = createSkill(2, 1, 5)

        skill.pointIncrement = 200
        skill.numPerformToCompletion = 1
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        skill3.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill4.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill5.selfReportingType = SkillDef.SelfReportingType.Approval

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

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.createSkill(proj2_skill4)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)
        skillsService.createSkill(proj2_skill5)

        def user = getRandomUsers(1)[0]
        println skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        println skillsService.addSkill([projectId: project2.projectId, skillId: proj2_skill4.skillId], user)
        println skillsService.addSkill([projectId: project2.projectId, skillId: skill3.skillId], user)
        when:
        def skillsSummary = skillsService.getSkillSummary(user, project2.projectId, p2subj1.subjectId, -1, true).skills
        println JsonOutput.prettyPrint(JsonOutput.toJson(skillsSummary))
        then:
        skillsSummary.size() == 5
        def self1 = skillsSummary.find { it.skillId == skill.skillId }.selfReporting
        self1.enabled
        self1.type == "Approval"
        self1.requestedOn

        def self2 = skillsSummary.find { it.skillId == skill2.skillId }.selfReporting
        self2.enabled
        self2.type == "Approval"
        !self2.requestedOn

        def self3 = skillsSummary.find { it.skillId == skill3.skillId }.selfReporting
        self3.enabled
        self3.type == "Approval"
        self3.requestedOn

        def self4 = skillsSummary.find { it.skillId == proj2_skill4.skillId }.selfReporting
        self4.enabled
        self4.type == "Approval"
        self4.requestedOn

        def self5 = skillsSummary.find { it.skillId == proj2_skill5.skillId }.selfReporting
        self5.enabled
        self5.type == "Approval"
        !self5.requestedOn
    }

    def "single skill endpoint must return self-report approval status for imported skills"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 10)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 10)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 10)

        def proj2_skill4 = createSkill(2, 1, 4)
        def proj2_skill5 = createSkill(2, 1, 5)

        skill.pointIncrement = 200
        skill.numPerformToCompletion = 1
        skill.selfReportingType = SkillDef.SelfReportingType.Approval
        skill2.selfReportingType = SkillDef.SelfReportingType.Approval
        skill3.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill4.selfReportingType = SkillDef.SelfReportingType.Approval
        proj2_skill5.selfReportingType = SkillDef.SelfReportingType.Approval

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

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.createSkill(proj2_skill4)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)
        skillsService.createSkill(proj2_skill5)

        def user = getRandomUsers(1)[0]
        skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user)
        skillsService.addSkill([projectId: project2.projectId, skillId: proj2_skill4.skillId], user)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill3.skillId], user)
        when:
        def self1 = skillsService.getSingleSkillSummary(user, project2.projectId, skill.skillId).selfReporting
        def self2 = skillsService.getSingleSkillSummary(user, project2.projectId, skill2.skillId).selfReporting
        def self3 = skillsService.getSingleSkillSummary(user, project2.projectId, skill3.skillId).selfReporting
        def self4 = skillsService.getSingleSkillSummary(user, project2.projectId, proj2_skill4.skillId).selfReporting
        def self5 = skillsService.getSingleSkillSummary(user, project2.projectId, proj2_skill5.skillId).selfReporting
        then:
        self1.enabled
        self1.type == "Approval"
        self1.requestedOn

        self2.enabled
        self2.type == "Approval"
        !self2.requestedOn

        self3.enabled
        self3.type == "Approval"
        self3.requestedOn

        self4.enabled
        self4.type == "Approval"
        self4.requestedOn

        self5.enabled
        self5.type == "Approval"
        !self5.requestedOn
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

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:

        def user = getRandomUsers(1)[0]
        Date skillDate = new Date()
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user, skillDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
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

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        def timestamp = new Date()
        def res = skillsService.addSkill([projectId: project2.projectId, skillId: skill.skillId], user, timestamp)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
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
        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)
        def skill4 = createSkill(1, 1, 40, 0, 1, 0, 100)
        def skill5 = createSkill(1, 2, 50, 0, 1, 0, 100)
        def skill6 = createSkill(1, 2, 60, 0, 1, 0, 100)

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

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)
        def exportedButNotImported = createSkill(1, 1, 4, 0, 1, 0, 10)

        def skill4 = createSkill(2, 2, 4, 0, 1, 0, 100)
        def skill5 = createSkill(2, 2, 5, 0, 1, 0, 100)
        def skill6 = createSkill(2, 2, 6, 0, 1, 0, 100)

        def p2native = createSkill(2, 2, 99, 0, 1, 0, 100)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(exportedButNotImported)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.createSkill(p2native)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, exportedButNotImported.skillId)  // this is does not get imported

        when:
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)

        def skills = skillsService.getSkillsForSubject(project2.projectId, p2subj1.subjectId)
        def skillsForProject = skillsService.getSkillsForProject(project2.projectId)
        def skillsForProjectWithoutImported = skillsService.getSkillsForProject(project2.projectId, "", true)

        def exportedSkills = skillsService.getExportedSkills(project1.projectId, 6, 1, "subjectName", true)

        then:
        skills.findAll { it.readOnly == true && it.copiedFromProjectId == project1.projectId && it.copiedFromProjectName == project1.name }.size() == 3
        //copiedFromProjectId, copiedFromProjectName, and readOnly are not populated by this endpoint
        skillsForProject.findAll { it.readOnly && it.copiedFromProjectId && it.copiedFromProjectName }.size() == 0

        skills.collect { it.skillId } == ["skill4subj2", "skill5subj2", "skill6subj2", "skill99subj2", "skill1", "skill2", "skill3"]
        skillsForProjectWithoutImported.collect { it.skillId } == ["skill4subj2", "skill5subj2", "skill6subj2", "skill99subj2"]

        exportedSkills
        exportedSkills.totalCount == 4
        exportedSkills.count == 4
        exportedSkills.data.find { it.skillId == 'skill1' }.importedProjectCount == 1
        exportedSkills.data.find { it.skillId == 'skill2' }.importedProjectCount == 1
        exportedSkills.data.find { it.skillId == 'skill3' }.importedProjectCount == 1
        exportedSkills.data.find { it.skillId == 'skill4' }.importedProjectCount == 0
    }

    def "get exported to catalog stats for project"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
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
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

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

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)

        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 100)
        def skill5 = createSkill(1, 1, 5, 0, 1, 0, 100)
        def skill6 = createSkill(1, 1, 6, 0, 1, 0, 100)

        def skill7 = createSkill(3, 1, 7, 0, 1, 0, 100)

        def p2subj2skill1 = createSkill(2, 3, 99, 0, 1, 0, 100)

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
        skillsService.createSkill(p2subj2skill1)

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, skill7.skillId)
        def noImports = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def oneImport = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj2.subjectId, project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)
        def threeImportsDifferentSubjects = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj2.subjectId, project3.projectId, skill7.skillId)
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

    def "get exported skill usage stats - skill not exported or reused"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, [skill])
        when:
        def res = skillsService.getExportedSkillStats(project1.projectId, skill.skillId)
        then:
        res.projectId == project1.projectId
        res.skillId == skill.skillId
        !res.isExported
        !res.users
        !res.exportedOn
        !res.isReusedLocally
    }

    def "get exported skill usage stats"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)
        def p2subj2 = createSubject(2, 3)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)

        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 100)
        def skill5 = createSkill(1, 1, 5, 0, 1, 0, 100)
        def skill6 = createSkill(1, 1, 6, 0, 1, 0, 100)

        def skill7 = createSkill(3, 1, 7, 0, 1, 0, 100)

        def p2subj2native = createSkill(2, 3, 1, 0, 1, 0, 100)

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
        skillsService.createSkill(p2subj2native)

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, skill7.skillId)

        def noImports = skillsService.getExportedSkillStats(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def oneImport = skillsService.getExportedSkillStats(project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)
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

        def skill = createSkill(1, 1, 1, 0, 1, 0, 1000)
        def skill2 = createSkill(1, 1, 2, 0, 2, 0, 2000)
        def skill3 = createSkill(1, 2, 3, 0, 3, 0, 3000)
        def skill4 = createSkill(1, 2, 4, 0, 1, 0, 100)
        def skill5 = createSkill(1, 1, 5, 0, 1, 0, 100)
        def skill6 = createSkill(1, 1, 6, 0, 1, 0, 100)

        def skill7 = createSkill(3, 1, 7, 0, 1, 0, 100)

        def skill9 = createSkill(2, 2, 9, 0, 1, 0, 100)

        def p2subj1native = createSkill(2, 2, 1, 0, 1, 0, 9000)
        p2subj1native.name = "skill99"
        def  p2subj2native= createSkill(2, 3, 1, 0, 1, 0, 9000)
        p2subj2native.name = "skill999"

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
        skillsService.createSkill(p2subj1native)
        skillsService.createSkill(p2subj2native)

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
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

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
        sortedByPointIncrementAsc.data[5].pointIncrement == 3000
        sortedByPointIncrementDesc.data[0].skillId == skill3.skillId
        sortedByPointIncrementDesc.data[0].pointIncrement == 3000
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
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
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

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)

        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 100)
        def skill5 = createSkill(1, 1, 5, 0, 1, 0, 100)
        def skill6 = createSkill(1, 1, 6, 0, 1, 0, 100)

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

    def "cannot export a skill with dependencies"() {
        // create skills, add dependencies, try to export top of chain
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        skillsService.addLearningPathPrerequisite(project1.projectId, skill2.skillId, skill.skillId)

        when:

        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        then:
        def e = thrown(Exception)
        e.getMessage().contains("Skill [skill2] has dependencies. Skills with dependencies may not be exported to the catalog, errorCode:ExportToCatalogNotAllowed")
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
        e.getMessage().contains("Skill [skill2] is disabled. Disabled skills may not be exported to the catalog, errorCode:ExportToCatalogNotAllowed")
    }

    def "export skill that other skills depend on"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 2)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        skillsService.addLearningPathPrerequisite(project1.projectId, skill2.skillId, skill.skillId)

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

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)
        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 100)

        def p2skill1 = createSkill(2, 1, 11, 0, 1, 0, 100)
        def p2skill2 = createSkill(2, 1, 12, 0, 1, 0, 100)

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
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.addLearningPathPrerequisite(project1.projectId, skill2.skillId, skill.skillId)

        skillsService.deleteSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: skill.skillId])

        def importedSkill = skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: skill.skillId])
        then:
        def e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.NOT_FOUND
    }

    def "add a dependency to a skill that has been exported to the catalog"() {
        def project1 = createProject(1)

        def p1subj1 = createSubject(1, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 100)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 100)
        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 100)


        skillsService.createProject(project1)
        skillsService.createSubject(p1subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)

        when:
        skillsService.exportSkillToCatalog(skill.projectId, skill4.skillId)
        skillsService.addLearningPathPrerequisite(skill4.projectId, skill4.skillId, skill.skillId)

        then:
        def e = thrown(SkillsClientException)
        e.getMessage().contains("Skill [${skill4.skillId}] was exported to the Skills Catalog. A skill in the catalog cannot have prerequisites on the learning path.")
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

    def "project badges can depend on imported skills" () {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 50)
        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        def p2badge1 = createBadge(2, 11)
        def p3badge1 = createBadge(3, 42)

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
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)
        skillsService.createBadge(p2badge1)
        skillsService.createBadge(p3badge1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill4.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill3.skillId)

        skillsService.assignSkillToBadge(project2.projectId, p2badge1.badgeId, p2skill1.skillId)
        skillsService.assignSkillToBadge(project2.projectId, p2badge1.badgeId, skill.skillId)

        skillsService.assignSkillToBadge(project3.projectId, p3badge1.badgeId, p3skill1.skillId)
        skillsService.assignSkillToBadge(project3.projectId, p3badge1.badgeId, skill3.skillId)

        p2badge1.enabled = true
        skillsService.updateBadge(p2badge1, p2badge1.badgeId)

        p3badge1.enabled = true
        skillsService.updateBadge(p3badge1, p3badge1.badgeId)

        when:
        def user = getRandomUsers(1)[0]

        skillsService.addSkill([projectId: project2.projectId, skillId: p2skill1.skillId], user)
        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def p2bSumm = skillsService.getBadgeSummary(user, project2.projectId, p2badge1.badgeId)
        def p3bSumPre = skillsService.getBadgeSummary(user, project3.projectId, p3badge1.badgeId)

        skillsService.addSkill([projectId: project1.projectId, skillId: skill3.skillId], user)
        skillsService.addSkill([projectId: project3.projectId, skillId: p3skill1.skillId], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def p3bSumPost = skillsService.getBadgeSummary(user, project3.projectId, p3badge1.badgeId)

        then:
        p2bSumm.numTotalSkills == 2
        p2bSumm.numSkillsAchieved == 2
        p2bSumm.badgeAchieved
        p3bSumPre.numTotalSkills == 2
        p3bSumPre.numSkillsAchieved == 0
        !p3bSumPre.badgeAchieved
        p3bSumPost.numTotalSkills == 2
        p3bSumPost.numSkillsAchieved == 2
        p3bSumPost.badgeAchieved
    }

    def "deleting imported catalog skill causes badge to be achieved if other dependencies are satisfied"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)
        def skill3 = createSkill(1, 1, 3, 0, 1, 0, 50)
        def skill4 = createSkill(1, 1, 4, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        def p2badge1 = createBadge(2, 11)
        def p3badge1 = createBadge(3, 42)

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
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)
        skillsService.createBadge(p2badge1)
        skillsService.createBadge(p3badge1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill4.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill3.skillId)

        skillsService.assignSkillToBadge(project2.projectId, p2badge1.badgeId, p2skill1.skillId)
        skillsService.assignSkillToBadge(project2.projectId, p2badge1.badgeId, skill.skillId)

        skillsService.assignSkillToBadge(project3.projectId, p3badge1.badgeId, p3skill1.skillId)
        skillsService.assignSkillToBadge(project3.projectId, p3badge1.badgeId, skill3.skillId)

        p2badge1.enabled = true
        skillsService.updateBadge(p2badge1, p2badge1.badgeId)

        p3badge1.enabled = true
        skillsService.updateBadge(p3badge1, p3badge1.badgeId)

        when:
        def user = getRandomUsers(1)[0]

        skillsService.addSkill([projectId: project2.projectId, skillId: p2skill1.skillId], user)
        skillsService.deleteSkill(skill)

        def p2bSumm = skillsService.getBadgeSummary(user, project2.projectId, p2badge1.badgeId)
        def p3bSum = skillsService.getBadgeSummary(user, project3.projectId, p3badge1.badgeId)

        then:
        p2bSumm.numTotalSkills == 1
        p2bSumm.numSkillsAchieved == 1
        p2bSumm.badgeAchieved
        p3bSum.numTotalSkills == 2
        p3bSum.numSkillsAchieved == 0
        !p3bSum.badgeAchieved
    }

    def "imported skills as dependency should not be returned as potential dependencies for global badges"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        def p2badge1 = createBadge(2, 11)
        def p3badge1 = createBadge(3, 42)

        skill.name = "Sample Name Query Test"

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)
        skillsService.createBadge(p2badge1)
        skillsService.createBadge(p3badge1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        def supervisorService = createSupervisor()

        def badge = SkillsFactory.createBadge()
        badge.enabled = true
        supervisorService.createGlobalBadge(badge)

        when:
        def res = supervisorService.getAvailableSkillsForGlobalBadge(badge.badgeId, "Sample")

        then:
        res.totalAvailable == 1
        res.suggestedSkills.findAll {it.name == 'Sample Name Query Test'}.size() == 1
    }

    def "cannot assign skill imported from catalog as a dependency to a global badge"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        def p2badge1 = createBadge(2, 11)
        def p3badge1 = createBadge(3, 42)

        skill.name = "Sample Name Query Test"

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)
        skillsService.createBadge(p2badge1)
        skillsService.createBadge(p3badge1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill.skillId)

        def supervisorService = createSupervisor()

        def badge = SkillsFactory.createBadge()
        badge.enabled = true
        supervisorService.createGlobalBadge(badge)

        when:
        supervisorService.assignSkillToGlobalBadge([badgeId: badge.badgeId, projectId: project3.projectId, skillId: skill.skillId])

        then:
        def e = thrown(SkillsClientException)
        e.message.contains('Imported Skills may not be added as Global Badge Dependencies')
    }

    def "cannot share via cross project skills that have been imported from the catalog"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill2.skillId)

        when:
        skillsService.shareSkill(project2.projectId, skill.skillId, project3.projectId)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("Skills imported from the catalog may not be shared as cross project dependencies")
    }

    def "skills imported from the catalog cannot be shared as cross project dependencies to all projects"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project3.projectId, p3subj1.subjectId, project1.projectId, skill2.skillId)

        when:
        skillsService.shareSkill(project2.projectId, skill.skillId, "ALL_SKILLS_PROJECTS")

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("Skills imported from the catalog may not be shared as cross project dependencies")
    }

    def "skills exported to the catalog can be shared as cross project dependencies"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)


        when:
        skillsService.shareSkill(project1.projectId, skill.skillId, project2.projectId)
        def sharedSkills = skillsService.getSharedWithMeSkills(project2.projectId)

        then:
        sharedSkills.size() == 1
        sharedSkills[0].skillName == skill.name
        sharedSkills[0].projectId == project1.projectId
    }

    def "skills exported to the catalog can be shared as cross project dependencies with all projects"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)
        def p3skill1 = createSkill(3, 1, 99, 0, 1, 0, 100)


        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)
        skillsService.createSkill(p3skill1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)


        when:
        skillsService.shareSkill(project1.projectId, skill.skillId, "ALL_SKILLS_PROJECTS")
        def sharedSkills = skillsService.getSharedWithMeSkills(project2.projectId)

        then:
        sharedSkills.size() == 1
        sharedSkills[0].skillName == skill.name
        sharedSkills[0].projectId == project1.projectId
    }

    def "changes in points per increment of exported skill are not replicated to imported skills (modifications are propagated to imported fields on async basis)"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..3).collect {createSkill(1, 1, it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each { skillsService.exportSkillToCatalog(project1.projectId, it.skillId) }

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2_skills = (1..3).collect {createSkill(2, 1, 3+it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2_skills)
        p2_skills.each { skillsService.exportSkillToCatalog(project2.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, p1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        p1_skills[0].pointIncrement = 5000
        skillsService.createSkills([p1_skills[0]])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        //projectId, subjectId, skillId
        skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: p1_skills[0].skillId]).pointIncrement == 250
        skillsService.getSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: p1_skills[0].skillId]).pointIncrement == 5000
    }

    def "changes in number of occurrences of exported skill ARE replicated to imported skills (modifications are propagated to imported fields on async basis)"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..3).collect {createSkill(1, 1, it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each { skillsService.exportSkillToCatalog(project1.projectId, it.skillId) }

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2_skills = (1..3).collect {createSkill(2, 1, 3+it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2_skills)
        p2_skills.each { skillsService.exportSkillToCatalog(project2.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, p1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        p1_skills[0].pointIncrement = 250
        p1_skills[0].numPerformToCompletion = 10
        skillsService.createSkills([p1_skills[0]])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        //projectId, subjectId, skillId
        skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: p1_skills[0].skillId]).totalPoints == 2500
        skillsService.getSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: p1_skills[0].skillId]).totalPoints == 2500
    }

    def "changes in the number of occurrences of an exported skill should cause changes in the level thresholds for the importing project and subject"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..4).collect {createSkill(1, 1, it, 0, 1, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each { skillsService.exportSkillToCatalog(project1.projectId, it.skillId) }

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2_skills = (1..4).collect {createSkill(2, 1, 3+it, 0, 1, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2_skills)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, p1_skills[0].skillId)

        List<LevelDefinitionRes> subjectLevelsPreEdit = levelDefinitionStorageService.getLevels(project2.projectId, p2subj1.subjectId)
        List<LevelDefinitionRes> projectLevelsPreEdit = levelDefinitionStorageService.getLevels(project2.projectId)

        println "subject pre: "+subjectLevelsPreEdit
        println "project pre: "+projectLevelsPreEdit

        when:
        p1_skills[0].numPerformToCompletion = 5
        skillsService.createSkills([p1_skills[0]])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        List<LevelDefinitionRes> subjectLevelsPostEdit = levelDefinitionStorageService.getLevels(project2.projectId, p2subj1.subjectId)
        List<LevelDefinitionRes> projectLevelsPostEdit = levelDefinitionStorageService.getLevels(project2.projectId)

        println "subject post: "+subjectLevelsPostEdit
        println "project post: "+projectLevelsPostEdit

        then:
        subjectLevelsPreEdit[0].pointsFrom == 125
        subjectLevelsPreEdit[1].pointsFrom == 312
        subjectLevelsPreEdit[2].pointsFrom == 562
        subjectLevelsPreEdit[3].pointsFrom == 837
        subjectLevelsPreEdit[4].pointsFrom == 1150
        projectLevelsPreEdit[0].pointsFrom == 125
        projectLevelsPreEdit[1].pointsFrom == 312
        projectLevelsPreEdit[2].pointsFrom == 562
        projectLevelsPreEdit[3].pointsFrom == 837
        projectLevelsPreEdit[4].pointsFrom == 1150
        subjectLevelsPostEdit[0].pointsFrom == 225
        subjectLevelsPostEdit[0].pointsFrom == 225
    }

    def "points awarded for imported skill must have last earned date when fetching users"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def p2skill1 = createSkill(2, 1, 55, 0, 1, 0, 100)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(p2skill1)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def user = getRandomUsers(1)[0]

        when:
        Date date = new Date()
        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user, date)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def subjectUsers = skillsService.getSubjectUsers(project2.projectId, p2subj1.subjectId)

        then:
        subjectUsers.data[0].userId == user
        subjectUsers.data[0].lastUpdated == DTF.print(date.time)
    }

    def "project users should take into account users of imported skills"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 50)

        def skill3 = createSkill(2, 1, 3, 0, 1, 0, 100)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)

        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)

        def users = getRandomUsers(6)
        def importUsers = users.subList(0, 5)
        importUsers.eachWithIndex { user, index ->
            def skillId = ''
            if (index % 2 == 0) {
                skillId = skill.skillId
            } else {
                skillId = skill2.skillId
            }
            skillsService.addSkill([projectId: project1.projectId, skillId: skillId], user)
        }

        def projectNativeUser = users.last()

        skillsService.addSkill([projectId: project2.projectId, skillId: skill3.skillId], projectNativeUser)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        skillsService.importSkillFromCatalog(project2.projectId, p2subj1.subjectId, project1.projectId, skill2.skillId)
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        def projectUsers = skillsService.getProjectUsers(project2.projectId)
        def projectUsers_subj = skillsService.getSubjectUsers(project2.projectId, p2subj1.subjectId)

        then:
        projectUsers.count == 6
        projectUsers.totalCount == 6
        projectUsers.data.size() == 6
        projectUsers.data.find { it.userId == users[0] }
        projectUsers.data.find { it.userId == users[1] }
        projectUsers.data.find { it.userId == users[2] }
        projectUsers.data.find { it.userId == users[3] }
        projectUsers.data.find { it.userId == users[4] }
        projectUsers.data.find { it.userId == users[5] }

        projectUsers_subj.count == 6
        projectUsers_subj.totalCount == 6
        projectUsers_subj.data.size() == 6
        projectUsers_subj.data.find { it.userId == users[0] }
        projectUsers_subj.data.find { it.userId == users[1] }
        projectUsers_subj.data.find { it.userId == users[2] }
        projectUsers_subj.data.find { it.userId == users[3] }
        projectUsers_subj.data.find { it.userId == users[4] }
        projectUsers_subj.data.find { it.userId == users[5] }
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

    def "exported skill usage stats should work when skill has been imported under a group"() {
        def project1 = createProject(1)
        def project2 = createProject(2)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p2sub1g1 = createSkillsGroup(2, 1, 55)

        def skill = createSkill(1, 1, 1, 0, 1, 0, 150)
        def skill2 = createSkill(1, 1, 2, 0, 1, 0, 150)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSkill(p2sub1g1)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, p2sub1g1.skillId, [
                [
                        "projectId": project1.projectId,
                        "skillId": skill.skillId
                ]
        ])

        when:
        def stats = skillsService.getExportedSkillStats(project1.projectId, skill.skillId)

        then:
        stats
    }
  
    def "acknowledging approval request rejection on importing project should not cause error"() {
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

        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)

        when:
        def user = getRandomUsers(1)[0]
        def res = skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)
        assert res.body.explanation == "Skill was submitted for approval"

        def p1Approvals = skillsService.getApprovals(project1.projectId, 7, 1, 'requestedOn', false)
        skillsService.rejectSkillApprovals(project1.projectId, [p1Approvals.data[0].id])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        skillsService.removeRejectionFromView(project2.projectId, p1Approvals.data[0].id, user)

        def p2Approvals = skillsService.getApprovals(project2.projectId, 7, 1, 'requestedOn', false)
        p1Approvals = skillsService.getApprovals(project1.projectId, 7, 1, 'requestedOn', false)

        then:
        p1Approvals.count == 0
        p2Approvals.count == 0
    }

}
