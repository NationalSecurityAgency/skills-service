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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.skillLoading.model.SkillHistoryPoints
import skills.storage.model.DayCountItem
import skills.storage.model.UserAchievement
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo

@Component
@Slf4j
@CompileStatic
class PointsHistoryBuilder {

    // optional
    int minNumOfDaysBeforeReturningHistory = 2

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    List<SkillHistoryPoints> buildHistory(String projectId, String userId, Integer showHistoryForNumDays, String skillId = null, Integer version = Integer.MAX_VALUE) {
        List<DayCountItem> userPoints
        if(skillId) {
            userPoints = userPerformedSkillRepo.calculatePointHistoryByProjectIdAndUserIdAndVersion(projectId, userId, skillId, version)
        } else {
            userPoints = userPerformedSkillRepo.calculatePointHistoryByProjectIdAndUserIdAndVersion(projectId, userId, null, version)
        }

        Map<Date, Long> pointsByDay = [:]
        userPoints.each {
            // it.day is java.sql.Date - bye bye!
            pointsByDay.put(new Date(it.day.time).clearTime(), it.count)
        }

        return doBuildHistory(pointsByDay, showHistoryForNumDays)
    }

    private List<SkillHistoryPoints> doBuildHistory(Map<Date, Long> pointsByDay, Integer showHistoryForNumDays) {
        if (!pointsByDay || pointsByDay.size() < minNumOfDaysBeforeReturningHistory) {
            return Collections.EMPTY_LIST
        }

        long toAddForFirstDay = 0
        if (showHistoryForNumDays != null) {
            Date startDate = new Date() - showHistoryForNumDays
            startDate = new Date(startDate.time).clearTime()
            Map<Date, Long> toRemove = pointsByDay.findAll { it.key.before(startDate) }
            if (toRemove) {
                toAddForFirstDay = (int) toRemove.collect { it.value }.sum()
                toRemove.each {
                    pointsByDay.remove(it.key)
                }
            }

            if (!pointsByDay || (toRemove && pointsByDay.collect { it.key }.min().after(startDate))) {
                // first case: this can happen if all of the values were removed because of showHistoryForNumDays argument
                // second case: need to begin at specified start date if we removed dates that were before
                pointsByDay[startDate] = toAddForFirstDay
                toAddForFirstDay = 0
            }
        }

        List<Date> dates = pointsByDay.collect { new Date(it.key.time).clearTime() }
        dates.min().upto(new Date().clearTime()) { Date theDate ->
            if (!pointsByDay.containsKey(theDate)) {
                pointsByDay[theDate] = 0L
            }
        }
        List<SkillHistoryPoints> historyPoints = pointsByDay.collect {
            new SkillHistoryPoints(dayPerformed: it.key, points: it.value.toInteger())
        }.sort({ it.dayPerformed })

        historyPoints.first().points = (historyPoints.first().points + toAddForFirstDay).toInteger()

        int pointsSoFar = 0
        historyPoints.each {
            it.points += pointsSoFar
            pointsSoFar = it.points
        }

        return historyPoints
    }

}
