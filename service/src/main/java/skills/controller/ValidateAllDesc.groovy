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
import skills.storage.model.ProjDefWithDescription
import skills.storage.model.QuizDefWithDescription
import skills.storage.model.QuizQuestionDef
import skills.storage.model.Setting
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefWithExtra
import skills.storage.repos.ProjDefWithDescriptionRepo
import skills.storage.repos.QuizDefWithDescRepo
import skills.storage.repos.QuizQuestionDefRepo
import skills.storage.repos.SettingRepo
import skills.storage.repos.SkillDefWithExtraRepo

import java.util.concurrent.atomic.AtomicInteger
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

    @Autowired
    SettingRepo settingRepo

    @Autowired
    ProjDefWithDescriptionRepo projDefWithDescriptionRepo

    @Autowired
    QuizDefWithDescRepo quizDefWithDescRepo

    @Autowired
    QuizQuestionDefRepo quizQuestionDefRepo

    String settingGroup = "DescValidationFailed"

    @Transactional
    void validateAllDesc() {

        AtomicInteger numProcessed = new AtomicInteger(0)
        AtomicInteger numPassed = new AtomicInteger(0)
        AtomicInteger numFailed = new AtomicInteger(0)

        settingRepo.scanSettingsByGroup(settingGroup).withCloseable { Stream<Setting> stream ->
            stream.forEach({ Setting setting ->
                String projectId = setting.projectId
                String skillId = setting.setting

                if (!projectId) {
                    assert setting.value == SkillDef.ContainerType.GlobalBadge.toString()
                }

                SkillDefWithExtra skillDef = projectId ?
                        skillDefWithExtraRepo.findByProjectIdAndSkillId(projectId, skillId)
                        : skillDefWithExtraRepo.findBySkillId(skillId)
                processSkillDef(skillDef, numProcessed, numPassed, numFailed, false)
            })
        }

        if (numProcessed.get() == 0) {
            skillDefWithExtraRepo.findAllExcludingTypes([SkillDef.ContainerType.Tag]).withCloseable { Stream<SkillDefWithExtra> stream ->
                stream.forEach({ SkillDefWithExtra skillDef ->
                    processSkillDef(skillDef, numProcessed, numPassed, numFailed)
                })
            }
        }

        log.info("Done!! Processed [${numProcessed.get()}] pass=[${numPassed.get()}] fail=[${numFailed.get()}]")
    }

    private void processSkillDef(SkillDefWithExtra skillDef, AtomicInteger numProcessed, AtomicInteger numPassed, AtomicInteger numFailed, boolean saveSetting = true) {
        if (skillDef?.projectId != "Inception") {
            numProcessed.incrementAndGet()

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
                        numPassed.incrementAndGet()
                        if (!saveSetting) {
                            settingRepo.deleteBySettingAndType(skillDef.skillId, Setting.SettingType.Global)
                        }
                    } else {
                        numFailed.incrementAndGet()
                        log.info("Failed validation for [${skillDef.skillId}], projectId=[${projectId}], validationFailedDetails=[${vr.validationFailedDetails}]")
                        if (saveSetting) {
                            settingRepo.save(new Setting(settingGroup: settingGroup, setting: "${skillDef.skillId}", projectId: projectId, value: skillDef.type.toString(), type: Setting.SettingType.Global))
                        }
                    }
                } catch (Throwable t) {
                    numFailed.incrementAndGet()
                    log.info("Failed validation with an excepton for [${skillDef.skillId}], projectId=[${projectId}]")
                    if (saveSetting) {
                        settingRepo.save(new Setting(settingGroup: settingGroup, setting: "${skillDef.skillId}", projectId: projectId, value: skillDef.type.toString()))
                    }
                }
            }
            if (numProcessed % 100 == 0) {
                log.info("Processed [${numProcessed.get()}] pass=[${numPassed.get()}] fail=[${numFailed.get()}]")
            }
        }
    }

    @Transactional
    void validateProjDesc() {
        AtomicInteger numProcessed = new AtomicInteger(0)
        AtomicInteger numPassed = new AtomicInteger(0)
        AtomicInteger numFailed = new AtomicInteger(0)
        projDefWithDescriptionRepo.streamAll().withCloseable { Stream<ProjDefWithDescription> stream ->
            stream.forEach({ ProjDefWithDescription projDef ->
                if (projDef?.projectId != "Inception") {
                    numProcessed.incrementAndGet()
                    try {
                        CustomValidationResult vr = customValidator.validateDescription(projDef.description, projDef.projectId, false, null)
                        if (vr.valid) {
                            numPassed.incrementAndGet()
                        } else {
                            numFailed.incrementAndGet()
                            log.info("Failed validation for proj desc [${projDef.projectId}], validationFailedDetails=[${vr.validationFailedDetails}]")
                        }
                    } catch (Throwable t) {
                        numFailed.incrementAndGet()
                        log.info("Failed validation with an excepton proj desc [${projDef.projectId}], exception=[${t.message}]")
                    }

                    if (numProcessed % 100 == 0) {
                        log.info("Processed Processed [${numProcessed.get()}] pass=[${numPassed.get()}] fail=[${numFailed.get()}]")
                    }

                }
            })
        }

        log.info("Done!! Processed [${numProcessed.get()}] pass=[${numPassed.get()}] fail=[${numFailed.get()}]")
    }

    @Transactional
    void validateQuizDesc() {
        AtomicInteger numProcessed = new AtomicInteger(0)
        AtomicInteger numPassed = new AtomicInteger(0)
        AtomicInteger numFailed = new AtomicInteger(0)
        quizDefWithDescRepo.streamAll().withCloseable { Stream<QuizDefWithDescription> stream ->
            stream.forEach({ QuizDefWithDescription quizDefWithDescription ->
                    numProcessed.incrementAndGet()
                    try {
                        CustomValidationResult vr = customValidator.validateDescription(quizDefWithDescription.description, null, false, quizDefWithDescription.quizId)
                        if (vr.valid) {
                            numPassed.incrementAndGet()
                        } else {
                            numFailed.incrementAndGet()
                            log.info("Failed validation for quiz desc [${quizDefWithDescription.quizId}], validationFailedDetails=[${vr.validationFailedDetails}]")
                        }
                    } catch (Throwable t) {
                        numFailed.incrementAndGet()
                        log.info("Failed validation with an excepton quiz desc [${quizDefWithDescription.quizId}], exception=[${t.message}]")
                    }

                    if (numProcessed % 100 == 0) {
                        log.info("Processed Processed [${numProcessed.get()}] pass=[${numPassed.get()}] fail=[${numFailed.get()}]")
                    }

            })
        }

        log.info("Done!! Processed [${numProcessed.get()}] pass=[${numPassed.get()}] fail=[${numFailed.get()}]")
    }

    @Transactional
    void validateQuizQuestions() {
        AtomicInteger numProcessed = new AtomicInteger(0)
        AtomicInteger numPassed = new AtomicInteger(0)
        AtomicInteger numFailed = new AtomicInteger(0)
        quizQuestionDefRepo.streamAll().withCloseable { Stream<QuizQuestionDef> stream ->
            stream.forEach({ QuizQuestionDef questionDef ->
                numProcessed.incrementAndGet()
                try {
                    CustomValidationResult vr = customValidator.validateDescription(questionDef.question, null, false, questionDef.quizId)
                    if (vr.valid) {
                        numPassed.incrementAndGet()
                    } else {
                        numFailed.incrementAndGet()
                        log.info("Failed validation for question desc qId=[${questionDef.id}], quizId=[${questionDef.quizId}], validationFailedDetails=[${vr.validationFailedDetails}]")
                    }
                } catch (Throwable t) {
                    numFailed.incrementAndGet()
                    log.info("Failed validation with an question desc qId=[${questionDef.id}], quizId=[${questionDef.quizId}], exception=[${t.message}]")
                }

                if (numProcessed % 100 == 0) {
                    log.info("Processed Processed [${numProcessed.get()}] pass=[${numPassed.get()}] fail=[${numFailed.get()}]")
                }

            })
        }

        log.info("Done!! Processed [${numProcessed.get()}] pass=[${numPassed.get()}] fail=[${numFailed.get()}]")
    }


}
