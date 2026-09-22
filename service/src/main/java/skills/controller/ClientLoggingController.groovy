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

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import skills.UIConfigProperties
import skills.dbupgrade.DBUpgradeSafe
import skills.profile.EnableCallStackProf

import java.time.Duration
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/public")
@Slf4j
@EnableCallStackProf
class ClientLoggingController {
    static final int DEFAULT_MAX_MESSAGE_LENGTH = 2000
    static final int DEFAULT_MAX_REQUESTS_PER_MINUTE = 60
    static final int DEFAULT_MAX_TRACKED_CLIENTS = 10_000

    private final UIConfigProperties uiConfigProperties
    private final int maxMessageLength
    private final int maxRequestsPerMinute
    private final Cache<String, RequestCounter> requestCounters
    private final RequestCounter globalRequestCounter

    ClientLoggingController(UIConfigProperties uiConfigProperties) {
        this.uiConfigProperties = uiConfigProperties
        maxMessageLength = getIntConfig('loggingMaxMessageLength', DEFAULT_MAX_MESSAGE_LENGTH)
        maxRequestsPerMinute = getIntConfig('loggingMaxRequestsPerMinute', DEFAULT_MAX_REQUESTS_PER_MINUTE)
        int maxTrackedClients = getIntConfig('loggingMaxTrackedClients', DEFAULT_MAX_TRACKED_CLIENTS)
        requestCounters = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(2))
                .maximumSize(maxTrackedClients)
                .build()
        globalRequestCounter = new RequestCounter(maxRequestsPerMinute * 100)
    }

    // Predefined logging levels.
//    Logger.TRACE = defineLogLevel(1, 'TRACE');
//    Logger.DEBUG = defineLogLevel(2, 'DEBUG');
//    Logger.INFO = defineLogLevel(3, 'INFO');
//    Logger.TIME = defineLogLevel(4, 'TIME');
//    Logger.WARN = defineLogLevel(5, 'WARN');
//    Logger.ERROR = defineLogLevel(8, 'ERROR');
//    Logger.OFF = defineLogLevel(99, 'OFF');

    @DBUpgradeSafe
    @CrossOrigin(originPatterns = ['*'])
    @RequestMapping(value = "/log", method = [RequestMethod.PUT, RequestMethod.POST])
    @ResponseBody
    boolean writeLog(@RequestBody LogMessage logMessage, HttpServletRequest request) {
        if (!Boolean.parseBoolean(uiConfigProperties.client.loggingEnabled)) {
            return true
        }
        validate(logMessage)
        enforceRateLimit(request.remoteAddr)
        String sanitizedMessage = sanitize(logMessage.message)

        switch (logMessage.level.value) {
            case 1:
                log.trace('Client log: [{}]', sanitizedMessage)
                break
            case 2:
                log.debug('Client log: [{}]', sanitizedMessage)
                break
            case 3:
                log.info('Client log: [{}]', sanitizedMessage)
                break
            case 5:
                log.warn('Client log: [{}]', sanitizedMessage)
                break
            case 8:
                log.error('Client log: [{}]', sanitizedMessage)
                break
        }
        return true
    }

    private void validate(LogMessage logMessage) {
        if (!logMessage?.message?.trim()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 'Log message is required')
        }
        if (logMessage.message.length() > maxMessageLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Log message must not exceed ${maxMessageLength} characters")
        }
        if (logMessage.level?.value !in [1, 2, 3, 5, 8]) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 'Unexpected log level')
        }
    }

    private void enforceRateLimit(String remoteAddress) {
        if (!globalRequestCounter.tryAcquire() ||
                !requestCounters.asMap().computeIfAbsent(remoteAddress ?: 'unknown') {
                    new RequestCounter(maxRequestsPerMinute)
                }.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 'Client log rate limit exceeded')
        }
    }

    private int getIntConfig(String key, int defaultValue) {
        String configuredValue = uiConfigProperties.client[key]
        return configuredValue ? Integer.parseInt(configuredValue) : defaultValue
    }

    private static String sanitize(String message) {
        return message.replaceAll(/[\r\n\p{Cc}]/, ' ')
    }

    private static class RequestCounter {
        private final int limit
        private long windowStarted = System.currentTimeMillis()
        private int count

        RequestCounter(int limit) {
            this.limit = limit
        }

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis()
            if (now - windowStarted >= TimeUnit.MINUTES.toMillis(1)) {
                windowStarted = now
                count = 0
            }
            if (count >= limit) {
                return false
            }
            count++
            return true
        }
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
