package skills.controller.exceptions

import groovy.util.logging.Slf4j
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
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import skills.auth.SkillsAuthorizationException

@ControllerAdvice
@Slf4j
class RestExceptionHandler extends ResponseEntityExceptionHandler {

    static class BasicErrBody {
        String explanation
        String errorCode = ErrorCode.InternalError
        boolean success = false
    }

    static class DomainSpecificErrBody extends BasicErrBody {
        String projectId
        String skillId
    }

    @ExceptionHandler(SkillException)
    protected ResponseEntity<Object> handleSkillException(Exception ex, WebRequest webRequest) {
        Object body
        if (ex instanceof SkillException) {
            body = new DomainSpecificErrBody(projectId: ex.projectId, skillId: ex.skillId, explanation: ex.message, errorCode: ex.errorCode.name())
            log.error("Exception for: projectId=[${ex.projectId}], skillId=${ex.skillId}", ex)
        } else {
            log.error("Unexpected exception type [${ex?.class?.simpleName}]", ex)
        }
        return new ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(DataIntegrityViolationException)
    protected ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException violationException, WebRequest webRequest) {
        String msg = "Data Integrity Violation"
        log.error(msg, violationException)
        BasicErrBody body = new BasicErrBody(explanation: msg, errorCode: ErrorCode.ConstraintViolation)
        return new ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(AccessDeniedException)
    protected ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException accessDeniedException, WebRequest webRequest) {
        log.warn("Access is denied - programmatic exception", accessDeniedException)
        String msg = "Access Denied"
        BasicErrBody body = new BasicErrBody(explanation: msg, errorCode: ErrorCode.AccessDenied)
        return new ResponseEntity(body, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(SkillsAuthorizationException)
    protected ResponseEntity<Object> handleSkillsAuthorizationException(SkillsAuthorizationException skillsAuthorizationException, WebRequest webRequest) {
        log.warn("Access is denied - programmatic exception", skillsAuthorizationException)
        String msg = "Access Denied"
        BasicErrBody body = new BasicErrBody(explanation: msg, errorCode: ErrorCode.AccessDenied)
        return new ResponseEntity(body, HttpStatus.UNAUTHORIZED)
    }

    @Override
    ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error("HttpMessageNotReadableException", ex)
        String msg = ex.message
        BasicErrBody body = new BasicErrBody(explanation: msg, errorCode: ErrorCode.BadParam)
        return new ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    /**
     * Supports the ResponseStatusException introduced in Spring 5
     */
    @ExceptionHandler(ResponseStatusException)
    protected ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, WebRequest webRequest) {
        BasicErrBody body = new BasicErrBody(explanation: ex.message)
        log.error(ex.message, ex)
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
        log.error(message, ex)
        // when calling handleExceptionInternal stack trace and err is added the response
        return new ResponseEntity(body, httpStatus);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error("Handling exception", ex)
        return new ResponseEntity(body, headers, status);
    }

}
