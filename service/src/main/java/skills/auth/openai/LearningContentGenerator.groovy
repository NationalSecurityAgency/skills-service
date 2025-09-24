/**
 * Copyright 2025 SkillTree
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
package skills.auth.openai

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.ResourceAccessException
import reactor.core.publisher.Flux
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException

@Service
@Slf4j
class LearningContentGenerator {

    @Autowired
    OpenAIService openAIService

    GenDescResponse generateDescription(GenDescRequest request) {
        try {
            OpenAIService.CompletionsResponse response = openAIService.callCompletions(request.instructions)
            return new GenDescResponse(description: response.choices[0].message.content)
        } catch (ResourceAccessException rae) {
            log.error("Failed to call OpenAI api", rae)
            throw new SkillException("Learning Content Generator is not available", rae, null,null, ErrorCode.LearningContentGeneratorNotAvailable)
        }
    }

    Flux<String> streamGenerateDescription(GenDescRequest request) {
        try {
            return openAIService.streamCompletions(request.instructions)
        } catch (ResourceAccessException rae) {
            log.error("Failed to call OpenAI api", rae)
            throw new SkillException("Learning Content Generator is not available", rae, null,null, ErrorCode.LearningContentGeneratorNotAvailable)
        }
    }

}
