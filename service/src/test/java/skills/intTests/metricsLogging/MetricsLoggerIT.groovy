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
package skills.intTests.metricsLogging

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.controller.request.model.PageVisitRequest
import skills.intTests.utils.DefaultIntSpec
import spock.lang.IgnoreIf

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.google.common.net.HttpHeaders.CONTENT_TYPE

@Slf4j
@SpringBootTest(properties = [
        'skills.external.metrics.enabled=true',
        'skills.config.ui.enablePageVisitReporting=true',
        'skills.external.metrics.minNumOfThreads=1',
        'skills.external.metrics.maxNumOfThreads=2',
        'skills.external.metrics.queueCapacity=2',
        'skills.external.metrics.endpoint.url=http://localhost:8082/metrics'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class MetricsLoggerIT extends DefaultIntSpec {

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


    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "drop request once the queue is full"() {
        mockServer.stubFor(put("/metrics")
                .withHeader(CONTENT_TYPE, containing("json"))
                .willReturn(ok()
                        .withFixedDelay(1500)
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody("{}")))


        PageVisitRequest pageVisitRequest = new PageVisitRequest(
                path: "/some/path",
                fullPath: "MyCoolName",
                hostname: "myhost.com",
                protocol: "http",
                port: 8080,
                skillDisplay: false,
                projectId: "proj1"
        )
        when:
        int numToSend = 10
        for (int i = 0; i < numToSend; i++) {
            log.info("Sending request [{}]", i)
            skillsService.reportPageVisit(pageVisitRequest)
        }

        Thread.sleep(6000)
        // pool accepts 2 threads + queue is 2, the rest of the request are rejected
        mockServer.verify(exactly(4), putRequestedFor(urlEqualTo("/metrics")));
        then:
        true
    }

}
