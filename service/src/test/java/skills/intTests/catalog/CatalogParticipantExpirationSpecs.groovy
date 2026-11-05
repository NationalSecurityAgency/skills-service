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
import skills.intTests.utils.SkillsService
import skills.services.attributes.ExpirationAttrs
import skills.services.settings.SettingsService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.repos.ProjectParticipationRepo
import skills.tasks.executors.ExpireUserAchievementsTaskExecutor

import java.time.Instant
import java.time.LocalDateTime

import static skills.intTests.utils.SkillsFactory.*

class CatalogParticipantExpirationSpecs extends CatalogIntSpec {
    @Autowired ProjectParticipationRepo participationRepo
    @Autowired ExpireUserAchievementsTaskExecutor expirationExecutor
    @Autowired SettingsService settingsService

    private static final String QUEUE = 'catalog_expiration_queue'

    def cleanup() {
        releaseSourceTasks()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        settingsService.deleteGlobalSetting(ExpireUserAchievementsTaskExecutor.SKILL_EXPIRATION_LAST_RUN_DATE)
    }

    def "explicit membership survives #expirationType source expiration and re-earning without another visit"() {
        Map fixture = fixture()
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        SkillsService outsider = createService(users[1])
        learner.initializeProjectProgress(fixture.projectId)
        ProjectParticipation membership = participationRepo.findByProjectIdAndUserId(fixture.projectId, users[0])
        Map originalMembership = [id: membership.id, created: membership.created, updated: membership.updated]
        Date earned = new Date() - 10
        users.each { String userId -> skillsService.addSkill(fixture.source, userId, earned) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        assert learner.getSkillsSummaryForCurrentUser(fixture.projectId).points == 100
        assert learner.getSkillsSummaryForCurrentUser(fixture.projectId).skillsAchieved == 1
        assert outsider.getSkillsSummaryForCurrentUser(fixture.projectId).points == 0

        when:
        expire(fixture.source, expirationType)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.getSkillsSummaryForCurrentUser(fixture.projectId).points == 0
        learner.getSkillsSummaryForCurrentUser(fixture.projectId).skillsAchieved == 0
        learner.getSkillsSummaryForCurrentUser(fixture.source.projectId).points == 0
        membershipSnapshot(fixture.projectId, users[0]) == originalMembership + [status: BackfillStatus.COMPLETED]
        !participationRepo.findByProjectIdAndUserId(fixture.projectId, users[1])

        when: 'source delivery alone restores credit for the lifetime participant'
        Date reearned = new Date()
        users.each { String userId -> assert skillsService.addSkill(fixture.source, userId, reearned).body.skillApplied }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.getSkillsSummaryForCurrentUser(fixture.projectId).points == 100
        learner.getSkillsSummaryForCurrentUser(fixture.projectId).skillsAchieved == 1
        Instant.parse(learner.getSkillSummary(null, fixture.projectId, fixture.subjectId).skills[0].achievedOn).toEpochMilli() == reearned.time
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], fixture.projectId, fixture.source.skillId).size() == 1
        outsider.getSkillsSummaryForCurrentUser(fixture.projectId).points == 0
        outsider.getSkillsSummaryForCurrentUser(fixture.projectId).skillsAchieved == 0
        membershipSnapshot(fixture.projectId, users[0]) == originalMembership + [status: BackfillStatus.COMPLETED]
        !backfillTasks()

        where:
        expirationType << [ExpirationAttrs.DAILY, ExpirationAttrs.YEARLY]
    }

    def "DAILY renewal retains points and original award dates while delayed delivery converges after expiration and re-awarding"() {
        Map fixture = fixture()
        String userId = getRandomUsers(1)[0]
        SkillsService learner = createService(userId)
        learner.initializeProjectProgress(fixture.projectId)
        skillsService.saveSkillExpirationAttributes(fixture.source.projectId, fixture.source.skillId,
                [expirationType: ExpirationAttrs.DAILY, every: 7])
        Date original = new Date() - 10
        Date renewed = original + 2
        skillsService.addSkill(fixture.source, userId, original)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map before = learner.getSkillSummary(null, fixture.source.projectId, fixture.subjectId).skills[0]
        Map membership = membershipSnapshot(fixture.projectId, userId)
        queueSourceTasks()

        when:
        Map renewal = skillsService.addSkill(fixture.source, userId, renewed).body
        Map after = learner.getSkillSummary(null, fixture.source.projectId, fixture.subjectId).skills[0]

        then:
        renewal.skillApplied
        renewal.explanation == 'Skill Achievement retained'
        before.points == 100
        after.points == 100
        Instant.parse(before.achievedOn).toEpochMilli() == original.time
        Instant.parse(after.achievedOn).toEpochMilli() == original.time
        Instant.parse(after.mostRecentlyPerformedOn).toEpochMilli() == renewed.time
        Instant.parse(after.expirationDate).toEpochMilli() - Instant.parse(before.expirationDate).toEpochMilli() == 2 * 86400000L
        learner.getSkillsSummaryForCurrentUser(fixture.projectId).points == 100
        Instant.parse(learner.getSkillSummary(null, fixture.projectId, fixture.subjectId).skills[0].achievedOn).toEpochMilli() == original.time

        when: 'the renewal and expiration requests stay queued until after a new valid award'
        expire(fixture.source, ExpirationAttrs.DAILY)
        assert learner.getSkillsSummaryForCurrentUser(fixture.source.projectId).points == 0
        Date reearned = new Date()
        assert skillsService.addSkill(fixture.source, userId, reearned).body.skillApplied
        releaseSourceTasks()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.getSkillsSummaryForCurrentUser(fixture.projectId).points == 100
        learner.getSkillsSummaryForCurrentUser(fixture.projectId).skillsAchieved == 1
        Instant.parse(learner.getSkillSummary(null, fixture.projectId, fixture.subjectId).skills[0].achievedOn).toEpochMilli() == reearned.time
        Instant.parse(learner.getSkillSummary(null, fixture.source.projectId, fixture.subjectId).skills[0].mostRecentlyPerformedOn).toEpochMilli() == reearned.time
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, fixture.projectId, fixture.source.skillId).size() == 1
        membershipSnapshot(fixture.projectId, userId) == membership
        !backfillTasks()
    }

    def "distinct valid #ordering source reports converge without imposing a new event-date ordering contract"() {
        Map fixture = fixture(2)
        String userId = getRandomUsers(1)[0]
        SkillsService learner = createService(userId)
        learner.initializeProjectProgress(fixture.projectId)
        Date first = new Date() - 3
        Date second = ordering == 'equal-date' ? first : first - 1
        queueSourceTasks()

        when: 'zero interval permits distinct reports, even at equal or backdated timestamps'
        Map firstReport = skillsService.addSkill(fixture.source, userId, first).body
        Map secondReport = skillsService.addSkill(fixture.source, userId, second).body
        releaseSourceTasks()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        firstReport.skillApplied
        secondReport.skillApplied
        learner.getSkillsSummaryForCurrentUser(fixture.source.projectId).points == 200
        learner.getSkillsSummaryForCurrentUser(fixture.projectId).points == 200
        learner.getSkillsSummaryForCurrentUser(fixture.projectId).skillsAchieved == 1
        // Provided dates use the latest retained performed date, not arrival/completion order.
        Instant.parse(learner.getSkillSummary(null, fixture.source.projectId, fixture.subjectId).skills[0].achievedOn).toEpochMilli() == first.time
        Instant.parse(learner.getSkillSummary(null, fixture.projectId, fixture.subjectId).skills[0].achievedOn).toEpochMilli() == first.time
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(userId, fixture.projectId, fixture.source.skillId).size() == 1
        participationRepo.readStatus(fixture.projectId, userId) == BackfillStatus.COMPLETED
        !backfillTasks()

        where:
        ordering << ['equal-date', 'backdated']
    }

    private Map fixture(int occurrences = 1) {
        Map source = createSkill(1, 1, 11, 0, occurrences, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [source])
        skillsService.exportSkillToCatalog(source.projectId, source.skillId)
        skillsService.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [])
        skillsService.bulkImportSkillsFromCatalog('TestProject2', 'TestSubject1', [source])
        skillsService.finalizeSkillsImportFromCatalog('TestProject2')
        [source: source, projectId: 'TestProject2', subjectId: 'TestSubject1']
    }

    private Map membershipSnapshot(String projectId, String userId) {
        ProjectParticipation participant = participationRepo.findByProjectIdAndUserId(projectId, userId)
        [id: participant.id, created: participant.created, updated: participant.updated, status: participant.backfillStatus]
    }

    private void expire(Map source, String type) {
        LocalDateTime yesterday = (new Date() - 1).toLocalDateTime()
        Map attributes = type == ExpirationAttrs.DAILY ? [expirationType: type, every: 7] :
                [expirationType: type, every: 1, monthlyDay: yesterday.dayOfMonth, nextExpirationDate: yesterday.toDate()]
        skillsService.saveSkillExpirationAttributes(source.projectId, source.skillId, attributes)
        expirationExecutor.removeExpiredUserAchievements()
        settingsService.deleteGlobalSetting(ExpireUserAchievementsTaskExecutor.SKILL_EXPIRATION_LAST_RUN_DATE)
    }

    private void queueSourceTasks() {
        installTrigger(QUEUE, 'scheduled_tasks', "if NEW.task_name = 'imported-skill-achievement' then NEW.execution_time := now() + interval '1 hour'; end if; return NEW;")
    }

    private void releaseSourceTasks() {
        removeTrigger(QUEUE, 'scheduled_tasks')
        jdbcTemplate.update("update scheduled_tasks set execution_time = now() where task_name = 'imported-skill-achievement'")
    }
}
