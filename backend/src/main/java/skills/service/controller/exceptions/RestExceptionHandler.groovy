package skills.service.controller.exceptions

import groovy.util.logging.Slf4j
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

    static class ErrBody{
        String projectId
        String skillId
        String errorMsg
    }

    @ExceptionHandler(SkillException)
    protected ResponseEntity<Object> handleSkillException(SkillException ex, WebRequest webRequest){
        Object body
        if(ex instanceof SkillException){
            body = new ErrBody(projectId: ex.projectId, skillId: ex.skillId, errorMsg: ex.message)
            log.error("Exception for: projectId=[${ex.projectId}], skillId=${ex.skillId}", ex)
        } else {
            log.error("Unexpected exception type [${ex?.class?.simpleName}]", ex)
        }
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest)
    }

}
