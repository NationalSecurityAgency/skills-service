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
package skills.skillLoading

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import skills.controller.result.model.TableResult
import skills.skillLoading.model.SkillPreviewItem
import skills.storage.repos.SkillDefRepo

@Component
@CompileStatic
@Slf4j
class SkillsService {

    @Autowired
    SkillDefRepo skillDefRepo

    List<SkillDefRepo.SkillWithAchievementDetails> getAllSkillsSubjectsAndBadgesWithAchievementDetails(String projectId, String userId) {
        return skillDefRepo.findAllSkillsSubjectsAndBadgesWithAchievementDetails(projectId, userId)
    }
}
