/**
 *
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
package skills.metrics.builders.multipleProj

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import skills.auth.UserInfoService
import skills.controller.exceptions.SkillException
import skills.controller.result.model.LabelCountItem
import skills.metrics.GlobalProgressMetricsService
import skills.metrics.builders.GlobalMetricsBuilder
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams
import skills.services.admin.UserCommunityService
import skills.storage.repos.GlobalProgressMetricsRepo
import skills.utils.TimeRangeFormatterUtil

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@Component
@Slf4j
class OverallNumberUsersPerTagMetricsBuilder implements GlobalMetricsBuilder {

    static final String BUILDER_ID = 'overallNumUsersPerTagBuilder'

    @Autowired
    GlobalProgressMetricsRepo globalProgressMetricsRepo

    @Autowired
    GlobalProgressMetricsService globalProgressMetricsService

    @Autowired
    private UserInfoService userInfoService

    @Autowired
    UserCommunityService userCommunityService


    @Override
    String getId() {
        return BUILDER_ID
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
    Object build(Map<String, String> props) {
        String userId = userInfoService.getCurrentUserId()
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

        List<String> projectIds = MetricsParams.getProjectIds(BUILDER_ID, props, true)
        projectIds?.removeAll { !it }
        List<String> quizIds = MetricsParams.getQuizIds(BUILDER_ID, props, true)
        quizIds?.removeAll { !it }
        if (!projectIds && !quizIds) {
            throw new SkillException("Metrics[${BUILDER_ID}]: Must supply ${MetricsParams.P_PROJECT_IDS} or ${MetricsParams.P_QUIZ_IDS} param")
        }

        Boolean isUserCommunityMember = userCommunityService.isUserCommunityMember(userId)
        if (!isUserCommunityMember && projectIds) {
            projectIds.removeAll { userCommunityService.isUserCommunityOnlyProject(it) }
        }
        if (!isUserCommunityMember && quizIds) {
            quizIds.removeAll { userCommunityService.isUserCommunityOnlyQuiz(it) }
        }
        log.debug("Retrieving event counts for user [{}], start date [{}], projectIds [{}], quizIds [{}]", userId, startDate, projectIds, quizIds)

        return getUserCountsForTag(projectIds, quizIds, tagKey, dates[0], dates[1], currentPage, pageSize, sortDesc, sortBy, tagFilter)
    }

    @Profile
    private UsersPerTagRes getUserCountsForTag(List<String> projectIds, List<String> quizIds, String tagKey, Date startDate, Date endDate, int currentPage, int pageSize, Boolean sortDesc, String sortBy, String tagFilter) {
        PageRequest pageRequest = PageRequest.of(currentPage, pageSize, sortDesc ? DESC : ASC, sortBy)
        List<GlobalProgressMetricsRepo.UserTagCount> userTagCounts = findDistinctUserIdByProjectIdAndUserTag(projectIds, quizIds, tagKey, tagFilter, startDate, endDate, pageRequest)
        Integer numDistinctTags = (pageSize > userTagCounts.size() && currentPage == 0) ? userTagCounts.size() : countDistinctUserTag(projectIds, quizIds, tagKey, tagFilter, startDate, endDate)

        List<LabelCountItem> items = userTagCounts.collect {
            new LabelCountItem(value: it.tag, count: it.numUsers)
        }

        return new UsersPerTagRes(totalNumItems: numDistinctTags, items: items)
    }

    @Profile
    private List<GlobalProgressMetricsRepo.UserTagCount> findDistinctUserIdByProjectIdAndUserTag(List<String> projectIds, List<String> quizIds, String tagKey, String tagFilter, Date startDate, Date endDate, Pageable pageable) {
        List<GlobalProgressMetricsRepo.UserTagCount> userTagCounts = globalProgressMetricsRepo.findUserTagCountByProjectIdInAndUserTagFilter(projectIds, quizIds, tagKey, tagFilter ?: null, startDate, endDate, pageable)
        return userTagCounts
    }

    @Profile
    private Integer countDistinctUserTag(List<String> projectIds, List<String> quizIds, String tagKey, String tagFilter, Date startDate, Date endDate) {
        return globalProgressMetricsRepo.countUserTagCountByProjectIdInAndUserTagFilter(projectIds, quizIds, tagKey, tagFilter ?: null, startDate, endDate)
    }


    private static void assertSqlStringsMatch() {
        String selectCte = GlobalProgressMetricsRepo.SELECT_DISTINCT_USER_BY_TAG_SQL.substring(0, GlobalProgressMetricsRepo.SELECT_DISTINCT_USER_BY_TAG_SQL.toLowerCase().lastIndexOf('select')).trim()
        String countCte = GlobalProgressMetricsRepo.COUNT_DISTINCT_USER_BY_TAG_SQL.substring(0, GlobalProgressMetricsRepo.COUNT_DISTINCT_USER_BY_TAG_SQL.toLowerCase().lastIndexOf('select')).trim()

        if (selectCte != countCte) {
            throw new IllegalStateException("Select and count SQL strings must match except for last select!")
        }
    }
}
