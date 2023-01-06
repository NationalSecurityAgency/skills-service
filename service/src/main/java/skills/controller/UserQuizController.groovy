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

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import skills.auth.UserInfoService
import skills.auth.aop.AdminOrApproverGetRequestUsersOnlyWhenUserIdSupplied
import skills.controller.result.model.RequestResult
import skills.quizLoading.QuizRunService
import skills.quizLoading.model.QuizAttemptReq
import skills.quizLoading.model.QuizAttemptStartResult
import skills.quizLoading.model.QuizGradedResult
import skills.quizLoading.model.QuizInfo
import skills.skillLoading.model.OverallSkillSummary

import javax.servlet.http.HttpServletRequest

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

    @RequestMapping(value = "/quizzes/{quizId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    QuizInfo getQuizInfo(@PathVariable("quizId") String quizId) {
        return quizRunService.loadQuizInfo(quizId);
    }

    @RequestMapping(value = "/quizzes/{quizId}/attempt/old", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    @Deprecated
    QuizGradedResult reportQuizAttempt(@PathVariable("quizId") String quizId,
                                       @RequestBody QuizAttemptReq quizAttemptReq) {
        return quizRunService.reportQuizAttempt(quizId, quizAttemptReq);
    }

    @RequestMapping(value = "/quizzes/{quizId}/attempt", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    QuizAttemptStartResult startQuizAttempt(@PathVariable("quizId") String quizId) {
        return quizRunService.startQuizAttempt(quizId);
    }

    @RequestMapping(value = "/quizzes/{quizId}/attempt/{attemptId}/answers/{answerId}", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    RequestResult reportQuizAnswer(@PathVariable("quizId") String quizId,
                                            @PathVariable("attemptId") Integer attemptId,
                                            @PathVariable("answerId") Integer answerId) {
        quizRunService.reportQuestionAnswer(quizId, attemptId, answerId);
        return RequestResult.success()
    }

    @RequestMapping(value = "/quizzes/{quizId}/attempt/{quizAttempId}/complete", method = [RequestMethod.POST, RequestMethod.PUT], produces = "application/json")
    @ResponseBody
    QuizGradedResult completeQuizAttempt(@PathVariable("quizId") String quizId,
                                         @PathVariable("quizAttempId") Integer quizAttemptId) {
        return quizRunService.completeQuizAttempt(quizId, quizAttemptId);
    }

}
