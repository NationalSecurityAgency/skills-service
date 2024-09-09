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

import groovy.transform.CompileStatic
import skills.controller.exceptions.SkillException

@CompileStatic
class MetricsPagingParamsHelper {
    static String PROP_PAGE_SIZE = "pageSize"
    static String PROP_CURRENT_PAGE = "currentPage"
    static String PROP_SORT_BY = "sortBy"
    static String PROP_SORT_DESC = "sortDesc"

    private String projectId
    private String chartId
    private Map<String,String> props
    Boolean validatePageSize
    MetricsPagingParamsHelper(String projectId, String chartId, Map<String,String> props, Boolean validatePageSize = true) {
        this.projectId = projectId
        this.chartId = chartId
        this.props = props
        this.validatePageSize = validatePageSize
    }

    int getPageSize() {
        String strPage = MetricsParams.getParam(props, PROP_PAGE_SIZE, chartId, projectId)
        int pageSize = Integer.valueOf(strPage)
        if (validatePageSize && pageSize > 100) {
            throw new SkillException("Metrics[${chartId}]: page size must not exceed 100. Provided [${pageSize}]", projectId)
        }
        if (validatePageSize && pageSize < 1) {
            throw new SkillException("Metrics[${chartId}]: page size must not be less than 1. Provided [${pageSize}]", projectId)
        }

        return pageSize
    }

    int getCurrentPage() {
        String strPage = MetricsParams.getParam(props, PROP_CURRENT_PAGE, chartId, projectId)
        // client's page starts 1, dbs at 0
        int currentPage = Integer.valueOf(strPage) - 1
        if (currentPage < 0) {
            throw new SkillException("Metrics[${chartId}]: current page must be >= 1. Provided [${strPage}]", projectId)
        }

        return currentPage
    }

    String getSortBy(boolean optional = false, String defaultValue = '') {
        String sortBy = props[PROP_SORT_BY]
        if (!optional && !sortBy) {
            throw new SkillException("Metrics[${chartId}]: Must supply ${PROP_SORT_BY} param", projectId)
        }

        if (optional && !sortBy) {
            sortBy = defaultValue
        }

        return sortBy
    }

    Boolean getSortDesc() {
        String sortDescStr = props[PROP_SORT_DESC]
        if (!sortDescStr || !(sortDescStr.equals("true") || sortDescStr.equals("false"))) {
            throw new SkillException("Metrics[${chartId}]: Must supply ${PROP_SORT_DESC} param with either 'true' or 'false' value", projectId)
        }

        Boolean sortDesc = Boolean.valueOf(sortDescStr)
        return sortDesc
    }
}
