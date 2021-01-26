/**
 * Copyright 2021 SkillTree
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
import skills.controller.exceptions.SkillException
import skills.storage.model.EventType

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields

class StartStopDateUtil {

    @CompileStatic
    public static StartStopDate computeStartAndStopDates(Date date, EventType type) {
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
}
