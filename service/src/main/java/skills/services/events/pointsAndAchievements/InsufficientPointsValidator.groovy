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
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillExceptionBuilder

@Component
@Slf4j
@CompileStatic
class InsufficientPointsValidator {

    @Value('#{"${skills.config.ui.minimumSubjectPoints}"}')
    int minimumSubjectPoints

    @Value('#{"${skills.config.ui.minimumProjectPoints}"}')
    int minimumProjectPoints

    void validateProjectPoints(int projDefPoints, String projectId, String userId = null) {
        if (projDefPoints < minimumProjectPoints) {
            SkillExceptionBuilder builder = new SkillExceptionBuilder()
                    .msg("Insufficient project points, skill achievement is disallowed")
                    .projectId(projectId)
                    .errorCode(ErrorCode.InsufficientProjectPoints)
            if (userId) {
                builder.userId(userId)
            }
            throw builder.build()
        }
    }

    void validateSubjectPoints(int subjectDefPoints, String projectId, String userId = null) {
        if (subjectDefPoints < minimumSubjectPoints) {
            SkillExceptionBuilder builder = new SkillExceptionBuilder()
                    .msg("Insufficient Subject points, skill achievement is disallowed")
                    .projectId(projectId)
                    .errorCode(ErrorCode.InsufficientSubjectPoints)
            if (userId) {
                builder.userId(userId)
            }
            throw builder.build()
        }
    }
}
