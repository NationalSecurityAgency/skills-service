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
class NumUserAchievedOverTimeChartBuilder implements MetricsChartBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    static class AchievementCount {
        Integer num
        Long timestamp
    }

    static class AchievementCountRes {
        List<AchievementCount> achievementCounts
    }

    @Override
    String getId() {
        return "numUserAchievedOverTimeChartBuilder"
    }

    AchievementCountRes build(String projectId, String chartId, Map<String, String> props) {
        String skillId = MetricsParams.getSkillId(projectId, chartId, props);
        List<DayCountItem> skillDayUserCounts = userAchievedRepo.countNumUsersOverTimeByProjectIdAndSkillId(projectId, skillId)

        List<AchievementCount> achievementCounts = skillDayUserCounts.collect {
            new AchievementCount(num: it.getCount(), timestamp: it.getDay().time)
        }
        achievementCounts = achievementCounts?.sort({ it.timestamp })
        return new AchievementCountRes(achievementCounts: achievementCounts
        )
    }
}
