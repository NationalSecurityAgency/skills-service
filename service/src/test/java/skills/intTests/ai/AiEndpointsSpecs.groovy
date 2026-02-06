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
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import skills.controller.request.model.AiChatRequest
import spock.lang.IgnoreIf

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
