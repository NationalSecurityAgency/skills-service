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


import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsFactory
import skills.services.admin.SkillCatalogFinalizationService
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints

class ApplyEventsThatWereReportedDuringTheFinalizationSpecs extends CatalogIntSpec {

    @Autowired
    SkillCatalogFinalizationService skillCatalogFinalizationService


    def "make sure to propagate events that were reported during finalization and were not accounted for during its data migration"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 1, 10000)
        skill2.pointIncrementInterval = 0
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [skill2])

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [])

        skillsService.exportSkillToCatalog(proj1.projectId, skill2.skillId)
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj2.projectId, p2_subj.subjectId, [
                [projectId: proj1.projectId, skillId: skill2.skillId],
        ])
        String user = getRandomUsers(1)[0]

        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], user)
        Thread.sleep(100)

        long start = System.currentTimeMillis()
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], user)
        Thread.sleep(100)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], user)
        long end = System.currentTimeMillis()

        Thread.sleep(100)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], user)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillId(proj2.projectId, skill2.skillId)
        assert skillDef

        Closure<Integer> getPoints = { String userParam, String projectId, String skillId ->
            List<UserPoints> points = userPointsRepo.findAll().findAll({it.userId == userParam && it.projectId == projectId && it.skillId == skillId })
            if (points) {
                assert points.size() == 1
            }
            return points ? points[0].points : 0
        }

        when:
        def skillPts_before = getPoints(user, proj2.projectId, skill2.skillId)
        def subjPts_before = getPoints(user, proj2.projectId, p2_subj.skillId)
        def projPts_before = getPoints(user, proj2.projectId, null)

        skillCatalogFinalizationService.applyEventsThatWereReportedDuringTheFinalizationRun([skillDef.id], start, end)

        then:
        skillPts_before == 10 * 4
        subjPts_before == 10 * 4
        projPts_before == 10 * 4

        getPoints(user, proj2.projectId, skill2.skillId) == 10 * 6
        getPoints(user, proj2.projectId, p2_subj.skillId) == 10 * 6
        getPoints(user, proj2.projectId, null) == 10 * 6

        !userAchievedRepo.findAll()
    }

    def "post migration event propagation must be able to handle achievements"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skill1 = SkillsFactory.createSkill(1, 1, 1, 1, 6)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 1, 6)
        def skill3 = SkillsFactory.createSkill(1, 1, 3, 1, 6)
        skill2.pointIncrementInterval = 0
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [skill1, skill2, skill3])

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [])

        skillsService.exportSkillToCatalog(proj1.projectId, skill1.skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(proj1.projectId, skill3.skillId)
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj2.projectId, p2_subj.subjectId, [
                [projectId: proj1.projectId, skillId: skill1.skillId],
                [projectId: proj1.projectId, skillId: skill2.skillId],
        ])
        String user = getRandomUsers(1)[0]

        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], user)
        Thread.sleep(100)

        long start = System.currentTimeMillis()
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], user)
        Thread.sleep(100)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], user)
        long end = System.currentTimeMillis()

        Thread.sleep(100)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], user)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillId(proj2.projectId, skill2.skillId)
        assert skillDef

        Closure<Integer> getPoints = { String userParam, String projectId, String skillId ->
            List<UserPoints> points = userPointsRepo.findAll().findAll({it.userId == userParam && it.projectId == projectId && it.skillId == skillId })
            if (points) {
                assert points.size() == 1
            }
            return points ? points[0].points : 0
        }

        when:
        def skillPts_before = getPoints(user, proj2.projectId, skill2.skillId)
        def subjPts_before = getPoints(user, proj2.projectId, p2_subj.skillId)
        def projPts_before = getPoints(user, proj2.projectId, null)

        // simulate skill2 achievement in proj1
        List<UserPerformedSkill> performedSkills = userPerformedSkillRepo.findAll().sort({ it.performedOn })
        UserAchievement skillAchieved = new UserAchievement(userId: user, projectId: proj1.projectId, skillId: skill2.skillId,
                skillRefId: skillDefRepo.findByProjectIdAndSkillId(proj1.projectId, skill2.skillId).id, pointsWhenAchieved: 60, achievedOn: performedSkills[performedSkills.size() - 2].performedOn)
        userAchievedRepo.save(skillAchieved)

        List<UserAchievement> achievementsBefore = userAchievedRepo.findAll()

        skillCatalogFinalizationService.applyEventsThatWereReportedDuringTheFinalizationRun([skillDef.id], start, end)

        List<UserAchievement> achievementsAfter = userAchievedRepo.findAll()

        then:
        skillPts_before == 10 * 4
        subjPts_before == 10 * 4
        projPts_before == 10 * 4
        !achievementsBefore.findAll( { it.skillId == skill2.skillId && it.projectId == proj2.projectId})
        achievementsBefore.findAll( { it.skillId == p2_subj.subjectId && it.projectId == proj2.projectId}).level.sort() == [1, 2]
        achievementsBefore.findAll( { !it.skillId && it.projectId == proj2.projectId}).level.sort() == [1, 2]

        getPoints(user, proj2.projectId, skill2.skillId) == 10 * 6
        getPoints(user, proj2.projectId, p2_subj.skillId) == 10 * 6
        getPoints(user, proj2.projectId, null) == 10 * 6
        achievementsAfter.findAll( { it.skillId == skill2.skillId && it.projectId == proj2.projectId}).pointsWhenAchieved == [60]
        achievementsAfter.findAll( { it.skillId == p2_subj.subjectId && it.projectId == proj2.projectId}).level.sort() == [1, 2, 3]
        achievementsAfter.findAll( { !it.skillId && it.projectId == proj2.projectId}).level.sort() == [1, 2, 3]
    }
}
