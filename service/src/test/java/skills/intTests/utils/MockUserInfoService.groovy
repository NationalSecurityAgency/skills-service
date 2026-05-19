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
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import skills.auth.SecurityMode
import skills.auth.UserInfo
import skills.storage.model.UserAttrs
import skills.storage.repos.UserAttrsRepo

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.*
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

@Slf4j
@Conditional(SecurityMode.PkiAuth)
@Component
class MockUserInfoService {

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
                .extensions(new UserInfoResponseTransformer(userAttrsRepo), new UserSuggestionResponseTransformer(userAttrsRepo, certificateRegistry)));

        mockServer.stubFor(any(urlPathEqualTo("/status")).willReturn(
                ok()
                .withHeader(CONTENT_TYPE, "application/json")
                .withBody("""{ "status": "OK" }""")
        ));
        mockServer.stubFor(any(urlPathEqualTo("/userQuery")).willReturn(
                ok()
                        .withTransformers("user-suggestion-transformer")
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
    void stop() {
        mockServer.stop();
    }

    static class UserInfoResponseTransformer extends ResponseDefinitionTransformer {

        UserAttrsRepo userAttrsRepo

        UserInfoResponseTransformer(UserAttrsRepo userAttrsRepo) {
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

            String usernamified = DnUsernameHelper.getUsername(dnQuery).toLowerCase()
            UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(usernamified)
            String usernamifiedForDisplay = userAttrs?.userIdForDisplay ?: "${usernamified} for display"

            String fname = userAttrs?.firstName ?: "${usernamified.toUpperCase()}_first"
            String lname = userAttrs?.lastName ?: "${usernamified.toUpperCase()}_last"

            log.info("looking up firstname/lastname for ${dnQuery}")
            FirstnameLastname configuredNames = DN_TO_NAME.get(dnQuery.toLowerCase())
            if (configuredNames) {
                log.info("!!!! found FirstnameLastname for $dnQuery")
                fname = configuredNames.firstname
                lname = configuredNames.lastname
            }

            String email = userAttrs?.email ?: EmailUtils.generateEmaillAddressFor(usernamified)

            return new ResponseDefinitionBuilder()
                    .withHeader(CONTENT_TYPE, "application/json")
                    .withBody("""
                    {
                        "firstName" : "${fname}",
                        "lastName": "${lname}",
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

    static class FirstnameLastname {
        String firstname=""
        String lastname=""
        FirstnameLastname(String firstname, String lastname){
            this.firstname = firstname
            this.lastname = lastname
        }
    }


    static class MockedUserInfoRes {
        String firstName
        String lastName
        String email
        String username
        String usernameForDisplay
        String userDn
    }

    static class UserSuggestionResponseTransformer extends ResponseDefinitionTransformer {

        UserAttrsRepo userAttrsRepo
        CertificateRegistry suggestTransformerCertRegistry

        UserSuggestionResponseTransformer(UserAttrsRepo userAttrsRepo, CertificateRegistry suggestTransformerCertRegistry) {
            this.userAttrsRepo = userAttrsRepo
            this.suggestTransformerCertRegistry = suggestTransformerCertRegistry
        }

        @Override
        ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
            String query = request.queryParameter('query')?.firstValue()

            if (!query || query == "null") {
                return new ResponseDefinitionBuilder()
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody("{}")
                        .build()
            }

            List<String> allUserIds = suggestTransformerCertRegistry.allUserIds
            List<String> foundUserIds = allUserIds.findAll { it.toLowerCase().contains(query.toLowerCase())}
            log.debug("Called /userQuery endpoint with [{}] query and found {} userIds", query, foundUserIds)

            List<MockedUserInfoRes> mockedUserInfoRes = foundUserIds.collect {
                String userName = it
                String dn = suggestTransformerCertRegistry.loadDnFromUserId(userName)
                log.debug("Loaded dn [{}] for username [{}]", dn, userName)
                UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userName)
                String userNameForDisplay = userAttrs?.userIdForDisplay ?: "${userName} for display"
                String firstName = userAttrs?.firstName ?: "${userName.toUpperCase()}_first"
                String lastName = userAttrs?.lastName ?: "${userName.toUpperCase()}_last"
                String email = userAttrs?.email ?: EmailUtils.generateEmaillAddressFor(userName)

                return new MockedUserInfoRes(
                        firstName: firstName,
                        lastName: lastName,
                        email: email,
                        username: userName,
                        usernameForDisplay: userNameForDisplay,
                        userDn: dn
                )
            }
            String bodyRes = JsonOutput.prettyPrint(JsonOutput.toJson(mockedUserInfoRes))
            log.debug("Returning /userQuery with body of {}", bodyRes)
            return new ResponseDefinitionBuilder()
                    .withHeader(CONTENT_TYPE, "application/json")
                    .withBody(bodyRes).build()
        }

        @Override
        String getName() {
            return "user-suggestion-transformer"
        }

        @Override
        boolean applyGlobally() {
            return false;
        }
    }
}
