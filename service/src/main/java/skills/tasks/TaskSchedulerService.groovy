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
package skills.tasks

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import skills.services.events.SkillDate
import skills.tasks.data.CatalogFinalizeRequest
import skills.tasks.data.CatalogSkillDefinitionUpdated
import skills.tasks.data.ImportedSkillAchievement
import skills.tasks.data.ProjectInviteCleanup

import javax.annotation.PostConstruct
import java.time.Instant

@Service
@Slf4j
class TaskSchedulerService {

    @Autowired
    Scheduler scheduler

    @Value('#{"${skills.config.taskSchedulingDelayInSeconds}"}')
    int schedulingDelaySeconds

    @Autowired
    OneTimeTask<CatalogSkillDefinitionUpdated> catalogSkillDefinitionUpdatedOneTimeTask

    @Autowired
    OneTimeTask<ImportedSkillAchievement> importedSkillAchievementOneTimeTask

    @Autowired
    OneTimeTask<CatalogFinalizeRequest> finalizeCatalogImportsOneTimeTask

    void scheduleCatalogSkillUpdate(String projectId, String catalogSkillId, Integer rawId){
        String id = "${catalogSkillId}-${UUID.randomUUID().toString()}}"
        log.debug("scheduling catalog skill update task [{}] using db-scheduler", id)
        scheduler.schedule(catalogSkillDefinitionUpdatedOneTimeTask.instance(id, new CatalogSkillDefinitionUpdated(
                rawId: rawId,
                skillId: catalogSkillId,
                projectId: projectId
        )), Instant.now().plusSeconds(schedulingDelaySeconds))
    }

    void scheduleImportedSkillAchievement(String userId, Integer rawSkillId, SkillDate incomingSkillDate, boolean thisRequestCompletedOriginalSkill) {
        String uuid = UUID.randomUUID().toString()
        String id = "${rawSkillId}-${uuid}"
        ImportedSkillAchievement importedSkillAchievement = new ImportedSkillAchievement(
                uuid: uuid,
                userId: userId,
                rawSkillId: rawSkillId,
                incomingSkillDate: incomingSkillDate,
                thisRequestCompletedOriginalSkill: thisRequestCompletedOriginalSkill
        )
        log.debug("scheduling imported skill achievement task [{}] using db-scheduler", id)
        scheduler.schedule(importedSkillAchievementOneTimeTask.instance(id, importedSkillAchievement), Instant.now().plusSeconds(schedulingDelaySeconds))
    }

    void scheduleCatalogImportFinalization(String projectId){
        String id = "${projectId}-${UUID.randomUUID().toString()}}"
        log.info("scheduling catalog import finalization for [{}] using db-scheduler", id)
        scheduler.schedule(finalizeCatalogImportsOneTimeTask.instance(id, new CatalogFinalizeRequest(
                projectId: projectId
        )), Instant.now().plusSeconds(schedulingDelaySeconds))
    }
}
