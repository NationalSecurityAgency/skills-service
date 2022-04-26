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
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillExceptionBuilder
import skills.services.RuleSetDefGraphService
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo

@Component
@Slf4j
@CompileStatic
class InsufficientPointsValidator {

    @Value('#{"${skills.config.ui.minimumSubjectPoints}"}')
    int minimumSubjectPoints

    @Value('#{"${skills.config.ui.minimumProjectPoints}"}')
    int minimumProjectPoints

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    RuleSetDefGraphService relationshipService

    void validateProjectPoints(int projDefPoints, String projectId, String userId = null, String explanation = ", skill achievement is disallowed") {
        if (projDefPoints < minimumProjectPoints) {
            SkillExceptionBuilder builder = new SkillExceptionBuilder()
                    .msg("Insufficient project points${explanation}")
                    .projectId(projectId)
                    .errorCode(ErrorCode.InsufficientProjectPoints)
            if (userId) {
                builder.userId(userId)
            }
            throw builder.build()
        }
    }

    void validateSubjectPoints(int subjectDefPoints, String projectId, String subjectId, String userId = null, String explanation = ", skill achievement is disallowed") {
        if (subjectDefPoints < minimumSubjectPoints) {
            SkillExceptionBuilder builder = new SkillExceptionBuilder()
                    .msg("Insufficient Subject points${explanation}")
                    .projectId(projectId)
                    .skillId(subjectId)
                    .errorCode(ErrorCode.InsufficientSubjectPoints)
            if (userId) {
                builder.userId(userId)
            }
            throw builder.build()
        }
    }

    boolean hasSufficientProjectPoints(String projectId) {
        ProjDef projDef = projDefRepo.findByProjectId(projectId)
        return projDef.totalPoints >= minimumProjectPoints;
    }

    boolean hasSufficientSubjectPointByProjectAndSkillId(String projectId, String skillId) {
        SkillDef skill = skillDefRepo.findByProjectIdAndSkillId(projectId, skillId)
        SkillDef subject = relationshipService.getMySubjectParent(skill.id)
        return subject.totalPoints >= minimumSubjectPoints
    }

}
