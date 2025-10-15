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

import groovy.json.JsonSlurper
import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
@Slf4j
class OpenAIService {

    @Value('#{"${skills.openai.host:null}"}')
    String openAiHost

    @Value('#{"${skills.openai.completionsEndpoint:/v1/chat/completions}"}')
    String completionsEndpoint

    @Value('#{"${skills.openai.key:null}"}')
    String openAiKey

    @Value('#{"${skills.openai.model:null}"}')
    String model

    String role = "user"

    @Autowired
    @Qualifier('openAIRestTemplate')
    RestTemplate restTemplate

    @Autowired
    WebClient.Builder webClientBuilder

    @Autowired
    SslWebClientConfig sslWebClientConfig

    CompletionsResponse callCompletions(String message) {
        if (!openAiHost) {
            log.debug("skills.openai.host is not configured")
            return null
        }

        String url = String.join("/", openAiHost, completionsEndpoint)
        log.debug("Calling [{}] with message [{}]", url, message)

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

        if (log.isDebugEnabled()) {
            log.debug("Response: [{}]", res.toString())
        }

        return res
    }

    Flux<String> streamCompletions(String message) {
        JsonSlurper jsonSlurper = new JsonSlurper()
        String url = String.join("/", openAiHost, completionsEndpoint)
        if (log.isDebugEnabled()) {
            log.debug("${Thread.currentThread().name} - streaming from [{}] with message [{}]", url, message)
        }

        CompletionsRequest request = new CompletionsRequest(
                messages: [
                        new CompletionMessage(role: role, content: message)
                ],
                model: model,
                stream: true
        )

        WebClient client = sslWebClientConfig.createWebClient()

        return client
                .post()
                .uri(url)
                .headers(headers -> {
                    headers.setBearerAuth(openAiKey);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
                })
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    // Extract and log the response body for debugging
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("No error details provided")
                            .flatMap(errorBody -> {
                                return Mono.error(new RuntimeException(
                                        String.format("OpenAI API error: %s - %s", response.statusCode(), errorBody)
                                ));
                            });
                })
                .bodyToFlux(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClient error: Status={}, Body={}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
                    return Flux.error(new RuntimeException("Failed to process OpenAI streaming response", ex));
                })
                .onErrorResume(Throwable.class, ex -> {
                    log.error("Unexpected error during OpenAI streaming", ex);
                    return Flux.error(new RuntimeException("Unexpected error during streaming", ex));
                })
                .mapNotNull { String json ->
                    if (json.equalsIgnoreCase('[DONE]')) {
                        return json
                    }
                    def result = jsonSlurper.parseText(json)
                    String res = result?.choices?.first()?.delta?.content ?: ""
                    res = res.replaceAll('\\n', '<<newline>>')
                    log.debug("Response: [{}] from json=[{}]", res, json)
                    return res
                }
    }

    @Canonical
    @ToString(includeNames = true)
    static class CompletionsRequest {
        String model
        List<CompletionMessage> messages
        Usage usage
        boolean stream = false
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
