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
package skills.controller

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.springframework.web.bind.annotation.*
import skills.profile.EnableCallStackProf

@RestController
@RequestMapping("/public")
@Slf4j
@EnableCallStackProf
class ClientLoggingController {
    // Predefined logging levels.
//    Logger.TRACE = defineLogLevel(1, 'TRACE');
//    Logger.DEBUG = defineLogLevel(2, 'DEBUG');
//    Logger.INFO = defineLogLevel(3, 'INFO');
//    Logger.TIME = defineLogLevel(4, 'TIME');
//    Logger.WARN = defineLogLevel(5, 'WARN');
//    Logger.ERROR = defineLogLevel(8, 'ERROR');
//    Logger.OFF = defineLogLevel(99, 'OFF');

    @CrossOrigin
    @RequestMapping(value = "/log", method = [RequestMethod.PUT, RequestMethod.POST])
    @ResponseBody
    boolean writeLog(@RequestBody LogMessage logMessage) {
        switch (logMessage.level.value) {
            case 1:
                log.trace(logMessage.message)
                break
            case 2:
                log.debug(logMessage.message)
                break
            case 3:
                log.info(logMessage.message)
                break
            case 5:
                log.warn(logMessage.message)
                break
            case 8:
                log.error(logMessage.message)
                break
            default:
                throw new IllegalArgumentException("Unexpected log level [${logMessage}]")
        }
        return true
    }

    @ToString
    static class LogMessage {
        // {"message":"We are initialized!","level":{"value":3,"name":"INFO"}}
        String message
        LogLevel level
    }

    @ToString
    static class LogLevel {
        String name
        Integer value
    }
}
