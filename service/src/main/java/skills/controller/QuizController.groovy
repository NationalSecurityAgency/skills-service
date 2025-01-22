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
package skills.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillQuizException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.QuizDefRequest
import skills.controller.request.model.QuizPreference
import skills.controller.request.model.QuizQuestionDefRequest
import skills.controller.request.model.QuizSettingsRequest
import skills.controller.result.model.*
import skills.quizLoading.QuizRunService
import skills.quizLoading.QuizSettings
import skills.quizLoading.model.*
import skills.services.adminGroup.AdminGroupService
import skills.services.quiz.QuizDefService
import skills.services.quiz.QuizRoleService
import skills.services.quiz.QuizSettingsService
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionsHistoryService
import skills.storage.model.UserQuizAttempt
import skills.storage.model.auth.RoleName
import skills.utils.TablePageUtil

import java.nio.charset.StandardCharsets

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@RestController
@RequestMapping("/admin/quiz-definitions")
@Slf4j
@skills.profile.EnableCallStackProf
class QuizController {

    @Autowired
    QuizDefService quizDefService

    @Autowired
    QuizRunService quizRunService

    @Autowired
    QuizSettingsService quizSettingsService

    @Autowired
    QuizRoleService quizRoleService

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Autowired
    AdminGroupService adminGroupService

    @RequestMapping(value = "/{quizId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    QuizDefResult saveQuizDef(@PathVariable("quizId") String quizId, @RequestBody QuizDefRequest quizDefRequest) {
        return quizDefService.saveQuizDef(quizId, quizDefRequest.quizId, quizDefRequest)
    }

    @RequestMapping(value = "/{quizId}/copy", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    QuizDefResult copyQuiz(@PathVariable("quizId") String quizId, @RequestBody QuizDefRequest quizDefRequest) {
        return quizDefService.copyQuiz(quizId, quizDefRequest.quizId, quizDefRequest)
    }

    @RequestMapping(value = "/{quizId}", method = RequestMethod.DELETE)
    void deleteQuiz(@PathVariable("quizId") String quizId) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        quizDefService.deleteQuiz(quizId)
    }

    @RequestMapping(value = "/{quizId}/validateEnablingCommunity", method = RequestMethod.GET, produces = "application/json")
    EnableUserCommunityValidationRes validateQuizForEnablingCommunity(@PathVariable("quizId") String quizId) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        return quizDefService.validateQuizForEnablingCommunity(quizId)
    }

    @RequestMapping(value = "/{quizId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    QuizDefResult getQuizDef(@PathVariable("quizId") String quizId) {
        return quizDefService.getQuizDef(quizId)
    }

    @RequestMapping(value = "/{quizId}/skills-count", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    Integer countSkillsForQuiz(@PathVariable("quizId") String quizId) {
        return quizDefService.countNumSkillsQuizAssignedTo(quizId)
    }

    @RequestMapping(value = "/{quizId}/skills/", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<QuizSkillResult> getSkillsForQuiz(@PathVariable("quizId") String quizId, @RequestParam String userId) {
        return quizDefService.getSkillsForQuiz(quizId, userId)
    }

    @RequestMapping(value = "/{quizId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    QuizDefSummaryResult getQuizSummary(@PathVariable("quizId") String quizId) {
        return quizDefService.getQuizDefSummary(quizId)
    }

    @RequestMapping(value = "/{quizId}/create-question", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    QuizQuestionDefResult saveQuestionDef(@PathVariable("quizId") String quizId,
                                          @RequestBody QuizQuestionDefRequest questionDefRequest) {
        return quizDefService.saveQuestion(quizId, questionDefRequest)
    }

    @RequestMapping(value = "/{quizId}/questions/{questionId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    QuizQuestionDefResult updateQuestionDef(@PathVariable("quizId") String quizId,
                                            @PathVariable("questionId") Integer questionId,
                                            @RequestBody QuizQuestionDefRequest questionDefRequest) {
        return quizDefService.saveQuestion(quizId, questionDefRequest, questionId)
    }

    @RequestMapping(value = "/{quizId}/questions/{questionRefId}", method = [RequestMethod.DELETE], produces = "application/json")
    @ResponseBody
    RequestResult deleteQuestionDef(@PathVariable("quizId") String quizId, @PathVariable("questionRefId") Integer questionRefId) {
        return quizDefService.deleteQuestion(quizId, questionRefId)
    }

    @RequestMapping(value = "/{quizId}/questions/{questionId}", method = RequestMethod.PATCH)
    @ResponseBody
    RequestResult updateSkillDisplayOrder(@PathVariable("quizId") String quizId,
                                          @PathVariable("questionId") Integer questionId,
                                          @RequestBody ActionPatchRequest patchRequest) {
        QuizValidator.isNotBlank(quizId, "Quiz Id", quizId)
        QuizValidator.isNotNull(questionId, "Question Id", quizId)
        QuizValidator.isNotNull(patchRequest.action, "Action must be provided", quizId)
        QuizValidator.isNotNull(patchRequest.newDisplayOrderIndex, "newDisplayOrderIndex must be provided", quizId)

        quizDefService.setDisplayOrder(quizId, questionId, patchRequest)
        return RequestResult.success()
    }

    @RequestMapping(value = "/{quizId}/questions", method = [RequestMethod.GET], produces = "application/json")
    @ResponseBody
    QuizQuestionsResult getQuestionDefs(@PathVariable("quizId") String quizId) {
        return quizDefService.getQuestionDefs(quizId)
    }

    @RequestMapping(value = "/{quizId}/questions/{questionId}", method = [RequestMethod.GET], produces = "application/json")
    @ResponseBody
    QuizQuestionDefResult getQuestionDef(@PathVariable("quizId") String quizId, @PathVariable("questionId") Integer questionId) {
        return quizDefService.getQuestionDef(quizId, questionId)
    }


    @RequestMapping(value = "/{quizId}/answers/{answerDefId}/attempts", method = [RequestMethod.GET], produces = "application/json")
    @ResponseBody
    TableResult getUserQuestionAnswerAttempts(@PathVariable("quizId") String quizId,
                                       @PathVariable("answerDefId") Integer answerDefId,
                                       @RequestParam int limit,
                                       @RequestParam int page,
                                       @RequestParam String orderBy,
                                       @RequestParam Boolean ascending) {
        PageRequest pageRequest = TablePageUtil.validateAndConstructQuizPageRequest(limit, page, orderBy, ascending)
        return quizDefService.getUserQuestionAnswers(quizId, answerDefId, pageRequest)
    }

    @RequestMapping(value = "/{quizId}/metrics", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    QuizMetrics getQuizMetrics(@PathVariable("quizId") String quizId) {
        return quizDefService.getMetrics(quizId);
    }

    @RequestMapping(value = "/{quizId}/runs", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    TableResult getQuizRuns(@PathVariable("quizId") String quizId,
                            @RequestParam String query,
                            @RequestParam(required = false) UserQuizAttempt.QuizAttemptStatus quizAttemptStatus,
                            @RequestParam int limit,
                            @RequestParam int page,
                            @RequestParam String orderBy,
                            @RequestParam Boolean ascending) {
        PageRequest pageRequest = TablePageUtil.validateAndConstructQuizPageRequest(limit, page, orderBy, ascending)
        return quizDefService.getQuizRuns(quizId, query, quizAttemptStatus, pageRequest);
    }

    @RequestMapping(value = "/{quizId}/runs/{attemptId}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    RequestResult deleteQuizRun(@PathVariable("quizId") String quizId, @PathVariable("attemptId") Integer attemptId) {
        quizDefService.deleteQuizRun(quizId, attemptId)
        return RequestResult.success()
    }

    @RequestMapping(value = "/{quizId}/runs/{attemptId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    UserGradedQuizQuestionsResult getUsersGradedResult(@PathVariable("quizId") String quizId,
                                                       @PathVariable("attemptId") Integer attemptId) {
        return quizDefService.getUsersGradedResult(quizId, attemptId);
    }

    @RequestMapping(value = "/{quizId}/users/{userId}/attempt", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    QuizAttemptStartResult startQuizAttempt(@PathVariable("quizId") String quizId,
                                            @PathVariable("userId") String userId,
                                            @RequestBody(required = false) StartQuizAttemptReq startQuizAttemptReq) {

        String skillId = startQuizAttemptReq?.skillId
        String projectId = startQuizAttemptReq?.projectId

        return quizRunService.startQuizAttempt(userId, quizId, skillId, projectId);
    }

    @RequestMapping(value = "/{quizId}/users/{userId}/attempt/{attemptId}/answers/{answerId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    RequestResult reportQuizAnswer(@PathVariable("quizId") String quizId,
                                   @PathVariable("userId") String userId,
                                   @PathVariable("attemptId") Integer attemptId,
                                   @PathVariable("answerId") Integer answerId,
                                   @RequestBody QuizReportAnswerReq quizReportAnswerReq) {
        quizRunService.reportQuestionAnswer(userId, quizId, attemptId, answerId, quizReportAnswerReq);
        return RequestResult.success()
    }

    @RequestMapping(value = "/{quizId}/users/{userId}/attempt/{attemptId}/gradeAnswer/{answerDefId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    QuizAnswerGradingResult gradeQuizAnswer(@PathVariable("quizId") String quizId,
                                            @PathVariable("userId") String userId,
                                            @PathVariable("attemptId") Integer attemptId,
                                            @PathVariable("answerDefId") Integer answerDefId,
                                            @RequestBody QuizGradeAnswerReq quizGradeAnswerReq) {
        return quizRunService.gradeQuestionAnswer(userId, quizId, attemptId, answerDefId, quizGradeAnswerReq);
    }


    @RequestMapping(value = "/{quizId}/users/{userId}/attempt/{quizAttempId}/fail", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    QuizGradedResult failQuizAttempt(@PathVariable("quizId") String quizId,
                                     @PathVariable("userId") String userId,
                                     @PathVariable("quizAttempId") Integer quizAttemptId) {
        return quizRunService.failQuizAttempt(userId, quizId, quizAttemptId);
    }

    @RequestMapping(value = "/{quizId}/users/{userId}/attempt/{quizAttempId}/complete", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    QuizGradedResult completeQuizAttempt(@PathVariable("quizId") String quizId,
                                         @PathVariable("userId") String userId,
                                         @PathVariable("quizAttempId") Integer quizAttemptId) {
        return quizRunService.completeQuizAttempt(userId, quizId, quizAttemptId);
    }

    @RequestMapping(value = "/{quizId}/settings", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult saveQuizSettings(@PathVariable("quizId") String quizId, @RequestBody List<QuizSettingsRequest> values) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        QuizValidator.isNotNull(values, "Settings")

        if (values?.find {it.setting.equalsIgnoreCase(QuizSettings.UserCommunityOnlyQuiz.setting) }) {
            throw new SkillQuizException("Not allowed to save [${QuizSettings.UserCommunityOnlyQuiz.setting}] setting using this endpoint", quizId, ErrorCode.BadParam)
        }
        quizSettingsService.saveSettings(quizId, values)

        return RequestResult.success()
    }


    @RequestMapping(value = "/{quizId}/settings", method = [RequestMethod.GET], produces = MediaType.APPLICATION_JSON_VALUE)
    List<QuizSettingsRes> getQuizSettings(@PathVariable("quizId") String quizId) {
        QuizValidator.isNotNull(quizId, "QuizId")
        return quizSettingsService.getSettings(quizId)
    }

    @RequestMapping(value = "/{quizId}/preferences/{preferenceKey}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult saveQuizAdminPreference(@PathVariable("quizId") String quizId, @PathVariable("preferenceKey") String preferenceKey, @RequestBody QuizPreference quizPreference) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        QuizValidator.isNotBlank(preferenceKey, "Quiz Preference")
        QuizValidator.isNotBlank(quizPreference?.value, "Quiz Preference Value")

        quizSettingsService.saveUserPreference(quizId, preferenceKey, quizPreference)

        return RequestResult.success()
    }

    @RequestMapping(value = "/{quizId}/preferences", method = [RequestMethod.GET], produces = MediaType.APPLICATION_JSON_VALUE)
    List<QuizPreferenceRes> getQuizAdminPreferences(@PathVariable("quizId") String quizId) {
        QuizValidator.isNotNull(quizId, "QuizId")
        return quizSettingsService.getCurrentUserQuizPreferences(quizId)
    }

    @RequestMapping(value = "/{quizId}/users/{userKey}/roles/{roleName}", method = [RequestMethod.PUT, RequestMethod.POST], produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult addQuizRole(@PathVariable("quizId") String quizId,
                          @PathVariable("userKey") String userKey,
                          @PathVariable("roleName") RoleName roleName) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        QuizValidator.isNotNull(userKey, "userKey")
        QuizValidator.isNotNull(roleName, "roleName")

        quizRoleService.addQuizRole(userKey, quizId, roleName)
        return RequestResult.success()
    }

    @RequestMapping(value = "/{quizId}/userRoles", method = RequestMethod.GET)
    List<UserRoleRes> getQuizUserRoles(@PathVariable("quizId") String quizId) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        return quizRoleService.getQuizUserRoles(quizId)
    }

    @RequestMapping(value = "/{quizId}/users/{userKey}/roles/{roleName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    RequestResult deleteQuizRole(@PathVariable("quizId") String quizId,
                              @PathVariable("userKey") String userKey,
                              @PathVariable("roleName") RoleName roleName) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        QuizValidator.isNotNull(userKey, "userKey")
        QuizValidator.isNotNull(roleName, "roleName")

        quizRoleService.deleteQuizRole(userKey, quizId, roleName)
        return RequestResult.success()
    }

    @RequestMapping(value = "/{quizId}/userTagCounts", method = [RequestMethod.GET], produces = "application/json")
    @ResponseBody
    List<LabelCountItem> getUserTagCounts(@PathVariable("quizId") String quizId, @RequestParam String userTagKey) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        QuizValidator.isNotBlank(userTagKey, "User Tag Key")
        return quizDefService.getUserTagCounts(quizId, userTagKey)
    }

    @RequestMapping(value = "/{quizId}/usageOverTime", method = [RequestMethod.GET], produces = "application/json")
    @ResponseBody
    List<TimestampCountItem> getUsageOverTime(@PathVariable("quizId") String quizId) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        List<TimestampCountItem> res = quizDefService.getUsageOverTime(quizId)
        return res
    }

    @RequestMapping(value = "/{quizId}/dashboardActions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CompileStatic
    TableResult getDashboardActions(@PathVariable("quizId") String quizId,
                                    @RequestParam int limit,
                                    @RequestParam int page,
                                    @RequestParam String orderBy,
                                    @RequestParam Boolean ascending,
                                    @RequestParam(required=false) String itemFilter,
                                    @RequestParam(required=false) String userFilter,
                                    @RequestParam(required=false) String itemIdFilter,
                                    @RequestParam(required=false) String actionFilter) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
        return userActionsHistoryService.getUsersActions(pageRequest,
                null,
                quizId,
                null,
                itemFilter? DashboardItem.valueOf(itemFilter) : null,
                userFilter ? URLDecoder.decode(userFilter, StandardCharsets.UTF_8) : null,
                null,
                itemIdFilter ? URLDecoder.decode(itemIdFilter, StandardCharsets.UTF_8) : null,
                actionFilter ? DashboardAction.valueOf(actionFilter) : null)
    }

    @RequestMapping(value = "/{quizId}/dashboardActions/filterOptions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CompileStatic
    DashboardUserActionsFilterOptions getActionFilterOptions(@PathVariable("quizId") String quizId) {
        return userActionsHistoryService.getUserActionsFilterOptions(null, quizId)
    }

    @RequestMapping(value = "/{quizId}/dashboardActions/{actionId}/attributes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @CompileStatic
    Map getDashboardActionAttributes(@PathVariable("quizId") String quizId, @PathVariable("actionId") Long actionId) {
        return userActionsHistoryService.getActionAttributes(actionId, null, quizId)
    }

    @RequestMapping(value = "/{quizId}/adminGroups", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    List<AdminGroupDefResult> getAdminGroupsForQuiz(@PathVariable("quizId") String quizId) {
        SkillsValidator.isNotBlank(quizId, "Quiz Id")
        return adminGroupService.getAdminGroupsForQuiz(quizId)
    }
}
