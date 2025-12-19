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
package skills.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TimeRangeFormatterUtil {

    static String defaultStart = "1900-01-01 00:00:00"
    static String defaultEnd = "2100-12-31 23:59:59"

    static List<Date> formatTimeRange(String start, String end, Boolean exactTime = true) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        LocalDateTime startDate = start ? LocalDateTime.parse(start, formatter) : LocalDateTime.parse(defaultStart, formatter) //start ? format.parse(start) : format.parse(defaultStart)
        LocalDateTime endDate = end ? LocalDateTime.parse(end, formatter) : LocalDateTime.parse(defaultEnd, formatter)

        if(!exactTime) {
            startDate = startDate.toLocalDate().atTime(LocalTime.MIN)
            endDate = endDate.toLocalDate().atTime(LocalTime.MAX)
        }

        return [startDate.toDate(), endDate.toDate()]
    }
}
