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
package skills.intTests

import org.apache.commons.lang3.RandomUtils
import skills.services.StartDateUtil
import skills.storage.model.EventType

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields

class TestDates {
    LocalDateTime now;
    LocalDateTime startOfCurrentWeek;
    LocalDateTime startOfPreviousWeek;

    public TestDates() {
        now = LocalDateTime.now()
        startOfCurrentWeek = StartDateUtil.computeStartDate(now.toDate(), EventType.DAILY).toLocalDateTime().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        startOfPreviousWeek = startOfCurrentWeek.minusWeeks(1)
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

    LocalDateTime getDateWithinPreviousWeek(Date excludeMe){
        TemporalField dayOfWeekField = WeekFields.of(Locale.US).dayOfWeek()
        int dayToReturn = -1

        if (excludeMe) {
            int excludeThisDay = excludeMe.toLocalDateTime().get(dayOfWeekField)
            int randomDay = -1
            while ((randomDay = RandomUtils.nextInt(1, 8)) == excludeThisDay) {
                //continue
            }
            dayToReturn = randomDay
        } else {
            dayToReturn = RandomUtils.nextInt(1, 8)
        }

        return startOfCurrentWeek.with(dayOfWeekField, dayToReturn)
    }

    LocalDateTime getDateWithinWeek(Date target, boolean afterOrSame=false){
        Date startOfWeek = StartDateUtil.computeStartDate(target, EventType.WEEKLY)
        TemporalField dayOfWeekField = WeekFields.of(Locale.US).dayOfWeek()
        int targetDayOfWeek = target.toLocalDateTime().get(dayOfWeekField)

        if (afterOrSame) {
            if (targetDayOfWeek == 7) {
                //end of the week already, we can't return a later date and stay within the target week
                return target
            } else {
                return target.toLocalDateTime().with(dayOfWeekField, RandomUtils.nextInt(targetDayOfWeek+1, 8))
            }
        } else {
            int randomDay = -1
            while ((randomDay = RandomUtils.nextInt(1, 8)) == targetDayOfWeek) {
                //continue
            }
            return target.toLocalDateTime().with(dayOfWeekField, randomDay)
        }
    }

}
