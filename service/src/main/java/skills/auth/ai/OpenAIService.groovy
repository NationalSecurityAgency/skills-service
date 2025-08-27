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
package skills.auth.ai

import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
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

    String callCompletions(String model, String userRole, String message) {
        if (openAiHost) {

            String url = String.join("/", openAiHost, completionsEndpoint)
            log.info("Calling [{}]", url)

            CompletionsRequest completionsRequest = new CompletionsRequest(
                    model: model,
                    messages: [
                            new CompletionMessage(role: userRole, content: message)
                    ]
            )
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CompletionsRequest> entity = new HttpEntity<>(completionsRequest, headers);

            String res = restTemplate.postForEntity(url, entity, String.class)

            log.info("Response: [{}]", res)

            return res
        }


        log.error("skills.openai.host is not configured")
        return "skills.openai.host is not configured"
    }

    @Canonical
    static class CompletionsRequest {
        String model
        List<CompletionMessage> messages
    }

    @Canonical
    static class CompletionMessage {
        String role
        String content
    }

}
