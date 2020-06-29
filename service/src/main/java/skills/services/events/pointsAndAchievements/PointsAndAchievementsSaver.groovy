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

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo

@Component
@Slf4j
@CompileStatic
class PointsAndAchievementsSaver {

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    @Profile
    void save(DataToSave dataToSave) {
        saveNewPoints(dataToSave)
        addToExistingPoints(dataToSave)
        saveAchievements(dataToSave)
    }

    @Profile
    private Iterable<UserAchievement> saveAchievements(DataToSave dataToSave) {
        userAchievedLevelRepo.saveAll(dataToSave.userAchievements)
    }

    @Profile
    private List<SkillEventsSupportRepo.TinyUserPoints> addToExistingPoints(DataToSave dataToSave) {
        dataToSave.toAddPointsTo.each {
            skillEventsSupportRepo.addUserPoints(it.id, dataToSave.pointIncrement)
        }
    }
    @Profile
    private Iterable<UserPoints> saveNewPoints(DataToSave dataToSave) {
        try {
            userPointsRepo.saveAll(dataToSave.toSave)
        } catch (Throwable t) {
            dataToSave.toSave.each {
                log.info("Failed ---> {}", it)
            }
        }
    }
}
