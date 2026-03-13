/**
 * Copyright 2026 SkillTree
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


import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.metrics.GlobalProgressMetricsService
import skills.storage.repos.GlobalProgressMetricsRepo
import skills.utils.TimeRangeFormatterUtil

@Component
@Slf4j
class OverallNumberUsersPerTagMetricsBuilder implements GlobalMetricsBuilder {

    static final String BUILDER_ID = 'overallNumUsersPerTagBuilder'

    @Autowired
    GlobalProgressMetricsRepo globalProgressMetricsRepo

    @Autowired
    GlobalProgressMetricsService globalProgressMetricsService

    @Override
    String getId() {
        return BUILDER_ID
    }

    @PostConstruct
    void init() {
        assertSqlStringsMatch()
    }

    @Override
    Object build(Map<String, String> props) {
        String projectId = null
        String chartId = BUILDER_ID
        MetricsPagingParamsHelper metricsPagingParamsHelper = new MetricsPagingParamsHelper(projectId, chartId, props)
        int currentPage = metricsPagingParamsHelper.currentPage
        int pageSize = metricsPagingParamsHelper.pageSize
        Boolean sortDesc = metricsPagingParamsHelper.sortDesc
        String sortBy = metricsPagingParamsHelper.getSortBy(true, 'numUsers')

        String tagKey = MetricsParams.getTagKey(projectId, chartId, props)
        String tagFilter = MetricsParams.getTagFilter(projectId, chartId, props)
        String startDate = MetricsParams.getFromDayFilterAsString(projectId, chartId, props)
        String endDate = MetricsParams.getToDayFilterAsString(projectId, chartId, props)
        List<Date> dates = TimeRangeFormatterUtil.formatTimeRange(startDate, endDate)

        return globalProgressMetricsService.getUserCountsForTag(tagKey, dates[0], dates[1], currentPage, pageSize, sortDesc, sortBy, tagFilter)
    }

    private static void assertSqlStringsMatch() {
        String selectCte = GlobalProgressMetricsRepo.SELECT_DISTINCT_USER_BY_TAG_SQL.substring(0, GlobalProgressMetricsRepo.SELECT_DISTINCT_USER_BY_TAG_SQL.toLowerCase().lastIndexOf('select')).trim()
        String countCte = GlobalProgressMetricsRepo.COUNT_DISTINCT_USER_BY_TAG_SQL.substring(0, GlobalProgressMetricsRepo.COUNT_DISTINCT_USER_BY_TAG_SQL.toLowerCase().lastIndexOf('select')).trim()

        if (selectCte != countCte) {
            throw new IllegalStateException("Select and count SQL strings must match except for last select!")
        }
    }
}
