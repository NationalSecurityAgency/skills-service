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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.CatalogParticipantBackfillService
import skills.services.LockingService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.UserAchievement
import skills.storage.model.SkillDef
import skills.storage.model.UserPoints
import skills.storage.repos.ProjectParticipationRepo
import skills.storage.repos.SkillDefRepo
import spock.lang.IgnoreRest
import spock.util.concurrent.PollingConditions

import javax.sql.DataSource
import java.sql.Connection
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static skills.intTests.utils.SkillsFactory.*

class CatalogParticipantBackfillSpecs extends CatalogIntSpec {
    @Autowired ProjectParticipationRepo participationRepo
    @Autowired SkillDefRepo skillDefRepo
    @Autowired DataSource dataSource
    @Autowired CatalogParticipantBackfillService backfillService
    @Autowired PlatformTransactionManager transactionManager
    @Autowired LockingService lockingService

    private List<String> users
    private final Date firstDate = new Date(System.currentTimeMillis() - 3 * 86400000L)
    private final Date completionDate = new Date(System.currentTimeMillis() - 2 * 86400000L)

    def setup() {
        users = getRandomUsers(8)
    }

    def cleanup() {
        removeTrigger('catalog_test_queue', 'scheduled_tasks')
        removeTrigger('catalog_test_fault', 'user_achievement')
        removeTrigger('catalog_test_schedule_fault', 'scheduled_tasks')
        removeTrigger('catalog_test_pause', 'user_points')
        jdbcTemplate.update("update scheduled_tasks set execution_time = now() where task_name = 'catalog-participant-backfill'")
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
    }


    def "first #entry backfills earned catalog credit only into the participating project"() {
        Map fixture = catalogFixture()
        earnSource(fixture, users[0])
        List<Map> sourceCreditBefore = creditSnapshot('TestProject1', users[0])
        SkillsService learner = createService(users[0])
        skillsService.bulkImportSkillsFromCatalogAndFinalize('TestProject3', 'TestSubject1', fixture.sourceSkills)
        // Finalization is finished. Hold the NEW user backfill that qualify() schedules below.
        queueBackfill()

        when:
        Map result = qualify(fixture, learner, entry)
        ProjectParticipation first = participationRepo.findByProjectIdAndUserId('TestProject2', users[0])
        List<Map> queued = backfillTasks()
        Map repeated = learner.initializeProjectProgress('TestProject2')
        Map pendingSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')

        then:
        // Inspect unfinished work before waiting; a fast worker could otherwise hide this state.
        first.backfillStatus == BackfillStatus.PENDING
        !repeated.ready
        repeated.backfillStatus == 'PENDING'
        queued.size() == 1
        backfillTasks() == queued
        pendingSummary.points == (entry == 'award' ? 100 : 0)
        pendingSummary.skillsLevel == (entry == 'award' ? 1 : 0)
        pendingSummary.skillsAchieved == 0
        pendingSummary.badges.numBadgesCompleted == 0
        !pendingSummary.badges.recentlyAwardedBadges
        points('TestProject2', users[0], null) == (entry == 'award' ? 100 : 0)
        points('TestProject3', users[0], null) == 0
        entry != 'visit' || (!result.ready && result.backfillStatus == 'PENDING')

        when:
        Date workerStart = new Date()
        // Let the queued worker run, then verify its completed credit.
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map ready = learner.initializeProjectProgress('TestProject2')
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        Map subjectSummary = summary.subjects.find { Map subject -> subject.subjectId == 'TestSubject1' }
        List<Map> credit = creditSnapshot('TestProject2', users[0])
        learner.initializeProjectProgress('TestProject2')
        Map repeatedSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        Map unrelatedSummary = learner.getSkillsSummaryForCurrentUser('TestProject3')

        then:
        ready.ready
        ready.backfillStatus == 'COMPLETED'
        summary.points == (entry == 'award' ? 500 : 400)
        summary.totalPoints == 900
        summary.skillsLevel == (entry == 'award' ? 3 : 2)
        summary.totalLevels == 5
        summary.skillsAchieved == 2
        summary.totalSkills == 3
        summary.subjects.subjectId == ['TestSubject1']
        subjectSummary.points == (entry == 'award' ? 500 : 400)
        subjectSummary.totalPoints == 900
        subjectSummary.skillsLevel == (entry == 'award' ? 3 : 2)
        subjectSummary.totalLevels == 5
        subjectSummary.skillsAchieved == 2
        subjectSummary.totalSkills == 3
        summary.badges.enabled
        summary.badges.numTotalBadges == 1
        summary.badges.numBadgesCompleted == 1
        summary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        summary.badges.recentlyAwardedBadges[0].badgeName == fixture.badge.name
        !summary.badges.recentlyAwardedBadges[0].isGlobalBadge
        Instant.parse(summary.badges.recentlyAwardedBadges[0].achievedOn).toEpochMilli() == completionDate.time
        repeatedSummary == summary
        unrelatedSummary.points == 0
        unrelatedSummary.skillsLevel == 0
        unrelatedSummary.skillsAchieved == 0
        unrelatedSummary.badges.numBadgesCompleted == 0
        points('TestProject2', users[0], 'TestSubject1') == subjectSummary.points
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], 'TestProject2', null)*.level.sort() == (entry == 'award' ? [1, 2, 3] : [1, 2])
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], 'TestProject2', 'TestSubject1')*.level.sort() == (entry == 'award' ? [1, 2, 3] : [1, 2])
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], 'TestProject2', null).find { UserAchievement award -> award.level == 2 }.achievedOn == completionDate
        fixture.sourceSkills.every { Map skill -> points('TestProject2', users[0], skill.skillId) == 200 }
        fixture.sourceSkills.every { Map skill -> achievement('TestProject2', users[0], skill.skillId).achievedOn == completionDate }
        achievement('TestProject2', users[0], fixture.group.skillId).achievedOn >= workerStart
        achievement('TestProject2', users[0], fixture.group.skillId).achievedOn <= new Date()
        achievement('TestProject2', users[0], fixture.badge.badgeId).achievedOn == completionDate
        creditSnapshot('TestProject2', users[0]) == credit
        participationRepo.findById(first.id).get().created == first.created
        points('TestProject3', users[0], null) == 0
        !achievement('TestProject3', users[0], fixture.sourceSkills[0].skillId)
        !creditSnapshot('TestProject3', users[0])
        creditSnapshot('TestProject1', users[0]) == sourceCreditBefore
        !backfillTasks()

        where:
        entry << ['visit', 'award']
    }

    def "catalog backfill awards a global badge when imported credit satisfies its project level requirement"() {
        Map fixture = catalogFixture()
        List<SkillsService> learners = users.take(2).collect { String userId -> createService(userId) }
        users.take(2).each { String userId -> earnSource(fixture, userId) }
        Map globalBadge = createBadge(1, 90)
        skillsService.createGlobalBadge(globalBadge)
        skillsService.assignSkillToGlobalBadge([projectId: 'TestProject1', badgeId: globalBadge.badgeId,
                                              skillId: fixture.sourceSkills[0].skillId])
        skillsService.assignProjectLevelToGlobalBadge([projectId: 'TestProject2', badgeId: globalBadge.badgeId, level: '2'])
        skillsService.updateGlobalBadge(globalBadge + [enabled: 'true'])
        queueBackfill()

        expect:
        learners.each { SkillsService learner ->
            Map badgeSummary = learner.getBadgeSummary(null, 'TestProject2', globalBadge.badgeId, -1, true)
            assert !badgeSummary.badgeAchieved
            assert !badgeSummary.dateAchieved
        }

        when:
        Map pending = learners[0].initializeProjectProgress('TestProject2')
        Map pendingBadge = learners[0].getBadgeSummary(null, 'TestProject2', globalBadge.badgeId, -1, true)

        then:
        !pending.ready
        pending.backfillStatus == 'PENDING'
        backfillTasks().size() == 1
        learners[0].getSkillsSummaryForCurrentUser('TestProject2').points == 0
        !pendingBadge.badgeAchieved
        !pendingBadge.dateAchieved

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learners[0].getSkillsSummaryForCurrentUser('TestProject2')
        Map awardedBadge = learners[0].getBadgeSummary(null, 'TestProject2', globalBadge.badgeId, -1, true)
        Map unrelatedBadge = learners[1].getBadgeSummary(null, 'TestProject2', globalBadge.badgeId, -1, true)

        then:
        summary.points == 400
        summary.skillsLevel == 2
        summary.skillsAchieved == 2
        awardedBadge.badgeAchieved
        awardedBadge.dateAchieved
        learners[0].initializeProjectProgress('TestProject2').backfillStatus == 'COMPLETED'
        !unrelatedBadge.badgeAchieved
        !unrelatedBadge.dateAchieved
        learners[1].getSkillsSummaryForCurrentUser('TestProject2').points == 0
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[1])
        !backfillTasks()
    }

    def "#entry completes without backfill when the user has no credit for imported skills"() {
        Map fixture = catalogFixture()
        earnSource(fixture, users[1])
        Map unrelated = createSkill(1, 1, 99, 0, 5, 0, 100)
        skillsService.createSkill(unrelated)
        skillsService.addSkill(unrelated, users[0])
        queueBackfill()

        when:
        SkillsService learner = createService(users[0])
        qualify(fixture, learner, entry)
        Map result = learner.initializeProjectProgress('TestProject2')

        then:
        result.ready
        result.backfillStatus == 'COMPLETED'
        !backfillTasks()
        points('TestProject2', users[0], null) == (entry == 'award' ? 100 : 0)

        where:
        entry << ['visit', 'award']
    }

    def "initialization ignores achievement-only credit belonging to #creditOwner on #creditedSkill skills"() {
        Map fixture = catalogFixture()
        Map unrelated = createSkill(1, 1, 99, 0, 2, 0, 100)
        skillsService.createSkill(unrelated)
        SkillsService learner = createService(users[0])
        String creditedUserId = creditOwner == 'this user' ? users[0] : users[1]
        SkillsService creditedLearner = createService(creditedUserId)
        Map sourceSkill = creditedSkill == 'imported' ? fixture.sourceSkills[0] : unrelated
        saveSourceAchievement(sourceSkill, creditedUserId)
        queueBackfill()

        when:
        Map result = learner.initializeProjectProgress('TestProject2')
        Map repeated = learner.initializeProjectProgress('TestProject2')
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        Map sourceSummary = creditedLearner.getSkillsSummaryForCurrentUser('TestProject1')

        then:
        result.ready
        result.backfillStatus == 'COMPLETED'
        repeated == result
        !backfillTasks()
        sourceSummary.points == 0
        sourceSummary.skillsAchieved == 1
        summary.points == 0
        summary.skillsAchieved == 0
        summary.badges.numBadgesCompleted == 0

        where:
        creditOwner  | creditedSkill
        'other user' | 'imported'
        'this user'  | 'unrelated'
        'other user' | 'unrelated'
    }

    def "initialization ignores #creditType credit for a disabled import with enabled imports #enabledImports"() {
        catalogFixture(enabledImports, false)
        Map disabledSource = createSkill(1, 1, 99, 0, 2, 0, 100)
        skillsService.createSkill(disabledSource)
        SkillsService learner = createService(users[0])
        if (creditType == 'points') {
            skillsService.addSkill(disabledSource, users[0], firstDate)
        } else {
            saveSourceAchievement(disabledSource, users[0])
        }
        skillsService.exportSkillToCatalog('TestProject1', disabledSource.skillId)
        skillsService.bulkImportSkillsFromCatalog('TestProject2', 'TestSubject1', [disabledSource])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        queueBackfill()

        when:
        Map result = learner.initializeProjectProgress('TestProject2')
        Map sourceSummary = learner.getSkillsSummaryForCurrentUser('TestProject1')
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')

        then:
        result.ready
        result.backfillStatus == (enabledImports ? 'COMPLETED' : 'NOT_NEEDED')
        !backfillTasks()
        sourceSummary.points == (creditType == 'points' ? 100 : 0)
        sourceSummary.skillsAchieved == (creditType == 'achievement' ? 1 : 0)
        summary.points == 0
        summary.skillsAchieved == 0
        !achievement('TestProject2', users[0], disabledSource.skillId)

        where:
        enabledImports | creditType
        false          | 'points'
        false          | 'achievement'
        true           | 'points'
        true           | 'achievement'
    }

    def "initialization scopes #creditType source credit to the exact importing destination"() {
        Map fixture = catalogFixture(false, false)
        SkillsService learner = createService(users[0])
        Map creditedSource = fixture.sourceSkills[0]
        Map emptySource = fixture.sourceSkills[1]
        skillsService.bulkImportSkillsFromCatalogAndFinalize('TestProject2', 'TestSubject1', [creditedSource])
        skillsService.bulkImportSkillsFromCatalogAndFinalize('TestProject3', 'TestSubject1', [emptySource])
        if (creditType == 'points') {
            skillsService.addSkill(creditedSource, users[0], firstDate)
        } else {
            saveSourceAchievement(creditedSource, users[0])
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        queueBackfill()

        when: 'credit imported by another destination must not make this destination pending'
        Map emptyResult = learner.initializeProjectProgress('TestProject3')
        Map emptySummary = learner.getSkillsSummaryForCurrentUser('TestProject3')

        then:
        emptyResult.ready
        emptyResult.backfillStatus == 'COMPLETED'
        emptySummary.points == 0
        emptySummary.skillsAchieved == 0
        !backfillTasks()

        when: 'the same learner initializes the destination importing the credited skill'
        Map pending = learner.initializeProjectProgress('TestProject2')

        then:
        !pending.ready
        pending.backfillStatus == 'PENDING'
        backfillTasks().size() == 1
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 0
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 0

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map completed = learner.initializeProjectProgress('TestProject2')
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        Map unchanged = learner.initializeProjectProgress('TestProject3')
        Map unchangedSummary = learner.getSkillsSummaryForCurrentUser('TestProject3')

        then:
        completed.ready
        completed.backfillStatus == 'COMPLETED'
        summary.points == (creditType == 'points' ? 100 : 0)
        summary.skillsAchieved == (creditType == 'achievement' ? 1 : 0)
        unchanged == emptyResult
        unchangedSummary.points == 0
        unchangedSummary.skillsAchieved == 0
        !backfillTasks()

        where:
        creditType << ['points', 'achievement']
    }

    def "#entry backfills partially earned catalog points without granting unearned achievements"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        skillsService.addSkill(fixture.sourceSkills[0], users[0], firstDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<Map> sourceCreditBefore = creditSnapshot('TestProject1', users[0])
        queueBackfill()

        when:
        qualify(fixture, learner, entry)
        then:
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.PENDING
        backfillTasks().size() == 1

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map ready = learner.initializeProjectProgress('TestProject2')
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        Map subjectSummary = summary.subjects.find { Map subject -> subject.subjectId == 'TestSubject1' }
        then:
        ready.ready
        ready.backfillStatus == 'COMPLETED'
        summary.points == (entry == 'award' ? 200 : 100)
        summary.totalPoints == 900
        summary.skillsLevel == 1
        summary.skillsAchieved == 0
        summary.totalSkills == 3
        subjectSummary.points == (entry == 'award' ? 200 : 100)
        subjectSummary.skillsLevel == 1
        subjectSummary.skillsAchieved == 0
        summary.badges.numTotalBadges == 1
        summary.badges.numBadgesCompleted == 0
        !summary.badges.recentlyAwardedBadges
        points('TestProject2', users[0], fixture.sourceSkills[0].skillId) == 100
        points('TestProject2', users[0], fixture.sourceSkills[1].skillId) == 0
        fixture.sourceSkills.every { Map skill -> !achievement('TestProject2', users[0], skill.skillId) }
        !achievement('TestProject2', users[0], fixture.group.skillId)
        creditSnapshot('TestProject1', users[0]) == sourceCreditBefore
        !backfillTasks()

        where:
        entry << ['visit', 'award']
    }

    def "earning local points does not enroll users for catalog backfill without enabled catalog imports - #kind"() {
        Map fixture = catalogFixture(false, false)
        if (kind == 'pending') {
            importSkills(fixture)
        } else if (kind == 'reuse') {
            Map subject = createSubject(2, 2)
            skillsService.createSubject(subject)
            skillsService.reuseSkillInAnotherSubject('TestProject2', fixture.local.skillId, subject.subjectId)
            waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        }

        when:
        skillsService.addSkill(fixture.local, users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[0])
        !backfillTasks()
        kind != 'reuse' || points('TestProject2', users[0], null) == 200

        when:
        Map result = createService(users[0]).initializeProjectProgress('TestProject2')
        then:
        result.ready
        result.backfillStatus == 'NOT_NEEDED'
        !backfillTasks()

        where:
        kind << ['none', 'pending', 'reuse']
    }

    def "rejected local reports and earning source credit do not enroll users in importing projects"() {
        Map fixture = catalogFixture()
        Map prerequisite = createSkill(2, 1, 90, 0, 5, 0, 100)
        skillsService.createSkill(prerequisite)
        skillsService.addLearningPathPrerequisite('TestProject2', fixture.local.skillId, prerequisite.skillId)

        when:
        Map rejected = skillsService.addSkill(fixture.local, users[0]).body
        earnSource(fixture, users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        skillsService.getSkillsSummaryForUser('TestProject2', users[0])
        then:
        !rejected.skillApplied
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[0])
        points('TestProject2', users[0], null) == 0
        !achievement('TestProject2', users[0], fixture.sourceSkills[0].skillId)
        !backfillTasks()
    }

    def "earning more local points does not restart completed catalog backfill"() {
        Map fixture = catalogFixture()
        skillsService.addSkill(fixture.local, users[0])
        ProjectParticipation initial = participationRepo.findByProjectIdAndUserId('TestProject2', users[0])

        when:
        skillsService.addSkill(fixture.local, users[0])
        skillsService.addSkill(fixture.local, users[0])
        then:
        initial.backfillStatus == BackfillStatus.COMPLETED
        participationRepo.findById(initial.id).get().updated == initial.updated
        points('TestProject2', users[0], null) == 300
        !backfillTasks()
    }

    def "finalizing imports credits users who earned local points or visited the project and skips other users"() {
        Map fixture = catalogFixture(false)
        List<SkillsService> learners = users.collect { String userId -> createService(userId) }
        // Full source completion: local reporter, visitor, completed participant, and unrelated user.
        users.take(4).each { String userId -> earnSource(fixture, userId) }
        // Partial source progress: local reporter and unrelated user. One completed source skill: visitor.
        [users[4], users[6]].each { String userId -> skillsService.addSkill(fixture.sourceSkills[0], userId, firstDate) }
        skillsService.addSkill(fixture.sourceSkills[0], users[5], firstDate)
        skillsService.addSkill(fixture.sourceSkills[0], users[5], completionDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        // Establish local-only eligibility before the project has any enabled imports.
        skillsService.addSkill(fixture.local, users[0])
        skillsService.addSkill(fixture.local, users[4])
        [1, 5, 7].each { int index -> learners[index].initializeProjectProgress('TestProject2') }

        // A prior finalized import with no earned credit establishes COMPLETED through the real visit flow.
        Map earlierImport = createSkill(1, 1, 90, 0, 1, 0, 100)
        skillsService.createSkill(earlierImport)
        skillsService.exportSkillToCatalog('TestProject1', earlierImport.skillId)
        skillsService.bulkImportSkillsFromCatalogAndFinalize('TestProject2', 'TestSubject1', [earlierImport])
        Map completedVisit = learners[2].initializeProjectProgress('TestProject2')

        importSkills(fixture)
        skillsService.createBadge(fixture.badge + [enabled: 'false'])
        List<Map> participationBefore = [1, 2, 5, 7].collect { int index ->
            ProjectParticipation participant = participationRepo.findByProjectIdAndUserId('TestProject2', users[index])
            [userId: users[index], id: participant.id, created: participant.created, status: participant.backfillStatus]
        }
        List<List<Map>> sourceCreditBefore = users.collect { String userId -> creditSnapshot('TestProject1', userId) }
        List<Map> expected = [
                [sourcePoints: 400, sourceSkills: 2, points: 500, level: 3, skills: 2, badges: 1],
                [sourcePoints: 400, sourceSkills: 2, points: 400, level: 2, skills: 2, badges: 1],
                [sourcePoints: 400, sourceSkills: 2, points: 400, level: 2, skills: 2, badges: 1],
                [sourcePoints: 400, sourceSkills: 2, points: 0, level: 0, skills: 0, badges: 0],
                [sourcePoints: 100, sourceSkills: 0, points: 200, level: 1, skills: 0, badges: 0],
                [sourcePoints: 200, sourceSkills: 1, points: 200, level: 1, skills: 1, badges: 0],
                [sourcePoints: 100, sourceSkills: 0, points: 0, level: 0, skills: 0, badges: 0],
                [sourcePoints: 0, sourceSkills: 0, points: 0, level: 0, skills: 0, badges: 0],
        ]

        expect:
        completedVisit.ready
        completedVisit.backfillStatus == 'COMPLETED'
        [0, 3, 4, 6].every { int index -> !participationRepo.existsByProjectIdAndUserId('TestProject2', users[index]) }
        [1, 5, 7].every { int index -> participationRepo.readStatus('TestProject2', users[index]) == BackfillStatus.NOT_NEEDED }
        participationRepo.readStatus('TestProject2', users[2]) == BackfillStatus.COMPLETED
        [3, 6].every { int index -> !creditSnapshot('TestProject2', users[index]) }
        !backfillTasks()
        learners.eachWithIndex { SkillsService learner, int index ->
            Map sourceSummary = learner.getSkillsSummaryForCurrentUser('TestProject1')
            assert sourceSummary.points == expected[index].sourcePoints
            assert sourceSummary.skillsAchieved == expected[index].sourceSkills
            Map destinationSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')
            assert destinationSummary.points == (index in [0, 4] ? 100 : 0)
            assert destinationSummary.skillsAchieved == 0
            assert destinationSummary.badges.numBadgesCompleted == 0
        }

        when:
        Date finalizationStart = new Date()
        skillsService.finalizeSkillsImportFromCatalog('TestProject2')
        List<Map> summaries = learners.collect { SkillsService learner -> learner.getSkillsSummaryForCurrentUser('TestProject2') }
        then:
        summaries.eachWithIndex { Map summary, int index ->
            Map outcome = expected[index]
            assert summary.points == outcome.points
            assert summary.totalPoints == 1000
            assert summary.skillsLevel == outcome.level
            assert summary.skillsAchieved == outcome.skills
            assert summary.totalSkills == 4
            assert summary.subjects.subjectId == ['TestSubject1']
            Map subjectSummary = summary.subjects[0]
            assert subjectSummary.points == outcome.points
            assert subjectSummary.totalPoints == 1000
            assert subjectSummary.skillsLevel == outcome.level
            assert subjectSummary.skillsAchieved == outcome.skills
            assert subjectSummary.totalSkills == 4
            assert summary.badges.numTotalBadges == 0
            assert summary.badges.numBadgesCompleted == 0
            assert !summary.badges.recentlyAwardedBadges
        }
        [0, 3, 4, 6].every { int index -> !participationRepo.existsByProjectIdAndUserId('TestProject2', users[index]) }
        participationBefore.every { Map before ->
            ProjectParticipation participant = participationRepo.findByProjectIdAndUserId('TestProject2', before.userId)
            participant.id == before.id && participant.created == before.created && participant.backfillStatus == before.status
        }
        users.take(3).every { String userId ->
            fixture.sourceSkills.every { Map skill -> achievement('TestProject2', userId, skill.skillId).achievedOn == completionDate }
        }
        users.take(3).every { String userId ->
            UserAchievement group = achievement('TestProject2', userId, fixture.group.skillId)
            group.achievedOn >= finalizationStart && group.achievedOn <= new Date()
        }
        achievement('TestProject2', users[5], fixture.sourceSkills[0].skillId).achievedOn == completionDate
        [4, 5, 7].every { int index -> !achievement('TestProject2', users[index], fixture.group.skillId) }
        [3, 6].every { int index -> !creditSnapshot('TestProject2', users[index]) }
        users.collect { String userId -> creditSnapshot('TestProject1', userId) } == sourceCreditBefore
        users.every { String userId -> !creditSnapshot('TestProject3', userId) }
        !backfillTasks()

        when:
        // Badge requirements can only be assigned after imported skills are finalized and enabled.
        skillsService.assignSkillsToBadge('TestProject2', fixture.badge.badgeId, fixture.sourceSkills.skillId)
        Date badgePublicationStart = new Date()
        skillsService.createBadge(fixture.badge + [enabled: 'true'])
        List<Map> badgeSummaries = learners.collect { SkillsService learner -> learner.getSkillsSummaryForCurrentUser('TestProject2') }
        then:
        badgeSummaries.eachWithIndex { Map summary, int index ->
            assert summary.points == expected[index].points
            assert summary.badges.numTotalBadges == 1
            assert summary.badges.numBadgesCompleted == expected[index].badges
            assert summary.badges.recentlyAwardedBadges.badgeId == (expected[index].badges ? [fixture.badge.badgeId] : [])
            if (expected[index].badges) {
                long achievedOn = Instant.parse(summary.badges.recentlyAwardedBadges[0].achievedOn).toEpochMilli()
                assert achievedOn >= badgePublicationStart.time
                assert achievedOn <= new Date().time
            }
        }
        [3, 6].every { int index -> !creditSnapshot('TestProject2', users[index]) }
    }

    def "catalog finalization awards badges only to eligible participants - protected community #protectedCommunity"() {
        SkillsService owner = createService(users[7])
        SkillsService root = createRootSkillService()
        List<SkillsService> learners = users.take(5).collect { String userId -> createService(userId) }
        if (protectedCommunity) {
            (users.take(5) + users[7]).each { String userId ->
                root.saveUserTag(userId, 'dragons', ['DivineDragon'])
            }
        }
        Map source = createSkill(1, 1, 11, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [source])
        skillsService.exportSkillToCatalog('TestProject1', source.skillId)
        Map destination = createProject(2) + [enableProtectedUserCommunity: protectedCommunity]
        Map local = createSkill(2, 1, 21, 0, 5, 0, 100)
        owner.createProjectAndSubjectAndSkills(destination, createSubject(2, 1), [local])
        owner.bulkImportSkillsFromCatalogAndFinalize('TestProject2', 'TestSubject1', [source])
        Map badge = createBadge(2, 1)
        owner.createBadge(badge + [enabled: 'false'])
        owner.assignSkillsToBadge('TestProject2', badge.badgeId, [source.skillId])
        users.take(5).each { String userId ->
            skillsService.addSkill(source, userId, completionDate)
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        // Earn imported credit through real reports/visits while all users can access the project.
        [0, 3].each { int index ->
            owner.addSkill(local, users[index], firstDate)
        }
        [1, 2, 4].each { int index -> learners[index].initializeProjectProgress('TestProject2') }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        // Model pre-enrollment credit: retain real credit but remove enrollment for local-only users
        // and the unrelated user. Users 1 and 4 retain visit-only eligibility.
        [0, 2, 3].each { int index ->
            participationRepo.delete(participationRepo.findByProjectIdAndUserId('TestProject2', users[index]))
        }
        if (protectedCommunity) {
            // The root API only adds tags; remove the membership tag through the repository.
            [3, 4].each { int index ->
                userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(users[index], 'dragons'))
            }
        }
        // Publishing through the API awards eligible users immediately. Enable the fixture directly
        // so catalog finalization is the first operation that can award this badge.
        SkillDef badgeDefinition = skillDefRepo.findByProjectIdAndSkillId('TestProject2', badge.badgeId)
        badgeDefinition.enabled = 'true'
        skillDefRepo.save(badgeDefinition)
        // Finalize another import to recalculate the enabled badge against the retained achievements.
        Map additional = createSkill(1, 1, 12, 0, 1, 0, 100)
        skillsService.createSkill(additional)
        skillsService.exportSkillToCatalog('TestProject1', additional.skillId)
        owner.bulkImportSkillsFromCatalog('TestProject2', 'TestSubject1', [additional])

        expect:
        users.take(5).every { String userId -> !achievement('TestProject2', userId, badge.badgeId) }
        users.take(5).every { String userId -> achievement('TestProject2', userId, source.skillId) }
        [0, 2, 3].every { int index -> !participationRepo.existsByProjectIdAndUserId('TestProject2', users[index]) }

        when:
        owner.finalizeSkillsImportFromCatalog('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learners.eachWithIndex { SkillsService learner, int index ->
            if (!protectedCommunity || index < 3) {
                Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
                boolean awarded = index != 2
                assert summary.badges.numTotalBadges == 1
                assert summary.badges.numBadgesCompleted == (awarded ? 1 : 0)
                assert summary.badges.recentlyAwardedBadges.badgeId == (awarded ? [badge.badgeId] : [])
            }
        }
        // Nonmembers cannot read the protected project; verify their retained credit and excluded awards directly.
        users.take(5).every { String userId -> achievement('TestProject2', userId, source.skillId).achievedOn == completionDate }
        !achievement('TestProject2', users[2], badge.badgeId)
        !protectedCommunity || [3, 4].every { int index -> !achievement('TestProject2', users[index], badge.badgeId) }

        where:
        protectedCommunity << [false, true]
    }

    def "users who visit or earn local points during finalization receive all catalog credit when it finishes"() {
        Map fixture = catalogFixture()
        users.take(3).each { String userId -> earnSource(fixture, userId) }
        Map newSource = createSkill(1, 1, 19, 0, 2, 0, 100)
        skillsService.createSkill(newSource)
        users.take(3).each { String userId ->
            skillsService.addSkill(newSource, userId, firstDate)
            skillsService.addSkill(newSource, userId, completionDate)
        }
        skillsService.exportSkillToCatalog('TestProject1', newSource.skillId)
        createService(users[0]).initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        skillsService.bulkImportSkillsFromCatalog('TestProject2', 'TestSubject1', [newSource])
        Connection blocker = dataSource.connection
        blocker.createStatement().execute('select pg_advisory_lock(4072002)')
        installTrigger('catalog_test_pause', 'user_points', "if NEW.project_id = 'TestProject2' and NEW.skill_id = '${newSource.skillId}' then perform pg_advisory_xact_lock(4072002); end if; return NEW;")

        when:
        skillsService.finalizeSkillsImportFromCatalog('TestProject2', false)
        new PollingConditions(timeout: 15).eventually {
            assert jdbcTemplate.queryForObject("select count(*) from pg_locks where locktype = 'advisory' and objid = 4072002 and not granted", Integer) > 0
        }
        Map partial = createService(users[1]).initializeProjectProgress('TestProject2')
        skillsService.addSkill(fixture.local, users[2])
        Map empty = createService(users[3]).initializeProjectProgress('TestProject2')
        skillsService.addSkill(fixture.local, users[4])

        then:
        !partial.ready
        !empty.ready
        [users[1], users[2], users[3], users[4]].every { String userId -> participationRepo.readStatus('TestProject2', userId) == BackfillStatus.PENDING }
        !backfillTasks()

        when:
        blocker.createStatement().execute('select pg_advisory_unlock(4072002)')
        blocker.close()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        points('TestProject2', users[1], null) == 600
        points('TestProject2', users[2], null) == 700
        points('TestProject2', users[3], null) == 0
        points('TestProject2', users[4], null) == 100
        [users[1], users[2], users[3], users[4]].every { String userId -> participationRepo.readStatus('TestProject2', userId) == BackfillStatus.COMPLETED }
        [users[1], users[2]].every { String userId -> achievement('TestProject2', userId, newSource.skillId).achievedOn == completionDate }
        achievement('TestProject2', users[1], fixture.badge.badgeId)

        cleanup:
        if (!blocker.closed) {
            blocker.createStatement().execute('select pg_advisory_unlock(4072002)')
            blocker.close()
        }
    }

    def "retrying failed import finalization completes pending user backfill"() {
        Map fixture = catalogFixture(false)
        earnSource(fixture, users[0])
        createService(users[0]).initializeProjectProgress('TestProject2')
        importSkills(fixture)
        installTrigger('catalog_test_fault', 'user_achievement', "if NEW.project_id = 'TestProject2' and NEW.skill_id = '${fixture.sourceSkills[0].skillId}' then raise exception 'ordinary catalog failure'; end if; return NEW;")

        when:
        skillsService.finalizeSkillsImportFromCatalog('TestProject2')
        SkillsService lateUser = createService(users[1])
        Map pending = lateUser.initializeProjectProgress('TestProject2')
        skillsService.addSkill(fixture.local, users[2])
        then:
        skillsService.getSetting('TestProject2', 'catalog.finalize.state').value == 'FAILED'
        !pending.ready
        participationRepo.readStatus('TestProject2', users[2]) == BackfillStatus.PENDING
        points('TestProject2', users[0], fixture.sourceSkills[0].skillId) == 200
        !backfillTasks()

        when:
        removeTrigger('catalog_test_fault', 'user_achievement')
        skillsService.finalizeSkillsImportFromCatalog('TestProject2')
        then:
        skillsService.getSetting('TestProject2', 'catalog.finalize.state').value == 'COMPLETED'
        participationRepo.readStatus('TestProject2', users[1]) == BackfillStatus.COMPLETED
        participationRepo.readStatus('TestProject2', users[2]) == BackfillStatus.COMPLETED
        points('TestProject2', users[0], null) == 400
        achievement('TestProject2', users[0], fixture.sourceSkills[0].skillId)
    }

    def "failed backfill rolls back copied credit and completes after an automatic retry"() {
        Map fixture = catalogFixture()
        earnSource(fixture, users[0])
        installTrigger('catalog_test_fault', 'user_achievement', "if NEW.project_id = 'TestProject2' and NEW.skill_id = '${fixture.sourceSkills[0].skillId}' then raise exception 'ordinary worker failure'; end if; return NEW;")
        SkillsService learner = createService(users[0])

        when:
        learner.initializeProjectProgress('TestProject2')
        new PollingConditions(timeout: 15).eventually {
            assert backfillTasks().first().consecutive_failures > 0
        }
        List<Map> failedTask = backfillTasks()
        Map partial = learner.initializeProjectProgress('TestProject2')
        then:
        !partial.ready
        points('TestProject2', users[0], fixture.sourceSkills[0].skillId) == 0
        !achievement('TestProject2', users[0], fixture.sourceSkills[0].skillId)
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.PENDING
        backfillTasks() == failedTask

        when:
        removeTrigger('catalog_test_fault', 'user_achievement')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        points('TestProject2', users[0], null) == 400
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.COMPLETED
    }

    def "failing to schedule backfill rolls back user enrollment and local points - #entry"() {
        Map fixture = catalogFixture()
        earnSource(fixture, users[0])
        installTrigger('catalog_test_schedule_fault', 'scheduled_tasks', "if NEW.task_name = 'catalog-participant-backfill' then raise exception 'ordinary scheduling failure'; end if; return NEW;")

        when:
        qualify(fixture, createService(users[0]), entry)
        then:
        thrown(SkillsClientException)
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[0])
        points('TestProject2', users[0], fixture.local.skillId) == 0
        !backfillTasks()

        cleanup:
        removeTrigger('catalog_test_schedule_fault', 'scheduled_tasks')

        where:
        entry << ['visit', 'award']
    }

    def "resetting progress restores catalog credit synchronously and retains eligibility - #eligibility"() {
        Map fixture = catalogFixture(eligibility == 'participant')
        earnSource(fixture, users[0])
        SkillsService learner = createService(users[0])
        skillsService.addSkill(fixture.local, users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        if (eligibility == 'local-only') {
            importSkills(fixture)
            skillsService.finalizeSkillsImportFromCatalog('TestProject2')
            skillsService.createBadge(fixture.badge + [enabled: 'false'])
            skillsService.assignSkillsToBadge('TestProject2', fixture.badge.badgeId, fixture.sourceSkills.skillId)
            skillsService.createBadge(fixture.badge + [enabled: 'true'])
        }
        ProjectParticipation before = participationRepo.findByProjectIdAndUserId('TestProject2', users[0])
        Map originalSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        List<Map> sourceCreditBefore = creditSnapshot('TestProject1', users[0])
        // Hold any accidentally scheduled backfill so immediate restoration cannot pass due to a fast worker.
        queueBackfill()

        expect:
        eligibility == 'local-only' ? !before : before.backfillStatus == BackfillStatus.COMPLETED
        originalSummary.points == 500
        originalSummary.skillsLevel == 3
        originalSummary.skillsAchieved == 2
        originalSummary.badges.numBadgesCompleted == 1

        when:
        Date resetStart = new Date()
        skillsService.deleteAllSkillEvents([projectId: 'TestProject2', userId: users[0]])
        Map restoredSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        ProjectParticipation after = participationRepo.findByProjectIdAndUserId('TestProject2', users[0])
        then:
        restoredSummary.points == 400
        restoredSummary.totalPoints == 900
        restoredSummary.skillsLevel == 2
        restoredSummary.skillsAchieved == 2
        restoredSummary.totalSkills == 3
        restoredSummary.subjects.subjectId == ['TestSubject1']
        restoredSummary.subjects[0].points == 400
        restoredSummary.subjects[0].totalPoints == 900
        restoredSummary.subjects[0].skillsLevel == 2
        restoredSummary.subjects[0].skillsAchieved == 2
        restoredSummary.subjects[0].totalSkills == 3
        restoredSummary.badges.numTotalBadges == 1
        restoredSummary.badges.numBadgesCompleted == 1
        restoredSummary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        Instant.parse(restoredSummary.badges.recentlyAwardedBadges[0].achievedOn).toEpochMilli() == completionDate.time
        points('TestProject2', users[0], fixture.local.skillId) == 0
        fixture.sourceSkills.every { Map skill -> achievement('TestProject2', users[0], skill.skillId).achievedOn == completionDate }
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], 'TestProject2', null)*.level.sort() == [1, 2]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], 'TestProject2', 'TestSubject1')*.level.sort() == [1, 2]
        achievement('TestProject2', users[0], fixture.group.skillId).achievedOn >= resetStart
        creditSnapshot('TestProject1', users[0]) == sourceCreditBefore
        after.backfillStatus == BackfillStatus.COMPLETED
        !before || (after.id == before.id && after.created == before.created)
        before || after.created >= resetStart
        learner.initializeProjectProgress('TestProject2').ready
        !backfillTasks()

        where:
        eligibility << ['participant', 'local-only']
    }

    def "resetting protected project progress restores catalog credit only for current community members"() {
        SkillsService owner = createService(users[7])
        SkillsService root = createRootSkillService()
        List<SkillsService> learners = users.take(2).collect { String userId -> createService(userId) }
        (users.take(2) + users[7]).each { String userId ->
            root.saveUserTag(userId, 'dragons', ['DivineDragon'])
        }
        List<Map> sourceSkills = (11..12).collect { int id -> createSkill(1, 1, id, 0, 2, 0, 100) }
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), sourceSkills)
        sourceSkills.each { Map skill -> skillsService.exportSkillToCatalog('TestProject1', skill.skillId) }
        Map local = createSkill(2, 1, 21, 0, 5, 0, 100)
        owner.createProjectAndSubjectAndSkills(createProject(2) + [enableProtectedUserCommunity: true],
                createSubject(2, 1), [local])
        owner.bulkImportSkillsFromCatalogAndFinalize('TestProject2', 'TestSubject1', sourceSkills)
        users.take(2).each { String userId ->
            earnSource([sourceSkills: sourceSkills], userId)
            owner.addSkill(local, userId, firstDate)
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<List<Map>> sourceCreditBefore = users.take(2).collect { String userId -> creditSnapshot('TestProject1', userId) }
        List<Map> enrollmentsBefore = users.take(2).collect { String userId ->
            ProjectParticipation participant = participationRepo.findByProjectIdAndUserId('TestProject2', userId)
            [id: participant.id, created: participant.created]
        }

        expect:
        learners.each { SkillsService learner ->
            Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
            assert summary.points == 500
            assert summary.skillsAchieved == 2
            assert participationRepo.readStatus('TestProject2', learner.userName) == BackfillStatus.COMPLETED
        }
        !backfillTasks()

        when:
        // Membership can change after credit was earned. The root API only adds tags.
        userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(users[1], 'dragons'))
        // Hold any accidentally scheduled task to prove restoration happens within the reset request.
        queueBackfill()
        users.take(2).each { String userId ->
            owner.deleteAllSkillEvents([projectId: 'TestProject2', userId: userId])
        }
        Map memberSummary = learners[0].getSkillsSummaryForCurrentUser('TestProject2')
        Map formerMemberSummary = owner.getSkillsSummaryForUser('TestProject2', users[1])

        then:
        memberSummary.points == 400
        memberSummary.totalPoints == 900
        memberSummary.skillsAchieved == 2
        memberSummary.subjects[0].points == 400
        memberSummary.subjects[0].skillsAchieved == 2
        formerMemberSummary.points == 0
        formerMemberSummary.skillsAchieved == 0
        formerMemberSummary.subjects[0].points == 0
        formerMemberSummary.subjects[0].skillsAchieved == 0
        // Also prove excluded credit was not recreated in storage behind a filtered response.
        !creditSnapshot('TestProject2', users[1])
        sourceSkills.every { Map skill -> achievement('TestProject2', users[0], skill.skillId).achievedOn == completionDate }
        users.take(2).collect { String userId -> creditSnapshot('TestProject1', userId) } == sourceCreditBefore
        learners.each { SkillsService learner ->
            Map summary = learner.getSkillsSummaryForCurrentUser('TestProject1')
            assert summary.points == 400
            assert summary.skillsAchieved == 2
        }
        enrollmentsBefore.eachWithIndex { Map before, int index ->
            ProjectParticipation participant = participationRepo.findByProjectIdAndUserId('TestProject2', users[index])
            assert participant.id == before.id
            assert participant.created == before.created
            assert participant.backfillStatus == BackfillStatus.COMPLETED
        }
        !backfillTasks()
    }

    def "resetting progress during #state finalization defers restoration until finalization succeeds - #eligibility"() {
        Map fixture = catalogFixture(eligibility != 'local-only')
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        skillsService.addSkill(fixture.local, users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        if (eligibility == 'local-only') {
            importSkills(fixture)
            skillsService.finalizeSkillsImportFromCatalog('TestProject2')
        }
        if (eligibility == 'reuse') {
            Map subject = createSubject(2, 2)
            skillsService.createSubject(subject)
            skillsService.reuseSkillInAnotherSubject('TestProject2', fixture.sourceSkills[0].skillId, subject.subjectId)
            waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        }
        ProjectParticipation before = participationRepo.findByProjectIdAndUserId('TestProject2', users[0])
        Map additional = createSkill(1, 1, 99, 0, 2, 0, 100)
        skillsService.createSkill(additional)
        skillsService.addSkill(additional, users[0], firstDate)
        skillsService.addSkill(additional, users[0], completionDate)
        skillsService.exportSkillToCatalog('TestProject1', additional.skillId)
        skillsService.bulkImportSkillsFromCatalog('TestProject2', 'TestSubject1', [additional])
        List<Map> sourceCreditBefore = creditSnapshot('TestProject1', users[0])
        if (state == 'running') {
            installTrigger('catalog_test_queue', 'scheduled_tasks', "if NEW.task_name = 'finalize-catalog-imports' then NEW.execution_time := now() + interval '1 hour'; end if; return NEW;")
        } else {
            installTrigger('catalog_test_fault', 'user_achievement', "if NEW.project_id = 'TestProject2' and NEW.skill_id = '${additional.skillId}' then raise exception 'reset finalization failure'; end if; return NEW;")
        }
        skillsService.finalizeSkillsImportFromCatalog('TestProject2', state == 'failed')

        expect:
        skillsService.getSetting('TestProject2', 'catalog.finalize.state').value == state.toUpperCase()
        eligibility == 'local-only' ? !before : before.backfillStatus == BackfillStatus.COMPLETED

        when:
        skillsService.deleteAllSkillEvents([projectId: 'TestProject2', userId: users[0]])
        Map pending = learner.initializeProjectProgress('TestProject2')
        Map pendingSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        ProjectParticipation after = participationRepo.findByProjectIdAndUserId('TestProject2', users[0])
        then:
        !pending.ready
        pending.backfillStatus == 'PENDING'
        pendingSummary.points == 0
        pendingSummary.skillsAchieved == 0
        after.backfillStatus == BackfillStatus.PENDING
        !before || (after.id == before.id && after.created == before.created)
        creditSnapshot('TestProject1', users[0]) == sourceCreditBefore
        !backfillTasks()

        when:
        removeTrigger('catalog_test_queue', 'scheduled_tasks')
        removeTrigger('catalog_test_fault', 'user_achievement')
        if (state == 'running') {
            jdbcTemplate.update("update scheduled_tasks set execution_time = now() where task_name = 'finalize-catalog-imports'")
        } else {
            skillsService.finalizeSkillsImportFromCatalog('TestProject2')
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        Map ready = learner.initializeProjectProgress('TestProject2')
        then:
        ready.ready
        ready.backfillStatus == 'COMPLETED'
        summary.points == (eligibility == 'reuse' ? 800 : 600)
        summary.totalPoints == (eligibility == 'reuse' ? 1300 : 1100)
        summary.skillsLevel == 3
        summary.skillsAchieved == (eligibility == 'reuse' ? 4 : 3)
        summary.totalSkills == (eligibility == 'reuse' ? 5 : 4)
        summary.subjects[0].points == 600
        summary.subjects[0].skillsLevel == 3
        summary.subjects[0].skillsAchieved == 3
        if (eligibility == 'reuse') {
            Map secondSubject = summary.subjects.find { Map row -> row.subjectId == 'TestSubject2' }
            assert secondSubject.points == 200
            assert secondSubject.skillsLevel == 5
            assert secondSubject.skillsAchieved == 1
        }
        summary.badges.numBadgesCompleted == (eligibility == 'local-only' ? 0 : 1)
        points('TestProject2', users[0], fixture.local.skillId) == 0
        achievement('TestProject2', users[0], additional.skillId).achievedOn == completionDate
        participationRepo.findById(after.id).get().created == after.created
        creditSnapshot('TestProject1', users[0]) == sourceCreditBefore
        !backfillTasks()

        cleanup:
        removeTrigger('catalog_test_queue', 'scheduled_tasks')
        removeTrigger('catalog_test_fault', 'user_achievement')
        jdbcTemplate.update("update scheduled_tasks set execution_time = now() where task_name = 'finalize-catalog-imports'")

        where:
        state     | eligibility
        'running' | 'participant'
        'failed'  | 'participant'
        'running' | 'local-only'
        'failed'  | 'local-only'
        'running' | 'reuse'
        'failed'  | 'reuse'
    }

    def "resetting an unrelated user does not grant catalog credit or enroll the user"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        List<Map> sourceCreditBefore = creditSnapshot('TestProject1', users[0])

        when:
        skillsService.deleteAllSkillEvents([projectId: 'TestProject2', userId: users[0]])
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        summary.points == 0
        summary.skillsLevel == 0
        summary.skillsAchieved == 0
        summary.badges.numBadgesCompleted == 0
        !creditSnapshot('TestProject2', users[0])
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[0])
        creditSnapshot('TestProject1', users[0]) == sourceCreditBefore
        !backfillTasks()
    }

    def "resetting source project progress removes the user's catalog credit from a project that imported its skills"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        learner.initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        ProjectParticipation before = participationRepo.findByProjectIdAndUserId('TestProject2', users[0])

        expect:
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 400

        when:
        skillsService.deleteAllSkillEvents([projectId: 'TestProject1', userId: users[0]])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map sourceSummary = learner.getSkillsSummaryForCurrentUser('TestProject1')
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        sourceSummary.points == 0
        sourceSummary.skillsAchieved == 0
        summary.points == 0
        summary.skillsLevel == 0
        summary.skillsAchieved == 0
        summary.subjects[0].points == 0
        summary.subjects[0].skillsAchieved == 0
        summary.badges.numBadgesCompleted == 0
        !summary.badges.recentlyAwardedBadges
        participationRepo.findById(before.id).get().created == before.created
        !backfillTasks()
    }

    def "failed synchronous restoration rolls back the entire progress reset"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        skillsService.addSkill(fixture.local, users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summaryBefore = learner.getSkillsSummaryForCurrentUser('TestProject2')
        List<Map> creditBefore = creditSnapshot('TestProject2', users[0])
        List<Map> sourceCreditBefore = creditSnapshot('TestProject1', users[0])
        ProjectParticipation before = participationRepo.findByProjectIdAndUserId('TestProject2', users[0])
        installTrigger('catalog_test_fault', 'user_achievement', "if NEW.project_id = 'TestProject2' and NEW.skill_id = '${fixture.sourceSkills[0].skillId}' then raise exception 'synchronous reset failure'; end if; return NEW;")

        when:
        skillsService.deleteAllSkillEvents([projectId: 'TestProject2', userId: users[0]])
        then:
        thrown(SkillsClientException)
        summaryBefore.points == 500
        learner.getSkillsSummaryForCurrentUser('TestProject2') == summaryBefore
        creditSnapshot('TestProject2', users[0]) == creditBefore
        creditSnapshot('TestProject1', users[0]) == sourceCreditBefore
        participationRepo.findById(before.id).get().created == before.created
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.COMPLETED
        !backfillTasks()
    }

    def "resetting progress restores catalog skills reused in another subject before recalculating project levels"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        skillsService.addSkill(fixture.local, users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map subject = createSubject(2, 2)
        skillsService.createSubject(subject)
        skillsService.reuseSkillInAnotherSubject('TestProject2', fixture.sourceSkills[0].skillId, subject.subjectId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<Map> sourceCreditBefore = creditSnapshot('TestProject1', users[0])
        queueBackfill()

        when:
        skillsService.deleteAllSkillEvents([projectId: 'TestProject2', userId: users[0]])
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        Map firstSubject = summary.subjects.find { Map row -> row.subjectId == 'TestSubject1' }
        Map secondSubject = summary.subjects.find { Map row -> row.subjectId == subject.subjectId }
        List<Map> restoredCredit = creditSnapshot('TestProject2', users[0])
        then:
        summary.points == 600
        summary.totalPoints == 1100
        summary.skillsLevel == 3
        summary.skillsAchieved == 3
        summary.totalSkills == 4
        firstSubject.points == 400
        firstSubject.skillsLevel == 2
        firstSubject.skillsAchieved == 2
        secondSubject.points == 200
        secondSubject.skillsLevel == 5
        secondSubject.skillsAchieved == 1
        summary.badges.numBadgesCompleted == 1
        summary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        // The overall summary sums subjects; check that its project aggregate is also restored.
        points('TestProject2', users[0], null) == 600
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], 'TestProject2', null)*.level.sort() == [1, 2, 3]
        creditSnapshot('TestProject1', users[0]) == sourceCreditBefore
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.COMPLETED
        !backfillTasks()

        when:
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        learner.getSkillsSummaryForCurrentUser('TestProject2') == summary
        creditSnapshot('TestProject2', users[0]) == restoredCredit
    }

    def "reporting a local skill for multiple users schedules one backfill per user"() {
        Map fixture = catalogFixture()
        users.take(3).each { String userId -> earnSource(fixture, userId) }
        queueBackfill()

        when:
        skillsService.bulkAddSkill(fixture.local, users.take(3), new Date())
        then:
        backfillTasks().size() == 3
        users.take(3).every { String userId -> participationRepo.readStatus('TestProject2', userId) == BackfillStatus.PENDING }

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        users.take(3).every { String userId -> points('TestProject2', userId, null) == 500 }
    }

    def "passing a shared quiz schedules separate backfill for each project that awards local points"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        skillsService.bulkImportSkillsFromCatalogAndFinalize('TestProject3', 'TestSubject1', fixture.sourceSkills)
        Map quiz = quizFixture()
        Map local2 = fixture.local + [selfReportingType: 'Quiz', quizId: quiz.quizId, numPerformToCompletion: 1]
        Map local3 = createSkill(3, 1, 21, 0, 1, 0, 100) + [selfReportingType: 'Quiz', quizId: quiz.quizId]
        skillsService.createSkill(local2)
        skillsService.createSkill(local3)
        queueBackfill()

        when:
        passQuiz(learner, quiz)
        then:
        backfillTasks().size() == 2
        ['TestProject2', 'TestProject3'].every { String projectId -> participationRepo.readStatus(projectId, users[0]) == BackfillStatus.PENDING }

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary2 = learner.getSkillsSummaryForCurrentUser('TestProject2')
        Map summary3 = learner.getSkillsSummaryForCurrentUser('TestProject3')
        then:
        [summary2, summary3].each { Map summary ->
            assert summary.points == 500
            assert summary.totalPoints == 500
            assert summary.skillsLevel == 5
            assert summary.skillsAchieved == 3
            assert summary.totalSkills == 3
            assert summary.subjects.subjectId == ['TestSubject1']
            assert summary.subjects[0].points == 500
            assert summary.subjects[0].totalPoints == 500
            assert summary.subjects[0].skillsLevel == 5
            assert summary.subjects[0].skillsAchieved == 3
            assert summary.subjects[0].totalSkills == 3
        }
        summary2.badges.numTotalBadges == 1
        summary2.badges.numBadgesCompleted == 1
        summary2.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        summary3.badges.numTotalBadges == 0
        summary3.badges.numBadgesCompleted == 0
        !summary3.badges.recentlyAwardedBadges
    }

    def "assigning a previously passed quiz awards local points and triggers catalog backfill"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        Map quiz = quizFixture()
        passQuiz(learner, quiz)
        Date quizCompleted = jdbcTemplate.queryForObject('select completed from user_quiz_attempt where user_id = ?', Date, users[0])
        queueBackfill()

        when:
        skillsService.createSkill(fixture.local + [selfReportingType: 'Quiz', quizId: quiz.quizId, numPerformToCompletion: 1])
        Map pendingSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        pendingSummary.points == 100
        pendingSummary.skillsLevel == 1
        pendingSummary.skillsAchieved == 1
        pendingSummary.badges.numBadgesCompleted == 0
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.PENDING
        backfillTasks().size() == 1
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], 'TestProject2', null).find { UserAchievement award -> award.level == 1 }.achievedOn == quizCompleted

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        summary.points == 500
        summary.totalPoints == 500
        summary.skillsLevel == 5
        summary.skillsAchieved == 3
        summary.totalSkills == 3
        summary.subjects.subjectId == ['TestSubject1']
        summary.subjects[0].points == 500
        summary.subjects[0].totalPoints == 500
        summary.subjects[0].skillsLevel == 5
        summary.subjects[0].skillsAchieved == 3
        summary.subjects[0].totalSkills == 3
        summary.badges.numTotalBadges == 1
        summary.badges.numBadgesCompleted == 1
        summary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], 'TestProject2', null).find { UserAchievement award -> award.level == 1 }.achievedOn == quizCompleted
    }

    def "assigning a quiz backfills multiple previous passers without crediting other quiz passers or restarting completed backfill"() {
        Map fixture = catalogFixture()
        List<SkillsService> learners = users.take(4).collect { String userId -> createService(userId) }
        users.take(4).each { String userId -> earnSource(fixture, userId) }
        Map quiz = quizFixture()
        Map otherQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(otherQuiz)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, 1, 2))
        [0, 1, 3].each { int index -> passQuiz(learners[index], quiz) }
        passQuiz(learners[2], otherQuiz)
        // Keep a different quiz assignment in the same project to exercise the skill-reference filter.
        // Its disabled skill must not retroactively award local credit to the other quiz's passer.
        Map otherSkill = createSkill(2, 1, 23, 0, 1, 0, 100) +
                [selfReportingType: 'Quiz', quizId: otherQuiz.quizId, enabled: 'false']
        skillsService.createSkill(otherSkill)
        skillsService.addSkill(fixture.local, users[3], firstDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        ProjectParticipation existing = participationRepo.findByProjectIdAndUserId('TestProject2', users[3])
        Map enrollmentBefore = [id: existing.id, created: existing.created, updated: existing.updated,
                                status: existing.backfillStatus]
        Map quizSkill = createSkill(2, 1, 22, 0, 1, 0, 100) +
                [selfReportingType: 'Quiz', quizId: quiz.quizId]
        queueBackfill()

        expect:
        enrollmentBefore.status == BackfillStatus.COMPLETED
        learners[3].getSkillsSummaryForCurrentUser('TestProject2').points == 500
        users.take(3).every { String userId -> !participationRepo.existsByProjectIdAndUserId('TestProject2', userId) }
        !backfillTasks()

        when:
        skillsService.createSkill(quizSkill)

        then:
        backfillTasks().size() == 2
        [0, 1].each { int index ->
            Map summary = learners[index].getSkillsSummaryForCurrentUser('TestProject2')
            assert summary.points == 100
            assert summary.skillsAchieved == 1
            assert summary.badges.numBadgesCompleted == 0
            assert participationRepo.readStatus('TestProject2', users[index]) == BackfillStatus.PENDING
        }
        learners[2].getSkillsSummaryForCurrentUser('TestProject2').points == 0
        learners[2].getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 0
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[2])
        learners[3].getSkillsSummaryForCurrentUser('TestProject2').points == 600
        learners[3].getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 3
        participationRepo.findById(enrollmentBefore.id).get().with { ProjectParticipation participant ->
            participant.created == enrollmentBefore.created && participant.updated == enrollmentBefore.updated &&
                    participant.backfillStatus == enrollmentBefore.status
        }

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        [0, 1, 3].each { int index ->
            Map summary = learners[index].getSkillsSummaryForCurrentUser('TestProject2')
            assert summary.points == (index == 3 ? 600 : 500)
            assert summary.totalPoints == 1000
            assert summary.skillsAchieved == 3
            assert summary.totalSkills == 4
            assert summary.subjects[0].points == summary.points
            assert summary.subjects[0].skillsAchieved == summary.skillsAchieved
            assert summary.badges.numBadgesCompleted == 1
            assert summary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
            assert participationRepo.readStatus('TestProject2', users[index]) == BackfillStatus.COMPLETED
        }
        Map excludedSummary = learners[2].getSkillsSummaryForCurrentUser('TestProject2')
        excludedSummary.points == 0
        excludedSummary.skillsAchieved == 0
        excludedSummary.badges.numBadgesCompleted == 0
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[2])
        !backfillTasks()
    }

    def "copying a project preserves earned quiz points without enrolling users for catalog backfill"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        Map quiz = quizFixture()
        skillsService.createSkill(fixture.local + [selfReportingType: 'Quiz', quizId: quiz.quizId, numPerformToCompletion: 1])
        passQuiz(learner, quiz)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        skillsService.copyProject('TestProject2', createProject(4))
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject4')
        then:
        summary.points == 100
        summary.totalPoints == 100
        summary.skillsLevel == 5
        summary.skillsAchieved == 1
        summary.totalSkills == 1
        summary.subjects.subjectId == ['TestSubject1']
        summary.subjects[0].points == 100
        summary.subjects[0].totalPoints == 100
        summary.subjects[0].skillsLevel == 5
        summary.subjects[0].skillsAchieved == 1
        summary.subjects[0].totalSkills == 1
        summary.badges.numTotalBadges == 0
        summary.badges.numBadgesCompleted == 0
        points('TestProject4', users[0], fixture.local.skillId) == 100
        !participationRepo.existsByProjectIdAndUserId('TestProject4', users[0])
        !backfillTasks()
    }

    def "revisiting a project with pending backfill does not recreate missing tasks or wait for reporting locks"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress('TestProject2')
        jdbcTemplate.update("update project_participation set backfill_status = 'PENDING' where project_id = 'TestProject2' and user_id = ?", users[0])
        ProjectParticipation before = participationRepo.findByProjectIdAndUserId('TestProject2', users[0])
        CountDownLatch locked = new CountDownLatch(1)
        CountDownLatch release = new CountDownLatch(1)
        def executor = Executors.newSingleThreadExecutor()
        def held = executor.submit({
            new TransactionTemplate(transactionManager).execute {
                lockingService.lockForSkillReporting(users[0], 'TestProject2')
                locked.countDown()
                assert release.await(10, TimeUnit.SECONDS)
            }
        } as Callable)
        assert locked.await(10, TimeUnit.SECONDS)

        when:
        Map result = learner.initializeProjectProgress('TestProject2')
        then:
        !result.ready
        result.backfillStatus == 'PENDING'
        !held.done
        !backfillTasks()
        participationRepo.findById(before.id).get().created == before.created
        participationRepo.findById(before.id).get().updated > before.updated

        cleanup:
        release.countDown()
        held.get(10, TimeUnit.SECONDS)
        executor.shutdownNow()
    }

    def "backfill uses imported point values and preserves local points while skipping unfinalized imports"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        fixture.sourceSkills.each { Map skill -> skillsService.updateImportedSkill('TestProject2', skill.skillId, 250) }
        Map pendingSkill = createSkill(1, 1, 99, 0, 2, 0, 100)
        skillsService.createSkill(pendingSkill)
        skillsService.addSkill(pendingSkill, users[0], firstDate)
        skillsService.addSkill(pendingSkill, users[0], completionDate)
        skillsService.exportSkillToCatalog('TestProject1', pendingSkill.skillId)
        skillsService.bulkImportSkillsFromCatalog('TestProject2', 'TestSubject1', [pendingSkill])

        when:
        skillsService.addSkill(fixture.local, users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        summary.points == 1100
        summary.totalPoints == 1500
        summary.skillsLevel == 4
        summary.skillsAchieved == 2
        summary.totalSkills == 3
        summary.subjects.subjectId == ['TestSubject1']
        summary.subjects[0].points == 1100
        summary.subjects[0].totalPoints == 1500
        summary.subjects[0].skillsLevel == 4
        summary.subjects[0].skillsAchieved == 2
        summary.subjects[0].totalSkills == 3
        summary.badges.numTotalBadges == 1
        summary.badges.numBadgesCompleted == 1
        summary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        fixture.sourceSkills.every { Map skill -> points('TestProject2', users[0], skill.skillId) == 500 }
        fixture.sourceSkills.every { Map skill -> achievement('TestProject2', users[0], skill.skillId).achievedOn == completionDate }
        points('TestProject2', users[0], pendingSkill.skillId) == 0
        !achievement('TestProject2', users[0], pendingSkill.skillId)
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.COMPLETED
    }

    def "completing a quiz prerequisite awards both local skills and schedules only one catalog backfill"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        Map quiz = quizFixture()
        fixture.local.numPerformToCompletion = 1
        skillsService.createSkill(fixture.local)
        Map quizSkill = createSkill(2, 1, 22, 0, 1, 0, 100) + [selfReportingType: 'Quiz', quizId: quiz.quizId]
        skillsService.createSkill(quizSkill)
        // Passing the quiz alone is insufficient until the native prerequisite is completed.
        skillsService.addLearningPathPrerequisite('TestProject2', quizSkill.skillId, fixture.local.skillId)
        passQuiz(learner, quiz)
        queueBackfill()

        when:
        skillsService.addSkill(fixture.local, users[0])
        Map pendingSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        pendingSummary.points == 200
        pendingSummary.skillsLevel == 2
        pendingSummary.skillsAchieved == 2
        pendingSummary.badges.numBadgesCompleted == 0
        points('TestProject2', users[0], quizSkill.skillId) == 100
        backfillTasks().size() == 1
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.PENDING

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        summary.points == 600
        summary.totalPoints == 600
        summary.skillsLevel == 5
        summary.skillsAchieved == 4
        summary.totalSkills == 4
        summary.subjects.subjectId == ['TestSubject1']
        summary.subjects[0].points == 600
        summary.subjects[0].totalPoints == 600
        summary.subjects[0].skillsLevel == 5
        summary.subjects[0].skillsAchieved == 4
        summary.subjects[0].totalSkills == 4
        summary.badges.numTotalBadges == 1
        summary.badges.numBadgesCompleted == 1
        summary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        points('TestProject2', users[0], quizSkill.skillId) == 100
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.COMPLETED
        !backfillTasks()
    }

    def "only community members receive catalog credit during finalization or later backfill"() {
        Map source = createProject(1)
        Map sourceSubject = createSubject(1, 1)
        List<Map> sourceSkills = (11..12).collect { int id -> createSkill(1, 1, id, 0, 2, 0, 100) }
        skillsService.createProjectAndSubjectAndSkills(source, sourceSubject, sourceSkills)
        sourceSkills.each { Map skill -> skillsService.exportSkillToCatalog('TestProject1', skill.skillId) }
        SkillsService owner = createService(users[7])
        SkillsService root = createRootSkillService()
        [users[7], users[0], users[1], users[2]].each { String userId ->
            createService(userId)
            root.saveUserTag(userId, 'dragons', ['DivineDragon'])
        }
        Map project = createProject(2) + [enableProtectedUserCommunity: true]
        Map local = createSkill(2, 1, 21, 0, 5, 0, 100)
        owner.createProjectAndSubjectAndSkills(project, createSubject(2, 1), [local])
        Map fixture = [sourceSkills: sourceSkills]
        users.take(4).each { String userId -> earnSource(fixture, userId) }
        createService(users[0]).initializeProjectProgress('TestProject2')
        owner.bulkImportSkillsFromCatalogAndFinalize('TestProject2', 'TestSubject1', sourceSkills)

        expect:
        points('TestProject2', users[0], null) == 400
        points('TestProject2', users[3], null) == 0

        when:
        createService(users[1]).initializeProjectProgress('TestProject2')
        owner.addSkill(local, users[2])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        points('TestProject2', users[1], null) == 400
        points('TestProject2', users[2], null) == 500
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[3])

        when:
        createService(users[3]).initializeProjectProgress('TestProject2')
        then:
        thrown(SkillsClientException)
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[3])

        when:
        SkillsService nonmember = createService(users[4])
        earnSource(fixture, users[4])
        Map sourceSummary = nonmember.getSkillsSummaryForCurrentUser('TestProject1')
        then:
        sourceSummary.points == 400
        sourceSummary.skillsAchieved == 2
        !creditSnapshot('TestProject2', users[4])
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[4])
    }

    def "queued backfill does not recreate credit after deleting the #deleted"() {
        Map fixture = catalogFixture()
        earnSource(fixture, users[0])
        queueBackfill()
        createService(users[0]).initializeProjectProgress('TestProject2')

        when:
        if (deleted == 'project') {
            skillsService.deleteProject('TestProject2')
        } else {
            jdbcTemplate.update('delete from user_attrs where user_id = ?', users[0])
        }
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[0])
        points('TestProject2', users[0], null) == 0
        !backfillTasks()

        where:
        deleted << ['project', 'user']
    }

    def "rerunning completed backfill leaves stored credit and enrollment timestamps unchanged"() {
        Map fixture = catalogFixture()
        earnSource(fixture, users[0])
        createService(users[0]).initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<Map> before = creditSnapshot('TestProject2', users[0])
        ProjectParticipation participation = participationRepo.findByProjectIdAndUserId('TestProject2', users[0])

        when:
        backfillService.backfill('TestProject2', users[0])
        then:
        creditSnapshot('TestProject2', users[0]) == before
        participationRepo.findById(participation.id).get().updated == participation.updated
        participationRepo.findById(participation.id).get().created == participation.created
    }

    def "later source reports add imported points without new backfill even when task data contains obsolete fields"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress('TestProject2')
        installTrigger('catalog_test_queue', 'scheduled_tasks', "if NEW.task_name = 'imported-skill-achievement' then NEW.execution_time := now() + interval '1 hour'; end if; return NEW;")

        when:
        skillsService.addSkill(fixture.sourceSkills[0], users[0], firstDate)
        jdbcTemplate.update("update scheduled_tasks set task_data = convert_to((convert_from(task_data, 'UTF8')::jsonb || '{\"obsoleteSnapshotField\": 123}'::jsonb)::text, 'UTF8') where task_name = 'imported-skill-achievement'")
        removeTrigger('catalog_test_queue', 'scheduled_tasks')
        jdbcTemplate.update("update scheduled_tasks set execution_time = now() where task_name = 'imported-skill-achievement'")
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        points('TestProject2', users[0], null) == 100
        learner.initializeProjectProgress('TestProject2').ready
        !backfillTasks()
    }

    def "backfill combines catalog credit across import batches and subjects with existing local points"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        Map subject = createSubject(2, 2)
        skillsService.createSubject(subject)
        Map additional = createSkill(1, 1, 99, 0, 2, 0, 100)
        skillsService.createSkill(additional)
        skillsService.addSkill(additional, users[0], firstDate)
        skillsService.addSkill(additional, users[0], completionDate)
        skillsService.exportSkillToCatalog('TestProject1', additional.skillId)
        skillsService.bulkImportSkillsFromCatalogAndFinalize('TestProject2', subject.subjectId, [additional])

        when:
        skillsService.addSkill(fixture.local, users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        Map firstSubjectSummary = summary.subjects.find { Map row -> row.subjectId == 'TestSubject1' }
        Map secondSubjectSummary = summary.subjects.find { Map row -> row.subjectId == subject.subjectId }
        then:
        summary.points == 700
        summary.totalPoints == 1100
        summary.skillsLevel == 3
        summary.skillsAchieved == 3
        summary.totalSkills == 4
        summary.subjects.subjectId.sort() == ['TestSubject1', subject.subjectId].sort()
        firstSubjectSummary.points == 500
        firstSubjectSummary.totalPoints == 900
        firstSubjectSummary.skillsLevel == 3
        firstSubjectSummary.skillsAchieved == 2
        firstSubjectSummary.totalSkills == 3
        secondSubjectSummary.points == 200
        secondSubjectSummary.totalPoints == 200
        secondSubjectSummary.skillsLevel == 5
        secondSubjectSummary.skillsAchieved == 1
        secondSubjectSummary.totalSkills == 1
        summary.badges.numTotalBadges == 1
        summary.badges.numBadgesCompleted == 1
        summary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        points('TestProject2', users[0], additional.skillId) == 200
        achievement('TestProject2', users[0], additional.skillId).achievedOn == completionDate
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], 'TestProject2', subject.subjectId)*.level.sort() == [1, 2, 3, 4, 5]
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.COMPLETED
        !creditSnapshot('TestProject3', users[0])
    }

    def "legacy imported credit stays unchanged until a project visit triggers backfill"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        fixture.group.numSkillsRequired = 2
        skillsService.createSkill(fixture.group)
        learner.initializeProjectProgress('TestProject2')
        earnSource(fixture, users[0])
        // Simulate pre-participation data while retaining credit created through real reports.
        ProjectParticipation participation = participationRepo.findByProjectIdAndUserId('TestProject2', users[0])
        participationRepo.delete(participation)
        List<Map> legacy = creditSnapshot('TestProject2', users[0])
        Map additional = createSkill(1, 1, 19, 0, 2, 0, 100)
        skillsService.createSkill(additional)
        skillsService.addSkill(additional, users[0], firstDate)
        skillsService.addSkill(additional, users[0], completionDate)
        skillsService.exportSkillToCatalog('TestProject1', additional.skillId)
        skillsService.bulkImportSkillsIntoGroupFromCatalog('TestProject2', 'TestSubject1', fixture.group.skillId, [additional])

        when:
        skillsService.finalizeSkillsImportFromCatalog('TestProject2')
        Map retainedSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        retainedSummary.points == 400
        retainedSummary.totalPoints == 1100
        retainedSummary.skillsLevel == 2
        retainedSummary.skillsAchieved == 2
        retainedSummary.totalSkills == 4
        retainedSummary.subjects.subjectId == ['TestSubject1']
        retainedSummary.subjects[0].points == 400
        retainedSummary.subjects[0].totalPoints == 1100
        retainedSummary.subjects[0].skillsLevel == 2
        retainedSummary.subjects[0].skillsAchieved == 2
        retainedSummary.subjects[0].totalSkills == 4
        retainedSummary.badges.numTotalBadges == 1
        retainedSummary.badges.numBadgesCompleted == 1
        retainedSummary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        creditSnapshot('TestProject2', users[0]) == legacy
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[0])
        achievement('TestProject2', users[0], fixture.group.skillId)
        achievement('TestProject2', users[0], fixture.badge.badgeId).achievedOn == completionDate
        points('TestProject2', users[0], additional.skillId) == 0

        when:
        learner.initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        summary.points == 600
        summary.totalPoints == 1100
        summary.skillsLevel == 3
        summary.skillsAchieved == 3
        summary.totalSkills == 4
        summary.subjects[0].points == 600
        summary.subjects[0].totalPoints == 1100
        summary.subjects[0].skillsLevel == 3
        summary.subjects[0].skillsAchieved == 3
        summary.subjects[0].totalSkills == 4
        summary.badges.numTotalBadges == 1
        summary.badges.numBadgesCompleted == 1
        summary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.COMPLETED
        legacy.findAll { Map row -> !row.containsKey('points') || row.skill_id in fixture.sourceSkills.skillId }
                .every { Map row -> creditSnapshot('TestProject2', users[0]).contains(row) }
        userPointsRepo.findByProjectIdAndUserIdAndSkillId('TestProject2', users[0], null).id == legacy.find { Map row -> row.containsKey('points') && row.skill_id == null }.id
        achievement('TestProject2', users[0], fixture.group.skillId)
        achievement('TestProject2', users[0], fixture.badge.badgeId).achievedOn == completionDate
    }

    private Map catalogFixture(boolean finalize = true, boolean grouped = true) {
        Map source = createProject(1)
        Map sourceSubject = createSubject(1, 1)
        List<Map> sourceSkills = (11..12).collect { int id -> createSkill(1, 1, id, 0, 2, 0, 100) }
        skillsService.createProjectAndSubjectAndSkills(source, sourceSubject, sourceSkills)
        sourceSkills.each { Map skill -> skillsService.exportSkillToCatalog(source.projectId, skill.skillId) }
        Map destination = createProject(2)
        Map subject = createSubject(2, 1)
        Map local = createSkill(2, 1, 21, 0, 5, 0, 100)
        Map group = createSkillsGroup(2, 1, 30)
        skillsService.createProjectAndSubjectAndSkills(destination, subject, grouped ? [local, group] : [local])
        skillsService.createProject(createProject(3))
        skillsService.createSubject(createSubject(3, 1))
        Map fixture = [sourceSkills: sourceSkills, local: local, group: grouped ? group : null, badge: createBadge(2, 1)]
        if (finalize) {
            importSkills(fixture)
            skillsService.finalizeSkillsImportFromCatalog('TestProject2')
            skillsService.createBadge(fixture.badge + [enabled: 'false'])
            skillsService.assignSkillsToBadge('TestProject2', fixture.badge.badgeId, sourceSkills.skillId)
            skillsService.createBadge(fixture.badge + [enabled: 'true'])
        }
        return fixture
    }

    private void importSkills(Map fixture) {
        if (fixture.group) {
            skillsService.bulkImportSkillsIntoGroupFromCatalog('TestProject2', 'TestSubject1', fixture.group.skillId, fixture.sourceSkills)
        } else {
            skillsService.bulkImportSkillsFromCatalog('TestProject2', 'TestSubject1', fixture.sourceSkills)
        }
    }

    private void saveSourceAchievement(Map skill, String userId) {
        SkillDef source = skillDefRepo.findByProjectIdAndSkillId(skill.projectId, skill.skillId)
        userAchievedRepo.save(new UserAchievement(userId: userId, projectId: source.projectId,
                skillId: source.skillId, skillRefId: source.id, pointsWhenAchieved: source.totalPoints,
                achievedOn: completionDate))
    }

    private void earnSource(Map fixture, String userId) {
        fixture.sourceSkills.each { Map skill ->
            skillsService.addSkill(skill, userId, firstDate)
            skillsService.addSkill(skill, userId, completionDate)
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
    }

    private Map qualify(Map fixture, SkillsService learner, String entry) {
        entry == 'visit' ? learner.initializeProjectProgress('TestProject2') : skillsService.addSkill(fixture.local, learner.userName).body
    }

    private int points(String projectId, String userId, String skillId) {
        userPointsRepo.findByProjectIdAndUserIdAndSkillId(projectId, userId, skillId)?.points ?: 0
    }

    private UserAchievement achievement(String projectId, String userId, String skillId) {
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, skillId).find { UserAchievement achievement -> achievement.level == null }
    }

    private List<Map> creditSnapshot(String projectId, String userId) {
        List<Map> points = userPointsRepo.findByProjectIdAndUserId(projectId, userId)
                .sort { UserPoints row -> row.id }
                .collect { UserPoints row -> [id: row.id, points: row.points, skill_id: row.skillId] }
        List<Map> achievements = userAchievedRepo.findAllByUserAndProjectIds(userId, [projectId])
                .sort { UserAchievement row -> row.id }
                .collect { UserAchievement row ->
                    [id: row.id, skill_id: row.skillId, level: row.level, achieved_on: row.achievedOn?.clone()]
                }
        return points + achievements
    }

    private Map quizFixture() {
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(1, 1, 2))
        return quiz
    }

    private void passQuiz(SkillsService learner, Map quiz) {
        Map attempt = learner.startQuizAttempt(quiz.quizId).body
        learner.reportQuizAnswer(quiz.quizId, attempt.id, attempt.questions[0].answerOptions[0].id)
        assert learner.completeQuizAttempt(quiz.quizId, attempt.id).body.passed
    }
}
