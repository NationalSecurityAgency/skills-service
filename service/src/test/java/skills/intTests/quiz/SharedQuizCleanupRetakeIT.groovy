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

import com.github.kagkarlsson.scheduler.task.TaskInstance
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.storage.model.SkillDef.SelfReportingType
import skills.tasks.data.RemoveSkillEventsForUserRequest

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static skills.intTests.utils.SkillsFactory.*

@Import(CleanupConfiguration)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SharedQuizCleanupRetakeIT extends QuizSkillAchievementsBaseIntSpec {

    @Autowired CleanupGate cleanupGate

    def cleanup() {
        cleanupGate.release.countDown()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        cleanupGate.userId = null
    }

    def "delayed shared quiz cleanup preserves a later pass after #operation"() {
        Map quiz = createQuiz(1)
        Map otherQuiz = createQuiz(2)
        skillsService.saveQuizSettings(quiz.quizId, [[setting: QuizSettings.MultipleTakes.setting, value: false]])
        Map sourceSkill = createSkill(1, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map otherSourceSkill = createSkill(1, 1, 2, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: otherQuiz.quizId]
        Map sharedSkill = createSkill(2, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map otherSharedSkill = createSkill(2, 1, 2, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: otherQuiz.quizId]
        Map unrelated = createSkill(2, 1, 3, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [sourceSkill, otherSourceSkill])
        skillsService.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [sharedSkill, otherSharedSkill, unrelated])
        Map badge = createBadge(2, 1)
        skillsService.createBadge(badge)
        [sharedSkill, unrelated].each { Map skill ->
            skillsService.assignSkillToBadge([projectId: badge.projectId, badgeId: badge.badgeId, skillId: skill.skillId])
        }
        badge.enabled = true
        skillsService.createBadge(badge)
        SkillsService learner = createService(getRandomUsers(1)[0])
        passQuiz(learner, quiz)
        passQuiz(learner, otherQuiz)
        skillsService.addSkill(unrelated, learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        cleanupGate.arm(learner.userName)

        expect:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 200
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 300
        learner.getBadgeSummary(null, 'TestProject2', badge.badgeId).badgeAchieved

        when:
        if (removeFromProject) {
            skillsService.removeUserFromProject('TestProject1', learner.userName)
        } else {
            skillsService.deleteAllSkillEvents([projectId: 'TestProject1', userId: learner.userName])
        }
        assert cleanupGate.arrived.await(30, TimeUnit.SECONDS): 'Shared quiz cleanup did not reach the test gate'

        then:
        skillsService.getQuizRuns(quiz.quizId).totalCount == 0
        skillsService.getQuizRuns(otherQuiz.quizId).totalCount == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        // Cleanup is paused before the reporting lock, leaving the old destination credit in place.
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 300

        when:
        Integer newPassedAttempt = passQuiz(learner, quiz)
        Integer newFailedAttempt = failQuiz(learner, otherQuiz)

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 100
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 300

        when:
        cleanupGate.release.countDown()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 100
        learner.getSkillsSummaryForCurrentUser('TestProject1').skillsAchieved == 1
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 200
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 2
        learner.getBadgeSummary(null, 'TestProject2', badge.badgeId).badgeAchieved
        skillsService.getPerformedSkills(learner.userName, 'TestProject2').data*.skillId.sort() == [sharedSkill.skillId, unrelated.skillId].sort()
        skillsService.getQuizRuns(quiz.quizId).data*.attemptId == [newPassedAttempt]
        skillsService.getQuizRuns(otherQuiz.quizId).data*.attemptId == [newFailedAttempt]

        when:
        learner.startQuizAttempt(quiz.quizId)

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('already took and passed this quiz')

        where:
        operation                  | removeFromProject
        'removal from the project' | true
        'deleting all progress'    | false
    }

    @TestConfiguration
    static class CleanupConfiguration {
        @Bean
        CleanupGate sharedQuizCleanupGate() { new CleanupGate() }
    }

    @Aspect
    @Order(0)
    static class CleanupGate {
        volatile String userId
        volatile CountDownLatch arrived = new CountDownLatch(1)
        volatile CountDownLatch release = new CountDownLatch(1)

        void arm(String learnerId) {
            arrived = new CountDownLatch(1)
            release = new CountDownLatch(1)
            userId = learnerId
        }

        // Outside transaction advice so the retake can acquire the destination reporting lock.
        @Around('execution(* skills.tasks.executors.RemoveSkillEventsForAUserExecutor.execute(..))')
        Object pauseCleanup(ProceedingJoinPoint joinPoint) {
            TaskInstance<RemoveSkillEventsForUserRequest> task = (TaskInstance<RemoveSkillEventsForUserRequest>) joinPoint.args[0]
            if (task.data.userId == userId && task.data.projectId == 'TestProject2') {
                arrived.countDown()
                assert release.await(60, TimeUnit.SECONDS): 'Shared quiz cleanup gate was not released'
            }
            return joinPoint.proceed()
        }
    }
}
