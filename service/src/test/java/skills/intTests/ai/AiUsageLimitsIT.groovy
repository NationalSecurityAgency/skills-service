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
import skills.intTests.utils.MockLlmServer.ControlledStream
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import static com.github.tomakehurst.wiremock.client.WireMock.*

@Import(LimiterConfiguration)
@TestPropertySource(properties = [
        'skills.openai.limits.requestsPerMinutePerUser=2',
        'skills.openai.limits.requestsPerMinuteGlobal=3',
        'skills.openai.limits.maxConcurrentRequestsGlobal=1'
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
        ControlledStream stream = mockLlmServer.stubControlledStream()
        CountDownLatch firstChunk = new CountDownLatch(1)
        CompletableFuture<List<String>> first = client.stream(chatRequest(['controlled stream']))
                .doOnNext { firstChunk.countDown() }.collectList().toFuture()
        assert firstChunk.await(10, TimeUnit.SECONDS): 'Controlled AI stream did not emit its first chunk'
        assert !first.isDone()

        when:
        def response = client.exchange(chatRequest())

        then:
        response.statusCode.value() == 429
        response.body.contains('concurrent request limit')
        response.headers.getFirst('Retry-After') == null

        when:
        stream.complete()
        first.get(10, TimeUnit.SECONDS)
        mockLlmServer.mockServer.removeStub(stream.stub)

        then:
        client.exchange(chatRequest()).statusCode.value() == 200
        mockLlmServer.mockServer.verify(2, postRequestedFor(urlEqualTo('/v1/chat/completions')))

        cleanup:
        first?.cancel(true)
        closeControlledStream(stream)
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
        ControlledStream stream = mockLlmServer.stubControlledStream()
        CountDownLatch firstChunk = new CountDownLatch(1)
        CompletableFuture<List<String>> first = client.stream(chatRequest(['controlled stream']))
                .doOnNext { firstChunk.countDown() }.collectList().toFuture()
        assert firstChunk.await(10, TimeUnit.SECONDS): 'Controlled AI stream did not emit its first chunk'
        assert !first.isDone()

        when:
        assert first.cancel(true)

        then:
        new PollingConditions(timeout: 10).eventually {
            // Keep emitting without completing so the servlet observes the disconnect.
            stream.sendChunk()
            assert client.exchange(chatRequest()).statusCode.value() == 200
        }

        cleanup:
        first?.cancel(true)
        closeControlledStream(stream)
    }

    private void closeControlledStream(ControlledStream stream) {
        if (stream != null) {
            stream.complete()
            mockLlmServer.mockServer.removeStub(stream.stub)
            if (stream.started.count == 0) {
                assert stream.finished.await(10, TimeUnit.SECONDS): 'Controlled AI stream did not close'
            }
        }
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
