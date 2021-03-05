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
import skills.storage.model.DayCountItem
import skills.storage.model.EventType

@Component
@Slf4j
class AllProjectsSkillEventsOverTimeMetricsBuilder implements GlobalMetricsBuilder {

    static final String BUILDER_ID = 'allProjectsSkillEventsOverTimeMetricsBuilder'

    @Autowired
    UserEventService userEventService

    @Autowired
    private UserInfoService userInfoService;

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
        String userId = userInfoService.getCurrentUserId();
        Date startDate = MetricsParams.getStart(null, BUILDER_ID, props)
        EventType eventType = userEventService.determineAppropriateEventType(startDate)

        if (EventType.WEEKLY == eventType) {
            // set the start date to the nearest sunday that encapsulates the provided startDate
            startDate = StartDateUtil.computeStartDate(startDate, eventType)
        }

        List<String> projectIds = MetricsParams.getProjectIds(BUILDER_ID, props)
        log.debug("Retrieving event counts for user [{}], start date [{}], projectIds [{}]", userId, startDate, projectIds)

        List<ProjResCount> projResCounts = []
        List<DayCountItem> counts = userEventService.getUserEventCountsForUser(userId, startDate, projectIds)

        Map<String, DayCountItem> byProject = counts.groupBy {it.projectId }
        byProject.each {projectId, countsForProject ->
            List<ResCount> countsByDay = countsForProject.collect {
                new ResCount(num: it.getCount(), timestamp: it.getDay().time)
            }?.sort({it.timestamp})
            projResCounts += new ProjResCount(project: projectId, countsByDay: countsByDay)
        }
        return projResCounts
    }
}
