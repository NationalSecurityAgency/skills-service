package skills.metricsNew.builders.project

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import skills.metricsNew.builders.MetricsChartBuilder
import skills.metricsNew.builders.MetricsPagingParamsHelper
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.repos.UserAchievedLevelRepo

import java.text.DateFormat
import java.text.SimpleDateFormat

@Component
class UserAchievementsChartBuilder implements MetricsChartBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    MetricsPagingParamsHelper pagingParamsHelper

    @Override
    String getId() {
        return "userAchievementsChartBuilder"
    }

    static class UserAchievementsRes {
        Integer totalNumItems
        List<MetricUserAchievement> items
    }

    static class MetricUserAchievement {
        String userId
        String userName
        Long timestamp
        MetricAchievement achievement
    }

    static class MetricAchievement {
        String skillId
        String name
        String type
        Integer level
    }

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd")
    static String OverallType = "Overall"
    private static long oneDay = 1000*60*60*24
    private static long oneDayMinusASecond = oneDay - 1000
    private static long thirtyDays = 30*oneDay
    @Override
    UserAchievementsRes build(String projectId, String chartId, Map<String, String> props) {
        PageRequest pageRequest = pagingParamsHelper.createPageRequest(projectId, chartId, props)
        String usernameFilter = props["usernameFilter"] ?: ""
        Date fromDate = props["fromDateFilter"] ? dateFormat.parse(props["fromDateFilter"]) : new Date(1)
        Date toDate = props["toDateFilter"] ? new Date(dateFormat.parse(props["toDateFilter"]).time+oneDayMinusASecond) : new Date(new Date().time + 30 * thirtyDays)

        String skillNameFilter = props["nameFilter"] ?: "ALL"
        Integer minLevel = props["minLevel"] ? Integer.parseInt(props["minLevel"]) : -1

        List<String> achievementTypes = props["achievementTypes"] ? props["achievementTypes"].split(",").toList() : []
        List<SkillDef.ContainerType> achievementTypesWithoutOverall = achievementTypes.findAll({ !it.equalsIgnoreCase(OverallType) }).collect { SkillDef.ContainerType.valueOf(it)}
        String allNonOverallTypes = achievementTypesWithoutOverall.size() < 3 ? "false" : true
        String includeOverallType = achievementTypes.contains(OverallType) ? "true" : "false"

        List<Object[]> achievements = userAchievedRepo.findAllForAchievementNavigator(
                projectId, usernameFilter, fromDate, toDate, skillNameFilter, minLevel, achievementTypesWithoutOverall, allNonOverallTypes, includeOverallType, pageRequest)
        int totalNumItems = userAchievedRepo.countForAchievementNavigator(
                projectId, usernameFilter, fromDate, toDate, skillNameFilter, minLevel, achievementTypesWithoutOverall, allNonOverallTypes, includeOverallType)

        List items = achievements.collect {
            UserAchievement userAchievement = it[0]
            SkillDef skillDef = it[1]

            return new MetricUserAchievement(
                    timestamp: userAchievement.updated.time,
                    userId: userAchievement.userId,
                    userName: userAchievement.userId,
                    achievement: getAchievement(userAchievement, skillDef)
            )
        }
        return new UserAchievementsRes(totalNumItems: totalNumItems, items: items)
    }

    private MetricAchievement getAchievement(UserAchievement achievement, SkillDef skillDef) {
        if (!achievement.skillId) {
            return new MetricAchievement(name: "Overall", type: "Overall", level: achievement.level)
        }
        return new MetricAchievement(
                skillId: skillDef.skillId,
                name: skillDef.name,
                type: skillDef.type.toString(),
                level: achievement.level,
        )
    }
}
