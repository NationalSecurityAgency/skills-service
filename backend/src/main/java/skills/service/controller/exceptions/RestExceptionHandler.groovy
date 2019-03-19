package skills.service.controller.exceptions

import groovy.util.logging.Slf4j
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
@Slf4j
class RestExceptionHandler extends ResponseEntityExceptionHandler{

    static class BasicErrBody {
        String message
        String errorCode
    }

    static class DomainSpecificErrBody extends BasicErrBody {
        String projectId
        String skillId
    }

    @ExceptionHandler(SkillException)
    protected ResponseEntity<Object> handleSkillException(SkillException ex, WebRequest webRequest){
        Object body
        if(ex instanceof SkillException){
            body = new DomainSpecificErrBody(projectId: ex.projectId, skillId: ex.skillId, message: ex.message, errorCode: ex.errorCode.name())
            log.error("Exception for: projectId=[${ex.projectId}], skillId=${ex.skillId}", ex)
        } else {
            log.error("Unexpected exception type [${ex?.class?.simpleName}]", ex)
        }
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest)
    }

    private static Map<String, String> constraintNameToMsgMapping = Collections.unmodifiableMap([
            "unique_name" : "Provided project name already exist.",
            "project_id_2": "Provided project id already exist.",
            "project_id_3": "Provided subject name already exist."
    ])

    @ExceptionHandler(DataIntegrityViolationException)
    protected ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException violationException, WebRequest webRequest) {
        log.error("Violation Exception", violationException)
        String msg = "Data Integrity Violation"
        BasicErrBody body = new BasicErrBody(message: msg, errorCode: ErrorCode.ConstraintViolation)
        return handleExceptionInternal(violationException, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest)
    }

}
