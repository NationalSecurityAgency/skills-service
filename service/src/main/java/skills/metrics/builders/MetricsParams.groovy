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
package skills.metrics.builders

import groovy.time.TimeCategory
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.PageRequest
import skills.controller.exceptions.SkillException

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@CompileStatic
class MetricsParams {
    public static final String P_SKILL_ID = "skillId"
    public static final String P_SUBJECT_ID = "subjectId"
    public static final String P_PROJECT_IDS = "projIds"
    public static final String P_PROJECT_IDS_AND_LEVEL = "projIdsAndLevel"
    public static final String P_START_TIMESTAMP = "start"
    public static final String P_ACHIEVEMENT_TYPES = "achievementTypes"
    public static final String P_USERNAME_FILTER = "usernameFilter"
    public static final String P_NAME_FILTER = "nameFilter"
    public static final String P_MIN_LEVEL = "minLevel"
    public static final String P_FROM_DAY_FILTER = "fromDayFilter"
    public static final String P_TO_DAY_FILTER = "toDayFilter"
    public static final String P_TAG_KEY = "tagKey"
    public static final DateFormat DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd")

    static String ACHIEVEMENT_TYPE_OVERALL = "Overall"

    static String getSkillId(String projectId, String chartId, Map<String, String> props) {
        return getParam(props, P_SKILL_ID, chartId, projectId)
    }

    static String getSubjectId(String projectId, String chartId, Map<String, String> props) {
        return getParam(props, P_SUBJECT_ID, chartId, projectId)
    }

    static String getTagKey(String projectId, String chartId, Map<String, String> props) {
        return getParam(props, P_TAG_KEY, chartId, projectId)
    }

    static String getUsernameFilter(String projectId, String chartId, Map<String, String> props, boolean isOptional = false) {
        return getParam(props, P_USERNAME_FILTER, chartId, projectId, isOptional)
    }

    static Date getFromDayFilter(String projectId, String chartId, Map<String, String> props) {
        String fromStr = getParam(props, P_FROM_DAY_FILTER, chartId, projectId, true)
        return fromStr ? LocalDate.parse(fromStr).toDate() : new Date(1)
    }

    static Date getToDayFilter(String projectId, String chartId, Map<String, String> props) {
        String fromStr = getParam(props, P_TO_DAY_FILTER, chartId, projectId, true)
        Date res
        if (fromStr) {
            res = LocalDate.parse(fromStr).atTime(LocalTime.MAX).toDate()
        } else {
            res = LocalDate.now().plusDays(900).toDate()
        }
        return res
    }

    static String getNameFilter(String projectId, String chartId, Map<String, String> props) {
        return getParam(props, P_NAME_FILTER, chartId, projectId, true) ?: "ALL"
    }

    static Integer getMinLevel(String projectId, String chartId, Map<String, String> props) {
        String resStr = getParam(props, P_MIN_LEVEL, chartId, projectId, true)
        return resStr ? Integer.parseInt(resStr) : -1
    }

    static List<String> getAchievementTypes(String projectId, String chartId, Map<String, String> props) {
        String achievementTypesStr = props[P_ACHIEVEMENT_TYPES]
        if (StringUtils.isEmpty(achievementTypesStr)) {
            return Collections.emptyList()
        }
        return achievementTypesStr.split(",").toList()
    }

    static List<String> getProjectIds(String chartId, Map<String, String> props) {
        String listStr = getParam(props, P_PROJECT_IDS, chartId)
        return listStr.split(",").toList()
    }

    static Date getStart(String projectId, String chartId, Map<String, String> props) {
        String tStr = getParam(props, P_START_TIMESTAMP, chartId, projectId)
        Long timestamp = Long.valueOf(tStr)
        int numYears = 3
        Date threeYearsAgo = LocalDateTime.now().minusYears(3).toDate()
        Date date = new Date(timestamp)
        if (date.before(threeYearsAgo)) {
            throw new SkillException("Metrics[${chartId}]: ${P_START_TIMESTAMP} param timestamp must be within last ${numYears} years. Provided: [${date}]", projectId)
        }
        return date
    }

    static String getParam(Map<String, String> props, String paramId, String chartId, String projectId = null, boolean isOptional = false) {
        String param = props[paramId]
        if (!param) {
            if (isOptional) {
                param = ""
            } else {
                throw new SkillException("Metrics[${chartId}]: Must supply ${paramId} param", projectId)
            }
        }
        return param
    }

    static PageRequest getPageRequest(String projectId, String chartId, Map<String, String> props, List<String> supportedSortFields) {
        if (!supportedSortFields.contains(props[MetricsPagingParamsHelper.PROP_SORT_BY])) {
            throw new SkillException("Metrics[${chartId}]: Invalid value [${props[MetricsPagingParamsHelper.PROP_SORT_BY]}] for [${MetricsPagingParamsHelper.PROP_SORT_BY}] property. Suppored values are ${supportedSortFields}", projectId)
        }

        MetricsPagingParamsHelper metricsPagingParamsHelper = new MetricsPagingParamsHelper(projectId, chartId, props)
        int currentPage = metricsPagingParamsHelper.currentPage
        int pageSize = metricsPagingParamsHelper.pageSize
        Boolean sortDesc = metricsPagingParamsHelper.sortDesc
        String sortBy = metricsPagingParamsHelper.sortBy
        return PageRequest.of(currentPage, pageSize, sortDesc ? DESC : ASC, sortBy)
    }
}
