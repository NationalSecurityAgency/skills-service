package skills.utils

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.exception.ExceptionUtils

@CompileStatic
@Slf4j
class RetryUtil {

    static Object withRetry(int numRetries, Closure closure) {
        return withRetry(numRetries, true, closure)
    }

    static Object withRetry(int numRetries, boolean logOnlyOnCompleteFailure, Closure closure) {
        String attemptsId = null;
        StringBuilder errMsBuilder
        for (int i = 0; i <= numRetries; i++) {
            try {
                Object res = closure.call()
                if (attemptsId && !logOnlyOnCompleteFailure) {
                    log.error("Retry [${attemptsId}] succeeded!!")
                }
                return res
            } catch (Throwable t) {
                if (!attemptsId) {
                    attemptsId = UUID.randomUUID().toString()
                }
                if (i == numRetries) {
                    if (logOnlyOnCompleteFailure) {
                        log.error(errMsBuilder.toString())
                    }
                    throw t
                }
                if (!logOnlyOnCompleteFailure) {
                    log.error("Retry [${attemptsId}] - attempt ${i + 1}/${numRetries}", t)
                } else {
                    if (!errMsBuilder) {
                        errMsBuilder = new StringBuilder()
                        errMsBuilder.append("Retry [${attemptsId}] - attempt ${i + 1}/${numRetries}:\n")
                        errMsBuilder.append(ExceptionUtils.getMessage(t))
                        errMsBuilder.append("\n")
                    }
                }
            }
        }
    }
}
