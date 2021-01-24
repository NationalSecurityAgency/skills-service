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
package skills.services


import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.Validate
import org.apache.commons.lang3.time.StopWatch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.storage.model.*
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserEventsRepo
import skills.storage.repos.nativeSql.NativeQueriesRepo

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields
import java.util.stream.Stream

@Component
@Slf4j
class UserEventService {

    @Autowired
    NativeQueriesRepo nativeQueriesRepo

    @Autowired
    UserEventsRepo userEventsRepo

    @PersistenceContext
    EntityManager entityManager;

    @Value('#{"${skills.config.compactDailyEventsOlderThan:30}"}')
    int maxDailyDays = 30

    @Autowired
    SkillDefRepo skillDefRepo

    private static final List<SkillDef.ContainerType> ALLOWABLE_CONTAINER_TYPES = [SkillDef.ContainerType.Skill, SkillDef.ContainerType.Subject]

    /**
     * Returns the daily user interaction counts for a skill or subject whether the skill was applied or not.
     *
     * Note that if the start date exceeds the configured compactDailyEventsOlderThan, the DayCountItems are returned
     * aggregated by week (the day attributes will be separated by week with the daily count aggregated under that week)
     * where DayCountItem.start refers to the start of that week's compacted metrics (Sunday).
     *
     * @param projectId
     * @param skillId
     * @param start
     * @return
     */
    @Transactional(readOnly = true)
    List<DayCountItem> getUserEventCountsForSkillId(String projectId, String skillId, Date start) {
        EventType eventType = determineAppropriateEventType(start)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillId(projectId, skillId)
        if (!skillDef) {
            throw new SkillException("Skill does not exist", projectId, skillId, ErrorCode.SkillNotFound)
        }

        Validate.isTrue(ALLOWABLE_CONTAINER_TYPES.contains(skillDef.type), "Unsupported ContainerType [${skillDef.type}]")

        List<DayCountItem> results
        Integer rawId = skillDef.id
        if (EventType.DAILY == eventType) {
            Stream<DayCountItem> stream
            if (SkillDef.ContainerType.Skill == skillDef.type) {
                stream = userEventsRepo.getEventCountForSkill(rawId, start, eventType)
            } else {
                stream = userEventsRepo.getEventCountForSubject(rawId, start, eventType)
            }
            results = convertResults(stream, eventType)
        } else {
            Stream<WeekCount> stream
            if (SkillDef.ContainerType.Skill == skillDef.type) {
                stream = userEventsRepo.getEventCountForSkillGroupedByWeek(rawId, start)
            } else {
                stream = userEventsRepo.getEventCountForSubjectGroupedByWeek(rawId, start)
            }
            results = convertResults(stream)
        }

        return results
    }

    /**
     * Returns the distinct user count for the specified project by day, unless start is older than the compactDailyEventsOlderThan
     * setting, in which case results are grouped by week where DayCountItem.start refers to the start-of-week (Sunday) for that week's
     * distinct user counts.
     *
     * @param projectId
     * @param skillId
     * @param start
     * @return
     */
    @Transactional(readOnly = true)
    List<DayCountItem> getDistinctUserCountForSkillId(String projectId, String skillId, Date start) {
        EventType eventType = determineAppropriateEventType(start)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillId(projectId, skillId)
        if (!skillDef) {
            throw new SkillException("Skill does not exist", projectId, skillId, ErrorCode.SkillNotFound)
        }

        Validate.isTrue(ALLOWABLE_CONTAINER_TYPES.contains(skillDef.type), "Unsupported ContainerType [${skillDef.type}]")

        List<DayCountItem> results
        Integer rawId = skillDef.id

        if (EventType.DAILY == eventType) {
            Stream<DayCountItem> stream
            if (SkillDef.ContainerType.Skill == skillDef.type) {
                stream = userEventsRepo.getDistinctUserCountForSkill(rawId, start, eventType)
            } else {
                stream = userEventsRepo.getDistinctUserCountForSubject(rawId, start, eventType)
            }
            results = convertResults(stream, eventType)
        } else {
            Stream<WeekCount> stream
            if (SkillDef.ContainerType.Skill == skillDef.type) {
                stream = userEventsRepo.getDistinctUserCountForSkillGroupedByWeek(rawId, start)
            } else {
                stream = userEventsRepo.getDistinctUserCountForSubjectGroupedByWeek(rawId, start)
            }
            results = convertResults(stream)
        }
        return results
    }

    /**
     * Returns the total number of user events for a given project id occurring since the
     * specified start date. Event counts are returned grouped by day with missing days zero filled.
     *
     * If start results in a date older than the compactDailyEventsOlderThan, then results will be
     * compacted into weekly results with each DayCountItem.start being the start of week date for that
     * weekly rollup (Sunday).
     *
     * @param projectId
     * @param start
     * @return
     */
    @Transactional(readOnly = true)
    List<DayCountItem> getUserEventCountsForProject(String projectId, Date start) {
        EventType eventType = determineAppropriateEventType(start)

        List<DayCountItem> results
        if (EventType.DAILY == eventType) {
            Stream<DayCountItem> stream = userEventsRepo.getEventCountForProject(projectId, start, eventType)
            results = convertResults(stream, eventType)
        } else {
            Stream<WeekCount> stream = userEventsRepo.getEventCountForProjectGroupedByWeek(projectId, start)
            results = convertResults(stream)
        }

        return results
    }

    /**
     * Returns the distinct user count for the specified project by day, unless start is older than the compactDailyEventsOlderThan
     * setting, in which case results are grouped by week where DayCountItem.start refers to the start-of-week (Sunday) for that week's
     * distinct user counts.
     *
     * @param projectId
     * @param start
     * @return
     */
    @Transactional(readOnly = true)
    List<DayCountItem> getDistinctUserCountsForProject(String projectId, Date start) {
        EventType eventType = determineAppropriateEventType(start)

        List<DayCountItem> results
        if (EventType.DAILY == eventType) {
            Stream<DayCountItem> stream = userEventsRepo.getDistinctUserCountForProject(projectId, start, eventType)
            results = convertResults(stream, eventType)
        } else {
            Stream<WeekCount> stream = userEventsRepo.getDistinctUserCountForProjectGroupedByWeek(projectId, start)

            results = convertResults(stream)
        }
        return results
    }

    /**
     * Records a SkillEvent for the specified user. Note that these events are separate from any achievement or point tracking events and
     * are recorded regardless of whether or not the reported SkillEvent was applied.
     *
     * @param skillRefId
     * @param userId
     * @param date
     * @param eventCount
     * @param type
     */
    @Transactional
    public void recordEvent(Integer skillRefId, String userId, Date date, Integer eventCount = 1, EventType type = EventType.DAILY) {
        StartStopDate startEnd = formatStartAndEnd(date, type)
        Integer weekNumber = WeekNumberUtil.getWeekNumber(startEnd.start)
        nativeQueriesRepo.createOrUpdateUserEvent(skillRefId, userId, startEnd.start, startEnd.stop, type.toString(), eventCount,  weekNumber)
    }

    /**
     * Compacts daily events older than compactDailyEventsOlderThan into weekly events.
     */
    @CompileStatic
    @Transactional
    public void compactDailyEvents() {
        LocalDateTime dateTime = LocalDateTime.now().minusDays(maxDailyDays)
        log.info("beginning compaction of daily events older than [${dateTime}] into weekly events")

        int totalProcessed = 0
        StopWatch sw = new StopWatch()
        sw.start()

        Stream<UserEvent> stream = userEventsRepo.findAllByEventTypeAndStartLessThan(EventType.DAILY, dateTime.toDate())
        stream.forEach({ UserEvent userEvent ->
            totalProcessed++
            recordEvent(userEvent.skillRefId, userEvent.userId, userEvent.start, userEvent.count, EventType.WEEKLY)
            entityManager.detach(userEvent)
            if (totalProcessed % 50000 == 0) {
                log.info("compacted $totalProcessed daily user events so far")
            }
        })
        sw.stop()
        Duration duration = Duration.of(sw.getTime(), ChronoUnit.MILLIS)

        log.info("Compacted [${totalProcessed}] rows in [${duration}], deleting input events now")
        sw.reset()
        sw.start()
        userEventsRepo.deleteByEventTypeAndStartLessThan(EventType.DAILY, dateTime.toDate())
        sw.stop()
        duration = Duration.of(sw.getTime(), ChronoUnit.MILLIS)
        log.info("Deleted compacted input events in [${duration}]")
    }

    @CompileStatic
    private static StartStopDate formatStartAndEnd(Date date, EventType type) {
        if (EventType.DAILY == type) {
            LocalDate localDate = date.toLocalDate()
            LocalDateTime start = LocalDateTime.of(localDate, LocalTime.MIN)
            LocalDateTime end = LocalDateTime.of(localDate, LocalTime.MAX)
            return new StartStopDate(start: start.toDate(), stop: end.toDate())
        } else if (EventType.WEEKLY == type) {
            TemporalField temporalField = WeekFields.of(Locale.US).dayOfWeek()
            LocalDate eventDate = date.toLocalDate()
            LocalDateTime startOfWeek = LocalDateTime.of(eventDate.with(temporalField, 1), LocalTime.MIN)
            LocalDateTime endOfWeek = LocalDateTime.of(eventDate.with(temporalField, 7), LocalTime.MAX)
            return new StartStopDate(start: startOfWeek.toDate(), stop: endOfWeek.toDate())
        } else {
            throw new SkillException("unrecognized EventType [${type}]")
        }
    }

    @CompileStatic
    private List<DayCountItem> convertResults(Stream<DayCountItem> stream, EventType eventType) {
        List<DayCountItem> results = []
        Date last = formatStartAndEnd(new Date(), eventType).start
        int count = 0;

        stream.forEach({
            if (EventType.WEEKLY == eventType) {
                if (it.day.toLocalDate().getDayOfWeek() != DayOfWeek.SUNDAY) {
                    //we have to fix the day to align with the start of the week as there can be gaps in the daily data
                    //this happens when daily metrics are grouped into weekly in the query
                    it.day = formatStartAndEnd(it.day, EventType.WEEKLY).start
                }
            }
            List<DayCountItem> zeroFills = zeroFillGaps(eventType, last, it.day, count == 0)
            if (zeroFills) {
                results.addAll(zeroFills)
            }
            last = it.day
            results.add(it)
        })

        return results
    }

    @CompileStatic
    private List<DayCountItem> convertResults(Stream<WeekCount> stream) {
        List<DayCountItem> results = []
        Date last = formatStartAndEnd(new Date(), EventType.WEEKLY).start
        int count = 0;

        stream.forEach({
            Date day = WeekNumberUtil.getStartOfWeekFromWeekNumber(it.weekNumber).atStartOfDay().toDate()
            DayCountItem dci = new DayCountItem(day, it.count)

            List<DayCountItem> zeroFills = zeroFillGaps(EventType.WEEKLY, last, dci.day, count == 0)
            if (zeroFills) {
                results.addAll(zeroFills)
            }
            last = dci.day
            results.add(dci)
        })

        return results
    }

    @CompileStatic
    private List<DayCountItem> fillGaps(List<DayCountItem> items, EventType eventType) {
        Date last = formatStartAndEnd(new Date(), eventType).start
        List<DayCountItem> zeroFilled = []

        items.eachWithIndex{ DayCountItem entry, int i ->
            List<DayCountItem> zeroFills = zeroFillGaps(eventType, last, entry.day, i == 0)
            if (zeroFills) {
                zeroFilled.addAll(zeroFills)
            }
            last = entry.day
            zeroFilled.add(entry)
        }
    }

    private static List<DayCountItem> zeroFillGaps(EventType eventType, Date n, Date nMinusOne, boolean inclusive) {
        if (EventType.DAILY == eventType) {
            return ZeroFillDayCountItemUtil.zeroFillDailyGaps(n, nMinusOne, inclusive)
        } else if (EventType.WEEKLY == eventType) {
            return ZeroFillDayCountItemUtil.zeroFillWeeklyGaps(n, nMinusOne, inclusive)
        } else {
            throw new SkillException("Unrecognized EventType [${eventType}]")
        }
    }

    private EventType determineAppropriateEventType(Date start) {
        EventType eventType = EventType.DAILY

        if (start.toLocalDateTime().isBefore(LocalDateTime.now().minusDays(maxDailyDays))) {
            eventType = EventType.WEEKLY
        }

        return eventType
    }

    @EqualsAndHashCode
    private static class StartStopDate {
        Date start
        Date stop
    }

}
