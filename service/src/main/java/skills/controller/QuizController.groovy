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
import org.springframework.web.bind.annotation.*
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.ActionPatchRequest
import skills.controller.request.model.QuizDefRequest
import skills.controller.request.model.QuizQuestionDefRequest
import skills.controller.result.model.QuizDefResult
import skills.controller.result.model.QuizQuestionDefResult
import skills.controller.result.model.RequestResult
import skills.services.quiz.QuizDefService

@RestController
@RequestMapping("/admin/quiz-definitions")
@Slf4j
@skills.profile.EnableCallStackProf
class QuizController {

    @Autowired
    QuizDefService quizDefService

    @RequestMapping(value = "/{id}", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    QuizDefResult saveQuizDef(@PathVariable("id") String quizId, @RequestBody QuizDefRequest quizDefRequest) {
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
    List<QuizQuestionDefResult> getQuestionDefs(@PathVariable("quizId") String quizId) {
        return quizDefService.getQuestionDefs(quizId)
    }

}
