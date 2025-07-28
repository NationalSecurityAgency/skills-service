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
package skills.intTests.skillExpiration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.attributes.ExpirationAttrs
import skills.services.attributes.SkillAttributeService
import skills.storage.model.ExpiredUserAchievement
import skills.storage.model.SkillAttributesDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.repos.ExpiredUserAchievementRepo
import skills.tasks.executors.ExpireUserAchievementsTaskExecutor

import java.time.LocalDateTime

import static skills.intTests.utils.SkillsFactory.*

class RecurringSkillExpirationSpecs extends DefaultIntSpec {

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds // loaded from system props

    @Autowired
    SkillAttributeService skillAttributeService

    @Autowired
    ExpiredUserAchievementRepo expiredUserAchievementRepo

    @Autowired
    ExpireUserAchievementsTaskExecutor expireUserAchievementsTaskExecutor

    def setup() {
        skillsService.deleteProjectIfExist(projId)
        sampleUserIds = System.getProperty("sampleUserIds", "tom|||dick|||harry")?.split("\\|\\|\\|").sort()
    }

    def "expire skill events for a single skill"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = "user1"
        Long timestamp = new Date().time

        setup:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date(timestamp))

        assert res.body.skillApplied
        assert res.body.explanation == "Skill event was applied"

        assert res.body.completed.size() == 3
        assert res.body.completed.find({ it.type == "Skill" }).id == skills[0].skillId
        assert res.body.completed.find({ it.type == "Skill" }).name == skills[0].name

        assert res.body.completed.find({ it.type == "Overall" }).id == "OVERALL"
        assert res.body.completed.find({ it.type == "Overall" }).name == "OVERALL"
        assert res.body.completed.find({ it.type == "Overall" }).level == 1

        assert res.body.completed.find({ it.type == "Subject" }).id == subj.subjectId
        assert res.body.completed.find({ it.type == "Subject" }).name == subj.name
        assert res.body.completed.find({ it.type == "Subject" }).level == 1

        def addedSkills = skillsService.getPerformedSkills(userId, proj.projectId)
        assert addedSkills
        assert addedSkills.data.find { it.skillId == skills[0].skillId}

        def userAchievements_t0 = userAchievedRepo.findAll()
        def expiredUserAchievements_t0 = expiredUserAchievementRepo.findAll()

        when:
        expireSkill(proj.projectId, skills[0])
        addedSkills = skillsService.getPerformedSkills(userId, proj.projectId)

        def userAchievements_t1 = userAchievedRepo.findAll()
        def expiredUserAchievements_t1 = expiredUserAchievementRepo.findAll()

        then:
        !addedSkills?.data?.find { it.skillId == skills[0].skillId }

        userAchievements_t0.find { it.skillId == skills[0].skillId }
        !expiredUserAchievements_t0.find { it.skillId == skills[0].skillId }

        !userAchievements_t1.find { it.skillId == skills[0].skillId }
        expiredUserAchievements_t1.find { it.skillId == skills[0].skillId }
    }

    def "expire skill events for multiple skills"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(30, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Map badge = [projectId: proj.projectId, badgeId: 'badge1', name: 'Test Badge 1']
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId)
        badge.enabled = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        String userId = "user1"
        String secondUserId = "user2"
        Long timestamp = new Date().time

        setup:
        skills.forEach { it ->
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, new Date(timestamp))
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], secondUserId, new Date(timestamp))
        }

        def userInfo = skillsService.getUserStats(proj.projectId, userId)
        def level = skillsService.getUserLevel(proj.projectId, userId)
        assert userInfo.numSkills == 30
        assert userInfo.userTotalPoints == 300
        assert level == 5
        def badges = skillsService.getBadgesSummary(userId, proj.projectId)
        assert badges.size() == 1
        assert badges[0].badgeAchieved == true

        def addedSkills = skillsService.getPerformedSkills(userId, proj.projectId)
        assert addedSkills
        assert addedSkills.totalCount == 30

        def userInfo2 = skillsService.getUserStats(proj.projectId, secondUserId)
        def levelUser2 = skillsService.getUserLevel(proj.projectId, secondUserId)
        assert userInfo2.numSkills == 30
        assert userInfo2.userTotalPoints == 300
        assert levelUser2 == 5
        def badgesUser2 = skillsService.getBadgesSummary(secondUserId, proj.projectId)
        assert badgesUser2.size() == 1
        assert badgesUser2[0].badgeAchieved == true

        def addedSkillsUser2 = skillsService.getPerformedSkills(secondUserId, proj.projectId)
        assert addedSkillsUser2
        assert addedSkillsUser2.totalCount == 30

        def subjectSummary = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)
        def subjectSummaryUser2 = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)
        assert subjectSummary?.skills.size() == 30
        assert subjectSummaryUser2?.skills.size() == 30

        when:
        expireSkills(proj.projectId, skills)

        userInfo = skillsService.getUserStats(proj.projectId, userId)
        level = skillsService.getUserLevel(proj.projectId, userId)
        badges = skillsService.getBadgesSummary(userId, proj.projectId)
        addedSkills = skillsService.getPerformedSkills(userId, proj.projectId)

        userInfo2 = skillsService.getUserStats(proj.projectId, secondUserId)
        levelUser2 = skillsService.getUserLevel(proj.projectId, secondUserId)
        badgesUser2 = skillsService.getBadgesSummary(secondUserId, proj.projectId)
        addedSkillsUser2 = skillsService.getPerformedSkills(secondUserId, proj.projectId)

        then:
        userInfo.numSkills == 0
        userInfo.userTotalPoints == 0
        level == 0
        badges[0].badgeAchieved == false
        addedSkills.totalCount == 0

        userInfo2.numSkills == 0
        userInfo2.userTotalPoints == 0
        levelUser2 == 0
        badgesUser2[0].badgeAchieved == false
        addedSkillsUser2.totalCount == 0

    }

    def "expiring skill events propagates to imported projects -- all traces of a user are gone from the imported project"() {
        List<String> users = getRandomUsers(2)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def subj2 = createSubject(1, 2)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        def subj2_skills = SkillsFactory.createSkills(5, 1, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }
        subj2_skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj4 = createProject(4)
        def proj4_subj = createSubject(4, 3)
        def proj4_subj2 = createSubject(4, 4)
        skillsService.createProjectAndSubjectAndSkills(proj4, proj4_subj, [])
        skillsService.createSubject(proj4_subj2)
        skillsService.bulkImportSkillsFromCatalog(proj4.projectId, proj4_subj.subjectId, skills.collect { [projectId: proj.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(proj4.projectId, proj4_subj2.subjectId, subj2_skills.collect { [projectId: proj.projectId, skillId: it.skillId] })
        skillsService.finalizeSkillsImportFromCatalog(proj4.projectId)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[1], new Date())

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[1], new Date())

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        when:
        List<UserPerformedSkill> userPerformedSkills_t0 = userPerformedSkillRepo.findAll()
        List<UserPoints> userPoints_t0 = userPointsRepo.findAll()
        List<UserAchievement> userAchievements_t0 = userAchievedRepo.findAll()
        List<ExpiredUserAchievement> expiredUserAchievements_t0 = expiredUserAchievementRepo.findAll()

        expireAllSkillsForProject(proj.projectId)

        List<UserPerformedSkill> userPerformedSkills_t1 = userPerformedSkillRepo.findAll()
        List<UserPoints> userPoints_t1 = userPointsRepo.findAll()
        List<UserAchievement> userAchievements_t1 = userAchievedRepo.findAll()
        List<ExpiredUserAchievement> expiredUserAchievements_t1 = expiredUserAchievementRepo.findAll()

        then:
        // imported skills do not get their own copies for performed skill
        // but rather utilize the original from the exported project
        userPerformedSkills_t0.collect { "${it.userId}-${it.projectId}-${it.skillId}".toString() }.sort() == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}",

                "${users[1]}-${proj.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}",
        ].collect { it.toString() }.sort()

        userPoints_t0.collect { UserPoints up -> "${up.userId}-${up.skillId ? "${up.projectId}-${up.skillId}" : up.projectId}-${up.points}".toString()}.sort() == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}-100",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-200",

                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-300",
                "${users[0]}-${proj.projectId}-500",

                "${users[0]}-${proj4.projectId}-${skills[0].skillId}-100",
                "${users[0]}-${proj4.projectId}-${skills[1].skillId}-100",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-200",

                "${users[0]}-${proj4.projectId}-${subj2_skills[0].skillId}-100",
                "${users[0]}-${proj4.projectId}-${subj2_skills[1].skillId}-100",
                "${users[0]}-${proj4.projectId}-${subj2_skills[2].skillId}-100",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-300",
                "${users[0]}-${proj4.projectId}-500",


                "${users[1]}-${proj.projectId}-${skills[0].skillId}-100",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-200",

                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-300",
                "${users[1]}-${proj.projectId}-500",

                "${users[1]}-${proj4.projectId}-${skills[0].skillId}-100",
                "${users[1]}-${proj4.projectId}-${skills[1].skillId}-100",
                "${users[1]}-${proj4.projectId}-${proj4_subj.subjectId}-200",

                "${users[1]}-${proj4.projectId}-${subj2_skills[0].skillId}-100",
                "${users[1]}-${proj4.projectId}-${subj2_skills[1].skillId}-100",
                "${users[1]}-${proj4.projectId}-${subj2_skills[2].skillId}-100",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-300",
                "${users[1]}-${proj4.projectId}-500",
        ].collect { it.toString() }.sort ()

        userAchievements_t0.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-1",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-2",

                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-1",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-2",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-3",

                "${users[0]}-${proj.projectId}-1",
                "${users[0]}-${proj.projectId}-2",
                "${users[0]}-${proj.projectId}-3",

                "${users[0]}-${proj4.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-1",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-2",

                "${users[0]}-${proj4.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj4.projectId}-${subj2_skills[2].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-1",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-2",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-3",

                "${users[0]}-${proj4.projectId}-1",
                "${users[0]}-${proj4.projectId}-2",
                "${users[0]}-${proj4.projectId}-3",

                // user 2
                "${users[1]}-${proj.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-1",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-2",

                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-1",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-2",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-3",

                "${users[1]}-${proj.projectId}-1",
                "${users[1]}-${proj.projectId}-2",
                "${users[1]}-${proj.projectId}-3",

                "${users[1]}-${proj4.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj4.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj4.projectId}-${proj4_subj.subjectId}-1",
                "${users[1]}-${proj4.projectId}-${proj4_subj.subjectId}-2",

                "${users[1]}-${proj4.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj4.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj4.projectId}-${subj2_skills[2].skillId}",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-1",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-2",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-3",

                "${users[1]}-${proj4.projectId}-1",
                "${users[1]}-${proj4.projectId}-2",
                "${users[1]}-${proj4.projectId}-3",
        ].collect { it.toString() }.sort ()
        expiredUserAchievements_t0 == []

        // t1
        userPerformedSkills_t1 == []
        userPoints_t1 == []
        userAchievements_t1 == []

        expiredUserAchievements_t1.collect { ExpiredUserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}",

                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}",

                // user 2
                "${users[1]}-${proj.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}",

                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}",
        ].collect { it.toString() }.sort ()
    }

    def "expiring skill events propagates to imported projects"() {
        List<String> users = getRandomUsers(2)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def subj2 = createSubject(1, 2)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        def subj2_skills = SkillsFactory.createSkills(5, 1, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }
        subj2_skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj4 = createProject(4)
        def proj4_subj = createSubject(4, 3)
        def proj4_subj2 = createSubject(4, 4)
        def proj4_native_skills = [
                SkillsFactory.createSkill(4, 3, 20, 0, 1, 100, 200),
                SkillsFactory.createSkill(4, 3, 21, 0, 1, 100, 200),
                SkillsFactory.createSkill(4, 3, 22, 0, 1, 100, 200),
        ]
        skillsService.createProjectAndSubjectAndSkills(proj4, proj4_subj, proj4_native_skills)
        skillsService.createSubject(proj4_subj2)
        skillsService.bulkImportSkillsFromCatalog(proj4.projectId, proj4_subj.subjectId, skills.collect { [projectId: proj.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(proj4.projectId, proj4_subj2.subjectId, subj2_skills.collect { [projectId: proj.projectId, skillId: it.skillId] })
        skillsService.finalizeSkillsImportFromCatalog(proj4.projectId)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[1], new Date())

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[1], new Date())

        skillsService.addSkill([projectId: proj4.projectId, skillId: proj4_native_skills[0].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj4.projectId, skillId: proj4_native_skills[1].skillId], users[0], new Date())

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        when:
        List<UserPerformedSkill> userPerformedSkills_t0 = userPerformedSkillRepo.findAll()
        List<UserPoints> userPoints_t0 = userPointsRepo.findAll()
        List<UserAchievement> userAchievements_t0 = userAchievedRepo.findAll()
        List<ExpiredUserAchievement> expiredUserAchievements_t0 = expiredUserAchievementRepo.findAll()

        expireAllSkillsForProject(proj.projectId)

        List<UserPerformedSkill> userPerformedSkills_t1 = userPerformedSkillRepo.findAll()
        List<UserPoints> userPoints_t1 = userPointsRepo.findAll()
        List<UserAchievement> userAchievements_t1 = userAchievedRepo.findAll()
        List<ExpiredUserAchievement> expiredUserAchievements_t1 = expiredUserAchievementRepo.findAll()

        then:
        // imported skills do not get their own copies for performed skill
        // but rather utilize the original from the exported project
        userPerformedSkills_t0.collect { "${it.userId}-${it.projectId}-${it.skillId}".toString() }.sort() == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}",

                "${users[1]}-${proj.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}",

                "${users[0]}-${proj4.projectId}-${proj4_native_skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[1].skillId}",
        ].collect { it.toString() }.sort()

        userPoints_t0.collect { UserPoints up -> "${up.userId}-${up.skillId ? "${up.projectId}-${up.skillId}" : up.projectId}-${up.points}".toString()}.sort() == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}-100",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-200",

                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-300",
                "${users[0]}-${proj.projectId}-500",

                "${users[0]}-${proj4.projectId}-${skills[0].skillId}-100",
                "${users[0]}-${proj4.projectId}-${skills[1].skillId}-100",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[0].skillId}-200",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[1].skillId}-200",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-600",

                "${users[0]}-${proj4.projectId}-${subj2_skills[0].skillId}-100",
                "${users[0]}-${proj4.projectId}-${subj2_skills[1].skillId}-100",
                "${users[0]}-${proj4.projectId}-${subj2_skills[2].skillId}-100",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-300",
                "${users[0]}-${proj4.projectId}-900",


                "${users[1]}-${proj.projectId}-${skills[0].skillId}-100",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-200",

                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-300",
                "${users[1]}-${proj.projectId}-500",

                "${users[1]}-${proj4.projectId}-${skills[0].skillId}-100",
                "${users[1]}-${proj4.projectId}-${skills[1].skillId}-100",
                "${users[1]}-${proj4.projectId}-${proj4_subj.subjectId}-200",

                "${users[1]}-${proj4.projectId}-${subj2_skills[0].skillId}-100",
                "${users[1]}-${proj4.projectId}-${subj2_skills[1].skillId}-100",
                "${users[1]}-${proj4.projectId}-${subj2_skills[2].skillId}-100",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-300",
                "${users[1]}-${proj4.projectId}-500",
        ].collect { it.toString() }.sort ()

        userAchievements_t0.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-1",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-2",

                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-1",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-2",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-3",

                "${users[0]}-${proj.projectId}-1",
                "${users[0]}-${proj.projectId}-2",
                "${users[0]}-${proj.projectId}-3",

                "${users[0]}-${proj4.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[1].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-1",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-2",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-3",

                "${users[0]}-${proj4.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj4.projectId}-${subj2_skills[2].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-1",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-2",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-3",

                "${users[0]}-${proj4.projectId}-1",
                "${users[0]}-${proj4.projectId}-2",
                "${users[0]}-${proj4.projectId}-3",

                "${users[1]}-${proj.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-1",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-2",

                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-1",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-2",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-3",

                "${users[1]}-${proj.projectId}-1",
                "${users[1]}-${proj.projectId}-2",
                "${users[1]}-${proj.projectId}-3",

                "${users[1]}-${proj4.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj4.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj4.projectId}-${proj4_subj.subjectId}-1",

                "${users[1]}-${proj4.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj4.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj4.projectId}-${subj2_skills[2].skillId}",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-1",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-2",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-3",

                "${users[1]}-${proj4.projectId}-1",
                "${users[1]}-${proj4.projectId}-2",
        ].collect { it.toString() }.sort ()

        expiredUserAchievements_t0 == []


        // t1
        userPerformedSkills_t1.collect { "${it.userId}-${it.projectId}-${it.skillId}".toString() }.sort() == [
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[1].skillId}",
        ].collect { it.toString() }.sort()

        userPoints_t1.collect { UserPoints up -> "${up.userId}-${up.skillId ? "${up.projectId}-${up.skillId}" : up.projectId}-${up.points}".toString()}.sort() == [
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[0].skillId}-200",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[1].skillId}-200",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-400",
                "${users[0]}-${proj4.projectId}-400",
        ].collect { it.toString() }.sort ()

        userAchievements_t1.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[1].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-1",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-2",
                "${users[0]}-${proj4.projectId}-1",
                "${users[0]}-${proj4.projectId}-2",
        ].collect { it.toString() }.sort ()

        expiredUserAchievements_t1.collect { ExpiredUserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}",

                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}",

                "${users[1]}-${proj.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}",

                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}",
        ].collect { it.toString() }.sort ()
    }

    def "expiring skill events ignores imported skills"() {
        List<String> users = getRandomUsers(2)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def subj2 = createSubject(1, 2)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        def subj2_skills = SkillsFactory.createSkills(5, 1, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }
        subj2_skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj4 = createProject(4)
        def proj4_subj = createSubject(4, 3)
        def proj4_subj2 = createSubject(4, 4)
        def proj4_native_skills = [
                SkillsFactory.createSkill(4, 3, 20, 0, 1, 100, 200),
                SkillsFactory.createSkill(4, 3, 21, 0, 1, 100, 200),
                SkillsFactory.createSkill(4, 3, 22, 0, 1, 100, 200),
        ]
        skillsService.createProjectAndSubjectAndSkills(proj4, proj4_subj, proj4_native_skills)
        skillsService.createSubject(proj4_subj2)
        def proj4_group = SkillsFactory.createSkillsGroup(4, 3, 30)
        skillsService.createSkill(proj4_group)
        skillsService.bulkImportSkillsIntoGroupFromCatalog(proj4.projectId, proj4_subj.subjectId, proj4_group.skillId, skills[0..1].collect { [projectId: proj.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(proj4.projectId, proj4_subj.subjectId, skills[2..4].collect { [projectId: proj.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(proj4.projectId, proj4_subj2.subjectId, subj2_skills.collect { [projectId: proj.projectId, skillId: it.skillId] })
        skillsService.finalizeSkillsImportFromCatalog(proj4.projectId)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[1], new Date())

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[1], new Date())

        skillsService.addSkill([projectId: proj4.projectId, skillId: proj4_native_skills[0].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj4.projectId, skillId: proj4_native_skills[1].skillId], users[0], new Date())

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        when:
        List<UserPerformedSkill> userPerformedSkills_t0 = userPerformedSkillRepo.findAll()
        List<UserPoints> userPoints_t0 = userPointsRepo.findAll()
        List<UserAchievement> userAchievements_t0 = userAchievedRepo.findAll()
        List<ExpiredUserAchievement> expiredUserAchievements_t0 = expiredUserAchievementRepo.findAll()

        expireAllSkillsForProject(proj4.projectId)

        List<UserPerformedSkill> userPerformedSkills_t1 = userPerformedSkillRepo.findAll()
        List<UserPoints> userPoints_t1 = userPointsRepo.findAll()
        List<UserAchievement> userAchievements_t1 = userAchievedRepo.findAll()
        List<ExpiredUserAchievement> expiredUserAchievements_t1 = expiredUserAchievementRepo.findAll()

        then:
        // imported skills do not get their own copies for performed skill
        // but rather utilize the original from the exported project
        userPerformedSkills_t0.collect { "${it.userId}-${it.projectId}-${it.skillId}".toString() }.sort() == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}",

                "${users[1]}-${proj.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}",

                "${users[0]}-${proj4.projectId}-${proj4_native_skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[1].skillId}",
        ].collect { it.toString() }.sort()
        userAchievements_t0.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-1",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-2",

                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-1",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-2",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-3",

                "${users[0]}-${proj.projectId}-1",
                "${users[0]}-${proj.projectId}-2",
                "${users[0]}-${proj.projectId}-3",

                "${users[0]}-${proj4.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_group.skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[1].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-1",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-2",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-3",


                "${users[0]}-${proj4.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj4.projectId}-${subj2_skills[2].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-1",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-2",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-3",

                "${users[0]}-${proj4.projectId}-1",
                "${users[0]}-${proj4.projectId}-2",
                "${users[0]}-${proj4.projectId}-3",

                "${users[1]}-${proj.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj4.projectId}-${proj4_group.skillId}",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-1",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-2",

                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-1",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-2",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-3",

                "${users[1]}-${proj.projectId}-1",
                "${users[1]}-${proj.projectId}-2",
                "${users[1]}-${proj.projectId}-3",

                "${users[1]}-${proj4.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj4.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj4.projectId}-${proj4_subj.subjectId}-1",

                "${users[1]}-${proj4.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj4.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj4.projectId}-${subj2_skills[2].skillId}",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-1",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-2",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-3",

                "${users[1]}-${proj4.projectId}-1",
                "${users[1]}-${proj4.projectId}-2",
        ].collect { it.toString() }.sort ()

        userPoints_t0.collect { UserPoints up -> "${up.userId}-${up.skillId ? "${up.projectId}-${up.skillId}" : up.projectId}-${up.points}".toString()}.sort() == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}-100",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-200",

                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-300",
                "${users[0]}-${proj.projectId}-500",

                "${users[0]}-${proj4.projectId}-${skills[0].skillId}-100",
                "${users[0]}-${proj4.projectId}-${skills[1].skillId}-100",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[0].skillId}-200",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[1].skillId}-200",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-600",

                "${users[0]}-${proj4.projectId}-${subj2_skills[0].skillId}-100",
                "${users[0]}-${proj4.projectId}-${subj2_skills[1].skillId}-100",
                "${users[0]}-${proj4.projectId}-${subj2_skills[2].skillId}-100",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-300",
                "${users[0]}-${proj4.projectId}-900",


                "${users[1]}-${proj.projectId}-${skills[0].skillId}-100",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-200",

                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-300",
                "${users[1]}-${proj.projectId}-500",

                "${users[1]}-${proj4.projectId}-${skills[0].skillId}-100",
                "${users[1]}-${proj4.projectId}-${skills[1].skillId}-100",
                "${users[1]}-${proj4.projectId}-${proj4_subj.subjectId}-200",

                "${users[1]}-${proj4.projectId}-${subj2_skills[0].skillId}-100",
                "${users[1]}-${proj4.projectId}-${subj2_skills[1].skillId}-100",
                "${users[1]}-${proj4.projectId}-${subj2_skills[2].skillId}-100",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-300",
                "${users[1]}-${proj4.projectId}-500",
        ].collect { it.toString() }.sort ()

        expiredUserAchievements_t0 == []

        // t1
        userPerformedSkills_t1.collect { "${it.userId}-${it.projectId}-${it.skillId}".toString() }.sort() == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}",

                "${users[1]}-${proj.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}",

        ].collect { it.toString() }.sort()

        userPoints_t1.collect { UserPoints up -> "${up.userId}-${up.skillId ? "${up.projectId}-${up.skillId}" : up.projectId}-${up.points}".toString()}.sort() == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}-100",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-200",

                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}-100",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-300",
                "${users[0]}-${proj.projectId}-500",

                "${users[0]}-${proj4.projectId}-${skills[0].skillId}-100",
                "${users[0]}-${proj4.projectId}-${skills[1].skillId}-100",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-200",

                "${users[0]}-${proj4.projectId}-${subj2_skills[0].skillId}-100",
                "${users[0]}-${proj4.projectId}-${subj2_skills[1].skillId}-100",
                "${users[0]}-${proj4.projectId}-${subj2_skills[2].skillId}-100",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-300",
                "${users[0]}-${proj4.projectId}-500",


                "${users[1]}-${proj.projectId}-${skills[0].skillId}-100",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-200",

                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}-100",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-300",
                "${users[1]}-${proj.projectId}-500",

                "${users[1]}-${proj4.projectId}-${skills[0].skillId}-100",
                "${users[1]}-${proj4.projectId}-${skills[1].skillId}-100",
                "${users[1]}-${proj4.projectId}-${proj4_subj.subjectId}-200",

                "${users[1]}-${proj4.projectId}-${subj2_skills[0].skillId}-100",
                "${users[1]}-${proj4.projectId}-${subj2_skills[1].skillId}-100",
                "${users[1]}-${proj4.projectId}-${subj2_skills[2].skillId}-100",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-300",
                "${users[1]}-${proj4.projectId}-500",
        ].collect { it.toString() }.sort ()

        userAchievements_t1.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${users[0]}-${proj.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-1",
                "${users[0]}-${proj.projectId}-${subj.subjectId}-2",

                "${users[0]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj.projectId}-${subj2_skills[2].skillId}",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-1",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-2",
                "${users[0]}-${proj.projectId}-${subj2.subjectId}-3",

                "${users[0]}-${proj.projectId}-1",
                "${users[0]}-${proj.projectId}-2",
                "${users[0]}-${proj.projectId}-3",

                "${users[0]}-${proj4.projectId}-${skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${skills[1].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_group.skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_subj.subjectId}-1",

                "${users[0]}-${proj4.projectId}-${subj2_skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${subj2_skills[1].skillId}",
                "${users[0]}-${proj4.projectId}-${subj2_skills[2].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-1",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-2",
                "${users[0]}-${proj4.projectId}-${proj4_subj2.subjectId}-3",

                "${users[0]}-${proj4.projectId}-1",
                "${users[0]}-${proj4.projectId}-2",

                "${users[1]}-${proj.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-1",
                "${users[1]}-${proj.projectId}-${subj.subjectId}-2",

                "${users[1]}-${proj.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj.projectId}-${subj2_skills[2].skillId}",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-1",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-2",
                "${users[1]}-${proj.projectId}-${subj2.subjectId}-3",

                "${users[1]}-${proj.projectId}-1",
                "${users[1]}-${proj.projectId}-2",
                "${users[1]}-${proj.projectId}-3",

                "${users[1]}-${proj4.projectId}-${skills[0].skillId}",
                "${users[1]}-${proj4.projectId}-${skills[1].skillId}",
                "${users[1]}-${proj4.projectId}-${proj4_group.skillId}",
                "${users[1]}-${proj4.projectId}-${proj4_subj.subjectId}-1",

                "${users[1]}-${proj4.projectId}-${subj2_skills[0].skillId}",
                "${users[1]}-${proj4.projectId}-${subj2_skills[1].skillId}",
                "${users[1]}-${proj4.projectId}-${subj2_skills[2].skillId}",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-1",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-2",
                "${users[1]}-${proj4.projectId}-${proj4_subj2.subjectId}-3",

                "${users[1]}-${proj4.projectId}-1",
                "${users[1]}-${proj4.projectId}-2",
        ].collect { it.toString() }.sort ()

        expiredUserAchievements_t1.collect { ExpiredUserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[0].skillId}",
                "${users[0]}-${proj4.projectId}-${proj4_native_skills[1].skillId}",
        ].collect { it.toString() }.sort ()
    }

    def "expiring skill events propagates to imported projects -- badges are removed"() {
        List<String> users = getRandomUsers(2)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def subj2 = createSubject(1, 2)
        def skills = SkillsFactory.createSkills(5, 1, 1, 100)
        def subj2_skills = SkillsFactory.createSkills(5, 1, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skillsService.createSubject(subj2)
        skillsService.createSkills(subj2_skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }
        subj2_skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj4 = createProject(4)
        def proj4_subj = createSubject(4, 3)
        def proj4_subj2 = createSubject(4, 4)
        skillsService.createProjectAndSubjectAndSkills(proj4, proj4_subj, [])
        skillsService.createSubject(proj4_subj2)
        skillsService.bulkImportSkillsFromCatalog(proj4.projectId, proj4_subj.subjectId, skills.collect { [projectId: proj.projectId, skillId: it.skillId] })
        skillsService.bulkImportSkillsFromCatalog(proj4.projectId, proj4_subj2.subjectId, subj2_skills.collect { [projectId: proj.projectId, skillId: it.skillId] })
        skillsService.finalizeSkillsImportFromCatalog(proj4.projectId)

        def proj4_badge = SkillsFactory.createBadge(4, 1)
        skillsService.createBadge(proj4_badge)
        skillsService.assignSkillToBadge([projectId: proj4.projectId, badgeId: proj4_badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj4.projectId, badgeId: proj4_badge.badgeId, skillId: skills.get(1).skillId])
        proj4_badge.enabled = true
        skillsService.createBadge(proj4_badge)

        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[1], new Date())

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[1], new Date())

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        when:
        List<UserAchievement> userAchievements_t0 = userAchievedRepo.findAll()

        expireAllSkillsForProject(proj.projectId)

        List<UserAchievement> userAchievements_t1 = userAchievedRepo.findAll()

        then:
        userAchievements_t0.find { it.projectId == proj4.projectId && it.skillId == proj4_badge.badgeId && it.userId == users[0]}
        userAchievements_t0.find { it.projectId == proj4.projectId && it.skillId == proj4_badge.badgeId && it.userId == users[1]}

        !userAchievements_t1.find { it.projectId == proj4.projectId && it.skillId == proj4_badge.badgeId && it.userId == users[0]}
        !userAchievements_t1.find { it.projectId == proj4.projectId && it.skillId == proj4_badge.badgeId && it.userId == users[1]}
    }

    def "expire skill events when there's more than 1 event that falls within the configured time window"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2, 1, 1, 20 )
        skills[0].numPerformToCompletion = 5
        skills[0].numMaxOccurrencesIncrementInterval = 3

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = "user1"
        Long timestamp = new Date().time

        setup:
        def res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date(timestamp))
        def res2 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date(timestamp))
        when:
        def before = skillsService.getPerformedSkills(userId, proj.projectId)
        expireSkill(proj.projectId, skills[0])
        def after = skillsService.getPerformedSkills(userId, proj.projectId)

        then:
        res1.body.skillApplied
        res2.body.skillApplied

        before.data.size() == 2
        after.data.size() == 0
    }

    def "attempt to expire skill event that doesn't exist"() {
        String subj = "testSubj"
        String skillId = "skillId"
        String userId = "user1"
        Long timestamp = new Date().time

        setup:
        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createProject([projectId: "otherProjId", name: "Other Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill([projectId: projId, subjectId: subj, skillId: skillId, name: "Test Skill", type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1,
        ])
        def res = skillsService.addSkill([projectId: projId, skillId: skillId], userId, new Date(timestamp))

        assert res.body.skillApplied
        assert res.body.explanation == "Skill event was applied"

        def addedSkills = skillsService.getPerformedSkills(userId, projId)
        assert addedSkills
        assert addedSkills.data.find { it.skillId == skillId}

        when:
        expireSkill("otherProjId", [skillId: skillId])
        then:
        SkillsClientException clientException = thrown()
        clientException.httpStatus == HttpStatus.BAD_REQUEST
        clientException.message.contains("Failed to find skillId [skillId] for [otherProjId] with type [Skill]")
    }

    def "expiring skill event required for a badge will remove the achieved badge"() {
        String userId = "user1"
        Date date = new Date()

        String subj = "testSubj"
        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: projId, subjectId: subj, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1']
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId, skill4.skillId]


        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createBadge(badge)
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }
        badge = skillsService.getBadge(badge)
        badge.enabled = 'true'
        skillsService.createBadge(badge)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, date).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId], userId, date).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId], userId, date).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId], userId, date).body

        assert resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
        assert resSkill2.skillApplied && !resSkill2.completed.find { it.id == 'badge1'}
        assert resSkill3.skillApplied && !resSkill3.completed.find { it.id == 'badge1'}
        assert resSkill4.skillApplied && resSkill4.completed.find { it.id == 'badge1'}

        when:
        def addedSkills = skillsService.getPerformedSkills(userId, projId)
        assert addedSkills?.count == 4
        def badgesSummary = skillsService.getBadgesSummary(userId, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()
        assert badgesSummary.badgeId == 'badge1'
        assert badgesSummary.badgeAchieved == true
        assert badgesSummary.numSkillsAchieved == 4
        assert badgesSummary.numTotalSkills == 4

        expireSkill(projId, skill3)
        addedSkills = skillsService.getPerformedSkills(userId, projId)
        badgesSummary = skillsService.getBadgesSummary(userId, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()

        then:
        badgesSummary.badgeId == 'badge1'
        badgesSummary.badgeAchieved == false
        badgesSummary.numSkillsAchieved == 3
        badgesSummary.numTotalSkills == 4
        addedSkills?.count == 3
    }

    def "expiring a skill event tied to a global badge will remove the global badges"() {
        String userId = "user1"
        Date date = new Date()

        String subj = "testSubj"
        String subj2 = "testSubj2"
        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: "proj2", subjectId: subj2, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 200, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Global Badge 1']
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId]

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createProject([projectId: "proj2", name: "Test Project 2"])
        skillsService.createSubject([projectId: "proj2", subjectId: subj2, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createGlobalBadge(badge)
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }
        skillsService.assignSkillToGlobalBadge(projectId: "proj2", badgeId: badge.badgeId, skillId: skill4.skillId)
        badge = skillsService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'true'

        skillsService.createGlobalBadge(badge)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, date).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId], userId, date).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId], userId, date).body
        def resSkill4 = skillsService.addSkill([projectId: "proj2", skillId: skill4.skillId], userId, date).body

        assert resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
        assert resSkill2.skillApplied && !resSkill2.completed.find { it.id == 'badge1'}
        assert resSkill3.skillApplied && !resSkill3.completed.find { it.id == 'badge1'}
        assert resSkill4.skillApplied && resSkill4.completed.find { it.id == 'badge1'}

        when:
        def addedSkills = skillsService.getPerformedSkills(userId, projId)
        def proj2AddedSkills = skillsService.getPerformedSkills(userId, "proj2")
        assert addedSkills?.count == 3
        assert proj2AddedSkills?.count == 1
        def badgesSummary = skillsService.getBadgesSummary(userId, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()
        assert badgesSummary.badgeId == 'badge1'
        assert badgesSummary.badgeAchieved == true
        assert badgesSummary.numSkillsAchieved == 4
        assert badgesSummary.numTotalSkills == 4

        expireSkill(projId, skill2)
        addedSkills = skillsService.getPerformedSkills(userId, projId)
        badgesSummary = skillsService.getBadgesSummary(userId, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()

        then:
        badgesSummary.badgeId == 'badge1'
        badgesSummary.numSkillsAchieved == 3
        badgesSummary.numTotalSkills == 4
        badgesSummary.badgeAchieved == false
        addedSkills?.count == 2
    }

    def "expiring a skill event tied to a global badge that uses levels will remove the global badges"() {
        String userId = "user1"
        Date date = new Date()

        String subj = "testSubj"
        String subj2 = "testSubj2"
        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: "proj2", subjectId: subj2, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 200, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Global Badge 1']
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId]

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createProject([projectId: "proj2", name: "Test Project 2"])
        skillsService.createSubject([projectId: "proj2", subjectId: subj2, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "1")
        badge = skillsService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'true'
        skillsService.updateGlobalBadge(badge)
        skillsService.createGlobalBadge(badge)

        skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, date).body
        skillsService.addSkill([projectId: projId, skillId: skill3.skillId], userId, date).body
        skillsService.addSkill([projectId: projId, skillId: skill2.skillId], userId, date).body
        skillsService.addSkill([projectId: "proj2", skillId: skill4.skillId], userId, date).body

        when:
        def level = skillsService.getUserLevel(projId, userId)
        def addedSkills = skillsService.getPerformedSkills(userId, projId)
        def proj2AddedSkills = skillsService.getPerformedSkills(userId, "proj2")
        assert addedSkills?.count == 3
        assert proj2AddedSkills?.count == 1
        def badgesSummary = skillsService.getBadgesSummary(userId, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()
        assert badgesSummary.badgeId == 'badge1'
        assert badgesSummary.badgeAchieved == true
        assert level > 0

        expireSkills(projId, [skill1, skill2, skill3])
        badgesSummary = skillsService.getBadgesSummary(userId, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()
        level = skillsService.getUserLevel(projId, userId)
        assert level == 0

        then:

        badgesSummary.badgeId == 'badge1'
        badgesSummary.badgeAchieved == false
    }

    def "expiring all skill events will remove any achieved global badges"() {
        String userId = "user1"
        Date date = new Date()

        String subj = "testSubj"
        String subj2 = "testSubj2"
        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: "proj2", subjectId: subj2, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 200, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Global Badge 1']
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId]

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createProject([projectId: "proj2", name: "Test Project 2"])
        skillsService.createSubject([projectId: "proj2", subjectId: subj2, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createGlobalBadge(badge)
        requiredSkillsIds.each { skillId ->
            skillsService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }
        skillsService.assignSkillToGlobalBadge(projectId: "proj2", badgeId: badge.badgeId, skillId: skill4.skillId)
        badge = skillsService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'true'

        skillsService.createGlobalBadge(badge)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, date).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId], userId, date).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId], userId, date).body
        def resSkill4 = skillsService.addSkill([projectId: "proj2", skillId: skill4.skillId], userId, date).body

        assert resSkill1.skillApplied && !resSkill1.completed.find { it.id == 'badge1'}
        assert resSkill2.skillApplied && !resSkill2.completed.find { it.id == 'badge1'}
        assert resSkill3.skillApplied && !resSkill3.completed.find { it.id == 'badge1'}
        assert resSkill4.skillApplied && resSkill4.completed.find { it.id == 'badge1'}

        when:
        def addedSkills = skillsService.getPerformedSkills(userId, projId)
        def proj2AddedSkills = skillsService.getPerformedSkills(userId, "proj2")
        assert addedSkills?.count == 3
        assert proj2AddedSkills?.count == 1
        def badgesSummary = skillsService.getBadgesSummary(userId, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()
        assert badgesSummary.badgeId == 'badge1'
        assert badgesSummary.badgeAchieved == true
        assert badgesSummary.numSkillsAchieved == 4
        assert badgesSummary.numTotalSkills == 4

        expireAllSkillsForProject(projId)

        addedSkills = skillsService.getPerformedSkills(userId, projId)
        badgesSummary = skillsService.getBadgesSummary(userId, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()

        then:
        badgesSummary.badgeId == 'badge1'
        badgesSummary.badgeAchieved == false
        badgesSummary.numSkillsAchieved == 1
        badgesSummary.numTotalSkills == 4
        addedSkills?.count == 0
    }

    def "expiring all skill events tied to a global badge that uses levels will remove the global badges"() {
        String userId = "user1"
        Date date = new Date()

        String subj = "testSubj"
        String subj2 = "testSubj2"
        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 25, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill2 = [projectId: projId, subjectId: subj, skillId: "skill2", name  : "Test Skill 2", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill3 = [projectId: projId, subjectId: subj, skillId: "skill3", name  : "Test Skill 3", type: "Skill",
                      pointIncrement: 50, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map skill4 = [projectId: "proj2", subjectId: subj2, skillId: "skill4", name  : "Test Skill 4", type: "Skill",
                      pointIncrement: 200, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Global Badge 1']
        List<String> requiredSkillsIds = [skill1.skillId, skill2.skillId, skill3.skillId]

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createProject([projectId: "proj2", name: "Test Project 2"])
        skillsService.createSubject([projectId: "proj2", subjectId: subj2, name: "Test Subject"])
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)
        skillsService.createSkill(skill3)
        skillsService.createSkill(skill4)
        skillsService.createGlobalBadge(badge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "1")
        badge = skillsService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'true'
        skillsService.updateGlobalBadge(badge)
        skillsService.createGlobalBadge(badge)

        skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, date).body
        skillsService.addSkill([projectId: projId, skillId: skill3.skillId], userId, date).body
        skillsService.addSkill([projectId: projId, skillId: skill2.skillId], userId, date).body
        skillsService.addSkill([projectId: "proj2", skillId: skill4.skillId], userId, date).body

        when:
        def level = skillsService.getUserLevel(projId, userId)
        def addedSkills = skillsService.getPerformedSkills(userId, projId)
        def proj2AddedSkills = skillsService.getPerformedSkills(userId, "proj2")
        assert addedSkills?.count == 3
        assert proj2AddedSkills?.count == 1
        def badgesSummary = skillsService.getBadgesSummary(userId, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()
        assert badgesSummary.badgeId == 'badge1'
        assert badgesSummary.badgeAchieved == true
        assert level > 0

        expireAllSkillsForProject(projId)
        badgesSummary = skillsService.getBadgesSummary(userId, projId)
        assert badgesSummary.size() == 1
        badgesSummary = badgesSummary.first()
        level = skillsService.getUserLevel(projId, userId)
        assert level == 0

        then:

        badgesSummary.badgeId == 'badge1'
        badgesSummary.badgeAchieved == false
    }

    def "expiring skill event should remove level achievements" () {
        String userId = "user1"
        Date date = new Date()

        String subj = "testSubj"
        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        def subjSummaryPreAddSkill = skillsService.getSkillSummary(userId, projId, subj)
        skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, date)
        def subjSummaryPostAddSkillEvent = skillsService.getSkillSummary(userId, projId, subj)

        when:
        expireSkill(projId, skill1)
        def subjSummaryPostDelete = skillsService.getSkillSummary(userId, projId, subj)

        then:
        subjSummaryPreAddSkill.skillsLevel == 0
        subjSummaryPreAddSkill.skills[0].points == 0
        subjSummaryPostAddSkillEvent.skillsLevel == 5
        subjSummaryPostAddSkillEvent.skills[0].points == 100
        subjSummaryPostDelete.skillsLevel == 0
        subjSummaryPostDelete.skills[0].points == 0
    }

    def "cannot expire skill event for skill imported from catalog"() {
        def user = getRandomUsers(1)[0]
        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def subj1 = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(2, 2)

        def skill1 = SkillsFactory.createSkill(1, 1, 1, 1, 1, 0, 100)
        def skill2 = SkillsFactory.createSkill(2, 2, 2, 1, 1, 0, 100)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkill(skill1)
        skillsService.createSkill(skill2)

        when:
        skillsService.exportSkillToCatalog(proj1.projectId, skill1.skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skill1.skillId)
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId, true)
        Date date = new Date()
        skillsService.addSkill([projectId: proj1.projectId, skillId: skill1.skillId], user, date)
        skillsService.addSkill([projectId: proj2.projectId, skillId: skill2.skillId], user, date)

        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        // bypass endpoint validation for testing purposes, show never get in this state though
        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        skillAttributeService.saveAttrs(proj2.projectId, skill1.skillId, SkillAttributesDef.SkillAttributesType.AchievementExpiration, new ExpirationAttrs(
                expirationType    : ExpirationAttrs.YEARLY,
                every             : 1,
                monthlyDay        : expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ))
        skillsService.saveSkillExpirationAttributes(proj2.projectId, skill2.skillId, [
                expirationType: ExpirationAttrs.YEARLY,
                every: 1,
                monthlyDay: expirationDate.dayOfMonth,
                nextExpirationDate: expirationDate.toDate(),
        ])

        def userAchievements_t0 = userAchievedRepo.findAll()
        def expiredUserAchievements_t0 = expiredUserAchievementRepo.findAll()

        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        def userAchievements_t1 = userAchievedRepo.findAll()
        def expiredUserAchievements_t1 = expiredUserAchievementRepo.findAll()

        then:
        // instead of failing the batch, the executor will log the error and move on to the next
        // imported skill achievements are not removed or moved to the expired table
        // other skills continue to be processed/expired even though the exception occurred
        !expiredUserAchievements_t0
        expiredUserAchievements_t1.size() == 1 && expiredUserAchievements_t1.find { it.skillId == skill2.skillId }

        userAchievements_t0.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [

                "${user}-${proj1.projectId}-1",
                "${user}-${proj1.projectId}-2",
                "${user}-${proj1.projectId}-3",
                "${user}-${proj1.projectId}-4",
                "${user}-${proj1.projectId}-5",

                "${user}-${proj1.projectId}-${subj1.subjectId}-1",
                "${user}-${proj1.projectId}-${subj1.subjectId}-2",
                "${user}-${proj1.projectId}-${subj1.subjectId}-3",
                "${user}-${proj1.projectId}-${subj1.subjectId}-4",
                "${user}-${proj1.projectId}-${subj1.subjectId}-5",

                "${user}-${proj1.projectId}-${skill1.skillId}",

                "${user}-${proj2.projectId}-1",
                "${user}-${proj2.projectId}-2",
                "${user}-${proj2.projectId}-3",
                "${user}-${proj2.projectId}-4",
                "${user}-${proj2.projectId}-5",

                "${user}-${proj2.projectId}-${subj2.subjectId}-1",
                "${user}-${proj2.projectId}-${subj2.subjectId}-2",
                "${user}-${proj2.projectId}-${subj2.subjectId}-3",
                "${user}-${proj2.projectId}-${subj2.subjectId}-4",
                "${user}-${proj2.projectId}-${subj2.subjectId}-5",

                "${user}-${proj2.projectId}-${skill1.skillId}",
                "${user}-${proj2.projectId}-${skill2.skillId}",
        ].collect { it.toString() }.sort ()

        userAchievements_t1.collect { UserAchievement ua -> "${ua.userId}-${ua.skillId ? "${ua.projectId}-${ua.skillId}" : ua.projectId}${ua.level ? "-$ua.level" : ""}".toString() }.sort () == [

                "${user}-${proj1.projectId}-1",
                "${user}-${proj1.projectId}-2",
                "${user}-${proj1.projectId}-3",
                "${user}-${proj1.projectId}-4",
                "${user}-${proj1.projectId}-5",

                "${user}-${proj1.projectId}-${subj1.subjectId}-1",
                "${user}-${proj1.projectId}-${subj1.subjectId}-2",
                "${user}-${proj1.projectId}-${subj1.subjectId}-3",
                "${user}-${proj1.projectId}-${subj1.subjectId}-4",
                "${user}-${proj1.projectId}-${subj1.subjectId}-5",

                "${user}-${proj1.projectId}-${skill1.skillId}",

                "${user}-${proj2.projectId}-1",
                "${user}-${proj2.projectId}-2",
                "${user}-${proj2.projectId}-3",

                "${user}-${proj2.projectId}-${subj2.subjectId}-1",
                "${user}-${proj2.projectId}-${subj2.subjectId}-2",
                "${user}-${proj2.projectId}-${subj2.subjectId}-3",

                "${user}-${proj2.projectId}-${skill1.skillId}",
        ].collect { it.toString() }.sort ()
    }

    def expireAllSkillsForProject(String projectId, LocalDateTime expirationDate=null,excludeImportedSkills = true, boolean includeDisabled = false, boolean excludeReusedSkills = true) {
        def skills = skillsService.getSkillsForProject(projectId, "", excludeImportedSkills, includeDisabled, excludeReusedSkills)
        expireSkills(projectId, skills, expirationDate)
    }

    def expireSkill(String projectId, def skill, LocalDateTime expirationDate=null) {
        expireSkills(projectId, [skill], expirationDate)
    }

    def expireSkills(String projectId, def skills, LocalDateTime expirationDate=null) {
        if (!expirationDate) {
            expirationDate = (new Date()-1).toLocalDateTime() // yesterday
        }
        skills.forEach { it ->
            skillsService.saveSkillExpirationAttributes(projectId, it.skillId, [
                    expirationType: ExpirationAttrs.YEARLY,
                    every: 1,
                    monthlyDay: expirationDate.dayOfMonth,
                    nextExpirationDate: expirationDate.toDate(),
            ])
        }
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()
    }
}
