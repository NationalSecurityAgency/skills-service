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
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.CatalogParticipantBackfillService
import skills.services.LockingService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import spock.util.concurrent.PollingConditions
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static skills.intTests.utils.SkillsFactory.*

class CatalogBackfillRetryAndIdempotencySpecs extends CatalogBackfillSpecBase {

    @Autowired CatalogParticipantBackfillService backfillService

    @Autowired PlatformTransactionManager transactionManager

    @Autowired LockingService lockingService

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

}
