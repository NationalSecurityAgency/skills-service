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

import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
@Slf4j
class OpenAIService {

    RestTemplate restTemplate = new RestTemplate();

    @Value('#{"${skills.openai.host:null}"}')
    String openAiHost

    @Value('#{"${skills.openai.completionsEndpoint:/v1/chat/completions}"}')
    String completionsEndpoint

    @Value('#{"${skills.openai.key:null}"}')
    String openAiKey

    @Value('#{"${skills.openai.model:null}"}')
    String model

    String role = "user"

    CompletionsResponse callCompletions(String message) {
        if (!openAiHost) {
            log.debug("skills.openai.host is not configured")
            return null
        }

        String url = String.join("/", openAiHost, completionsEndpoint)
        log.info("Calling [{}] with message [{}]", url, message)

        CompletionsRequest completionsRequest = new CompletionsRequest(
                messages: [
                        new CompletionMessage(role: role, content: message)
                ]
        )
        if (model) {
            completionsRequest.model = model
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (openAiKey) {
            headers.set("Authorization", "Bearer " + openAiKey)
        }
        HttpEntity<CompletionsRequest> entity = new HttpEntity<>(completionsRequest, headers);

        CompletionsResponse res = restTemplate.postForObject(url, entity, CompletionsResponse.class)

        log.info("Response: [{}]", res.toString())

        return res
    }

    @Canonical
    @ToString(includeNames = true)
    static class CompletionsRequest {
        String model
        List<CompletionMessage> messages
        Usage usage
    }

    @Canonical
    @ToString(includeNames = true)
    static class CompletionMessage {
        String role
        String content
    }

    @Canonical
    @ToString(includeNames = true)
    static class CompletionsResponse {
        String id
        String object
        int created
        String model
        List<Choice> choices
    }

    @Canonical
    @ToString(includeNames = true)
    static class Choice {
        int index
        Message message
    }

    @Canonical
    @ToString(includeNames = true)
    static class Message {
        String role
        String content
    }

    @Canonical
    @ToString(includeNames = true)
    static class Usage {
        int promptTokens
        int completionTokens
        int totalTokens
    }

}
