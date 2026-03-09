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
package skills.metrics.builders.multipleProj

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.auth.UserInfoService
import skills.controller.exceptions.SkillException
import skills.metrics.GlobalProgressMetricsService
import skills.metrics.GlobalProgressMetricsService.GroupingType
import skills.metrics.builders.GlobalMetricsBuilder
import skills.metrics.builders.MetricsParams
import skills.services.StartDateUtil
import skills.services.UserEventService
import skills.services.admin.UserCommunityService
import skills.controller.result.model.TimestampCountItem
import skills.storage.model.DayCountItem
import skills.storage.model.EventType
import skills.storage.repos.GlobalProgressMetricsRepo

import java.time.LocalDateTime
import java.time.LocalTime

@Component
@Slf4j
class OverallDistinctUsersOverTimeMetricsBuilder implements GlobalMetricsBuilder {

    static final String BUILDER_ID = 'overallDistinctUsersOverTimeMetricsBuilder'

    @Autowired
    UserEventService userEventService

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

    @Override
    Object build(Map<String, String> props) {
        String userId = userInfoService.getCurrentUserId()
        Date startDate = MetricsParams.getStart(null, BUILDER_ID, props)
        startDate = LocalDateTime.of(startDate.toLocalDate(), LocalTime.MIN).toDate()
        EventType eventType = userEventService.determineAppropriateEventType(startDate)
        Boolean byMonth = props.containsKey(MetricsParams.P_BY_MONTH) ? MetricsParams.getByMonth(props) : false

        if (EventType.WEEKLY == eventType) {
            // set the start date to the nearest sunday that encapsulates the provided startDate
            startDate = StartDateUtil.computeStartDate(startDate, eventType)
        }

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

        GroupingType groupingType = GroupingType.DAY
        if (byMonth) {
            groupingType = GroupingType.MONTH
        } else if (EventType.WEEKLY == eventType) {
            // set the start date to the nearest sunday that encapsulates the provided startDate
            startDate = StartDateUtil.computeStartDate(startDate, eventType)
            groupingType = GroupingType.WEEK
        }
        List<DayCountItem> dayCounts = globalProgressMetricsService.getDistinctUserCountForProjectsAndQuizzes(projectIds, quizIds, startDate, groupingType)
        List<TimestampCountItem> counts = dayCounts.collect { new TimestampCountItem(value: it.day.time, count: it.count) }

        return [ users: counts ]
    }
}
