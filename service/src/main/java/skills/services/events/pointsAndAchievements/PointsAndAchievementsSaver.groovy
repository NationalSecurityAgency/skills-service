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
        handleSkillsGroupUserPoints(dataToSave)
    }

    @Profile
    private Iterable<UserAchievement> saveAchievements(DataToSave dataToSave) {
        userAchievedLevelRepo.saveAll(dataToSave.userAchievements)
    }

    @Profile
    private List<SkillEventsSupportRepo.TinyUserPoints> addToExistingPoints(DataToSave dataToSave) {
        List<SkillEventsSupportRepo.TinyUserPoints> toAddPointsTo = dataToSave.toAddPointsTo
        if (isPartOfSkillsGroupWithLessThanAllRequired(dataToSave)) {
            // do not add points to overall group, subject, or project yet, will be done in handleSkillsGroupUserPoints
            toAddPointsTo = toAddPointsTo.findAll {it.day || (it.skillRefId && it.skillRefId != dataToSave.skillsGroupDefId && it.skillRefId != dataToSave.subjectDefId) }
        }
        toAddPointsTo.each {
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

    @Profile
    private void handleSkillsGroupUserPoints(DataToSave dataToSave) {
        if (isPartOfSkillsGroupWithLessThanAllRequired(dataToSave)) {
            Integer skillsGroupDefId = dataToSave.skillsGroupDefId
            Integer subjectDefId = dataToSave.subjectDefId
            String userId = dataToSave.userId
            Integer numChildSkillsRequired = dataToSave.numChildSkillsRequired
            Integer newSkillsGroupPoints = 0

            // load all of the group's child skills and update their contributing flag
            List<SkillEventsSupportRepo.TinyUserPoints> skillsGroupChildUserPoints = loadChildPoints(userId, skillsGroupDefId)
            skillsGroupChildUserPoints.sort{it.points}.dropRight(numChildSkillsRequired).each {
                skillEventsSupportRepo.updateContributingFlag(it.id, Boolean.FALSE.toString())
            }
            skillsGroupChildUserPoints.sort{it.points}.takeRight(numChildSkillsRequired).each {
                skillEventsSupportRepo.updateContributingFlag(it.id, Boolean.TRUE.toString())
                newSkillsGroupPoints += it.points
            }

            List<SkillEventsSupportRepo.TinyUserPoints> toAddPointsTo = dataToSave.toAddPointsTo.findAll { !it.day && (!it.skillRefId || it.skillRefId == skillsGroupDefId || it.skillRefId == subjectDefId) }
            Integer existingSkillsGroupPoints = dataToSave.toAddPointsTo.find { it.skillRefId == skillsGroupDefId }?.points ?: 0
            Integer pointsToAddOrSubtract = 0
            if (existingSkillsGroupPoints > newSkillsGroupPoints) {
                // need to subtract from previously added group points
                pointsToAddOrSubtract = existingSkillsGroupPoints - newSkillsGroupPoints
            } else {
                pointsToAddOrSubtract = newSkillsGroupPoints - existingSkillsGroupPoints
            }
            toAddPointsTo.each {
                skillEventsSupportRepo.addUserPoints(it.id, pointsToAddOrSubtract)
            }

        }
    }

    boolean isPartOfSkillsGroupWithLessThanAllRequired(DataToSave dataToSave) {
        return dataToSave.skillsGroupDefId && dataToSave.numChildSkillsRequired > 0
    }

    @Profile
    private List<SkillEventsSupportRepo.TinyUserPoints> loadChildPoints(String userId, Integer parentId) {
        return skillEventsSupportRepo.findTotalTinyUserPointsByUserIdAndParentId(userId, parentId)
    }
}
