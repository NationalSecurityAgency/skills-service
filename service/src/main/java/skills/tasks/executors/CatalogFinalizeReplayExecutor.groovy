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
package skills.tasks.executors

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.VoidExecutionHandler
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.admin.SkillCatalogFinalizationService
import skills.tasks.data.CatalogFinalizeReplayRequest

@Component
@Slf4j
class CatalogFinalizeReplayExecutor implements VoidExecutionHandler<CatalogFinalizeReplayRequest> {
    @Autowired SkillCatalogFinalizationService skillCatalogFinalizationService

    @Override
    void execute(TaskInstance<CatalogFinalizeReplayRequest> taskInstance, ExecutionContext executionContext) {
        CatalogFinalizeReplayRequest data = taskInstance.data
        log.info('Replaying catalog finalization events: taskId=[{}], projectId=[{}], start=[{}], end=[{}]',
                taskInstance.id, data.projectId, data.start, data.end)
        skillCatalogFinalizationService.applyEventsThatWereReportedDuringTheFinalizationRun(data.skillRefIds, data.start, data.end)
    }
}
