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

import callStack.profiler.Profile
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import skills.controller.result.model.LabelCountItem
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams
import skills.metrics.builders.ProjectMetricsBuilder
import skills.storage.repos.UserTagRepo
import skills.utils.TimeRangeFormatterUtil

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@Component
class NumberUsersPerTagMetricsBuilder implements ProjectMetricsBuilder {

    @Autowired
    UserTagRepo userTagRepo

    @Override
    String getId() {
        return "numUsersPerTagBuilder"
    }

    @PostConstruct
    void init() {
        assertSqlStringsMatch()
    }

    static class UsersPerTagRes {
        Integer totalNumItems
        List<LabelCountItem> items
    }

    @Override
    def build(String projectId, String chartId, Map<String, String> props) {
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

        return getUserCountsForTag(projectId, tagKey, dates[0], dates[1], currentPage, pageSize, sortDesc, sortBy, tagFilter)
    }

    @Profile
    private UsersPerTagRes getUserCountsForTag(String projectId, String tagKey, Date startDate, Date endDate, int currentPage, int pageSize, Boolean sortDesc,  String sortBy, String tagFilter) {
        PageRequest pageRequest = PageRequest.of(currentPage, pageSize, sortDesc ? DESC : ASC, sortBy)
        List<UserTagRepo.UserTagCount> userTagCounts = findDistinctUserIdByProjectIdAndUserTag(projectId, tagKey, tagFilter, startDate, endDate, pageRequest)
        Integer numDistinctTags = (pageSize > userTagCounts.size() && currentPage == 0) ? userTagCounts.size() : countDistinctUserTag(projectId, tagKey, tagFilter, startDate, endDate)

        List<LabelCountItem> items = userTagCounts.collect {
            new LabelCountItem(value: it.tag, count: it.numUsers)
        }

        return new UsersPerTagRes(totalNumItems: numDistinctTags, items: items)
    }

    @Profile
    private List<UserTagRepo.UserTagCount> findDistinctUserIdByProjectIdAndUserTag(String projectId, String tagKey, String tagFilter, Date startDate, Date endDate, Pageable pageable) {
        List<UserTagRepo.UserTagCount> userTagCounts = userTagRepo.findDistinctUserIdByProjectIdAndUserTag(projectId, tagKey, tagFilter ?: null, startDate, endDate, pageable)
        return userTagCounts
    }

    @Profile
    private Integer countDistinctUserTag(String projectId, String tagKey, String tagFilter, Date startDate, Date endDate) {
        return userTagRepo.countDistinctUserTag(projectId, tagKey, tagFilter ?: null, startDate, endDate)
    }


    private static void assertSqlStringsMatch() {
        String selectCte = UserTagRepo.SELECT_DISTINCT_USER_BY_TAG_SQL.substring(0, UserTagRepo.SELECT_DISTINCT_USER_BY_TAG_SQL.toLowerCase().lastIndexOf('select')).trim()
        String countCte = UserTagRepo.COUNT_DISTINCT_USER_BY_TAG_SQL.substring(0, UserTagRepo.COUNT_DISTINCT_USER_BY_TAG_SQL.toLowerCase().lastIndexOf('select')).trim()

        if (selectCte != countCte) {
            throw new IllegalStateException("Select and count SQL strings must match except for last select!")
        }
    }
}
