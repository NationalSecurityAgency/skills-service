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

import skills.storage.model.DayCountItem
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields

class ZeroFillDayCountItemUtilSpec extends Specification {

    def "produces accurate zero fills for DAILY gaps"() {
        LocalDateTime n = LocalDateTime.now()
        LocalDateTime nMinus = n.minusDays(7);

        println "n: ${n}, nMinus: ${nMinus}"

        when:
        List<DayCountItem> res = ZeroFillDayCountItemUtil.zeroFillDailyGaps(n.toDate(), nMinus.toDate(), false)
        res.each {
            println it.day
        }

        then:
        res.size() == 6
        !res.find() { it.day == n.toDate() }
        !res.find() { it.day == nMinus.toDate() }
        validateDaily(res, n)
    }

    def "produces accurate zero fills for DAILY gaps when nInclusive is set to true"() {
        LocalDateTime n = LocalDateTime.now()
        LocalDateTime nMinus = n.minusDays(7);

        println "n: ${n}, nMinus: ${nMinus}"

        when:
        List<DayCountItem> res = ZeroFillDayCountItemUtil.zeroFillDailyGaps(n.toDate(), nMinus.toDate(), true)
        res.each {
            println it.day
        }

        then:
        res.size() == 7
        res.find() { it.day == n.toDate() }
    }

    def "produces no DAILY zero fills when dates are separated by one day"() {
        LocalDateTime n = LocalDateTime.now()
        LocalDateTime nMinus = n.minusDays(1);

        println "n: ${n}, nMinus: ${nMinus}"

        when:
        List<DayCountItem> res = ZeroFillDayCountItemUtil.zeroFillDailyGaps(n.toDate(), nMinus.toDate(), false)
        res?.each {
            println it.day
        }

        then:
        !res
    }

    def "produces accurate zero fills for WEEKLY gaps"() {
        TemporalField temporalField = WeekFields.of(Locale.US).dayOfWeek()
        LocalDate eventDate = LocalDate.now()
        LocalDateTime startOfWeek = LocalDateTime.of(eventDate.with(temporalField, 1), LocalTime.MIN)
        LocalDateTime nMinus = startOfWeek.minusWeeks(7).with(temporalField, 1);

        println "n: ${startOfWeek}, nMinus: ${nMinus}"

        when:
        List<DayCountItem> res = ZeroFillDayCountItemUtil.zeroFillWeeklyGaps(startOfWeek.toDate(), nMinus.toDate(), false)
        res.each {
            println it.day
        }

        then:
        res.size() == 6
        !res.find() { it.day == startOfWeek.toDate() }
        !res.find() { it.day == nMinus.toDate() }
        validateWeekly(res, startOfWeek)
    }

    def "produces no WEEKLY zero fills when dates are separated by one week"() {
        TemporalField temporalField = WeekFields.of(Locale.US).dayOfWeek()
        LocalDate eventDate = LocalDate.now()
        LocalDateTime startOfWeek = LocalDateTime.of(eventDate.with(temporalField, 1), LocalTime.MIN)
        LocalDateTime nMinus = startOfWeek.minusWeeks(1).with(temporalField, 1);

        println "n: ${startOfWeek}, nMinus: ${nMinus}"

        when:
        List<DayCountItem> res = ZeroFillDayCountItemUtil.zeroFillWeeklyGaps(startOfWeek.toDate(), nMinus.toDate(), false)
        res.each {
            println it.day
        }

        then:
        !res
    }

    def "produces accurate zero fills for WEEKLY gaps when nInclusive set to true"() {
        TemporalField temporalField = WeekFields.of(Locale.US).dayOfWeek()
        LocalDate eventDate = LocalDate.now()
        LocalDateTime startOfWeek = LocalDateTime.of(eventDate.with(temporalField, 1), LocalTime.MIN)
        LocalDateTime nMinus = startOfWeek.minusWeeks(7).with(temporalField, 1);

        println "n: ${startOfWeek}, nMinus: ${nMinus}"

        when:
        List<DayCountItem> res = ZeroFillDayCountItemUtil.zeroFillWeeklyGaps(startOfWeek.toDate(), nMinus.toDate(), true)
        res.each {
            println it.day
        }

        then:
        res.size() == 7
        res.find() { it.day == startOfWeek.toDate() }
    }

    def validateDaily(List<DayCountItem> daily, LocalDateTime n) {
        LocalDateTime mostRecent = n
        daily.each {
            Period diff = Period.between(it.day.toLocalDate(), mostRecent.toLocalDate())
            assert diff.getDays() == 1
            mostRecent = it.day.toLocalDateTime()
        }
        return true
    }

    def validateWeekly(List<DayCountItem> daily, LocalDateTime n) {
        LocalDateTime mostRecent = n
        daily.each {
            Period diff = Period.between(it.day.toLocalDate(), mostRecent.toLocalDate())
            assert diff.getDays() == 7
            mostRecent = it.day.toLocalDateTime()
        }
        return true
    }
}
