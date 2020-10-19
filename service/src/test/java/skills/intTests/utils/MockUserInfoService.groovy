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
package skills.intTests.utils;


import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import skills.auth.SecurityMode

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.*
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

@Slf4j
@Conditional(SecurityMode.PkiAuth)
@Component
public class MockUserInfoService {

    private static final int port = 8181;

    WireMockServer mockServer = new WireMockServer(wireMockConfig()
            .httpsPort(port)
            .keystorePath("classpath:/test.skilltree.userinfoservice.p12")
            .keystorePassword("skillspass")
            .keyManagerPassword("skillspass")
            .keystoreType("PKCS12")
            .trustStorePath("classpath:/truststore.jks")
            .trustStorePassword("skillspass")
            .trustStoreType("JKS")
            .httpDisabled(true)
            .needClientAuth(true)
            .extensions(new ResponseTemplateTransformer(false))
    );

    @PostConstruct
    public void start() {
        log.info("starting mock user-info-service on port ${port}")

        mockServer.stubFor(any(urlPathEqualTo("/actuator/health")).willReturn(okJson("")));
        mockServer.stubFor(
                any(urlPathEqualTo("/userInfo"))
                        .willReturn(
                                ok()
                                .withHeader(CONTENT_TYPE, "application/json")
                                .withBody("""{
    "firstName" : "Fake",
    "lastName": "Fake",
    "nickname": "Fake",
    "email": "fake@fakeplace",
    "username": "{{request.query.dn}}",
    "usernameForDisplay": "{{request.query.dn}}",
    "userDn": "{{request.query.dn}}",
}""")
                                .withTransformers("response-template")
                        )
        );

        mockServer.start();
    }

    @PreDestroy
    public void stop() {
        mockServer.stop();
    }

}
