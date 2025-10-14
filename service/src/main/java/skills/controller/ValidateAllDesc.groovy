/**
 * Copyright 2025 SkillTree
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
package skills.controller

import groovy.util.logging.Slf4j
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.services.admin.UserCommunityService
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefWithExtra
import skills.storage.repos.SkillDefWithExtraRepo

import java.util.stream.Stream

@Component
@Slf4j
class ValidateAllDesc {

    @Autowired
    CustomValidator customValidator

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    UserCommunityService userCommunityService

    @Transactional
    void validateAllDesc() {
        int numProcessed = 0
        int numPassed = 0
        int numFailed = 0
        skillDefWithExtraRepo.findAllExcludingTypes([SkillDef.ContainerType.Tag]).withCloseable { Stream<SkillDefWithExtra> stream ->
            stream.forEach({ SkillDefWithExtra skillDef ->
                if (skillDef?.projectId != "Inception") {
                    numProcessed++

                    if (skillDef.description) {
                        String projectId = null
                        Boolean shouldUseProtectedCommunityValidator = false
                        String quizId = null

                        if (skillDef.type == SkillDef.ContainerType.GlobalBadge) {
                            shouldUseProtectedCommunityValidator = userCommunityService.isUserCommunityOnlyGlobalBadge(skillDef.skillId)
                        }

                        if (skillDef.type == SkillDef.ContainerType.Skill
                                || skillDef.type == SkillDef.ContainerType.Badge
                                || skillDef.type == SkillDef.ContainerType.SkillsGroup
                                || skillDef.type == SkillDef.ContainerType.Subject) {
                            projectId = skillDef.projectId
                        }

                        try {
                            CustomValidationResult vr = customValidator.validateDescription(skillDef.description, projectId, shouldUseProtectedCommunityValidator, quizId)
                            if (vr.valid) {
                                numPassed++
                            } else {
                                numFailed++
                                log.info("Failed validation for [${skillDef.skillId}], projectId=[${projectId}], validationFailedDetails=[${vr.validationFailedDetails}]")
                            }
                        } catch (Throwable t) {
                            numFailed++
                            log.info("Failed validation with an excepton for [${skillDef.skillId}], projectId=[${projectId}]")
                        }
                    }
                    if (numProcessed % 100 == 0) {
                        log.info("Processed [$numProcessed] pass=[$numPassed] fail=[$numFailed]")
                    }
                }
            })
        }

        log.info("Done!! Processed [$numProcessed] pass=[$numPassed] fail=[$numFailed]")
    }
}
