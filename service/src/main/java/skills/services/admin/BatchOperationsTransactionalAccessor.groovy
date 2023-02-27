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
package skills.services.admin

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.services.RuleSetDefinitionScoreUpdater
import skills.services.UserAchievementsAndPointsManagement
import skills.storage.model.SkillDef
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo
import skills.storage.repos.nativeSql.NativeQueriesRepo

import jakarta.transaction.Transactional

@Service
@Slf4j
class BatchOperationsTransactionalAccessor {

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    @Autowired
    NativeQueriesRepo nativeQueriesRepo

    @Autowired
    UserAchievementsAndPointsManagement userAchievementsAndPointsManagement

    @Autowired
    RuleSetDefinitionScoreUpdater ruleSetDefinitionScoreUpdater

    @Autowired
    SkillsGroupAdminService skillsGroupAdminService

    @Transactional
    @Profile
    void enableSkills(List<SkillDef> disabledImportedSkills) {
        disabledImportedSkills.each {
            it.enabled = Boolean.TRUE.toString()
        }
        skillDefRepo.saveAll(disabledImportedSkills)
    }

    @Transactional
    @Profile
    void updateSubjectTotalPoints(String projectId, String subjectId) {
        ruleSetDefinitionScoreUpdater.updateSubjectTotalPoints(projectId, subjectId, false)
    }


    @Transactional
    @Profile
    void updateGroupTotalPoints(String projectId, String groupId) {
        ruleSetDefinitionScoreUpdater.updateGroupTotalPoints(projectId, groupId, false)
    }

    @Transactional
    @Profile
    void updateProjectsTotalPoints(String projectId) {
        skillDefRepo.updateProjectsTotalPoints(projectId, false)
    }

    @Transactional
    @Profile
    void copySkillUserPointsToTheImportedProjects(String toProjectId, List<Integer> fromSkillRefIds) {
        userPointsRepo.copySkillUserPointsToTheImportedProjects(toProjectId, fromSkillRefIds)
    }

    @Transactional
    @Profile
    void copySkillAchievementsToTheImportedProjects(List<Integer> fromSkillRefIds) {
        userAchievedLevelRepo.copySkillAchievementsToTheImportedProjects(fromSkillRefIds)
    }

    @Transactional
    @Profile
    void createSubjectUserPointsForTheNewUsers(String toProjectId, String toSubjectId) {
        userPointsRepo.createSubjectUserPointsForTheNewUsers(toProjectId, toSubjectId)
    }

    @Transactional
    @Profile
    void updateUserPointsForSubject(String projectId, String skillId) {
        nativeQueriesRepo.updateUserPointsForSubject(projectId, skillId, false)
    }

    @Transactional
    @Profile
    void identifyAndAddGroupAchievements(List<SkillDef> groups) {
        groups.each { skillsGroupSkillDef ->
            int numSkillsRequired = skillsGroupAdminService.getActualNumSkillsRequred(skillsGroupSkillDef.numSkillsRequired, skillsGroupSkillDef.id)
            userAchievedLevelRepo.identifyAndAddGroupAchievements(
                    skillsGroupSkillDef.projectId,
                    skillsGroupSkillDef.skillId,
                    skillsGroupSkillDef.id,
                    numSkillsRequired,
                    Boolean.FALSE.toString(),
            )
        }
    }

    @Transactional
    @Profile
    void identifyAndAddSubjectLevelAchievements(String projectId, String subjectId) {
        SkillDef subject = skillDefRepo.findByProjectIdAndSkillId(projectId, subjectId)
        userAchievementsAndPointsManagement.identifyAndAddSubjectLevelAchievements(subject)
    }

    @Transactional
    @Profile
    void createProjectUserPointsForTheNewUsers(String toProjectId) {
        userPointsRepo.createProjectUserPointsForTheNewUsers(toProjectId)
    }

    @Transactional
    @Profile
    void updateUserPointsForProject(String projectId) {
        nativeQueriesRepo.updateUserPointsForProject(projectId)
    }

    @Transactional
    @Profile
    void identifyAndAddProjectLevelAchievements(String projectId, boolean pointsBasedLevels){
        userAchievementsAndPointsManagement.identifyAndAddProjectLevelAchievements(projectId)
    }

}
