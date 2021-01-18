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
import org.joda.time.DateTimeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.storage.model.UserEvent
import skills.storage.repos.UserEventsRepo
import skills.storage.repos.nativeSql.NativeQueriesRepo

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalField
import java.time.temporal.TemporalUnit
import java.time.temporal.WeekFields
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
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

    public void recordEvent(Integer skillRefId, String userId, Date date, Integer eventCount=1, UserEvent.EventType type=UserEvent.EventType.DAILY) {

        if (UserEvent.EventType.DAILY == type) {
            LocalDate localDate = date.toLocalDate()
            LocalDateTime start = LocalDateTime.of(localDate, LocalTime.MIN)
            LocalDateTime end = LocalDateTime.of(localDate, LocalTime.MAX)
            nativeQueriesRepo.createOrUpdateUserEvent(skillRefId, userId, start.toDate(), end.toDate(), type.toString(), 1)
        } else if (UserEvent.EventType.WEEKLY == type){
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

        Stream<UserEvent> stream = userEventsRepo.findAllByEventTypeAndStartLessThan(UserEvent.EventType.DAILY, dateTime.toDate())
        stream.forEach({UserEvent userEvent ->
            totalProcessed++
            recordEvent(userEvent.skillRefId, userEvent.userId, userEvent.start, userEvent.count, UserEvent.EventType.WEEKLY)
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
        userEventsRepo.deleteByEventTypeAndStartLessThan(UserEvent.EventType.DAILY, dateTime.toDate())
        sw.stop()
        duration = Duration.of(sw.getTime(), ChronoUnit.MILLIS)
        log.info("Deleted compacted input events in [${duration}]")
    }

}
