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
package skills.metricsNew.builders

import groovy.time.TimeCategory
import groovy.transform.CompileStatic
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.JpaSort
import skills.controller.exceptions.SkillException

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC
import static org.springframework.data.domain.Sort.Direction.DESC

@CompileStatic
class MetricsParams {
    final static String P_SKILL_ID = "skillId"
    final static String P_SUBJECT_ID = "subjectId"
    final static String P_PROJECT_IDS = "projIds"
    final static String P_PROJECT_IDS_AND_LEVEL = "projIdsAndLevel"
    final static String P_START_TIMESTAMP = "start"

    static String getSkillId(String projectId, String chartId, Map<String, String> props) {
        return getParam(props, P_SKILL_ID, chartId, projectId)
    }

    static String getSubjectId(String projectId, String chartId, Map<String, String> props) {
        return getParam(props, P_SUBJECT_ID, chartId, projectId)
    }

    static List<String> getProjectIds(String chartId, Map<String, String> props) {
        String listStr = getParam(props, P_PROJECT_IDS, chartId)
        return listStr.split(",").toList()
    }

    static Date getStart(String projectId, String chartId, Map<String, String> props) {
        String tStr = getParam(props, P_START_TIMESTAMP, chartId, projectId)
        Long timestamp = Long.valueOf(tStr)
        Date threeYearsAgo = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 365 * 3))
        Date date = new Date(timestamp)
        if (date.before(threeYearsAgo)) {
            throw new SkillException("Chart[${chartId}]: ${P_START_TIMESTAMP} param timestamp must be within last 4 years", projectId)
        }
        return date
    }

    static String getParam(Map<String, String> props, String paramId, String chartId, String projectId = null) {
        String sortBy = props[paramId]
        if (!sortBy) {
            throw new SkillException("Chart[${chartId}]: Must supply ${paramId} param", projectId)
        }
        return sortBy
    }

    static PageRequest getPageRequest(String projectId, String chartId, Map<String, String> props, List<String> supportedSortFields) {
        if (!supportedSortFields.contains(props[MetricsPagingParamsHelper.PROP_SORT_BY])) {
            throw new SkillException("Chart[${chartId}]: Invalid value [${props[MetricsPagingParamsHelper.PROP_SORT_BY]}] for [${MetricsPagingParamsHelper.PROP_SORT_BY}] property. Suppored values are ${supportedSortFields}", projectId)
        }

        MetricsPagingParamsHelper metricsPagingParamsHelper = new MetricsPagingParamsHelper(projectId, chartId, props)
        int currentPage = metricsPagingParamsHelper.currentPage
        int pageSize = metricsPagingParamsHelper.pageSize
        Boolean sortDesc = metricsPagingParamsHelper.sortDesc
        String sortBy = metricsPagingParamsHelper.sortBy
        return PageRequest.of(currentPage, pageSize, sortDesc ?  DESC : ASC, sortBy)
    }
}
