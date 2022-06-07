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
}
