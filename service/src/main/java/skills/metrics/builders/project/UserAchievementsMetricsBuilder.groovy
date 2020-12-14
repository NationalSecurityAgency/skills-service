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
package skills.metrics.builders.project

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.JpaSort
import org.springframework.stereotype.Component
import skills.controller.exceptions.SkillException
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams
import skills.metrics.builders.ProjectMetricsBuilder
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserAttrs
import skills.storage.repos.UserAchievedLevelRepo

import java.text.DateFormat
import java.text.SimpleDateFormat

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@Component
class UserAchievementsMetricsBuilder implements ProjectMetricsBuilder {

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

    private final static String PROP_SORT_BY_USER_ID = "userName"
    private final static List<String> supportedSortBy = ["achievedOn", PROP_SORT_BY_USER_ID]

    @Override
    UserAchievementsRes build(String projectId, String chartId, Map<String, String> props) {
        PageRequest pageRequest = getPageRequest(projectId, chartId, props)

        String usernameFilter = MetricsParams.getUsernameFilter(projectId, chartId, props, true)
        Date from = MetricsParams.getFromDayFilter(projectId, chartId, props)
        Date to = MetricsParams.getToDayFilter(projectId, chartId, props)

        String skillNameFilter = MetricsParams.getNameFilter(projectId, chartId, props)
        Integer minLevel = MetricsParams.getMinLevel(projectId, chartId, props)

        List<String> achievementTypes = MetricsParams.getAchievementTypes(projectId, chartId, props)
        List<SkillDef.ContainerType> achievementTypesWithoutOverall = achievementTypes.findAll({ !it.equalsIgnoreCase(MetricsParams.ACHIEVEMENT_TYPE_OVERALL) }).collect { SkillDef.ContainerType.valueOf(it) }
        String allNonOverallTypes = achievementTypesWithoutOverall.size() < 3 ? "false" : "true"
        String includeOverallType = achievementTypes.contains(MetricsParams.ACHIEVEMENT_TYPE_OVERALL) ? "true" : "false"
        String includeGlobalBadge = achievementTypesWithoutOverall.contains(SkillDef.ContainerType.Badge).toString()

        int totalNumItems = userAchievedRepo.countForAchievementNavigator(
                projectId, usernameFilter, from, to, skillNameFilter, minLevel, achievementTypesWithoutOverall,
                allNonOverallTypes, includeOverallType, includeGlobalBadge)
        if (totalNumItems == 0) {
            return new UserAchievementsRes(totalNumItems: 0, items: [])
        }
        List<Object[]> achievements = userAchievedRepo.findAllForAchievementNavigator(
                projectId, usernameFilter, from, to, skillNameFilter, minLevel, achievementTypesWithoutOverall,
                allNonOverallTypes, includeOverallType, includeGlobalBadge, pageRequest)

        List items = achievements.collect {
            UserAchievement userAchievement = it[0]
            SkillDef skillDef = it[1]
            UserAttrs userAttrs = it[2]
            return buildMetricUserAchievement(userAchievement, skillDef, userAttrs)
        }
        return new UserAchievementsRes(totalNumItems: totalNumItems, items: items)
    }

    private PageRequest getPageRequest(String projectId, String chartId, Map<String, String> props) {


        MetricsPagingParamsHelper metricsPagingParamsHelper = new MetricsPagingParamsHelper(projectId, chartId, props)
        int currentPage = metricsPagingParamsHelper.currentPage
        int pageSize = metricsPagingParamsHelper.pageSize
        Boolean sortDesc = metricsPagingParamsHelper.sortDesc
        String sortBy = metricsPagingParamsHelper.sortBy

        if (!supportedSortBy.contains(sortBy)) {
            throw new SkillException("Metrics[${chartId}]: Invalid value [${props[MetricsPagingParamsHelper.PROP_SORT_BY]}] for [${MetricsPagingParamsHelper.PROP_SORT_BY}] property. Suppored values are ${supportedSortBy}", projectId)
        }

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
    private MetricUserAchievement buildMetricUserAchievement(UserAchievement achievement, SkillDef skillDef, UserAttrs userAttrs) {

        MetricUserAchievement res = new MetricUserAchievement(
                achievedOn: achievement.achievedOn.time,
                userId: achievement.userId,
                userName: userAttrs.userIdForDisplay,
                name: "Overall",
                type: "Overall",
                level: achievement.level,
        )

        if (achievement.skillId) {
            res.skillId = skillDef.skillId
            res.name = skillDef.name
            res.type = skillDef.type.toString()
            res.level = achievement.level
        }

        return res
    }
}
