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

import groovy.transform.Canonical
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.JpaSort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillException
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams
import skills.metrics.builders.ProjectMetricsBuilder
import skills.storage.model.SkillDef
import skills.storage.repos.UserAchievedLevelRepo
import skills.utils.InputSanitizer

import java.util.stream.Collectors
import java.util.stream.Stream

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@Component
class UserAchievementsMetricsBuilder implements ProjectMetricsBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Value('${skills.config.ui.usersTableAdditionalUserTagKey:}')
    String usersTableAdditionalUserTagKey

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
    @Transactional(readOnly = true)
    UserAchievementsRes build(String projectId, String chartId, Map<String, String> props) {
        return build(projectId, chartId, props, true)
    }

    @Transactional(readOnly = true)
    UserAchievementsRes build(String projectId, String chartId, Map<String, String> props, Boolean validatePageSize) {
        QueryParams queryParams = getQueryParams(projectId, chartId, props, validatePageSize)
        if (!queryParams) {
            return new UserAchievementsRes(totalNumItems: 0, items: [])
        } else {
            Stream<UserAchievedLevelRepo.AchievementItem> achievements = userAchievedRepo.findAllForAchievementNavigator(
                    queryParams.projectId, queryParams.usernameFilter, queryParams.from, queryParams.to,
                    queryParams.skillNameFilter, queryParams.minLevel, queryParams.achievementTypesWithoutOverall,
                    queryParams.allNonOverallTypes, queryParams.includeOverallType, usersTableAdditionalUserTagKey, queryParams.pageRequest)
            try {
                List items = achievements.collect(Collectors.toList()).collect {
                    return buildMetricUserAchievement(it)
                }
                return new UserAchievementsRes(totalNumItems: queryParams.totalNumItems, items: items)
            } finally {
                achievements.close()
            }
        }
    }

    QueryParams getQueryParams(String projectId, String chartId, Map<String, String> props, Boolean validatePageSize = true) {
        PageRequest pageRequest = getPageRequest(projectId, chartId, props, validatePageSize)

        String usernameFilter = MetricsParams.getUsernameFilter(projectId, chartId, props, true)
        Date from = MetricsParams.getFromDayFilter(projectId, chartId, props)
        Date to = MetricsParams.getToDayFilter(projectId, chartId, props)

        String skillNameFilter = MetricsParams.getNameFilter(projectId, chartId, props)
        Integer minLevel = MetricsParams.getMinLevel(projectId, chartId, props)

        List<String> achievementTypes = MetricsParams.getAchievementTypes(projectId, chartId, props)
        if (!achievementTypes) {
            return null
        }

        List<SkillDef.ContainerType> achievementTypesWithoutOverall = achievementTypes.findAll({ !it.equalsIgnoreCase(MetricsParams.ACHIEVEMENT_TYPE_OVERALL) }).collect { SkillDef.ContainerType.valueOf(it) }
        String allNonOverallTypes = achievementTypesWithoutOverall.size() < 3 ? "false" : "true"
        String includeOverallType = achievementTypes.contains(MetricsParams.ACHIEVEMENT_TYPE_OVERALL) ? "true" : "false"

        int totalNumItems = userAchievedRepo.countForAchievementNavigator(
                projectId, usernameFilter, from, to, skillNameFilter, minLevel, achievementTypesWithoutOverall, allNonOverallTypes, includeOverallType)
        if (totalNumItems == 0) {
            return null
        }
        return new QueryParams(projectId, usernameFilter, from, to, skillNameFilter, minLevel, achievementTypesWithoutOverall, allNonOverallTypes, includeOverallType, totalNumItems, pageRequest)
    }

    @Canonical
    static class QueryParams {
        String projectId
        String usernameFilter
        Date from
        Date to
        String skillNameFilter
        Integer minLevel
        List<SkillDef.ContainerType> achievementTypesWithoutOverall
        String allNonOverallTypes
        String includeOverallType
        Integer totalNumItems
        PageRequest pageRequest
    }

    static PageRequest getPageRequest(String projectId, String chartId, Map<String, String> props, Boolean validatePageSize) {
        MetricsPagingParamsHelper metricsPagingParamsHelper = new MetricsPagingParamsHelper(projectId, chartId, props, validatePageSize)
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
            Sort sort = JpaSort.unsafe(sortDesc ?  DESC : ASC, "(uAttrs.userIdForDisplay)", "type")
            return PageRequest.of(currentPage, pageSize, sort)
        }
        return PageRequest.of(currentPage, pageSize, sortDesc ?  DESC : ASC, sortBy, "type")
    }

    static MetricUserAchievement buildMetricUserAchievement(UserAchievedLevelRepo.AchievementItem achievementItem) {
        MetricUserAchievement res = new MetricUserAchievement(
                achievedOn: achievementItem.achievedOn.time,
                userId: achievementItem.userId,
                userName: achievementItem.userIdForDisplay,
                name: "Overall",
                type: "Overall",
                level: achievementItem.level,
                skillId: achievementItem.skillId,
        )

        if (achievementItem.skillId) {
            res.name = InputSanitizer.unsanitizeName(achievementItem.name)
            res.type = achievementItem.type.toString()
            res.level = achievementItem.level
        }

        return res
    }
}
