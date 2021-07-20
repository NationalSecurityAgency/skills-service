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
package skills.services.events.pointsAndAchievements

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.storage.model.SkillDef
import skills.storage.repos.SkillEventsSupportRepo

@Component
@Slf4j
@CompileStatic
class LoadedDataValidator {

    @Autowired
    InsufficientPointsValidator insufficientPointsValidator

    void validate(LoadedData loadedData) {
        insufficientPointsValidator.validateProjectPoints(loadedData.tinyProjectDef.totalPoints, loadedData.projectId, loadedData.userId)

        loadedData.parentDefs.each { SkillEventsSupportRepo.TinySkillDef parentSkillDef ->
            if (parentSkillDef.type == SkillDef.ContainerType.Subject) {
                insufficientPointsValidator.validateSubjectPoints(parentSkillDef.totalPoints, loadedData.projectId, loadedData.userId)
            }
        }
    }
}
