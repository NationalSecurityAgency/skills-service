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

import groovy.json.JsonOutput
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsFactory
import skills.services.LevelDefinitionStorageService
import skills.services.admin.SkillCatalogTransactionalAccessor
import skills.storage.model.UserAchievement
import skills.storage.model.UserEvent
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo

import static skills.intTests.utils.SkillsFactory.*

class CatalogImportAndAchievementsSpecs extends CatalogIntSpec {

    @Autowired
    LevelDefinitionStorageService levelDefinitionStorageService

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    SkillDefRepo skillDefRepo

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

    def "reporting original skill event does NOT achieve level in all of the imported projects when imported skills are disabled"() {
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

        printLevels(project1.projectId, "")
        printLevels(project2.projectId, "")
        printLevels(project3.projectId, "")

        when:
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

        then:
        proj2_user1Level_before == 1
        proj2_user1Stats_before.userTotalPoints == 500
        proj2_user1Achievements_before.collect { it.level }.sort() == [1]
        proj2_user1Achievements_subj1_import0.collect { it.level }.sort() == [1]

        proj2_user1Stats_after.userTotalPoints == 500
        proj2_user1Level_after == 1
        proj2_user1Achievements_after.collect { it.level }.sort() == [1]
        proj2_user1Achievements_subj1_import1.collect { it.level }.sort() == [1]

        proj3_user1Level_after == 0
        proj3_user1Achievements_after.collect { it.level }.sort() == []
        proj3_user1Achievements_subj1_import1.collect { it.level }.sort() == []

        proj3_user1Level_report2 == 0
        proj3_user1Achievements_report2.collect { it.level }.sort() == []
        proj3_user1Achievements_subj1_report.collect { it.level }.sort() == []
    }

    def "user points are not created/update when skill is disabled"() {
        def proj1 = createProjWithCatalogSkills(1)
        def proj2 = createProjWithCatalogSkills(2)
        skillsService.importSkillFromCatalog(proj2.p.projectId, proj2.s1.subjectId, proj1.p.projectId, proj1.s1_skills[0].skillId)

        String userId = getRandomUsers(1)[0]

        when:
        List<UserPoints> allPoints1 = userPointsRepo.findAll()
        def addSkillRes = skillsService.addSkill(proj1.s1_skills[0], userId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        println JsonOutput.toJson(addSkillRes)
        List<UserPoints> allPoints2 = userPointsRepo.findAll().findAll({ it.skillId == proj1.s1_skills[0].skillId})
        println JsonOutput.toJson(allPoints2)
        then:
        !allPoints1
        allPoints2.collect { it.projectId } == [proj1.p.projectId]
    }

    def "user_events and user_performed_skill rows are only created for the original skill"() {
        def proj1 = createProjWithCatalogSkills(1)
        def proj2 = createProjWithCatalogSkills(2)
        skillsService.importSkillFromCatalog(proj2.p.projectId, proj2.s1.subjectId, proj1.p.projectId, proj1.s1_skills[0].skillId)
        skillsService.finalizeSkillsImportFromCatalog(proj2.p.projectId)

        String userId = getRandomUsers(1)[0]
        when:
        def addSkillRes = skillsService.addSkill(proj1.s1_skills[0], userId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<UserPerformedSkill> performedSkills = userPerformedSkillRepo.findAll()
        List<UserEvent> events = userEventsRepo.findAll()
        then:
        performedSkills.collect({it.projectId}) == [proj1.p.projectId]
        events.collect({it.projectId}) == [proj1.p.projectId]
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
        user0.size() == 4
        user0.findAll { it.skillId == proj1.s1_skills[0].skillId }.collect { it.points } == [200]
        user0.findAll { it.skillId == proj1.s1.subjectId }.collect { it.points } == [400]
        user0.findAll { !it.skillId }.collect { it.points } == [400]
        user0.findAll { it.skillId == proj1.s1_skills[1].skillId }.collect { it.points } == [200]
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

    @Autowired
    SkillCatalogTransactionalAccessor skillCatalogTransactionalAccessor

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
        List<UserAchievement> proj2_user3Achievements_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == null}
        List<UserAchievement> proj2_user3Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user3 && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        printLevels(project2.projectId, "before import")
        skillsService.bulkImportSkillsFromCatalog(project2.projectId, p2subj1.subjectId, [[ projectId: project1.projectId, skillId: skill.skillId], [projectId: project3.projectId, skillId: skill7.skillId]])
        skillsService.finalizeSkillsImportFromCatalog(project2.projectId)

        skillCatalogTransactionalAccessor.updateSubjectTotalPoints(project2.projectId, p2subj1.subjectId)
        skillCatalogTransactionalAccessor.updateProjectsTotalPoints(project2.projectId)
        List<Integer> skillRefIds = [[ projectId: project1.projectId, skillId: skill.skillId], [projectId: project3.projectId, skillId: skill7.skillId]].collect({ skillDefRepo.findByProjectIdAndSkillId(it.projectId, it.skillId).id })
        skillCatalogTransactionalAccessor.copySkillUserPointsToTheImportedProjects(project2.projectId, skillRefIds)
        skillCatalogTransactionalAccessor.copySkillAchievementsToTheImportedProjects(project2.projectId, skillRefIds)

        skillCatalogTransactionalAccessor.createSubjectUserPointsForTheNewUsers(project2.projectId, p2subj1.subjectId)
        skillCatalogTransactionalAccessor.updateUserPointsForSubjectOrGroup(project2.projectId, p2subj1.subjectId)
        skillCatalogTransactionalAccessor.identifyAndAddSubjectLevelAchievements(project2.projectId, p2subj1.subjectId, false)

        skillCatalogTransactionalAccessor.createProjectUserPointsForTheNewUsers(project2.projectId)
        skillCatalogTransactionalAccessor.updateUserPointsForProject(project2.projectId)
        skillCatalogTransactionalAccessor.identifyAndAddProjectLevelAchievements(project2.projectId, false)

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

    def "changes in number of occurrences in the original skill causes user(s) to achieve a level within imported project"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..3).collect {createSkill(1, 1, it, 0, 50, 0, 250) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each { skillsService.exportSkillToCatalog(project1.projectId, it.skillId) }

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2_skills = (1..3).collect {createSkill(2, 1, 3+it, 0, 1, 0, 50) }
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2_skills)
        p2_skills.each { skillsService.exportSkillToCatalog(project2.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, [ [projectId:  p1_skills[0].projectId, skillId:  p1_skills[0].skillId] ])

        printLevels(project1.projectId, "")
        printLevels(project2.projectId, "")

        String user = getRandomUsers(1)[0]
        skillsService.addSkill(p1_skills[0], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Integer proj1_subj1_ref_id = skillDefRepo.findByProjectIdAndSkillId(project1.projectId, p1subj1.subjectId).id
        Integer proj2_subj1_ref_id = skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p2subj1.subjectId).id

        List<UserAchievement> proj1_user1Achievements_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project1.projectId && !it.skillRefId }
        List<UserAchievement> proj2_user1Achievements_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && !it.skillRefId }

        List<UserAchievement> proj1_user1Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project1.projectId && it.skillRefId == proj1_subj1_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        def userPtsProject1_t0 = skillsService.getUserStats(project1.projectId, user).userTotalPoints
        def userPtsProject2_t0 = skillsService.getUserStats(project2.projectId, user).userTotalPoints

        when:
        p1_skills[0].pointIncrement = 10
        p1_skills[0].numPerformToCompletion = 5
        skillsService.createSkills([p1_skills[0]])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        printLevels(project1.projectId, "")
        printLevels(project2.projectId, "")

        List<UserAchievement> proj1_user1Achievements_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project1.projectId && !it.skillRefId }
        List<UserAchievement> proj2_user1Achievements_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && !it.skillRefId }

        List<UserAchievement> proj1_user1Achievements_subj1_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project1.projectId && it.skillRefId == proj1_subj1_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj1_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        def userPtsProject1_t1 = skillsService.getUserStats(project1.projectId, user).userTotalPoints
        def userPtsProject2_t1 = skillsService.getUserStats(project2.projectId, user).userTotalPoints

        then:
        skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: p1_skills[0].skillId]).totalPoints == 250 * 5
        skillsService.getSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: p1_skills[0].skillId]).totalPoints == 10 * 5

        userPtsProject1_t0 == 250
        userPtsProject2_t0 == 250

        userPtsProject1_t1 == 10
        userPtsProject2_t1 == 250

        proj1_user1Achievements_import0.collect { it.level }.sort() == []
        proj2_user1Achievements_import0.collect { it.level }.sort() == []

        proj1_user1Achievements_subj1_import0.collect { it.level }.sort() == []
        proj2_user1Achievements_subj1_import0.collect { it.level }.sort() == []

        proj1_user1Achievements_subj1_import1.collect { it.level }.sort() == []
        proj2_user1Achievements_subj1_import1.collect { it.level }.sort() == [1]

        proj1_user1Achievements_import1.collect { it.level }.sort() == []
        proj2_user1Achievements_import1.collect { it.level }.sort() == [1]
    }

    def "decrease in number of occurrences in the original skill updates user(s) skills, subject and overall points"() {
        def project1 = createProjWithCatalogSkills(1, 10)
        def project2 = createProjWithCatalogSkills(2, 8)
        def project3 = createProjWithCatalogSkills(8)

        skillsService.bulkImportSkillsFromCatalogAndFinalize(project2.p.projectId, project2.s1.subjectId,
                project1.s1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        List<String> users = getRandomUsers(3)
        (1..8).each {skillsService.addSkill(project1.s1_skills[0], users[0], new Date() - it) }
        (1..5).each {skillsService.addSkill(project1.s1_skills[1], users[0], new Date() - it) }
        (1..4).each {skillsService.addSkill(project2.s1_skills[1], users[0], new Date() - it) }
        (1..7).each {skillsService.addSkill(project2.s2_skills[1], users[0], new Date() - it) }

        // muddle the water with other users
        (1..3).each {skillsService.addSkill(project1.s1_skills[0], users[1], new Date() - it) }
        (1..8).each {skillsService.addSkill(project1.s1_skills[1], users[1], new Date() - it) }
        (1..9).each {skillsService.addSkill(project2.s1_skills[1], users[2], new Date() - it) }
        (1..10).each {skillsService.addSkill(project2.s2_skills[1], users[2], new Date() - it) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Closure<UserPoints> getPoints = { String user, String projectId, String skillId ->
            List<UserPoints> points = userPointsRepo.findAll().findAll({it.userId == user && it.projectId == projectId && it.skillId == skillId })
            assert points.size() == 1
            return points[0]
        }

        when:
        // original skill
        UserPoints user0_p1skill0Pts_t0 = getPoints.call(users[0], project1.p.projectId, project1.s1_skills[0].skillId)
        UserPoints user0_p1skill1Pts_t0 = getPoints.call(users[0], project1.p.projectId, project1.s1_skills[1].skillId)
        UserPoints user0_p1S1Pts_t0 = getPoints.call(users[0], project1.p.projectId, project1.s1.subjectId)
        UserPoints user0_p1Pts_t0 = getPoints.call(users[0], project1.p.projectId, null)

        // imported skill
        UserPoints user0_p2skill0Pts_t0 = getPoints.call(users[0], project2.p.projectId, project1.s1_skills[0].skillId)
        UserPoints user0_p2skill1Pts_t0 = getPoints.call(users[0], project2.p.projectId, project1.s1_skills[1].skillId)
        UserPoints user0_p2S1skill1Pts_t0 = getPoints.call(users[0], project2.p.projectId, project2.s1_skills[1].skillId)
        UserPoints user0_p2S2skill1Pts_t0 = getPoints.call(users[0], project2.p.projectId, project2.s2_skills[1].skillId)
        UserPoints user0_p2S1Pts_t0 = getPoints.call(users[0], project2.p.projectId, project2.s1.subjectId)
        UserPoints user0_p2S2Pts_t0 = getPoints.call(users[0], project2.p.projectId, project2.s2.subjectId)
        UserPoints user0_p2Pts_t0 = getPoints.call(users[0], project2.p.projectId, null)

        project1.s1_skills[0].numPerformToCompletion = 7
        skillsService.createSkill(project1.s1_skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        // original skill
        UserPoints user0_p1skill0Pts_t1 = getPoints.call(users[0], project1.p.projectId, project1.s1_skills[0].skillId)
        UserPoints user0_p1skill1Pts_t1 = getPoints.call(users[0], project1.p.projectId, project1.s1_skills[1].skillId)
        UserPoints user0_p1S1Pts_t1 = getPoints.call(users[0], project1.p.projectId, project1.s1.subjectId)
        UserPoints user0_p1Pts_t1 = getPoints.call(users[0], project1.p.projectId, null)

        // imported skill
        UserPoints user0_p2skill0Pts_t1 = getPoints.call(users[0], project2.p.projectId, project1.s1_skills[0].skillId)
        UserPoints user0_p2skill1Pts_t1 = getPoints.call(users[0], project2.p.projectId, project1.s1_skills[1].skillId)
        UserPoints user0_p2S1skill1Pts_t1 = getPoints.call(users[0], project2.p.projectId, project2.s1_skills[1].skillId)
        UserPoints user0_p2S2skill1Pts_t1 = getPoints.call(users[0], project2.p.projectId, project2.s2_skills[1].skillId)
        UserPoints user0_p2S1Pts_t1 = getPoints.call(users[0], project2.p.projectId, project2.s1.subjectId)
        UserPoints user0_p2S2Pts_t1 = getPoints.call(users[0], project2.p.projectId, project2.s2.subjectId)
        UserPoints user0_p2Pts_t1 = getPoints.call(users[0], project2.p.projectId, null)

        then:
        // imported skill
        user0_p2skill0Pts_t0.points == 800
        user0_p2skill1Pts_t0.points == 500
        user0_p2S1skill1Pts_t0.points == 400
        user0_p2S2skill1Pts_t0.points == 700
        user0_p2S1Pts_t0.points == 800 + 500 + 400
        user0_p2S2Pts_t0.points == 700
        user0_p2Pts_t0.points == (user0_p2S1Pts_t0.points + user0_p2S2Pts_t0.points)

        user0_p2skill0Pts_t1.points == 700
        user0_p2skill1Pts_t1.points == 500
        user0_p2S1skill1Pts_t1.points == 400
        user0_p2S2skill1Pts_t1.points == 700
        user0_p2S1Pts_t1.points == 700 + 500 + 400
        user0_p2S2Pts_t1.points == 700
        user0_p2Pts_t1.points == (user0_p2S1Pts_t1.points + user0_p2S2Pts_t1.points)

        // original skill
        user0_p1skill0Pts_t0.points == 800
        user0_p1skill1Pts_t0.points == 500
        user0_p1S1Pts_t0.points == 800 + 500
        user0_p1Pts_t0.points == user0_p1S1Pts_t0.points

        user0_p1skill0Pts_t1.points == 700
        user0_p1skill1Pts_t1.points == 500
        user0_p1S1Pts_t1.points == 700 + 500
        user0_p1Pts_t1.points == user0_p1S1Pts_t1.points
    }

    def "decrease in number of occurrences removes extra UserPerformedSkill entries"() {
        def project1 = createProjWithCatalogSkills(1, 10)
        def project2 = createProjWithCatalogSkills(2, 8)
        def project3 = createProjWithCatalogSkills(8)

        skillsService.bulkImportSkillsFromCatalogAndFinalize(project2.p.projectId, project2.s1.subjectId,
                project1.s1_skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        List<String> users = getRandomUsers(3)
        List<Date> dates = (1..12).collect { new Date() -it }
        (1..8).each {skillsService.addSkill(project1.s1_skills[0], users[0], dates[it]) }
        (1..5).each {skillsService.addSkill(project1.s1_skills[1], users[0], dates[it]) }
        (1..4).each {skillsService.addSkill(project2.s1_skills[1], users[0], dates[it]) }
        (1..7).each {skillsService.addSkill(project2.s2_skills[1], users[0], dates[it]) }

        // muddle the water with other users
        (1..3).each {skillsService.addSkill(project1.s1_skills[0], users[1], dates[it]) }
        (1..8).each {skillsService.addSkill(project1.s1_skills[1], users[1], dates[it]) }
        (1..9).each {skillsService.addSkill(project2.s1_skills[1], users[2], dates[it]) }
        (1..10).each {skillsService.addSkill(project2.s2_skills[1], users[2], dates[it]) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Closure<List<UserPerformedSkill>> getPerformedSkills = { String user, String projectId, String skillId ->
            return userPerformedSkillRepo.findAll().findAll({ it.userId == user && it.projectId == projectId && it.skillId == skillId }).sort({ it.performedOn }).reverse()
        }

        when:
        List<UserPerformedSkill> u0_sk0_t0 = getPerformedSkills.call(users[0], project1.p.projectId, project1.s1_skills[0].skillId)
        List<UserPerformedSkill> u0_sk1_t0 = getPerformedSkills.call(users[0], project1.p.projectId, project1.s1_skills[1].skillId)

        project1.s1_skills[0].numPerformToCompletion = 7
        skillsService.createSkill(project1.s1_skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        List<UserPerformedSkill> u0_sk0_t1 = getPerformedSkills.call(users[0], project1.p.projectId, project1.s1_skills[0].skillId)
        List<UserPerformedSkill> u0_sk1_t1 = getPerformedSkills.call(users[0], project1.p.projectId, project1.s1_skills[1].skillId)

        then:
        u0_sk0_t0.collect { it.performedOn } == [dates[1], dates[2], dates[3], dates[4], dates[5], dates[6], dates[7], dates[8]]
        u0_sk0_t1.collect { it.performedOn } == [dates[2], dates[3], dates[4], dates[5], dates[6], dates[7], dates[8]]

        u0_sk1_t0.collect { it.performedOn } == [dates[1], dates[2], dates[3], dates[4], dates[5]]
        u0_sk1_t1.collect { it.performedOn } == [dates[1], dates[2], dates[3], dates[4], dates[5]]
    }

    def "change in number of points in the imported skill levels up user(s)"() {
        def project1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1_skills = (1..3).collect {createSkill(1, 1, it, 0, 5, 0, 10) }
        skillsService.createProjectAndSubjectAndSkills(project1, p1subj1, p1_skills)
        p1_skills.each { skillsService.exportSkillToCatalog(project1.projectId, it.skillId) }

        def project2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2_skills = (1..3).collect {createSkill(2, 1, 3+it, 0, 1, 0, 50) }
        skillsService.createProjectAndSubjectAndSkills(project2, p2subj1, p2_skills)
        p2_skills.each { skillsService.exportSkillToCatalog(project2.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalogAndFinalize(project2.projectId, p2subj1.subjectId, [ [projectId:  p1_skills[0].projectId, skillId:  p1_skills[0].skillId] ])

        printLevels(project1.projectId, "")
        printLevels(project2.projectId, "")

        String user = getRandomUsers(1)[0]
//        skillsService.addSkill(p1_skills[0], user, new Date() - 2)
//        skillsService.addSkill(p1_skills[0], user, new Date() - 1)
        skillsService.addSkill(p1_skills[0], user)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        Integer proj1_subj1_ref_id = skillDefRepo.findByProjectIdAndSkillId(project1.projectId, p1subj1.subjectId).id
        Integer proj2_subj1_ref_id = skillDefRepo.findByProjectIdAndSkillId(project2.projectId, p2subj1.subjectId).id

        List<UserAchievement> proj1_user1Achievements_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project1.projectId && !it.skillRefId }
        List<UserAchievement> proj2_user1Achievements_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && !it.skillRefId }

        List<UserAchievement> proj1_user1Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project1.projectId && it.skillRefId == proj1_subj1_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj1_import0 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        def userPtsProject1_t0 = skillsService.getUserStats(project1.projectId, user).userTotalPoints
        def userPtsProject2_t0 = skillsService.getUserStats(project2.projectId, user).userTotalPoints

        when:
        Map skill = new HashMap<>(p1_skills[0])
        skill.projectId = project2.projectId
        skill.subjectId = p2subj1.subjectId
        skill.pointIncrement = 500
        skillsService.createSkill(skill)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        printLevels(project1.projectId, "")
        printLevels(project2.projectId, "")

        List<UserAchievement> proj1_user1Achievements_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project1.projectId && !it.skillRefId }
        List<UserAchievement> proj2_user1Achievements_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && !it.skillRefId }

        List<UserAchievement> proj1_user1Achievements_subj1_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project1.projectId && it.skillRefId == proj1_subj1_ref_id}
        List<UserAchievement> proj2_user1Achievements_subj1_import1 = userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == project2.projectId && it.skillRefId == proj2_subj1_ref_id}

        def userPtsProject1_t1 = skillsService.getUserStats(project1.projectId, user).userTotalPoints
        def userPtsProject2_t1 = skillsService.getUserStats(project2.projectId, user).userTotalPoints

        then:
        skillsService.getSkill([projectId: project2.projectId, subjectId: p2subj1.subjectId, skillId: p1_skills[0].skillId]).totalPoints == 500 * 5
        skillsService.getSkill([projectId: project1.projectId, subjectId: p1subj1.subjectId, skillId: p1_skills[0].skillId]).totalPoints == 10 * 5

        userPtsProject1_t0 == 10
        userPtsProject2_t0 == 10

        userPtsProject1_t1 == 10
        userPtsProject2_t1 == 500

        proj1_user1Achievements_import0.collect { it.level }.sort() == []
        proj2_user1Achievements_import0.collect { it.level }.sort() == []

        proj1_user1Achievements_subj1_import0.collect { it.level }.sort() == []
        proj2_user1Achievements_subj1_import0.collect { it.level }.sort() == []

        proj1_user1Achievements_subj1_import1.collect { it.level }.sort() == []
        proj2_user1Achievements_subj1_import1.collect { it.level }.sort() == [1]

        proj1_user1Achievements_import1.collect { it.level }.sort() == []
        proj2_user1Achievements_import1.collect { it.level }.sort() == [1]
    }

    def "changes to skill points causes user's point history to be updated"(){
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skill2 = SkillsFactory.createSkill(1, 1, 2)
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [skill2])

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2)
        def p2_skill1 = SkillsFactory.createSkill(2, 1, 1, 0, 3, 0)
        def p2_skill3 = SkillsFactory.createSkill(2, 1, 3)
        p2_skill3.pointIncrement = 60
        def p2_skill4 = SkillsFactory.createSkill(2, 1, 4, 0, 1, 480, 100)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [p2_skill1, p2_skill3, p2_skill4])

        skillsService.exportSkillToCatalog(proj2.projectId, p2_skill1.skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, p2_skill3.skillId)
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj1.projectId, subj.subjectId, [
                [projectId: proj2.projectId, skillId: p2_skill1.skillId],
                [projectId: proj2.projectId, skillId: p2_skill3.skillId],
        ])

        String user = getRandomUsers(1)[0]

        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user, new DateTime().minusDays(1).toDate())
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        def skillSummaryBeforeEdit = skillsService.getSkillSummary(user, proj1.projectId, subj.subjectId)
        def pointHistoryBeforeEdit = skillsService.getPointHistory(user, proj1.projectId, subj.subjectId)
        def pointHistoryBeforeEdit_proj = skillsService.getPointHistory(user, proj1.projectId)

        skillsService.updateImportedSkill(proj1.projectId, p2_skill1.skillId, 5)

        def pointHistoryAfterEdit = skillsService.getPointHistory(user, proj1.projectId, subj.subjectId)
        def skillSummaryAfterEdit = skillsService.getSkillSummary(user, proj1.projectId, subj.subjectId)
        def pointHistoryAfterEdit_proj = skillsService.getPointHistory(user, proj1.projectId)

        then:
        skillSummaryBeforeEdit.points == 30
        skillSummaryBeforeEdit.totalPoints == 100
        skillSummaryBeforeEdit.todaysPoints == 20
        pointHistoryBeforeEdit.pointsHistory[0].points == 10
        pointHistoryBeforeEdit.pointsHistory[1].points == 30
        skillSummaryAfterEdit.totalPoints == 85
        skillSummaryAfterEdit.points == 15
        skillSummaryAfterEdit.todaysPoints == 10
        pointHistoryAfterEdit.pointsHistory[0].points == 5
        pointHistoryAfterEdit.pointsHistory[1].points == 15

        pointHistoryBeforeEdit_proj.pointsHistory[0].points == 10
        pointHistoryBeforeEdit_proj.pointsHistory[1].points == 30
        pointHistoryAfterEdit_proj.pointsHistory[0].points == 5
        pointHistoryAfterEdit_proj.pointsHistory[1].points == 15
    }

    def "changes to the original skill's points causes multiple users's point history to be updated"(){
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skill2 = SkillsFactory.createSkill(1, 1, 2)
        skillsService.createProjectAndSubjectAndSkills(proj1, subj, [skill2])

        def proj2 = SkillsFactory.createProject(2)
        def p2_subj = SkillsFactory.createSubject(2)
        def p2_skill1 = SkillsFactory.createSkill(2, 1, 1, 0, 3, 0)
        def p2_skill3 = SkillsFactory.createSkill(2, 1, 3)
        p2_skill3.pointIncrement = 60
        def p2_skill4 = SkillsFactory.createSkill(2, 1, 4, 0, 1, 480, 100)
        skillsService.createProjectAndSubjectAndSkills(proj2, p2_subj, [p2_skill1, p2_skill3, p2_skill4])

        skillsService.exportSkillToCatalog(proj2.projectId, p2_skill1.skillId)
        skillsService.exportSkillToCatalog(proj2.projectId, p2_skill3.skillId)
        skillsService.bulkImportSkillsFromCatalogAndFinalize(proj1.projectId, subj.subjectId, [
                [projectId: proj2.projectId, skillId: p2_skill1.skillId],
                [projectId: proj2.projectId, skillId: p2_skill3.skillId],
        ])

        String user = getRandomUsers(1)[0]

        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], "u123", new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], "u123", new DateTime().minusDays(1).toDate())

        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], "u124", new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], "u124", new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], "u124", new DateTime().minusDays(1).toDate())

        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], "u125", new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], "u125", new DateTime().minusDays(1).toDate())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill3.skillId], "u125", new DateTime().minusDays(2).toDate())

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        when:

        def u123SkillSummaryBeforeEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)
        def u123PointHistoryBeforeEdit = skillsService.getPointHistory("u123", proj1.projectId, subj.subjectId)
        def u124SkillSummaryBeforeEdit = skillsService.getSkillSummary("u124", proj1.projectId, subj.subjectId)
        def u124PointHistoryBeforeEdit = skillsService.getPointHistory("u124", proj1.projectId, subj.subjectId)
        def u125SkillSummaryBeforeEdit = skillsService.getSkillSummary("u125", proj1.projectId, subj.subjectId)
        def u125PointHistoryBeforeEdit = skillsService.getPointHistory("u125", proj1.projectId, subj.subjectId)

        skillsService.updateImportedSkill(proj1.projectId, p2_skill1.skillId, 5)

        def u123pointHistoryAfterEdit = skillsService.getPointHistory("u123", proj1.projectId, subj.subjectId)
        def u123SkillSummaryAfterEdit = skillsService.getSkillSummary("u123", proj1.projectId, subj.subjectId)
        def u124pointHistoryAfterEdit = skillsService.getPointHistory("u124", proj1.projectId, subj.subjectId)
        def u124SkillSummaryAfterEdit = skillsService.getSkillSummary("u124", proj1.projectId, subj.subjectId)
        def u125pointHistoryAfterEdit = skillsService.getPointHistory("u125", proj1.projectId, subj.subjectId)
        def u125SkillSummaryAfterEdit = skillsService.getSkillSummary("u125", proj1.projectId, subj.subjectId)

        then:
        u123SkillSummaryBeforeEdit.points == 30
        u123SkillSummaryBeforeEdit.totalPoints == 100
        u123SkillSummaryBeforeEdit.todaysPoints == 20
        u123PointHistoryBeforeEdit.pointsHistory[0].points == 10
        u123PointHistoryBeforeEdit.pointsHistory[1].points == 30
        u123SkillSummaryAfterEdit.totalPoints == 85
        u123SkillSummaryAfterEdit.points == 15
        u123SkillSummaryAfterEdit.todaysPoints == 10
        u123pointHistoryAfterEdit.pointsHistory[0].points == 5
        u123pointHistoryAfterEdit.pointsHistory[1].points == 15

        u124SkillSummaryBeforeEdit.points == 30
        u124SkillSummaryBeforeEdit.totalPoints == 100
        u124SkillSummaryBeforeEdit.todaysPoints == 20
        u124PointHistoryBeforeEdit.pointsHistory[0].points == 10
        u124PointHistoryBeforeEdit.pointsHistory[1].points == 30
        u124SkillSummaryAfterEdit.totalPoints == 85
        u124SkillSummaryAfterEdit.points == 15
        u124SkillSummaryAfterEdit.todaysPoints == 10
        u124pointHistoryAfterEdit.pointsHistory[0].points == 5
        u124pointHistoryAfterEdit.pointsHistory[1].points == 15

        u125SkillSummaryBeforeEdit.points == 80
        u125SkillSummaryBeforeEdit.totalPoints == 100
        u125SkillSummaryBeforeEdit.todaysPoints == 10
        u125PointHistoryBeforeEdit.pointsHistory[0].points == 60
        u125PointHistoryBeforeEdit.pointsHistory[1].points == 70
        u125PointHistoryBeforeEdit.pointsHistory[2].points == 80
        u125SkillSummaryAfterEdit.totalPoints == 85
        u125SkillSummaryAfterEdit.points == 75
        u125SkillSummaryAfterEdit.todaysPoints == 5
        u125pointHistoryAfterEdit.pointsHistory[0].points == 60
        u125pointHistoryAfterEdit.pointsHistory[1].points == 70
        u125pointHistoryAfterEdit.pointsHistory[2].points == 75
    }

    private void printLevels(String projectId, String label, String subjectId = null) {
        println "------------\n${projectId}${subjectId ? ":${subjectId}" : ""} - ${label}:"
        levelDefinitionStorageService.getLevels(projectId, subjectId).each{
            println "  Level ${it.level} : [${it.pointsFrom}]=>[${it.pointsTo}]"
        }
        println "-----------"
    }
}

