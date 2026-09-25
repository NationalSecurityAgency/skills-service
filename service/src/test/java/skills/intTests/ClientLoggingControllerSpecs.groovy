/**
 * Copyright 2026 SkillTree
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
package skills.intTests

import org.springframework.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import skills.controller.ClientLoggingController
import skills.UIConfigProperties
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.RestTemplateWrapper
import skills.utils.LoggerHelper

class ClientLoggingControllerSpecs extends DefaultIntSpec {

    @Autowired
    ClientLoggingController clientLoggingController

    @Autowired
    UIConfigProperties uiConfigProperties

    RestTemplateWrapper anonymousClient
    String logUrl
    String originalLoggingEnabled

    def setup() {
        // Reuse the test facade's transport for its PKI client certificate, but do not
        // reuse its authenticated cookies or authorization headers for this public endpoint.
        anonymousClient = new RestTemplateWrapper(skillsService.wsHelper.oAuthRestTemplate, isPkiMode)
        logUrl = "${skillsService.wsHelper.skillsService}/public/log"
        originalLoggingEnabled = uiConfigProperties.client.loggingEnabled
        uiConfigProperties.client.loggingEnabled = 'true'
    }

    def cleanup() {
        uiConfigProperties.client.loggingEnabled = originalLoggingEnabled
    }

    def 'anonymous clients can submit logs and control characters are sanitized'() {
        LoggerHelper loggerHelper = new LoggerHelper(ClientLoggingController)

        when:
        def response = postLog([message: 'first line\r\nforged line\u0000', level: [value: 5, name: 'WARN']])

        then:
        response.statusCode == HttpStatus.OK
        loggerHelper.logEvents.find {
            it.formattedMessage == 'Client log: [first line  forged line ]'
        }
        !loggerHelper.logEvents.find { it.formattedMessage.contains('\n') || it.formattedMessage.contains('\r') }

        cleanup:
        loggerHelper.stop()
    }

    def 'invalid client log payloads are rejected'() {
        when:
        def response = postLog(payload)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST

        where:
        payload << [
                [message: null, level: [value: 3, name: 'INFO']],
                [message: '   ', level: [value: 3, name: 'INFO']],
                [message: 'x' * (ClientLoggingController.DEFAULT_MAX_MESSAGE_LENGTH + 1), level: [value: 3, name: 'INFO']],
                [message: 'message', level: null],
                [message: 'message', level: [value: 99, name: 'OFF']],
        ]
    }

    def 'client log requests are ignored when client logging is disabled'() {
        LoggerHelper loggerHelper = new LoggerHelper(ClientLoggingController)
        uiConfigProperties.client.loggingEnabled = 'false'

        when:
        def response = postLog([message: null, level: null])

        then:
        response.statusCode == HttpStatus.OK
        !loggerHelper.logEvents

        cleanup:
        loggerHelper.stop()
    }

    def 'anonymous client logging is rate limited'() {
        given:
        clientLoggingController.requestCounters.invalidateAll()

        when:
        int maxRequestsPerMinute = Integer.parseInt(uiConfigProperties.client.loggingMaxRequestsPerMinute)
        def responses = (1..(maxRequestsPerMinute + 1)).collect {
            postLog([message: "message ${it}".toString(), level: [value: 3, name: 'INFO']])
        }

        then:
        responses.take(maxRequestsPerMinute).every { it.statusCode == HttpStatus.OK }
        responses.last().statusCode == HttpStatus.TOO_MANY_REQUESTS
    }

    private def postLog(Map payload) {
        anonymousClient.postForEntity(logUrl, payload, String)
    }
}
