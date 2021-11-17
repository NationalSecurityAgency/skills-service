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
package skills.intTests

import ch.qos.logback.classic.Level
import skills.controller.exceptions.RestExceptionHandler
import skills.intTests.utils.DefaultIntSpec
import skills.utils.LoggerHelper
import skills.utils.WaitFor

class ClientAbortExceptionSpecs extends DefaultIntSpec {

    def "client disconnects causing clientAbortException"() {
        setup:
        LoggerHelper loggerHelper = new LoggerHelper(RestExceptionHandler.class)

        when:
        int responseCode = disconnectEarly('/public/longRunningEndpoint')
        WaitFor.wait { loggerHelper.hasLogMsgStartsWith("GET uri=/public/longRunningEndpoint,") }

        then:
        responseCode == 200
        loggerHelper.logEvents.find { it.level == Level.WARN && it.message == "GET uri=/public/longRunningEndpoint, ClientAbortException - client prematurely closed the connection" }
    }

    private int disconnectEarly(String path) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) URI.create("http://localhost:" + localPort + path).toURL().openConnection()
        // Prematurely close the connection.
        urlConnection.getInputStream().close()
        return urlConnection.getResponseCode()
    }
}