/**
 * Copyright 2020 SkillTree
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

import groovy.util.logging.Slf4j
import org.apache.commons.collections4.MapUtils
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.lang.Nullable
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import skills.auth.SkillsAuthorizationException
import skills.controller.exceptions.SkillException.SkillExceptionLogLevel

@ControllerAdvice
@Slf4j
class RestExceptionHandler extends ResponseEntityExceptionHandler {

    static final List<ErrorCode> NOT_FOUND_CODES = [ErrorCode.SkillNotFound, ErrorCode.SubjectNotFound, ErrorCode.ProjectNotFound, ErrorCode.BadgeNotFound]

    static class BasicErrBody {
        String explanation
        String errorCode = ErrorCode.InternalError
        boolean success = false
    }

    static class DomainSpecificErrBody extends BasicErrBody {
        String projectId
        String skillId
        String userId
    }

    @ExceptionHandler(SkillException)
    protected ResponseEntity<Object> handleSkillException(Exception ex, WebRequest webRequest) {
        Object body
        HttpStatus status = HttpStatus.BAD_REQUEST
        if (ex instanceof SkillException) {
            body = new DomainSpecificErrBody(userId: ex.userId, projectId: ex.projectId, skillId: ex.skillId, explanation: ex.message, errorCode: ex.errorCode.name())
            String msg = "Exception for: projectId=[${ex.projectId}], skillId=${ex.skillId}, ${buildRequestInfo(webRequest)}"
            if (ex.userId) {
                msg = "${msg}, userId=[${ex.userId}]"
            }

            if ( ex.logLevel == SkillException.SkillExceptionLogLevel.ERROR){
                if (ex.printStackTrace) {
                    log.error(msg.toString(), ex)
                } else {
                    log.error(msg.toString() + ", exception message: [" + ex.message + "]")
                }
            } else if (ex.logLevel == SkillException.SkillExceptionLogLevel.WARN) {
                if (ex.printStackTrace) {
                    log.warn(msg.toString(), ex)
                } else {
                    log.warn(msg.toString() + ", exception message: [" + ex.message + "]")
                }
            } else if (ex.logLevel == SkillException.SkillExceptionLogLevel.INFO) {
                if (ex.printStackTrace) {
                    log.info(msg.toString(), ex)
                } else {
                    log.info(msg.toString() + ", exception message: [" + ex.message + "]")
                }
            }

            if (NOT_FOUND_CODES.contains(ex.errorCode)) {
                status = HttpStatus.NOT_FOUND
            }

        } else {
            log.error("Unexpected exception type [${ex?.class?.simpleName}], ${buildRequestInfo(webRequest)}", ex)
        }
        return new ResponseEntity(body, status)
    }

    @ExceptionHandler(DataIntegrityViolationException)
    protected ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException violationException, WebRequest webRequest) {
        String msg = "Data Integrity Violation"
        log.error("${buildRequestInfo(webRequest)} $msg", violationException)
        BasicErrBody body = new BasicErrBody(explanation: msg, errorCode: ErrorCode.ConstraintViolation)
        return new ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(AccessDeniedException)
    protected ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException accessDeniedException, WebRequest webRequest) {
        log.warn("Access is denied - programmatic exception", accessDeniedException)
        log.warn("Acesss is Denied to [${webRequest.getDescription(true)}]")
        String msg = "Access Denied"
        BasicErrBody body = new BasicErrBody(explanation: msg, errorCode: ErrorCode.AccessDenied)
        return new ResponseEntity(body, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(SkillsAuthorizationException)
    protected ResponseEntity<Object> handleSkillsAuthorizationException(SkillsAuthorizationException skillsAuthorizationException, WebRequest webRequest) {
        log.warn("Access is denied - programmatic exception", skillsAuthorizationException)
        String msg = "Access Denied"
        BasicErrBody body = new BasicErrBody(explanation: msg, errorCode: ErrorCode.AccessDenied)
        return new ResponseEntity(body, HttpStatus.FORBIDDEN)
    }

    @Override
    ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error("${buildRequestInfo(request)}, HttpMessageNotReadableException", ex)
        String msg = "${ex.message}"
        BasicErrBody body = new BasicErrBody(explanation: msg, errorCode: ErrorCode.BadParam)
        return new ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    /**
     * Supports the ResponseStatusException introduced in Spring 5
     */
    @ExceptionHandler(ResponseStatusException)
    protected ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, WebRequest webRequest) {
        BasicErrBody body = new BasicErrBody(explanation: ex.message)
        log.error("${buildRequestInfo(webRequest)} ${ex.message}", ex)
        return new ResponseEntity(body, ex.status);
    }

    /**
     * Fallback handler - this will any Exception that does have a more specific handler method
     * defined above.  This method will also support Exceptions that have the @ResponseStatus
     * annotation.
     */
    @ExceptionHandler([Throwable, Error, Exception])
    protected ResponseEntity<Object> handleOtherExceptions(Exception ex, WebRequest webRequest) {
        BasicErrBody body
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
        // check if the exception is annotated w/ @ResponseStatus
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class)
        String message
        if (responseStatus) {
            httpStatus = responseStatus.code()
            String reason = responseStatus.reason()
            message = "${httpStatus}${(reason ? " \"${reason}\"" : "")}"
            body = new BasicErrBody(explanation: message)
        } else {
            message = 'Unexpected Error'
            body = new BasicErrBody(explanation: message)
        }
        log.error("${buildRequestInfo(webRequest)}, $message", ex)
        // when calling handleExceptionInternal stack trace and err is added the response
        return new ResponseEntity(body, httpStatus);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error("${buildRequestInfo(request)} Handling exception", ex)
        return new ResponseEntity(body, headers, status);
    }

    private String buildRequestInfo(WebRequest request) {
        String uriInfoFrag = request.getDescription(false)

        String requestMethodFrag = ""
        if (request instanceof ServletWebRequest) {
            requestMethodFrag = "${request.getHttpMethod().toString()} "
        }

        List<String> params = []
        Map<String, String[]> parameterMap = request.getParameterMap()
        String paramsFrag = ""
        if (parameterMap) {
            parameterMap.each { String key, String[] value ->
                params.add("${key}=${value.join(',')}")
            }
            paramsFrag = ", params=[${params.join(", ")}]"
        }

        return "${requestMethodFrag}${uriInfoFrag}$paramsFrag"
    }

}
