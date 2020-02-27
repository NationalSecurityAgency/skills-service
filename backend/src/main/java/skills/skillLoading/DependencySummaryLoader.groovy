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
package skills.skillLoading

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.skillLoading.model.SkillDependencySummary
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.UserPointsRepo

@Component
@Slf4j
class DependencySummaryLoader {

    @Autowired
    UserPointsRepo userPointsRepo

    private static class SkillWithAchievementIndicator {
        Integer skillDefId
        boolean isAchieved
    }

    @Transactional(readOnly = true)
    @Profile
    SkillDependencySummary loadDependencySummary(String userId, String projectId, String skillId){
        List<SkillWithAchievementIndicator> dependents = loadDependentSkills(userId, projectId, skillId)
        SkillDependencySummary dependencySummary = dependents ? new SkillDependencySummary(
                numDirectDependents: dependents.size(),
                achieved: !dependents.find { !it.isAchieved }
        ) : null

        return dependencySummary
    }

    private List<SkillWithAchievementIndicator> loadDependentSkills(String userId, String projectId, String skillId) {
        // there is no reason to exclude based on version as the system will not allow to dependent skills with later version
        List<Object []> dependentSkillsAndTheirAchievementStatus = userPointsRepo.findChildrenAndTheirAchievements(userId, projectId, skillId, SkillRelDef.RelationshipType.Dependence, Integer.MAX_VALUE)
        return dependentSkillsAndTheirAchievementStatus.collect {
            new SkillWithAchievementIndicator(skillDefId: it[0], isAchieved: it[1] != null)
        }
    }
}
