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
package skills.services


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import skills.SpringBootApp
import skills.profile.CallStackProfAspect
import skills.utils.LoggerHelper
import spock.lang.IgnoreIf
import spock.lang.Specification

@SpringBootTest(properties = ['skills.prof.endpoints.slowMethod2=2000', 'skills.h2.port=9093'],
        webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class EnableCallStackProfIT extends Specification {

    @Autowired
    TestController testController

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "Test custom endpoint method minMillisToPrint"() {

        LoggerHelper loggerHelper = new LoggerHelper(CallStackProfAspect.class)

        when:
        testController.slowMethod1()
        testController.slowMethod2()

        then:
        loggerHelper.logEvents?.find {it.formattedMessage.contains('Profiling Endpoint') && it.formattedMessage.contains('slowMethod1') }
        !loggerHelper.logEvents?.find {it.formattedMessage.contains('Profiling Endpoint') && it.formattedMessage.contains('slowMethod2') }
    }

    @skills.profile.EnableCallStackProf
    @Component
    static class TestController {

        void slowMethod1() {
            Thread.sleep(1000)
        }

        void slowMethod2() {
            Thread.sleep(1000)
        }
    }

}

