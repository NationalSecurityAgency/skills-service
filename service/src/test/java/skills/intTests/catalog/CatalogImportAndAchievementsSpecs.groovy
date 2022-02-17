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
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.WaitForAsyncTasksCompletion
import skills.services.LevelDefinitionStorageService
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo

import static skills.intTests.utils.SkillsFactory.*

class CatalogImportAndAchievementsSpecs extends DefaultIntSpec {

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    UserPointsRepo userPointsRepo

    def "reporting original skill event achieves level in all of the imported projects"() {
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

        def project3 = createProject(3)
        def p3subj1 = createSubject(3, 1)
        def p3_skills = (1..3).collect {createSkill(3, 1, 6+it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project3, p3subj1, p3_skills)
        p3_skills.each { skillsService.exportSkillToCatalog(project3.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, p1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(project3.projectId, p3subj1.subjectId, p1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(project3.projectId, p3subj1.subjectId, p2_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        skillsService.finalizeSkillsImportFromCatalog(project3.projectId)

        def users = getRandomUsers(5)
        skillsService.addSkill([projectId: project2.projectId, skillId: p2_skills[0].skillId], users[0])
        skillsService.addSkill([projectId: project2.projectId, skillId: p2_skills[1].skillId], users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Integer proj2_user1Level_before = skillsService.getUserLevel(project2.projectId, users[0])
        List<UserAchievement> proj2_user1Achievements_before = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        def proj2_user1Stats_before = skillsService.getUserStats(project2.projectId, users[0])
        Integer proj1_subj1_ref_id = skillDefRepo.findByProjectIdAndSkillId(project1.projectId, p1subj1.subjectId).id
        Integer proj2_subj1_ref_id = skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p2subj1.subjectId).id
        Integer proj3_subj1_ref_id = skillDefRepo.findByProjectIdAndSkillId(project3.projectId, p3subj1.subjectId).id
        List<UserAchievement> proj2_user1Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        when:
        printLevels(project1.projectId, "")
        printLevels(project2.projectId, "")
        printLevels(project3.projectId, "")

        skillsService.addSkill([projectId: project1.projectId, skillId: p1_skills[1].skillId], users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Integer proj2_user1Level_after = skillsService.getUserLevel(project2.projectId, users[0])
        List<UserAchievement> proj2_user1Achievements_after = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        def proj2_user1Stats_after = skillsService.getUserStats(project2.projectId, users[0])
        List<UserAchievement> proj2_user1Achievements_subj1_import1 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        Integer proj3_user1Level_after = skillsService.getUserLevel(project3.projectId, users[0])
        List<UserAchievement> proj3_user1Achievements_after = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == project3.projectId && it.skillRefId == null}
        List<UserAchievement> proj3_user1Achievements_subj1_import1 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == project3.projectId && it.skillRefId == proj3_subj1_ref_id}

        skillsService.addSkill([projectId: project1.projectId, skillId: p1_skills[1].skillId], users[0])
        skillsService.addSkill([projectId: project1.projectId, skillId: p1_skills[1].skillId], users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Integer proj3_user1Level_report2 = skillsService.getUserLevel(project3.projectId, users[0])
        List<UserAchievement> proj3_user1Achievements_report2 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == project3.projectId && it.skillRefId == null}
        List<UserAchievement> proj3_user1Achievements_subj1_report = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == project3.projectId && it.skillRefId == proj3_subj1_ref_id}

        // user 2 - just project 2
        skillsService.addSkill([projectId: project2.projectId, skillId: p2_skills[1].skillId], users[1])
        skillsService.addSkill([projectId: project2.projectId, skillId: p2_skills[2].skillId], users[1])
        skillsService.addSkill([projectId: project2.projectId, skillId: p2_skills[1].skillId], users[1])

        // user 3 - starts with project 2 but achievement happens natively in project 3
        5.times {
            skillsService.addSkill([projectId: project2.projectId, skillId: p2_skills[0].skillId], users[2])
            skillsService.addSkill([projectId: project2.projectId, skillId: p2_skills[1].skillId], users[2])
        }
        skillsService.addSkill([projectId: project2.projectId, skillId: p2_skills[2].skillId], users[2])
        skillsService.addSkill([projectId: project3.projectId, skillId: p3_skills[1].skillId], users[2])

        // user 4 - starts has a mix of native and imported skills and achievement happens via an imported skill
        5.times {
            skillsService.addSkill([projectId: project2.projectId, skillId: p2_skills[0].skillId], users[3])
            skillsService.addSkill([projectId: project3.projectId, skillId: p3_skills[0].skillId], users[3])
            skillsService.addSkill([projectId: project2.projectId, skillId: p2_skills[1].skillId], users[3])
            skillsService.addSkill([projectId: project3.projectId, skillId: p3_skills[1].skillId], users[3])
        }
        skillsService.addSkill([projectId: project3.projectId, skillId: p3_skills[2].skillId], users[3])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        proj2_user1Level_before == 0
        proj2_user1Stats_before.userTotalPoints == 500
        proj2_user1Achievements_before.collect { it.level }.sort() == []
        proj2_user1Achievements_subj1_import0.collect { it.level }.sort() == []

        proj2_user1Stats_after.userTotalPoints == 750
        proj2_user1Level_after == 1
        proj2_user1Achievements_after.collect { it.level }.sort() == [1]
        proj2_user1Achievements_subj1_import1.collect { it.level }.sort() == [1]

        proj3_user1Level_after == 0
        proj3_user1Achievements_after.collect { it.level }.sort() == []
        proj3_user1Achievements_subj1_import1.collect { it.level }.sort() == []

        proj3_user1Level_report2 == 1
        proj3_user1Achievements_report2.collect { it.level }.sort() == [1]
        proj3_user1Achievements_subj1_report.collect { it.level }.sort() == [1]

        // user 2
        skillsService.getUserLevel(project1.projectId, users[1]) == 0
        skillsService.getUserStats(project2.projectId, users[1]).userTotalPoints == 750
        skillsService.getUserLevel(project2.projectId, users[1]) == 1
        skillsService.getUserLevel(project3.projectId, users[1]) == 0
        getLevels(users[1], project1.projectId) == []
        getLevels(users[1], project2.projectId) == [1]
        getLevels(users[1], project3.projectId) == []
        getLevels(users[1], project1.projectId, proj1_subj1_ref_id) == []
        getLevels(users[1], project2.projectId, proj2_subj1_ref_id) == [1]
        getLevels(users[1], project3.projectId, proj3_subj1_ref_id) == []

        // user 3
        skillsService.getUserStats(project3.projectId, users[2]).userTotalPoints == 3000
        skillsService.getUserLevel(project3.projectId, users[2]) == 2
        getLevels(users[2], project3.projectId) == [1, 2]
        getLevels(users[2], project3.projectId, proj3_subj1_ref_id) == [1, 2]

        // user 4
        skillsService.getUserStats(project3.projectId, users[3]).userTotalPoints == 5250
        skillsService.getUserLevel(project3.projectId, users[3]) == 3
        getLevels(users[3], project3.projectId) == [1, 2, 3]
        getLevels(users[3], project3.projectId, proj3_subj1_ref_id) == [1, 2, 3]
    }

    private createProjWithCatalogSkills ( Integer projNum ) {
        def proj = createProject(projNum)
        def subj1 = createSubject(projNum, 1)
        def subj2 = createSubject(projNum, 2)
        def subj3 = createSubject(projNum, 3)
        def subj1_skills = (1..3).collect {createSkill(projNum, 1, projNum * 10 + it, 0,2, 480, 100) }
        def subj2_skills = (1..3).collect {createSkill(projNum, 2, projNum * 10 + it + 3, 0, 2, 480, 100) }
        def subj3_skills = (1..3).collect {createSkill(projNum, 3, projNum * 10 + it + 6, 0, 2, 480, 100) }
        skillsService.createProject(proj)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSubject(subj3)
        List skills = [subj1_skills, subj2_skills, subj3_skills].flatten()
        skillsService.createSkills(skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }
        return [
                p : proj,
                s1: subj1,
                s2: subj2,
                s1_skills: subj1_skills,
                s2_skills: subj2_skills,
                s3_skills: subj3_skills,
        ]
    }

    def "reporting original skill event achieves subject level"() {
        def proj1 = createProjWithCatalogSkills(1)
        def proj2 = createProjWithCatalogSkills(2)

        def proj3 = SkillsFactory.createProject(3)
        def p3_subj1 = SkillsFactory.createSubject(3, 1)
        def p3_subj2 = SkillsFactory.createSubject(3, 2)
        skillsService.createProject(proj3)
        skillsService.createSubject(p3_subj1)
        skillsService.createSubject(p3_subj2)

        skillsService.bulkImportSkillsFromCatalog(proj3.projectId, p3_subj1.subjectId, proj1.s1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(proj3.projectId, p3_subj1.subjectId, proj2.s1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(proj3.projectId, p3_subj2.subjectId, proj1.s2_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.finalizeSkillsImportFromCatalog(proj3.projectId)

        def users = getRandomUsers(5)

        Integer proj3_subj1_ref_id = skillDefRepo.findByProjectIdAndSkillId(proj3.projectId, p3_subj1.subjectId).id
        Integer proj3_subj2_ref_id = skillDefRepo.findByProjectIdAndSkillId(proj3.projectId, p3_subj2.subjectId).id

        List<UserAchievement> user1_subj1_report0 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId ==proj3.projectId && it.skillRefId == proj3_subj1_ref_id}
        List<UserAchievement> user1_subj2_report0 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId ==proj3.projectId && it.skillRefId == proj3_subj2_ref_id}
        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[0].skillId], users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<UserAchievement> user1_subj1_report1 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId ==proj3.projectId && it.skillRefId == proj3_subj1_ref_id}
        List<UserAchievement> user1_subj2_report1 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == proj3.projectId && it.skillRefId == proj3_subj2_ref_id}

        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s2_skills[0].skillId], users[0], new Date() - 1)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<UserAchievement> user1_subj1_report2 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId ==proj3.projectId && it.skillRefId == proj3_subj1_ref_id}
        List<UserAchievement> user1_subj2_report2 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == proj3.projectId && it.skillRefId == proj3_subj2_ref_id}

        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[0].skillId], users[0], new Date() - 2)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<UserAchievement> user1_subj1_report3 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId ==proj3.projectId && it.skillRefId == proj3_subj1_ref_id}
        List<UserAchievement> user1_subj2_report3 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == proj3.projectId && it.skillRefId == proj3_subj2_ref_id}

        skillsService.addSkill([projectId: proj1.p.projectId, skillId: proj1.s1_skills[0].skillId], users[0], new Date() - 1)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<UserAchievement> user1_subj1_report4 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId ==proj3.projectId && it.skillRefId == proj3_subj1_ref_id}
        List<UserAchievement> user1_subj2_report4 = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == proj3.projectId && it.skillRefId == proj3_subj2_ref_id}

        when:
        printLevels(proj3.projectId, "", p3_subj1.subjectId)
        printLevels(proj3.projectId, "", p3_subj2.subjectId)

        then:
        user1_subj1_report0.collect { it.level }.sort() == []
        user1_subj2_report0.collect { it.level }.sort() == []

        user1_subj1_report1.collect { it.level }.sort() == []
        user1_subj2_report1.collect { it.level }.sort() == [1]

        user1_subj1_report2.collect { it.level }.sort() == []
        user1_subj2_report2.collect { it.level }.sort() == [1, 2]

        user1_subj1_report3.collect { it.level }.sort() == []
        user1_subj2_report3.collect { it.level }.sort() == [1, 2]

        user1_subj1_report4.collect { it.level }.sort() == [1]
        user1_subj2_report4.collect { it.level }.sort() == [1, 2]
    }

    def "copy user-points when importing a skill"() {
        def proj1 = createProjWithCatalogSkills(1)
        def proj2 = createProjWithCatalogSkills(2)
        def proj3 = createProjWithCatalogSkills(3)

        def users = getRandomUsers(15)
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
        user0.size() == 14
        user0.findAll { !it.day && it.skillId == proj1.s1_skills[0].skillId }.collect { it.points } == [200]
        user0.findAll { it.day && it.skillId == proj1.s1_skills[0].skillId }.collect { it.day.format("yyyy/MM/dd") }.sort() == [dates[1].format("yyyy/MM/dd"), dates[0].format("yyyy/MM/dd")]
        user0.findAll { it.day && it.skillId == proj1.s1_skills[0].skillId }.collect { it.points } == [100, 100]
        user0.findAll { !it.day && it.skillId == proj1.s1.subjectId }.collect { it.points } == [400]
        user0.findAll { it.day && it.skillId == proj1.s1.subjectId }.sort { it.day }.collect { it.day.format("yyyy/MM/dd") }.sort() == [dates[2].format("yyyy/MM/dd"), dates[1].format("yyyy/MM/dd"), dates[0].format("yyyy/MM/dd")]
        user0.findAll { it.day && it.skillId == proj1.s1.subjectId }.collect { it.points } == [100, 200, 100]
        user0.findAll { !it.day && !it.skillId }.collect { it.points } == [400]
        user0.findAll { it.day && !it.skillId }.collect { it.day.format("yyyy/MM/dd") }.sort() == [dates[2].format("yyyy/MM/dd"), dates[1].format("yyyy/MM/dd"), dates[0].format("yyyy/MM/dd")]
        user0.findAll { it.day && !it.skillId }.sort { it.day }.collect { it.points } == [100, 200, 100]
        user0.findAll { !it.day && it.skillId == proj1.s1_skills[1].skillId }.collect { it.points } == [200]
        user0.findAll { it.day && it.skillId == proj1.s1_skills[1].skillId }.collect { it.day.format("yyyy/MM/dd") }.sort() == [dates[2].format("yyyy/MM/dd"), dates[1].format("yyyy/MM/dd")]
        user0.findAll { it.day && it.skillId == proj1.s1_skills[1].skillId }.collect { it.points } == [100, 100]
    }

    private List<Integer> getLevels(String user, String projectId, Integer skillRefId = null) {
        return userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == projectId && it.skillRefId == skillRefId}.collect { it.level }.sort()
    }

    def "report skill event to a project with imported skills - achieves level in that project"() {
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

        def project3 = createProject(3)
        def p3subj1 = createSubject(3, 1)
        def p3_skills = (1..3).collect {createSkill(3, 1, 6+it, 0, 5, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project3, p3subj1, p3_skills)
        p3_skills.each { skillsService.exportSkillToCatalog(project3.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, p1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(project3.projectId, p3subj1.subjectId, p1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(project3.projectId, p3subj1.subjectId, p2_skills.collect { [projectId: it.projectId, skillId: it.skillId] })
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)
        skillsService.finalizeSkillsImportFromCatalog(project3.projectId)

        def users = getRandomUsers(3)
        skillsService.addSkill([projectId: project1.projectId, skillId: p1_skills[0].skillId], users[0])
        skillsService.addSkill([projectId: project1.projectId, skillId: p1_skills[1].skillId], users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Integer proj2_user1Level_before = skillsService.getUserLevel(project2.projectId, users[0])
        List<UserAchievement> proj2_user1Achievements_before = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        def proj2_user1Stats_before = skillsService.getUserStats(project2.projectId, users[0])

        when:
        printLevels(project1.projectId, "")
        printLevels(project2.projectId, "")
        printLevels(project3.projectId, "")

        skillsService.addSkill([projectId: project2.projectId, skillId: p2_skills[2].skillId], users[0])

        Integer proj2_user1Level_after = skillsService.getUserLevel(project2.projectId, users[0])
        List<UserAchievement> proj2_user1Achievements_after = userAchievedRepo.findAll().findAll { it.userId == users[0] && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        def proj2_user1Stats_after = skillsService.getUserStats(project2.projectId, users[0])

        then:
        proj2_user1Level_before == 0
        proj2_user1Stats_before.userTotalPoints == 500
        proj2_user1Achievements_before.collect { it.level }.sort() == []

        proj2_user1Stats_after.userTotalPoints == 750
        proj2_user1Level_after == 1
        proj2_user1Achievements_after.collect { it.level }.sort() == [1]
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

    def "achieve a badge via a catalog import"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)

        def p2badge1 = createBadge(2, 11)
        skillsService.createBadge(p2badge1)

        skillsService.importSkillFromCatalog(project2.p.projectId, project1.s2.subjectId, project1.p.projectId, project1.s1_skills[0].skillId)

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

    def "skills achievements are copied when skills are imported"() {
        def project1 = createProjWithCatalogSkills(1)
        def project2 = createProjWithCatalogSkills(2)
        def project3 = createProjWithCatalogSkills(3)

        def p2badge1 = createBadge(2, 11)
        skillsService.createBadge(p2badge1)

        def randomUsers = getRandomUsers(3)
        def user = randomUsers[0]

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


    private void printLevels(String projectId, String label, String subjectId = null) {
        println "------------\n${projectId}${subjectId ? ":${subjectId}" : ""} - ${label}:"
        levelDefinitionStorageService.getLevels(projectId, subjectId).each{
            println "  Level ${it.level} : [${it.pointsFrom}]=>[${it.pointsTo}]"
        }
        println "-----------"
    }
}

