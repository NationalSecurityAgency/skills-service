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
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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

    // Changing this to a larger value once events have been compacted (so in this case after there are events that
    // are 30 days old that have been compacted into weekly events) will cause weekly events that are now newer than
    // compactDailyEventsOlderThan to no longer be visible in the system.
    // compactDailyEventsOlderThan can be decreased and at any time with the only impact being that the daily events older than the new compactDailyEventsOlderThan
    // will not be exposed until after compaction next runs.
    @Value('#{"${skills.config.compactDailyEventsOlderThan:30}"}')
    int maxDailyDays = 30

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    LockingService lockingService

    private static final List<SkillDef.ContainerType> ALLOWABLE_CONTAINER_TYPES = [SkillDef.ContainerType.Skill, SkillDef.ContainerType.Subject]

    /**
     * Returns the daily user interaction counts for a skill or subject whether the skill was applied or not.
     *
     * Note that if the start date exceeds the configured compactDailyEventsOlderThan, the DayCountItems are returned
     * aggregated by week (the day attributes will be separated by week with the daily count aggregated under that week)
     * where DayCountItem.start refers to the start of that week's compacted metrics (Sunday). Missing days/weeks are zero filled.
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
            results = convertResults(stream, eventType, start)
        } else {
            start = StartDateUtil.computeStartDate(start, EventType.WEEKLY)
            Stream<WeekCount> stream
            if (SkillDef.ContainerType.Skill == skillDef.type) {
                stream = userEventsRepo.getEventCountForSkillGroupedByWeek(rawId, start)
            } else {
                stream = userEventsRepo.getEventCountForSubjectGroupedByWeek(rawId, start)
            }
            results = convertResults(stream, start)
        }

        return results
    }

    /**
     * Returns the distinct user count for the specified project by day, unless start is older than the compactDailyEventsOlderThan
     * setting, in which case results are grouped by week where DayCountItem.start refers to the start-of-week (Sunday) for that week's
     * distinct user counts. Missing days/weeks are zero-filled.
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
            results = convertResults(stream, eventType, start)
        } else {
            start = StartDateUtil.computeStartDate(start, EventType.WEEKLY)
            Stream<WeekCount> stream
            if (SkillDef.ContainerType.Skill == skillDef.type) {
                stream = userEventsRepo.getDistinctUserCountForSkillGroupedByWeek(rawId, start)
            } else {
                stream = userEventsRepo.getDistinctUserCountForSubjectGroupedByWeek(rawId, start)
            }
            results = convertResults(stream, start)
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
            results = convertResults(stream, eventType, start)
        } else {
            start = StartDateUtil.computeStartDate(start, EventType.WEEKLY)
            Stream<WeekCount> stream = userEventsRepo.getEventCountForProjectGroupedByWeek(projectId, start)
            results = convertResults(stream, start)
        }

        return results
    }

    /**
     * Returns the total number of user events for a given user id occurring since the
     * specified start date. Event counts are returned grouped by day with missing days zero filled.
     *
     * If start results in a date older than the compactDailyEventsOlderThan, then results will be
     * compacted into weekly results with each DayCountItem.start being the start of week date for that
     * weekly rollup (Sunday).
     *
     * NOTE - when multiple projectIds are included in the results, DayCountItem order is not defined
     * @param userId
     * @param start
     * @return
     */
    @Transactional(readOnly = true)
    List<DayCountItem> getUserEventCountsForUser(String userId, Date start, List<String> projectIds) {
        EventType eventType = determineAppropriateEventType(start)

        List<DayCountItem> results
        if (EventType.DAILY == eventType) {
            Stream<DayCountItem> stream = userEventsRepo.getEventCountForUser(userId, start, eventType, projectIds)
            results = convertResults(stream, eventType, start, projectIds)
        } else {
            start = StartDateUtil.computeStartDate(start, EventType.WEEKLY)
            Stream<WeekCount> stream = userEventsRepo.getEventCountForUserGroupedByWeek(userId, start, projectIds)
            results = convertResults(stream, start, projectIds)
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
            results = convertResults(stream, eventType, start)
        } else {
            start = StartDateUtil.computeStartDate(start, EventType.WEEKLY)
            Stream<WeekCount> stream = userEventsRepo.getDistinctUserCountForProjectGroupedByWeek(projectId, start)

            results = convertResults(stream, start)
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
    public void recordEvent(String projectId, Integer skillRefId, String userId, Date date, Integer eventCount = 1, EventType type = EventType.DAILY) {
        Date start = StartDateUtil.computeStartDate(date, type)
        Integer weekNumber = WeekNumberUtil.getWeekNumber(start)
        nativeQueriesRepo.createOrUpdateUserEvent(projectId, skillRefId, userId, start, type.toString(), eventCount,  weekNumber)
    }

    /**
     * Compacts daily events older than compactDailyEventsOlderThan into weekly events.
     */
    @CompileStatic
    @Transactional
    public void compactDailyEvents() {
        lockingService.lockEventCompaction()

        LocalDateTime dateTime = LocalDateTime.now().minusDays(maxDailyDays)
        log.info("beginning compaction of daily events older than [${dateTime}] into weekly events")

        int totalProcessed = 0
        StopWatch sw = new StopWatch()
        sw.start()

        Stream<UserEvent> stream = userEventsRepo.findAllByEventTypeAndEventTimeLessThan(EventType.DAILY, dateTime.toDate())
        stream.forEach({ UserEvent userEvent ->
            totalProcessed++
            recordEvent(userEvent.projectId,  userEvent.skillRefId, userEvent.userId, userEvent.eventTime, userEvent.count, EventType.WEEKLY)
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
        userEventsRepo.deleteByEventTypeAndEventTimeLessThan(EventType.DAILY, dateTime.toDate())
        sw.stop()
        duration = Duration.of(sw.getTime(), ChronoUnit.MILLIS)
        log.info("Deleted compacted input events in [${duration}]")
    }

    @CompileStatic
    private List<DayCountItem> convertResults(Stream<DayCountItem> stream, EventType eventType, Date startOfQueryRange, List<String> projectIds=[]) {
        // initialize counts for all passed in project id's so there will be zero counts added for projects with no events yet
        Map<String, PerProjectCounts> perProjectCounts = projectIds.collectEntries {projectId ->
            [projectId, new PerProjectCounts(lastDate: StartDateUtil.computeStartDate(new Date(), eventType))]
        }

        stream.forEach({
            PerProjectCounts perProject = perProjectCounts.get(it.projectId)
            if (!perProject) {
                perProject = new PerProjectCounts(lastDate: StartDateUtil.computeStartDate(new Date(), eventType))
                perProjectCounts.put(it.projectId, perProject)
            }
            if (EventType.WEEKLY == eventType) {
                if (it.day.toLocalDate().getDayOfWeek() != DayOfWeek.SUNDAY) {
                    //we have to fix the day to align with the start of the week as there can be gaps in the daily data
                    //this happens when daily metrics are grouped into weekly in the query
                    it.day = StartDateUtil.computeStartDate(it.day, EventType.WEEKLY)
                }
            }

            Date last = perProject.lastDate
            boolean first = perProject.count == 0
            List<DayCountItem> zeroFills = zeroFillGaps(eventType, last, it.day, (first && it.day != perProject.lastDate), it.projectId)
            if (zeroFills) {
                perProject.results.addAll(zeroFills)
            }
            perProject.lastDate = it.day
            perProject.results.add(it)
            perProject.count++
        })

        List<DayCountItem> finalResults = []
        perProjectCounts?.each {String projectId , PerProjectCounts value ->
            List<DayCountItem> zeroFillFromStart = zeroFillGaps(eventType, value.lastDate, startOfQueryRange, false, projectId)
            if (zeroFillFromStart) {
                value.results.addAll(zeroFillFromStart)
            }
            finalResults.addAll(value.results)
        }

        return finalResults
    }

    @CompileStatic
    private List<DayCountItem> convertResults(Stream<WeekCount> stream, Date startOfQueryRange, List<String> projectIds=[]) {
        // initialize counts for all passed in project id's so there will be zero counts added for projects with no events yet
        Map<String, PerProjectCounts> perProjectCounts = projectIds.collectEntries {projectId ->
            [projectId, new PerProjectCounts(lastDate: StartDateUtil.computeStartDate(new Date(), EventType.WEEKLY))]
        }

        stream.forEach({
            Date day = WeekNumberUtil.getStartOfWeekFromWeekNumber(it.weekNumber).atStartOfDay().toDate()
            DayCountItem dci = new DayCountItem(it.projectId, day, it.count)

            PerProjectCounts perProject = perProjectCounts.get(it.projectId)
            if (!perProject) {
                perProject = new PerProjectCounts(lastDate: StartDateUtil.computeStartDate(new Date(), EventType.WEEKLY))
                perProjectCounts.put(it.projectId, perProject)
            }

            boolean first=perProject.count == 0
            List<DayCountItem> zeroFills = zeroFillGaps(EventType.WEEKLY, perProject.lastDate, dci.day, (first && day != perProject.lastDate), it.projectId)
            if (zeroFills) {
                perProject.results.addAll(zeroFills)
            }
            perProject.lastDate = dci.day
            perProject.results.add(dci)
            perProject.count++
        })

        List<DayCountItem> finalResults = []
        perProjectCounts?.each {String projectId , PerProjectCounts value ->
            List<DayCountItem> zeroFillFromStart = zeroFillGaps(EventType.WEEKLY, value.lastDate, startOfQueryRange, false, projectId)
            if (zeroFillFromStart) {
                value.results.addAll(zeroFillFromStart)
            }
            finalResults.addAll(value.results)
        }

        return finalResults
    }

    private static List<DayCountItem> zeroFillGaps(EventType eventType, Date n, Date nMinusOne, boolean inclusive, String projectId) {
        if (EventType.DAILY == eventType) {
            return ZeroFillDayCountItemUtil.zeroFillDailyGaps(n, nMinusOne, inclusive, projectId)
        } else if (EventType.WEEKLY == eventType) {
            return ZeroFillDayCountItemUtil.zeroFillWeeklyGaps(n, nMinusOne, inclusive, projectId)
        } else {
            throw new SkillException("Unrecognized EventType [${eventType}]")
        }
    }

    public EventType determineAppropriateEventType(Date start) {
        EventType eventType = EventType.DAILY

        if (start.toLocalDateTime().isBefore(LocalDate.now().atStartOfDay().minusDays(maxDailyDays))) {
            eventType = EventType.WEEKLY
        }

        return eventType
    }

    private static class PerProjectCounts {
        Date lastDate;
        List<DayCountItem> results = [];
        int count;
    }
}
