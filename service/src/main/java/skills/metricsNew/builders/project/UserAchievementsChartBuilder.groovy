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
package skills.metricsNew.builders.project

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.JpaSort
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
import skills.metricsNew.builders.MetricsChartBuilder
import skills.metricsNew.builders.MetricsPagingParamsHelper
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.repos.UserAchievedLevelRepo

import java.text.DateFormat
import java.text.SimpleDateFormat

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC
import static org.springframework.data.domain.Sort.Direction.DESC

@Component
class UserAchievementsChartBuilder implements MetricsChartBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

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
        Long achievedOn
        String skillId
        String name
        String type
        Integer level
    }

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd")
    static String OverallType = "Overall"
    private static long oneDay = 1000 * 60 * 60 * 24
    private static long oneDayMinusASecond = oneDay - 1000
    private static long thirtyDays = 30 * oneDay

    private final static String PROP_SORT_BY_USER_ID = "userName"
    private final static List<String> supportedSortBy = ["achievedOn", PROP_SORT_BY_USER_ID]
    @Override
    UserAchievementsRes build(String projectId, String chartId, Map<String, String> props) {

        PageRequest pageRequest = getPageRequest(projectId, chartId, props)

        String usernameFilter = props["usernameFilter"] ?: ""
        Date fromDate = props["fromDateFilter"] ? dateFormat.parse(props["fromDateFilter"]) : new Date(1)
        Date toDate = props["toDateFilter"] ? new Date(dateFormat.parse(props["toDateFilter"]).time + oneDayMinusASecond) : new Date(new Date().time + 30 * thirtyDays)

        String skillNameFilter = props["nameFilter"] ?: "ALL"
        Integer minLevel = props["minLevel"] ? Integer.parseInt(props["minLevel"]) : -1

        List<String> achievementTypes = props["achievementTypes"] ? props["achievementTypes"].split(",").toList() : []
        List<SkillDef.ContainerType> achievementTypesWithoutOverall = achievementTypes.findAll({ !it.equalsIgnoreCase(OverallType) }).collect { SkillDef.ContainerType.valueOf(it) }
        String allNonOverallTypes = achievementTypesWithoutOverall.size() < 3 ? "false" : true
        String includeOverallType = achievementTypes.contains(OverallType) ? "true" : "false"


        List<Object[]> achievements = userAchievedRepo.findAllForAchievementNavigator(
                projectId, usernameFilter, fromDate, toDate, skillNameFilter, minLevel, achievementTypesWithoutOverall, allNonOverallTypes, includeOverallType, pageRequest)
        int totalNumItems = userAchievedRepo.countForAchievementNavigator(
                projectId, usernameFilter, fromDate, toDate, skillNameFilter, minLevel, achievementTypesWithoutOverall, allNonOverallTypes, includeOverallType)

        List items = achievements.collect {
            UserAchievement userAchievement = it[0]
            SkillDef skillDef = it[1]
            return buildMetricUserAchievement(userAchievement, skillDef)
        }
        return new UserAchievementsRes(totalNumItems: totalNumItems, items: items)
    }

    private PageRequest getPageRequest(String projectId, String chartId, Map<String, String> props) {
        if (!supportedSortBy.contains(props[MetricsPagingParamsHelper.PROP_SORT_BY])) {
            throw new SkillException("Chart[${chartId}]: Invalid value [${props[MetricsPagingParamsHelper.PROP_SORT_BY]}] for [${MetricsPagingParamsHelper.PROP_SORT_BY}] property. Suppored values are ${supportedSortBy}", projectId)
        }

        MetricsPagingParamsHelper metricsPagingParamsHelper = new MetricsPagingParamsHelper(projectId, chartId, props)
        int currentPage = metricsPagingParamsHelper.currentPage
        int pageSize = metricsPagingParamsHelper.pageSize
        Boolean sortDesc = metricsPagingParamsHelper.sortDesc
        String sortBy = metricsPagingParamsHelper.sortBy
        if (sortBy == "userName") {
            // looks like there is a bug in the Spring Data's
            //  org.springframework.data.jpa.repository.query.QueryUtils.java
            // where aliases are not respected in some JPQL joins and the default alias (first table in the join) is added
            // no matter what is provided.
            // It maybe fixed by https://jira.spring.io/browse/DATAJPA-1406 even though they scope it to Native Queries
            // the issue is present on JPQL queries as well
            //
            // This is a workaround to "disable" aliasing
            Sort sort = JpaSort.unsafe(sortDesc ?  DESC : ASC, "(uAttrs.userIdForDisplay)")
            return PageRequest.of(currentPage, pageSize, sort)
        }
        return PageRequest.of(currentPage, pageSize, sortDesc ?  DESC : ASC, sortBy)
    }
    private MetricUserAchievement buildMetricUserAchievement(UserAchievement achievement, SkillDef skillDef) {

        MetricUserAchievement res = new MetricUserAchievement(
                achievedOn: achievement.achievedOn.time,
                userId: achievement.userId,
                userName: achievement.userId)

        if (!achievement.skillId) {
            res.name = "Overall"
            res.type = "Overall"
            res.level = achievement.level
        } else {
            res.skillId = skillDef.skillId
            res.name = skillDef.name
            res.type = skillDef.type.toString()
            res.level = achievement.level
        }

        return res
    }
}
