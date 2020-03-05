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
package skills.utils

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.exception.ExceptionUtils
import skills.controller.exceptions.SkillException

@CompileStatic
@Slf4j
class RetryUtil {

    static Object withRetry(int numRetries, Closure closure) {
        return withRetry(numRetries, true, closure)
    }

    static Object withRetry(int numRetries, boolean logOnlyOnCompleteFailure, Closure closure) {
        String attemptsId = null;
        StringBuilder errMsBuilder
        for (int i = 0; (i <= numRetries); i++) {
            try {
                Object res = closure.call()
                if (attemptsId && !logOnlyOnCompleteFailure) {
                    log.error("Retry [${attemptsId}] succeeded!!")
                }
                return res
            } catch (Throwable t) {
                if (t instanceof SkillException) {
                    if (t.doNotRetry){
                        throw t
                    }
                }

                if (!attemptsId) {
                    attemptsId = UUID.randomUUID().toString()
                }
                if (i == numRetries) {
                    if (logOnlyOnCompleteFailure) {
                        if(!errMsBuilder) { //this happens if numRetries is 0
                            String msg = "Retry [${attemptsId}] - attempt ${i + 1}/${numRetries}:\n"
                            errMsBuilder = appendMsg(errMsBuilder, msg, t)
                        }
                        log.error(errMsBuilder.toString())
                    }
                    throw t
                }
                if (!logOnlyOnCompleteFailure) {
                    log.error("Retry [${attemptsId}] - attempt ${i + 1}/${numRetries}", t)
                } else {
                    String msg = "Retry [${attemptsId}] - attempt ${i + 1}/${numRetries}:\n"
                    errMsBuilder = appendMsg(errMsBuilder, msg, t)
                }
            }
        }
    }

    private static StringBuilder appendMsg(StringBuilder errMsBuilder, String msg, Throwable t) {
        if (!errMsBuilder) {
            errMsBuilder = new StringBuilder()
            errMsBuilder.append("\n")
        }
        errMsBuilder.append(msg)
        errMsBuilder.append(ExceptionUtils.getStackTrace(t))
        errMsBuilder.append("\n")

        return errMsBuilder
    }
}
