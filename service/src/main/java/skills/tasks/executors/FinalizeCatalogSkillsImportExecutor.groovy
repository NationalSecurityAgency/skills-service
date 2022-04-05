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
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.VoidExecutionHandler
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.admin.SkillCatalogFinalizationService
import skills.tasks.data.CatalogFinalizeRequest

@Component
@Slf4j
class FinalizeCatalogSkillsImportExecutor implements VoidExecutionHandler<CatalogFinalizeRequest> {

    @Autowired
    SkillCatalogFinalizationService skillCatalogFinalizationService

    @Override
    void execute(TaskInstance<CatalogFinalizeRequest> taskInstance, ExecutionContext executionContext) {
        CatalogFinalizeRequest data = taskInstance.getData()
        log.debug("running async FinalizeCatalogSkillsImportExecutor for [{}]", data.projectId)

        CProf.clear()
        String profName = "${data.projectId}-finalize".toString()
        CProf.start(profName)

        SkillCatalogFinalizationService.FinalizeCatalogSkillsImportResult res = skillCatalogFinalizationService.finalizeCatalogSkillsImport(data.projectId)
        skillCatalogFinalizationService.applyEventsThatWereReportedDuringTheFinalizationRun(res.skillRefIds, res.start, res.end)

        CProf.stop(profName)
        log.info("Profiled FinalizeCatalogSkillsImportExecutor for [{}]:\n{}", data.projectId, CProf.prettyPrint())
    }
}
