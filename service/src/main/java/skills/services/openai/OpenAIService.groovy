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
package skills.services.openai

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.metadata.Usage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.lang.Nullable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import skills.controller.exceptions.SkillException
import skills.controller.result.model.TextInputAIGradingResult
import skills.controller.request.model.AiChatRequest

@Service
@Slf4j
class OpenAIService {

    @Value('${skills.openai.host:#{null}}')
    String openAiHost

    @Value('${skills.openai.host:#{null}}')
    String openAiBaseUrl

    @Value('#{"${skills.openai.completionsEndpoint:/v1/chat/completions}"}')
    String completionsEndpoint

    @Value('#{"${skills.openai.modelsEndpoint:/v1/models}"}')
    String modelsEndpoint

    @Value('${skills.openai.key:#{null}}')
    String openAiKey

    @Value('#{"${skills.openai.stream.stream-usage:true}"}')
    Boolean streamUsage

    @Value('${skills.openai.gradingModel:#{null}}')
    String gradingModel

    @Value('#{"${skills.openai.gradingModelTemperature:0.0}"}')
    Double gradingModelTemperature

    String systemMsg

    String textInputQuestionGradingMsg

    @Autowired
    @Qualifier('openAIRestTemplate')
    RestTemplate restTemplate

    @Autowired
    WebClient.Builder webClientBuilder

    @Autowired(required = false)
    OpenAiChatModel chatModel;

    static class AvailableModels {
        List<AvailableModel> models
    }
    static class AvailableModel {
        String model
        Date created
    }

    AvailableModels getAvailableModels() {
        if (!openAiHost) {
            throw new UnsupportedOperationException("ai support is not configured" )
        }

        String url = String.join("/", openAiBaseUrl, modelsEndpoint)

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (openAiKey) {
            headers.set("Authorization", "Bearer " + openAiKey)
        }
        HttpEntity entity = new HttpEntity<>(headers);

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

    Flux<String> streamChat(AiChatRequest genDescRequest) {
        if (!openAiHost) {
            throw new UnsupportedOperationException("ai support is not configured" )
        }
        if (!systemMsg) {
            throw new UnsupportedOperationException("ai systemMsg is not configured" )
        }

        long startTime = System.currentTimeMillis()
        boolean isFirstMessage = genDescRequest.messages.size() == 1
        List<Message> messages = isFirstMessage ? [new SystemMessage(systemMsg)] : []
        messages.addAll(genDescRequest.messages.collect { msg ->
            if (msg.role == AiChatRequest.Role.User) {
                return new UserMessage(msg.content)
            } else if (msg.role == AiChatRequest.Role.Assistant) {
                return new AssistantMessage(msg.content)
            } else {
                throw new IllegalArgumentException("Invalid role: " + msg.role)
            }
        })

        Prompt prompt = new Prompt(
                messages,
                OpenAiChatOptions.builder()
                        .model(genDescRequest.model)
                        .temperature(genDescRequest.modelTemperature)
                        .build()
        )
        List<ChatResponse> collectedResponses = Collections.synchronizedList([] as List<ChatResponse>)
        Flux<ChatResponse> response = chatModel.stream(prompt)
        return response.mapNotNull { ChatResponse chatResponse ->
            try {
                if (streamUsage && chatResponse?.getMetadata()?.getUsage()?.getTotalTokens() > 0) {
                    collectedResponses << chatResponse
                }
                List<Generation> genList = chatResponse.getResults()
                if (!genList) {
                    return ""
                }
                String res = (String) genList.get(0).getOutput().getText()
                res = res?.replaceAll('\\n', '<<newline>>')
                log.debug("Response: [{}]", res)
                return res
            } catch (Throwable t) {
                String chatResponseAsStr = ""
                try {
                    chatResponseAsStr = JsonOutput.toJson(chatResponse)
                } catch (Throwable t2) {
                }
                log.error("Failed to get response from OpenAI, chatResponse=[${chatResponseAsStr}]", t)
                throw t
            }
        }.doOnComplete {
            if (streamUsage) {
                long totalRuntimeMs = (System.currentTimeMillis() - startTime)
                if (!collectedResponses.isEmpty()) {
                    // Use the last response which should have the complete usage info
                    Usage usage = collectedResponses.last().getMetadata().getUsage()
                    log.info("Chat Usage: totalTokens=[${usage.totalTokens}], promptTokens=[${usage.promptTokens}], completionTokens=[${usage.completionTokens}], totalRuntimeMs=[${totalRuntimeMs}]")
                } else {
                    log.warn("Failed to collect chat usage. Total runtime: [${totalRuntimeMs}]")
                }
            }
        }
    }

    TextInputAIGradingResult gradeTextInputQuizAnswer(String question, String correctAnswer, Integer minimumConfidenceLevel, String studentAnswer) {
        if (!openAiHost) {
            throw new UnsupportedOperationException("ai support is not configured" )
        }
        if (!gradingModel) {
            throw new UnsupportedOperationException("ai grading model is not configured" )
        }
        String promptStr = textInputQuestionGradingMsg
                .replace('{{ question }}', question)
                .replace('{{ studentAnswer }}', studentAnswer)
                .replace('{{ correctAnswer }}', correctAnswer)
                .replace('{{ minimumConfidenceLevel }}', minimumConfidenceLevel.toString())
        log.debug("Prompt: {}", promptStr)
        List<Message> messages = [
                new UserMessage(promptStr)
        ]
        Prompt prompt = new Prompt(
                messages,
                OpenAiChatOptions.builder()
                        .model(gradingModel)
                        .temperature(gradingModelTemperature)
                        .build()
        )
        ChatResponse chatResponse = chatModel.call(prompt)
        List<Generation> genList = chatResponse.getResults()
        if (!genList) {
            throw new SkillException("Failed to get response from OpenAI")
        }
        String res = (String) genList.get(0).getOutput().getText()
        log.debug("LLM Response: {}", res)
        try {
            // Parse JSON response into TextInputAIGradingResult
            def jsonSlurper = new JsonSlurper()
            def parsedResponse = jsonSlurper.parseText(extractJsonFromResponse(res))
            assert parsedResponse.confidenceLevel != null && parsedResponse.confidenceLevel instanceof Integer, "invalid or missing confidenceLevel [${parsedResponse.confidenceLevel}]"
            assert parsedResponse.gradingDecisionReason instanceof String, "invalid or missing gradingDecisionReason [${parsedResponse.gradingDecisionReason}]"
            return new TextInputAIGradingResult(
                    confidenceLevel: parsedResponse.confidenceLevel as Integer,
                    gradingDecisionReason: parsedResponse.gradingDecisionReason
            )
        } catch (Throwable e) {
            log.error("Failed to parse JSON response from LLM: {}", res, e)
            throw new SkillException("Failed to parse LLM response: ${e.message}", e)
        }
    }

    private static String extractJsonFromResponse(String response) {
        def startIndex = response.indexOf('```json')
        if (startIndex == -1) {
            return response.trim()
        }
        startIndex += '```json'.length()
        def endIndex = response.indexOf('```', startIndex)
        if (endIndex == -1) {
            return response.substring(startIndex).trim()
        }
        return response.substring(startIndex, endIndex).trim()
    }
}
