package skills.service.controller.exceptions

import groovy.util.logging.Slf4j
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
@Slf4j
class RestExceptionHandler extends ResponseEntityExceptionHandler {

    static class BasicErrBody {
        String message
        String errorCode = ErrorCode.InternalError
    }

    static class DomainSpecificErrBody extends BasicErrBody {
        String projectId
        String skillId
    }

    @ExceptionHandler(SkillException)
    protected ResponseEntity<Object> handleSkillException(Exception ex, WebRequest webRequest) {
        Object body
        if (ex instanceof SkillException) {
            body = new DomainSpecificErrBody(projectId: ex.projectId, skillId: ex.skillId, message: ex.message, errorCode: ex.errorCode.name())
            log.error("Exception for: projectId=[${ex.projectId}], skillId=${ex.skillId}", ex)
        } else {
            log.error("Unexpected exception type [${ex?.class?.simpleName}]", ex)
        }
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest)
    }

    @ExceptionHandler(DataIntegrityViolationException)
    protected ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException violationException, WebRequest webRequest) {
        log.error("Violation Exception", violationException)
        String msg = "Data Integrity Violation"
        BasicErrBody body = new BasicErrBody(message: msg, errorCode: ErrorCode.ConstraintViolation)
        return handleExceptionInternal(violationException, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest)
    }

    @ExceptionHandler(AccessDeniedException)
    protected ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException accessDeniedException, WebRequest webRequest) {
        log.warn("Access is denied - programmatic exception", accessDeniedException)
        String msg = "Access Denied"
        BasicErrBody body = new BasicErrBody(message: msg, errorCode: ErrorCode.AccessDenied)
        return handleExceptionInternal(accessDeniedException, body, new HttpHeaders(), HttpStatus.FORBIDDEN, webRequest)
    }

    /**
     * Supports the ResponseStatusException introduced in Spring 5
     */
    @ExceptionHandler(ResponseStatusException)
    protected ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, WebRequest webRequest) {
        BasicErrBody body = new BasicErrBody(message: ex.message)
        log.error(ex.message, ex)
        return handleExceptionInternal(ex, body, new HttpHeaders(), ex.status, webRequest)
    }

    /**
     * Fallback handler - this will any Exception that does have a more specific handler method
     * defined above.  This method will also support Exceptions that have the @ResponseStatus
     * annotation.
     */
    @ExceptionHandler([Throwable, Error])
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
            body = new BasicErrBody(message: message)
        } else {
            message = 'Unexpected Exception'
            body = new BasicErrBody(message: message)
        }
        log.error(message, ex)
        return handleExceptionInternal(ex, body, new HttpHeaders(), httpStatus, webRequest)
    }

}
