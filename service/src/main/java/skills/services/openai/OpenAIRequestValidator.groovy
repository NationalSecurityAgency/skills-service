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
package skills.services.openai

import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.AiChatRequest

@Component
@Slf4j
class OpenAIRequestValidator {
    private final OpenAIUsageLimitsProperties limits

    OpenAIRequestValidator(OpenAIUsageLimitsProperties limits) {
        this.limits = limits
    }

    void validate(AiChatRequest request) {
        SkillsValidator.isNotNull(request.messages, 'genDescRequest.messages')
        SkillsValidator.isTrue(!request.messages.isEmpty(), 'genDescRequest.messages must have at least one message')
        SkillsValidator.isNotBlank(request.model, 'genDescRequest.model')
        SkillsValidator.isNotNull(request.modelTemperature, 'genDescRequest.modelTemperature')
        SkillsValidator.isTrue(Double.isFinite(request.modelTemperature) && request.modelTemperature >= 0 && request.modelTemperature <= 2,
                'genDescRequest.modelTemperature must be >= 0 and <= 2')
        SkillsValidator.isTrue(!limits.allowedModels || limits.allowedModels.contains(request.model), 'Requested AI model is not allowed')
        checkLimit(request.messages.size(), limits.maxMessages, 'AI message count')
        long totalCharacters = 0
        int largestMessageCharacters = 0
        request.messages.each { AiChatRequest.ChatMessage message ->
            SkillsValidator.isNotNull(message, 'message')
            SkillsValidator.isNotNull(message.role, 'message.role')
            SkillsValidator.isNotNull(message.content, 'message.content')
            checkLimit(message.content.length(), limits.maxMessageCharacters, 'AI message characters')
            totalCharacters += message.content.length()
            largestMessageCharacters = Math.max(largestMessageCharacters, message.content.length())
            checkLimit(totalCharacters, limits.maxTotalMessageCharacters, 'AI total message characters')
        }
        log.info('AI chat input sizes: model=[{}], messageCount=[{}], totalCharacters=[{}], largestMessageCharacters=[{}], maxOutputTokens=[{}]',
                request.model, request.messages.size(), totalCharacters, largestMessageCharacters, limits.maxOutputTokens)
    }

    private static void checkLimit(long actual, Integer limit, String label) {
        if (limit != null) {
            SkillsValidator.isTrue(actual <= limit, "${label} must not exceed [${limit}]")
        }
    }
}
