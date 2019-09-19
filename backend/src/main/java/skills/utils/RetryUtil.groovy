package skills.utils

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class RetryUtil {

    static Object withRetry(int numRetries, Closure closure) {
        for (int i = 0; i <= numRetries; i++) {
            try {
                return closure.call()
            } catch (Throwable t) {
                if (i == numRetries) {
                    throw t
                }
                log.error("With retry - attempt ${i + 1}/${numRetries}", t)
            }
        }
    }
}
