/**
 * Copyright 2026 SkillTree
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
package skills.controller.exceptions

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import skills.PublicProps
import skills.controller.PublicPropsBasedValidator
import skills.controller.request.model.SkillRequest

@Slf4j
@Service
class SkillRequestValidator {

    @Value('#{"${skills.config.ui.maxTimeWindowInMinutes}"}')
    int maxTimeWindowInMinutes

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    void validateSkillRequest(String projectId, String skillId, SkillRequest skillRequest){
        validatePointIncrement(projectId, skillId, skillRequest.pointIncrement)
        validatePointIncrementInterval(projectId, skillId, skillRequest.pointIncrementInterval)
        validateNumPerformToCompletion(projectId, skillId, skillRequest.numPerformToCompletion)
        validateNumMaxOccurrencesIncrementInterval(projectId, skillId, skillRequest.pointIncrementInterval, skillRequest.numPerformToCompletion, skillRequest.numMaxOccurrencesIncrementInterval)
        validateVersion(projectId, skillId, skillRequest.version)
        validateDescription(skillRequest.description)
    }


    void validatePointIncrement(String projectId, String skillId, Integer pointIncrement) {
        SkillsValidator.isTrue(pointIncrement > 0, "pointIncrement must be > 0", projectId, skillId)
        propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxPointIncrement, "pointIncrement", pointIncrement)
    }

    void validatePointIncrementInterval(String projectId, String skillId, Integer pointIncrementInterval) {
        SkillsValidator.isTrue(pointIncrementInterval >= 0, "pointIncrementInterval must be >= 0", projectId, skillId)
        SkillsValidator.isTrue(pointIncrementInterval <= maxTimeWindowInMinutes, "pointIncrementInterval must be <= $maxTimeWindowInMinutes", projectId, skillId)
    }

    void validateNumPerformToCompletion(String projectId, String skillId, Integer numPerformToCompletion) {
        SkillsValidator.isTrue(numPerformToCompletion > 0, "numPerformToCompletion must be > 0", projectId, skillId)
        propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxNumPerformToCompletion, "numPerformToCompletion", numPerformToCompletion)
    }

    void validateNumMaxOccurrencesIncrementInterval(String projectId, String skillId, Integer pointIncrementInterval, Integer numPerformToCompletion, Integer numMaxOccurrencesIncrementInterval) {
        if (pointIncrementInterval > 0) {
            // if pointIncrementInterval is disabled then this validation is not needed
            SkillsValidator.isTrue(numMaxOccurrencesIncrementInterval > 0, "numMaxOccurrencesIncrementInterval must be > 0", projectId, skillId)
            SkillsValidator.isTrue(numPerformToCompletion >= numMaxOccurrencesIncrementInterval, "numPerformToCompletion must be >= numMaxOccurrencesIncrementInterval", projectId, skillId)
            propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxNumPointIncrementMaxOccurrences, "numMaxOccurrencesIncrementInterval", numMaxOccurrencesIncrementInterval)
        }
    }

    void validateVersion(String projectId, String skillId, Integer version) {
        SkillsValidator.isTrue(version >= 0, "version must be >= 0", projectId, skillId)
        propsBasedValidator.validateMaxIntValue(PublicProps.UiProp.maxSkillVersion, "Skill Version", version)
    }

    void validateDescription(String description) {
        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.descriptionMaxLength, "Skill Description", description)
    }

}
