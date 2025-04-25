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

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.math.NumberUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillQuizException
import skills.controller.request.model.QuizDefRequest
import skills.controller.request.model.QuizPreference
import skills.controller.request.model.QuizSettingsRequest
import skills.controller.result.model.QuizPreferenceRes
import skills.controller.result.model.QuizSettingsRes
import skills.controller.result.model.SettingsResult
import skills.quizLoading.QuizSettings
import skills.quizLoading.QuizUserPreferences
import skills.services.admin.UserCommunityService
import skills.services.settings.Settings
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.QuizSetting
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizQuestionDefRepo
import skills.storage.repos.QuizSettingsRepo
import skills.storage.repos.UserAttrsRepo

@Service
@Slf4j
class QuizSettingsService {

    @Autowired
    QuizSettingsRepo quizSettingsRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizQuestionDefRepo quizQuestionDefRepo

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Transactional
    void copySettings(String fromQuizId, String toQuizId, boolean enableProtectedUserCommunity) {
        List<QuizSettingsRes> fromSettings = getSettings(fromQuizId, false)
        List<QuizSettingsRequest> toSettings = new ArrayList<QuizSettingsRequest>()

        if (enableProtectedUserCommunity) {
            fromSettings = fromSettings.findAll { it.setting != QuizSettings.UserCommunityOnlyQuiz.setting }
            toSettings.add(new QuizSettingsRequest(value: Boolean.TRUE.toString(), setting: QuizSettings.UserCommunityOnlyQuiz.setting))
        }
        fromSettings.forEach( setting -> {
            QuizSettingsRequest request = new QuizSettingsRequest(value: setting.value, setting: setting.setting)
            toSettings.add(request)
        })

        saveSettings(toQuizId, toSettings)
    }

    @Transactional
    void saveSettings(String quizId, List<QuizSettingsRequest> settingsRequests, boolean documentUserActions = true) {
        Integer quizRefId = getQuizDefRefId(quizId)
        settingsRequests.each {
            validateProvidedQuizSetting(quizId, it)

            QuizSetting existing = quizSettingsRepo.findBySettingAndQuizRefId(it.setting, quizRefId)
            if (existing) {
                existing.value = it.value
                quizSettingsRepo.save(existing)
            } else {
                quizSettingsRepo.save(new QuizSetting(setting: it.setting, value: it.value, quizRefId: quizRefId))
            }

            if (documentUserActions) {
                userActionsHistoryService.saveUserAction(new UserActionInfo(
                        action: DashboardAction.Create, item: DashboardItem.Settings,
                        itemId: quizId, quizId: quizId,
                        actionAttributes: [
                                setting: it.setting,
                                value  : it.value,
                        ]
                ))
            }
        }
    }

    @Transactional
    void saveUserPreference(String quizId, String preferenceKey, QuizPreference quizPreference) {
        Integer quizRefId = getQuizDefRefId(quizId)

        List<String> availablePreferenceKeys = QuizUserPreferences.values().collect { it.preference }
        if (!availablePreferenceKeys.contains(preferenceKey)) {
            throw new SkillQuizException("Provided preferenceKey [${preferenceKey}] is not a valid setting. Available settings: ${availablePreferenceKeys}", quizId, ErrorCode.BadParam)
        }
        UserAttrs currentUserAttrs = getCurrentUserAttrs()
        QuizSetting existing = quizSettingsRepo.findBySettingAndQuizRefIdAndUserRefId(preferenceKey, quizRefId, currentUserAttrs.id)
        if (existing) {
            existing.value = quizPreference.value
            quizSettingsRepo.save(existing)
        } else {
            quizSettingsRepo.save(new QuizSetting(setting: preferenceKey, value: quizPreference.value, quizRefId: quizRefId, userRefId: currentUserAttrs.id))
        }

        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: DashboardAction.Create, item: DashboardItem.Settings,
                itemId: quizId, quizId: quizId,
                actionAttributes: [
                        userId: currentUserAttrs.userIdForDisplay,
                        setting: preferenceKey,
                        value  : quizPreference.value,
                ]
        ))
    }

    @Transactional(readOnly = true)
    List<QuizPreferenceRes> getCurrentUserQuizPreferences(String quizId) {
        Integer quizRefId = getQuizDefRefId(quizId)
        UserAttrs currentUserAttrs = getCurrentUserAttrs()
        List<QuizSetting> quizSettings = quizSettingsRepo.findAllByQuizRefIdAndUserRefId(quizRefId, currentUserAttrs.id)
        List<QuizPreferenceRes> res = quizSettings.collect {
            new QuizPreferenceRes(preference: it.setting, value: it.value)
        } ?: []
        return res.sort({ it.preference })
    }

    private UserAttrs getCurrentUserAttrs() {
        UserInfo currentUser = userInfoService.getCurrentUser()
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(currentUser.username)
        return userAttrs
    }


    private void validateProvidedQuizSetting(String quizId, QuizSettingsRequest quizSettingsRequest) {
        QuizValidator.isNotBlank(quizSettingsRequest.setting, "settings.setting", quizId)
        QuizValidator.isNotBlank(quizSettingsRequest.value, "settings.value", quizId)

        if (quizSettingsRequest.setting == QuizSettings.MaxNumAttempts.setting || quizSettingsRequest.setting == QuizSettings.MinNumQuestionsToPass.setting) {
            if (!NumberUtils.isCreatable(quizSettingsRequest.value)) {
                throw new SkillQuizException("Provided value [${quizSettingsRequest.value}] for [${quizSettingsRequest.setting}] setting must be numeric", quizId, ErrorCode.BadParam)
            }

            Integer maxNumAttempts = Integer.valueOf(quizSettingsRequest.value)
            if (maxNumAttempts < -1) {
                throw new SkillQuizException("Provided value [${quizSettingsRequest.value}] for [${quizSettingsRequest.setting}] setting must be >= -1", quizId, ErrorCode.BadParam)
            }
        }
        if (quizSettingsRequest.setting == QuizSettings.MinNumQuestionsToPass.setting) {
            Integer minNumQuestionsToPass = Integer.valueOf(quizSettingsRequest.value)
            int numDeclaredQuestions = quizQuestionDefRepo.countByQuizId(quizId)
            if (numDeclaredQuestions == 0) {
                throw new SkillQuizException("Cannot modify [${quizSettingsRequest.setting}] becuase there is 0 declared questions", quizId, ErrorCode.BadParam)
            }

            if (numDeclaredQuestions < minNumQuestionsToPass) {
                throw new SkillQuizException("Provided [${quizSettingsRequest.setting}] setting [${minNumQuestionsToPass}] must be less than [${numDeclaredQuestions}] declared questions.", quizId, ErrorCode.BadParam)
            }
        }

    }

    @Transactional(readOnly = true)
    List<QuizSettingsRes> getSettings(String quizId, boolean addUserCommunityOnlyQuizSetting = true) {
        Integer quizRefId = getQuizDefRefId(quizId)
        List<QuizSetting> quizSettings = quizSettingsRepo.findAllByQuizRefId(quizRefId)
        List<QuizSettingsRes> res = quizSettings.collect {
            new QuizSettingsRes(setting: it.setting, value: it.value, created: it.created, updated: it.updated)
        } ?: []

        UserInfo currentUser = userInfoService.getCurrentUser()
        List<String> usrRoles = currentUser.authorities.collect { it.authority.toUpperCase() }
        if (usrRoles.contains(RoleName.ROLE_QUIZ_ADMIN.toString())) {
            res.add(new QuizSettingsRes(setting: QuizSettings.QuizUserRole.setting, value: RoleName.ROLE_QUIZ_ADMIN.toString()))
        } else if (usrRoles.contains(RoleName.ROLE_QUIZ_READ_ONLY.toString())) {
            res.add(new QuizSettingsRes(setting: QuizSettings.QuizUserRole.setting, value: RoleName.ROLE_QUIZ_READ_ONLY.toString()))
        }

        if (addUserCommunityOnlyQuizSetting && userCommunityService.isUserCommunityConfigured()) {
            boolean isUserCommunityProtectedQuiz = quizSettings.find { it.setting == QuizSettings.UserCommunityOnlyQuiz.setting}?.isEnabled()
            res.add(new QuizSettingsRes(
                    setting: QuizSettings.UserCommunityOnlyQuiz.setting,
                    value: userCommunityService.getCommunityNameBasedOnConfAndItemStatus(isUserCommunityProtectedQuiz),
            ))
        }

        return res.sort({ it.setting })
    }

    @Profile
    private Integer getQuizDefRefId(String quizId) {
        Integer id = quizDefRepo.getQuizRefIdByQuizIdIgnoreCase(quizId)
        if (id == null) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return id
    }
}
