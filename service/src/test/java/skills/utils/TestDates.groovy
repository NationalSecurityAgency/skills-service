/*

 Copyright 2026 SkillTree

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package skills.utils

import org.apache.commons.lang3.RandomUtils

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields

class TestDates {
    LocalDateTime now;
    LocalDateTime startOfCurrentWeek;
    LocalDateTime startOfTwoWeeksAgo;

    TestDates() {
        now = LocalDateTime.now()
        startOfCurrentWeek = LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        startOfTwoWeeksAgo = startOfCurrentWeek.minusWeeks(1)
    }

    LocalDateTime getFirstOfMonth(Integer monthsAgo = 0) {
        if(now.month.value > monthsAgo) {
            return now.withMonth(now.month.value - monthsAgo).withDayOfMonth(1)
        } else {
            def monthDifference = (now.month.value - monthsAgo) + 12
            return now.withYear(now.getYear() - 1).withMonth(monthDifference).withDayOfMonth(1)
        }
    }

    LocalDateTime getDateWithinCurrentWeek(boolean allowFutureDate=false) {
        if(now.getDayOfWeek() == DayOfWeek.SUNDAY) {
            if (allowFutureDate) {
                return now.plusDays(RandomUtils.nextInt(1, 6))
            }
            return now//nothing we can do
        } else {
            //us days of week are sun-saturday as 1-7
            TemporalField dayOfWeekField = WeekFields.of(Locale.US).dayOfWeek()
            int currentDayOfWeek = now.get(dayOfWeekField)

            if (allowFutureDate) {
                int randomDay = -1
                while ((randomDay = RandomUtils.nextInt(1, 7)) == currentDayOfWeek) {
                    //
                }
                return now.with(dayOfWeekField, randomDay)
            }

            return now.with(dayOfWeekField, RandomUtils.nextInt(1,currentDayOfWeek))
        }
    }

    LocalDateTime getDateInPreviousWeek(Date olderThan=null){
        TemporalField dayOfWeekField = WeekFields.of(Locale.US).dayOfWeek()
        if (olderThan) {
            LocalDateTime retVal
            while((retVal = startOfTwoWeeksAgo.with(dayOfWeekField, RandomUtils.nextInt(1,7))).isAfter(olderThan.toLocalDateTime())){
                //loop until we get a date that is before the target
            }
            return retVal
        } else {
            return startOfTwoWeeksAgo.with(dayOfWeekField, RandomUtils.nextInt(1, 7))
        }
    }
}
