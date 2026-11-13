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

import groovy.json.JsonOutput
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsFactory
import skills.services.admin.BatchOperationsTransactionalAccessor
import skills.storage.model.UserAchievement
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo

import static skills.intTests.utils.SkillsFactory.*

class CatalogAchievementFinalizationSpecs extends CatalogAchievementSpecBase {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    BatchOperationsTransactionalAccessor batchOperationsTransactionalAccessor

    def "finalization sql statements can be executed multiple times without causing issues"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p2subj1 = createSubject(2, 1)
        def p3subj1 = createSubject(3, 1)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p3subj1)

        // project 1
        def skill = createSkill(1, 1, 1, 0, 5, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 5, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 5, 0, 250)
        skillsService.createSkill(skill)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.exportSkillToCatalog(project1.projectId, skill.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill2.skillId)
        skillsService.exportSkillToCatalog(project1.projectId, skill3.skillId)

        // project 2
        def skill4 = createSkill(2, 1, 4, 0, 5, 0, 250)
        def skill5 = createSkill(2, 1, 5, 0, 5, 0, 250)
        def skill6 = createSkill(2, 1, 6, 0, 5, 0, 250)
        skillsService.createSkill(skill4)
        skillsService.createSkill(skill5)
        skillsService.createSkill(skill6)
        skillsService.exportSkillToCatalog(project2.projectId, skill4.skillId)
        skillsService.exportSkillToCatalog(project2.projectId, skill5.skillId)
        skillsService.exportSkillToCatalog(project2.projectId, skill6.skillId)

        // project 3
        def skill7 = createSkill(3, 1, 7, 0, 5, 0, 1500)
        def skill8 = createSkill(3, 1, 8, 0, 5, 0, 250)
        def skill9 = createSkill(3, 1, 9, 0, 5, 0, 250)
        skillsService.createSkill(skill7)
        skillsService.createSkill(skill8)
        skillsService.createSkill(skill9)

        skillsService.exportSkillToCatalog(project3.projectId, skill7.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, skill8.skillId)
        skillsService.exportSkillToCatalog(project3.projectId, skill9.skillId)

        def randomUsers = getRandomUsers(3)
        def user = randomUsers[0]
        def user2 = randomUsers[1]
        def user3 = randomUsers[2]

        // user 1
        skillsService.addSkill([projectId: project2.projectId, skillId: skill4.skillId], user)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill5.skillId], user)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill6.skillId], user)

        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)
        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user)

        5.times {
            skillsService.addSkill([projectId: project3.projectId, skillId: skill7.skillId], user)
            skillsService.addSkill([projectId: project3.projectId, skillId: skill8.skillId], user)
            skillsService.addSkill([projectId: project3.projectId, skillId: skill9.skillId], user)
        }

        // user 2
        skillsService.addSkill([projectId: project2.projectId, skillId: skill4.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill5.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill6.skillId], user2)

        // user 3
        4.times {
            skillsService.addSkill([projectId: project2.projectId, skillId: skill4.skillId], user3)
        }
        5.times {
            skillsService.addSkill([projectId: project2.projectId, skillId: skill6.skillId], user3)
        }
        skillsService.addSkill([projectId: project3.projectId, skillId: skill9.skillId], user3)
        4.times {
            skillsService.addSkill([projectId: project3.projectId, skillId: skill7.skillId], user3)
        }
        skillsService.addSkill([projectId: project1.projectId, skillId: skill.skillId], user3)
        skillsService.addSkill([projectId: project1.projectId, skillId: skill2.skillId], user3)
        skillsService.addSkill([projectId: project1.projectId, skillId: skill2.skillId], user3)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Integer proj2_subj1_ref_id = skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p1subj1.subjectId).id
        when:
        Integer proj2_user1Level_import0 = skillsService.getUserLevel(project2.projectId, user)
        Integer proj2_user2Level_import0 = skillsService.getUserLevel(project2.projectId, user2)
        def proj2_user1Stats_import0 = skillsService.getUserStats(project2.projectId, user)
        List<UserAchievement> proj2_user1Achievements_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        List<UserAchievement> proj2_user1Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        Integer proj2_user3Level_import0 = skillsService.getUserLevel(project2.projectId, user3)
        def proj2_user3Stats_import0 = skillsService.getUserStats(project2.projectId, user3)
        List<UserAchievement> proj2_user3Achievements_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == null }
        List<UserAchievement> proj2_user3Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id }

        printLevels(project2.projectId, "before import")
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, [[projectId: project1.projectId, skillId: skill.skillId], [projectId: project3.projectId, skillId: skill7.skillId]])
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)

        batchOperationsTransactionalAccessor.updateSubjectTotalPoints(project2.projectId, p2subj1.subjectId)
        batchOperationsTransactionalAccessor.updateProjectsTotalPoints(project2.projectId)
        List<Integer> skillRefIds = [[projectId: project1.projectId, skillId: skill.skillId], [projectId: project3.projectId, skillId: skill7.skillId]].collect({ skillDefRepo.findByProjectIdAndSkillId(it.projectId, it.skillId).id })
        batchOperationsTransactionalAccessor.copySkillUserPointsToTheImportedProjects(project2.projectId, skillRefIds)
        batchOperationsTransactionalAccessor.copySkillAchievementsToTheImportedProjects(project2.projectId, skillRefIds)

        batchOperationsTransactionalAccessor.createSubjectUserPointsForTheNewUsers(project2.projectId, p2subj1.subjectId)
        batchOperationsTransactionalAccessor.updateUserPointsForSubject(project2.projectId, p2subj1.subjectId)
        batchOperationsTransactionalAccessor.identifyAndAddSubjectLevelAchievements(project2.projectId, p2subj1.subjectId)

        batchOperationsTransactionalAccessor.createProjectUserPointsForTheNewUsers(project2.projectId)
        batchOperationsTransactionalAccessor.updateUserPointsForProject(project2.projectId)
        batchOperationsTransactionalAccessor.identifyAndAddProjectLevelAchievements(project2.projectId, false)

        printLevels(project2.projectId, "after import2 (2 skills)")

        Integer proj2_user1Level_import2 = skillsService.getUserLevel(project2.projectId, user)
        Integer proj2_user2Level_import2 = skillsService.getUserLevel(project2.projectId, user2)
        def proj2_user1Stats_import2 = skillsService.getUserStats(project2.projectId, user)
        List<UserAchievement> proj2_user1Achievements_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == null }
        List<UserAchievement> proj2_user1Achievements_subj1_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id }

        Integer proj2_user3Level_import2 = skillsService.getUserLevel(project2.projectId, user3)
        def proj2_user3Stats_import2 = skillsService.getUserStats(project2.projectId, user3)
        List<UserAchievement> proj2_user3Achievements_import2 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        List<UserAchievement> proj2_user3Achievements_subj1_import2 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        then:
        proj2_user1Level_import0 == 1
        proj2_user2Level_import0 == 1
        proj2_user1Stats_import0.numSkills == 3
        proj2_user1Stats_import0.userTotalPoints == 750
        proj2_user1Achievements_import0.collect { it.level }.sort() == [1]
        proj2_user1Achievements_subj1_import0.collect { it.level }.sort() == [1]
        proj2_user3Level_import0 == 3
        proj2_user3Stats_import0.numSkills == 2
        proj2_user3Stats_import0.userTotalPoints == 2250
        proj2_user3Achievements_import0.collect { it.level }.sort() == [1, 2, 3]
        proj2_user3Achievements_subj1_import0.collect { it.level }.sort() == [1, 2, 3]

        proj2_user1Level_import2 == 4
        proj2_user2Level_import2 == 1
        proj2_user1Stats_import2.numSkills == 5
        proj2_user1Stats_import2.userTotalPoints == 8750
        proj2_user1Achievements_import2.collect { it.level }.sort() == [1, 2, 3, 4]
        proj2_user1Achievements_subj1_import2.collect { it.level }.sort() == [1, 2, 3, 4]
        proj2_user3Level_import2 == 4
        proj2_user3Stats_import2.numSkills == 4
        proj2_user3Stats_import2.userTotalPoints == 8500
        proj2_user3Achievements_import2.collect { it.level }.sort() == [1, 2, 3, 4]
        proj2_user3Achievements_subj1_import2.collect { it.level }.sort() == [1, 2, 3, 4]
    }


    def "finalization - level achievement dates are assigned to when the level was actually achieved"() {
        def project1 = createProjWithCatalogSkills(1, 1)
        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2, 1)
        def p2_subj2 = SkillsFactory.createSubject(2, 2)
        def p2_subj3 = SkillsFactory.createSubject(2, 3)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [])
        skillsService.createSubject(p2_subj2)
        skillsService.createSubject(p2_subj3)


        List<String> users = getRandomUsers(3)
        participateInProject(proj2.projectId, users.take(2))
        List<Date> dates = (1..12).collect { new Date() - it }.reverse()
        skillsService.addSkill(project1.s1_skills[0], users[0], dates[0])
        Thread.sleep(10)
        skillsService.addSkill(project1.s1_skills[1], users[0], dates[1])
        Thread.sleep(10)
        skillsService.addSkill(project1.s1_skills[2], users[0], dates[2])
        Thread.sleep(10)
        skillsService.addSkill(project1.s2_skills[0], users[0], dates[3])
        Thread.sleep(10)
        skillsService.addSkill(project1.s2_skills[1], users[0], dates[4])
        Thread.sleep(10)
        skillsService.addSkill(project1.s2_skills[2], users[0], dates[5])

        Thread.sleep(10)
        skillsService.addSkill(project1.s1_skills[0], users[1], dates[0])
        Thread.sleep(10)
        skillsService.addSkill(project1.s1_skills[1], users[1], dates[1])
        Thread.sleep(10)
        skillsService.addSkill(project1.s1_skills[2], users[1], dates[2])
        Thread.sleep(10)
        skillsService.addSkill(project1.s2_skills[0], users[1], dates[3])
        Thread.sleep(10)
        skillsService.addSkill(project1.s2_skills[1], users[1], dates[4])
        Thread.sleep(10)
        skillsService.addSkill(project1.s2_skills[2], users[1], dates[5])
        Thread.sleep(10)
        skillsService.addSkill(project1.s3_skills[0], users[1], dates[6])
        Thread.sleep(10)
        skillsService.addSkill(project1.s3_skills[1], users[1], dates[7])
        Thread.sleep(10)
        skillsService.addSkill(project1.s3_skills[2], users[1], dates[8])


        List<UserAchievement> userAchievements_proj1 = userAchievedRepo.findAll().findAll {
            it.skillId == null && it.level != null && it.userId == users[0] && it.projectId == project1.p.projectId }
        List<UserAchievement> userAchievements_proj1_user1 = userAchievedRepo.findAll().findAll {
            it.skillId == null && it.level != null && it.userId == users[1] && it.projectId == project1.p.projectId }
        List<UserAchievement> userAchievements_proj1_subj1 = userAchievedRepo.findAll().findAll {
            it.skillId == project1.s1.subjectId && it.level != null && it.userId == users[0] && it.projectId == project1.p.projectId }
        List<UserAchievement> userAchievements_proj1_subj2 = userAchievedRepo.findAll().findAll {
            it.skillId == project1.s2.subjectId && it.level != null && it.userId == users[0] && it.projectId == project1.p.projectId }
        List<UserAchievement> userAchievements_proj1_subj3 = userAchievedRepo.findAll().findAll {
            it.skillId == project1.s3.subjectId && it.level != null && it.userId == users[0] && it.projectId == project1.p.projectId }
        List<UserAchievement> userAchievements_proj1_user1_subj1 = userAchievedRepo.findAll().findAll {
            it.skillId == project1.s1.subjectId && it.level != null && it.userId == users[1] && it.projectId == project1.p.projectId }
        List<UserAchievement> userAchievements_proj1_user1_subj2 = userAchievedRepo.findAll().findAll {
            it.skillId == project1.s2.subjectId && it.level != null && it.userId == users[1] && it.projectId == project1.p.projectId }
        List<UserAchievement> userAchievements_proj1_user1_subj3 = userAchievedRepo.findAll().findAll {
            it.skillId == project1.s3.subjectId && it.level != null && it.userId == users[1] && it.projectId == project1.p.projectId }
//        println JsonOutput.prettyPrint(JsonOutput.toJson(userAchievements_proj1_subj3))

        when:
        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, p2_subj.subjectId,
                project1.s1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, p2_subj2.subjectId,
                project1.s2_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, p2_subj3.subjectId,
                project1.s3_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId)

        List<UserAchievement> userAchievements_proj2 = userAchievedRepo.findAll().findAll {
            it.skillId == null && it.level != null && it.userId == users[0] && it.projectId == proj2.projectId }
        List<UserAchievement> userAchievements_proj2_user1 = userAchievedRepo.findAll().findAll {
            it.skillId == null && it.level != null && it.userId == users[1] && it.projectId == proj2.projectId }
        List<UserAchievement> userAchievements_proj2_subj1 = userAchievedRepo.findAll().findAll {
            it.skillId == p2_subj.subjectId && it.level != null && it.userId == users[0] && it.projectId == proj2.projectId }
        List<UserAchievement> userAchievements_proj2_subj2 = userAchievedRepo.findAll().findAll {
            it.skillId == p2_subj2.subjectId && it.level != null && it.userId == users[0] && it.projectId == proj2.projectId }
        List<UserAchievement> userAchievements_proj2_subj3 = userAchievedRepo.findAll().findAll {
            it.skillId == p2_subj3.subjectId && it.level != null && it.userId == users[0] && it.projectId == proj2.projectId }
        List<UserAchievement> userAchievements_proj2_user1_subj1 = userAchievedRepo.findAll().findAll {
            it.skillId == p2_subj.subjectId && it.level != null && it.userId == users[1] && it.projectId == proj2.projectId }
        List<UserAchievement> userAchievements_proj2_user1_subj2 = userAchievedRepo.findAll().findAll {
            it.skillId == p2_subj2.subjectId && it.level != null && it.userId == users[1] && it.projectId == proj2.projectId }
        List<UserAchievement> userAchievements_proj2_user1_subj3 = userAchievedRepo.findAll().findAll {
            it.skillId == p2_subj3.subjectId && it.level != null && it.userId == users[1] && it.projectId == proj2.projectId }
//        println JsonOutput.prettyPrint(JsonOutput.toJson(userAchievements_proj2))

        then:
        userAchievements_proj2.size() == userAchievements_proj1.size()
        userAchievements_proj2.size() == 3
        userAchievements_proj2.find { it.level == 1 }.achievedOn == userAchievements_proj1.find { it.level == 1 }.achievedOn
        userAchievements_proj2.find { it.level == 1 }.pointsWhenAchieved == userAchievements_proj1.find { it.level == 1 }.pointsWhenAchieved
        userAchievements_proj2.find { it.level == 2 }.achievedOn == userAchievements_proj1.find { it.level == 2 }.achievedOn
        userAchievements_proj2.find { it.level == 2 }.pointsWhenAchieved == userAchievements_proj1.find { it.level == 2 }.pointsWhenAchieved
        userAchievements_proj2.find { it.level == 3 }.achievedOn == userAchievements_proj1.find { it.level == 3 }.achievedOn
        userAchievements_proj2.find { it.level == 3 }.pointsWhenAchieved == userAchievements_proj1.find { it.level == 3 }.pointsWhenAchieved

        userAchievements_proj2_user1.size() == userAchievements_proj1_user1.size()
        userAchievements_proj2_user1.size() == 5
        userAchievements_proj2_user1.find { it.level == 1 }.achievedOn == userAchievements_proj1_user1.find { it.level == 1 }.achievedOn
        userAchievements_proj2_user1.find { it.level == 1 }.pointsWhenAchieved == userAchievements_proj1_user1.find { it.level == 1 }.pointsWhenAchieved
        userAchievements_proj2_user1.find { it.level == 2 }.achievedOn == userAchievements_proj1_user1.find { it.level == 2 }.achievedOn
        userAchievements_proj2_user1.find { it.level == 2 }.pointsWhenAchieved == userAchievements_proj1_user1.find { it.level == 2 }.pointsWhenAchieved
        userAchievements_proj2_user1.find { it.level == 3 }.achievedOn == userAchievements_proj1_user1.find { it.level == 3 }.achievedOn
        userAchievements_proj2_user1.find { it.level == 3 }.pointsWhenAchieved == userAchievements_proj1_user1.find { it.level == 3 }.pointsWhenAchieved
        userAchievements_proj2_user1.find { it.level == 4 }.achievedOn == userAchievements_proj1_user1.find { it.level == 4 }.achievedOn
        userAchievements_proj2_user1.find { it.level == 4 }.pointsWhenAchieved == userAchievements_proj1_user1.find { it.level == 4 }.pointsWhenAchieved
        userAchievements_proj2_user1.find { it.level == 5 }.achievedOn == userAchievements_proj1_user1.find { it.level == 5 }.achievedOn
        userAchievements_proj2_user1.find { it.level == 5 }.pointsWhenAchieved == userAchievements_proj1_user1.find { it.level == 5 }.pointsWhenAchieved

        userAchievements_proj1_subj1.size() == userAchievements_proj2_subj1.size()
        userAchievements_proj1_subj1.size() == 5
        userAchievements_proj1_subj1.find { it.level == 1 }.achievedOn == userAchievements_proj2_subj1.find { it.level == 1 }.achievedOn
        userAchievements_proj1_subj1.find { it.level == 1 }.pointsWhenAchieved == userAchievements_proj2_subj1.find { it.level == 1 }.pointsWhenAchieved
        userAchievements_proj1_subj1.find { it.level == 2 }.achievedOn == userAchievements_proj2_subj1.find { it.level == 2 }.achievedOn
        userAchievements_proj1_subj1.find { it.level == 2 }.pointsWhenAchieved == userAchievements_proj2_subj1.find { it.level == 2 }.pointsWhenAchieved
        userAchievements_proj1_subj1.find { it.level == 3 }.achievedOn == userAchievements_proj2_subj1.find { it.level == 3 }.achievedOn
        userAchievements_proj1_subj1.find { it.level == 3 }.pointsWhenAchieved == userAchievements_proj2_subj1.find { it.level == 3 }.pointsWhenAchieved
        userAchievements_proj1_subj1.find { it.level == 4 }.achievedOn == userAchievements_proj2_subj1.find { it.level == 4 }.achievedOn
        userAchievements_proj1_subj1.find { it.level == 4 }.pointsWhenAchieved == userAchievements_proj2_subj1.find { it.level == 4 }.pointsWhenAchieved
        userAchievements_proj1_subj1.find { it.level == 5 }.achievedOn == userAchievements_proj2_subj1.find { it.level == 5 }.achievedOn
        userAchievements_proj1_subj1.find { it.level == 5 }.pointsWhenAchieved == userAchievements_proj2_subj1.find { it.level == 5 }.pointsWhenAchieved

        userAchievements_proj1_subj2.size() == userAchievements_proj2_subj2.size()
        userAchievements_proj1_subj2.size() == 5
        userAchievements_proj1_subj2.find { it.level == 1 }.achievedOn == userAchievements_proj2_subj2.find { it.level == 1 }.achievedOn
        userAchievements_proj1_subj2.find { it.level == 1 }.pointsWhenAchieved == userAchievements_proj2_subj2.find { it.level == 1 }.pointsWhenAchieved
        userAchievements_proj1_subj2.find { it.level == 2 }.achievedOn == userAchievements_proj2_subj2.find { it.level == 2 }.achievedOn
        userAchievements_proj1_subj2.find { it.level == 2 }.pointsWhenAchieved == userAchievements_proj2_subj2.find { it.level == 2 }.pointsWhenAchieved
        userAchievements_proj1_subj2.find { it.level == 3 }.achievedOn == userAchievements_proj2_subj2.find { it.level == 3 }.achievedOn
        userAchievements_proj1_subj2.find { it.level == 3 }.pointsWhenAchieved == userAchievements_proj2_subj2.find { it.level == 3 }.pointsWhenAchieved
        userAchievements_proj1_subj2.find { it.level == 4 }.achievedOn == userAchievements_proj2_subj2.find { it.level == 4 }.achievedOn
        userAchievements_proj1_subj2.find { it.level == 4 }.pointsWhenAchieved == userAchievements_proj2_subj2.find { it.level == 4 }.pointsWhenAchieved
        userAchievements_proj1_subj2.find { it.level == 5 }.achievedOn == userAchievements_proj2_subj2.find { it.level == 5 }.achievedOn
        userAchievements_proj1_subj2.find { it.level == 5 }.pointsWhenAchieved == userAchievements_proj2_subj2.find { it.level == 5 }.pointsWhenAchieved

        !userAchievements_proj1_subj3
        !userAchievements_proj2_subj3

        userAchievements_proj2_user1_subj1.size() == userAchievements_proj1_user1_subj1.size()
        userAchievements_proj2_user1_subj1.size() == 5
        userAchievements_proj2_user1_subj1.find { it.level == 1 }.achievedOn == userAchievements_proj1_user1_subj1.find { it.level == 1 }.achievedOn
        userAchievements_proj2_user1_subj1.find { it.level == 1 }.pointsWhenAchieved == userAchievements_proj1_user1_subj1.find { it.level == 1 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj1.find { it.level == 2 }.achievedOn == userAchievements_proj1_user1_subj1.find { it.level == 2 }.achievedOn
        userAchievements_proj2_user1_subj1.find { it.level == 2 }.pointsWhenAchieved == userAchievements_proj1_user1_subj1.find { it.level == 2 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj1.find { it.level == 3 }.achievedOn == userAchievements_proj1_user1_subj1.find { it.level == 3 }.achievedOn
        userAchievements_proj2_user1_subj1.find { it.level == 3 }.pointsWhenAchieved == userAchievements_proj1_user1_subj1.find { it.level == 3 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj1.find { it.level == 4 }.achievedOn == userAchievements_proj1_user1_subj1.find { it.level == 4 }.achievedOn
        userAchievements_proj2_user1_subj1.find { it.level == 4 }.pointsWhenAchieved == userAchievements_proj1_user1_subj1.find { it.level == 4 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj1.find { it.level == 5 }.achievedOn == userAchievements_proj1_user1_subj1.find { it.level == 5 }.achievedOn
        userAchievements_proj2_user1_subj1.find { it.level == 5 }.pointsWhenAchieved == userAchievements_proj1_user1_subj1.find { it.level == 5 }.pointsWhenAchieved

        userAchievements_proj2_user1_subj2.size() == 5
        userAchievements_proj2_user1_subj2.find { it.level == 1 }.achievedOn == userAchievements_proj1_user1_subj2.find { it.level == 1 }.achievedOn
        userAchievements_proj2_user1_subj2.find { it.level == 1 }.pointsWhenAchieved == userAchievements_proj1_user1_subj2.find { it.level == 1 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj2.find { it.level == 2 }.achievedOn == userAchievements_proj1_user1_subj2.find { it.level == 2 }.achievedOn
        userAchievements_proj2_user1_subj2.find { it.level == 2 }.pointsWhenAchieved == userAchievements_proj1_user1_subj2.find { it.level == 2 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj2.find { it.level == 3 }.achievedOn == userAchievements_proj1_user1_subj2.find { it.level == 3 }.achievedOn
        userAchievements_proj2_user1_subj2.find { it.level == 3 }.pointsWhenAchieved == userAchievements_proj1_user1_subj2.find { it.level == 3 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj2.find { it.level == 4 }.achievedOn == userAchievements_proj1_user1_subj2.find { it.level == 4 }.achievedOn
        userAchievements_proj2_user1_subj2.find { it.level == 4 }.pointsWhenAchieved == userAchievements_proj1_user1_subj2.find { it.level == 4 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj2.find { it.level == 5 }.achievedOn == userAchievements_proj1_user1_subj2.find { it.level == 5 }.achievedOn
        userAchievements_proj2_user1_subj2.find { it.level == 5 }.pointsWhenAchieved == userAchievements_proj1_user1_subj2.find { it.level == 5 }.pointsWhenAchieved

        userAchievements_proj2_user1_subj3.size() == 5
        userAchievements_proj2_user1_subj3.find { it.level == 1 }.achievedOn == userAchievements_proj1_user1_subj3.find { it.level == 1 }.achievedOn
        userAchievements_proj2_user1_subj3.find { it.level == 1 }.pointsWhenAchieved == userAchievements_proj1_user1_subj3.find { it.level == 1 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj3.find { it.level == 2 }.achievedOn == userAchievements_proj1_user1_subj3.find { it.level == 2 }.achievedOn
        userAchievements_proj2_user1_subj3.find { it.level == 2 }.pointsWhenAchieved == userAchievements_proj1_user1_subj3.find { it.level == 2 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj3.find { it.level == 3 }.achievedOn == userAchievements_proj1_user1_subj3.find { it.level == 3 }.achievedOn
        userAchievements_proj2_user1_subj3.find { it.level == 3 }.pointsWhenAchieved == userAchievements_proj1_user1_subj3.find { it.level == 3 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj3.find { it.level == 4 }.achievedOn == userAchievements_proj1_user1_subj3.find { it.level == 4 }.achievedOn
        userAchievements_proj2_user1_subj3.find { it.level == 4 }.pointsWhenAchieved == userAchievements_proj1_user1_subj3.find { it.level == 4 }.pointsWhenAchieved
        userAchievements_proj2_user1_subj3.find { it.level == 5 }.achievedOn == userAchievements_proj1_user1_subj3.find { it.level == 5 }.achievedOn
        userAchievements_proj2_user1_subj3.find { it.level == 5 }.pointsWhenAchieved == userAchievements_proj1_user1_subj3.find { it.level == 5 }.pointsWhenAchieved
    }


    def "changes to skill points before finalization causes user's point history to be updated"(){
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 1)
        skill2.pointIncrement = 150
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [skill2])

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2)
        def p2_skill1 = SkillsFactory.createSkill(2, 1, 1, 0, 1, 0)
        p2_skill1.pointIncrement = 150
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [p2_skill1])

        String user = getRandomUsers(1)[0]
        participateInProject(proj1.projectId, [user])

        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user, new DateTime().minusDays(1).toDate())

        skillsService.exportSkillToCatalog(proj2.projectId, p2_skill1.skillId)
        skillsService.bulkImportSkillsFromCatalog(proj1.projectId, subj.subjectId, [
                [projectId: proj2.projectId, skillId: p2_skill1.skillId],
        ])

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        skillsService.updateImportedSkill(proj1.projectId, p2_skill1.skillId, 5)
        skillsService.finalizeSkillsImportFromCatalog(proj1.projectId, true);

        when:
        def skillSummaryAfterEdit = skillsService.getSkillSummary(user, proj1.projectId, subj.subjectId)
        def userInfoAfterUpdate = skillsService.getUserStats(proj1.projectId, user)

        then:
        userInfoAfterUpdate.userTotalPoints == 5
        skillSummaryAfterEdit.points == 5
        skillSummaryAfterEdit.todaysPoints == 5
    }


    def "changes to skill in original project before finalization doesn't break"(){
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 1)
        skill2.pointIncrement = 150
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, null)

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2)
        def p2_skill1 = SkillsFactory.createSkill(2, 1, 1, 0, 1, 0)
        p2_skill1.pointIncrement = 150
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [p2_skill1])

        skillsService.exportSkillToCatalog(proj2.projectId, p2_skill1.skillId)

        def status = skillsService.getCatalogFinalizeInfo(proj1.projectId)
        assert status.numSkillsToFinalize == 0

        skillsService.bulkImportSkillsFromCatalog(proj1.projectId, subj.subjectId, [
                [projectId: proj2.projectId, skillId: p2_skill1.skillId],
        ])

        status = skillsService.getCatalogFinalizeInfo(proj1.projectId)
        assert status.numSkillsToFinalize == 1

        p2_skill1.pointIncrement = 250
        skillsService.updateSkill(p2_skill1, p2_skill1.skillId);
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        status = skillsService.getCatalogFinalizeInfo(proj1.projectId)

        then:
        status.numSkillsToFinalize == 1
    }


    def "changes to skill in original project before finalization doesn't break and doesn't disable other imported skills"(){
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skill2 = SkillsFactory.createSkill(1, 1, 2, 0, 1)
        skill2.pointIncrement = 150
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, null)

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2)
        def p2_skills = (1..3).collect {createSkill(2, 1, it, 0, 5, 0, 150) }

        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, p2_skills)

        skillsService.exportSkillToCatalog(proj2.projectId, p2_skills[0].skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, p2_skills[1].skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, p2_skills[2].skillId)

        def status = skillsService.getCatalogFinalizeInfo(proj1.projectId)
        assert status.numSkillsToFinalize == 0

        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj1.projectId, subj.subjectId, [
                [projectId: proj2.projectId, skillId: p2_skills[0].skillId],
        ])

        status = skillsService.getCatalogFinalizeInfo(proj1.projectId)
        assert status.numSkillsToFinalize == 0

        skillsService.bulkImportSkillsFromCatalog(proj1.projectId, subj.subjectId, [
                [projectId: proj2.projectId, skillId: p2_skills[1].skillId],
                [projectId: proj2.projectId, skillId: p2_skills[2].skillId],
        ])

        status = skillsService.getCatalogFinalizeInfo(proj1.projectId)
        assert status.numSkillsToFinalize == 2

        p2_skills[0].pointIncrement = 250
        skillsService.updateSkill(p2_skills[0], p2_skills[0].skillId);
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        status = skillsService.getCatalogFinalizeInfo(proj1.projectId)
        def skills = skillsService.getSkillsForSubject(proj1.projectId, subj.subjectId)

        then:
        status.numSkillsToFinalize == 2
        skills[0].enabled
        skills[0].copiedFromProjectId == proj2.projectId
    }

}
