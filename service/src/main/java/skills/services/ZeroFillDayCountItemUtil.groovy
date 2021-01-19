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

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ZeroFillDayCountItemUtil {

    /**
     * Produces DayCountItems zero filled for the gaps between n and nMinusOne, exclusive of both
     * n and nMinusOne
     *
     * @param n the most recent date in the data set
     * @param nMinusOne a date semantically before n
     * @return
     */
    public static List<DayCountItem> zeroFillDailyGaps(Date n, Date nMinusOne) {
        return zeroFillGaps(true, n, nMinusOne)
    }

    /**
     * Produces DayCountItems zero filled for the gap between n and nMinusOne, exclusive of both
     * with the assumption that the dates are meant to be separated by week and both are for the start of week
     *
     * e.g.,:
     *
     * TemporalField temporalField = WeekFields.of(Locale.US).dayOfWeek()
     * LocalDate eventDate = date.toLocalDate()
     * LocalDateTime startOfWeek = LocalDateTime.of(eventDate.with(temporalField, 1), LocalTime.MIN)
     *
     * @param n
     * @param nMinusOne
     * @return
     */
    public static List<DayCountItem> zeroFillWeeklyGaps(Date n, Date nMinusOne) {
        return zeroFillGaps(false, n, nMinusOne)
    }

    private static List<DayCountItem> zeroFillGaps(boolean daily, Date n, Date nMinusOne) {
        def nMinusOneLd = nMinusOne.toLocalDate()
        def nLd = n.toLocalDate()
        long daysBetween = ChronoUnit.DAYS.between(nMinusOneLd, nLd)
        List<DayCountItem> fills = null
        if (daily && daysBetween > 1) {
            fills = []
            LocalDateTime ldt = n.toLocalDateTime()
            for (int i = 1; i < daysBetween; i++) {
                Date fill = ldt.minusDays(i).toDate()
                fills.add(new ZeroFillDayCountItem(day:fill))
            }
        } else if (!daily && daysBetween > 7){
            // since we force the start date to be based on start of week this should work cleanly
            long fillWeeks = ChronoUnit.WEEKS.between(nMinusOneLd, nLd)
            fills = []
            LocalDateTime ldt = n.toLocalDateTime()
            for (int i = 1; i < fillWeeks; i++) {
                Date fill = ldt.minusWeeks(i).toDate()
                fills.add(new ZeroFillDayCountItem(day: fill))
            }
        }

        return fills
    }

    public static class ZeroFillDayCountItem implements DayCountItem {
        Date day
        Integer count = 0;
    }
}
