package skills.controller.exceptions

import groovy.util.logging.Slf4j
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException

@Slf4j
class DataIntegrityViolationExceptionHandler {

    private final Map<String, String> constraintNameToMsgMapping

    DataIntegrityViolationExceptionHandler(Map<String, String> constraintNameToMsgMapping) {
        this.constraintNameToMsgMapping = constraintNameToMsgMapping
    }

    Object handle(String projectId, Closure closure) {
        return handle(projectId, null, closure)
    }

    Object handle(String projectId, String skillId, Closure closure) {
        try {
            return closure.call()
        } catch (DataIntegrityViolationException violationException) {
            log.error("Violation Exception", violationException)
            String msg = "Data Integrity Violation"
            if (violationException.cause instanceof ConstraintViolationException) {
                ConstraintViolationException constraintViolationException = violationException.cause
                def entry = constraintNameToMsgMapping.find {
                    boolean generalCase = it.key.equalsIgnoreCase(constraintViolationException.constraintName)
                    boolean h2DBCase = constraintViolationException.constraintName.toUpperCase().startsWith("\"${it.key.toUpperCase()}")
                    generalCase || h2DBCase
                }
                if (entry) {
                    msg = entry.value
                } else {
                    log.warn("Failed to locate error explanation for the constraint name [{}], please consider adding one!", constraintViolationException.constraintName)
                }
            }

            if (projectId) {
                msg = "${msg}; ProjectId=[${projectId}]"
            }

            if (skillId) {
                msg = "${msg}; SkillId=[${skillId}]"
            }

            throw new SkillException(msg, projectId, skillId, ErrorCode.ConstraintViolation)
        }
    }
}
