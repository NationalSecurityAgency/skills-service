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
import org.springframework.transaction.annotation.Transactional
import skills.services.admin.BatchOperationsTransactionalAccessor
import skills.services.LockingService
import skills.services.events.SkillDate
import skills.services.events.pointsAndAchievements.ImportedSkillsAchievementsHandler
import skills.storage.model.SkillDefMin
import skills.storage.repos.SkillDefRepo
import skills.tasks.data.RemoveSkillEventsForUserRequest

@Component
@Slf4j
class RemoveSkillEventsForAUserExecutor implements VoidExecutionHandler<RemoveSkillEventsForUserRequest> {

    @Autowired
    BatchOperationsTransactionalAccessor batchOperationsTransactionalAccessor
    @Autowired SkillDefRepo skillDefRepo
    @Autowired LockingService lockingService
    @Autowired ImportedSkillsAchievementsHandler importedSkillsAchievementsHandler

    @Override
    @Transactional
    void execute(TaskInstance<RemoveSkillEventsForUserRequest> taskInstance, ExecutionContext executionContext) {
        RemoveSkillEventsForUserRequest data = taskInstance.getData()
        log.debug("running async RemoveSkillEventsForAUserExecutor for userId=[{}], projId=[{}], skillRefIds={}", data.userId, data.projectId, data.skillRefIds)

        CProf.clear()
        String profName = "${data.projectId}-${data.userId}-RemoveSkillEventsForAUserExecutor".toString()
        CProf.start(profName)

        lockingService.lockForSkillReporting(data.userId, data.projectId)
        List<SkillDefMin> skills = data.skillRefIds.collect { Integer id -> skillDefRepo.findSkillDefMinById(id) }.findAll { it != null }
        List<Integer> localIds = skills.findAll { SkillDefMin skill -> !skill.copiedFrom }.collect { SkillDefMin skill -> skill.id }
        if (localIds) {
            batchOperationsTransactionalAccessor.batchRemovePerformedSkillsForUserAndSpecificSkills(data.userId, data.projectId, localIds)
        }
        skills.findAll { SkillDefMin skill -> skill.copiedFrom != null }.each { SkillDefMin skill ->
            // An old deletion request must not erase newer source credit.
            importedSkillsAchievementsHandler.handleAchievementsForImportedSkills(data.userId, skill,
                    new SkillDate(date: new Date(), isProvided: false), false, false)
        }

        CProf.stop(profName)
        log.info("Profiled RemoveSkillEventsForAUserExecutor for [{}-{}]:\n{}", data.projectId, data.userId, CProf.prettyPrint())
    }
}
