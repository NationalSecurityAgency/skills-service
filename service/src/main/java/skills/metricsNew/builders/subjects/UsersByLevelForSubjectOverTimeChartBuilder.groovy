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
package skills.metricsNew.builders.subjects


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.CountItem
import skills.controller.result.model.TimestampCountItem
import skills.metrics.ChartParams
import skills.metricsNew.builders.MetricsChartBuilder
import skills.services.AdminUsersService
import skills.storage.model.DayCountItem
import skills.storage.repos.UserAchievedLevelRepo

@Component
class UsersByLevelForSubjectOverTimeChartBuilder implements MetricsChartBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Override
    String getId() {
        return "usersByLevelForSubjectOverTimeChartBuilder"
    }

    static class UserCountsByLevel {
        Integer level
        List<TimestampCountItem> counts
    }

    @Override
    List<UserCountsByLevel> build(String projectId, String chartId, Map<String, String> props) {
        String subjectId = ChartParams.getValue(props, ChartParams.SUBJECT_ID)
        assert subjectId, "Property [${ChartParams.SUBJECT_ID}] was not provided"

        List<UserAchievedLevelRepo.SkillAndDayUserCount> counts =
                userAchievedRepo.countNumUsersOverTimeByProjectIdAndSkillId(projectId, subjectId)

        Map<Integer, List<UserAchievedLevelRepo.SkillAndDayUserCount>> byLevel =
                counts.groupBy { it.getLevel() }

        List<UserCountsByLevel> res = byLevel.collect {
            List<UserAchievedLevelRepo.SkillAndDayUserCount> countsByDay = it.value.sort({ it.getDay() })

            int currentNumUsers = 0
            Date minDate
            List<TimestampCountItem> numUsersTimeline = countsByDay.collect {
                currentNumUsers += it.getNumberUsers()
                Date day = it.getDay()
                minDate = (!minDate || day.before(minDate)) ? day : minDate
                new TimestampCountItem(value: day.time, count: currentNumUsers)
            }

            // artificially add 0 count the day before the earliest day
            if (numUsersTimeline) {
                numUsersTimeline.add(0, new TimestampCountItem(value:  (minDate -1).time, count: 0))
            }

            new UserCountsByLevel(level: it.key, counts: numUsersTimeline)
        }

        return res;
    }
}
