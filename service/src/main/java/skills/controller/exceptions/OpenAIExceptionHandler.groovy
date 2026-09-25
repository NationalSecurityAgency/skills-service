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
package skills.controller.exceptions

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import skills.controller.OpenAiController

/** Errors before SSE starts must remain JSON even when the client only accepts event streams. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = OpenAiController)
class OpenAIExceptionHandler {
    @ExceptionHandler(SkillException)
    ResponseEntity<?> invalidRequest(SkillException exception) {
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                .body(new RestExceptionHandler.BasicErrBody(explanation: exception.message, errorCode: exception.errorCode.name()))
    }

    @ExceptionHandler(ResponseStatusException)
    ResponseEntity<?> rejectedRequest(ResponseStatusException exception) {
        return ResponseEntity.status(exception.statusCode).headers(exception.headers).contentType(MediaType.APPLICATION_JSON)
                .body(new RestExceptionHandler.BasicErrBody(explanation: exception.reason, errorCode: ErrorCode.BadParam))
    }

    @ExceptionHandler(HttpMessageNotReadableException)
    ResponseEntity<?> unreadableRequest(HttpMessageNotReadableException ignored) {
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                .body(new RestExceptionHandler.BasicErrBody(explanation: 'Invalid AI chat request', errorCode: ErrorCode.BadParam))
    }
}
