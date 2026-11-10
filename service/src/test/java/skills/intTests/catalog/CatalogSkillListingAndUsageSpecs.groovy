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

import static skills.intTests.utils.SkillsFactory.*

class CatalogSkillListingAndUsageSpecs extends CatalogIntSpec {

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

        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, skill7.skillId)

        def proj4 = createProject(4)
        def proj4_subj1 = createSubject(4, 1)
        skillsService.createProjectAndSubjectAndSkills(proj4, proj4_subj1, [])
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj4.projectId, proj4_subj1.subjectId, [
                [projectId: project1.projectId, skillId: skill.skillId],
                [projectId: project1.projectId, skillId: skill2.skillId],
        ])

        when:
        def noImports = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill.skillId)
        def oneImport = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj2.subjectId, project1.projectId, skill2.skillId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, project1.projectId, skill3.skillId)
        def threeImportsDifferentSubjects = skillsService.getImportedSkillsStats(project2.projectId)
        skillsService.importSkillFromCatalogAndFinalize(project2.projectId, p2subj2.subjectId, project3.projectId, skill7.skillId)

        def fourImporstTwoProjectsTwoSubjects = skillsService.getImportedSkillsStats(project2.projectId)

        def proj4Stats = skillsService.getImportedSkillsStats(proj4.projectId)

        then:
        noImports.numberOfProjectsImportedFrom == 0
        noImports.numberOfSkillsImported == 0
        oneImport.numberOfProjectsImportedFrom == 1
        oneImport.numberOfSkillsImported == 1
        threeImportsDifferentSubjects.numberOfProjectsImportedFrom == 1
        threeImportsDifferentSubjects.numberOfSkillsImported == 3
        fourImporstTwoProjectsTwoSubjects.numberOfProjectsImportedFrom == 2
        fourImporstTwoProjectsTwoSubjects.numberOfSkillsImported == 4

        proj4Stats.numberOfProjectsImportedFrom == 1
        proj4Stats.numberOfSkillsImported == 2
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
        participateInProject(project2.projectId, [user])
        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user, date)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def subjectUsers = skillsService.getSubjectUsers(project2.projectId, p2subj1.subjectId)

        then:
        subjectUsers.data[0].userId == user
        subjectUsers.data[0].lastUpdated == date.toInstant().toString()
    }


    def "project users include visiting participants with imported skill credit"() {
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
        participateInProject(project2.projectId, importUsers)
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

}
