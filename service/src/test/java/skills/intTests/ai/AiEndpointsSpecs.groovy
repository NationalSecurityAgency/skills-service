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
package skills.intTests.ai


import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import skills.auth.openai.AiChatRequest
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.MockLlmServer
import spock.lang.IgnoreIf

import java.time.Duration

@Slf4j
class AiEndpointsSpecs extends DefaultIntSpec {

    @LocalServerPort
    int localPort

    @Autowired
    MockLlmServer mockLlmServer

    def setup() {
        mockLlmServer.start()
    }

    def cleanup() {
        mockLlmServer.stop()
    }


    def "get available models"() {
        when:
        def models = skillsService.getAiModels()
        then:
        models.models.model == ['model1', 'model2', 'model3']
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "must be logged in to retrieve models "() {
        RestTemplate unauthorized = new RestTemplate()
        when:
        unauthorized.getForEntity("http://localhost:${localPort}/openai/models", String)
        then:
        HttpClientErrorException e = thrown(HttpClientErrorException)
        e.statusCode == HttpStatus.UNAUTHORIZED
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "must be logged in to use chat endpoint"() {
        RestTemplate unauthorized = new RestTemplate()
        when:
        unauthorized.getForEntity("http://localhost:${localPort}/openai/chat", String)
        then:
        HttpClientErrorException e = thrown(HttpClientErrorException)
        e.statusCode == HttpStatus.UNAUTHORIZED
    }

    private String getCookie() {
        String username = "skills@skills.org"
        String password = "password"

        HttpHeaders authHeaders = new HttpHeaders()
        authHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED)
        MultiValueMap<String, String> authParams = new LinkedMultiValueMap<>()
        authParams.add('username', username)
        authParams.add('password', password)

        HttpEntity<MultiValueMap<String, String>> httpRequest = new HttpEntity<>(authParams, authHeaders)
        ResponseEntity<String> authResponse = new RestTemplate().postForEntity("http://localhost:${localPort}/performLogin", httpRequest, String.class)

        List<String> setcookies = authResponse.headers.get(HttpHeaders.SET_COOKIE)
        log.info("authResponse: [{}]", setcookies)
        String cookieHeader = String.join("; ", setcookies)
        return cookieHeader
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "chat endpoint"() {
        String url = "http://localhost:${localPort}/openai/chat"

        String cookieHeader = getCookie()
        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.COOKIE, cookieHeader)
                .build()

        AiChatRequest chatRequest = new AiChatRequest(
                messages: [new AiChatRequest.ChatMessage(role: AiChatRequest.Role.User, content: "hi")],
                model: "model1",
                modelTemperature: 1.0,
        )

        Flux<String> response = client
            .post()
            .uri(url)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
            })
            .bodyValue(chatRequest)
            .retrieve()
            .onStatus(HttpStatus::isError, response -> {
                return response.bodyToMono(String.class)
                        .defaultIfEmpty("No error details")
                        .flatMap(errorBody -> {
                            log.error("Error response [{}]: {}", response.statusCode(), errorBody)
                            return Mono.error(new RuntimeException("API error: ${response.statusCode()} - $errorBody"))
                        })
            })
            .bodyToFlux(String.class)
            .doOnNext(chunk -> log.info("Received chunk: {}", chunk))
            .doOnError(error -> log.error("Error in response stream", error))
            .doOnComplete(() -> log.info("Response stream completed"))

        when:
        List<String> collectedResponses = response
                .timeout(Duration.ofSeconds(30))  // Add timeout to prevent hanging
                .collectList()
                .doOnSuccess(list -> log.info("Collected {} responses", list.size()))
                .doOnError(error -> log.error("Error collecting responses", error))
                .block()
        log.info("All responses: {}", collectedResponses)
        then:
        collectedResponses == ["Hello! " , "How can I help " , "you today?"]
    }


}
