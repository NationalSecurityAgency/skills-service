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
package skills.services.openai

import groovy.json.JsonOutput
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import skills.controller.request.model.AiChatRequest
import skills.auth.UserInfoService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@Service
class OpenAIChatFacade {
    private final OpenAIRequestValidator validator
    private final OpenAIService provider
    private final OpenAIProviderErrors errors
    private final OpenAIUsageLimiter limiter
    private final UserInfoService userInfoService

    OpenAIChatFacade(OpenAIRequestValidator validator, OpenAIService provider, OpenAIProviderErrors errors,
                     OpenAIUsageLimiter limiter, UserInfoService userInfoService) {
        this.validator = validator
        this.provider = provider
        this.errors = errors
        this.limiter = limiter
        this.userInfoService = userInfoService
    }

    OpenAIService.AvailableModels getModels() {
        try {
            return provider.getAvailableModels()
        } catch (Exception failure) {
            throw errors.translate(failure, 'models', null, provider.openAiBaseUrl)
        }
    }

    Flux<ServerSentEvent<String>> streamChat(AiChatRequest request) {
        validator.validate(request)
        // Resolve identity before execution moves off the authenticated request thread.
        String userId = userInfoService.currentUser?.username
        if (!userId) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 'Authentication required')
        }
        // Admission errors bypass provider translation and retain their 429 response.
        return Flux.using({ limiter.acquire(userId) }, { OpenAIUsageLimiter.Permit ignored -> Flux.defer {
            boolean started = false
            Flux.defer { provider.streamChat(request) }
                    .map { String text ->
                        started = true
                        ServerSentEvent.builder(text).build()
                    }.onErrorResume { Throwable failure ->
                        def error = errors.translate(failure, 'chat/completions', request.model, provider.openAiHost)
                        if (!started) {
                            return Flux.error(error)
                        }
                        // Once SSE begins, HTTP status cannot change. Send a terminal named event.
                        String body = JsonOutput.toJson([explanation: error.reason, status: error.statusCode.value(),
                                                        retryAfter: error.headers.getFirst('Retry-After')])
                        return Flux.just(ServerSentEvent.builder(body).event('error').build())
                    }
        } }, { OpenAIUsageLimiter.Permit permit -> permit.close() })
    }
}
