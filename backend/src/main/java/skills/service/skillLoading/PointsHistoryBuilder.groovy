package skills.service.skillLoading

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.service.skillLoading.model.SkillHistoryPoints
import skills.storage.model.DayCountItem
import skills.storage.repos.UserPerformedSkillRepo

@Component
@Slf4j
@CompileStatic
class PointsHistoryBuilder {

    // optional
    int minNumOfDaysBeforeReturningHistory = 2

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    List<SkillHistoryPoints> buildHistory(String projectId, String userId, Integer showHistoryForNumDays, String skillId = null, Integer version = Integer.MAX_VALUE) {
        List<DayCountItem> userPoints
        if(skillId) {
            userPoints = userPerformedSkillRepo.calculatePointHistoryByProjectIdAndUserIdAndVersion(projectId, userId, skillId, version)
        } else {
            userPoints = userPerformedSkillRepo.calculatePointHistoryByProjectIdAndUserIdAndVersion(projectId, userId, null, version)
        }

        Map<Date, Integer> pointsByDay = [:]
        userPoints.each {
            // it.day is java.sql.Date - bye bye!
            pointsByDay.put(new Date(it.day.time).clearTime(), it.count)
        }

        return doBuildHistory(pointsByDay, showHistoryForNumDays)
    }

    private List<SkillHistoryPoints> doBuildHistory(Map<Date, Integer> pointsByDay, Integer showHistoryForNumDays) {
        if (!pointsByDay || pointsByDay.size() < minNumOfDaysBeforeReturningHistory) {
            return Collections.EMPTY_LIST
        }

        int toAddForFirstDay = 0
        if (showHistoryForNumDays != null) {
            Date startDate = new Date() - showHistoryForNumDays
            startDate = new Date(startDate.time).clearTime()
            Map<Date, Integer> toRemove = pointsByDay.findAll { it.key.before(startDate) }
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
                pointsByDay[theDate] = 0
            }
        }
        List<SkillHistoryPoints> historyPoints = pointsByDay.collect {
            new SkillHistoryPoints(dayPerformed: it.key, points: it.value)
        }.sort({ it.dayPerformed })

        historyPoints.first().points = historyPoints.first().points + toAddForFirstDay

        int pointsSoFar = 0
        historyPoints.each {
            it.points += pointsSoFar
            pointsSoFar = it.points
        }

        return historyPoints
    }

}
