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
package skills.metricsNew.builders.skill

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.metricsNew.builders.MetricsChartBuilder
import skills.metricsNew.builders.MetricsParams
import skills.storage.model.DayCountItem
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo

@Component
@Slf4j
class SkillEventsOverTimeChartBuilder implements MetricsChartBuilder {

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    static class ResCount {
        Integer num
        Long timestamp
    }

    static class FinalRes {
        List<ResCount> countsByDay
    }

    @Override
    String getId() {
        return "skillEventsOverTimeChartBuilder"
    }

    FinalRes build(String projectId, String chartId, Map<String, String> props) {
        String skillId = MetricsParams.getSkillId(projectId, chartId, props);
        List<DayCountItem> counts = userPerformedSkillRepo.countsByDay(projectId, skillId)

        List<ResCount> countsByDay = counts.collect {
            new ResCount(num: it.getCount(), timestamp: it.getDay().time)
        }
        countsByDay = countsByDay?.sort({it.timestamp})

        return new FinalRes(countsByDay: countsByDay)
    }
}
