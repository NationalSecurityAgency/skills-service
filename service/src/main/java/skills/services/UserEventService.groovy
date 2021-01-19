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

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.time.StopWatch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.storage.model.DayCountItem
import skills.storage.model.SkillDef
import skills.storage.model.UserEvent
import skills.storage.model.UserEvent.EventType
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserEventsRepo
import skills.storage.repos.nativeSql.NativeQueriesRepo

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
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

    /**
     * Returns the daily user interaction counts for a skill or subject whether the skill was applied or not.
     *
     * Note that if the start date exceeds the configured compactDailyEventsOlderThan, the DayCountItems are returned
     * aggregated by week (the day attributes will be separated by week with the daily count aggregated under that week)
     *
     * @param projectId
     * @param skillId
     * @param start
     * @return
     */
    List<DayCountItem> getUserEventCountsForSkillId(String projectId, String skillId, Date start) {
       EventType eventType = determineAppropriateEventType(start)

        SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillId(projectId, skillId)
        if (!skillDef) {
            throw new SkillException("Skill does not exist", projectId, skillId, ErrorCode.SkillNotFound)
        }

        Integer rawId = skillDef.id
        Stream<DayCountItem> stream
        if (SkillDef.ContainerType.Skill == skillDef.type) {
            stream = userEventsRepo.getCountForSkill(rawId, start, eventType)
        } else if (SkillDef.ContainerType.Subject == skillDef.type) {
            stream = userEventsRepo.getCountForSubject(rawId, start, eventType)
        } else {
            throw new SkillException("Unexpected ContainerType [${skillDef.type}]")
        }

        List<DayCountItem> results = convertResults(stream, eventType)

        return results
    }

    List<DayCountItem> getUserEventCountsForProject(String projectId, Date start) {
        EventType eventType = determineAppropriateEventType(start)

        Stream<DayCountItem> stream = userEventsRepo.getCountForProject(projectId, start, eventType)
        List<DayCountItem> results = convertResults(stream, eventType)

        return results
    }

    public void recordEvent(Integer skillRefId, String userId, Date date, Integer eventCount=1, EventType type=EventType.DAILY) {

        if (EventType.DAILY == type) {
            LocalDate localDate = date.toLocalDate()
            LocalDateTime start = LocalDateTime.of(localDate, LocalTime.MIN)
            LocalDateTime end = LocalDateTime.of(localDate, LocalTime.MAX)
            nativeQueriesRepo.createOrUpdateUserEvent(skillRefId, userId, start.toDate(), end.toDate(), type.toString(), 1)
        } else if (EventType.WEEKLY == type){
            TemporalField temporalField = WeekFields.of(Locale.US).dayOfWeek()
            LocalDate eventDate = date.toLocalDate()
            LocalDateTime startOfWeek = LocalDateTime.of(eventDate.with(temporalField, 1), LocalTime.MIN)
            LocalDateTime endOfWeek = LocalDateTime.of(eventDate.with(temporalField, 7), LocalTime.MAX)
            nativeQueriesRepo.createOrUpdateUserEvent(skillRefId, userId, startOfWeek.toDate(), endOfWeek.toDate(), type.toString(), eventCount)
        } else {
            throw new UnsupportedOperationException("Unsupported UserEvent.EventType [${type}]")
        }
    }

    @Transactional
    public void compactDailyEvents(){
        LocalDateTime dateTime = LocalDateTime.now().minusDays(maxDailyDays)
        log.info("beginning compaction of daily events older than [${dateTime}] into weekly events")

        int totalProcessed = 0
        StopWatch sw = new StopWatch()
        sw.start()

        Stream<UserEvent> stream = userEventsRepo.findAllByEventTypeAndStartLessThan(EventType.DAILY, dateTime.toDate())
        stream.forEach({UserEvent userEvent ->
            totalProcessed++
            recordEvent(userEvent.skillRefId, userEvent.userId, userEvent.start, userEvent.count, EventType.WEEKLY)
            entityManager.detach(userEvent)
            if(totalProcessed % 50000 == 0) {
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

    private List<DayCountItem> convertResults(Stream<DayCountItem> stream, EventType eventType) {
        List<DayCountItem> results = []
        Date last = null
        stream.forEach({
            results.add(it)
            if (last != null) {
                List<DayCountItem> zeroFills = zeroFillGaps(eventType, last, it.day)
                if (zeroFills) {
                    results.addAll(zeroFills)
                }
            } else {
                last = it.day
            }
        })

        return results
    }

    private List<DayCountItem> zeroFillGaps(EventType eventType, Date n, Date nMinusOne) {
        if (EventType.DAILY == eventType) {
            return ZeroFillDayCountItemUtil.zeroFillDailyGaps(n, nMinusOne)
        } else if (EventType.WEEKLY == eventType) {
            return ZeroFillDayCountItemUtil.zeroFillWeeklyGaps(n, nMinusOne)
        } else {
            throw new SkillException("Unrecognized EventType [${eventType}]")
        }
    }

    private UserEvent.EventType determineAppropriateEventType(Date start) {
        UserEvent.EventType eventType = UserEvent.EventType.DAILY

        if (start.toLocalDateTime().isBefore(LocalDateTime.now().minusDays(maxDailyDays))) {
            eventType = UserEvent.EventType.WEEKLY
        }

        return eventType
    }


}
