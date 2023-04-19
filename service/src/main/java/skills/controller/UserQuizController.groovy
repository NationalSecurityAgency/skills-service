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
import org.springframework.web.bind.annotation.*
import skills.auth.UserInfoService
import skills.auth.aop.AdminOrApproverGetRequestUsersOnlyWhenUserIdSupplied
import skills.controller.result.model.RequestResult
import skills.quizLoading.QuizRunService
import skills.quizLoading.model.QuizAttemptStartResult
import skills.quizLoading.model.QuizGradedResult
import skills.quizLoading.model.QuizInfo
import skills.quizLoading.model.QuizReportAnswerReq
import skills.quizLoading.model.CompleteQuizAttemptReq
import skills.quizLoading.model.StartQuizAttemptReq

@CrossOrigin(allowCredentials = "true", originPatterns = ["*"])
@RestController
@RequestMapping("/api")
@AdminOrApproverGetRequestUsersOnlyWhenUserIdSupplied
@skills.profile.EnableCallStackProf
@CompileStatic
@Slf4j
class UserQuizController {

    @Autowired
    QuizRunService quizRunService

    @Autowired
    UserInfoService userInfoService

    @RequestMapping(value = "/quizzes/{quizId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    QuizInfo getQuizInfo(@PathVariable("quizId") String quizId,
                         @RequestParam(name = "userId", required = false) String userIdParam,
                         @RequestParam(name = "idType", required = false) String idType) {
        String userId = userInfoService.getUserName(userIdParam, true, idType);
        return quizRunService.loadQuizInfo(userId, quizId);
    }

    @RequestMapping(value = "/quizzes/{quizId}/attempt", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    QuizAttemptStartResult startQuizAttempt(@PathVariable("quizId") String quizId,
                                            @RequestBody(required = false) StartQuizAttemptReq startQuizAttemptReq) {
        String userId = userInfoService.getUserName(startQuizAttemptReq?.userId, true, startQuizAttemptReq?.idType);
        return quizRunService.startQuizAttempt(userId, quizId);
    }

    @RequestMapping(value = "/quizzes/{quizId}/attempt/{attemptId}/answers/{answerId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    RequestResult reportQuizAnswer(@PathVariable("quizId") String quizId,
                                   @PathVariable("attemptId") Integer attemptId,
                                   @PathVariable("answerId") Integer answerId,
                                   @RequestBody QuizReportAnswerReq quizReportAnswerReq) {
        String userId = userInfoService.getUserName(quizReportAnswerReq?.userId, true, quizReportAnswerReq?.idType);
        quizRunService.reportQuestionAnswer(userId, quizId, attemptId, answerId, quizReportAnswerReq)
        return RequestResult.success()
    }

    @RequestMapping(value = "/quizzes/{quizId}/attempt/{quizAttempId}/complete", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    QuizGradedResult completeQuizAttempt(@PathVariable("quizId") String quizId,
                                         @PathVariable("quizAttempId") Integer quizAttemptId,
                                         @RequestBody(required = false) CompleteQuizAttemptReq completeQuizAttemptReq) {
        String userId = userInfoService.getUserName(completeQuizAttemptReq?.userId, true, completeQuizAttemptReq?.idType);
        return quizRunService.completeQuizAttempt(userId, quizId, quizAttemptId);
    }

}
