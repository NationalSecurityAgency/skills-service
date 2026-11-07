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
package skills.intTests.oauth2

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpServer
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import skills.SpringBootApp
import skills.intTests.utils.DataResetHelper
import skills.storage.repos.UserAttrsRepo
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp,
        properties = ['skills.authorization.authMode=FORM'])
class CognitoLoginSpec extends Specification {
    static final def signingKey = new RSAKeyGenerator(2048).keyID('test-key').generate()
    static final def wrongKey = new RSAKeyGenerator(2048).keyID('test-key').generate()
    static final HttpServer provider = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
    static final String issuer = "http://127.0.0.1:${provider.address.port}"
    static String nonce
    static String scenario

    static {
        provider.createContext('/.well-known/openid-configuration') { exchange ->
            respond(exchange, [issuer: issuer, authorization_endpoint: issuer + '/authorize',
                    token_endpoint: issuer + '/token', jwks_uri: issuer + '/keys',
                    response_types_supported: ['code'], subject_types_supported: ['public'],
                    id_token_signing_alg_values_supported: ['RS256']])
        }
        provider.createContext('/keys') { exchange ->
            respond(exchange, new JWKSet(signingKey.toPublicJWK()).toJSONObject())
        }
        provider.createContext('/token') { exchange ->
            exchange.requestBody.close()
            Instant now = Instant.now()
            def claims = new JWTClaimsSet.Builder().issuer(scenario == 'wrong-issuer' ? issuer + '/other' : issuer).subject('cognito-subject')
                    .audience('skilltree-test').issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(300)))
                    .claim('nonce', scenario == 'wrong-nonce' ? 'different-nonce' : nonce)
                    .claim('given_name', 'Joe').claim('family_name', 'User')
            if (scenario != 'missing-email') {
                claims.claim('email', 'joe@example.com')
            }
            def jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID('test-key').build(), claims.build())
            jwt.sign(new RSASSASigner(scenario == 'wrong-signature' ? wrongKey : signingKey))
            respond(exchange, [access_token: 'test-access-token', token_type: 'Bearer', expires_in: 300, id_token: jwt.serialize()])
        }
        provider.start()
    }

    static void respond(exchange, Map body) {
        byte[] bytes = JsonOutput.toJson(body).getBytes('UTF-8')
        exchange.responseHeaders.set('Content-Type', 'application/json')
        exchange.sendResponseHeaders(200, bytes.length)
        exchange.responseBody.withCloseable { it.write(bytes) }
    }

    @DynamicPropertySource
    static void configureProvider(DynamicPropertyRegistry registry) {
        String registration = 'spring.security.oauth2.client.registration.cognito.'
        String config = 'spring.security.oauth2.client.provider.cognito.'
        registry.add(registration + 'client-id', { 'skilltree-test' })
        registry.add(registration + 'client-secret', { 'test-secret' })
        registry.add(registration + 'authorization-grant-type', { 'authorization_code' })
        registry.add(registration + 'scope', { 'openid,profile,email' })
        registry.add(config + 'issuer-uri', { issuer })
        registry.add(config + 'authorization-uri', { issuer + '/authorize' })
        registry.add(config + 'token-uri', { issuer + '/token' })
        registry.add(config + 'jwk-set-uri', { issuer + '/keys' })
        registry.add(config + 'user-name-attribute', { 'sub' })
    }

    @LocalServerPort
    int port

    @Autowired
    DataResetHelper dataResetHelper

    @Autowired
    UserAttrsRepo userAttrsRepo

    def setup() {
        dataResetHelper.resetData()
    }

    def cleanupSpec() {
        provider.stop(0)
    }

    def "Cognito code exchange creates an authenticated SkillTree session"() {
        when:
        def client = login('valid')
        def response = get(client, '/app/userInfo')
        def user = new JsonSlurper().parseText(response.body())

        then:
        response.statusCode() == 200
        user.userId == 'cognito-subject-cognito'
        user.first == 'Joe'
        user.last == 'User'
        userAttrsRepo.findByUserIdIgnoreCase('cognito-subject-cognito').email == 'joe@example.com'
    }

    def "Cognito login refuses #failure"() {
        when:
        def client = login(failure)
        def response = get(client, '/app/userInfo')

        then:
        response.statusCode() != 200 || response.body() == 'null'
        !response.body().contains('cognito-subject-cognito')
        userAttrsRepo.findByUserIdIgnoreCase('cognito-subject-cognito') == null

        where:
        failure << ['wrong-signature', 'wrong-nonce', 'wrong-issuer', 'missing-email']
    }

    HttpClient login(String mode) {
        scenario = mode
        def client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER).build()
        def authorization = get(client, '/oauth2/authorization/cognito')
        assert authorization.statusCode() == 302
        def parameters = URI.create(authorization.headers().firstValue('Location').get()).rawQuery.split('&').collectEntries {
            def pair = it.split('=', 2)
            [(URLDecoder.decode(pair[0], 'UTF-8')): URLDecoder.decode(pair[1], 'UTF-8')]
        }
        nonce = parameters.nonce
        assert nonce
        get(client, '/login/oauth2/code/cognito?code=test-code&state=' + URLEncoder.encode(parameters.state, 'UTF-8'))
        return client
    }

    HttpResponse<String> get(HttpClient client, String path) {
        client.send(HttpRequest.newBuilder(URI.create("http://localhost:${port}${path}")).GET().build(), HttpResponse.BodyHandlers.ofString())
    }
}
