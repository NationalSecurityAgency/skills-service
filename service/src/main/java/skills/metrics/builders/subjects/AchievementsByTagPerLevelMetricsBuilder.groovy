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
package skills.metrics.builders.subjects

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.metrics.builders.MetricsParams
import skills.metrics.builders.ProjectMetricsBuilder
import skills.storage.model.SkillDef
import skills.storage.repos.UserAchievedLevelRepo

@Component
@Slf4j
class AchievementsByTagPerLevelMetricsBuilder implements ProjectMetricsBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Override
    String getId() {
        return "achievementsByTagPerLevelMetricsBuilder"
    }

    @Override
    def build(String projectId, String chartId, Map<String, String> props) {
        String skillId = MetricsParams.getSkillId(projectId, chartId, props);

        def userCount = userAchievedRepo.countNumUsersPerSubjectTagAndLevel(projectId, SkillDef.ContainerType.Subject, skillId, props.userTagKey);

        HashMap<Integer, HashMap<String, Integer>> users = new HashMap<Integer, HashMap<String, Integer>>();
        userCount.forEach( it -> {
            HashMap<Integer, Integer> userInfo = new HashMap<String, Integer>();
            userInfo.put(it.userTag, it.numberUsers);
            if(users[it.level]) {
                def tag = users[it.level];
                tag[it.userTag] = it.numberUsers
                users[it.level] = tag;
            } else {
                users[it.level] = userInfo;
            }
        })

        adjustCountsToOnlyCountLastLevel(users)

        return users;
    }

    private void adjustCountsToOnlyCountLastLevel(users) {
        def levels = users.keySet().sort();
        if(levels.size() > 1) {
            for (int level = levels[0]; level < levels.size(); level++) {
                if(users[level]) {
                    users[level]?.keySet().forEach(tag -> {
                        for (int nextLevel = levels[1]; nextLevel <= levels.size(); nextLevel++) {
                            if(users[level][tag] && users[nextLevel][tag]) {
                                users[level][tag] = users[level][tag] - users[nextLevel][tag];
                            }
                        }
                    })
                }
            }
        }
    }
}
