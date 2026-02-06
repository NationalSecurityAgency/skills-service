/**
 * Copyright 2026 SkillTree
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
package skills.tasks.executors

import callStack.profiler.CProf
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.VoidExecutionHandler
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.TextInputAIGradingResult
import skills.quizLoading.QuizRunService
import skills.quizLoading.model.QuizGradeAnswerReq
import skills.services.openai.OpenAIService
import skills.tasks.data.TextInputAiGradingRequest

@Component
@Slf4j
class TextInputQuestionAiGradingExecutor implements VoidExecutionHandler<TextInputAiGradingRequest> {

    @Autowired
    QuizRunService quizRunService

    @Autowired
    OpenAIService openAIService

    @Override
    void execute(TaskInstance<TextInputAiGradingRequest> taskInstance, ExecutionContext executionContext) {
        TextInputAiGradingRequest data = taskInstance.getData()
        log.debug("running async TextInputQuestionAiGradingExecutor for userId=[{}], quizId=[{}], quizAttemptId={}, answerDefId={}", data.userId, data.quizId, data.quizAttemptId, data.answerDefId)

        CProf.clear()
        String profName = "${data.quizId}-${data.userId}-${data.quizAttemptId}-${data.answerDefId}-TextInputQuestionAiGradingExecutor".toString()
        CProf.start(profName)

        try {
            TextInputAIGradingResult textInputAIGradingResult = openAIService.gradeTextInputQuizAnswer(data.question, data.textInputAiGradingAttrs.correctAnswer, data.textInputAiGradingAttrs.minimumConfidenceLevel, data.studentAnswer)
            QuizGradeAnswerReq quizGradeAnswerReq = new QuizGradeAnswerReq(isCorrect: textInputAIGradingResult.confidenceLevel >= data.textInputAiGradingAttrs.minimumConfidenceLevel, feedback: textInputAIGradingResult.gradingDecisionReason)
            quizRunService.gradeQuestionAnswer(data.userId, data.quizId, data.quizAttemptId, data.answerDefId, quizGradeAnswerReq, true, textInputAIGradingResult.confidenceLevel)
        } finally {
            quizRunService.incrementAiGradingAttemptCount(data.quizAttemptId, data.answerDefId)
        }

        CProf.stop(profName)
        log.info("Profiled TextInputQuestionAiGradingExecutor for [{}-{}-{}-{}]:\n{}", data.quizId, data.userId, data.quizAttemptId, data.answerDefId, CProf.prettyPrint())
    }
}
