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
package skills.intTests.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.LockingService
import skills.storage.model.SkillDef.SelfReportingType
import skills.storage.model.auth.RoleName
import skills.storage.repos.UserAchievedLevelRepo
import spock.util.concurrent.PollingConditions

import javax.sql.DataSource
import java.sql.Connection
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

import static skills.intTests.utils.SkillsFactory.*

class ProjectUserRemovalQuizSpecs extends QuizSkillAchievementsBaseIntSpec {

    @Autowired DataSource dataSource
    @Autowired UserAchievedLevelRepo userAchievedLevelRepo
    @Autowired PlatformTransactionManager transactionManager
    @Autowired LockingService lockingService

    def "removal waits for in-flight quiz #operation and clears shared credit without restoring participation"() {
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs(manualGrade ? [QuizDefFactory.createTextInputQuestion(1, 1)] :
                QuizDefFactory.createChoiceQuestions(1, 1, 2))
        Map source = createSkill(1, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map shared = createSkill(2, 1, 2, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map unrelated = createSkill(2, 1, 3, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [source])
        skillsService.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [shared, unrelated])
        SkillsService learner = createService(getRandomUsers(1)[0])
        learner.initializeProjectProgress('TestProject1')
        skillsService.addSkill(unrelated, learner.userName)
        Map attempt = learner.startQuizAttempt(quiz.quizId).body
        Integer answerId = attempt.questions[0].answerOptions[0].id
        if (manualGrade) {
            learner.reportQuizAnswer(quiz.quizId, attempt.id, answerId, [isSelected: true, answerText: 'Pending answer'])
            assert learner.completeQuizAttempt(quiz.quizId, attempt.id).body.needsGrading
        } else {
            learner.reportQuizAnswer(quiz.quizId, attempt.id, answerId)
        }
        long barrier = 4072402L
        Connection connection = dataSource.connection
        ExecutorService callers = Executors.newFixedThreadPool(2)
        connection.createStatement().execute("select pg_advisory_lock(${barrier})")
        String table = manualGrade ? 'user_quiz_answer_graded' : 'user_quiz_attempt'
        String condition = manualGrade ? 'true' : "NEW.id = ${attempt.id} and NEW.status = 'PASSED'"
        // Pause grading after the insert locks the answer but before credit reporting.
        // Completion instead pauses at commit after awarding linked credit.
        jdbcTemplate.execute("""create function removal_quiz_commit_pause() returns trigger language plpgsql as \$\$
            begin if ${condition} then
                perform pg_advisory_xact_lock(${barrier}); end if; return NEW; end; \$\$""")
        String trigger = manualGrade ? 'trigger removal_quiz_commit_pause after insert on user_quiz_answer_graded' :
                'constraint trigger removal_quiz_commit_pause after update on user_quiz_attempt deferrable initially deferred'
        jdbcTemplate.execute("create ${trigger} for each row execute function removal_quiz_commit_pause()")

        when:
        Future<Boolean> completion = callers.submit({
            manualGrade ? skillsService.gradeAnswer(learner.userName, quiz.quizId, attempt.id, answerId, true, 'Accepted').body.doneGradingAttempt :
                    learner.completeQuizAttempt(quiz.quizId, attempt.id).body.passed
        } as Callable<Boolean>)
        new PollingConditions(timeout: 15).eventually {
            assert jdbcTemplate.queryForObject("select count(*) from pg_locks where locktype = 'advisory' and objid = ? and not granted", Integer, barrier) > 0
        }
        Future<Map> removal = callers.submit({ skillsService.removeUserFromProject('TestProject1', learner.userName) } as Callable<Map>)
        new PollingConditions(timeout: 15).eventually {
            assert jdbcTemplate.queryForObject('''select count(*) from pg_stat_activity remover
                where remover.datname = current_database() and remover.wait_event_type = 'Lock'
                and exists(select 1 from pg_locks completer where completer.locktype = 'advisory'
                    and completer.objid = ? and not completer.granted
                    and completer.pid = any(pg_blocking_pids(remover.pid)))''', Integer, barrier) > 0
        }

        then:
        !completion.isDone()
        !removal.isDone()

        when:
        connection.createStatement().execute('select pg_advisory_unlock_all()')
        boolean result = completion.get(20, TimeUnit.SECONDS)
        Map removed = removal.get(20, TimeUnit.SECONDS)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        result
        removed.success
        skillsService.getProjectUsers('TestProject1').count == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').skillsAchieved == 0
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 100
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 1
        skillsService.getPerformedSkills(learner.userName, 'TestProject2').data*.skillId == [unrelated.skillId]
        skillsService.getQuizRuns(quiz.quizId).totalCount == 0

        cleanup:
        connection?.createStatement()?.execute('select pg_advisory_unlock_all()')
        connection?.close()
        callers?.shutdown()
        assert callers == null || callers.awaitTermination(30, TimeUnit.SECONDS)
        if (table) {
            jdbcTemplate.execute("drop trigger if exists removal_quiz_commit_pause on ${table}")
            jdbcTemplate.execute('drop function if exists removal_quiz_commit_pause()')
        }

        where:
        operation    | manualGrade
        'completion' | false
        'grading'    | true
    }

    def "removal deletes a passed attempt without waiting on its grading lock"() {
        Map quiz = createQuiz(1)
        Map skill = createSkill(1, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [skill])
        SkillsService learner = createService(getRandomUsers(1)[0])
        Integer attemptId = passQuiz(learner, quiz)
        assert learner.getSkillsSummaryForCurrentUser('TestProject1').points == 100
        ExecutorService caller = Executors.newSingleThreadExecutor()

        when:
        Map removed = new TransactionTemplate(transactionManager).execute {
            // Hold the grading coordination lock, not the row that removal must delete.
            lockingService.lockUserQuizAttempt(attemptId)
            Future<Map> removal = caller.submit({ skillsService.removeUserFromProject('TestProject1', learner.userName) } as Callable<Map>)
            return removal.get(15, TimeUnit.SECONDS)
        }

        then:
        removed.success
        skillsService.getQuizRuns(quiz.quizId).totalCount == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0

        cleanup:
        caller?.shutdown()
        assert caller == null || caller.awaitTermination(30, TimeUnit.SECONDS)
    }

    def "a stale manual grade after removal cannot restore a pending attempt or project credit"() {
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs([QuizDefFactory.createTextInputQuestion(1, 1)])
        Map skill = createSkill(1, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [skill])
        SkillsService learner = createService(getRandomUsers(1)[0])
        learner.initializeProjectProgress('TestProject1')
        Map attempt = learner.startQuizAttempt(quiz.quizId).body
        Integer answerId = attempt.questions[0].answerOptions[0].id
        learner.reportQuizAnswer(quiz.quizId, attempt.id, answerId, [isSelected: true, answerText: 'Pending answer'])
        assert learner.completeQuizAttempt(quiz.quizId, attempt.id).body.needsGrading
        assert skillsService.getQuizRuns(quiz.quizId).data*.attemptId == [attempt.id]
        skillsService.removeUserFromProject('TestProject1', learner.userName)
        assert skillsService.getProjectUsers('TestProject1').count == 0

        when:
        // A deleted attempt may reject the stale request; a no-op is also safe.
        try {
            skillsService.gradeAnswer(learner.userName, quiz.quizId, attempt.id, answerId, true, 'Accepted')
        } catch (SkillsClientException error) {
            assert error.httpStatus == HttpStatus.BAD_REQUEST
            assert error.message.contains('does not exist')
        }

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        skillsService.getQuizRuns(quiz.quizId).totalCount == 0
        skillsService.getProjectUsers('TestProject1').count == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').skillsAchieved == 0
    }

    def "#operation for an absent project user preserves shared quiz attempts and other project progress - archived #archived"() {
        List<String> users = getRandomUsers(4)
        SkillsService projectAdmin = createService(users[0])
        SkillsService otherProjectAdmin = createService(users[1])
        SkillsService quizAdmin = createService(users[2])
        SkillsService learner = createService(users[3])
        Map quiz = QuizDefFactory.createQuiz(1)
        quizAdmin.createQuizDef(quiz)
        quizAdmin.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(1, 1, 2))
        Map disabledSkill = createSkill(1, 1, 1, 0, 1, 0, 100) +
                [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId, enabled: 'false']
        Map sharedSkill = createSkill(2, 1, 2, 0, 1, 0, 100) +
                [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map unrelated = createSkill(2, 1, 3, 0, 1, 0, 100)
        projectAdmin.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [disabledSkill])
        otherProjectAdmin.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [sharedSkill, unrelated])
        Integer passedAttempt = passQuiz(learner, quiz)
        otherProjectAdmin.addSkill(unrelated, learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        if (archived) {
            projectAdmin.archiveUsers([learner.userName], 'TestProject1')
        }
        Long actionsBefore = projectAdmin.getUserActionsForProject('TestProject1').totalCount

        expect:
        otherProjectAdmin.getUserRolesForProjectAndUser('TestProject2', projectAdmin.userName).isEmpty()
        quizAdmin.getQuizUserRoles(quiz.quizId)*.userId == [quizAdmin.userName]
        projectAdmin.getProjectUsers('TestProject1').count == 0
        projectAdmin.getArchivedUsers('TestProject1').data*.userId == (archived ? [learner.userName] : [])
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 200
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 2
        quizAdmin.getQuizRuns(quiz.quizId).data*.attemptId == [passedAttempt]

        when:
        // Only the first removal of archived membership should add an audit action.
        List<Map> results = (1..2).collect { int request ->
            if (removeFromProject) {
                return projectAdmin.removeUserFromProject('TestProject1', learner.userName)
            }
            return projectAdmin.deleteAllSkillEvents([projectId: 'TestProject1', userId: learner.userName])
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        results.every { Map result -> result.success }
        projectAdmin.getProjectUsers('TestProject1').count == 0
        projectAdmin.getUserActionsForProject('TestProject1').totalCount == actionsBefore + (archived && removeFromProject ? 1 : 0)
        projectAdmin.getArchivedUsers('TestProject1').data*.userId == (archived && !removeFromProject ? [learner.userName] : [])
        quizAdmin.getQuizRuns(quiz.quizId).data*.attemptId == [passedAttempt]
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 200
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 2
        otherProjectAdmin.getPerformedSkills(learner.userName, 'TestProject2').data*.skillId.sort() ==
                [sharedSkill.skillId, unrelated.skillId].sort()
        otherProjectAdmin.getProjectUsers('TestProject2').data*.userId == [learner.userName]

        where:
        operation     | removeFromProject | archived
        'Remove User' | true              | false
        'Delete All'  | false             | false
        'Remove User' | true              | true
        'Delete All'  | false             | true
    }

    def "removal clears passed shared quiz attempts and dependent progress but retains failed attempts and unrelated progress"() {
        Map quiz = createQuiz(1)
        Map sourceSkill = createSkill(1, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map sharedSkill = createSkill(2, 1, 2, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map unrelated = createSkill(2, 1, 3, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [sourceSkill])
        skillsService.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [sharedSkill, unrelated])
        SkillsService learner = createService(getRandomUsers(1)[0])
        Integer failedAttempt = failQuiz(learner, quiz)
        passQuiz(learner, quiz)
        skillsService.addSkill(unrelated, learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        expect:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 100
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 200

        when:
        skillsService.removeUserFromProject('TestProject1', learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').skillsAchieved == 0
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 100
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 1
        skillsService.getPerformedSkills(learner.userName, 'TestProject2').data*.skillId == [unrelated.skillId]
        skillsService.getQuizRuns(quiz.quizId).data*.attemptId == [failedAttempt]
        skillsService.getProjectUsers('TestProject1').count == 0
    }

    def "removal reconciles a shared quiz skill group when quiz passed=#passedQuiz and group requires #required skills"() {
        Map quiz = createQuiz(1)
        Map source = createSkill(1, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map local = createSkill(1, 1, 2, 0, 1, 0, 100)
        Map shared = createSkill(2, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map other = createSkill(2, 1, 2, 0, 1, 0, 100)
        Map group = createSkillsGroup(2, 1, 3)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [source, local])
        skillsService.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [group])
        [shared, other].each { Map skill -> skillsService.assignSkillToSkillsGroup(group.skillId, skill) }
        group.numSkillsRequired = required
        skillsService.updateSkill(group, null)
        SkillsService learner = createService(getRandomUsers(1)[0])
        skillsService.addSkill(local, learner.userName)
        if (passedQuiz) {
            passQuiz(learner, quiz)
        }
        skillsService.addSkill(other, learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        expect:
        userAchievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(learner.userName, group.projectId, group.skillId).size() == 1

        when:
        skillsService.removeUserFromProject(source.projectId, learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.getSkillsSummaryForCurrentUser(source.projectId).points == 0
        learner.getSkillsSummaryForCurrentUser(group.projectId).skillsAchieved == 1
        userAchievedLevelRepo.findAllByUserIdAndProjectIdAndSkillId(learner.userName, group.projectId, group.skillId).size() == remainingAchievements

        where:
        passedQuiz | required | remainingAchievements
        false      | 1        | 1
        true       | 1        | 1
        true       | -1       | 0
    }

    def "project quiz association authorizes rolling removal without administering the shared quiz or other project"() {
        List<String> users = getRandomUsers(4)
        SkillsService projectAdmin = createService(users[0])
        SkillsService otherProjectAdmin = createService(users[1])
        SkillsService quizAdmin = createService(users[2])
        SkillsService learner = createService(users[3])
        Map sharedQuiz = QuizDefFactory.createQuiz(1)
        Map unrelatedQuiz = QuizDefFactory.createQuiz(2)
        quizAdmin.createQuizDef(sharedQuiz)
        quizAdmin.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(1, 1, 2))
        quizAdmin.createQuizDef(unrelatedQuiz)
        quizAdmin.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, 1, 2))
        Map sourceSkill = createSkill(1, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: sharedQuiz.quizId]
        Map sharedSkill = createSkill(2, 1, 2, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: sharedQuiz.quizId]
        Map unrelatedSkill = createSkill(2, 1, 3, 0, 1, 0, 100)
        Map unrelatedQuizSkill = createSkill(2, 1, 4, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: unrelatedQuiz.quizId]
        projectAdmin.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [sourceSkill])
        otherProjectAdmin.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [sharedSkill, unrelatedSkill, unrelatedQuizSkill])
        Integer sharedAttempt = passQuiz(learner, sharedQuiz)
        Integer unrelatedAttempt = passQuiz(learner, unrelatedQuiz)
        otherProjectAdmin.addSkill(unrelatedSkill, learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        expect:
        !projectAdmin.isRoot()
        projectAdmin.getUserRolesForProjectAndUser('TestProject1', projectAdmin.userName)*.roleName == [RoleName.ROLE_PROJECT_ADMIN.toString()]
        otherProjectAdmin.getUserRolesForProjectAndUser('TestProject2', projectAdmin.userName).isEmpty()
        quizAdmin.getQuizUserRoles(sharedQuiz.quizId)*.userId == [quizAdmin.userName]
        quizAdmin.getQuizRuns(sharedQuiz.quizId).data*.attemptId == [sharedAttempt]
        quizAdmin.getQuizRuns(unrelatedQuiz.quizId).data*.attemptId == [unrelatedAttempt]
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 100
        learner.getSkillsSummaryForCurrentUser('TestProject1').skillsAchieved == 1
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 300
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 3

        when:
        Map result = projectAdmin.removeUserFromProject('TestProject1', learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        result.success
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').skillsAchieved == 0
        projectAdmin.getProjectUsers('TestProject1').count == 0
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 200
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 2
        otherProjectAdmin.getPerformedSkills(learner.userName, 'TestProject2').data*.skillId.sort() == [unrelatedSkill.skillId, unrelatedQuizSkill.skillId].sort()
        otherProjectAdmin.getProjectUsers('TestProject2').data*.userId == [learner.userName]
        quizAdmin.getQuizRuns(sharedQuiz.quizId).totalCount == 0
        quizAdmin.getQuizRuns(unrelatedQuiz.quizId).data*.attemptId == [unrelatedAttempt]
    }
}
