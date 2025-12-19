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
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import static com.github.tomakehurst.wiremock.client.WireMock.any
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import static com.google.common.net.HttpHeaders.CONTENT_TYPE

@Slf4j
@Component
class MockLlmServer {

    @Value('#{"${skills.tests.mockLlmServerPort:50001}"}')
    Integer mockLlmServerPort

    WireMockServer mockServer

    @PostConstruct
    void start() {
        try {
            mockServer = new WireMockServer(wireMockConfig()
                    .port(mockLlmServerPort))
            stubModelsEndpoint()
            stubChatsCompletionsEndpoint()

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

    private static String createStreamMsg(String val) {
        def delta = val ? ["role": "assistant", "content": val] : [:]
        def msg = [
                data: [
                        "id"     : "chat-1", "object": "chat.completion.chunk", "created": 1677652288, "model": "model-1",
                        "choices": [
                                [
                                        "index"        : 0,
                                        "delta"        : delta,
                                        "finish_reason": val ? null : "stop"
                                ]
                        ]
                ]]

        return JsonOutput.toJson(msg)
    }

    private void stubChatsCompletionsEndpoint() {
        String msg1 = createStreamMsg("Hi ")
        String msg2 = createStreamMsg("there! ")
        String msg3 = createStreamMsg(" I a")
        String msg4 = createStreamMsg("m ")
        String msg5 = createStreamMsg("your ")
        String msg6 = createStreamMsg("assistant.")
        String stopMsg = createStreamMsg(null)
        String endMsg = "data: [DONE]"

        String body = "$msg1\n$msg2\n$msg3\n$msg4\n$msg5\n$msg6\n$stopMsg\n$endMsg"
        mockServer.stubFor(
                any(urlPathEqualTo("/v1/chat/completions"))
                        .willReturn(ok()
                                .withHeader(CONTENT_TYPE, "text/event-stream")
                                .withChunkedDribbleDelay(1, 10)  // Small delay between chunks for better simulation
                                .withBody(body.stripIndent().trim())
                        )
        )
    }

    @PreDestroy
    void stop() {
        mockServer.stop();
    }

}
