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
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import skills.services.CatalogParticipantBackfillService
import skills.storage.model.ProjectParticipation
import skills.storage.repos.ProjectParticipationRepo
import skills.tasks.data.CatalogParticipantBackfillRequest

@Component
@Slf4j
class CatalogParticipantBackfillExecutor implements VoidExecutionHandler<CatalogParticipantBackfillRequest> {
    @Autowired CatalogParticipantBackfillService backfillService
    @Autowired ProjectParticipationRepo participationRepo

    @Override
    void execute(TaskInstance<CatalogParticipantBackfillRequest> task, ExecutionContext context) {
        CatalogParticipantBackfillRequest request = task.data
        long started = System.nanoTime()
        int processed = 0
        String currentUserId = request.userId
        if (request.userId) {
            log.debug('Starting catalog participant backfill: taskId=[{}], projectId=[{}], userId=[{}]', task.id, request.projectId, request.userId)
        } else {
            log.info('Starting catalog participant backfill sweep: taskId=[{}], projectId=[{}]', task.id, request.projectId)
        }
        try {
            if (request.userId) {
                // Backfill one newly qualified participant after catalog finalization.
                backfillService.backfill(request.projectId, request.userId)
                processed++
            } else {
                // Finalization schedules a sweep of pending participants, including those who joined while it was running.
                int afterId = 0
                List<ProjectParticipation> page
                while ((page = participationRepo.findPending(request.projectId, afterId, PageRequest.of(0, 100)))) {
                    log.debug('Processing catalog participant backfill page: taskId=[{}], projectId=[{}], afterId=[{}], pageSize=[{}]', task.id, request.projectId, afterId, page.size())
                    page.each { ProjectParticipation participant ->
                        currentUserId = participant.userId
                        backfillService.backfill(request.projectId, participant.userId)
                        processed++
                        log.trace('Returned from catalog participant backfill transaction: taskId=[{}], projectId=[{}], userId=[{}]', task.id, request.projectId, participant.userId)
                    }
                    afterId = page.last().id
                }
            }
        } catch (Exception e) {
            // The scheduler owns retry handling and exception stack-trace logging.
            log.warn('Catalog participant backfill failed: taskId=[{}], projectId=[{}], userId=[{}], participantsProcessed=[{}], elapsedMs=[{}], failureType=[{}]',
                    task.id, request.projectId, currentUserId, processed, (System.nanoTime() - started) / 1_000_000L, e.class.simpleName)
            throw e
        }
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L
        if (request.userId) {
            log.debug('Completed catalog participant backfill task: taskId=[{}], projectId=[{}], userId=[{}], elapsedMs=[{}]', task.id, request.projectId, request.userId, elapsedMs)
            if (elapsedMs > 500) {
                log.info('Slow catalog participant backfill task: taskId=[{}], projectId=[{}], userId=[{}], elapsedMs=[{}]', task.id, request.projectId, request.userId, elapsedMs)
            }
        } else {
            log.info('Completed catalog participant backfill sweep: taskId=[{}], projectId=[{}], participantsProcessed=[{}], elapsedMs=[{}]', task.id, request.projectId, processed, elapsedMs)
        }
    }
}
