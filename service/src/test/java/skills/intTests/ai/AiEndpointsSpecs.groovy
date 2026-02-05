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
import skills.intTests.utils.QuizDefFactory
import skills.quizLoading.QuizSettings
import skills.storage.model.UserAttrs
import skills.storage.model.UserQuizAttempt
import spock.lang.IgnoreIf

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.time.Duration

@Slf4j
class AiEndpointsSpecs extends DefaultAiIntSpec {

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

}
