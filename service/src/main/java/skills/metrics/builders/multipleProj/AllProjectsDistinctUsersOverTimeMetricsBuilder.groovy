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
package skills.metrics.builders.multipleProj

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.auth.UserInfoService
import skills.metrics.builders.GlobalMetricsBuilder
import skills.metrics.builders.MetricsParams
import skills.services.StartDateUtil
import skills.services.UserEventService
import skills.services.admin.UserCommunityService
import skills.storage.model.DayCountItem
import skills.storage.model.EventType
import skills.storage.model.MonthlyCountItem
import skills.storage.model.WeekCountItem
import skills.storage.repos.GlobalProgressMetricsRepo

import java.time.LocalDateTime
import java.time.LocalTime
import java.util.stream.Stream

@Component
@Slf4j
class AllProjectsDistinctUsersOverTimeMetricsBuilder implements GlobalMetricsBuilder {

    // Note: overallDistinctUsersOverTimeMetricsBuilder if we decide to combine projects and quizzes
    static final String BUILDER_ID = 'allProjectsDistinctUsersOverTimeMetricsBuilder'

    @Autowired
    UserEventService userEventService

    @Autowired
    GlobalProgressMetricsRepo globalProgressMetricsRepo

    @Autowired
    private UserInfoService userInfoService

    @Autowired
    UserCommunityService userCommunityService

    static class ResCount {
        Integer num
        Long timestamp
    }

    static class ProjResCount {
        String project
        List<ResCount> countsByDay
    }

    static class FinalRes {
        List<ProjResCount> countsByDay = []
    }

    @Override
    String getId() {
        return BUILDER_ID
    }

    @Override
    List<ProjResCount> build(Map<String, String> props) {
        String userId = userInfoService.getCurrentUserId()
        Date startDate = MetricsParams.getStart(null, BUILDER_ID, props)
        startDate = LocalDateTime.of(startDate.toLocalDate(), LocalTime.MIN).toDate()
        EventType eventType = userEventService.determineAppropriateEventType(startDate)
        Boolean byMonth = props.containsKey(MetricsParams.P_BY_MONTH) ? MetricsParams.getByMonth(props) : false

        if (EventType.WEEKLY == eventType) {
            // set the start date to the nearest sunday that encapsulates the provided startDate
            startDate = StartDateUtil.computeStartDate(startDate, eventType)
        }

        List<String> projectIds = MetricsParams.getProjectIds(BUILDER_ID, props)

        if (!userCommunityService.isUserCommunityMember(userId)) {
            projectIds.removeAll { userCommunityService.isUserCommunityOnlyProject(it) }
        }
        log.debug("Retrieving event counts for user [{}], start date [{}], projectIds [{}]", userId, startDate, projectIds)

        List<ProjResCount> projResCounts = []
        if (byMonth) {
            Stream<MonthlyCountItem> stream = globalProgressMetricsRepo.getDistinctUserCountForProjectsGroupedByMonth(projectIds, startDate)
            List<MonthlyCountItem> counts = userEventService.convertMonthlyResults(stream, startDate)
            Map<String, MonthlyCountItem> byProject = counts.groupBy {it.projectId }
            byProject.each {projectId, countsForProject ->
                List<ResCount> countsByDate = countsForProject.collect {
                    new ResCount(num: it.getCount(), timestamp: it.getMonth().time)
                }?.sort({it.timestamp})
                projResCounts += new ProjResCount(project: projectId, countsByDay: countsByDate)
            }
        } else {
            List<DayCountItem> counts
            if (EventType.DAILY == eventType) {
                Stream<DayCountItem> stream = globalProgressMetricsRepo.getDistinctUserCountForProjects(projectIds, startDate, eventType)
                counts = userEventService.convertResults(stream, eventType, startDate, projectIds)
            } else {
                Stream<WeekCountItem> stream = globalProgressMetricsRepo.getDistinctUserCountForProjectsGroupedByWeek(projectIds, startDate)
                counts = userEventService.convertResults(stream, startDate)
            }
            Map<String, DayCountItem> byProject = counts.groupBy {it.projectId }
            byProject.each {projectId, countsForProject ->
                List<ResCount> countsByDay = countsForProject.collect {
                    new ResCount(num: it.getCount(), timestamp: it.getDay().time)
                }?.sort({it.timestamp})
                projResCounts += new ProjResCount(project: projectId, countsByDay: countsByDay)
            }
        }

        return projResCounts
    }
}
