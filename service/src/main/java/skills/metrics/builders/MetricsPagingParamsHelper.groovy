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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import skills.controller.exceptions.ErrorCode
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
        int pageSize = parseIntegerParam(PROP_PAGE_SIZE, strPage)
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
        int currentPage = parseIntegerParam(PROP_CURRENT_PAGE, strPage) - 1
        if (currentPage < 0) {
            throw new SkillException("Metrics[${chartId}]: current page must be >= 1. Provided [${strPage}]", projectId)
        }

        return currentPage
    }

    private int parseIntegerParam(String paramId, String value) {
        try {
            return Integer.valueOf(value)
        } catch (NumberFormatException ignored) {
            throw new SkillException(
                    "Metrics[${chartId}]: ${paramId} must be a valid integer. Provided [${value}]",
                    projectId,
                    SkillException.NA,
                    ErrorCode.BadParam)
        }
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

    /**
     * Builds the PageRequest for the metrics builders that are driven by the <code>page</code> (1 based),
     * <code>pageSize</code>, <code>sortBy</code> and <code>sortDesc</code> params. Invalid or missing values are
     * reported as a BadParam rather than escaping as a NumberFormatException, NullPointerException or the
     * IllegalArgumentException that PageRequest throws.
     */
    static PageRequest buildPageRequest(String projectId, String chartId, Map<String, ?> props) {
        int page = parseRequiredInt("page", props, chartId, projectId)
        if (page < 1) {
            throw badParam("Metrics[${chartId}]: page must be >= 1. Provided [${page}]", projectId)
        }
        int pageSize = parseRequiredInt("pageSize", props, chartId, projectId)
        if (pageSize < 1) {
            throw badParam("Metrics[${chartId}]: pageSize must be >= 1. Provided [${pageSize}]", projectId)
        }
        String sortBy = props["sortBy"]
        if (!sortBy) {
            throw new SkillException("Metrics[${chartId}]: Must supply sortBy param", projectId)
        }
        Object sortDesc = props["sortDesc"]
        if (sortDesc == null || sortDesc == "") {
            throw new SkillException("Metrics[${chartId}]: Must supply sortDesc param", projectId)
        }
        Sort.Direction direction = sortDesc.toString().toBoolean() ? Sort.Direction.DESC : Sort.Direction.ASC
        return PageRequest.of(page - 1, pageSize, direction, sortBy)
    }

    private static int parseRequiredInt(String paramId, Map<String, ?> props, String chartId, String projectId) {
        Object value = props[paramId]
        if (value == null || value == "") {
            throw new SkillException("Metrics[${chartId}]: Must supply ${paramId} param", projectId)
        }
        try {
            return Integer.valueOf(value.toString())
        } catch (NumberFormatException ignored) {
            throw badParam("Metrics[${chartId}]: ${paramId} must be a valid integer. Provided [${value}]", projectId)
        }
    }

    private static SkillException badParam(String msg, String projectId) {
        return new SkillException(msg, projectId, SkillException.NA, ErrorCode.BadParam)
    }
}
