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
package skills.auth

import tools.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.csrf.MissingCsrfTokenException
import skills.controller.exceptions.RestExceptionHandler
import skills.controller.exceptions.ErrorCode

/** Writes a safe, actionable response without exposing CSRF token values. */
@Slf4j
class CsrfAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper

    CsrfAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper
    }

    @Override
    void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) {
        String reason = exception instanceof MissingCsrfTokenException ? 'missing_token' : 'invalid_token'
        String path = request.requestURI.replaceAll('[\\r\\n]', '_')
        log.warn('CSRF validation failed: reason={} method={} path={}',
                reason, request.method, path)
        if (response.committed) {
            return
        }
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = 'UTF-8'
        RestExceptionHandler.BasicErrBody body = new RestExceptionHandler.BasicErrBody(
                errorCode: ErrorCode.CsrfValidationFailed.name(),
                explanation: 'CSRF validation failed: the security token is missing or invalid. Refresh the page, or obtain a fresh CSRF token if using a programmatic client, then try again.'
        )
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
