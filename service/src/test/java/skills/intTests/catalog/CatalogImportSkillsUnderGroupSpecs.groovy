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
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserPoints

import static skills.intTests.utils.SkillsFactory.*

class CatalogImportSkillsUnderGroupSpecs extends CatalogIntSpec {

    def "import skills under a group - contribute to user brand new points for subject and project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 1, 10)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skillsGroup])

        String user = getRandomUsers(1)[0]
        when:
        skillsService.addSkill(p1skills[0], user)
        skillsService.addSkill(p1skills[1], user)

        skillsService.bulkImportSkillsIntoGroupFromCatalog(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                p1skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        UserPoints user_p2subj1_pts_before = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, p2subj1.subjectId)
        UserPoints user_p2_pts_before = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, null)
        def user_subjSummary_before = skillsService.getSkillSummary(user, p2.projectId, p2subj1.subjectId)
        def user_projSummary_before = skillsService.getSkillSummary(user, p2.projectId)

        skillsService.finalizeSkillsImportFromCatalog(p2.projectId)

        UserPoints user_p2subj1_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, p2subj1.subjectId)
        UserPoints user_p2_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, null)
        def user_subjSummary = skillsService.getSkillSummary(user, p2.projectId, p2subj1.subjectId)
        def user_projSummary = skillsService.getSkillSummary(user, p2.projectId)
        then:
        !user_p2subj1_pts_before
        user_subjSummary_before.points == 0
        user_subjSummary_before.totalPoints == 0

        !user_p2_pts_before
        user_projSummary_before.points == 0
        user_projSummary_before.totalPoints == 0

        user_p2subj1_pts.points == 200
        user_subjSummary.points == 200
        user_subjSummary.totalPoints == 500

        user_p2_pts.points == 200
        !user_p2_pts.skillId
        user_projSummary.points == 200
        user_projSummary.totalPoints == 500
    }

    def "do not allow finalizing if imported skills belongs to a disabled group"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2, 2)

        def p2skillsGroup = createSkillsGroup(2, 1, 10)
        p2skillsGroup.enabled = 'false'
        skillsService.createSkill(p2skillsGroup)

        skillsService.bulkImportSkillsIntoGroupFromCatalog(project2.p.projectId, project2.s1.subjectId, p2skillsGroup.skillId,
                project1.s1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        when:
        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId, false)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Cannot finalize imported skills, there are [3] skill(s) pending finalization that belong to a disabled subject or group")
    }

    def "import skills under a group - update to user existing points for subject and project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 2, 10)
        def p2skills = createSkills(5, 2, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skills, p2skillsGroup].flatten())

        String user = getRandomUsers(1)[0]
        when:
        skillsService.addSkill(p1skills[0], user)
        skillsService.addSkill(p1skills[1], user)
        skillsService.addSkill(p2skills[2], user)

        skillsService.bulkImportSkillsIntoGroupFromCatalog(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                p1skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        UserPoints user_p2subj1_pts_before = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, p2subj1.subjectId)
        UserPoints user_p2_pts_before = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, null)
        def user_subjSummary_before = skillsService.getSkillSummary(user, p2.projectId, p2subj1.subjectId)
        def user_projSummary_before = skillsService.getSkillSummary(user, p2.projectId)

        skillsService.finalizeSkillsImportFromCatalog(p2.projectId)

        UserPoints user_p2subj1_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, p2subj1.subjectId)
        UserPoints user_p2_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, null)
        def user_subjSummary = skillsService.getSkillSummary(user, p2.projectId, p2subj1.subjectId)
        def user_projSummary = skillsService.getSkillSummary(user, p2.projectId)
        then:
        user_p2subj1_pts_before.points == 100
        user_subjSummary_before.points == 100
        user_subjSummary_before.totalPoints == 500

        user_p2_pts_before.points == 100
        !user_p2_pts_before.skillId
        user_projSummary_before.points == 100
        user_projSummary_before.totalPoints == 500

        user_p2subj1_pts.points == 300
        user_subjSummary.points == 300
        user_subjSummary.totalPoints == 1000

        user_p2_pts.points == 300
        !user_p2_pts.skillId
        user_projSummary.points == 300
        user_projSummary.totalPoints == 1000
    }

    def "import skills under a group - update points for subject and project - multiple projects, subjects and skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2skillsGroup = SkillsFactory.createSkillsGroup(2, 2, 10)
        def p2skills = createSkills(5, 2, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [p2skills, p2skillsGroup].flatten())

        def p3 = createProject(3)
        def p3subj1 = createSubject(3, 2)
        def p3subj2 = createSubject(3, 3)
        def p3subj3 = createSubject(3, 4)
        def p3skillsGroup = SkillsFactory.createSkillsGroup(3, 2, 10)
        skillsService.createProjectAndSubjectAndSkills(p3, p3subj1, [p3skillsGroup].flatten())
        skillsService.createSubject(p3subj2)
        def p3s2skillsGroup = SkillsFactory.createSkillsGroup(3, 3, 20)
        skillsService.createSkill(p3s2skillsGroup)
        skillsService.createSubject(p3subj3)
        def p3_subj3_skills = createSkills(3, 3, 4, 100)
        skillsService.createSkills(p3_subj3_skills)

        List<String> users = getRandomUsers(4)
        String user = users[0]
        String user2 = users[1]
        String user3 = users[2]
        when:
        // user 1
        skillsService.addSkill(p1skills[0], user)
        skillsService.addSkill(p1skills[1], user)
        skillsService.addSkill(p2skills[2], user)

        // user 2
        p1skills.each {
            skillsService.addSkill(it, user2)
        }

        // user 3
        [p1skills, p2skills, p3_subj3_skills].flatten().each {
            skillsService.addSkill(it, user3)
        }

        skillsService.bulkImportSkillsIntoGroupFromCatalog(p2.projectId, p2subj1.subjectId, p2skillsGroup.skillId,
                p1skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        UserPoints user_p2subj1_pts_before = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, p2subj1.subjectId)
        UserPoints user_p2_pts_before = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, null)
        def user_subjSummary_before = skillsService.getSkillSummary(user, p2.projectId, p2subj1.subjectId)
        def user_projSummary_before = skillsService.getSkillSummary(user, p2.projectId)

        skillsService.finalizeSkillsImportFromCatalog(p2.projectId)

        UserPoints user_p2subj1_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, p2subj1.subjectId)
        UserPoints user_p2_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user, null)
        def user_subjSummary = skillsService.getSkillSummary(user, p2.projectId, p2subj1.subjectId)
        def user_projSummary = skillsService.getSkillSummary(user, p2.projectId)

        skillsService.bulkImportSkillsIntoGroupFromCatalog(p3.projectId, p3subj1.subjectId, p3skillsGroup.skillId,
                [p1skills[1], p1skills[2]].collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(p3.projectId, p3subj1.subjectId,
                [p1skills[0], p1skills[3]].collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsIntoGroupFromCatalog(p3.projectId, p3subj2.subjectId, p3s2skillsGroup.skillId,
                [p1skills[4]].collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.finalizeSkillsImportFromCatalog(p3.projectId)

        UserPoints user_p3subj1_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p3.projectId, user, p3subj1.subjectId)
        UserPoints user_p3_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p3.projectId, user, null)
        def user_p3subjSummary = skillsService.getSkillSummary(user, p3.projectId, p3subj1.subjectId)
        def user_p3subj2Summary = skillsService.getSkillSummary(user, p3.projectId, p3subj2.subjectId)
        def user_p3subj3Summary = skillsService.getSkillSummary(user, p3.projectId, p3subj3.subjectId)
        def user_p3projSummary = skillsService.getSkillSummary(user, p3.projectId)

        // user 2
        UserPoints user2_p2subj1_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user2, p2subj1.subjectId)
        UserPoints user2_p2_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user2, null)
        def user2_subjSummary = skillsService.getSkillSummary(user2, p2.projectId, p2subj1.subjectId)
        def user2_projSummary = skillsService.getSkillSummary(user2, p2.projectId)

        UserPoints user2_p3subj1_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p3.projectId, user2, p3subj1.subjectId)
        UserPoints user2_p3subj2_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p3.projectId, user2, p3subj2.subjectId)
        UserPoints user2_p3subj3_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p3.projectId, user2, p3subj3.subjectId)
        UserPoints user2_p3_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p3.projectId, user2, null)
        def user2_p3subjSummary = skillsService.getSkillSummary(user2, p3.projectId, p3subj1.subjectId)
        def user2_p3subj2Summary = skillsService.getSkillSummary(user2, p3.projectId, p3subj2.subjectId)
        def user2_p3subj3Summary = skillsService.getSkillSummary(user2, p3.projectId, p3subj3.subjectId)
        def user2_p3projSummary = skillsService.getSkillSummary(user2, p3.projectId)

        // user 3
        UserPoints user3_p2subj1_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user3, p2subj1.subjectId)
        UserPoints user3_p2_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p2.projectId, user3, null)
        def user3_subjSummary = skillsService.getSkillSummary(user3, p2.projectId, p2subj1.subjectId)
        def user3_projSummary = skillsService.getSkillSummary(user3, p2.projectId)

        UserPoints user3_p3subj1_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p3.projectId, user3, p3subj1.subjectId)
        UserPoints user3_p3subj2_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p3.projectId, user3, p3subj2.subjectId)
        UserPoints user3_p3subj3_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p3.projectId, user3, p3subj3.subjectId)
        UserPoints user3_p3_pts = userPointsRepo.findByProjectIdAndUserIdAndSkillId(p3.projectId, user3, null)
        def user3_p3subjSummary = skillsService.getSkillSummary(user3, p3.projectId, p3subj1.subjectId)
        def user3_p3subj2Summary = skillsService.getSkillSummary(user3, p3.projectId, p3subj2.subjectId)
        def user3_p3subj3Summary = skillsService.getSkillSummary(user3, p3.projectId, p3subj3.subjectId)
        def user3_p3projSummary = skillsService.getSkillSummary(user3, p3.projectId)

        then:
        // -----------------------------------------
        // user 1
        user_p2subj1_pts_before.points == 100
        user_subjSummary_before.points == 100
        user_subjSummary_before.totalPoints == 500

        user_p2_pts_before.points == 100
        !user_p2_pts_before.skillId
        user_projSummary_before.points == 100
        user_projSummary_before.totalPoints == 500

        user_p2subj1_pts.points == 300
        user_subjSummary.points == 300
        user_subjSummary.totalPoints == 1000

        user_p2_pts.points == 300
        !user_p2_pts.skillId
        user_projSummary.points == 300
        user_projSummary.totalPoints == 1000

        user_p3subj1_pts.points == 200
        user_p3_pts.points == 200
        !user_p3_pts.skillId
        user_p3subjSummary.points == 200
        user_p3subjSummary.totalPoints == 400
        user_p3subj2Summary.points == 0
        user_p3subj2Summary.totalPoints == 100
        user_p3subj3Summary.points == 0
        user_p3subj3Summary.totalPoints == 300
        user_p3projSummary.points == 200
        user_p3projSummary.totalPoints == 800

        // user 2
        user2_p2subj1_pts.points == 500
        user2_subjSummary.points == 500
        user2_subjSummary.totalPoints == 1000
        user2_p2_pts.points == 500
        !user2_p2_pts.skillId
        user2_projSummary.points == 500
        user2_projSummary.totalPoints == 1000

        user2_p3subj1_pts.points == 400
        user2_p3subj2_pts.points == 100
        !user2_p3subj3_pts
        user2_p3_pts.points == 500
        user2_p3subjSummary.points == 400
        user2_p3subjSummary.totalPoints == 400
        user2_p3subj2Summary.points == 100
        user2_p3subj2Summary.totalPoints == 100
        user2_p3subj3Summary.points == 0
        user2_p3subj3Summary.totalPoints == 300
        user2_p3projSummary.points == 500
        user2_p3projSummary.totalPoints == 800

        // user 3
        user3_p2subj1_pts.points == 1000
        user3_subjSummary.points == 1000
        user3_subjSummary.totalPoints == 1000
        user3_p2_pts.points == 1000
        !user3_p2_pts.skillId
        user3_projSummary.points == 1000
        user3_projSummary.totalPoints == 1000

        user3_p3subj1_pts.points == 400
        user3_p3subj2_pts.points == 100
        user3_p3subj3_pts.points == 300
        user3_p3_pts.points == 800
        user3_p3subjSummary.points == 400
        user3_p3subjSummary.totalPoints == 400
        user3_p3subj2Summary.points == 100
        user3_p3subj2Summary.totalPoints == 100
        user3_p3subj3Summary.points == 300
        user3_p3subj3Summary.totalPoints == 300
        user3_p3projSummary.points == 800
        user3_p3projSummary.totalPoints == 800
    }


}
