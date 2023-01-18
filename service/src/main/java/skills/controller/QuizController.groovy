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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.controller.exceptions.QuizValidator
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.QuizDefRequest
import skills.controller.request.model.QuizQuestionDefRequest
import skills.controller.request.model.QuizSettingsRequest
import skills.controller.result.model.*
import skills.quizLoading.QuizRunService
import skills.quizLoading.model.QuizAttemptStartResult
import skills.quizLoading.model.QuizGradedResult
import skills.quizLoading.model.QuizReportAnswerReq
import skills.services.quiz.QuizDefService
import skills.services.quiz.QuizSettingsService

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

    @RequestMapping(value = "/{quizId}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    QuizDefResult saveQuizDef(@PathVariable("quizId") String quizId, @RequestBody QuizDefRequest quizDefRequest) {
        return quizDefService.saveQuizDef(quizId, quizDefRequest.quizId, quizDefRequest)
    }


    @RequestMapping(value = "/{quizId}", method = RequestMethod.DELETE)
    void deleteProject(@PathVariable("quizId") String quizId) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        quizDefService.deleteQuiz(quizId)
    }

    @RequestMapping(value = "/{quizId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    QuizDefResult getQuizDef(@PathVariable("quizId") String quizId) {
        return quizDefService.getQuizDef(quizId)
    }

    @RequestMapping(value = "/{quizId}/summary", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    QuizDefSummaryResult getQuizSummary(@PathVariable("quizId") String quizId) {
        return quizDefService.getQuizDefSummary(quizId)
    }

    @RequestMapping(value = "/{quizId}/questions/create", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    QuizQuestionDefResult saveQuestionDef(@PathVariable("quizId") String quizId,
                                          @RequestBody QuizQuestionDefRequest questionDefRequest) {
        return quizDefService.saveQuestion(quizId, questionDefRequest)
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

    @RequestMapping(value = "/{quizId}/metrics", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    QuizMetrics getQuizMetrics(@PathVariable("quizId") String quizId) {
        return quizDefService.getMetrics(quizId);
    }

    @RequestMapping(value = "/{quizId}/users/{userId}/attempt", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    QuizAttemptStartResult startQuizAttempt(@PathVariable("quizId") String quizId,
                                            @PathVariable("userId") String userId) {
        return quizRunService.startQuizAttempt(userId, quizId);
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

        quizSettingsService.saveSettings(quizId, values)

        return RequestResult.success()
    }


    @RequestMapping(value = "/{quizId}/settings", method = [RequestMethod.GET], produces = MediaType.APPLICATION_JSON_VALUE)
    List<QuizSettingsRes> getQuizSettings(@PathVariable("quizId") String quizId) {
        QuizValidator.isNotNull(quizId, "QuizId")
        return quizSettingsService.getSettings(quizId)
    }

}
