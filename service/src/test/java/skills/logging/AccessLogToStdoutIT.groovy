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
package skills.logging

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsServiceFactory
import spock.lang.IgnoreIf

@Slf4j
@SpringBootTest(properties = ['server.tomcat.accesslog.enabled=true'],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class AccessLogToStdoutIT extends DefaultIntSpec {

    @Autowired
    SkillsServiceFactory skillsServiceFactory


    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "logs are sent to stdout"() {
        def originalOut = System.out
        def outputBuffer = new ByteArrayOutputStream()
        System.setOut(new PrintStream(outputBuffer))
        when:
        skillsService.getAvailableMyProjects()

        then:
        String capturedOutput = outputBuffer.toString()
        capturedOutput =~ /access\.skills-service \[skills@skills.org\] .* "GET \/api\/availableForMyProjects HTTP\/1.1" 200 .*/

        cleanup: "restore original System.out"
        System.setOut(originalOut)
    }
}
