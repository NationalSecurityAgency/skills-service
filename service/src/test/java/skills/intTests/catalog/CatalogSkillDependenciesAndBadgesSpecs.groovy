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

import org.springframework.http.HttpStatus
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.*

class CatalogSkillDependenciesAndBadgesSpecs extends CatalogIntSpec {

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
        e.getMessage().contains("Skill [skill2] has dependencies. Skills with dependencies may not be exported to the catalog")
        e.getMessage().contains("errorCode:ExportToCatalogNotAllowed")
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

        participateInProject(project3.projectId, [user])
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

        def badge = SkillsFactory.createBadge()
        badge.enabled = true
        skillsService.createGlobalBadge(badge)

        when:
        def res = skillsService.getAvailableSkillsForGlobalBadge(badge.badgeId, "Sample")

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

        def badge = SkillsFactory.createBadge()
        badge.enabled = true
        skillsService.createGlobalBadge(badge)

        when:
        skillsService.assignSkillToGlobalBadge([badgeId: badge.badgeId, projectId: project3.projectId, skillId: skill.skillId])

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

}
