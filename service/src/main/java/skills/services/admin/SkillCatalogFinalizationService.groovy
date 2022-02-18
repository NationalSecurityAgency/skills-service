/**
 * Copyright 2021 SkillTree
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
package skills.services.admin

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.controller.request.model.ProjectSettingsRequest
import skills.controller.result.model.SettingsResult
import skills.services.RuleSetDefGraphService
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.SkillDef
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo
import skills.storage.repos.nativeSql.NativeQueriesRepo
import skills.tasks.TaskSchedulerService
import skills.tasks.config.TaskConfig

@Service
@Slf4j
class SkillCatalogFinalizationService {

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    RuleSetDefGraphService ruleSetDefGraphService

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    NativeQueriesRepo nativeQueriesRepo

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    @Autowired
    TaskSchedulerService taskSchedulerService

    @Autowired
    SettingsService settingsService

    final String PROJ_FINALIZE_STATE_PROP = "FinalizeState"
    static enum FinalizeState {
        NOT_RUNNING, RUNNING, COMPLETED, FAILED
    }

    @Transactional
    @Profile
    void requestFinalizationOfImportedSkills(String projectId) {
        if (getCurrentState(projectId) == FinalizeState.RUNNING) {
            throw new SkillException("Catalog import finalize is already running for [${projectId}]", projectId)
        }
        updateState(projectId, FinalizeState.RUNNING)

        taskSchedulerService.scheduleCatalogImportFinalization(projectId)
    }

    @Transactional
    @Profile
    void finalizeCatalogSkillsImport(String projectId) {
        try {
            log.info("Finalizing imported skills for [{}]", projectId)
            projDefAccessor.getProjDef(projectId) // validate
            List<SkillDef> disabledImportedSkills = skillDefRepo.findAllByProjectIdAndTypeAndEnabledAndCopiedFromIsNotNull(projectId, SkillDef.ContainerType.Skill, Boolean.FALSE.toString())

            if (disabledImportedSkills) {
                disabledImportedSkills.each {
                    it.enabled = Boolean.TRUE.toString()
                }
                skillDefRepo.saveAll(disabledImportedSkills)

                // important: must update subject's total points first then project
                List<SkillDef> subjects = disabledImportedSkills.collect { ruleSetDefGraphService.getParentSkill(it.id) }
                        .unique(false) { SkillDef a, SkillDef b -> a.skillId <=> b.skillId }
                subjects.each {
                    skillDefRepo.updateSubjectTotalPoints(projectId, it.skillId)
                }
                skillDefRepo.updateProjectsTotalPoints(projectId)


                List<Integer> skillRefIds = disabledImportedSkills.collect { it.copiedFrom }

                userPointsRepo.copyUserPointsToTheImportedProjects(projectId, skillRefIds)
                userPointsRepo.createProjectUserPointsForTheNewUsers(projectId)
                nativeQueriesRepo.updateProjectUserPointsForAllUsers(projectId)

                userAchievedLevelRepo.copySkillAchievementsToTheImportedProjects(projectId, skillRefIds)

                SettingsResult settingsResult = settingsService.getProjectSetting(projectId, Settings.LEVEL_AS_POINTS.settingName)
                boolean pointsBased = settingsResult ? settingsResult.isEnabled() : false

                subjects.each { SkillDef subject ->
                    userPointsRepo.createSubjectUserPointsForTheNewUsers(projectId, subject.skillId)
                    nativeQueriesRepo.updateSubjectUserPointsForAllUsers(projectId, subject.skillId)

                    nativeQueriesRepo.identifyAndAddSubjectLevelAchievements(subject.projectId, subject.skillId, pointsBased)
                    log.info("Completed import for subject. projectIdTo=[{}], subjectIdTo=[{}]", projectId, subject.skillId)
                }
                nativeQueriesRepo.identifyAndAddProjectLevelAchievements(projectId, pointsBased)

            } else {
                log.warn("Finalize was called for [{}] projectId but there were no disabled skills", projectId)
            }

            updateState(projectId, FinalizeState.COMPLETED)
            log.info("Completed finalizing imported skills for [{}]", projectId)
        } catch (Throwable t) {
            updateState(projectId, FinalizeState.FAILED)
            throw new TaskConfig.DoNotRetryAsyncTaskException("Failed to finazlie [${projectId}] project", t)
        }
    }

    private FinalizeState getCurrentState(String projectId) {
        SettingsResult res = settingsService.getProjectSetting(projectId, PROJ_FINALIZE_STATE_PROP)
        return res?.value ? FinalizeState.valueOf(res?.value) : FinalizeState.NOT_RUNNING
    }
    private void updateState(String projectId, FinalizeState state) {
        ProjectSettingsRequest startedState = new ProjectSettingsRequest(
                projectId: projectId,
                setting: PROJ_FINALIZE_STATE_PROP,
                value: state.toString()
        )
        settingsService.saveSetting(startedState)
    }
}
