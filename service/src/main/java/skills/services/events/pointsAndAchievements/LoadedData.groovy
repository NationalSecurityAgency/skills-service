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
package skills.services.events.pointsAndAchievements

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import skills.storage.model.LevelDefInterface
import skills.storage.repos.SkillEventsSupportRepo

@Canonical
@CompileStatic
class LoadedData {
    String userId
    String projectId
    List<SkillEventsSupportRepo.TinySkillDef> parentDefs
    List<SkillEventsSupportRepo.TinyUserPoints> tinyUserPoints
    List<LevelDefInterface> levels
    List<SkillEventsSupportRepo.TinyUserAchievement> tinyUserAchievements
    SkillEventsSupportRepo.TinyProjectDef tinyProjectDef

    /**
     * @param skillRefId null will retrieve OVERALL points (an not for a specific skill);
     * the word 'Total' in the name refers to total points for all days which can either be for a specific skill or OVERALL
     * @return
     */
    SkillEventsSupportRepo.TinyUserPoints getTotalUserPoints(Integer skillRefId) {
        return tinyUserPoints.find({
            if (skillRefId) {
                return it.getSkillRefId() && it.getSkillRefId() == skillRefId && !it.getDay()
            }

            return !it.getSkillRefId() && !it.getDay()
        })
    }

    List<SkillEventsSupportRepo.TinyUserAchievement> getUserAchievements(Integer skillRefId) {
        return tinyUserAchievements.findAll({
            if (skillRefId) {
                return it.getSkillRefId() && it.getSkillRefId() == skillRefId
            }
            return !it.getSkillRefId()
        })
    }

    List<SkillEventsSupportRepo.TinyUserPoints> getUserPoints(Integer skillRefId) {
        return tinyUserPoints.findAll({
            if (skillRefId) {
                return it.getSkillRefId() && it.getSkillRefId() == skillRefId
            }
            return !it.getSkillRefId()
        })
    }
}
