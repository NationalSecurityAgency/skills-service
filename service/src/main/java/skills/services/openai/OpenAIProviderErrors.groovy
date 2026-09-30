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

import com.openai.errors.OpenAIServiceException
import com.openai.errors.OpenAIIoException
import groovy.util.logging.Slf4j
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.server.ResponseStatusException

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeoutException

@Component
@Slf4j
class OpenAIProviderErrors {
    ResponseStatusException translate(Throwable failure, String operation = 'chat', String model = null, String baseUrl = null) {
        List<Throwable> causes = []
        Throwable current = failure
        while (current != null && !causes.any { it.is(current) }) {
            causes.add(current)
            current = current.cause
        }
        OpenAIServiceException upstream = causes.find { it instanceof OpenAIServiceException } as OpenAIServiceException
        RestClientResponseException restError = causes.find { it instanceof RestClientResponseException } as RestClientResponseException
        int status = upstream?.statusCode() ?: restError?.statusCode?.value() ?: 0
        boolean timeout = causes.any { it instanceof InterruptedIOException || it instanceof TimeoutException }
        HttpStatus result = status == 429 ? HttpStatus.TOO_MANY_REQUESTS :
                (timeout || status in [408, 504]) ? HttpStatus.GATEWAY_TIMEOUT :
                        (status >= 500 || causes.any { it instanceof ConnectException || it instanceof OpenAIIoException }) ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY
        String message = result == HttpStatus.TOO_MANY_REQUESTS ? 'AI usage limit reached. Please try again later or contact your administrator.' :
                result == HttpStatus.GATEWAY_TIMEOUT ? 'The AI provider timed out. Please try again.' :
                        result == HttpStatus.SERVICE_UNAVAILABLE ? 'The AI provider is temporarily unavailable. Please try again later.' :
                                'The AI provider could not complete the request. Please contact your administrator.'
        if (status == 404) {
            message = 'The AI provider returned HTTP 404 (Not Found). The requested API endpoint or model may be unavailable. Please contact your administrator.'
        }
        String requestId = (upstream?.headers()?.values('x-request-id') ?: restError?.responseHeaders?.get('x-request-id'))?.find()
        String providerCode = readDiagnostic { upstream?.code()?.orElse(null) }
        String providerType = readDiagnostic { upstream?.type()?.orElse(null) }
        Throwable root = upstream ?: restError ?: causes.last()
        // Log routing context and structured diagnostics, never provider bodies or credentials.
        log.warn('AI provider request failed: operation=[{}], providerBaseUrl=[{}], model=[{}], exceptionType=[{}], providerStatus=[{}], providerCode=[{}], providerType=[{}], providerRequestId=[{}]. {}',
                safeIdentifier(operation), safeBaseUrl(baseUrl), safeIdentifier(model), root.class.simpleName, status,
                providerCode, providerType, safeIdentifier(requestId),
                status == 404 ? 'Verify skills.openai.host (including the API base path), gateway routing, and the selected model/deployment and its access permissions.' : message)
        String retryAfter = (upstream?.headers()?.values('retry-after') ?: restError?.responseHeaders?.get('Retry-After'))?.find { validRetryAfter(it) }
        return new ProviderException(result, message, retryAfter)
    }

    private static String readDiagnostic(Closure<String> read) {
        try {
            return safeIdentifier(read.call())
        } catch (RuntimeException ignored) {
            // SDK accessors can throw for missing or malformed fields, even when returning Optional.
            // Optional diagnostics must never replace the original provider failure.
            return '(unavailable)'
        }
    }

    private static String safeIdentifier(String value) {
        return value && value ==~ /[a-zA-Z0-9_.:\/@-]{1,200}/ ? value : '(unavailable)'
    }

    private static String safeBaseUrl(String value) {
        try {
            URI uri = new URI(value)
            if (!(uri.scheme in ['http', 'https']) || !uri.host) {
                return '(unavailable)'
            }
            // Drop URL user-info, query parameters and fragments, which can carry credentials.
            return new URI(uri.scheme, null, uri.host, uri.port, uri.path, null, null).toASCIIString()
        } catch (Exception ignored) {
            return '(unavailable)'
        }
    }

    private static boolean validRetryAfter(String value) {
        if (value ==~ /[0-9]{1,10}/) {
            return true
        }
        try {
            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
            return true
        } catch (Exception ignored) {
            return false
        }
    }

    static class ProviderException extends ResponseStatusException {
        private final HttpHeaders responseHeaders = new HttpHeaders()

        ProviderException(HttpStatus status, String reason, String retryAfter) {
            super(status, reason)
            if (retryAfter != null) {
                responseHeaders.set('Retry-After', retryAfter)
            }
        }

        @Override
        HttpHeaders getHeaders() { responseHeaders }
    }
}
