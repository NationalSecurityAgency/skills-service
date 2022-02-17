/**
 * Copyright 2022 SkillTree
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

import com.github.kagkarlsson.scheduler.Serializer
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer
import com.github.kagkarlsson.scheduler.task.FailureHandler
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import skills.tasks.JsonSerializer
import skills.tasks.data.CatalogSkillDefinitionUpdated
import skills.tasks.data.ImportedSkillAchievement
import skills.tasks.executors.CatalogSkillUpdatedTaskExecutor
import skills.tasks.executors.ImportedSkillAchievementTaskExecutor

import java.time.Duration

@Slf4j
@Configuration
class TaskConfig {

    @Value('#{"${skills.config.taskMaxRetries:6}"}')
    int maxRetries

    @Value('#{"${skills.config.exponentialBackOffSeconds:1}"}')
    int exponentialBackOffSeconds

    @Value('#{"${skills.config.exponentialBackOffRate:2}"}')
    int exponentialBackOffRate

    @Bean
    DbSchedulerCustomizer customizer() {
        return new DbSchedulerCustomizer() {
            @Override
            Optional<Serializer> serializer() {
                return Optional.of(new JsonSerializer())
            }
        }
    }

    @Bean
    OneTimeTask<CatalogSkillDefinitionUpdated> catalogSkillDefinitionUpdatedOneTimeTask(CatalogSkillUpdatedTaskExecutor catalogSkillUpdatedTaskExecutor) {
        return Tasks.oneTime("catalog-skill-updated", CatalogSkillDefinitionUpdated.class)
                .onFailure(
                        new FailureHandler.MaxRetriesFailureHandler(maxRetries,
                                new FailureHandler.ExponentialBackoffFailureHandler<>(Duration.ofSeconds(exponentialBackOffSeconds), exponentialBackOffRate))
                )
                .execute(catalogSkillUpdatedTaskExecutor);
    }

    @Bean
    OneTimeTask<ImportedSkillAchievement> importedSkillAchievementOneTimeTask(ImportedSkillAchievementTaskExecutor importedSkillAchievementTaskExecutor) {
        return Tasks.oneTime("imported-skill-achievement", ImportedSkillAchievement.class)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler(maxRetries,
                    new FailureHandler.ExponentialBackoffFailureHandler<>(Duration.ofSeconds(exponentialBackOffSeconds), exponentialBackOffRate))
            )
            .execute(importedSkillAchievementTaskExecutor)
    }

}
