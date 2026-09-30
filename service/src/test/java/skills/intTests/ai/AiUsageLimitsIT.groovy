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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.TestPropertySource
import skills.services.openai.OpenAIUsageLimiter
import skills.services.openai.OpenAIUsageLimitsProperties
import skills.intTests.utils.MockLlmServer
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import static com.github.tomakehurst.wiremock.client.WireMock.*

@Import(LimiterConfiguration)
@TestPropertySource(properties = [
        'skills.openai.limits.requestsPerMinutePerUser=2',
        'skills.openai.limits.requestsPerMinuteGlobal=3',
        'skills.openai.limits.maxConcurrentRequestsGlobal=1',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8192/status',
        'skills.authorization.userInfoUri=https://localhost:8192/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8192/userQuery?query={query}'
])
class AiUsageLimitsIT extends DefaultAiIntSpec {
    @Autowired
    TestLimiter limiter
    ChatClient client

    def setup() {
        limiter.now.addAndGet(TimeUnit.MINUTES.toNanos(2))
        client = new ChatClient(localPort, certificateRegistry)
    }

    def 'user rate boundary returns 429 and Retry-After then resets'() {
        expect:
        client.exchange(chatRequest()).statusCode.value() == 200
        client.exchange(chatRequest()).statusCode.value() == 200

        when:
        def response = client.exchange(chatRequest())

        then:
        response.statusCode.value() == 429
        response.body.contains('User AI request rate exceeded')
        response.headers.getFirst('Retry-After') == '60'
        mockLlmServer.mockServer.verify(2, postRequestedFor(urlEqualTo('/v1/chat/completions')))

        when:
        limiter.now.addAndGet(TimeUnit.SECONDS.toNanos(59))
        def beforeReset = client.exchange(chatRequest())

        then:
        beforeReset.statusCode.value() == 429
        beforeReset.headers.getFirst('Retry-After') == '1'

        when:
        limiter.now.addAndGet(TimeUnit.SECONDS.toNanos(1))

        then:
        client.exchange(chatRequest()).statusCode.value() == 200
    }

    def 'users have separate quotas and user rejections do not consume global quota'() {
        given:
        String user = getRandomUsers(1)[0]
        createService(user)
        ChatClient other = new ChatClient(localPort, certificateRegistry, user)

        expect:
        client.exchange(chatRequest()).statusCode.value() == 200
        client.exchange(chatRequest()).statusCode.value() == 200
        client.exchange(chatRequest()).statusCode.value() == 429
        other.exchange(chatRequest()).statusCode.value() == 200

        when:
        def response = other.exchange(chatRequest())

        then:
        response.statusCode.value() == 429
        response.body.contains('Global AI request rate exceeded')
        mockLlmServer.mockServer.verify(3, postRequestedFor(urlEqualTo('/v1/chat/completions')))
    }

    def 'invalid input consumes no allowance'() {
        when:
        4.times { assert client.exchange(chatRequest([])).statusCode.value() == 400 }

        then:
        client.exchange(chatRequest()).statusCode.value() == 200
        client.exchange(chatRequest()).statusCode.value() == 200
    }

    def 'active stream cap rejects immediately and completion releases capacity'() {
        given:
        def slow = slowStream()
        CountDownLatch firstChunk = new CountDownLatch(1)
        def first = client.stream(chatRequest()).doOnNext { firstChunk.countDown() }.collectList().toFuture()
        assert firstChunk.await(10, TimeUnit.SECONDS)

        when:
        def response = client.exchange(chatRequest())

        then:
        response.statusCode.value() == 429
        response.body.contains('concurrent request limit')
        response.headers.getFirst('Retry-After') == null

        when:
        first.get(10, TimeUnit.SECONDS)
        mockLlmServer.mockServer.removeStub(slow)

        then:
        client.exchange(chatRequest()).statusCode.value() == 200
        mockLlmServer.mockServer.verify(2, postRequestedFor(urlEqualTo('/v1/chat/completions')))

        cleanup:
        first.cancel(true)
    }

    def 'provider error releases active stream capacity'() {
        given:
        def failure = mockLlmServer.mockServer.stubFor(post(urlPathEqualTo('/v1/chat/completions')).atPriority(1)
                .willReturn(aResponse().withStatus(503).withHeader('Content-Type', 'application/json')
                        .withBody('{"error":{"message":"unavailable"}}')))

        expect:
        client.exchange(chatRequest()).statusCode.value() == 503

        when:
        mockLlmServer.mockServer.removeStub(failure)

        then:
        client.exchange(chatRequest()).statusCode.value() == 200
    }

    def 'client cancellation releases capacity once disconnect is observed'() {
        given:
        def slow = slowStream()
        CountDownLatch firstChunk = new CountDownLatch(1)
        def first = client.stream(chatRequest()).doOnNext { firstChunk.countDown() }.collectList().toFuture()
        assert firstChunk.await(10, TimeUnit.SECONDS)

        when:
        first.cancel(true)
        mockLlmServer.mockServer.removeStub(slow)

        then:
        new PollingConditions(timeout: 10).eventually {
            assert client.exchange(chatRequest()).statusCode.value() == 200
        }

        cleanup:
        first.cancel(true)
    }

    private def slowStream() {
        String body = (1..30).collect { MockLlmServer.createStreamMsg('partial ') }.join('\n\n') + '\n\ndata: [DONE]\n\n'
        return mockLlmServer.mockServer.stubFor(post(urlPathEqualTo('/v1/chat/completions')).atPriority(1)
                .willReturn(ok().withHeader('Content-Type', 'text/event-stream').withChunkedDribbleDelay(30, 3000).withBody(body)))
    }

    @TestConfiguration
    static class LimiterConfiguration {
        @Bean
        @Primary
        TestLimiter testLimiter(OpenAIUsageLimitsProperties limits) { new TestLimiter(limits) }
    }

    static class TestLimiter extends OpenAIUsageLimiter {
        final AtomicLong now = new AtomicLong(TimeUnit.DAYS.toNanos(1))
        TestLimiter(OpenAIUsageLimitsProperties limits) { super(limits) }
        @Override
        protected long nanoTime() { now.get() }
    }
}
