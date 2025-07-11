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

class TimeRangeFormatterUtil {

    static String defaultStart = "1900-01-01 00:00:00"
    static String defaultEnd = "2100-12-31 23:59:59"

    static List<Date> formatTimeRange(String start, String end) {
        def format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        Date startDate = start ? format.parse(start) : format.parse(defaultStart)
        Date endDate = end ? format.parse(end) : format.parse(defaultEnd)

        return [startDate, endDate]
    }
}
