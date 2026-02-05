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
package skills.intTests.utils

import com.github.tomakehurst.wiremock.WireMockServer
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import static com.google.common.net.HttpHeaders.CONTENT_TYPE

@Slf4j
@Component
class MockLlmServer {

    @Value('#{"${skills.tests.mockLlmServerPort:50001}"}')
    Integer mockLlmServerPort

    WireMockServer mockServer

    void start() {
        try {
            mockServer = new WireMockServer(wireMockConfig()
                    .port(mockLlmServerPort))
            stubModelsEndpoint()
            stubChatCompletionsEndpoint()
            stubTextInputQuizGradingEndpoint()

            mockServer.start()
            log.info("WireMock server started on port {}", mockLlmServerPort)

        } catch (Exception e) {
            log.error("Failed to start WireMock server", e)
            throw e
        }
    }

    private void stubModelsEndpoint() {
        def models = [
                "object": "list",
                "data"  : [[
                                   "id"           : "model1",
                                   "object"       : "model",
                                   "created"      : 1766147425720,
                                   "owned_by"     : "abc",
                                   "root"         : "/path/to/model1",
                                   "parent"       : null,
                                   "max_model_len": 32768,
                           ], [
                                   "id"           : "model2",
                                   "object"       : "model",
                                   "created"      : 1766147415720,
                                   "owned_by"     : "abc",
                                   "root"         : "/path/to/model2",
                                   "parent"       : null,
                                   "max_model_len": 32768,
                           ], [
                                   "id"           : "model3",
                                   "object"       : "model",
                                   "created"      : 1766146425720,
                                   "owned_by"     : "abc",
                                   "root"         : "/path/to/model3",
                                   "parent"       : null,
                                   "max_model_len": 32768,
                           ]]
        ]
        String jsonModel = JsonOutput.toJson(models)

        mockServer.stubFor(
                any(urlPathEqualTo("/v1/models"))
                        .willReturn(ok()
                                .withHeader(CONTENT_TYPE, "application/json")
                                .withBody(jsonModel)
                        )
        )
    }

    static List<String> createStreamMessages() {
        List<String> messages = []

        // Add initial message
        messages.add(createStreamMsg("Hello! "))
        messages.add(createStreamMsg("How can I help "))
        messages.add(createStreamMsg("you today?"))

        // Add stop reason
        messages.add(createStreamMsg(null)) // null content indicates stop

        // Add done signal
        messages.add("data: [DONE]")

        return messages
    }

    static String createStreamMsg(String content) {
        def message = [
                id: "chatcmpl-${UUID.randomUUID()}",
                object: "chat.completion.chunk",
                created: System.currentTimeMillis() / 1000,
                model: "mock-model",
                choices: [
                        [
                                index: 0,
                                delta: content ? [content: content] : [:],
                                finish_reason: content ? null : "stop"
                        ]
                ]
        ]
        return "data: ${new JsonOutput().toJson(message)}"
    }

    void stubChatCompletionsEndpoint() {
        String body = createStreamMessages().join("\n\n")  // Double newline between events

        mockServer.stubFor(
                post(urlPathEqualTo("/v1/chat/completions"))
                        .willReturn(ok()
                                .withHeader("Content-Type", "text/event-stream")
                                .withHeader("Cache-Control", "no-cache")
                                .withHeader("Connection", "keep-alive")
                                .withChunkedDribbleDelay(3, 100)  // 3 chunks, 100ms between
                                .withBody(body)
                        )
        )

        // Log requests
        mockServer.addMockServiceRequestListener { request, _ ->
            log.info("Received request to: {}", request.getUrl())
            log.info("Headers: {}", request.getHeaders())
            log.info("Body: {}", request.getBodyAsString())
        }
    }

    void stubTextInputQuizGradingEndpoint() {
        // Stub for passing answers (confidence level 90)
        mockServer.stubFor(
                post(urlPathEqualTo("/v1/chat/completions"))
                        .withRequestBody(containing("this answer should pass"))
                        .willReturn(ok()
                                .withHeader(CONTENT_TYPE, "application/json")
                                .withBody(JsonOutput.toJson([
                                        id: "chatcmpl-${UUID.randomUUID()}",
                                        object: "chat.completion",
                                        created: System.currentTimeMillis() / 1000,
                                        model: "mock-grading-model",
                                        choices: [[
                                                index: 0,
                                                message: [
                                                        role: "assistant",
                                                        content: JsonOutput.toJson([
                                                                confidenceLevel: 90,
                                                                gradingDecisionReason: "The student's answer demonstrates excellent understanding and closely matches the correct answer."
                                                        ])
                                                ],
                                                finish_reason: "stop"
                                        ]],
                                        usage: [
                                                prompt_tokens: 100,
                                                completion_tokens: 50,
                                                total_tokens: 150
                                        ]
                                ]))
                        )
        )

        // Stub for failing answers (confidence level 10)
        mockServer.stubFor(
                post(urlPathEqualTo("/v1/chat/completions"))
                        .withRequestBody(containing("this answer should fail"))
                        .willReturn(ok()
                                .withHeader(CONTENT_TYPE, "application/json")
                                .withBody(JsonOutput.toJson([
                                        id: "chatcmpl-${UUID.randomUUID()}",
                                        object: "chat.completion",
                                        created: System.currentTimeMillis() / 1000,
                                        model: "mock-grading-model",
                                        choices: [[
                                                index: 0,
                                                message: [
                                                        role: "assistant",
                                                        content: JsonOutput.toJson([
                                                                confidenceLevel: 10,
                                                                gradingDecisionReason: "The student's answer shows significant misunderstandings and does not match the correct answer."
                                                        ])
                                                ],
                                                finish_reason: "stop"
                                        ]],
                                        usage: [
                                                prompt_tokens: 100,
                                                completion_tokens: 50,
                                                total_tokens: 150
                                        ]
                                ]))
                        )
        )

        // Log requests for debugging
        mockServer.addMockServiceRequestListener { request, _ ->
            if (request.getUrl().contains("/v1/chat/completions")) {
                log.info("Grading request received: {}", request.getBodyAsString())
            }
        }
    }

    void stop() {
        mockServer.stop();
    }

}
