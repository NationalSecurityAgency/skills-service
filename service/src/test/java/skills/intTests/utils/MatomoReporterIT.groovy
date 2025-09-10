/**
 * Copyright 2025 SkillTree
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
package skills.intTests.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.controller.request.model.PageVisitRequest
import spock.lang.IgnoreIf

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.google.common.net.HttpHeaders.CONTENT_TYPE

@Slf4j
@SpringBootTest(properties = [
        'skills.matomo.enableSkillApiUsage=true',
        'skills.config.ui.matomoUrl=http://localhost:8082/matomo',
        'skills.config.ui.matomoSiteId=1',
        'skills.matomo.minNumOfThreads=1',
        'skills.matomo.maxNumOfThreads=2',
        'skills.matomo.queueCapacity=2',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8201/status',
        'skills.authorization.userInfoUri=https://localhost:8201/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8201/userQuery?query={query}',
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class MatomoReporterIT extends DefaultIntSpec {

    WireMockServer mockServer

    def setup() {
        mockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .port(8082)
        )
        mockServer.start();
    }

    def cleanup() {
        mockServer.stop()
    }

    def "report add skill events to matomo"() {
        def proj = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        List<Map> skills = SkillsFactory.createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subject, skills)

        List<String> users = getRandomUsers(1)

        // Stub for Matomo endpoint
        mockServer.stubFor(post(urlPathMatching("/matomo"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .willReturn(ok()))

        when:
        skillsService.addSkill(skills.get(0), users[0])

        Thread.sleep(6000)
        
        // Verify the request was made
        mockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/matomo")));
        
        // Get all requests to the matomo endpoint
        def requests = mockServer.findAll(postRequestedFor(urlEqualTo("/matomo")))
        def request = requests[0]
        
        // Verify the request content type
        assert request.getHeader("Content-Type").startsWith("application/x-www-form-urlencoded")
        
        // Parse the form data
        def formData = request.bodyAsString.split('&').collectEntries { 
            def parts = it.split('=', 2)
            [(URLDecoder.decode(parts[0], 'UTF-8')): parts.length > 1 ? URLDecoder.decode(parts[1], 'UTF-8') : '']
        }

        boolean isPki = System.getenv("SPRING_PROFILES_ACTIVE") == 'pki'
        String userId = isPki ? userAttrsRepo.findByUserIdIgnoreCase(users[0]).dn : users[0]

        then:
        formData.idsite == "1"  // From test properties
        formData.rec == "1"      // Default value from MatomoReporter
        formData.uid == userId
        formData.url ==~ ".*${proj.projectId}.*${skills.get(0).skillId}"
        formData.action_name == "Report Skill / ${proj.projectId} / ${skills.get(0).skillId}"
        !formData.urlref
        formData.ua?.contains("Apache-HttpClient")
        !formData.lang
    }

    def "drop request once the queue is full"() {
        def proj = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        List<Map> skills = SkillsFactory.createSkills(2, 1, 1, 10, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subject, skills)

        // Stub for Matomo endpoint with logging
        mockServer.stubFor(post("/matomo")
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .willReturn(ok()
                    .withFixedDelay(1500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{}")
                ))

        List<String> users = getRandomUsers(1)

        when:
        int numToSend = 10
        for (int i = 0; i < numToSend; i++) {
            skillsService.addSkill(skills.get(0), users[0])
        }

        Thread.sleep(6000)
        // pool accepts 2 threads + queue is 2, the rest of the request are rejected
        mockServer.verify(exactly(4), postRequestedFor(urlEqualTo("/matomo")));
        then:
        true
    }

}


