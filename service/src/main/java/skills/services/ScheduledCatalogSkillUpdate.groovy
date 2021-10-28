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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import skills.services.admin.SkillCatalogService

@Slf4j
@Component
class ScheduledCatalogSkillUpdate {

    @Autowired
    SkillCatalogService skillCatalogService

    @Scheduled(cron='#{"${skills.config.updateImportedSkills:* */3 * * * *}"}')
    public void updateImportedSkills() {
        log.info("propagating updates to catalog skills to the imported skill instances")
        skillCatalogService.distributeCatalogSkillUpdates()
        log.info("done")
    }
}
