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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.metrics.GlobalProgressMetricsService
import skills.metrics.GlobalProgressMetricsService.GroupingType
import skills.services.StartDateUtil
import skills.services.UserEventService
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

    @Override
    String getId() {
        return BUILDER_ID
    }

    @Override
    Object build(Map<String, String> props) {
        Date startDate = MetricsParams.getStart(null, BUILDER_ID, props)
        startDate = LocalDateTime.of(startDate.toLocalDate(), LocalTime.MIN).toDate()
        EventType eventType = userEventService.determineAppropriateEventType(startDate)
        Boolean byMonth = props.containsKey(MetricsParams.P_BY_MONTH) ? MetricsParams.getByMonth(props) : false

        if (EventType.WEEKLY == eventType) {
            // set the start date to the nearest sunday that encapsulates the provided startDate
            startDate = StartDateUtil.computeStartDate(startDate, eventType)
        }

        GroupingType groupingType = GroupingType.DAY
        if (byMonth) {
            groupingType = GroupingType.MONTH
        } else if (EventType.WEEKLY == eventType) {
            // set the start date to the nearest sunday that encapsulates the provided startDate
            startDate = StartDateUtil.computeStartDate(startDate, eventType)
            groupingType = GroupingType.WEEK
        }
        List<DayCountItem> dayCounts = globalProgressMetricsService.getDistinctUserCountForProjectsAndQuizzes(startDate, groupingType)
        List<TimestampCountItem> counts = dayCounts.collect { new TimestampCountItem(value: it.day.time, count: it.count) }

        return [ users: counts ]
    }
}
