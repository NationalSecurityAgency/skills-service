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
package skills.services.quiz

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.PublicProps
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.PublicPropsBasedValidator
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillQuizException
import skills.controller.request.model.ProjectRequest
import skills.controller.request.model.QuizDefRequest
import skills.controller.result.model.QuizDefResult
import skills.services.AccessSettingsStorageService
import skills.services.CreatedResourceLimitsValidator
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.services.IdFormatValidator
import skills.services.LockingService
import skills.services.admin.DataIntegrityExceptionHandlers
import skills.services.admin.ServiceValidatorHelper
import skills.storage.model.ProjDef
import skills.storage.model.ProjDefWithDescription
import skills.storage.model.QuizDef
import skills.storage.model.QuizDefWithDescription
import skills.storage.model.auth.RoleName
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizDefWithDescRepo
import skills.utils.ClientSecretGenerator
import skills.utils.InputSanitizer
import skills.utils.Props

@Service
@Slf4j
class QuizDefService {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    LockingService lockingService

    @Autowired
    CustomValidator customValidator

    @Autowired
    QuizDefWithDescRepo quizDefWithDescRepo

    @Autowired
    ServiceValidatorHelper serviceValidatorHelper

    @Autowired
    CreatedResourceLimitsValidator createdResourceLimitsValidator

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    PublicPropsBasedValidator propsBasedValidator

    @Transactional(readOnly = true)
    List<QuizDefResult> getCurrentUsersTestDefs() {
        boolean isRoot = userInfoService.isCurrentUserASuperDuperUser()

        UserInfo userInfo = userInfoService.currentUser
        String userId = userInfo.username?.toLowerCase()
        List<QuizDefResult> res = []

        List<QuizDefRepo.QuizDefSummaryResult> fromDb = quizDefRepo.getQuizDefSummariesByUser(userId)
        if (fromDb) {
            res.addAll(fromDb.collect {convert(it) })
        }


//        Map<String, Integer> projectIdSortOrder = sortingService.getUserProjectsOrder(userId)
//        List<ProjectResult> finalRes
//        if (isRoot) {
//            finalRes = loadProjectsForRoot(projectIdSortOrder, userId)
//        } else {
//            // sql join with UserRoles and there is 1-many relationship that needs to be normalized
//            List<ProjSummaryResult> projects = projDefRepo.getProjectSummariesByUser(userId)
//            finalRes = projects?.unique({ it.projectId })?.collect({
//                ProjectResult res = convert(it, projectIdSortOrder)
//                return res
//            })
//        }
//
//        finalRes.sort() { it.displayOrder }
//
//        if (finalRes) {
//            finalRes.first().isFirst = true
//            finalRes.last().isLast = true

        return res
    }

    @Transactional(readOnly = true)
    QuizDefResult getQuizDef(String quizId) {
        assert quizId
        QuizDefWithDescription quizDefWithDescription = quizDefWithDescRepo.findByQuizIdIgnoreCase(quizId)
        return convert(quizDefWithDescription)
    }
    @Transactional()
    QuizDefResult saveQuizDef(String originalQuizId, String newQuizId, QuizDefRequest quizDefRequest, String userIdParam = null) {
        validateQuizDefRequest(newQuizId, quizDefRequest)

        newQuizId = InputSanitizer.sanitize(newQuizId)
        quizDefRequest.name = InputSanitizer.sanitize(quizDefRequest.name)?.trim()
        quizDefRequest.description = StringUtils.trimToNull(InputSanitizer.sanitize(quizDefRequest.description))

        lockingService.lockQuizDefs()

        QuizDefWithDescription quizDefWithDescription = originalQuizId ? quizDefWithDescRepo.findByQuizIdIgnoreCase(originalQuizId) : null
        if (!quizDefWithDescription || !quizDefWithDescription.quizId.equalsIgnoreCase(originalQuizId)) {
            serviceValidatorHelper.validateQuizIdDoesNotExist(newQuizId)
        }
        if (!quizDefWithDescription || !quizDefWithDescription.name.equalsIgnoreCase(quizDefWithDescription.name)) {
            serviceValidatorHelper.validateQuizNameDoesNotExist(quizDefRequest.name, newQuizId)
        }
        if (quizDefWithDescription) {
            Props.copy(quizDefRequest, quizDefWithDescription)
            log.debug("Updating [{}]", quizDefWithDescription)

            DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(null, null, quizDefWithDescription.quizId) {
                quizDefWithDescription = quizDefWithDescRepo.save(quizDefWithDescription)
            }
            log.debug("Saved [{}]", quizDefWithDescription)
        } else {
            quizDefWithDescription = new QuizDefWithDescription(quizId: newQuizId, name: quizDefRequest.name,
                    description: quizDefRequest.description)
            log.debug("Created project [{}]", quizDefWithDescription)

            String userId = userIdParam ?: userInfoService.getCurrentUserId()

            if (!userInfoService.isCurrentUserASuperDuperUser()) {
                createdResourceLimitsValidator.validateNumQuizDefsCreated(userId)
            }

            DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(null, null, quizDefWithDescription.quizId) {
                quizDefWithDescription = quizDefWithDescRepo.save(quizDefWithDescription)
            }

            log.debug("Saved [{}]", quizDefWithDescription)

            accessSettingsStorageService.addQuizDefUserRole(userId, newQuizId, RoleName.ROLE_QUIZ_ADMIN)
        }

        QuizDef updatedDef = quizDefRepo.findByQuizIdIgnoreCase(quizDefWithDescription.quizId)
        return convert(updatedDef)
    }

    @Transactional()
    void deleteQuiz(String quizId) {
        int numRemoved = quizDefRepo.deleteByQuizIdIgnoreCase(quizId)
        log.debug("Deleted project with id [{}]. Removed [{}] record", quizId, numRemoved)
    }

    @Transactional(readOnly = true)
    boolean existsByQuizId(String quizId) {
        return quizDefRepo.existsByQuizIdIgnoreCase(quizId)
    }

    @Transactional(readOnly = true)
    boolean existsByQuizName(String quizName) {
        return quizDefRepo.existsByNameIgnoreCase(quizName)
    }


    private void validateQuizDefRequest(String quizId, QuizDefRequest quizDefRequest) {
        IdFormatValidator.validate(quizId)
        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxIdLength, "QuizId Id", quizId)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minIdLength, "QuizId Id", quizId)

        propsBasedValidator.validateMaxStrLength(PublicProps.UiProp.maxQuizNameLength, "Quiz Name", quizDefRequest.name)
        propsBasedValidator.validateMinStrLength(PublicProps.UiProp.minNameLength, "Quiz Name", quizDefRequest.name)

        if (!quizDefRequest?.name) {
            throw new SkillQuizException("Quiz name was not provided.", quizId, ErrorCode.BadParam)
        }

        CustomValidationResult customValidationResult = customValidator.validate(quizDefRequest)
        if (!customValidationResult.valid) {
            throw new SkillQuizException(customValidationResult.msg, quizId, ErrorCode.BadParam)
        }
    }

    private QuizDefResult convert(QuizDefRepo.QuizDefSummaryResult quizDefSummaryResult) {
        QuizDefResult result = Props.copy(quizDefSummaryResult, new QuizDefResult())
        result.displayOrder = 0 // todo
        return result
    }

    private QuizDefResult convert(QuizDef updatedDef) {
        QuizDefResult result = Props.copy(updatedDef, new QuizDefResult())
        result.displayOrder = 0 // todo
        return result
    }

    private QuizDefResult convert(QuizDefWithDescription updatedDef) {
        QuizDefResult result = Props.copy(updatedDef, new QuizDefResult())
        result.displayOrder = 0 // todo
        return result
    }
}
