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

import spock.lang.Specification

class TimeRangeFormatterUtilSpec extends Specification {

    def "end date is used when only the end of the range is supplied"() {
        when:
        List<Date> range = TimeRangeFormatterUtil.formatTimeRange(start, end)

        then:
        range[0].toLocalDateTime().toString() == expectedStart
        range[1].toLocalDateTime().toString() == expectedEnd

        where:
        start                 | end                   | expectedStart         | expectedEnd
        null                  | "2024-03-04 05:06:07" | "1900-01-01T00:00"    | "2024-03-04T05:06:07"
        ""                    | "2024-03-04 05:06:07" | "1900-01-01T00:00"    | "2024-03-04T05:06:07"
        "null"                | "2024-03-04 05:06:07" | "1900-01-01T00:00"    | "2024-03-04T05:06:07"
        "2023-01-02 03:04:05" | null                  | "2023-01-02T03:04:05" | "2100-12-31T23:59:59"
        "2023-01-02 03:04:05" | "null"                | "2023-01-02T03:04:05" | "2100-12-31T23:59:59"
        "2023-01-02 03:04:05" | "2024-03-04 05:06:07" | "2023-01-02T03:04:05" | "2024-03-04T05:06:07"
    }

    def "no arguments gives the full default range"() {
        when:
        List<Date> range = TimeRangeFormatterUtil.formatTimeRange(null, null)

        then:
        range[0].toLocalDateTime().toString() == "1900-01-01T00:00"
        range[1].toLocalDateTime().toString() == "2100-12-31T23:59:59"
    }

    def "the inexact range covers whole days"() {
        when:
        List<Date> range = TimeRangeFormatterUtil.formatTimeRange(null, "2024-03-04 05:06:07", false)

        then:
        range[0].toLocalDateTime().toLocalTime() == java.time.LocalTime.MIN
        range[1].toLocalDateTime().toLocalDate().toString() == "2024-03-04"
    }
}
