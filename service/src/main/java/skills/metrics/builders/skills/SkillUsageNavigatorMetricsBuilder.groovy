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
package skills.metrics.builders.skills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.metrics.builders.ProjectMetricsBuilder
import skills.services.admin.SkillTagService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.skillLoading.model.SkillTag
import skills.storage.repos.UserAchievedLevelRepo
import skills.utils.InputSanitizer

@Component
@Slf4j
class SkillUsageNavigatorMetricsBuilder implements ProjectMetricsBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    SkillTagService skillTagService

    @Override
    String getId() {
        return "skillUsageNavigatorChartBuilder"
    }

    static class SkillUsageNavigatorItem {
        String skillId
        String skillName
        String subjectId
        Integer numUserAchieved
        Integer numUsersInProgress
        Long lastReportedTimestamp
        Long lastAchievedTimestamp
        Boolean isReusedSkill
        List <SkillTag> skillTags
    }

    def build(String projectId, String chartId, Map<String, String> props) {
        def res = userAchievedRepo.findAllForSkillsNavigator(projectId)
        def skillTags = skillTagService.getTagsForProject(projectId)
        def results = res.collect {
            Integer numAchieved = it.getNumUserAchieved() ?: 0
            Integer numProgress = it.getNumUsersInProgress() ?: 0
            List <SkillTag> tags = skillTagService.getTagsForSkill(projectId, it.getSkillId())
            new SkillUsageNavigatorItem(
                    skillId: it.getSkillId(),
                    skillName: SkillReuseIdUtil.removeTag(InputSanitizer.unsanitizeName(it.getSkillName())),
                    subjectId: it.getSubjectId(),
                    numUserAchieved: numAchieved,
                    numUsersInProgress: numProgress - numAchieved,
                    lastReportedTimestamp: it.getLastReported()?.time,
                    lastAchievedTimestamp: it.getLastAchieved()?.time,
                    isReusedSkill: SkillReuseIdUtil.isTagged(it.skillId),
                    skillTags: tags
            )
        }?.sort({ it.skillId })

        return [ skills: results, tags: skillTags ]
    }
}
