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
import skills.storage.model.EventType

class MetricCompactionUtil {

    static List<DayCountItem> manuallyCompactDaily(List<DayCountItem> dailyMetrics) {
        Map<Date, DayCountItem> compact = [:]
        dailyMetrics.each{
            Date start = StartDateUtil.computeStartDate(it.day, EventType.WEEKLY)
            DayCountItem aggregate = compact.get(start)
            if (!aggregate) {
                aggregate = new DayCountItem(start, 0)
                compact.put(start, aggregate)
            }
            aggregate.count += it.count
        }

        return new ArrayList<>(compact.values()).sort {it.day }
    }
}
