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
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
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
import skills.storage.repos.nativeSql.PostgresQlNativeRepo

import java.time.*
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

import static skills.storage.model.SkillDef.ContainerType

@Component
@Slf4j
class UserEventService {

    @Autowired
    PostgresQlNativeRepo PostgresQlNativeRepo

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
    int maxDailyDays = 30 //changing this can cause some problems

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    LockingService lockingService

    private static final List<ContainerType> ALLOWABLE_CONTAINER_TYPES = [ContainerType.Skill, ContainerType.Subject]

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
        //TODO: fix all these
        List<DayCountItem> results
        Integer rawId = skillDef.id
        if (EventType.DAILY == eventType) {
            Stream<DayCountItem> stream
            if (ContainerType.Skill == skillDef.type) {
                stream = userEventsRepo.getEventCountForSkill(rawId, start, eventType)
            } else {
                stream = userEventsRepo.getEventCountForSubject(rawId, start, eventType)
            }
            results = convertResults(stream, eventType, start)
        } else {
            start = StartDateUtil.computeStartDate(start, EventType.WEEKLY)
            Stream<WeekCountItem> stream
            if (ContainerType.Skill == skillDef.type) {
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
    List<DayCountItem> getDistinctUserCountForSkillId(String projectId, String skillId, Date start, Boolean newUsersOnly = false) {
        EventType eventType = determineAppropriateEventType(start)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillId(projectId, skillId)
        if (!skillDef) {
            throw new SkillException("Skill does not exist", projectId, skillId, ErrorCode.SkillNotFound)
        }

        Validate.isTrue(ALLOWABLE_CONTAINER_TYPES.contains(skillDef.type), "Unsupported ContainerType [${skillDef.type}]")

        List<DayCountItem> results
        //only applicable to skills
        Integer rawId = skillDef.id

        if (EventType.DAILY == eventType) {
            Stream<DayCountItem> stream
            if (ContainerType.Skill == skillDef.type) {
                stream = userEventsRepo.getDistinctUserCountForSkill(rawId, start, eventType, newUsersOnly)
            } else {
                stream = userEventsRepo.getDistinctUserCountForSubject(rawId, start, eventType, newUsersOnly)
            }
            results = convertResults(stream, eventType, start)
        } else {
            start = StartDateUtil.computeStartDate(start, EventType.WEEKLY)
            Stream<WeekCountItem> stream
            if (ContainerType.Skill == skillDef.type) {
                stream = userEventsRepo.getDistinctUserCountForSkillGroupedByWeek(rawId, start, newUsersOnly)
            } else {
                stream = userEventsRepo.getDistinctUserCountForSubjectGroupedByWeek(rawId, start, newUsersOnly)
            }
            results = convertResults(stream, start)
        }
        return results
    }

    @Transactional(readOnly = true)
    List<MonthlyCountItem> getDistinctUserCountForSubjectByMonth(String projectId, String skillId, Date start, Boolean newUsersOnly = false) {

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillId(projectId, skillId)
        if (!skillDef) {
            throw new SkillException("Skill does not exist", projectId, skillId, ErrorCode.SkillNotFound)
        }

        Validate.isTrue(ALLOWABLE_CONTAINER_TYPES.contains(skillDef.type), "Unsupported ContainerType [${skillDef.type}]")

        Stream<MonthlyCountItem> stream = userEventsRepo.getDistinctUserCountForSubjectGroupedByMonth(projectId, skillDef.id, start, newUsersOnly)
        List<MonthlyCountItem> results = convertMonthlyResults(stream, start)

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
            Stream<WeekCountItem> stream = userEventsRepo.getEventCountForUserGroupedByWeek(userId, start, projectIds)
            results = convertResults(stream, start, projectIds)
        }

        return results
    }

    /**
     * Returns the distinct user count for the specified project by day, unless start is older than the compactDailyEventsOlderThan
     * setting, in which case results are grouped by week where DayCountItem.start refers to the start-of-week (Sunday) for that week's
     * distinct user counts.
     *
     * NOTE: There is the potential that DayCountItems returned by this method will have a different projectId than the
     * one specified. In the event that the specified projectId has imported skills from the catalog, events relating to those
     * skills will be returned as a DayCountItem where projectId = the EXPORTING projectId
     *
     * @param projectId
     * @param start
     * @return
     */
    @Transactional(readOnly = true)
    List<DayCountItem> getDistinctUserCountsForProject(String projectId, Date start, Boolean newUsersOnly = false) {
        EventType eventType = determineAppropriateEventType(start)
        List<DayCountItem> results
        if (EventType.DAILY == eventType) {
            Stream<DayCountItem> stream = userEventsRepo.getDistinctUserCountForProject(projectId, start, eventType, newUsersOnly)
            results = convertResults(stream, eventType, start, [projectId])
        } else {
            start = StartDateUtil.computeStartDate(start, EventType.WEEKLY)
            Stream<WeekCountItem> stream = userEventsRepo.getDistinctUserCountForProjectGroupedByWeek(projectId, start, newUsersOnly)

            results = convertResults(stream, start)
        }
        return results
    }

    @Transactional(readOnly = true)
    List<MonthlyCountItem> getDistinctUserCountsForProjectByMonth(String projectId, Date start, Boolean newUsersOnly = false) {
        List<MonthlyCountItem> results
        Stream<MonthlyCountItem> stream = userEventsRepo.getDistinctUserCountForProjectGroupedByMonth(projectId, start, newUsersOnly)
        results = convertMonthlyResults(stream, start)

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
        PostgresQlNativeRepo.createOrUpdateUserEvent(projectId, skillRefId, userId, start, type.toString(), eventCount,  weekNumber)
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

        userEventsRepo.findAllByEventTypeAndEventTimeLessThan(EventType.DAILY, dateTime.toDate()).withCloseable { Stream<UserEvent> stream ->
            stream.forEach({ UserEvent userEvent ->
                totalProcessed++
                recordEvent(userEvent.projectId, userEvent.skillRefId, userEvent.userId, userEvent.eventTime, userEvent.count, EventType.WEEKLY)
                entityManager.detach(userEvent)
                if (totalProcessed % 50000 == 0) {
                    log.info("compacted $totalProcessed daily user events so far")
                }
            })
        }
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

    /**
     * Removes a recorded userEvent, decrementing the event count column. This assumes
     * that event compaction has been run
     * @param performedOn
     * @param userId
     * @param skillRefId
     */
    @Transactional
    public void removeEvent(Date performedOn, String userId, Integer skillRefId) {
        Date dailyEventTime = StartDateUtil.computeStartDate(performedOn, EventType.DAILY)
        Date weeklyEventTime = StartDateUtil.computeStartDate(performedOn, EventType.WEEKLY)

        // to guard against an event being decremented in the window where performedOn < now-maxDailyDays
        // but compaction has not yet run for that day, we need to first check if a DAILY event type exists for this event
        UserEvent event = userEventsRepo.findByUserIdAndSkillRefIdAndEventTimeAndEventType(userId, skillRefId, dailyEventTime, EventType.DAILY)
        if (event) {
            decrementEvent(event)
        } else if ((event = userEventsRepo.findByUserIdAndSkillRefIdAndEventTimeAndEventType(userId, skillRefId, weeklyEventTime, EventType.WEEKLY)) != null){
            decrementEvent(event)
        } else {
            throw new SkillException("Unable to remove event for skillRefId" +
                    " [${skillRefId}], userId [${userId}], performedOn=[${performedOn}] checked both DAILY=[${dailyEventTime}] AND weeklyEventTime=[${weeklyEventTime}]," +
                    " no event exists. This should not happen")
        }
    }

    void removeAllEvents(String projectId, String userId) {
        userEventsRepo.deleteAllByUserIdAndProjectId(userId, projectId);
    }

    void removeAllEvents(String userId, List<Integer> skillRefIds) {
        userEventsRepo.deleteAllByUserIdAndSkillRefIdIn(userId, skillRefIds);
    }

    private void decrementEvent(UserEvent event) {
        event.count = Math.max(0, event.count-1)
        if (event.count == 0) {
            userEventsRepo.delete(event)
        } else {
            userEventsRepo.save(event)
        }
    }

    @CompileStatic
    private List<MonthlyCountItem> convertMonthlyResults(Stream<MonthlyCountItem> stream, Date startOfQueryRange) {
        List<MonthlyCountItem> items = new ArrayList<MonthlyCountItem>()

        LocalDate start = startOfQueryRange.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        LocalDate end = LocalDate.now()
        def monthsBetween = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), end.withDayOfMonth(1))
        def months = (0..monthsBetween).collect { i -> start.plusMonths(i as long).withDayOfMonth(1)}

        List<MonthlyCountItem> monthlyCounts = stream.toList()
        stream.close()

        for(month in months) {
            def foundMonth = monthlyCounts.find(it -> it.month.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == month)
            if(foundMonth) {
                items.push(new MonthlyCount(foundMonth.projectId, month.toDate(), foundMonth.count))
            } else {
                items.push(new MonthlyCount('', month.toDate(), 0))
            }
        }

        return items
    }

    @CompileStatic
    private List<DayCountItem> convertResults(Stream<DayCountItem> stream, EventType eventType, Date startOfQueryRange, List<String> projectIds=[]) {
        // initialize counts for all passed in project id's so there will be zero counts added for projects with no events yet
        Map<String, PerProjectCounts> perProjectCounts = projectIds.collectEntries {projectId ->
            [projectId, new PerProjectCounts(lastDate: StartDateUtil.computeStartDate(new Date(), eventType))]
        }

        try {
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
                        it = new DayCount(it.projectId, StartDateUtil.computeStartDate(it.day, EventType.WEEKLY), it.count)
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
        } finally {
            stream.close()
        }

        List<DayCountItem> finalResults = []
        perProjectCounts?.each {String projectId , PerProjectCounts value ->
            boolean inclusive = value.count == 0;
            List<DayCountItem> zeroFillFromStart = zeroFillGaps(eventType, value.lastDate, startOfQueryRange, inclusive, projectId)
            if (zeroFillFromStart) {
                value.results.addAll(zeroFillFromStart)
            }
            finalResults.addAll(value.results)
        }

        return finalResults
    }

    @CompileStatic
    private List<DayCountItem> convertResults(Stream<WeekCountItem> stream, Date startOfQueryRange, List<String> projectIds=[]) {
        // initialize counts for all passed in project id's so there will be zero counts added for projects with no events yet
        Map<String, PerProjectCounts> perProjectCounts = projectIds.collectEntries {projectId ->
            [projectId, new PerProjectCounts(lastDate: StartDateUtil.computeStartDate(new Date(), EventType.WEEKLY))]
        }

        try {
            stream.forEach({
                Date day = WeekNumberUtil.getStartOfWeekFromWeekNumber(it.weekNumber).atStartOfDay().toDate()
                DayCount dci = new DayCount(it.projectId, day, it.count)

                PerProjectCounts perProject = perProjectCounts.get(it.projectId)
                if (!perProject) {
                    perProject = new PerProjectCounts(lastDate: StartDateUtil.computeStartDate(new Date(), EventType.WEEKLY))
                    perProjectCounts.put(it.projectId, perProject)
                }

                boolean first = perProject.count == 0
                List<DayCountItem> zeroFills = zeroFillGaps(EventType.WEEKLY, perProject.lastDate, dci.day, (first && day != perProject.lastDate), it.projectId)
                if (zeroFills) {
                    perProject.results.addAll(zeroFills)
                }
                perProject.lastDate = dci.day
                perProject.results.add(dci)
                perProject.count++
            })
        } finally {
            stream.close()
        }

        List<DayCountItem> finalResults = []
        perProjectCounts?.each {String projectId , PerProjectCounts value ->
            boolean inclusive = value.count == 0;
            List<DayCountItem> zeroFillFromStart = zeroFillGaps(EventType.WEEKLY, value.lastDate, startOfQueryRange, inclusive, projectId)
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
