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
import org.springframework.http.HttpStatus
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.admin.SkillCatalogFinalizationService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.SkillDef.SelfReportingType
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.ArchivedUsersRepo
import skills.storage.repos.UserRepo
import skills.storage.repos.UserRoleRepo
import spock.util.concurrent.PollingConditions

import javax.sql.DataSource
import java.sql.Connection
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

import static skills.intTests.utils.SkillsFactory.*

class ProjectUserRemovalSpecs extends CatalogBackfillSpecBase {

    @Autowired ArchivedUsersRepo archivedUsersRepo
    @Autowired DataSource dataSource
    @Autowired UserRoleRepo userRoleRepo
    @Autowired UserRepo userRepo

    def "remove a native project's #entry participant without deleting their account"() {
        Map skill = createSkill(1, 1, 1, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [skill])
        SkillsService learner = createService(users[0])
        if (entry != 'local-only') {
            learner.initializeProjectProgress('TestProject1')
        }
        if (entry != 'visit-only') {
            skillsService.addSkill(skill, users[0])
        }
        Integer accountId = userAttrsRepo.findByUserIdIgnoreCase(users[0]).id

        expect:
        skillsService.getProjectUsers('TestProject1').count == 1

        when:
        skillsService.removeUserFromProject('testproject1', users[0].toUpperCase(Locale.ROOT))

        then:
        skillsService.getProjectUsers('TestProject1').count == 0
        skillsService.getProjectUsersCount('TestProject1') == 0
        skillsService.getPerformedSkills(users[0], 'TestProject1').totalCount == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        !participationRepo.existsByProjectIdAndUserId('TestProject1', users[0])
        userAttrsRepo.findByUserIdIgnoreCase(users[0]).id == accountId

        when:
        skillsService.removeUserFromProject('TestProject1', users[0])

        then:
        skillsService.getProjectUsers('TestProject1').count == 0

        where:
        entry << ['visit-only', 'local-only', 'visit-and-local']
    }

    def "removal clears imported and reused progress and dependent awards without changing other users or source credit - reuse #reuse"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        SkillsService peer = createService(users[1])
        earnSource(fixture, users[0])
        earnSource(fixture, users[1])
        qualify(fixture, learner, 'local')
        qualify(fixture, peer, 'visit')
        if (reuse) {
            skillsService.createSubject(createSubject(2, 2))
            skillsService.reuseSkillInAnotherSubject('TestProject2', fixture.sourceSkills[0].skillId, 'TestSubject2')
        }
        Map unrelated = createSkill(3, 1, 31, 0, 1, 0, 100)
        skillsService.createSkill(unrelated)
        skillsService.addSkill(unrelated, users[0])
        Map globalBadge = [badgeId: 'removalGlobalBadge', name: 'Removal Global Badge']
        skillsService.createGlobalBadge(globalBadge)
        skillsService.assignProjectLevelToGlobalBadge(projectId: 'TestProject2', badgeId: globalBadge.badgeId, level: '1')
        skillsService.updateGlobalBadge(globalBadge + [enabled: 'true'])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        List<Map> sourceBefore = creditSnapshot('TestProject1', users[0])
        List<Map> unrelatedBefore = creditSnapshot('TestProject3', users[0])
        List<Map> peerBefore = creditSnapshot('TestProject2', users[1])

        expect:
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == (reuse ? 700 : 500)
        learner.getSkillsSummaryForCurrentUser('TestProject2').badges.numBadgesCompleted == 2
        userAchievedRepo.existsByUserIdAndSkillIdAndProjectIdIsNull(users[0], globalBadge.badgeId)

        when:
        skillsService.removeUserFromProject('TestProject2', users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')

        then:
        summary.points == 0
        summary.skillsLevel == 0
        summary.skillsAchieved == 0
        summary.badges.numBadgesCompleted == 0
        !creditSnapshot('TestProject2', users[0])
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[0])
        !userAchievedRepo.existsByUserIdAndSkillIdAndProjectIdIsNull(users[0], globalBadge.badgeId)
        skillsService.getProjectUsers('TestProject2').data*.userId == [users[1]]
        skillsService.getProjectUsersCount('TestProject2') == 1
        peer.getRank(null, 'TestProject2').numUsers == 1
        creditSnapshot('TestProject1', users[0]) == sourceBefore
        creditSnapshot('TestProject3', users[0]) == unrelatedBefore
        creditSnapshot('TestProject2', users[1]) == peerBefore

        where:
        reuse << [false, true]
    }

    def "pending participant backfill and later source events do not restore a removed user"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        queueBackfill()
        learner.initializeProjectProgress('TestProject2')

        expect:
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.PENDING
        backfillTasks()

        when:
        skillsService.removeUserFromProject('TestProject2', users[0])
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 0
        skillsService.getProjectUsers('TestProject2').count == 0
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[0])

        when:
        skillsService.deleteAllSkillEvents([projectId: 'TestProject1', userId: users[0]])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        earnSource(fixture, users[0])

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 0
        skillsService.getProjectUsers('TestProject2').count == 0
        !creditSnapshot('TestProject2', users[0])
    }

    def "a removed imported-only user can participate again by #entry without duplicate credit"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        qualify(fixture, learner, 'visit')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        skillsService.removeUserFromProject('TestProject2', users[0])

        expect:
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 0
        skillsService.getProjectUsers('TestProject2').count == 0

        when:
        qualify(fixture, learner, entry)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        learner.initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')

        then:
        summary.points == (entry == 'visit' ? 400 : 500)
        summary.skillsAchieved == 2
        summary.badges.numBadgesCompleted == 1
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.COMPLETED
        skillsService.getProjectUsers('TestProject2').count == 1

        where:
        entry << ['visit', 'local']
    }

    def "queued incremental import delivery cannot recreate removed progress - reuse #reuse"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        qualify(fixture, learner, 'local')
        if (reuse) {
            skillsService.createSubject(createSubject(2, 2))
            skillsService.reuseSkillInAnotherSubject('TestProject2', fixture.sourceSkills[0].skillId, 'TestSubject2')
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        installTrigger('catalog_test_queue', 'scheduled_tasks', "if NEW.task_name = 'imported-skill-achievement' then NEW.execution_time := now() + interval '1 hour'; end if; return NEW;")
        fixture.sourceSkills.each { Map skill -> skillsService.addSkill(skill, users[0], firstDate) }

        expect:
        jdbcTemplate.queryForObject("select count(*) from scheduled_tasks where task_name = 'imported-skill-achievement'", Integer) > 0

        when:
        skillsService.removeUserFromProject('TestProject2', users[0])
        removeTrigger('catalog_test_queue', 'scheduled_tasks')
        jdbcTemplate.update("update scheduled_tasks set execution_time = now() where task_name = 'imported-skill-achievement'")
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 0
        skillsService.getProjectUsers('TestProject2').count == 0
        !creditSnapshot('TestProject2', users[0])
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 200

        cleanup:
        removeTrigger('catalog_test_queue', 'scheduled_tasks')
        jdbcTemplate.update("update scheduled_tasks set execution_time = now() where task_name = 'imported-skill-achievement'")

        where:
        reuse << [false, true]
    }

    def "#operation is a no-op for approval-only records without participation or earned progress"() {
        Map skill = createSkill(1, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Approval]
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [skill])
        SkillsService learner = createService(users[0])
        learner.addSkill(skill)
        Long actionsBefore = skillsService.getUserActionsForProject('TestProject1').totalCount

        expect:
        !participationRepo.existsByProjectIdAndUserId('TestProject1', users[0])
        !userPointsRepo.existsByProjectIdAndUserId('TestProject1', users[0])
        skillsService.getProjectUsers('TestProject1').count == 0
        skillsService.getPerformedSkills(users[0], 'TestProject1').totalCount == 0
        skillsService.getApprovals('TestProject1', 10, 1, 'requestedOn', false).count == 1

        when:
        if (removeFromProject) {
            skillsService.removeUserFromProject('TestProject1', users[0])
        } else {
            skillsService.deleteAllSkillEvents([projectId: 'TestProject1', userId: users[0]])
        }

        then:
        skillsService.getApprovals('TestProject1', 10, 1, 'requestedOn', false).count == 1
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        skillsService.getProjectUsers('TestProject1').count == 0
        skillsService.getUserActionsForProject('TestProject1').totalCount == actionsBefore

        where:
        operation     | removeFromProject
        'Remove User' | true
        'Delete All'  | false
    }

    def "removal clears pending approved and rejected self-report records"() {
        List<Map> skills = (1..3).collect { int id -> createSkill(1, 1, id, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Approval] }
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), skills)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress('TestProject1')
        skills.each { Map skill -> learner.addSkill(skill) }
        Map requests = skillsService.getApprovals('TestProject1', 10, 1, 'requestedOn', false)
        skillsService.approve('TestProject1', [requests.data.find { it.skillId == skills[0].skillId }.id])
        skillsService.rejectSkillApprovals('TestProject1', [requests.data.find { it.skillId == skills[1].skillId }.id], 'Not completed')

        expect:
        skillsService.getApprovals('TestProject1', 10, 1, 'requestedOn', false).count == 1
        skillsService.getApprovalsHistory('TestProject1', 10, 1, 'requestedOn', false).count == 2

        when:
        skillsService.removeUserFromProject('TestProject1', users[0])

        then:
        skillsService.getApprovals('TestProject1', 10, 1, 'requestedOn', false).count == 0
        skillsService.getApprovalsHistory('TestProject1', 10, 1, 'requestedOn', false).count == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        skillsService.getProjectUsers('TestProject1').count == 0
    }

    def "removing source project progress propagates credit removal to importing projects"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        qualify(fixture, learner, 'visit')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        expect:
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 400

        when:
        skillsService.removeUserFromProject('TestProject1', users[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 0
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 0
        participationRepo.existsByProjectIdAndUserId('TestProject2', users[0])
        skillsService.getProjectUsers('TestProject1').count == 0
    }

    def "removal rejects #state finalization before changing participation progress archive or audit history"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        qualify(fixture, learner, 'local')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        skillsService.archiveUsers([users[0]], 'TestProject2')
        skillsService.changeSetting('TestProject2', SkillCatalogFinalizationService.PROJ_FINALIZE_STATE_PROP,
                [projectId: 'TestProject2', setting: SkillCatalogFinalizationService.PROJ_FINALIZE_STATE_PROP, value: state])
        List<Map> before = creditSnapshot('TestProject2', users[0])
        Map actionsBefore = skillsService.getUserActionsForProject('TestProject2')

        when:
        skillsService.removeUserFromProject('TestProject2', users[0])

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains('errorCode:BadParam')
        error.message.contains(message)
        creditSnapshot('TestProject2', users[0]) == before
        participationRepo.existsByProjectIdAndUserId('TestProject2', users[0])
        archivedUsersRepo.existsByProjectIdAndUserId('TestProject2', users[0])
        skillsService.getUserActionsForProject('TestProject2').totalCount == actionsBefore.totalCount

        where:
        state     | message
        'RUNNING' | 'Please try again after finalization completes.'
        'FAILED'  | 'Please resolve finalization before trying again.'
    }

    def "removal clears archived membership and records a distinct project user audit action"() {
        skillsService.createProject(createProject(1))
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress('TestProject1')
        skillsService.archiveUsers([users[0]], 'TestProject1')

        when:
        skillsService.removeUserFromProject('TestProject1', users[0])
        Map history = skillsService.getUserActionsForProject('TestProject1', 10, 1, 'created', false,
                DashboardItem.ProjectUser, '', '', DashboardAction.Delete)

        then:
        !archivedUsersRepo.existsByProjectIdAndUserId('TestProject1', users[0])
        !participationRepo.existsByProjectIdAndUserId('TestProject1', users[0])
        history.totalCount == 1
        history.data[0].item == 'ProjectUser'
        history.data[0].itemId == userAttrsRepo.findByUserIdIgnoreCase(users[0]).userIdForDisplay
        skillsService.getProjectUserActionAttributes('TestProject1', history.data[0].id).removedFromProject == true
    }

    def "finalization cannot start after removal's state check until removal commits"() {
        Map fixture = catalogFixture(false)
        SkillsService learner = createService(users[0])
        SkillsService finalizer = createService(skillsService.userName)
        earnSource(fixture, users[0])
        skillsService.addSkill(fixture.local, users[0])
        importSkills(fixture)
        long barrier = 4072401L
        Connection connection = dataSource.connection
        connection.createStatement().execute("select pg_advisory_lock(${barrier})")
        jdbcTemplate.execute("""create function removal_pause() returns trigger language plpgsql as \$\$
            begin if OLD.project_id = 'TestProject2' and OLD.user_id = '${users[0]}' then
                perform pg_advisory_xact_lock(${barrier}); end if; return OLD; end; \$\$""")
        jdbcTemplate.execute('create trigger removal_pause before delete on user_points for each row execute function removal_pause()')
        ExecutorService callers = Executors.newFixedThreadPool(2)

        when:
        Future<Map> removal = callers.submit({ skillsService.removeUserFromProject('TestProject2', users[0]) } as Callable<Map>)
        new PollingConditions(timeout: 15).eventually {
            assert jdbcTemplate.queryForObject('select count(*) from pg_locks where locktype = \'advisory\' and objid = ? and not granted', Integer, barrier) > 0
        }
        Future<Map> finalization = callers.submit({ finalizer.finalizeSkillsImportFromCatalog('TestProject2', false) } as Callable<Map>)
        new PollingConditions(timeout: 15).eventually {
            assert jdbcTemplate.queryForObject('''select count(*) from pg_stat_activity finalizer
                where finalizer.datname = current_database() and finalizer.wait_event_type = 'Lock'
                  and lower(finalizer.query) like '%f_select_lock_and_insert%'
                  and exists(select 1 from pg_locks removal where removal.locktype = 'advisory'
                    and removal.objid = ? and not removal.granted
                    and removal.pid = any(pg_blocking_pids(finalizer.pid)))''', Integer, barrier) > 0
        }

        then:
        settingRepo.findProjectSettingValue('TestProject2', SkillCatalogFinalizationService.PROJ_FINALIZE_STATE_PROP) != 'RUNNING'

        when:
        connection.createStatement().execute('select pg_advisory_unlock_all()')
        removal.get(20, TimeUnit.SECONDS)
        finalization.get(20, TimeUnit.SECONDS)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 0
        skillsService.getProjectUsers('TestProject2').count == 0
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[0])

        cleanup:
        if (connection) {
            connection.createStatement().execute('select pg_advisory_unlock_all()')
            connection.close()
        }
        callers?.shutdown()
        assert callers == null || callers.awaitTermination(30, TimeUnit.SECONDS)
        removeTrigger('removal_pause', 'user_points')
    }

    def "removing a private project's participant does not revoke their existing access"() {
        skillsService.createProject(createProject(1))
        skillsService.changeSetting('TestProject1', 'invite_only',
                [projectId: 'TestProject1', setting: 'invite_only', value: 'true'])
        SkillsService learner = createService(users[0])
        // Seed accepted invitation access; the admin-role endpoint cannot grant this role.
        userRoleRepo.save(new UserRole(userId: users[0], userRefId: userRepo.findByUserId(users[0]).id,
                projectId: 'TestProject1', roleName: RoleName.ROLE_PRIVATE_PROJECT_USER))
        learner.initializeProjectProgress('TestProject1')

        when:
        skillsService.removeUserFromProject('TestProject1', users[0])

        then:
        skillsService.getProjectUsers('TestProject1').count == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0

        when:
        learner.initializeProjectProgress('TestProject1')

        then:
        skillsService.getProjectUsers('TestProject1').data*.userId == [users[0]]
    }

    def "removal still enforces protected community membership for a project administrator"() {
        SkillsService root = createRootSkillService()
        SkillsService learner = createService(users[0])
        root.saveUserTag(skillsService.userName, 'dragons', ['DivineDragon'])
        root.saveUserTag(users[0], 'dragons', ['DivineDragon'])
        skillsService.createProject(createProject(1) + [enableProtectedUserCommunity: true])
        learner.initializeProjectProgress('TestProject1')
        userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(skillsService.userName, 'dragons'))

        when:
        skillsService.removeUserFromProject('TestProject1', users[0])

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.FORBIDDEN
        participationRepo.existsByProjectIdAndUserId('TestProject1', users[0])
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
    }

    def "a project administrator or root can remove a participant - #role"() {
        skillsService.createProject(createProject(1))
        SkillsService learner = createService(users[0])
        SkillsService caller = createService(users[1])
        learner.initializeProjectProgress('TestProject1')
        if (role == 'admin') {
            skillsService.addUserRole(users[1], 'TestProject1', RoleName.ROLE_PROJECT_ADMIN.toString())
        } else {
            caller = createRootSkillService()
        }

        when:
        caller.removeUserFromProject('TestProject1', users[0])

        then:
        skillsService.getProjectUsers('TestProject1').count == 0

        where:
        role << ['admin', 'root']
    }

    def "unauthorized callers cannot remove a participant - #role"() {
        skillsService.createProject(createProject(1))
        SkillsService learner = createService(users[0])
        SkillsService caller = createService(users[1])
        learner.initializeProjectProgress('TestProject1')
        if (role == 'approver') {
            skillsService.addUserRole(users[1], 'TestProject1', RoleName.ROLE_PROJECT_APPROVER.toString())
        } else if (role == 'other-project-admin') {
            caller.createProject(createProject(2))
        }

        when:
        caller.removeUserFromProject('TestProject1', users[0])

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.FORBIDDEN
        skillsService.getProjectUsers('TestProject1').data*.userId == [users[0]]

        where:
        role << ['approver', 'other-project-admin', 'ordinary-user']
    }

    def "removing an administrator's trainee participation preserves their project role"() {
        skillsService.createProject(createProject(1))
        skillsService.initializeProjectProgress('TestProject1')

        when:
        skillsService.removeUserFromProject('TestProject1', skillsService.userName)

        then:
        skillsService.getProjectUsers('TestProject1').count == 0
        skillsService.getProject('TestProject1').projectId == 'TestProject1'
        skillsService.getUserRolesForProjectAndUser('TestProject1', skillsService.userName)*.roleName == ['ROLE_PROJECT_ADMIN']
    }
}
