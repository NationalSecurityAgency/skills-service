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
import skills.services.LevelDefinitionStorageService
import skills.services.events.CompletionItem
import skills.services.events.SkillDate
import skills.storage.model.SkillDefMin
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.SkillRelDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo

@Component
@Slf4j
@CompileStatic
class PointsAndAchievementsHandler {

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    PointsAndAchievementsSaver saver

    @Autowired
    PointsAndAchievementsDataLoader dataLoader

    @Profile
    List<CompletionItem> updatePointsAndAchievements(String userId, SkillDefMin skillDef, SkillDate incomingSkillDate){
        LoadedData loadedData = dataLoader.loadData(skillDef.projectId, userId, incomingSkillDate, skillDef)

        PointsAndAchievementsBuilder builder = new PointsAndAchievementsBuilder(
                userId: userId,
                projectId: skillDef.projectId,
                skillId: skillDef.skillId,
                skillRefId: skillDef.id,
                loadedData: loadedData,
                pointIncrement: skillDef.pointIncrement,
                incomingSkillDate: incomingSkillDate,
                levelDefService: levelDefService,
                skillEventsSupportRepo: skillEventsSupportRepo,
        )
        PointsAndAchievementsBuilder.PointsAndAchievementsResult result = builder.build()
        saver.save(result.dataToSave)
        return result.completionItems
    }
}
