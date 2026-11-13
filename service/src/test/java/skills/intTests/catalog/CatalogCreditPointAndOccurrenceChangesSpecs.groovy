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

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsFactory
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo

import static skills.intTests.utils.SkillsFactory.*

class CatalogCreditPointAndOccurrenceChangesSpecs extends CatalogAchievementSpecBase {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    SkillDefRepo skillDefRepo

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
        participateInProject(project2.projectId, [user])
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
        participateInProject(project2.p.projectId, users.take(2))
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
        participateInProject(project2.p.projectId, users.take(1))
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
        participateInProject(project2.projectId, [user])
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
        participateInProject(proj1.projectId, [user])

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

        List<String> users = getRandomUsers(3)
        String user123 = users[0]
        String user124 = users[1]
        String user125 = users[2]
        participateInProject(proj1.projectId, users)

        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user123, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user123, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user123, new DateTime().minusDays(1).toDate())

        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user124, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user124, new Date())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user124, new DateTime().minusDays(1).toDate())

        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill1.skillId], user125, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill2.skillId], user125, new DateTime().minusDays(1).toDate())
        skillsService.addSkill([projectId: proj2.projectId, skillId: p2_skill3.skillId], user125, new DateTime().minusDays(2).toDate())

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        when:

        def u123SkillSummaryBeforeEdit = skillsService.getSkillSummary(user123, proj1.projectId, subj.subjectId)
        def u123PointHistoryBeforeEdit = skillsService.getPointHistory(user123, proj1.projectId, subj.subjectId)
        def u124SkillSummaryBeforeEdit = skillsService.getSkillSummary(user124, proj1.projectId, subj.subjectId)
        def u124PointHistoryBeforeEdit = skillsService.getPointHistory(user124, proj1.projectId, subj.subjectId)
        def u125SkillSummaryBeforeEdit = skillsService.getSkillSummary(user125, proj1.projectId, subj.subjectId)
        def u125PointHistoryBeforeEdit = skillsService.getPointHistory(user125, proj1.projectId, subj.subjectId)

        skillsService.updateImportedSkill(proj1.projectId, p2_skill1.skillId, 5)

        def u123pointHistoryAfterEdit = skillsService.getPointHistory(user123, proj1.projectId, subj.subjectId)
        def u123SkillSummaryAfterEdit = skillsService.getSkillSummary(user123, proj1.projectId, subj.subjectId)
        def u124pointHistoryAfterEdit = skillsService.getPointHistory(user124, proj1.projectId, subj.subjectId)
        def u124SkillSummaryAfterEdit = skillsService.getSkillSummary(user124, proj1.projectId, subj.subjectId)
        def u125pointHistoryAfterEdit = skillsService.getPointHistory(user125, proj1.projectId, subj.subjectId)
        def u125SkillSummaryAfterEdit = skillsService.getSkillSummary(user125, proj1.projectId, subj.subjectId)

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

}
