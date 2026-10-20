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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import skills.services.CatalogParticipantBackfillService
import skills.storage.model.ProjectParticipation
import skills.storage.repos.ProjectParticipationRepo
import skills.tasks.data.CatalogParticipantBackfillRequest

@Component
class CatalogParticipantBackfillExecutor implements VoidExecutionHandler<CatalogParticipantBackfillRequest> {
    @Autowired CatalogParticipantBackfillService backfillService
    @Autowired ProjectParticipationRepo participationRepo

    @Override
    void execute(TaskInstance<CatalogParticipantBackfillRequest> task, ExecutionContext context) {
        CatalogParticipantBackfillRequest request = task.data
        if (request.userId) {
            // Backfill one newly qualified participant after catalog finalization.
            backfillService.backfill(request.projectId, request.userId)
        } else {
            // Finalization schedules a sweep of pending participants, including those who joined while it was running.
            int afterId = 0
            List<ProjectParticipation> page
            while ((page = participationRepo.findPending(request.projectId, afterId, PageRequest.of(0, 100)))) {
                page.each { ProjectParticipation participant -> backfillService.backfill(request.projectId, participant.userId) }
                afterId = page.last().id
            }
        }
    }
}
