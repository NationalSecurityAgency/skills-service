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
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
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

    @Value('#{"${spring.ai.openai.base-url:null}"}')
    String openAiBaseUrl

    @Value('#{"${skills.openai.completionsEndpoint:/v1/chat/completions}"}')
    String completionsEndpoint

    @Value('#{"${skills.openai.modelsEndpoint:/v1/models}"}')
    String modelsEndpoint

    @Value('#{"${skills.openai.key:null}"}')
    String openAiKey

    String role = "user"

    @Autowired
    @Qualifier('openAIRestTemplate')
    RestTemplate restTemplate

    @Autowired
    WebClient.Builder webClientBuilder

    @Autowired
    SslWebClientConfig sslWebClientConfig

    @Autowired(required = false)
    OpenAiChatModel chatModel;

    boolean isChatEnabled() {
        return chatModel != null
    }

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


    static class AvailableModels {
        List<AvailableModel> models
    }
    static class AvailableModel {
        String model
        Date created
    }


    AvailableModels getAvailableModels() {
        if (!openAiHost) {
            log.debug("skills.openai.host is not configured")
            return null
        }

        String url = String.join("/", openAiBaseUrl, modelsEndpoint)

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (openAiKey) {
            headers.set("Authorization", "Bearer " + openAiKey)
        }
        HttpEntity<CompletionsRequest> entity = new HttpEntity<>(headers);

        JsonSlurper jsonSlurper = new JsonSlurper()
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class)
            String bodyAsStr = response.body
            def parsed = jsonSlurper.parseText(bodyAsStr)
            List<AvailableModel> models = parsed?.data?.collect { parsedModel ->
                new AvailableModel(
                        model: parsedModel.id,
                        created: parsedModel.created ? new Date(parsedModel.created) : null
                )
            }

            return new AvailableModels(models: models)
        } catch (Exception e) {
            log.error("Failed to call external service", e)
            throw new RuntimeException("Failed to fetch data from external service", e)
        }
    }

    Flux<String> streamCompletions1(GenDescRequest genDescRequest) {
        if (!isChatEnabled()) {
            return Flux.error(new IllegalStateException("Chat model is not enabled. Set spring.ai.model.chat to enable."))
        }

        Prompt prompt = new Prompt(
                genDescRequest.instructions,
                OpenAiChatOptions.builder()
                        .model(genDescRequest.model)
                        .temperature(genDescRequest.modelTemperature)
                        .build()
        )
        Flux<ChatResponse> response = chatModel.stream(prompt)
        return response.mapNotNull { ChatResponse chatResponse ->
            String res = (String) chatResponse.getResults().get(0).getOutput().getText()
            res = res?.replaceAll('\\n', '<<newline>>')
            log.debug("Response: [{}]", res)
            return res
        }
    }

    Flux<String> streamCompletions(GenDescRequest genDescRequest) {
        String message = genDescRequest.instructions
        JsonSlurper jsonSlurper = new JsonSlurper()
        String url = String.join("/", openAiHost, completionsEndpoint)
        if (log.isDebugEnabled()) {
            log.debug("${Thread.currentThread().name} - streaming from [{}] with message [{}]", url, message)
        }

        String model = genDescRequest.model
        Double modelTemperature = genDescRequest.modelTemperature
        CompletionsRequest request = new CompletionsRequest(
                messages: [
                        new CompletionMessage(role: role, content: message)
                ],
                model: model,
                stream: true,
                temperature: modelTemperature
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
        boolean stream = false
        Double temperature = 1.0
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
