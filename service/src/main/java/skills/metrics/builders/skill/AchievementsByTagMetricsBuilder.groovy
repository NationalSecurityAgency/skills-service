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
package skills.metrics.builders.skill

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import skills.metrics.builders.MetricsParams
import skills.metrics.builders.ProjectMetricsBuilder
import skills.storage.model.UserTagCount
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo
import static org.springframework.data.domain.Sort.Direction.ASC

@Component
@Slf4j
class AchievementsByTagMetricsBuilder implements ProjectMetricsBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Override
    String getId() {
        return "skillAchievementsByTagBuilder"
    }

    static class UserTagCounts {
        Integer numberAchieved
        Integer numberInProgress
    }


    @Override
    Object build(String projectId, String chartId, Map<String, String> props) {
        String skillId = MetricsParams.getSkillId(projectId, chartId, props);
        PageRequest pageRequest = PageRequest.of(0, 20 )

        def skillsPerformed = userPerformedSkillRepo.findAllByProjectIdAndSkillIdAndUserTag(projectId, skillId, props.userTagKey, pageRequest)
        def skillsAchieved = userAchievedRepo.countNumAchievedByUserTag(projectId, skillId, props.userTagKey)

        def tagMap = new HashMap<String, UserTagCounts>()
        skillsPerformed.forEach( skill -> {
            def tag = new UserTagCounts()
            tag.numberInProgress = skill.userCount
            tag.numberAchieved = 0
            def achieved = skillsAchieved.find( it -> it.tagValue == skill.tagValue )
            if(achieved) {
                tag.numberInProgress -= achieved.userCount
                tag.numberAchieved = achieved.userCount
            }
            tagMap[skill.tagValue] = tag
        })
        return tagMap
    }
}
