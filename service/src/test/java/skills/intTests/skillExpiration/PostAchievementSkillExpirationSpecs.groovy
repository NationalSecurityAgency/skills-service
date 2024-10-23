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

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.services.attributes.ExpirationAttrs
import skills.storage.model.ExpiredUserAchievement
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.model.UserQuizAttempt
import skills.storage.repos.ExpiredUserAchievementRepo
import skills.tasks.executors.ExpireUserAchievementsTaskExecutor

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

import static skills.intTests.utils.SkillsFactory.*

class PostAchievementSkillExpirationSpecs extends DefaultIntSpec {

    DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").withZoneUTC()

    String projId = SkillsFactory.defaultProjId

    List<String> sampleUserIds // loaded from system props

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
        Long timestamp = (new Date()-8).time

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
        Long timestamp = (new Date()-8).time

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

    def "retain skill that would otherwise expire"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )
        skills = [SkillsFactory.createSkill(1, 1, 1, 0, 1, 0, 100,)]

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = "user1"
        Date eightDaysAgo = (new Date()-8)
        Date sixDaysAgo = (new Date()-6)

        setup:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, eightDaysAgo)

        assert res.body.skillApplied
        assert res.body.explanation == "Skill event was applied"

        assert res.body.completed.size() == 11
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

        def userPeformedSkills_t0 = userPerformedSkillRepo.findAll()
        def userAchievements_t0 = userAchievedRepo.findAll()
        def expiredUserAchievements_t0 = expiredUserAchievementRepo.findAll()

        when:
        skillsService.saveSkillExpirationAttributes( proj.projectId, skills[0].skillId, [
                expirationType: ExpirationAttrs.DAILY,
                every: 7,
        ])

        res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, sixDaysAgo)
        assert res.body.skillApplied
        assert res.body.explanation == "Skill Achievement retained"

        def userPeformedSkills_t1 = userPerformedSkillRepo.findAll()

        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        addedSkills = skillsService.getPerformedSkills(userId, proj.projectId)
        def userPeformedSkills_t2 = userPerformedSkillRepo.findAll()
        def userAchievements_t1 = userAchievedRepo.findAll()
        def expiredUserAchievements_t1 = expiredUserAchievementRepo.findAll()

        then:
        addedSkills?.data?.find { it.skillId == skills[0].skillId }
        userPeformedSkills_t0.find { it.skillId == skills[0].skillId && it.performedOn == eightDaysAgo }
        userAchievements_t0.find { it.skillId == skills[0].skillId }
        !expiredUserAchievements_t0.find { it.skillId == skills[0].skillId }

        userPeformedSkills_t1.find { it.skillId == skills[0].skillId && it.performedOn == sixDaysAgo }
        userPeformedSkills_t2.find { it.skillId == skills[0].skillId && it.performedOn == sixDaysAgo }
        userAchievements_t1.find { it.skillId == skills[0].skillId }
        !expiredUserAchievements_t1.find { it.skillId == skills[0].skillId }
    }

    def "reporting a skill with a date older then the expiration date does NOT retain skill and the skill will still expire"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )
        skills = [SkillsFactory.createSkill(1, 1, 1, 0, 1, 0, 100,)]

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = "user1"
        Date eightDaysAgo = (new Date()-8)
        Date nineDaysAgo = (new Date()-9)

        setup:
        def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, eightDaysAgo)

        assert res.body.skillApplied
        assert res.body.explanation == "Skill event was applied"

        assert res.body.completed.size() == 11
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

        def userPeformedSkills_t0 = userPerformedSkillRepo.findAll()
        def userAchievements_t0 = userAchievedRepo.findAll()
        def expiredUserAchievements_t0 = expiredUserAchievementRepo.findAll()

        when:
        skillsService.saveSkillExpirationAttributes( proj.projectId, skills[0].skillId, [
                expirationType: ExpirationAttrs.DAILY,
                every: 7,
        ])

        res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, nineDaysAgo)
        assert !res.body.skillApplied
        assert res.body.explanation == "This skill reached its maximum points"

        def userPeformedSkills_t1 = userPerformedSkillRepo.findAll()

        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()

        addedSkills = skillsService.getPerformedSkills(userId, proj.projectId)
        def userPeformedSkills_t2 = userPerformedSkillRepo.findAll()
        def userAchievements_t1 = userAchievedRepo.findAll()
        def expiredUserAchievements_t1 = expiredUserAchievementRepo.findAll()

        then:
        addedSkills.totalCount == 0
        userPeformedSkills_t0.find { it.skillId == skills[0].skillId && it.performedOn == eightDaysAgo }
        userAchievements_t0.find { it.skillId == skills[0].skillId }
        !expiredUserAchievements_t0.find { it.skillId == skills[0].skillId }

        userPeformedSkills_t1.find { it.skillId == skills[0].skillId && it.performedOn == eightDaysAgo }  // older date was reported, so performedOn was not changed

        !userPeformedSkills_t2.find { it.skillId == skills[0].skillId }
        !userAchievements_t1.find { it.skillId == skills[0].skillId }
        expiredUserAchievements_t1.find { it.skillId == skills[0].skillId }
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

        Date eightDaysAgo = new Date()-8
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[1], eightDaysAgo)

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[1], eightDaysAgo)

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

        Date eightDaysAgo = new Date()-8
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[1], eightDaysAgo)

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[1], eightDaysAgo)

        skillsService.addSkill([projectId: proj4.projectId, skillId: proj4_native_skills[0].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj4.projectId, skillId: proj4_native_skills[1].skillId], users[0], eightDaysAgo)

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

        Date eightDaysAgo = new Date()-8
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[1], eightDaysAgo)

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[1], eightDaysAgo)

        skillsService.addSkill([projectId: proj4.projectId, skillId: proj4_native_skills[0].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj4.projectId, skillId: proj4_native_skills[1].skillId], users[0], eightDaysAgo)

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

        Date eightDaysAgo = new Date()-8
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[1].skillId], users[1], eightDaysAgo)

        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[0], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[0].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[1].skillId], users[1], eightDaysAgo)
        skillsService.addSkill([projectId: proj.projectId, skillId: subj2_skills[2].skillId], users[1], eightDaysAgo)

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

    def "expire skill events after achievement will NOT expire events, points and achievements if the skill has yet been achieved"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2, 1, 1, 20 )
        skills[0].numPerformToCompletion = 5
        skills[0].numMaxOccurrencesIncrementInterval = 3

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = "user1"

        setup:
        def res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date()-8)
        def res2 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date()-9)
        def res3 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date()-10)
        def res4 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date()-11)
//        def res5 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date()-12)
        when:
        def before = skillsService.getPerformedSkills(userId, proj.projectId)
        List<UserAchievement> userAchievements_t0 = userAchievedRepo.findAll()

        expireSkill(proj.projectId, skills[0])

        def after = skillsService.getPerformedSkills(userId, proj.projectId)
        List<UserAchievement> userAchievements_t1 = userAchievedRepo.findAll()


        then:
        res1.body.skillApplied
        res2.body.skillApplied
        res3.body.skillApplied
        res4.body.skillApplied

        before.data.size() == 4
        !userAchievements_t0.find { it.projectId == proj.projectId && it.skillId == skills[0].skillId && it.userId == userId}

        after.data.size() == 4
        !userAchievements_t1.find { it.projectId == proj.projectId && it.skillId == skills[0].skillId && it.userId == userId}
    }

    def "expire skill events when there's more than 1 event that falls within the configured time window"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2, 1, 1, 20 )
        skills[0].numPerformToCompletion = 5
        skills[0].numMaxOccurrencesIncrementInterval = 5

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = "user1"
        Date eightDaysAgo = new Date()-8

        setup:
        def res1 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date()-8)
        def res2 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date()-9)
        def res3 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date()-10)
        def res4 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date()-11)
        def res5 = skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date()-12)

        when:
        def before = skillsService.getPerformedSkills(userId, proj.projectId)
        List<UserAchievement> userAchievements_t0 = userAchievedRepo.findAll()

        expireSkill(proj.projectId, skills[0])

        def after = skillsService.getPerformedSkills(userId, proj.projectId)
        List<UserAchievement> userAchievements_t1 = userAchievedRepo.findAll()

        then:
        res1.body.skillApplied
        res2.body.skillApplied
        res3.body.skillApplied
        res4.body.skillApplied
        res5.body.skillApplied

        before.data.size() == 5
        userAchievements_t0.find { it.projectId == proj.projectId && it.skillId == skills[0].skillId && it.userId == userId}

        after.data.size() == 0
        !userAchievements_t1.find { it.projectId == proj.projectId && it.skillId == skills[0].skillId && it.userId == userId}
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
        Date eightDaysAgo = new Date()-8

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

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, eightDaysAgo).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId], userId, eightDaysAgo).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId], userId, eightDaysAgo).body
        def resSkill4 = skillsService.addSkill([projectId: projId, skillId: skill4.skillId], userId, eightDaysAgo).body

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
        SkillsService supervisorService = createSupervisor()
        String userId = "user1"
        Date eightDaysAgo = new Date()-8

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
        supervisorService.createGlobalBadge(badge)
        requiredSkillsIds.each { skillId ->
            supervisorService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }
        supervisorService.assignSkillToGlobalBadge(projectId: "proj2", badgeId: badge.badgeId, skillId: skill4.skillId)
        badge = supervisorService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'true'

        supervisorService.createGlobalBadge(badge)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, eightDaysAgo).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId], userId, eightDaysAgo).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId], userId, eightDaysAgo).body
        def resSkill4 = skillsService.addSkill([projectId: "proj2", skillId: skill4.skillId], userId, eightDaysAgo).body

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
        SkillsService supervisorService = createSupervisor()
        String userId = "user1"
        Date eightDaysAgo = new Date()-8

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
        supervisorService.createGlobalBadge(badge)
        supervisorService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "1")
        badge = supervisorService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'true'
        supervisorService.updateGlobalBadge(badge)
        supervisorService.createGlobalBadge(badge)

        skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, eightDaysAgo).body
        skillsService.addSkill([projectId: projId, skillId: skill3.skillId], userId, eightDaysAgo).body
        skillsService.addSkill([projectId: projId, skillId: skill2.skillId], userId, eightDaysAgo).body
        skillsService.addSkill([projectId: "proj2", skillId: skill4.skillId], userId, eightDaysAgo).body

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
        SkillsService supervisorService = createSupervisor()
        String userId = "user1"
        Date eightDaysAgo = new Date()-8

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
        supervisorService.createGlobalBadge(badge)
        requiredSkillsIds.each { skillId ->
            supervisorService.assignSkillToGlobalBadge(projectId: projId, badgeId: badge.badgeId, skillId: skillId)
        }
        supervisorService.assignSkillToGlobalBadge(projectId: "proj2", badgeId: badge.badgeId, skillId: skill4.skillId)
        badge = supervisorService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'true'

        supervisorService.createGlobalBadge(badge)

        def resSkill1 = skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, eightDaysAgo).body
        def resSkill3 = skillsService.addSkill([projectId: projId, skillId: skill3.skillId], userId, eightDaysAgo).body
        def resSkill2 = skillsService.addSkill([projectId: projId, skillId: skill2.skillId], userId, eightDaysAgo).body
        def resSkill4 = skillsService.addSkill([projectId: "proj2", skillId: skill4.skillId], userId, eightDaysAgo).body

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
        SkillsService supervisorService = createSupervisor()
        String userId = "user1"
        Date eightDaysAgo = new Date()-8

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
        supervisorService.createGlobalBadge(badge)
        supervisorService.assignProjectLevelToGlobalBadge(projectId: projId, badgeId: badge.badgeId, level: "1")
        badge = supervisorService.getGlobalBadge(badge.badgeId)
        badge.enabled = 'true'
        supervisorService.updateGlobalBadge(badge)
        supervisorService.createGlobalBadge(badge)

        skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, eightDaysAgo).body
        skillsService.addSkill([projectId: projId, skillId: skill3.skillId], userId, eightDaysAgo).body
        skillsService.addSkill([projectId: projId, skillId: skill2.skillId], userId, eightDaysAgo).body
        skillsService.addSkill([projectId: "proj2", skillId: skill4.skillId], userId, eightDaysAgo).body

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
        Date eightDaysAgo = new Date()-8

        String subj = "testSubj"
        Map skill1 = [projectId: projId, subjectId: subj, skillId: "skill1", name  : "Test Skill 1", type: "Skill",
                      pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]

        skillsService.createProject([projectId: projId, name: "Test Project"])
        skillsService.createSubject([projectId: projId, subjectId: subj, name: "Test Subject"])
        skillsService.createSkill(skill1)
        def subjSummaryPreAddSkill = skillsService.getSkillSummary(userId, projId, subj)
        skillsService.addSkill([projectId: projId, skillId: skill1.skillId], userId, eightDaysAgo)
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

    def "cannot configure skill expiration for skills imported from catalog"() {
        def user = getRandomUsers(1)[0]
        def proj1 = SkillsFactory.createProject(1)
        def proj2 = SkillsFactory.createProject(2)
        def subj1 = SkillsFactory.createSubject(1, 1)
        def subj2 = SkillsFactory.createSubject(2, 2)

        def skill1 = SkillsFactory.createSkill(1, 1, 1, 1, 2, 0, 100)

        skillsService.createProject(proj1)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)
        skillsService.createSkill(skill1)

        when:
        skillsService.exportSkillToCatalog(proj1.projectId, skill1.skillId)
        skillsService.importSkillFromCatalog(proj2.projectId, subj2.subjectId, proj1.projectId, skill1.skillId)
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId, true)

        skillsService.saveSkillExpirationAttributes(proj2.projectId, skill1.skillId, [
                expirationType: ExpirationAttrs.DAILY,
                every: 14,
        ])

        then:
        def e = thrown(Exception)
        e.getMessage().contains("Cannot configure expiration attribute on skills imported from the catalog")
    }

    def "cannot configure skill expiration for skills re-used in the same project"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        when:

        LocalDateTime expirationDate = (new Date() - 1).toLocalDateTime() // yesterday
        skillsService.saveSkillExpirationAttributes(p1.projectId, 'skill1STREUSESKILLST0', [
                expirationType: ExpirationAttrs.DAILY,
                every: 10,
        ])

        then:
        def e = thrown(Exception)
        e.getMessage().contains("Cannot configure expiration attribute on skills that are reused")
    }

    def "skill summary reflect expiration time relative to the next expiration task exeution time"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = "user1"
        Date yesterday = (new Date()-1)
        Date tomorrow = (new Date()+1)

        setup:
        (0..2).each {
            skillsService.saveSkillExpirationAttributes( proj.projectId, skills[it].skillId, [
                    expirationType: ExpirationAttrs.DAILY,
                    every: it+1,
            ])

            def res = skillsService.addSkill([projectId: proj.projectId, skillId: skills[it].skillId], userId, yesterday)
            assert res.body.skillApplied
            assert res.body.explanation == "Skill event was applied"

            assert res.body.completed.find({ it.type == "Skill" }).id == skills[it].skillId
            assert res.body.completed.find({ it.type == "Skill" }).name == skills[it].name

            def addedSkills = skillsService.getPerformedSkills(userId, proj.projectId)
            assert addedSkills
            assert addedSkills.data.find { it2 -> it2.skillId == skills[it].skillId }
        }

        Instant now = Instant.now()
        Boolean before1am = now.atOffset(ZoneOffset.UTC).getHour() < 1
        Instant nextExpirationRun
        if (before1am) {
            nextExpirationRun = now.atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).withHour(1).toInstant()
        } else {
            nextExpirationRun = tomorrow.toInstant().atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).withHour(1).toInstant()
        }

        when:

        def subjectSummary = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)

        then:
        subjectSummary.skills[0].expirationDate == DTF.print(nextExpirationRun.toDate().time)
        subjectSummary.skills[1].expirationDate == DTF.print((nextExpirationRun.toDate()+1).time)
        subjectSummary.skills[2].expirationDate == DTF.print((nextExpirationRun.toDate()+2).time)
    }

    def expireAllSkillsForProject(String projectId, Integer numDaysAfterAchievement=7,excludeImportedSkills = true, boolean includeDisabled = false, boolean excludeReusedSkills = true) {
        def skills = skillsService.getSkillsForProject(projectId, "", excludeImportedSkills, includeDisabled, excludeReusedSkills)
        expireSkills(projectId, skills, numDaysAfterAchievement)
    }

    def expireSkill(String projectId, def skill, Integer numDaysAfterAchievement=7) {
        expireSkills(projectId, [skill], numDaysAfterAchievement)
    }

    def expireSkills(String projectId, def skills, Integer numDaysAfterAchievement=7) {
        skills.forEach { it ->
            skillsService.saveSkillExpirationAttributes(projectId, it.skillId, [
                    expirationType: ExpirationAttrs.DAILY,
                    every: numDaysAfterAchievement,
            ])
        }
        expireUserAchievementsTaskExecutor.removeExpiredUserAchievements()
    }

    def "get list of expired skills for project"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

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
        Long timestamp = (new Date()-8).time

        setup:
        skills.forEach { it ->
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, new Date(timestamp))
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], secondUserId, new Date(timestamp))
        }

        when:
        expireSkills(proj.projectId, skills)

        def user1Expired = skillsService.getExpiredSkills(proj.projectId, "user1", "", 30, 1, "skillName", true).data
        def user2Expired = skillsService.getExpiredSkills(proj.projectId, "user2", "", 30, 1, "skillName", true).data

        then:
        user1Expired.skillId == [
            "skill1", "skill10", "skill2", "skill3", "skill4", "skill5", "skill6", "skill7", "skill8", "skill9"
        ]
        user1Expired.skillName == [
            "Test Skill 1", "Test Skill 10", "Test Skill 2", "Test Skill 3", "Test Skill 4", "Test Skill 5", "Test Skill 6", "Test Skill 7", "Test Skill 8", "Test Skill 9"
        ]
        user2Expired.skillId == [
            "skill1", "skill10", "skill2", "skill3", "skill4", "skill5", "skill6", "skill7", "skill8", "skill9"
        ]
        user2Expired.skillName == [
            "Test Skill 1", "Test Skill 10", "Test Skill 2", "Test Skill 3", "Test Skill 4", "Test Skill 5", "Test Skill 6", "Test Skill 7", "Test Skill 8", "Test Skill 9"
        ]

    }

    def "get list of expired skills for project filtered by skill name"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

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
        Long timestamp = (new Date()-8).time

        setup:
        skills.forEach { it ->
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, new Date(timestamp))
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], secondUserId, new Date(timestamp))
        }

        when:
        expireSkills(proj.projectId, skills)

        def skill4Expired = skillsService.getExpiredSkills(proj.projectId, "", "Test Skill 4", 30, 1, "userId", true).data
        def skill7Expired = skillsService.getExpiredSkills(proj.projectId, "", "Test Skill 7", 30, 1, "userId", true).data

        then:
        skill4Expired.skillId == [
                "skill4", "skill4"
        ]
        skill4Expired.skillName == [
                "Test Skill 4", "Test Skill 4"
        ]
        skill7Expired.skillId == [
                "skill7", "skill7"
        ]
        skill7Expired.skillName == [
                "Test Skill 7", "Test Skill 7"
        ]

    }

    def "get expired skill summaries"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

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
        Date timestamp8Days = new Date()-8
        Date timestamp9Days = new Date()-8
        Long secondTimestamp = new Date().time

        setup:
        skills.forEach { it ->
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, timestamp8Days)
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, timestamp9Days)
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], secondUserId, new Date(secondTimestamp))
        }

        when:
        expireSkills(proj.projectId, skills)
        def skill4Information = skillsService.getSingleSkillSummary(userId, proj.projectId, skills[3].skillId)
        def skill7Information = skillsService.getSingleSkillSummary(userId, proj.projectId, skills[6].skillId)
        def skill4InformationUser2 = skillsService.getSingleSkillSummary(secondUserId, proj.projectId, skills[3].skillId)
        def skill7InformationUser2 = skillsService.getSingleSkillSummary(secondUserId, proj.projectId, skills[6].skillId)

        then:
        skill4Information.lastExpirationDate != null
        skill7Information.lastExpirationDate != null
        skill4InformationUser2.lastExpirationDate == null
        skill7InformationUser2.lastExpirationDate == null
    }

    def "achieve skill after expiration"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Map badge = [projectId: proj.projectId, badgeId: 'badge1', name: 'Test Badge 1']
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId)
        badge.enabled = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        String userId = "user1"
        Date yesterday = (new Date()-1)
        Date twoDaysAgo = (new Date()-2)
        Date timestamp8Days = new Date()-8
        Date timestamp9Days = new Date()-8

        setup:
        skills.forEach { it ->
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, timestamp8Days)
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, timestamp9Days)
        }

        when:
        expireSkills(proj.projectId, skills)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[3].skillId], userId, yesterday)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[3].skillId], userId, twoDaysAgo)
        def skill4Information = skillsService.getSingleSkillSummary(userId, proj.projectId, skills[3].skillId)

        then:
        skill4Information.lastExpirationDate == null
    }

    def "get expired skill summaries for subject"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

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
        Date timestamp8Days = new Date()-8
        Date timestamp9Days = new Date()-8
        Long secondTimestamp = new Date().time

        setup:
        skills.forEach { it ->
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, timestamp8Days)
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, timestamp9Days)
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], secondUserId, new Date(secondTimestamp))
        }

        when:
        expireSkills(proj.projectId, skills)
        def subjectInfo = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)

        then:
        subjectInfo.skills.lastExpirationDate != null
    }

    def "achieve skill after expiration and get subject summary"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Map badge = [projectId: proj.projectId, badgeId: 'badge1', name: 'Test Badge 1']
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId)
        badge.enabled = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        String userId = "user1"

        setup:
        skills.forEach { it ->
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, new Date() - 21)
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, new Date() - 20)
        }

        when:
        expireSkills(proj.projectId, skills)
        def subjectInfo = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date() - 8)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date() - 9)
        def laterSubjectInfo = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)

        then:
        subjectInfo.skills.lastExpirationDate != null
        laterSubjectInfo.skills[0].lastExpirationDate == null
    }

    def "with multiple expirations, latest expiration is returned in subject summary"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(10, )

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        Map badge = [projectId: proj.projectId, badgeId: 'badge1', name: 'Test Badge 1']
        skillsService.addBadge(badge)
        skillsService.assignSkillToBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId)
        badge.enabled = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        String userId = "user1"

        setup:
        skills.forEach { it ->
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, new Date() - 31)
            skillsService.addSkill([projectId: proj.projectId, skillId: it.skillId], userId, new Date() - 30)
        }

        when:
        expireSkills(proj.projectId, skills)
        def subjectInfo = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date() - 20)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date() - 19)
        expireSkills(proj.projectId, skills)
        def secondSubjectInfo = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date() - 9)
        skillsService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId], userId, new Date() - 8)
        expireSkills(proj.projectId, skills)
        def laterSubjectInfo = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)

        then:
        laterSubjectInfo.skills[0].lastExpirationDate != null
        subjectInfo.skills[0].lastExpirationDate != laterSubjectInfo.skills[0].lastExpirationDate != null
        secondSubjectInfo.skills[0].lastExpirationDate != laterSubjectInfo.skills[0].lastExpirationDate != null
        subjectInfo.skills[0].lastExpirationDate != secondSubjectInfo.skills[0].lastExpirationDate != null
    }

    def "expiring skill events for a skill with a completed quiz removes the quiz run"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skills = SkillsFactory.createSkills(1, 1, 1, 200, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(1, true)
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0]).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, users[0]).body

        def initialQuizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        def performedSkills = skillsService.getPerformedSkills(users[0], proj.projectId)

        assert performedSkills.count == 1
        assert performedSkills.data[0].skillId == skills[0].skillId

        when:
        expireSkill(proj.projectId, skills[0], 0)
        def quizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        performedSkills = skillsService.getPerformedSkills(users[0], proj.projectId)

        then:
        initialQuizRuns.totalCount == users.size()
        quizRuns.totalCount == 0
        performedSkills.count == 0
    }

    def "can complete quiz again after skill has expired"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skills = SkillsFactory.createSkills(1, 1, 1, 200, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(1, true)
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0]).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, users[0]).body

        def initialQuizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        expireSkill(proj.projectId, skills[0], 0)
        def quizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')

        when:
        def quizAttempt2 =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0]).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt2.id, quizInfo.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt2.id, users[0]).body

        then:
        def newQuizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        initialQuizRuns.totalCount == 1
        quizRuns.totalCount == 0
        newQuizRuns.totalCount == 1
    }

    def "can not complete quiz again if multiple takes not enabled and not expiring"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skills = SkillsFactory.createSkills(1, 1, 1, 200, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(1, true)
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0]).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, users[0]).body

        when:
        def initialQuizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        initialQuizRuns.totalCount == 1
        initialQuizRuns.data[0].status == "PASSED"
        def quizAttempt2 =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0]).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt2.id, quizInfo.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt2.id, users[0]).body

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('User [' + users[0] + '] already took and passed this quiz.')
    }

    def "can complete quiz again if multiple takes enabled"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skills = SkillsFactory.createSkills(1, 1, 1, 200, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: 'quizMultipleTakes', value: 'true'],
        ])

        List<String> users = getRandomUsers(1, true)
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0]).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, users[0]).body

        when:
        def initialQuizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        initialQuizRuns.totalCount == 1
        initialQuizRuns.data[0].status == "PASSED"
        def quizAttempt2 =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0]).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt2.id, quizInfo.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt2.id, users[0]).body

        then:
        def newQuizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        newQuizRuns.totalCount == 2
    }

    def "can complete quiz again if skill is expiring in a day"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skills = SkillsFactory.createSkills(1, 1, 1, 200, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.saveSkillExpirationAttributes( proj.projectId, "skill1", [ expirationType: ExpirationAttrs.DAILY, every: 1 ]);

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(1, true)
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0], "skill1", proj.projectId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, users[0]).body

        when:
        def initialQuizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        initialQuizRuns.totalCount == 1
        initialQuizRuns.data[0].status == "PASSED"
        def quizAttempt2 =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0], "skill1", proj.projectId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt2.id, quizInfo.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt2.id, users[0]).body

        then:
        def newQuizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        newQuizRuns.totalCount == 2
    }

    def "can complete quiz again if skill expiration is daily"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1)
        def skills = SkillsFactory.createSkills(1, 1, 1, 200, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.saveSkillExpirationAttributes( proj.projectId, "skill1", [ expirationType: ExpirationAttrs.DAILY, every: 2 ]);

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(1, true)
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0], "skill1", proj.projectId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, users[0]).body

        when:
        def initialQuizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        def quizAttempt2 =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0], "skill1", proj.projectId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt2.id, quizInfo.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt2.id, users[0]).body
        def newQuizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')

        then:
        initialQuizRuns.totalCount == 1
        initialQuizRuns.data.status == [UserQuizAttempt.QuizAttemptStatus.PASSED.toString()]

        newQuizRuns.totalCount == 2
        newQuizRuns.data.status == [
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString(),
                UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
        ]
    }
}
