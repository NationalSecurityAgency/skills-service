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
package skills.services.quiz

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.QuizValidator
import skills.controller.request.model.TextInputAiGradingConfRequest
import skills.services.attributes.TextInputAiGradingAttrs
import skills.storage.model.QuizQuestionDef
import skills.storage.repos.QuizQuestionDefRepo

@Service
@Slf4j
class QuizAttributesService {

    static final ObjectMapper mapper = new ObjectMapper()

    @Autowired
    QuizQuestionDefRepo quizQuestionRepo

    @Value('#{"${skills.openai.textInputAiGraderDefaultMinimumConfidenceLevel:75}"}')
    Integer minimumConfidenceLevel

    @Transactional
    TextInputAiGradingAttrs saveTextInputAiGradingAttrs(String quizId, Integer questionId, TextInputAiGradingConfRequest gradingConfRequest) {
        validateTextInputAiGradingInput(quizId, questionId)
        QuizValidator.isNotNull(gradingConfRequest, "Grading Configuration")
        QuizValidator.isNotNull(gradingConfRequest.enabled, "Enabled")
        QuizValidator.isNotNull(gradingConfRequest.correctAnswer, "Correct Answer")
        QuizValidator.isNotNull(gradingConfRequest.minimumConfidenceLevel, "Minimum Confidence Level")
        QuizValidator.isTrue(gradingConfRequest.minimumConfidenceLevel > 0 && gradingConfRequest.minimumConfidenceLevel <= 100, "Minimum Confidence Level must be > 0 and <= 100")

        TextInputAiGradingAttrs textInputAiGradingAttrs = new TextInputAiGradingAttrs(
                enabled: gradingConfRequest.enabled,
                correctAnswer: gradingConfRequest.correctAnswer,
                minimumConfidenceLevel: gradingConfRequest.minimumConfidenceLevel,
        )
        quizQuestionRepo.saveTextInputAiGradingAttrs(quizId, questionId, mapper.writeValueAsString(textInputAiGradingAttrs))

        return textInputAiGradingAttrs
    }

    @Transactional
    TextInputAiGradingAttrs getTextInputAiGradingAttrs(String quizId, Integer questionId) {
        validateTextInputAiGradingInput(quizId, questionId)

        String strAttrs = quizQuestionRepo.getTextInputAiGradingAttrs(quizId, questionId)
        TextInputAiGradingAttrs res = strAttrs ?
                mapper.readValue(strAttrs, TextInputAiGradingAttrs.class) :
                new TextInputAiGradingAttrs(enabled: false, minimumConfidenceLevel: minimumConfidenceLevel)
        return res
    }

    private void validateTextInputAiGradingInput(String quizId, Integer questionId) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        QuizValidator.isNotNull(questionId, "Question Id")

        Optional<QuizQuestionDef> questionDefOptional = quizQuestionRepo.findById(questionId)
        QuizValidator.isTrue(questionDefOptional.isPresent(), "Did not find question with the provided id")
        QuizQuestionDef questionDef = questionDefOptional.get()
        QuizValidator.isTrue(questionDef.type == QuizQuestionType.TextInput, "Only TextInput type is supported")
        QuizValidator.isTrue(questionDef.quizId == quizId, "Question's quiz id must match provided quiz id")
    }

}
