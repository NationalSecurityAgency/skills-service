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
package skills.intTests.csrf

import groovy.json.JsonOutput
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.client5.http.ssl.TrustAllStrategy
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.ssl.SSLContexts
import java.security.KeyStore
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SystemSSLConfiguration
import spock.lang.Unroll
import spock.lang.Requires

@SpringBootTest(properties = [
        'skills.config.disableCsrfProtectionInPkiMode=true',
        'skills.authorization.userInfoUri=https://localhost:8186/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8186/userQuery?query={query}',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8186/status'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class DisableCsrfProtectionInPkiModeIT extends DefaultIntSpec {

    def setup() {
        RestTemplate client = probeClient()
        HttpHeaders headers = requestHeaders(client, MediaType.APPLICATION_JSON)
        headers.set('X-XSRF-TOKEN', token(headers))
        [
                ['/app/projects/TestProject1', SkillsFactory.createProject()],
                ['/admin/projects/TestProject1/subjects/TestSubject1', SkillsFactory.createSubject()],
                ['/admin/projects/TestProject1/subjects/TestSubject1/skills/skill1', SkillsFactory.createSkill(1, 1, 1, 0, 1, 0, 10)]
        ].each { path, body ->
            assert client.exchange("${skillsService.wsHelper.skillsService}${path}", HttpMethod.POST,
                    new HttpEntity<>(JsonOutput.toJson(body), headers), String).statusCode == HttpStatus.OK
        }
    }

    @Requires({ SystemSSLConfiguration.isPki() })
    @Unroll
    def '#method non-multipart admin request bypasses CSRF in PKI mode'() {
        given:
        RestTemplate client = probeClient()
        HttpHeaders headers = requestHeaders(client, MediaType.APPLICATION_JSON)
        String url = "${skillsService.wsHelper.skillsService}${path}"

        when:
        ResponseEntity<String> response = client.exchange(url, method, new HttpEntity<>(body, headers), String)

        then:
        response.statusCode == HttpStatus.OK
        skillsService.getSubject([projectId: 'TestProject1', subjectId: subjectId]).name == subjectName

        where:
        method          | path                                                 | body                                                     | subjectId      | subjectName
        HttpMethod.POST | '/admin/projects/TestProject1/subjects/Subject2'     | '{"subjectId":"Subject2","name":"New Subject"}'        | 'Subject2'     | 'New Subject'
        HttpMethod.PUT  | '/admin/projects/TestProject1/subjects/TestSubject1' | '{"subjectId":"TestSubject1","name":"Updated Subject"}' | 'TestSubject1' | 'Updated Subject'
    }

    @Requires({ !SystemSSLConfiguration.isPki() })
    @Unroll
    def '#method non-multipart admin request still requires CSRF in FORM mode'() {
        given:
        RestTemplate client = probeClient()
        HttpHeaders headers = requestHeaders(client, MediaType.APPLICATION_JSON)
        String url = "${skillsService.wsHelper.skillsService}${path}"

        when:
        client.exchange(url, method, new HttpEntity<>(body, headers), String)

        then:
        HttpClientErrorException missingToken = thrown()
        missingToken.statusCode == HttpStatus.FORBIDDEN

        when:
        headers.set('X-XSRF-TOKEN', token(headers))
        ResponseEntity<String> protectedResponse = client.exchange(url, method, new HttpEntity<>(body, headers), String)

        then:
        protectedResponse.statusCode == HttpStatus.OK
        skillsService.getSubject([projectId: 'TestProject1', subjectId: subjectId]).name == subjectName

        where:
        method          | path                                                 | body                                                     | subjectId      | subjectName
        HttpMethod.POST | '/admin/projects/TestProject1/subjects/Subject2'     | '{"subjectId":"Subject2","name":"New Subject"}'        | 'Subject2'     | 'New Subject'
        HttpMethod.PUT  | '/admin/projects/TestProject1/subjects/TestSubject1' | '{"subjectId":"TestSubject1","name":"Updated Subject"}' | 'TestSubject1' | 'Updated Subject'
    }

    @Unroll
    def '#method multipart #path still requires CSRF with the PKI bypass enabled'() {
        given:
        RestTemplate client = probeClient()
        HttpHeaders headers = requestHeaders(client, MediaType.MULTIPART_FORM_DATA)
        def body = new LinkedMultiValueMap<String, Object>()
        body.add('file', new ClassPathResource('/testSlides/test-slides-1.pdf'))
        String url = "${skillsService.wsHelper.skillsService}${path}"

        when:
        client.exchange(url, method, new HttpEntity<>(body, headers), String)

        then:
        HttpClientErrorException missingToken = thrown()
        missingToken.statusCode == HttpStatus.FORBIDDEN

        when:
        headers.set('X-XSRF-TOKEN', token(headers))
        ResponseEntity<String> response = client.exchange(url, method, new HttpEntity<>(body, headers), String)

        then:
        response.statusCode == HttpStatus.OK

        where:
        [method, path] << [[HttpMethod.POST, HttpMethod.PUT],
                          ['/admin/projects/TestProject1/upload', '/api/projects/TestProject1/skills/skill1/upload']].combinations()
    }

    private RestTemplate probeClient() {
        if (!isPkiMode) {
            return new RestTemplate(new HttpComponentsClientHttpRequestFactory(HttpClients.custom().disableCookieManagement().build()))
        }
        KeyStore keyStore = KeyStore.getInstance('PKCS12')
        certificateRegistry.getCertificate(skillsService.userName).inputStream.withCloseable {
            keyStore.load(it, 'skillspass'.toCharArray())
        }
        KeyStore trustStore = KeyStore.getInstance('JKS')
        new ClassPathResource('/certs/truststore.jks').inputStream.withCloseable {
            trustStore.load(it, 'skillspass'.toCharArray())
        }
        def sslContext = SSLContexts.custom().loadTrustMaterial(trustStore, TrustAllStrategy.INSTANCE)
                .loadKeyMaterial(keyStore, 'skillspass'.toCharArray()).build()
        def sslSocketFactory = new SSLConnectionSocketFactory(sslContext, ['TLSv1.2'] as String[],
                null, NoopHostnameVerifier.INSTANCE)
        def connectionManager = new PoolingHttpClientConnectionManager(RegistryBuilder.create()
                .register('http', PlainConnectionSocketFactory.getSocketFactory())
                .register('https', sslSocketFactory).build())
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(HttpClients.custom()
                .setConnectionManager(connectionManager).disableCookieManagement().build()))
    }

    private HttpHeaders requestHeaders(RestTemplate client, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders()
        if (!isPkiMode) {
            String session = skillsService.wsHelper.restTemplateWrapper.authResponse.headers.get(HttpHeaders.SET_COOKIE)
                    .find { it.startsWith('JSESSIONID=') }.split(';')[0]
            headers.set(HttpHeaders.COOKIE, session)
        }
        ResponseEntity<String> response = client.exchange("${skillsService.wsHelper.skillsService}/app/userInfo",
                HttpMethod.GET, new HttpEntity<>(headers), String)
        assert response.statusCode == HttpStatus.OK
        String csrfCookie = response.headers.get(HttpHeaders.SET_COOKIE).find { it.startsWith('XSRF-TOKEN=') }.split(';')[0]
        headers.set(HttpHeaders.COOKIE, [headers.getFirst(HttpHeaders.COOKIE), csrfCookie].findAll { it }.join('; '))
        headers.contentType = contentType
        headers.accept = [MediaType.APPLICATION_JSON]
        headers
    }

    private String token(HttpHeaders headers) {
        headers.getFirst(HttpHeaders.COOKIE).split('; ').find { it.startsWith('XSRF-TOKEN=') }.substring('XSRF-TOKEN='.length())
    }
}
