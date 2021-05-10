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
package skills.utils


import spock.lang.Specification

import java.time.LocalDate

class RelativeTimeUtilSpec extends Specification {

    static Date end = LocalDate.parse("2021-05-03").toDate().toLocalDateTime().plusMinutes(120).toDate()

    def "produces relative time"() {
        expect:
        diff == RelativeTimeUtil.relativeTimeBetween(start, end)

        where:
        start                                                                   | diff
        end.toLocalDate().minusMonths(1).toDate()                               | "1 month ago"
        end.toLocalDate().minusMonths(2).toDate()                               | "2 months ago"
        end.toLocalDate().minusDays(45).toDate()                                | "1 month, 14 days ago"
        end.toLocalDate().minusMonths(1).minusDays(1).toDate()                  | "1 month, 1 day ago"
        end.toLocalDateTime().minusMinutes(5).toDate()                          | "today"
        end.toLocalDate().minusYears(1).toDate()                                | "1 year ago"
        end.toLocalDate().minusYears(1).minusMonths(1).toDate()                 | "1 year, 1 month ago"
        end.toLocalDate().minusYears(1).minusMonths(2).toDate()                 | "1 year, 2 months ago"
        end.toLocalDate().minusYears(1).minusMonths(2).minusDays(2).toDate()    | "1 year, 2 months ago"
        end.toLocalDate().minusYears(2).toDate()                                | "2 years ago"
        null                                                                    | "never"
    }
}
