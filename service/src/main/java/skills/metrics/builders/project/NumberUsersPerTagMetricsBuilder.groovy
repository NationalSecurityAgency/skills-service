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
import org.springframework.stereotype.Component
import skills.controller.result.model.LabelCountItem
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams
import skills.metrics.builders.ProjectMetricsBuilder
import skills.storage.repos.UserTagRepo

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

        return getUserCountsForTag(projectId, tagKey, currentPage, pageSize, sortDesc, sortBy, tagFilter)
    }


    private UsersPerTagRes getUserCountsForTag(String projectId, String tagKey, int currentPage, int pageSize, Boolean sortDesc,  String sortBy, String tagFilter) {
        PageRequest pageRequest = PageRequest.of(currentPage, pageSize, sortDesc ? DESC : ASC, sortBy)
        List<UserTagRepo.UserTagCount> userTagCounts = userTagRepo.findDistinctUserIdByProjectIdAndUserTag(projectId, tagKey, tagFilter, pageRequest)

        Integer numDistinctTags = (pageSize > userTagCounts.size() && currentPage == 1) ? userTagCounts.size() : userTagRepo.countDistinctUserTag(projectId, tagKey, tagFilter)

        List<LabelCountItem> items = userTagCounts.collect{
            new LabelCountItem(value: it.tag, count: it.numUsers)
        }

        return new UsersPerTagRes(totalNumItems: numDistinctTags, items: items)
    }
}
