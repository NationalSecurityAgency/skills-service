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

import callStack.profiler.CProf
import callStack.profiler.ProfileEvent
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.VoidExecutionHandler
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.services.admin.SkillCatalogService
import skills.services.admin.SkillsAdminService
import skills.tasks.data.CatalogSkillDefinitionUpdated

@Component
@Slf4j
class CatalogSkillUpdatedTaskExecutor implements VoidExecutionHandler<CatalogSkillDefinitionUpdated> {

    @Autowired
    SkillCatalogService skillCatalogService

    @Autowired
    SkillsAdminService skillsAdminService

    @Value('#{"${skills.async.syncCatalogSkillDefinition.prof.minMillisToPrint:2000}"}')
    int minMillisToPrint

    @Transactional
    @Override
    void execute(TaskInstance<CatalogSkillDefinitionUpdated> taskInstance, ExecutionContext executionContext) {
        def data = taskInstance.getData()
        log.debug("Running async CatalogSkillUpdatedTaskExecutor for [{}-{}]", data.projectId, data.skillId)

        CProf.clear()
        String profName = "skillSync".toString()
        CProf.start(profName)

        skillCatalogService.distributeCatalogSkillUpdates(data.projectId, data.skillId, data.rawId)

        ProfileEvent resProfEvent = CProf.stop(profName)
        if (resProfEvent.getRuntimeInMillis() > minMillisToPrint) {
            log.info("Profiled CatalogSkillUpdatedTaskExecutor for projectId=[{}], skillId=[{}]:\n{}", data.projectId, data.skillId, CProf.prettyPrint())
        }
        log.debug("Completed async CatalogSkillUpdatedTaskExecutor for [{}-{}]", data.projectId, data.skillId)
    }
}
