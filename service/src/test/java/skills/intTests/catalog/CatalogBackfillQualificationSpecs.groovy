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

import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.UserAchievement
import skills.storage.model.SkillDef
import java.time.Instant

import static skills.intTests.utils.SkillsFactory.*

class CatalogBackfillQualificationSpecs extends CatalogBackfillSpecBase {

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


    def "incremental imported group completion preserves historical event metadata after initialization"() {
        Map fixture = catalogFixture()
        fixture.sourceSkills.each { Map skill -> skillsService.updateImportedSkill('TestProject2', skill.skillId, importedIncrement) }
        SkillsService learner = createService(users[0])
        assert learner.initializeProjectProgress('TestProject2').backfillStatus == 'COMPLETED'
        Map firstSkill = fixture.sourceSkills[0]
        Map lastSkill = fixture.sourceSkills[1]
        skillsService.addSkill(firstSkill, users[0], firstDate)
        skillsService.addSkill(firstSkill, users[0], new Date(firstDate.time + 1000))
        skillsService.addSkill(lastSkill, users[0], firstDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        expect:
        learner.getSkillsGroupSummary('TestProject2', fixture.group.skillId).skillsAchieved == 1
        !learner.getSubjectDescriptions('TestProject2', 'TestSubject1')
                .find { Map description -> description.skillId == fixture.group.skillId }.achievedOn

        when:
        skillsService.addSkill(lastSkill, users[0], completionDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map group = learner.getSubjectDescriptions('TestProject2', 'TestSubject1')
                .find { Map description -> description.skillId == fixture.group.skillId }

        then:
        learner.getSkillsGroupSummary('TestProject2', fixture.group.skillId).skillsAchieved == 2
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 4 * importedIncrement
        Instant.parse(group.achievedOn).toEpochMilli() == completionDate.time
        achievement('TestProject2', users[0], fixture.group.skillId).pointsWhenAchieved == 4 * importedIncrement

        where:
        importedIncrement << [100, 250]
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


    private void saveSourceAchievement(Map skill, String userId) {
        SkillDef source = skillDefRepo.findByProjectIdAndSkillId(skill.projectId, skill.skillId)
        userAchievedRepo.save(new UserAchievement(userId: userId, projectId: source.projectId,
                skillId: source.skillId, skillRefId: source.id, pointsWhenAchieved: source.totalPoints,
                achievedOn: completionDate))
    }

}
