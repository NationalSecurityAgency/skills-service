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
class QuizValidatorService {

    @Autowired
    QuizQuestionDefRepo questionDefRepo

    void validateQuestion(String quizId, Integer questionId, QuizQuestionType expectedType = null) {
        QuizValidator.isNotBlank(quizId, "Quiz Id")
        QuizValidator.isNotNull(questionId, "Question Id")

        Optional<QuizQuestionDef> questionDefOptional = questionDefRepo.findById(questionId)
        QuizValidator.isTrue(questionDefOptional.isPresent(), "Did not find question with the provided id")
        QuizQuestionDef questionDef = questionDefOptional.get()
        if (expectedType && questionDef.type != expectedType) {
            QuizValidator.isTrue(questionDef.type == expectedType, "Only ${expectedType} type is supported")
        }
        QuizValidator.isTrue(questionDef.quizId == quizId, "Question's quiz id must match provided quiz id")
    }

}
