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


import skills.intTests.utils.SkillsClientException
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class CatalogSkillsClientDisplaySpecs extends CatalogIntSpec {

    def "skills are only visible after they were finalized"() {
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

        when:
        def proj2Sum = skillsService.getSkillSummary("user1", project2.projectId)
        def subjSum = skillsService.getSkillSummary("user1", project2.projectId, p2subj1.subjectId)
        def subjDesc = skillsService.getSubjectDescriptions(project2.projectId, p2subj1.subjectId)

        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)

        def proj2Sum_after = skillsService.getSkillSummary("user1", project2.projectId)
        def subjSum_after = skillsService.getSkillSummary("user1", project2.projectId, p2subj1.subjectId)
        def subjDesc_after = skillsService.getSubjectDescriptions(project2.projectId, p2subj1.subjectId)
        then:
        proj2Sum.totalPoints == 5 * 250 * 3
        proj2Sum.subjects[0].totalPoints == 5 * 250 * 3
        subjSum.skills.collect { it.skillId } == ["skill4", "skill5", "skill6"]
        subjSum.totalPoints == 5 * 250 * 3
        subjDesc.collect { it.skillId } == ["skill4", "skill5", "skill6"]

        proj2Sum_after.totalPoints == 5 * 250 * 3 * 2
        proj2Sum_after.subjects[0].totalPoints == 5 * 250 * 3 * 2
        subjSum_after.skills.collect { it.skillId } == ["skill4", "skill5", "skill6", "skill1", "skill2", "skill3"]
        subjSum_after.totalPoints == 5 * 250 * 3 * 2
        subjDesc_after.collect { it.skillId } == ["skill1", "skill2", "skill3", "skill4", "skill5", "skill6"]
    }

    def "getting single skill that's disable should emit an exception"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..3).collect { createSkill(1, 1, it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each { skillsService.exportSkillToCatalog(project1.projectId, it.skillId) }

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2_skills = (1..3).collect { createSkill(2, 1, 3 + it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2_skills)
        p2_skills.each { skillsService.exportSkillToCatalog(project2.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, p1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        when:
        skillsService.getSingleSkillSummary("user1", project2.projectId, p1_skills[0].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.resBody.contains("Skill with id [skill1] is not enabled")
    }

    def "user points are reflected in the imported skills after being reported via Honor System"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..3).collect { createSkill(1, 1, it, 0, 5, 0, 250) }
        p1_skills.each { it.selfReportingType = SkillDef.SelfReportingType.HonorSystem }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each { skillsService.exportSkillToCatalog(project1.projectId, it.skillId) }

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2_skills = (1..3).collect { createSkill(2, 1, 3 + it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2_skills)

        skillsService.bulkImportSkillsFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, p1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        String user = getRandomUsers(1)[0]
        when:
        def addSkill1 = skillsService.addSkill([projectId: project2.projectId, skillId: p1_skills[0].skillId], user)
        assert addSkill1.body.explanation == "Skill event was applied"
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def res1 = skillsService.getSkillSummary(user, project2.projectId, p2subj1.subjectId)
        then:
        res1.points == 250
        res1.skills.find { it.skillId == p1_skills[0].skillId }.points == 250
    }

    def "today user points are returned for imported skills"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..3).collect { createSkill(1, 1, it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each { skillsService.exportSkillToCatalog(project1.projectId, it.skillId) }

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2_skills = (1..3).collect { createSkill(2, 1, 3 + it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2_skills)

        String user = getRandomUsers(1)[0]
        skillsService.addSkill([projectId: project1.projectId, skillId: p1_skills[0].skillId], user)
        skillsService.addSkill([projectId: project1.projectId, skillId: p1_skills[1].skillId], user, new Date() - 1)
        skillsService.addSkill([projectId: project1.projectId, skillId: p1_skills[2].skillId], user, new Date() - 1)
        skillsService.addSkill([projectId: project1.projectId, skillId: p1_skills[2].skillId], user)

        skillsService.bulkImportSkillsFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, p1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        when:
        def projSummary = skillsService.getSkillSummary(user, project2.projectId)
        def subjSummary = skillsService.getSkillSummary(user, project2.projectId, p2subj1.subjectId)
        def skill0 = skillsService.getSingleSkillSummary(user, project2.projectId, p1_skills[0].skillId)
        def skill1 = skillsService.getSingleSkillSummary(user, project2.projectId, p1_skills[1].skillId)
        def skill2 = skillsService.getSingleSkillSummary(user, project2.projectId, p1_skills[2].skillId)

        def projSummary_p1 = skillsService.getSkillSummary(user, project1.projectId)
        def subjSummary_p1 = skillsService.getSkillSummary(user, project1.projectId, p1subj1.subjectId)
        def skill0_p1 = skillsService.getSingleSkillSummary(user, project1.projectId, p1_skills[0].skillId)
        def skill1_p1 = skillsService.getSingleSkillSummary(user, project1.projectId, p1_skills[1].skillId)
        def skill2_p1 = skillsService.getSingleSkillSummary(user, project1.projectId, p1_skills[2].skillId)
        then:
        projSummary.points == 1000
        projSummary.todaysPoints == 500

        subjSummary.points == 1000
        subjSummary.todaysPoints == 500
        subjSummary.skills.find { it.skillId == p1_skills[0].skillId }.points == 250
        subjSummary.skills.find { it.skillId == p1_skills[1].skillId }.points == 250
        subjSummary.skills.find { it.skillId == p1_skills[2].skillId }.points == 500
        subjSummary.skills.find { it.skillId == p1_skills[0].skillId }.todaysPoints == 250
        subjSummary.skills.find { it.skillId == p1_skills[1].skillId }.todaysPoints == 0
        subjSummary.skills.find { it.skillId == p1_skills[2].skillId }.todaysPoints == 250

        skill0.points == 250
        skill0.todaysPoints == 250
        skill1.points == 250
        skill1.todaysPoints == 0
        skill2.points == 500
        skill2.todaysPoints == 250

        // validate original project as well
        projSummary_p1.points == 1000
        projSummary_p1.todaysPoints == 500

        subjSummary_p1.points == 1000
        subjSummary_p1.todaysPoints == 500
        subjSummary_p1.skills.find { it.skillId == p1_skills[0].skillId }.points == 250
        subjSummary_p1.skills.find { it.skillId == p1_skills[1].skillId }.points == 250
        subjSummary_p1.skills.find { it.skillId == p1_skills[2].skillId }.points == 500
        subjSummary_p1.skills.find { it.skillId == p1_skills[0].skillId }.todaysPoints == 250
        subjSummary_p1.skills.find { it.skillId == p1_skills[1].skillId }.todaysPoints == 0
        subjSummary_p1.skills.find { it.skillId == p1_skills[2].skillId }.todaysPoints == 250

        skill0_p1.points == 250
        skill0_p1.todaysPoints == 250
        skill1_p1.points == 250
        skill1_p1.todaysPoints == 0
        skill2_p1.points == 500
        skill2_p1.todaysPoints == 250
    }
}
