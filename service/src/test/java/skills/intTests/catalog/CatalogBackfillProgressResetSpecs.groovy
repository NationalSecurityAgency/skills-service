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
import java.time.Instant

import static skills.intTests.utils.SkillsFactory.*

class CatalogBackfillProgressResetSpecs extends CatalogBackfillSpecBase {

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

}
