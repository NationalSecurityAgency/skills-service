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
package skills.tasks.executors

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.VoidExecutionHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.services.admin.InviteOnlyProjectService
import skills.tasks.data.ProjectInviteCleanup

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class ProjectInviteCleanupTaskExecutor implements VoidExecutionHandler<ProjectInviteCleanup> {

    //how long expired or claimed invites are retained in the system before being deleted
    //ISO-8601 duration format
    @Value('#{"${skills.config.projectInvites.retention-time:P30D}"}')
    String retentionPeriod = "P30D"

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    @Override
    void execute(TaskInstance<ProjectInviteCleanup> taskInstance, ExecutionContext executionContext) {
        Duration duration = Duration.parse(retentionPeriod)
        LocalDateTime removeBefore = LocalDateTime.now().minus(duration)
        Date date = Date.from(removeBefore.atZone(ZoneId.systemDefault()).toInstant())
        inviteOnlyProjectService.removeExpiredInviteTokens(date)
        inviteOnlyProjectService.removeClaimedInviteTokens(date)
    }
}
