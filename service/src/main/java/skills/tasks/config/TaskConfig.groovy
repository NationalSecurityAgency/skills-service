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
package skills.tasks.config

import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer
import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.ExecutionOperations
import com.github.kagkarlsson.scheduler.task.FailureHandler
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import skills.tasks.JsonSerializer
import skills.tasks.data.CatalogFinalizeRequest
import skills.tasks.data.CatalogSkillDefinitionUpdated
import skills.tasks.data.ExpireUserAchievements
import skills.tasks.data.ImportedSkillAchievement
import skills.tasks.data.ProjectInviteCleanup
import skills.tasks.data.RemoveSkillEventsForUserRequest
import skills.tasks.data.TextInputAiGradingRequest
import skills.tasks.data.UnachievableLevelIdentification
import skills.tasks.executors.CatalogSkillUpdatedTaskExecutor
import skills.tasks.executors.ExpireUserAchievementsTaskExecutor
import skills.tasks.executors.FinalizeCatalogSkillsImportExecutor
import skills.tasks.executors.ImportedSkillAchievementTaskExecutor
import skills.tasks.executors.ProjectInviteCleanupTaskExecutor
import skills.tasks.executors.RemoveSkillEventsForAUserExecutor
import skills.tasks.executors.TextInputQuestionAiGradingExecutor
import skills.tasks.executors.UnachievableLevelIdentificationTaskExecutor

import java.time.Duration
import java.time.Instant

@Slf4j
@Configuration
class TaskConfig {

    @Value('#{"${skills.config.taskMaxRetries:6}"}')
    int maxRetries

    @Value('#{"${skills.config.exponentialBackOffSeconds:1}"}')
    int exponentialBackOffSeconds

    @Value('#{"${skills.config.exponentialBackOffRate:2}"}')
    int exponentialBackOffRate

    @Value('#{"${skills.config.inviteCleanupSchedule:DAILY|23:30}"}')
    String projectInviteCleanupSchedule

    @Value('#{"${skills.config.unachievableLevelIdentificationSchedule:DAILY|23:45}"}')
    String unachievableLevelIdentificationSchedule

    @Value('#{"${skills.config.expireUserAchievementsSchedule:DAILY|01:00}"}')
    String expireUserAchievementsSchedule

    @Bean
    DbSchedulerCustomizer customizer() {
        return new DbSchedulerCustomizer() {
            @Override
            Optional<Serializer> serializer() {
                return Optional.of(new JsonSerializer())
            }
        }
    }

    static class DoNotRetryAsyncTaskException extends RuntimeException {
        DoNotRetryAsyncTaskException(String message) {
            super(message)
        }

        DoNotRetryAsyncTaskException(String message, Throwable cause) {
            super(message, cause)
        }

        DoNotRetryAsyncTaskException(Throwable cause) {
            super(cause)
        }
    }

    static class DontRetryOnNoRetryExceptionHandler<T> implements FailureHandler<T> {
        private static final Logger LOG = LoggerFactory.getLogger(MaxRetriesFailureHandler.class);
        private FailureHandler<T> failureHandler;

        DontRetryOnNoRetryExceptionHandler(FailureHandler<T> failureHandler){
            this.failureHandler = failureHandler;
        }

        @Override
        void onFailure(final ExecutionComplete executionComplete, final ExecutionOperations<T> executionOperations) {
            Throwable t = executionComplete.getCause()?.get()
            if(t instanceof DoNotRetryAsyncTaskException){
                LOG.error("Stopping execution because received DoNotRetryAsyncTaskException.", t);
                executionOperations.stop();
            }else{
                this.failureHandler.onFailure(executionComplete, executionOperations);
            }
        }
    }

    @Bean
    OneTimeTask<CatalogSkillDefinitionUpdated> catalogSkillDefinitionUpdatedOneTimeTask(CatalogSkillUpdatedTaskExecutor catalogSkillUpdatedTaskExecutor) {
        return Tasks.oneTime("catalog-skill-updated", CatalogSkillDefinitionUpdated.class)
                .onFailure(
                        new DontRetryOnNoRetryExceptionHandler(new FailureHandler.MaxRetriesFailureHandler(maxRetries,
                                new FailureHandler.ExponentialBackoffFailureHandler(Duration.ofSeconds(exponentialBackOffSeconds), exponentialBackOffRate)))
                )
                .execute(catalogSkillUpdatedTaskExecutor);
    }

    @Bean
    OneTimeTask<ImportedSkillAchievement> importedSkillAchievementOneTimeTask(ImportedSkillAchievementTaskExecutor importedSkillAchievementTaskExecutor) {
        return Tasks.oneTime("imported-skill-achievement", ImportedSkillAchievement.class)
            .onFailure(new DontRetryOnNoRetryExceptionHandler(new FailureHandler.MaxRetriesFailureHandler(maxRetries,
                    new FailureHandler.ExponentialBackoffFailureHandler(Duration.ofSeconds(exponentialBackOffSeconds), exponentialBackOffRate)))
            )
            .execute(importedSkillAchievementTaskExecutor)
    }

    @Bean
    OneTimeTask<CatalogFinalizeRequest> finalizeCatalogImportsOneTimeTask(FinalizeCatalogSkillsImportExecutor finalizeCatalogSkillsImportExecutor) {
        return Tasks.oneTime("finalize-catalog-imports", CatalogFinalizeRequest.class)
                .onFailure(new DontRetryOnNoRetryExceptionHandler(new FailureHandler.MaxRetriesFailureHandler(maxRetries,
                        new FailureHandler.ExponentialBackoffFailureHandler(Duration.ofSeconds(exponentialBackOffSeconds), exponentialBackOffRate)))
                )
                .execute(finalizeCatalogSkillsImportExecutor)
    }
    @Bean
    OneTimeTask<RemoveSkillEventsForUserRequest> removeSkillEventsForAUser(RemoveSkillEventsForAUserExecutor removeSkillEventsForAUserExecutor) {
        return Tasks.oneTime("remove-skill-events-for-user", RemoveSkillEventsForUserRequest.class)
                .onFailure(
                        new DontRetryOnNoRetryExceptionHandler(new FailureHandler.MaxRetriesFailureHandler(maxRetries,
                                new FailureHandler.ExponentialBackoffFailureHandler(Duration.ofSeconds(exponentialBackOffSeconds), exponentialBackOffRate)))
                )
                .execute(removeSkillEventsForAUserExecutor);
    }

    @Bean
    OneTimeTask<TextInputAiGradingRequest> gradeTextInputUsingAi(TextInputQuestionAiGradingExecutor textInputQuestionAiGradingExecutor) {
        return Tasks.oneTime("grade-text-input-using-ai", TextInputAiGradingRequest.class)
                .onFailure(
                        new DontRetryOnNoRetryExceptionHandler(new FailureHandler.MaxRetriesFailureHandler(maxRetries,
                                new FailureHandler.ExponentialBackoffFailureHandler(Duration.ofSeconds(exponentialBackOffSeconds), exponentialBackOffRate)))
                )
                .execute(textInputQuestionAiGradingExecutor);
    }
    @Bean
    RecurringTask<ProjectInviteCleanup> cleanupProjectInvitesTask(ProjectInviteCleanupTaskExecutor projectInviteCleanupTaskExecutor) {
        //recurring tasks are automatically picked up by the scheduler
        return Tasks.recurring("project-invite-cleanup", Schedules.parseSchedule(projectInviteCleanupSchedule)).execute(projectInviteCleanupTaskExecutor)
    }

    @Bean
    RecurringTask<UnachievableLevelIdentification> identifyUnachievableLevels(UnachievableLevelIdentificationTaskExecutor unachievableLevelIdentificationTaskExecutor) {
        //recurring tasks are automatically picked up by the scheduler
        return Tasks.recurring("unachievable-level-identification", Schedules.parseSchedule(unachievableLevelIdentificationSchedule)).execute(unachievableLevelIdentificationTaskExecutor)
    }

    @Bean
    RecurringTask<ExpireUserAchievements> expireUserAchievements(ExpireUserAchievementsTaskExecutor expireUserAchievementsTaskExecutor) {
        //recurring tasks are automatically picked up by the scheduler
        return Tasks.recurring("expire-user-achievements", Schedules.parseSchedule(expireUserAchievementsSchedule)).execute(expireUserAchievementsTaskExecutor)
    }

    Instant getExpireUserAchievementsTaskExecutionTime(Instant expirationTime) {
        Instant now = Instant.now()
        if (expirationTime.isBefore(now)) {
            // the calculated expiration time is in the past, so get the next task execution time
            Schedules.parseSchedule(expireUserAchievementsSchedule).getNextExecutionTime(ExecutionComplete.simulatedSuccess(now))
        }
        return Schedules.parseSchedule(expireUserAchievementsSchedule).getNextExecutionTime(ExecutionComplete.simulatedSuccess(expirationTime))
    }
}
