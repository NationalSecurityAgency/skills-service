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
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserAchievement
import skills.storage.model.UserEvent
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo

import static skills.intTests.utils.SkillsFactory.*

class CatalogIncrementalCreditSpecs extends CatalogAchievementSpecBase {

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
        participateInProject(project3.projectId, users.take(4))
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
        participateInProject(project3.projectId, users.take(1))
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
        participateInProject(proj2.p.projectId, [userId])

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
        participateInProject(proj2.p.projectId, [userId])
        when:
        def addSkillRes = skillsService.addSkill(proj1.s1_skills[0], userId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<UserPerformedSkill> performedSkills = userPerformedSkillRepo.findAll()
        List<UserEvent> events = userEventsRepo.findAll()
        then:
        performedSkills.collect({it.projectId}) == [proj1.p.projectId]
        events.collect({it.projectId}) == [proj1.p.projectId]
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(proj2.p.projectId, userId, proj1.s1_skills[0].skillId).points == 100
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

        participateInProject(proj3.projectId, users.take(1))
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
        participateInProject(project2.projectId, users.take(1))
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


    private List<Integer> getLevels(String user, String projectId, Integer skillRefId = null) {
        return userAchievedRepo.findAll().findAll { it.userId == user && it.level != null && it.projectId == projectId && it.skillRefId == skillRefId}.collect { it.level }.sort()
    }

}
