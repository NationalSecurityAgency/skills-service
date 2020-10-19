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
package skills.metricsNew.builders.skills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import skills.metricsNew.builders.MetricsChartBuilder
import skills.storage.repos.UserAchievedLevelRepo

@Component
@Slf4j
class SkillUsageNavigatorChartBuilder implements MetricsChartBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Override
    String getId() {
        return "skillUsageNavigatorChartBuilder"
    }

    static class SkillUsageNavigatorItem {
        String skillId
        String skillName
        Integer numUserAchieved
        Integer numUsersInProgress
        Long lastReportedTimestamp
        Long lastAchievedTimestamp
    }

    List<SkillUsageNavigatorItem> build(String projectId, String chartId, Map<String, String> props) {
        def res = userAchievedRepo.findAllForSkillsNavigator(projectId)
        return res.collect {
            Integer numAchieved = it.getNumUserAchieved() ?: 0
            Integer numProgress = it.getNumUsersInProgress() ?: 0
            new SkillUsageNavigatorItem(
                    skillId: it.getSkillId(),
                    skillName: it.getSkillName(),
                    numUserAchieved: numAchieved,
                    numUsersInProgress: numProgress - numAchieved,
                    lastReportedTimestamp: it.getLastReported()?.time,
                    lastAchievedTimestamp: it.getLastAchieved()?.time
            )
        }
    }
}
