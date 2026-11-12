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
package skills.metrics

import groovy.json.JsonException
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillsValidator

@Component
@Slf4j
class UserTagMetricsValidator {

    @Value('${skills.config.ui.projectMetricsTagCharts:}')
    String projectMetricsTagCharts

    @Value('${skills.config.ui.overallMetricsTagCharts:}')
    String overallMetricsTagCharts

    @Value('${skills.config.ui.usersTableAdditionalUserTagKey:}')
    String usersTableAdditionalUserTagKey

    @Value('${skills.config.ui.userPageTagsToDisplay:}')
    String userPageTagsToDisplay

    private Set<String> projectMetricsKeys
    private Set<String> overallMetricsKeys
    private Set<String> drillDownKeys

    @PostConstruct
    void init() {
        projectMetricsKeys = chartKeys(projectMetricsTagCharts, 'projectMetricsTagCharts').asImmutable()
        overallMetricsKeys = chartKeys(overallMetricsTagCharts, 'overallMetricsTagCharts').asImmutable()

        Set<String> allowedDrillDownKeys = new HashSet<>(projectMetricsKeys)
        if (StringUtils.isNotBlank(usersTableAdditionalUserTagKey)) {
            allowedDrillDownKeys.add(usersTableAdditionalUserTagKey)
        }
        if (StringUtils.isNotBlank(userPageTagsToDisplay)) {
            userPageTagsToDisplay.split('\\|', -1).each { String section ->
                String[] parts = section.split('/', -1)
                // Match the disclosure policy used by AdminUsersService.getUserTagsForProject.
                if (parts.length == 2 && StringUtils.isNotBlank(parts[0]) && StringUtils.isNotBlank(parts[1])) {
                    allowedDrillDownKeys.add(parts[0])
                }
            }
        }
        drillDownKeys = allowedDrillDownKeys.asImmutable()
    }

    void validateProjectMetricsKey(String key, String projectId, String skillId = null) {
        validateKey(key, projectMetricsKeys, 'project tag metrics', projectId, skillId)
    }

    void validateOverallMetricsKey(String key) {
        validateKey(key, overallMetricsKeys, 'overall tag metrics')
    }

    void validateDrillDownKey(String key, String projectId) {
        validateKey(key, drillDownKeys, 'tag metrics drill-down', projectId)
    }

    void validateQuizMetricsKey(String key, String quizId) {
        QuizValidator.isNotBlank(key, 'User Tag Key', quizId)
        QuizValidator.isTrue(StringUtils.isNotBlank(usersTableAdditionalUserTagKey) && key == usersTableAdditionalUserTagKey,
                "Tag key [${key}] is not configured for quiz tag metrics", quizId)
    }

    private static void validateKey(String key, Set<String> allowedKeys, String scope, String projectId = null, String skillId = null) {
        SkillsValidator.isNotBlank(key, 'Tag Key', projectId, skillId)
        SkillsValidator.isTrue(allowedKeys.contains(key), "Tag key [${key}] is not configured for ${scope}", projectId, skillId)
    }

    private static Set<String> chartKeys(String charts, String propertyName) {
        Set<String> keys = [] as Set<String>
        if (StringUtils.isBlank(charts)) {
            return keys
        }
        Object parsedCharts
        try {
            parsedCharts = new JsonSlurper().parseText(charts)
        } catch (JsonException ignored) {
            log.warn('Invalid skills.config.ui.{} JSON; no tag metrics keys are allowed for this configuration', propertyName)
            return keys
        }
        if (!(parsedCharts instanceof List)) {
            log.warn('skills.config.ui.{} must be a JSON array; no tag metrics keys are allowed for this configuration', propertyName)
            return keys
        }
        parsedCharts.each { Object chart ->
            if (chart instanceof Map && chart.key instanceof String && StringUtils.isNotBlank(chart.key)) {
                keys.add(chart.key)
            }
        }
        return keys
    }
}
