/**
 * Copyright 2026 SkillTree
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

import groovy.json.JsonOutput
import org.springframework.test.context.TestPropertySource
import skills.controller.request.model.AiChatRequest
import spock.lang.Unroll

import static com.github.tomakehurst.wiremock.client.WireMock.*

@TestPropertySource(properties = [
        'skills.openai.limits.allowedModels=model1,model2',
        'skills.openai.limits.maxMessages=3',
        'skills.openai.limits.maxMessageCharacters=10',
        'skills.openai.limits.maxTotalMessageCharacters=20',
        'skills.openai.limits.maxRequestBytes=512',
        'skills.openai.limits.maxOutputTokens=64'
])
class AiRequestLimitsIT extends DefaultAiIntSpec {
    ChatClient client

    def setup() {
        client = new ChatClient(localPort, certificateRegistry)
    }

    @Unroll
    def 'accept configured boundary: #contents'() {
        when:
        def response = client.exchange(request(contents))

        then:
        response.statusCode.value() == 200
        response.body.contains('Hello!')
        mockLlmServer.mockServer.verify(1, postRequestedFor(urlEqualTo('/v1/chat/completions'))
                .withRequestBody(matchingJsonPath('$.max_completion_tokens', equalTo('64'))))

        where:
        contents << [['1234567890'], ['1234567890', '1234567890'], ['one', 'two', 'three']]
    }

    @Unroll
    def 'reject request before provider invocation: #description'() {
        when:
        def response = client.exchange(payload)

        then:
        response.statusCode.value() == 400
        response.body.contains(reason)
        mockLlmServer.mockServer.verify(0, postRequestedFor(urlEqualTo('/v1/chat/completions')))

        where:
        description       | payload                                                               | reason
        'model'           | request(['hi'], 'model3')                                              | 'model is not allowed'
        'model case'      | request(['hi'], 'MODEL1')                                              | 'model is not allowed'
        'message count'   | request(['1', '2', '3', '4'])                                          | 'message count'
        'message length'  | request(['12345678901'])                                               | 'message characters'
        'total length'    | request(['1234567890', '1234567890', '1'])                              | 'total message characters'
        'null messages'   | [model: 'model1', modelTemperature: 1, messages: null]                  | 'messages'
        'empty messages'  | request([])                                                           | 'at least one message'
        'null message'    | [model: 'model1', modelTemperature: 1, messages: [null]]                | 'message'
        'null content'    | request([null])                                                       | 'message.content'
        'null role'       | [model: 'model1', modelTemperature: 1, messages: [[content: 'hi']]]     | 'message.role'
        'temperature'     | [model: 'model1', modelTemperature: 3, messages: [[role: 'User', content: 'hi']]] | 'modelTemperature'
    }

    @Unroll
    def 'body byte limit accepts exact boundary and rejects extra byte - chunked=#chunked'() {
        given:
        String json = JsonOutput.toJson([model: 'model1', modelTemperature: 1, messages: [[role: 'User', content: 'hi']]])
        String atLimit = json + ' '.repeat(512 - json.getBytes('UTF-8').length)

        expect:
        client.exchangeRaw(atLimit, chunked).statusCode.value() == 200

        when:
        def response = client.exchangeRaw(atLimit + ' ', chunked)

        then:
        response.statusCode.value() == 413
        response.body.contains('byte limit')
        mockLlmServer.mockServer.verify(1, postRequestedFor(urlEqualTo('/v1/chat/completions')))

        where:
        chunked << [false, true]
    }

    def 'body limit counts UTF-8 bytes rather than characters'() {
        given:
        String json = '{"model":"model1","modelTemperature":1,"messages":[{"role":"User","content":"hi"}],"extra":"' + 'é'.repeat(220) + '"}'
        assert json.length() < 512
        assert json.getBytes('UTF-8').length > 512

        expect:
        client.exchangeRaw(json, true).statusCode.value() == 413
        mockLlmServer.mockServer.verify(0, postRequestedFor(urlEqualTo('/v1/chat/completions')))
    }

    def 'allowed alternate model reaches provider unchanged'() {
        expect:
        client.exchange(request(['hi'], 'model2')).statusCode.value() == 200
        mockLlmServer.mockServer.verify(1, postRequestedFor(urlEqualTo('/v1/chat/completions'))
                .withRequestBody(matchingJsonPath('$.model', equalTo('model2'))))
    }

    def 'model discovery only advertises allowed provider models'() {
        expect:
        skillsService.getAiModels().models.model == ['model1', 'model2']
    }

    static AiChatRequest request(List<String> contents = ['hi'], String model = 'model1') {
        new AiChatRequest(model: model, modelTemperature: 1d,
                messages: contents.collect { new AiChatRequest.ChatMessage(role: AiChatRequest.Role.User, content: it) })
    }
}
