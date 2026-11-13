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

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo

import static skills.intTests.utils.SkillsFactory.*

class CatalogImportTimeAchievementsSpecs extends CatalogAchievementSpecBase {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    UserAchievedLevelRepo achievedRepo

    def "copy user-points when importing a skill"() {
        def proj1 = createProjWithCatalogSkills(1)
        def proj2 = createProjWithCatalogSkills(2)
        def proj3 = createProjWithCatalogSkills(3)

        def users = getRandomUsers(15)
        participateInProject(proj3.p.projectId, users.take(3))
        List<Date> dates = (1..10).collect { new Date() - it }
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[0].skillId], users[0], dates[0])
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[0].skillId], users[0], dates[1])
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[1].skillId], users[0], dates[1])
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[1].skillId], users[0], dates[2])
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[2].skillId], users[0], dates[2])
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[2].skillId], users[0], dates[3])
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[0].skillId], users[0], dates[3])
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[0].skillId], users[0], dates[3])
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[1].skillId], users[0], dates[4])
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[1].skillId], users[0], dates[4])
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[2].skillId], users[0], dates[4])
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[2].skillId], users[0], dates[4])

        dates.each {
            skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[1].skillId], users[1], dates[1])
            skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[1].skillId], users[2], dates[2])
            skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[2].skillId], users[3], dates[2])
            skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[2].skillId], users[4], dates[3])
            skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[0].skillId], users[5], dates[3])
            skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[0].skillId], users[6], dates[3])
            skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[1].skillId], users[7], dates[4])
            skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[1].skillId], users[8], dates[4])
            skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[2].skillId], users[9], dates[4])
            skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[2].skillId], users[10], dates[4])
        }

        when:
        List<UserPoints> userPoints_import0 = userPointsRepo.findAll().findAll( { it.getProjectId() == proj3.p.projectId})
        skillsService.bulkImportSkillsFromCatalog(proj3.p.projectId, proj3.s1.subjectId, [[projectId: proj1.p.projectId, skillId: proj1.s1_skills[0].skillId], [projectId: proj1.p.projectId, skillId: proj1.s1_skills[1].skillId]])
        skillsService.finalizeSkillsImportFromCatalog(proj3.p.projectId)

        List<UserPoints> userPoints_import1 = userPointsRepo.findAll().findAll( { it.getProjectId() == proj3.p.projectId})
        then:
        !userPoints_import0
        userPoints_import1
        List<UserPoints> user0 = userPoints_import1.findAll( { it.userId == users[0] })
        user0.size() == 4
        user0.findAll { it.skillId == proj1.s1_skills[0].skillId }.collect { it.points } == [200]
        user0.findAll { it.skillId == proj1.s1.subjectId }.collect { it.points } == [400]
        user0.findAll { !it.skillId }.collect { it.points } == [400]
        user0.findAll { it.skillId == proj1.s1_skills[1].skillId }.collect { it.points } == [200]
    }


    def "importing a skill causes exciting project users to level up"() {
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
        List<UserAchievement> proj2_user3Achievements_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        List<UserAchievement> proj2_user3Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}


        printLevels(project2.projectId, "before import")
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, [[ projectId: project1.projectId, skillId: skill.skillId]])
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        printLevels(project2.projectId, "after import1")

        Integer proj2_user1Level_import1 = skillsService.getUserLevel(project2.projectId, user)
        Integer proj2_user2Level_import1 = skillsService.getUserLevel(project2.projectId, user2)
        def proj2_user1Stats_import1 = skillsService.getUserStats(project2.projectId, user)
        List<UserAchievement> proj2_user1Achievements_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        List<UserAchievement> proj2_user1Achievements_subj1_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        Integer proj2_user3Level_import1 = skillsService.getUserLevel(project2.projectId, user3)
        def proj2_user3Stats_import1 = skillsService.getUserStats(project2.projectId, user3)
        List<UserAchievement> proj2_user3Achievements_import1 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        List<UserAchievement> proj2_user3Achievements_subj1_import1 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId,  [[ projectId: project3.projectId, skillId: skill7.skillId]])
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        printLevels(project2.projectId, "after import2")

        Integer proj2_user1Level_import2 = skillsService.getUserLevel(project2.projectId, user)
        Integer proj2_user2Level_import2 = skillsService.getUserLevel(project2.projectId, user2)
        def proj2_user1Stats_import2 = skillsService.getUserStats(project2.projectId, user)
        List<UserAchievement> proj2_user1Achievements_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        List<UserAchievement> proj2_user1Achievements_subj1_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

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

        proj2_user1Stats_import1.numSkills == 4
        proj2_user1Stats_import1.userTotalPoints == 1250
        proj2_user2Level_import1 == 1
        proj2_user1Level_import1 == 2
        proj2_user1Achievements_import1.collect { it.level }.sort() == [1, 2]
        proj2_user1Achievements_subj1_import1.collect { it.level }.sort() == [1, 2]
        proj2_user3Level_import1 == 3
        proj2_user3Stats_import1.numSkills == 3
        proj2_user3Stats_import1.userTotalPoints == 2500
        proj2_user3Achievements_import1.collect { it.level }.sort() == [1, 2, 3]
        proj2_user3Achievements_subj1_import1.collect { it.level }.sort() == [1, 2, 3]

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


    def "importing a skill causes exciting project users to level up - bulk import"() {
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
        List<UserAchievement> proj2_user3Achievements_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        List<UserAchievement> proj2_user3Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        printLevels(project2.projectId, "before import")
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, [[ projectId: project1.projectId, skillId: skill.skillId], [projectId: project3.projectId, skillId: skill7.skillId]])
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        printLevels(project2.projectId, "after import2 (2 skills)")

        Integer proj2_user1Level_import2 = skillsService.getUserLevel(project2.projectId, user)
        Integer proj2_user2Level_import2 = skillsService.getUserLevel(project2.projectId, user2)
        def proj2_user1Stats_import2 = skillsService.getUserStats(project2.projectId, user)
        List<UserAchievement> proj2_user1Achievements_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        List<UserAchievement> proj2_user1Achievements_subj1_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

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


    def "importing a skill causes exciting users to level up within the subject"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p2subj1 = createSubject(2, 1)
        def p2subj2 = createSubject(2, 2)
        def p2subj3 = createSubject(2, 3)
        def p3subj1 = createSubject(3, 1)
        def p3subj2 = createSubject(3, 2)
        def p3subj3 = createSubject(3, 3)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)
        skillsService.createSubject(p1subj3)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p2subj2)
        skillsService.createSubject(p2subj3)
        skillsService.createSubject(p3subj1)
        skillsService.createSubject(p3subj2)
        skillsService.createSubject(p3subj3)

        // project 1
        def skill1 = createSkill(1, 1, 1, 0, 5, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 5, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 5, 0, 250)
        def skill4 = createSkill(1, 2, 1, 0, 5, 0, 250)
        def skill5 = createSkill(1, 2, 2, 0, 5, 0, 250)
        def skill6 = createSkill(1, 3, 3, 0, 5, 0, 250)
        skillsService.createSkills([skill1, skill2, skill3, skill4, skill5, skill6])
        skillsService.bulkExportSkillsToCatalog(project1.projectId, [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId, skill5.skillId, skill6.skillId])

        // project 2
        def skill7 = createSkill(2, 1, 4, 0, 5, 0, 250)
        def skill8 = createSkill(2, 1, 5, 0, 5, 0, 250)
        def skill9 = createSkill(2, 1, 6, 0, 5, 0, 250)
        def skill10 = createSkill(2, 2, 7, 0, 5, 0, 250)
        def skill11 = createSkill(2, 2, 8, 0, 5, 0, 250)
        def skill12 = createSkill(2, 3, 9, 0, 5, 0, 250)
        def skill13 = createSkill(2, 3, 10, 0, 5, 0, 250)
        def skill14 = createSkill(2, 3, 11, 0, 5, 0, 250)
        skillsService.createSkills([skill7, skill8, skill9, skill10, skill11, skill12, skill13, skill14])
        skillsService.bulkExportSkillsToCatalog(project2.projectId, [skill7.skillId, skill8.skillId, skill9.skillId, skill10.skillId, skill11.skillId, skill12.skillId, skill13.skillId, skill14.skillId])

        // project 3
        def skill15 = createSkill(3, 1, 12, 0, 5, 0, 250)
        def skill16 = createSkill(3, 1, 13, 0, 5, 0, 250)
        def skill17 = createSkill(3, 1, 14, 0, 5, 0, 250)
        def skill18 = createSkill(3, 1, 15, 0, 5, 0, 250)
        def skill19 = createSkill(3, 2, 16, 0, 5, 0, 250)
        def skill20 = createSkill(3, 2, 17, 0, 5, 0, 250)
        def skill21 = createSkill(3, 2, 18, 0, 5, 0, 250)
        def skill22 = createSkill(3, 3, 19, 0, 5, 0, 250)
        def skill23 = createSkill(3, 3, 20, 0, 5, 0, 250)
        skillsService.createSkills([skill15, skill16, skill17, skill18, skill19, skill20, skill21, skill22, skill23])
        skillsService.bulkExportSkillsToCatalog(project3.projectId, [skill15.skillId, skill16.skillId, skill17.skillId, skill18.skillId, skill19.skillId, skill20.skillId, skill21.skillId, skill22.skillId, skill23.skillId])

        def randomUsers = getRandomUsers(3)
        def user = randomUsers[0]
        def user2 = randomUsers[1]
        def user3 = randomUsers[2]

        // user 1
        3.times {
            skillsService.addSkill([projectId: project2.projectId, skillId: skill7.skillId], user)
            skillsService.addSkill([projectId: project2.projectId, skillId: skill10.skillId], user)
            skillsService.addSkill([projectId: project2.projectId, skillId: skill12.skillId], user)
        }

        2.times {
            skillsService.addSkill([projectId: project1.projectId, skillId: skill1.skillId], user)
        }

        5.times {
            skillsService.addSkill([projectId: project3.projectId, skillId: skill17.skillId], user)
            skillsService.addSkill([projectId: project3.projectId, skillId: skill18.skillId], user)
            skillsService.addSkill([projectId: project3.projectId, skillId: skill19.skillId], user)
        }

        // user 2
        skillsService.addSkill([projectId: project2.projectId, skillId: skill7.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill8.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill9.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill9.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill10.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill13.skillId], user2)

        // user 3
        skillsService.addSkill([projectId: project2.projectId, skillId: skill7.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill8.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill9.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill9.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill10.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill13.skillId], user3)

        5.times {
            skillsService.addSkill([projectId: project1.projectId, skillId: skill1.skillId], user3)
            skillsService.addSkill([projectId: project1.projectId, skillId: skill2.skillId], user3)
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Integer proj2_subj1_ref_id = skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p1subj1.subjectId).id
        Integer proj2_subj2_ref_id = skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p1subj2.subjectId).id
        Integer proj2_subj3_ref_id = skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p1subj3.subjectId).id
        when:
        List<UserAchievement> proj2_user1Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj2_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj3_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        List<UserAchievement> proj2_user2Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user2Achievements_subj2_import0 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user2Achievements_subj3_import0 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        List<UserAchievement> proj2_user3Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user3Achievements_subj2_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user3Achievements_subj3_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        printLevels(project2.projectId, "before import", p1subj1.subjectId)
        printLevels(project2.projectId, "before import", p1subj2.subjectId)
        printLevels(project2.projectId, "before import", p1subj3.subjectId)
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, [[projectId: project1.projectId, skillId: skill1.skillId]])
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        printLevels(project2.projectId, "after import1", p1subj1.subjectId)
        printLevels(project2.projectId, "after import1", p1subj2.subjectId)
        printLevels(project2.projectId, "after import1", p1subj3.subjectId)

        List<UserAchievement> proj2_user1Achievements_subj1_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj2_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj3_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        List<UserAchievement> proj2_user2Achievements_subj1_import1 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user2Achievements_subj2_import1 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user2Achievements_subj3_import1 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        List<UserAchievement> proj2_user3Achievements_subj1_import1 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user3Achievements_subj2_import1 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user3Achievements_subj3_import1 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj2.subjectId, [[projectId: project3.projectId, skillId: skill17.skillId]])
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        printLevels(project2.projectId, "after import2", p1subj1.subjectId)
        printLevels(project2.projectId, "after import2", p1subj2.subjectId)
        printLevels(project2.projectId, "after import2", p1subj3.subjectId)

        List<UserAchievement> proj2_user1Achievements_subj1_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj2_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj3_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        List<UserAchievement> proj2_user2Achievements_subj1_import2 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user2Achievements_subj2_import2 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user2Achievements_subj3_import2 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        List<UserAchievement> proj2_user3Achievements_subj1_import2 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user3Achievements_subj2_import2 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user3Achievements_subj3_import2 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        then:
        // user 1
        proj2_user1Achievements_subj1_import0.collect { it.level }.sort() == [1]
        proj2_user1Achievements_subj2_import0.collect { it.level }.sort() == [1, 2]
        proj2_user1Achievements_subj3_import0.collect { it.level }.sort() == [1]

        proj2_user1Achievements_subj1_import1.collect { it.level }.sort() == [1, 2]
        proj2_user1Achievements_subj2_import1.collect { it.level }.sort() == [1, 2]
        proj2_user1Achievements_subj3_import1.collect { it.level }.sort() == [1]

        proj2_user1Achievements_subj1_import2.collect { it.level }.sort() == [1, 2]
        proj2_user1Achievements_subj2_import2.collect { it.level }.sort() == [1, 2, 3]
        proj2_user1Achievements_subj3_import2.collect { it.level }.sort() == [1]

        // user 2
        proj2_user2Achievements_subj1_import0.collect { it.level }.sort() == [1, 2]
        proj2_user2Achievements_subj2_import0.collect { it.level }.sort() == [1]
        proj2_user2Achievements_subj3_import0.collect { it.level }.sort() == []

        proj2_user2Achievements_subj1_import1.collect { it.level }.sort() == [1, 2]
        proj2_user2Achievements_subj2_import1.collect { it.level }.sort() == [1]
        proj2_user2Achievements_subj3_import1.collect { it.level }.sort() == []

        proj2_user2Achievements_subj1_import2.collect { it.level }.sort() == [1, 2]
        proj2_user2Achievements_subj2_import2.collect { it.level }.sort() == [1]
        proj2_user2Achievements_subj3_import2.collect { it.level }.sort() == []

        // user 3
        proj2_user3Achievements_subj1_import0.collect { it.level }.sort() == [1, 2]
        proj2_user3Achievements_subj2_import0.collect { it.level }.sort() == [1]
        proj2_user3Achievements_subj3_import0.collect { it.level }.sort() == []

        proj2_user3Achievements_subj1_import1.collect { it.level }.sort() == [1, 2, 3]
        proj2_user3Achievements_subj2_import1.collect { it.level }.sort() == [1]
        proj2_user3Achievements_subj3_import1.collect { it.level }.sort() == []

        proj2_user3Achievements_subj1_import2.collect { it.level }.sort() == [1, 2, 3]
        proj2_user3Achievements_subj2_import2.collect { it.level }.sort() == [1]
        proj2_user3Achievements_subj3_import2.collect { it.level }.sort() == []
    }


    def "importing a skill causes exciting users to level up within the subject - bulk"() {
        def project1 = createProject(1)
        def project2 = createProject(2)
        def project3 = createProject(3)

        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p2subj1 = createSubject(2, 1)
        def p2subj2 = createSubject(2, 2)
        def p2subj3 = createSubject(2, 3)
        def p3subj1 = createSubject(3, 1)
        def p3subj2 = createSubject(3, 2)
        def p3subj3 = createSubject(3, 3)

        skillsService.createProject(project1)
        skillsService.createProject(project2)
        skillsService.createProject(project3)
        skillsService.createSubject(p1subj1)
        skillsService.createSubject(p1subj2)
        skillsService.createSubject(p1subj3)
        skillsService.createSubject(p2subj1)
        skillsService.createSubject(p2subj2)
        skillsService.createSubject(p2subj3)
        skillsService.createSubject(p3subj1)
        skillsService.createSubject(p3subj2)
        skillsService.createSubject(p3subj3)

        // project 1
        def skill1 = createSkill(1, 1, 1, 0, 5, 0, 250)
        def skill2 = createSkill(1, 1, 2, 0, 5, 0, 250)
        def skill3 = createSkill(1, 1, 3, 0, 5, 0, 250)
        def skill4 = createSkill(1, 2, 1, 0, 5, 0, 250)
        def skill5 = createSkill(1, 2, 2, 0, 5, 0, 250)
        def skill6 = createSkill(1, 3, 3, 0, 5, 0, 250)
        skillsService.createSkills([skill1, skill2, skill3, skill4, skill5, skill6])
        skillsService.bulkExportSkillsToCatalog(project1.projectId, [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId, skill5.skillId, skill6.skillId])

        // project 2
        def skill7 = createSkill(2, 1, 4, 0, 5, 0, 250)
        def skill8 = createSkill(2, 1, 5, 0, 5, 0, 250)
        def skill9 = createSkill(2, 1, 6, 0, 5, 0, 250)
        def skill10 = createSkill(2, 2, 7, 0, 5, 0, 250)
        def skill11 = createSkill(2, 2, 8, 0, 5, 0, 250)
        def skill12 = createSkill(2, 3, 9, 0, 5, 0, 250)
        def skill13 = createSkill(2, 3, 10, 0, 5, 0, 250)
        def skill14 = createSkill(2, 3, 11, 0, 5, 0, 250)
        skillsService.createSkills([skill7, skill8, skill9, skill10, skill11, skill12, skill13, skill14])
        skillsService.bulkExportSkillsToCatalog(project2.projectId, [skill7.skillId, skill8.skillId, skill9.skillId, skill10.skillId, skill11.skillId, skill12.skillId, skill13.skillId, skill14.skillId])

        // project 3
        def skill15 = createSkill(3, 1, 12, 0, 5, 0, 250)
        def skill16 = createSkill(3, 1, 13, 0, 5, 0, 250)
        def skill17 = createSkill(3, 1, 14, 0, 5, 0, 250)
        def skill18 = createSkill(3, 1, 15, 0, 5, 0, 250)
        def skill19 = createSkill(3, 2, 16, 0, 5, 0, 250)
        def skill20 = createSkill(3, 2, 17, 0, 5, 0, 250)
        def skill21 = createSkill(3, 2, 18, 0, 5, 0, 250)
        def skill22 = createSkill(3, 3, 19, 0, 5, 0, 250)
        def skill23 = createSkill(3, 3, 20, 0, 5, 0, 250)
        skillsService.createSkills([skill15, skill16, skill17, skill18, skill19, skill20, skill21, skill22, skill23])
        skillsService.bulkExportSkillsToCatalog(project3.projectId, [skill15.skillId, skill16.skillId, skill17.skillId, skill18.skillId, skill19.skillId, skill20.skillId, skill21.skillId, skill22.skillId, skill23.skillId])

        def randomUsers = getRandomUsers(3)
        def user = randomUsers[0]
        def user2 = randomUsers[1]
        def user3 = randomUsers[2]

        // user 1
        3.times {
            skillsService.addSkill([projectId: project2.projectId, skillId: skill7.skillId], user)
            skillsService.addSkill([projectId: project2.projectId, skillId: skill10.skillId], user)
            skillsService.addSkill([projectId: project2.projectId, skillId: skill12.skillId], user)
        }

        2.times {
            skillsService.addSkill([projectId: project1.projectId, skillId: skill1.skillId], user)
        }

        5.times {
            skillsService.addSkill([projectId: project3.projectId, skillId: skill17.skillId], user)
            skillsService.addSkill([projectId: project3.projectId, skillId: skill18.skillId], user)
            skillsService.addSkill([projectId: project3.projectId, skillId: skill19.skillId], user)
        }

        // user 2
        skillsService.addSkill([projectId: project2.projectId, skillId: skill7.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill8.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill9.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill9.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill10.skillId], user2)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill13.skillId], user2)

        // user 3
        skillsService.addSkill([projectId: project2.projectId, skillId: skill7.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill8.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill9.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill9.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill10.skillId], user3)
        skillsService.addSkill([projectId: project2.projectId, skillId: skill13.skillId], user3)

        5.times {
            skillsService.addSkill([projectId: project1.projectId, skillId: skill1.skillId], user3)
            skillsService.addSkill([projectId: project1.projectId, skillId: skill2.skillId], user3)
            skillsService.addSkill([projectId: project3.projectId, skillId: skill17.skillId], user3)
            skillsService.addSkill([projectId: project3.projectId, skillId: skill18.skillId], user3)
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Integer proj2_subj1_ref_id = skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p1subj1.subjectId).id
        Integer proj2_subj2_ref_id = skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p1subj2.subjectId).id
        Integer proj2_subj3_ref_id = skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p1subj3.subjectId).id
        when:
        List<UserAchievement> proj2_user1Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj2_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj3_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        List<UserAchievement> proj2_user2Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user2Achievements_subj2_import0 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user2Achievements_subj3_import0 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        List<UserAchievement> proj2_user3Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user3Achievements_subj2_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user3Achievements_subj3_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        printLevels(project2.projectId, "before import", p1subj1.subjectId)
        printLevels(project2.projectId, "before import", p1subj2.subjectId)
        printLevels(project2.projectId, "before import", p1subj3.subjectId)
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, [[projectId: project1.projectId, skillId: skill1.skillId], [projectId: project3.projectId, skillId: skill17.skillId]])
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        printLevels(project2.projectId, "after import1", p1subj1.subjectId)
        printLevels(project2.projectId, "after import1", p1subj2.subjectId)
        printLevels(project2.projectId, "after import1", p1subj3.subjectId)

        List<UserAchievement> proj2_user1Achievements_subj1_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj2_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj3_import2 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        List<UserAchievement> proj2_user2Achievements_subj1_import2 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user2Achievements_subj2_import2 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user2Achievements_subj3_import2 = userAchievedRepo.findAll().findAll { it.userId == user2 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        List<UserAchievement> proj2_user3Achievements_subj1_import2 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}
        List<UserAchievement> proj2_user3Achievements_subj2_import2 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj2_ref_id}
        List<UserAchievement> proj2_user3Achievements_subj3_import2 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj3_ref_id}

        then:
        // user 1
        proj2_user1Achievements_subj1_import0.collect { it.level }.sort() == [1]
        proj2_user1Achievements_subj2_import0.collect { it.level }.sort() == [1, 2]
        proj2_user1Achievements_subj3_import0.collect { it.level }.sort() == [1]

        proj2_user1Achievements_subj1_import2.collect { it.level }.sort() == [1, 2]
        proj2_user1Achievements_subj2_import2.collect { it.level }.sort() == [1, 2]
        proj2_user1Achievements_subj3_import2.collect { it.level }.sort() == [1]

        // user 2
        proj2_user2Achievements_subj1_import0.collect { it.level }.sort() == [1, 2]
        proj2_user2Achievements_subj2_import0.collect { it.level }.sort() == [1]
        proj2_user2Achievements_subj3_import0.collect { it.level }.sort() == []

        proj2_user2Achievements_subj1_import2.collect { it.level }.sort() == [1, 2]
        proj2_user2Achievements_subj2_import2.collect { it.level }.sort() == [1]
        proj2_user2Achievements_subj3_import2.collect { it.level }.sort() == []

        // user 3
        proj2_user3Achievements_subj1_import0.collect { it.level }.sort() == [1, 2]
        proj2_user3Achievements_subj2_import0.collect { it.level }.sort() == [1]
        proj2_user3Achievements_subj3_import0.collect { it.level }.sort() == []

        proj2_user3Achievements_subj1_import2.collect { it.level }.sort() == [1, 2, 3]
        proj2_user3Achievements_subj2_import2.collect { it.level }.sort() == [1]
        proj2_user3Achievements_subj3_import2.collect { it.level }.sort() == []
    }


    def "achieve a skill group via a catalog import"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        def p2Group1 = createSkillsGroup(2, 2, 77)
        String skillsGroupId = p2Group1.skillId
        skillsService.createSkill(p2Group1)

        // add a regular/non-imported child skill to the group
        def child1 = SkillsFactory.createSkill(2, 2)
        String regChildSkillId = child1.skillId
        child1.numPerformToCompletion = 2
        skillsService.assignSkillToSkillsGroup(skillsGroupId, child1)

        // add an imported child skill to the group
        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(project2.p.projectId, project2.s2.subjectId, skillsGroupId,
                [[projectId: project1.p.projectId, skillId: project1.s1_skills[0].skillId]])
        String importedChildSkillId = project1.s1_skills[0].skillId

        def randomUsers = getRandomUsers(3)
        def user = randomUsers[0]

        when:
        // achieve non-imported child skill
        def sum1 = skillsService.getSkillSummary(user, project2.p.projectId, p2Group1.subjectId)
        skillsService.addSkill(child1, user, new Date() - 1)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def skill1CompletedRes = skillsService.addSkill(child1, user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def sum2 = skillsService.getSkillSummary(user, project2.p.projectId, p2Group1.subjectId)

        // achieve imported child skill
        skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[0].skillId], user, new Date() - 1)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def skill2CompletedRes = skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[0].skillId], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def sum3 = skillsService.getSkillSummary(user, project2.p.projectId, p2Group1.subjectId)

        List<UserAchievement> groupAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, project2.p.projectId, skillsGroupId)

        then:
        skill1CompletedRes.body.completed.find { it.type == "Skill" && it.id == regChildSkillId }
        skill2CompletedRes.body.completed.find { it.type == "Skill" && it.id == importedChildSkillId }

        groupAchievements
        groupAchievements.size() == 1
        groupAchievements[0].userId == user
        groupAchievements[0].projectId == project2.p.projectId
        groupAchievements[0].skillId == skillsGroupId

        sum1.points == 0
        sum1.skills.find { it.skillId == skillsGroupId }.points == 0

        sum2.points == 20
        sum2.skills.find { it.skillId == skillsGroupId }.points == 20

        sum3.points == 220
        sum3.skills.find { it.skillId == skillsGroupId }.points == 220
    }


    def "achieve a skill group via a catalog import, skill has already been completed in exported project prior to import"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        def randomUsers = getRandomUsers(3)
        def user = randomUsers[0]
        participateInProject(project2.p.projectId, [user])

        // achieve imported child skill
        skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[0].skillId], user, new Date() - 1)
        def skill2CompletedRes = skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[0].skillId], user)

        def p2Group1 = createSkillsGroup(2, 2, 77)
        String skillsGroupId = p2Group1.skillId
        skillsService.createSkill(p2Group1)

        // add an imported child skill to the group and finalize
        skillsService.bulkImportSkillsIntoGroupFromCatalogAndFinalize(project2.p.projectId, project1.s2.subjectId, skillsGroupId,
                [[projectId: project1.p.projectId, skillId: project1.s1_skills[0].skillId]])
        String importedChildSkillId = project1.s1_skills[0].skillId


        when:
        List<UserAchievement> childSkillAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, project2.p.projectId, importedChildSkillId)
        List<UserAchievement> groupAchievements = achievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, project2.p.projectId, skillsGroupId)

        then:
        skill2CompletedRes.body.completed.find { it.type == "Skill" && it.id == importedChildSkillId }

        childSkillAchievements
        childSkillAchievements.size() == 1
        childSkillAchievements[0].userId == user
        childSkillAchievements[0].projectId == project2.p.projectId
        childSkillAchievements[0].skillId == importedChildSkillId

        groupAchievements
        groupAchievements.size() == 1
        groupAchievements[0].userId == user
        groupAchievements[0].projectId == project2.p.projectId
        groupAchievements[0].skillId == skillsGroupId
    }


    def "achieve a badge via a catalog import"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        def p2badge1 = createBadge(2, 11)
        skillsService.createBadge(p2badge1)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)
        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId)

        skillsService.assignSkillToBadge(project2.p.projectId, p2badge1.badgeId, project2.s1_skills[0].skillId)
        skillsService.assignSkillToBadge(project2.p.projectId, p2badge1.badgeId, project1.s1_skills[0].skillId)

        p2badge1.enabled = true
        skillsService.updateBadge(p2badge1, p2badge1.badgeId)

        def randomUsers = getRandomUsers(3)
        def user = randomUsers[0]

        when:
        def sum1 = skillsService.getBadgeSummary(user, project2.p.projectId, p2badge1.badgeId)
        skillsService.addSkill([projectId: project2.p.projectId, skillId:project2.s1_skills[0].skillId], user, new Date() - 1)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def skill1CompletedRes = skillsService.addSkill([projectId: project2.p.projectId, skillId:project2.s1_skills[0].skillId], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def sum2 = skillsService.getBadgeSummary(user, project2.p.projectId, p2badge1.badgeId)

        skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[0].skillId], user, new Date() - 1)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def skill2CompletedRes = skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[0].skillId], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def sum3 = skillsService.getBadgeSummary(user, project2.p.projectId, p2badge1.badgeId)

        then:
        skill1CompletedRes.body.completed.find { it.type == "Skill" }
        skill2CompletedRes.body.completed.find { it.type == "Skill" }

        !sum1.badgeAchieved
        sum1.numTotalSkills == 2
        sum1.numSkillsAchieved == 0

        !sum2.badgeAchieved
        sum2.numTotalSkills == 2
        sum2.numSkillsAchieved == 1

        sum3.badgeAchieved
        sum3.numTotalSkills == 2
        sum3.numSkillsAchieved == 2
    }


    def "if imported skill was the only skill that was not completed under a badge and then imported skill is removed, badge should be awarded"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        def p2badge1 = createBadge(2, 11)
        skillsService.createBadge(p2badge1)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)
        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId)

        skillsService.assignSkillToBadge(project2.p.projectId, p2badge1.badgeId, project2.s1_skills[0].skillId)
        skillsService.assignSkillToBadge(project2.p.projectId, p2badge1.badgeId, project1.s1_skills[0].skillId)

        p2badge1.enabled = true
        skillsService.updateBadge(p2badge1, p2badge1.badgeId)

        def randomUsers = getRandomUsers(3)
        def user = randomUsers[0]

        when:
        def sum1 = skillsService.getBadgeSummary(user, project2.p.projectId, p2badge1.badgeId)

        skillsService.addSkill([projectId: project2.p.projectId, skillId:project2.s1_skills[0].skillId], user, new Date() - 1)
        def skill1CompletedRes = skillsService.addSkill([projectId: project2.p.projectId, skillId:project2.s1_skills[0].skillId], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def sum2 = skillsService.getBadgeSummary(user, project2.p.projectId, p2badge1.badgeId)

        skillsService.deleteSkill([projectId: project2.p.projectId, subjectId: project1.s2.subjectId, skillId:project1.s1_skills[0].skillId])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def sum3 = skillsService.getBadgeSummary(user, project2.p.projectId, p2badge1.badgeId)

        then:
        skill1CompletedRes.body.completed.find { it.type == "Skill" }

        !sum1.badgeAchieved
        sum1.numTotalSkills == 2
        sum1.numSkillsAchieved == 0

        !sum2.badgeAchieved
        sum2.numTotalSkills == 2
        sum2.numSkillsAchieved == 1

        sum3.badgeAchieved
        sum3.numTotalSkills == 1
        sum3.numSkillsAchieved == 1
    }


    def "skills achievements are copied when skills are imported"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)
        def project3 = createProjWithCatalogSkills(3)

        def p2badge1 = createBadge(2, 11)
        skillsService.createBadge(p2badge1)

        def randomUsers = getRandomUsers(3)
        def user = randomUsers[0]
        participateInProject(project2.p.projectId, [user])

        skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[0].skillId], user, new Date() - 1)
        def skill1CompletedRes = skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[0].skillId], user)

        skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[1].skillId], user, new Date() - 1)
        def skill2CompletedRes = skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[1].skillId], user)

        skillsService.addSkill([projectId: project3.p.projectId, skillId:project3.s1_skills[1].skillId], user, new Date() - 1)
        def skill3CompletedRes = skillsService.addSkill([projectId: project3.p.projectId, skillId:project3.s1_skills[1].skillId], user)

        when:
        List<UserAchievement> before = userAchievedRepo.findAllByUserAndProjectIds(user, [project2.p.projectId])
        skillsService.bulkImportSkillsFromCatalog(project2.p.projectId, project2.s2.subjectId, [
                [projectId: project1.p.projectId, skillId: project1.s1_skills[0].skillId],
                [projectId: project1.p.projectId, skillId: project1.s1_skills[1].skillId],
                [projectId: project3.p.projectId, skillId: project3.s1_skills[1].skillId],
        ])
        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId)

        List<UserAchievement> after = userAchievedRepo.findAllByUserAndProjectIds(user, [project2.p.projectId])

        def skill1 = skillsService.getSingleSkillSummary(user, project2.p.projectId, project1.s1_skills[0].skillId)
        def skill2 = skillsService.getSingleSkillSummary(user, project2.p.projectId, project1.s1_skills[1].skillId)
        def skill3 = skillsService.getSingleSkillSummary(user, project2.p.projectId, project3.s1_skills[1].skillId)
        then:
        skill1CompletedRes.body.completed.find { it.type == "Skill" }
        skill2CompletedRes.body.completed.find { it.type == "Skill" }
        skill3CompletedRes.body.completed.find { it.type == "Skill" }
        skill1.totalPoints == skill1.points
        skill2.totalPoints == skill2.points
        skill3.totalPoints == skill3.points

        !before
        after.find { it.skillId == project1.s1_skills[0].skillId }
        after.find { it.skillId == project1.s1_skills[1].skillId }
        after.find { it.skillId == project3.s1_skills[1].skillId }

        // make sure attributes got copied
        UserAchievement originalSk1 = userAchievedRepo.findAll().find {
            it.userId == user && it.projectId == project1.p.projectId && it.skillId == project1.s1_skills[0].skillId
        }
        UserAchievement importedSk1 = userAchievedRepo.findAll().find {
            it.userId == user && it.projectId == project2.p.projectId && it.skillId == project1.s1_skills[0].skillId
        }
        originalSk1.pointsWhenAchieved == importedSk1.pointsWhenAchieved
        originalSk1.achievedOn == importedSk1.achievedOn
        importedSk1.skillRefId == skillDefRepo.findByProjectIdAndSkillId( project2.p.projectId, project1.s1_skills[0].skillId).id
    }


    def "skills achievements should be removed when imported skills are removed"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)
        def project3 = createProjWithCatalogSkills(3)

        def randomUsers = getRandomUsers(3)
        def user = randomUsers[0]
        participateInProject(project2.p.projectId, [user])

        skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[0].skillId], user, new Date() - 1)
        skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[1].skillId], user, new Date() - 1)
        skillsService.addSkill([projectId: project3.p.projectId, skillId:project3.s1_skills[1].skillId], user, new Date() - 1)

        skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[0].skillId], user, new Date())
        skillsService.addSkill([projectId: project1.p.projectId, skillId:project1.s1_skills[1].skillId], user, new Date())
        skillsService.addSkill([projectId: project3.p.projectId, skillId:project3.s1_skills[1].skillId], user, new Date())

        when:
        List<UserAchievement> before = userAchievedRepo.findAllByUserAndProjectIds(user, [project2.p.projectId])
        skillsService.bulkImportSkillsFromCatalog(project2.p.projectId, project2.s2.subjectId, [
                [projectId: project1.p.projectId, skillId: project1.s1_skills[0].skillId],
                [projectId: project1.p.projectId, skillId: project1.s1_skills[1].skillId],
                [projectId: project3.p.projectId, skillId: project3.s1_skills[1].skillId],
        ])
        skillsService.finalizeSkillsImportFromCatalog(project2.p.projectId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        List<UserAchievement> afterImport = userAchievedRepo.findAllByUserAndProjectIds(user, [project2.p.projectId])

        skillsService.deleteSkill([projectId: project2.p.projectId, subjectId: project2.s2.subjectId, skillId: project1.s1_skills[0].skillId])
        skillsService.deleteSkill([projectId: project2.p.projectId, subjectId: project2.s2.subjectId, skillId: project1.s1_skills[1].skillId])
        skillsService.deleteSkill([projectId: project2.p.projectId, subjectId: project2.s2.subjectId, skillId: project3.s1_skills[1].skillId])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        List<UserAchievement> afterImportDeleted = userAchievedRepo.findAllByUserAndProjectIds(user, [project2.p.projectId])

        then:
        !before

        afterImport.find {it.projectId == project2.p.projectId && it.skillId == project1.s1_skills[0].skillId }
        afterImport.find { it.projectId == project2.p.projectId  && it.skillId == project1.s1_skills[1].skillId }
        afterImport.find { it.projectId == project2.p.projectId  && it.skillId == project3.s1_skills[1].skillId }

        !afterImportDeleted.find {it.projectId == project2.p.projectId && it.skillId == project1.s1_skills[0].skillId }
        !afterImportDeleted.find { it.projectId == project2.p.projectId  && it.skillId == project1.s1_skills[1].skillId }
        !afterImportDeleted.find { it.projectId == project2.p.projectId  && it.skillId == project3.s1_skills[1].skillId }
    }

}
