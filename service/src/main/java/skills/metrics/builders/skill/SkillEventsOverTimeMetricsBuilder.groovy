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
package skills.metrics.builders.skill

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.metrics.builders.MetricsParams
import skills.metrics.builders.ProjectMetricsBuilder
import skills.services.MetricCompactionUtil
import skills.services.StartDateUtil
import skills.services.UserEventService
import skills.storage.model.DayCountItem
import skills.storage.model.EventType
import skills.storage.repos.UserPerformedSkillRepo

import static skills.services.ZeroFillDayCountItemUtil.zeroFillDailyGaps
import static skills.services.ZeroFillDayCountItemUtil.zeroFillWeeklyGaps

@Component
@Slf4j
class SkillEventsOverTimeMetricsBuilder implements ProjectMetricsBuilder {

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    UserEventService eventService

    static class ResCount {
        Integer num
        Long timestamp
    }

    static class FinalRes {
        List<ResCount> countsByDay
        List<ResCount> allEvents
    }

    @Override
    String getId() {
        return "skillEventsOverTimeChartBuilder"
    }

    @CompileStatic
    FinalRes build(String projectId, String chartId, Map<String, String> props) {
        String skillId = MetricsParams.getSkillId(projectId, chartId, props);
        Date start = MetricsParams.getStart(projectId, chartId, props)
        List<DayCountItem> counts = userPerformedSkillRepo.countsByDay(projectId, skillId, start)
        counts = counts.sort{it.day}
        List<DayCountItem> allCounts = eventService.getUserEventCountsForSkillId(projectId, skillId, start)

        EventType eventType = eventService.determineAppropriateEventType(start)
        if (EventType.WEEKLY == eventType) {
            //we need to coerce the applied skill events into the same granularity as the all skill events
            counts = MetricCompactionUtil.manuallyCompactDaily(counts)
        }

        start = StartDateUtil.computeStartDate(start, eventType)
        Date nMinus = start
        List<DayCountItem> filled = []
        counts.eachWithIndex { DayCountItem entry, int i ->
            List<DayCountItem> fills = eventType == EventType.DAILY ? zeroFillDailyGaps(entry.day, nMinus, false) : zeroFillWeeklyGaps(entry.day, nMinus, false)
            if (fills) {
                filled.addAll(fills)
            }
            filled.add(entry)
            nMinus = entry.day
        }

        Date now = StartDateUtil.computeStartDate(new Date(), eventType)
        List<DayCountItem> fillToNow = null
        if (filled) {
            eventType == EventType.DAILY ? zeroFillDailyGaps(now, nMinus, filled?.last()?.day != now) : zeroFillWeeklyGaps(now, nMinus, filled.last().day != now)
        }

        if (fillToNow) {
            filled.addAll(fillToNow)
        }

        List<ResCount> countsByDay = filled.collect {
            new ResCount(num: it.getCount().toInteger(), timestamp: it.getDay().time)
        }
        countsByDay = countsByDay?.sort({it.timestamp})

        List<ResCount> allCountsByDay = allCounts.collect { new ResCount(num: it.getCount().toInteger(), timestamp: it.getDay().time)}
        allCountsByDay = allCountsByDay.sort({it.timestamp})

        return new FinalRes(countsByDay: countsByDay, allEvents: allCountsByDay)
    }
}
