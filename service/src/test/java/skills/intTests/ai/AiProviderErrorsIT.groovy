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

import org.springframework.test.context.TestPropertySource
import skills.intTests.utils.MockLlmServer
import spock.lang.Unroll
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import skills.services.openai.OpenAIProviderErrors

import static com.github.tomakehurst.wiremock.client.WireMock.*

@TestPropertySource(properties = [
        'skills.openai.options.timeoutInSecs=1',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8191/status',
        'skills.authorization.userInfoUri=https://localhost:8191/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8191/userQuery?query={query}'
])
class AiProviderErrorsIT extends DefaultAiIntSpec {
    ChatClient client

    def setup() {
        client = new ChatClient(localPort, certificateRegistry)
    }

    @Unroll
    def 'provider #providerStatus becomes sanitized #expected without retrying'() {
        given:
        def stub = mockLlmServer.mockServer.stubFor(post(urlPathEqualTo('/v1/chat/completions')).atPriority(1)
                .willReturn(aResponse().withStatus(providerStatus).withHeader('Content-Type', 'application/json')
                        .withHeader('Retry-After', retryAfter)
                        .withBody('{"error":{"message":"private-provider-detail","type":"insufficient_quota","code":"insufficient_quota"}}')))

        when:
        def response = client.exchange(chatRequest())

        then:
        response.statusCode.value() == expected
        response.headers.contentType.toString().startsWith('application/json')
        response.body.contains(message)
        !response.body.contains('private-provider-detail')
        response.headers.getFirst('Retry-After') == expectedRetry
        mockLlmServer.mockServer.verify(1, postRequestedFor(urlEqualTo('/v1/chat/completions')))

        when:
        mockLlmServer.mockServer.removeStub(stub)

        then:
        client.exchange(chatRequest()).statusCode.value() == 200

        where:
        providerStatus | expected | retryAfter                       | expectedRetry                    | message
        429            | 429      | '30'                             | '30'                             | 'usage limit reached'
        429            | 429      | 'Wed, 21 Oct 2037 07:28:00 GMT'    | 'Wed, 21 Oct 2037 07:28:00 GMT'    | 'usage limit reached'
        429            | 429      | 'invalid'                        | null                             | 'usage limit reached'
        503            | 503      | '5'                              | '5'                              | 'temporarily unavailable'
        500            | 503      | 'invalid'                        | null                             | 'temporarily unavailable'
        504            | 504      | 'invalid'                        | null                             | 'timed out'
        401            | 502      | 'invalid'                        | null                             | 'could not complete'
        403            | 502      | 'invalid'                        | null                             | 'could not complete'
        400            | 502      | 'invalid'                        | null                             | 'could not complete'
        404            | 502      | 'invalid'                        | null                             | 'HTTP 404 (Not Found)'
    }

    def 'empty provider 404 gives actionable diagnostics instead of Unknown'() {
        given:
        Logger logger = (Logger) LoggerFactory.getLogger(OpenAIProviderErrors)
        ListAppender appender = new ListAppender()
        appender.start()
        logger.addAppender(appender)
        mockLlmServer.mockServer.stubFor(post(urlPathEqualTo('/v1/chat/completions')).atPriority(1)
                .willReturn(aResponse().withStatus(404).withHeader('x-request-id', 'provider-request-404')))

        when:
        def response = client.exchange(chatRequest())

        then:
        response.statusCode.value() == 502
        response.body.contains('HTTP 404 (Not Found)')
        response.body.contains('API endpoint or model may be unavailable')
        !response.body.contains('Unknown')
        !response.body.contains('localhost')
        def diagnostic = appender.list.find { it.formattedMessage.startsWith('AI provider request failed:') }?.formattedMessage
        diagnostic.contains('operation=[chat/completions]')
        diagnostic.contains('localhost:50001/v1')
        diagnostic.contains('model=[model1]')
        diagnostic.contains('exceptionType=[NotFoundException]')
        diagnostic.contains('providerRequestId=[provider-request-404]')
        diagnostic.contains('Verify skills.openai.host')

        cleanup:
        logger.detachAppender(appender)
        appender.stop()
    }

    @Unroll
    def 'incomplete provider metadata preserves HTTP #status and permits a subsequent request'() {
        given:
        def failure = mockLlmServer.mockServer.stubFor(post(urlPathEqualTo('/v1/chat/completions')).atPriority(1)
                .willReturn(aResponse().withStatus(status).withHeader('Content-Type', 'application/json')
                        .withHeader('Retry-After', '7').withBody(body)))

        when:
        def response = client.exchange(chatRequest())

        then:
        response.statusCode.value() == status
        response.headers.getFirst('Retry-After') == '7'
        response.body.contains(explanation)
        !response.body.contains('private-provider-detail')
        mockLlmServer.mockServer.verify(1, postRequestedFor(urlEqualTo('/v1/chat/completions')))

        when:
        mockLlmServer.mockServer.removeStub(failure)

        then:
        client.exchange(chatRequest()).statusCode.value() == 200

        where:
        status | body                                                                                       | explanation
        503    | '{"error":{"message":"private-provider-detail"}}'                                          | 'temporarily unavailable'
        503    | '{"error":{"message":"private-provider-detail","type":null,"code":42}}'                    | 'temporarily unavailable'
        429    | '{"error":{"message":"private-provider-detail","type":{},"code":[]}}'                      | 'usage limit reached'
    }

    def 'provider timeout returns 504 without exposing details'() {
        given:
        mockLlmServer.mockServer.stubFor(post(urlPathEqualTo('/v1/chat/completions')).atPriority(1)
                .willReturn(ok().withFixedDelay(2000).withHeader('Content-Type', 'text/event-stream')
                        .withBody(MockLlmServer.createStreamMessages().join('\n\n'))))

        expect:
        client.exchange(chatRequest()).statusCode.value() == 504
    }

    def 'model discovery also sanitizes provider failures'() {
        given:
        mockLlmServer.mockServer.stubFor(get(urlPathEqualTo('/v1/models')).atPriority(1)
                .willReturn(aResponse().withStatus(503).withHeader('Content-Type', 'application/json')
                        .withBody('{"error":"private-provider-detail"}')))

        when:
        skillsService.getAiModels()

        then:
        skills.intTests.utils.SkillsClientException exception = thrown()
        exception.resBody.contains('temporarily unavailable')
        !exception.resBody.contains('private-provider-detail')
    }

    def 'provider stream failure preserves partial output and sends terminal error event'() {
        given:
        String body = MockLlmServer.createStreamMsg('Partial answer') + '\n\n' +
                'data: {"error":{"message":"private-provider-detail","type":"server_error"}}\n\n'
        mockLlmServer.mockServer.stubFor(post(urlPathEqualTo('/v1/chat/completions')).atPriority(1)
                .willReturn(ok().withHeader('Content-Type', 'text/event-stream').withChunkedDribbleDelay(2, 100).withBody(body)))

        when:
        def response = client.exchange(chatRequest())

        then:
        response.statusCode.value() == 200
        response.body.contains('data:Partial answer')
        response.body.contains('event:error')
        response.body.contains('explanation')
        !response.body.contains('private-provider-detail')
        mockLlmServer.mockServer.verify(1, postRequestedFor(urlEqualTo('/v1/chat/completions')))
    }
}
