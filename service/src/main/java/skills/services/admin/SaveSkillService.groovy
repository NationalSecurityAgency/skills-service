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
package skills.services.admin

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.controller.request.model.SkillRequest
import skills.tasks.TaskSchedulerService

@Service
@Slf4j
class SaveSkillService {

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    TaskSchedulerService taskSchedulerService

    void saveSkillAndSchedulePropagationToImportedSkills(String originalSkillId, SkillRequest skillRequest, boolean performCustomValidation=true, String groupId=null) {
        SkillsAdminService.SaveSkillTmpRes saveSkillTmpRes =
                skillsAdminService.saveSkill(originalSkillId, skillRequest, performCustomValidation, groupId)
        if (saveSkillTmpRes.isImportedByOtherProjects) {
            // IMPORTANT: must schedule once transaction that saves skill committed; logic in the async propagation relies on the completion of the original skill modification
            taskSchedulerService.scheduleCatalogSkillUpdate(saveSkillTmpRes.projectId, saveSkillTmpRes.skillId, saveSkillTmpRes.skillRefId)
        }
    }
}
