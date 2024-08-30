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
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import skills.auth.SecurityMode
import skills.storage.repos.UserAttrsRepo

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.*
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

@Slf4j
@Conditional(SecurityMode.PkiAuth)
@Component
public class MockUserInfoService {

    @Value('${skills.authorization.userInfoHealthCheckUri}')
    String userInfoHealthCheckUri

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    CertificateRegistry certificateRegistry

    static final Map<String, FirstnameLastname> DN_TO_NAME = [
            "cn=jdoe@email.foo, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("John", "Doe"),
            "cn=jadoe@email.foo, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("Jane", "Doe"),
            "cn=fbar@email.foo, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("Foo", "Bar"),
            "cn=aaa@email.foo, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("Aaa", "Aaa"),
            "cn=bbb@email.foo, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("Bbb", "Bbb"),
            "cn=ccc@email.foo, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("Ccc", "Ccc"),
            "cn=ddd@email.foo, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("Ddd", "Ddd"),
            "cn=eee@email.foo, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("Eee", "Eee"),
            "cn=fff@email.foo, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("Fff", "Fff"),
            "cn=ggg@email.foo, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("Ggg", "Ggg"),
            "cn=hhh@email.foo, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("Hhh", "Hhh"),
            "cn=userrolespecsuser1, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("John", "Smith"),
            "cn=userrolespecsuser2, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("Bob", "Cool"),
            "cn=userrolespecsuser3, ou=integration tests, o=skilltree test, c=us": new FirstnameLastname("John", "Smith"),
    ]

    WireMockServer mockServer

    @PostConstruct
    void start() {
        def matcher = userInfoHealthCheckUri =~ /https:\/\/localhost:(\d\d\d\d)\/status/
        int port = Integer.parseInt(matcher[0][1]);

        log.info("starting mock user-info-service on port ${port}")

        mockServer = new WireMockServer(wireMockConfig()
                .httpsPort(port)
                .keystorePath("classpath:certs/test.skilltree.userinfoservice.p12")
                .keystorePassword("skillspass")
                .keyManagerPassword("skillspass")
                .keystoreType("PKCS12")
                .trustStorePath("classpath:certs/truststore.jks")
                .trustStorePassword("skillspass")
                .trustStoreType("JKS")
                .httpDisabled(true)
                .needClientAuth(true)
                .extensions(new UserInfoResponseTransformer(userAttrsRepo)));

        mockServer.stubFor(any(urlPathEqualTo("/status")).willReturn(
                ok()
                .withHeader(CONTENT_TYPE, "application/json")
                .withBody("""{ "status": "OK" }""")
        ));
        mockServer.stubFor(any(urlPathEqualTo("/userQuery")).willReturn(
                ok()
                .withHeader(CONTENT_TYPE, "application/json")
                .withBody("""[{"username": "skills@skills.org", "usernameForDisplay":"skills@skills.org"}]""")
        ));
        mockServer.stubFor(
                any(urlPathEqualTo("/userInfo"))
                        .willReturn(
                                ok()
                                .withTransformers("user-info-transformer")
                        )
        );

        mockServer.start();
    }

    @PreDestroy
    public void stop() {
        mockServer.stop();
    }

    public static class UserInfoResponseTransformer extends ResponseDefinitionTransformer {

        UserAttrsRepo userAttrsRepo

        public UserInfoResponseTransformer(UserAttrsRepo userAttrsRepo) {
            this.userAttrsRepo = userAttrsRepo
        }

        @Override
        ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
            String dnQuery = request.queryParameter('dn')?.firstValue()

            if (dnQuery?.containsIgnoreCase('doesNotExist')) {
                throw new Exception("Unknown User [${dnQuery}]")
            }

            if (!dnQuery || dnQuery == "null") {
                return new ResponseDefinitionBuilder()
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody("{}")
                        .build()
            }

            String fname = "Fake"
            String lname = "Fake"

            String usernamified = DnUsernameHelper.getUsername(dnQuery)
            String usernamifiedForDisplay = "${usernamified} for display"
//            String usernamified = dnQuery.replaceAll(" ", "_").replaceAll(",","").replaceAll("=","-")

            log.info("looking up firstname/lastname for ${dnQuery}")
            FirstnameLastname configuredNames = DN_TO_NAME.get(dnQuery.toLowerCase())
            if (configuredNames) {
                log.info("!!!! found FirstnameLastname for $dnQuery")
                fname = configuredNames.firstname
                lname = configuredNames.lastname
            }

            String email = EmailUtils.generateEmaillAddressFor(usernamified)

            def existingEmail = userAttrsRepo.findEmailByUserId(usernamified.toLowerCase())
            if (existingEmail) {
                email = existingEmail
            }

            return new ResponseDefinitionBuilder()
                    .withHeader(CONTENT_TYPE, "application/json")
                    .withBody("""
                    {
                        "firstName" : "${fname}",
                        "lastName": "${lname}",
                        "nickname": "Fake",
                        "email": "${email.toLowerCase()}",
                        "username": "${usernamified}",
                        "usernameForDisplay": "${usernamifiedForDisplay}",
                        "userDn": "${dnQuery}"
                    }
                    """).build()
        }

        @Override
        String getName() {
            return "user-info-transformer"
        }

        @Override
        boolean applyGlobally() {
            return false;
        }
    }

    FirstnameLastname getFirstNameLastnameForUserId(String userId) {
        return DN_TO_NAME.get(certificateRegistry.loadDnFromUserId(userId)?.toLowerCase())
    }

    String getUserIdWithCase(String userId) {
        return certificateRegistry.loadCNFromCert(certificateRegistry.getCertificate(userId))
    }

    static class FirstnameLastname {
        String firstname=""
        String lastname=""
        public FirstnameLastname(String firstname, String lastname){
            this.firstname = firstname
            this.lastname = lastname
        }
    }

}
