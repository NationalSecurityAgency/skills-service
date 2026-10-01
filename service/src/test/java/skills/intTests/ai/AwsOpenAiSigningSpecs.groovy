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

import com.github.tomakehurst.wiremock.WireMockServer
import com.openai.client.OpenAIClient
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import skills.intTests.utils.MockLlmServer
import skills.services.openai.AwsOpenAiSigningInterceptor
import skills.services.openai.OpenAIChatConfig
import skills.services.openai.OpenAIService
import skills.services.openai.OpenAIUsageLimitsProperties
import skills.services.openai.OpenAIProviderErrors
import skills.controller.exceptions.SkillException
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

class AwsOpenAiSigningSpecs extends Specification {
    WireMockServer server
    OpenAIClient sdkClient

    def setup() {
        server = new WireMockServer(wireMockConfig().dynamicPort())
        server.start()
    }

    def cleanup() {
        sdkClient?.close()
        server.stop()
    }

    @Unroll
    def 'Spring AI signs streaming requests for #service with refreshed role credentials'() {
        given:
        server.stubFor(post(urlEqualTo('/v1/chat/completions'))
                .willReturn(ok().withHeader('Content-Type', 'text/event-stream')
                        .withBody(MockLlmServer.createStreamMessages().join('\n\n'))))
        AwsCredentialsProvider credentials = Mock()
        def signer = new AwsOpenAiSigningInterceptor(credentials, 'us-east-1', service)
        boolean customized = false
        OpenAiHttpClientBuilderCustomizer customizer = { builder -> customized = true }
        def config = new OpenAIChatConfig(aiHost: server.baseUrl() + '/v1', openAiKey: 'ignored-bearer-key', streamUsage: true)
        sdkClient = config.openAiClient(Optional.of(customizer), Optional.of(signer))
        def model = config.openAiChatModel(Optional.of(sdkClient))
        def options = OpenAiChatOptions.builder().model('test-model').maxCompletionTokens(100).build()

        when:
        def first = model.stream(new Prompt('Hello IAM', options)).collectList().block()
        def second = model.stream(new Prompt('Hello refreshed IAM', options)).collectList().block()

        then:
        1 * credentials.resolveCredentials() >> AwsSessionCredentials.create('first-access', 'first-secret', 'first-token')
        1 * credentials.resolveCredentials() >> AwsSessionCredentials.create('second-access', 'second-secret', 'second-token')
        customized
        first
        second
        server.verify(1, postRequestedFor(urlEqualTo('/v1/chat/completions'))
                .withHeader('Authorization', matching("AWS4-HMAC-SHA256 Credential=first-access/.*?/us-east-1/${service}/aws4_request,.*Signature=[a-f0-9]{64}"))
                .withHeader('X-Amz-Security-Token', equalTo('first-token'))
                .withHeader('X-Amz-Date', matching('[0-9]{8}T[0-9]{6}Z'))
                .withRequestBody(containing('Hello IAM'))
                .withRequestBody(matchingJsonPath('$.max_completion_tokens', equalTo('100'))))
        server.verify(1, postRequestedFor(urlEqualTo('/v1/chat/completions'))
                .withHeader('Authorization', matching("AWS4-HMAC-SHA256 Credential=second-access/.*?/us-east-1/${service}/aws4_request,.*Signature=[a-f0-9]{64}"))
                .withHeader('X-Amz-Security-Token', equalTo('second-token'))
                .withRequestBody(containing('Hello refreshed IAM')))

        where:
        service << ['bedrock-mantle', 'bedrock']
    }

    def 'SDK model discovery signs GET requests and preserves model filtering and creation dates'() {
        given:
        server.stubFor(get(urlEqualTo('/v1/models')).willReturn(okJson('''{"object":"list","data":[
            {"id":"test-model","created":1700000000,"object":"model","owned_by":"test"},
            {"id":"undated-model","object":"model","owned_by":"test"},
            {"id":"excluded-model","created":1700000001,"object":"model","owned_by":"test"}
        ]}''')))
        AwsCredentialsProvider credentials = Mock()
        def signer = new AwsOpenAiSigningInterceptor(credentials, 'us-east-1', 'bedrock')
        def config = new OpenAIChatConfig(aiHost: server.baseUrl() + '/v1', openAiKey: 'ignored-bearer-key')
        sdkClient = config.openAiClient(Optional.empty(), Optional.of(signer))
        def provider = new OpenAIService(openAiClient: sdkClient, usageLimits: new OpenAIUsageLimitsProperties(
                allowedModels: ['test-model', 'undated-model'] as Set))

        when:
        def response = provider.getAvailableModels()

        then:
        1 * credentials.resolveCredentials() >> AwsSessionCredentials.create('model-access', 'model-secret', 'model-token')
        response.models*.model == ['test-model', 'undated-model']
        response.models[0].created == new Date(1700000000000L)
        response.models[1].created == null
        server.verify(1, getRequestedFor(urlEqualTo('/v1/models'))
                .withHeader('Authorization', matching('AWS4-HMAC-SHA256 Credential=model-access/.*?/us-east-1/bedrock/aws4_request,.*Signature=[a-f0-9]{64}'))
                .withHeader('X-Amz-Security-Token', equalTo('model-token')))
    }

    def 'chat and model discovery use Spring Cloud AWS configured credentials and region'() {
        given:
        server.stubFor(post(urlEqualTo('/v1/chat/completions'))
                .willReturn(ok().withHeader('Content-Type', 'text/event-stream')
                        .withBody(MockLlmServer.createStreamMessages().join('\n\n'))))
        server.stubFor(get(urlEqualTo('/v1/models')).willReturn(okJson('{"data":[{"id":"test-model"}]}')))
        def runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CredentialsProviderAutoConfiguration, RegionProviderAutoConfiguration))
                .withUserConfiguration(OpenAIChatConfig)
                .withPropertyValues(
                        "skills.openai.host=${server.baseUrl()}/v1",
                        'skills.openai.aws.enabled=true',
                        'spring.cloud.aws.credentials.access-key=shared-access',
                        'spring.cloud.aws.credentials.secret-key=shared-secret',
                        'spring.cloud.aws.region.static=us-west-2')

        expect:
        runner.run { context ->
            assert !context.startupFailure
            assert context.getBeansOfType(AwsCredentialsProvider).size() == 1
            assert !context.containsBean('openAiAwsCredentialsProvider')
            def model = context.getBean(OpenAiChatModel)
            def options = OpenAiChatOptions.builder().model('test-model').build()
            assert model.stream(new Prompt('Using shared AWS providers', options)).collectList().block()
            def provider = new OpenAIService(openAiClient: context.getBean(OpenAIClient),
                    usageLimits: new OpenAIUsageLimitsProperties())
            assert provider.getAvailableModels().models*.model == ['test-model']
        }
        server.verify(1, postRequestedFor(urlEqualTo('/v1/chat/completions'))
                .withHeader('Authorization', matching('AWS4-HMAC-SHA256 Credential=shared-access/.*?/us-west-2/bedrock/aws4_request,.*Signature=[a-f0-9]{64}')))
        server.verify(1, getRequestedFor(urlEqualTo('/v1/models'))
                .withHeader('Authorization', matching('AWS4-HMAC-SHA256 Credential=shared-access/.*?/us-west-2/bedrock/aws4_request,.*Signature=[a-f0-9]{64}')))
    }

    def 'synchronous grading uses the same signed SDK transport'() {
        given:
        server.stubFor(post(urlEqualTo('/v1/chat/completions')).willReturn(okJson('''{
            "id":"grading-response","object":"chat.completion","created":1700000000,"model":"test-model",
            "choices":[{"index":0,"finish_reason":"stop","message":{"role":"assistant",
                "content":"{\\"confidenceLevel\\":95,\\"gradingDecisionReason\\":\\"Correct answer\\"}"}}],
            "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}
        }''')))
        AwsCredentialsProvider credentials = Mock()
        def signer = new AwsOpenAiSigningInterceptor(credentials, 'us-east-1', 'bedrock')
        def config = new OpenAIChatConfig(aiHost: server.baseUrl() + '/v1', streamUsage: true)
        sdkClient = config.openAiClient(Optional.empty(), Optional.of(signer))
        def provider = new OpenAIService(openAiHost: config.aiHost, gradingModel: 'test-model',
                gradingModelTemperature: 0d, textInputQuestionGradingMsg: 'Grade the answer.',
                chatModel: config.openAiChatModel(Optional.of(sdkClient)))

        when:
        def result = provider.gradeTextInputQuizAnswer('What is 2 + 2?', '4', 90, '4')

        then:
        1 * credentials.resolveCredentials() >> AwsSessionCredentials.create('grading-access', 'grading-secret', 'grading-token')
        result.confidenceLevel == 95
        result.gradingDecisionReason == 'Correct answer'
        server.verify(1, postRequestedFor(urlEqualTo('/v1/chat/completions'))
                .withHeader('Authorization', matching('AWS4-HMAC-SHA256 Credential=grading-access/.*?/us-east-1/bedrock/aws4_request,.*Signature=[a-f0-9]{64}'))
                .withHeader('X-Amz-Security-Token', equalTo('grading-token')))
    }

    def 'SDK model discovery failures retain sanitized provider error translation without retrying'() {
        given:
        server.stubFor(get(urlEqualTo('/v1/models')).willReturn(aResponse().withStatus(401)
                .withHeader('Content-Type', 'application/json')
                .withBody('{"error":{"message":"private-provider-detail","code":"invalid_api_key","type":"permission_denied_error"}}')))
        def config = new OpenAIChatConfig(aiHost: server.baseUrl() + '/v1', openAiKey: 'test-key')
        sdkClient = config.openAiClient(Optional.empty(), Optional.empty())
        def provider = new OpenAIService(openAiClient: sdkClient, usageLimits: new OpenAIUsageLimitsProperties())

        when:
        provider.getAvailableModels()

        then:
        SkillException failure = thrown()
        def translated = new OpenAIProviderErrors().translate(failure, 'models', null, config.aiHost)
        translated.statusCode.value() == 502
        !translated.reason.contains('private-provider-detail')
        server.verify(1, getRequestedFor(urlEqualTo('/v1/models')).withHeader('Authorization', equalTo('Bearer test-key')))
    }
}
