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


import groovy.util.logging.Slf4j
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import skills.controller.request.model.AiChatRequest
import skills.intTests.utils.CertificateRegistry
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.MockLlmServer
import spock.lang.IgnoreIf

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.time.Duration

@Slf4j
class AiEndpointsSpecs extends DefaultIntSpec {

    @LocalServerPort
    int localPort

    @Autowired
    MockLlmServer mockLlmServer

    @Autowired(required=false)
    CertificateRegistry certificateRegistry

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

    def "chat endpoint"() {
        ChatClient chatClient = new ChatClient(localPort, certificateRegistry)

        AiChatRequest chatRequest = new AiChatRequest(
                messages: [new AiChatRequest.ChatMessage(role: AiChatRequest.Role.User, content: "hi")],
                model: "model1",
                modelTemperature: 1.0,
        )
        when:
        List<String> response = chatClient.chat(chatRequest)
        then:
        response == ["Hello! " , "How can I help " , "you today?"]
    }

    static class ChatClient {

        String chatUrl
        String loginUrl
        String username
        String password
        CertificateRegistry certificateRegistry

        private String cookieHeader
        private WebClient client
        private boolean isPki
        ChatClient(int skillsServicePort,
                   CertificateRegistry certificateRegistry,
                   String username = "skills@skills.org",
                   String password = "password") {
            this.certificateRegistry = certificateRegistry
            isPki = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki'
            String skillsServiceHost = isPki ? "https://localhost:${skillsServicePort}" : "http://localhost:${skillsServicePort}"
            chatUrl = "${skillsServiceHost}/openai/chat"
            loginUrl = "${skillsServiceHost}/performLogin"
            this.username = username
            this.password = password

            WebClient.Builder webClientBuilder = WebClient.builder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

            if (!isPki) {
                cookieHeader = getCookie()
                webClientBuilder.defaultHeader(HttpHeaders.COOKIE, cookieHeader)
                webClientBuilder.clientConnector(new ReactorClientHttpConnector())
            } else {
                webClientBuilder.clientConnector(new ReactorClientHttpConnector(createPkiHttpClient()))
            }

            client = webClientBuilder.build()
        }

        List<String> chat(AiChatRequest chatRequest) {
            Flux<String> response = client
                    .post()
                    .uri(chatUrl)
                    .headers(headers -> {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
                    })
                    .bodyValue(chatRequest)
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
                    .doOnNext(chunk -> log.info("Received chunk: {}", chunk))
                    .doOnError(error -> log.error("Error in response stream", error))
                    .doOnComplete(() -> log.info("Response stream completed"))

            List<String> collectedResponses = response
                    .timeout(Duration.ofSeconds(30))  // Add timeout to prevent hanging
                    .collectList()
                    .doOnSuccess(list -> log.info("Collected {} responses", list.size()))
                    .doOnError(error -> log.error("Error collecting responses", error))
                    .block()

            return collectedResponses
        }

        private String getCookie() {
            HttpHeaders authHeaders = new HttpHeaders()
            authHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED)
            MultiValueMap<String, String> authParams = new LinkedMultiValueMap<>()
            authParams.add('username', username)
            authParams.add('password', password)

            HttpEntity<MultiValueMap<String, String>> httpRequest = new HttpEntity<>(authParams, authHeaders)
            ResponseEntity<String> authResponse = new RestTemplate().postForEntity(loginUrl, httpRequest, String.class)

            List<String> setCookiesHeaders = authResponse.headers.get(HttpHeaders.SET_COOKIE)
            log.debug("authResponse: [{}]", setCookiesHeaders)
            String cookieHeader = String.join("; ", setCookiesHeaders)
            return cookieHeader
        }

        HttpClient createPkiHttpClient() throws Exception {
            String password = "skillspass"
            char[] passwordAsCharArr = password.toCharArray()

            Resource resource = certificateRegistry.getCertificate(username)
            assert resource != null : "No certificate found for ${username}"
            KeyStore keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(resource.getInputStream(), passwordAsCharArr)

            KeyStore trustStore = KeyStore.getInstance("JKS")
            trustStore.load(new ClassPathResource("/certs/truststore.jks").getInputStream(), passwordAsCharArr)

            // Set up KeyManagerFactory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, passwordAsCharArr);

            // Set up TrustManagerFactory
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // Configure SslContext
            SslContextBuilder sslContextBuilder = SslContextBuilder
                    .forClient()
                    .keyManager(keyManagerFactory)
                    .trustManager(getTrustedManager(trustManagerFactory));

            // Create HttpClient with SSL configuration
            SslContext sslContext = sslContextBuilder.build();
            ConnectionProvider connectionProvider = ConnectionProvider.builder("ChatConnection").build()
            HttpClient httpClient = HttpClient.create(connectionProvider)
                    .secure(spec -> spec.sslContext(sslContext))
            return httpClient
        }

        private static X509TrustManager getTrustedManager(TrustManagerFactory tmf) {
            X509TrustManager defaultTm = tmf.getTrustManagers().find { it instanceof X509TrustManager } as X509TrustManager

            return new X509TrustManager() {
                @Override
                void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    defaultTm.checkClientTrusted(chain, authType)
                }

                @Override
                void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    // This is where we skip hostname verification
                    // but still do certificate chain validation
                    try {
                        defaultTm.checkServerTrusted(chain, authType)
                    } catch (CertificateException e) {
                        if (!e.message?.contains("No subject alternative names present")) {
                            throw e
                        }
                        // Ignore hostname verification errors
                    }
                }

                @Override
                X509Certificate[] getAcceptedIssuers() {
                    return defaultTm.acceptedIssuers
                }
            }
        }
    }



}
