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

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@CompileStatic
class WeekNumberUtil {
    static final LocalDate epoch = LocalDate.ofEpochDay ( 0 ).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

    public static int getWeekNumber(Date aDate) {
        return ChronoUnit.WEEKS.between(epoch, aDate.toLocalDate())
    }

    public static LocalDate getStartOfWeekFromWeekNumber(Integer weekNumber) {
        return epoch.plusWeeks(weekNumber);
    }

}
