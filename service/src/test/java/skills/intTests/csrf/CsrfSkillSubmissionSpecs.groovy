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

import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.client5.http.ssl.TrustAllStrategy
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.ssl.SSLContexts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import skills.intTests.reportSkills.ReportSkillsTransactionController
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import spock.lang.IgnoreIf
import spock.lang.Unroll

import javax.net.ssl.SSLContext
import java.security.KeyStore

class CsrfSkillSubmissionSpecs extends DefaultIntSpec {

    @Autowired
    RequestMappingHandlerMapping requestMappingHandlerMapping

    private static final String SKILL_PATH = '/api/projects/{projectId}/skills/{skillId}'
    private static final String UPLOAD_PATH = '/api/projects/{projectId}/skills/{skillId}/upload'

    // Client endpoints are probed with and without the XSRF header.
    // The upload mapping needs multipart encoding and is exercised in AdminCsrfSpecs.
    private static final List<List> JSON_ENDPOINTS = [
            [HttpMethod.POST, '/api/projects/{projectId}/skillsClientVersion'],
            [HttpMethod.PUT, '/api/projects/{projectId}/skillsClientVersion'],
            [HttpMethod.POST, '/api/projects/{projectId}/crossProject/{crossProjectId}/skills/{skillId}'],
            [HttpMethod.PUT, '/api/projects/{projectId}/crossProject/{crossProjectId}/skills/{skillId}'],
            [HttpMethod.POST, '/api/pageVisit'],
            [HttpMethod.PUT, '/api/pageVisit'],
            [HttpMethod.POST, '/api/projects/{projectId}/skills/visited/{skillId}'],
            [HttpMethod.PUT, '/api/projects/{projectId}/skills/visited/{skillId}'],
            [HttpMethod.POST, '/api/myprojects/{projectId}'],
            [HttpMethod.POST, '/api/projects/{projectId}/contact'],
            [HttpMethod.POST, '/api/projects/{projectId}/newInviteRequest'],
            [HttpMethod.POST, '/api/validation/description'],
            [HttpMethod.POST, '/api/validation/addPrefixToInvalidParagraphs'],
            [HttpMethod.POST, '/api/validation/name'],
            [HttpMethod.POST, '/api/validation/url'],
            [HttpMethod.POST, '/api/webNotifications/{notificationId}/dismiss'],
            [HttpMethod.POST, '/api/webNotifications/dismissAll'],
            [HttpMethod.POST, '/api/quizzes/{quizId}/attempt'],
            [HttpMethod.PUT, '/api/quizzes/{quizId}/attempt'],
            [HttpMethod.POST, '/api/quizzes/{quizId}/attempt/{attemptId}/answers/{answerId}'],
            [HttpMethod.PUT, '/api/quizzes/{quizId}/attempt/{attemptId}/answers/{answerId}'],
            [HttpMethod.POST, '/api/quizzes/{quizId}/attempt/{quizAttempId}/complete'],
            [HttpMethod.PUT, '/api/quizzes/{quizId}/attempt/{quizAttempId}/complete'],
            [HttpMethod.POST, '/api/quizzes/{quizId}/attempt/{quizAttempId}/fail'],
            [HttpMethod.PUT, '/api/quizzes/{quizId}/attempt/{quizAttempId}/fail']
    ]

    private static final List<List> PUBLIC_ENDPOINTS = [
            [HttpMethod.POST, '/public/log'], [HttpMethod.PUT, '/public/log']
    ]

    private static final List<List> MULTIPART_ENDPOINTS = [
            [HttpMethod.POST, UPLOAD_PATH], [HttpMethod.PUT, UPLOAD_PATH]
    ]

    private static final List<List> SKILL_EVENT_ENDPOINTS = [
            [HttpMethod.POST, SKILL_PATH], [HttpMethod.PUT, SKILL_PATH]
    ]

    def setup() {
        if (!isPkiMode) {
            // Login rotates the CSRF token; refresh the fixture client's cookie.
            skillsService.wsHelper.restTemplateWrapper.getForEntity("${skillsService.wsHelper.skillsService}/app/userInfo", String)
        }
        skillsService.createProjectAndSubjectAndSkills(SkillsFactory.createProject(), SkillsFactory.createSubject(),
                [SkillsFactory.createSkill(1, 1, 1, 0, 10, 0, 10)])
    }

    @Unroll
    def '#method #path enforces CSRF in form mode and skips it in PKI mode'() {
        given:
        RestTemplate client = probeClient()
        String url = "${skillsService.wsHelper.skillsService}${resolvePath(path)}"
        HttpHeaders headers = new HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = [MediaType.APPLICATION_JSON]
        String token = isPkiMode ? null : csrfToken(client)
        if (!isPkiMode) {
            headers.set(HttpHeaders.COOKIE, "${sessionCookie()}; XSRF-TOKEN=${token}")
        }
        String payload = payloadFor(path)

        when:
        HttpStatusCode withoutHeader = responseOrError(client, url, method, payload, headers)

        then:
        // The PKI endpoint may reject fixture data after the request passes the CSRF filter.
        isPkiMode ? withoutHeader != HttpStatus.FORBIDDEN : withoutHeader == HttpStatus.FORBIDDEN

        when:
        if (!isPkiMode) {
            headers.set('X-XSRF-TOKEN', token)
        }
        HttpStatusCode withValidCredentials = isPkiMode ? withoutHeader : responseOrError(client, url, method,
                payload, headers)

        then:
        withValidCredentials != HttpStatus.FORBIDDEN

        where:
        [method, path] << JSON_ENDPOINTS + PUBLIC_ENDPOINTS
    }

    def 'every production POST and PUT mapping under /api and /public has a CSRF policy probe'() {
        given:
        Set<List> covered = (JSON_ENDPOINTS + PUBLIC_ENDPOINTS + MULTIPART_ENDPOINTS + SKILL_EVENT_ENDPOINTS) as Set
        // The transactional-report controller is a test fixture, not a production API mapping.
        Set<List> mappings = requestMappingHandlerMapping.handlerMethods.findAll { RequestMappingInfo info, handler ->
            handler.beanType != ReportSkillsTransactionController
        }.keySet().collectMany { RequestMappingInfo info ->
            info.pathPatternsCondition.patternValues.findAll {
                it.startsWith('/api/') || it.startsWith('/public/')
            }.collectMany { String path ->
                info.methodsCondition.methods.findAll { it.name() in ['POST', 'PUT'] }
                        .collect { requestMethod -> [HttpMethod.valueOf(requestMethod.name()), path] }
            }
        } as Set

        expect:
        mappings == covered
    }

    @IgnoreIf({ env['SPRING_PROFILES_ACTIVE'] == 'pki' })
    @Unroll
    def 'form skill report with #authorizationDescription and no session is forbidden'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization)
        }
        RestTemplate client = new RestTemplate()

        when:
        client.exchange(skillUrl(), HttpMethod.POST, new HttpEntity<>('{}', headers), String)

        then:
        HttpClientErrorException exception = thrown()
        exception.statusCode == HttpStatus.FORBIDDEN
        skillsService.getPerformedSkills(skillsService.userName, SkillsFactory.getDefaultProjId(), '', 'skillId').data.isEmpty()

        where:
        authorizationDescription | authorization
        'empty Bearer header'    | 'Bearer '
        'no Authorization header' | null
    }

    @Unroll
    @IgnoreIf({ env['SPRING_PROFILES_ACTIVE'] == 'pki' })
    def '#method skill report with a bearer token needs no XSRF header in form mode'() {
        given:
        String projectId = SkillsFactory.getDefaultProjId()
        skillsService.setProxyCredentials(projectId, skillsService.getClientSecret(projectId))
        String bearerToken = skillsService.wsHelper.getTokenForUser(skillsService.userName)
        HttpHeaders headers = new HttpHeaders()
        headers.setBearerAuth(bearerToken)
        headers.contentType = MediaType.APPLICATION_JSON

        when:
        def response = new RestTemplate().exchange(skillUrl(), method, new HttpEntity<>('{}', headers), Map)

        then:
        response.statusCode == HttpStatus.OK
        response.body.skillApplied == true
        response.body.skillId == SkillsFactory.getSkillId()
        skillsService.getPerformedSkills(skillsService.userName, projectId, '', 'skillId').data.size() == 1

        where:
        method << [HttpMethod.POST, HttpMethod.PUT]
    }

    def 'skill report through the authenticated fixture client succeeds in either mode'() {
        when:
        def response = skillsService.addSkill([projectId: SkillsFactory.getDefaultProjId(), skillId: SkillsFactory.getSkillId()])

        then:
        response.body.skillApplied == true
        skillsService.getPerformedSkills(skillsService.userName, SkillsFactory.getDefaultProjId(), '', 'skillId').data.size() == 1
    }

    @Unroll
    @IgnoreIf({ env['SPRING_PROFILES_ACTIVE'] != 'pki' })
    def '#method skill report with a PKI certificate needs no XSRF header'() {
        given:
        RestTemplate client = probeClient()
        HttpHeaders headers = new HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        when:
        def response = client.exchange(skillUrl(), method, new HttpEntity<>('{}', headers), Map)

        then:
        response.statusCode == HttpStatus.OK
        response.body.skillApplied == true
        response.body.skillId == SkillsFactory.getSkillId()
        skillsService.getPerformedSkills(skillsService.userName, SkillsFactory.getDefaultProjId(), '', 'skillId').data.size() == 1

        where:
        method << [HttpMethod.POST, HttpMethod.PUT]
    }

    private String resolvePath(String path) {
        path.replace('{projectId}', SkillsFactory.getDefaultProjId())
                .replace('{crossProjectId}', SkillsFactory.getDefaultProjId())
                .replace('{skillId}', SkillsFactory.getSkillId())
                .replace('{notificationId}', '1')
                .replace('{quizId}', 'MissingQuiz')
                .replace('{attemptId}', '1')
                .replace('{answerId}', '1')
                .replace('{quizAttempId}', '1')
    }

    private String payloadFor(String path) {
        if (path == '/public/log') {
            return '{"message":"CSRF probe","level":{"value":3,"name":"INFO"}}'
        }
        if (path.endsWith('/skillsClientVersion')) {
            return '{"skillsClientVersion":"1.0.0"}'
        }
        if (path.endsWith('/contact')) {
            return '{"message":"Hello project owner"}'
        }
        if (path.endsWith('/addPrefixToInvalidParagraphs')) {
            return '{"value":"Hello world","prefix":"Note: "}'
        }
        if (path.endsWith('/answers/{answerId}')) {
            return '{"isCorrect":true}'
        }
        return '{}'
    }

    private HttpStatusCode responseOrError(RestTemplate client, String url, HttpMethod method, String payload, HttpHeaders headers) {
        try {
            return client.exchange(url, method, new HttpEntity<>(payload, headers), String).statusCode
        } catch (HttpStatusCodeException e) {
            return e.statusCode
        }
    }


    private String skillUrl() {
        "${skillsService.wsHelper.skillsService}${resolvePath(SKILL_PATH)}"
    }

    private String sessionCookie() {
        skillsService.wsHelper.restTemplateWrapper.authResponse.headers.get(HttpHeaders.SET_COOKIE)
                .find { it.startsWith('JSESSIONID=') }.split(';')[0]
    }

    private String csrfToken(RestTemplate client) {
        HttpHeaders headers = new HttpHeaders()
        if (!isPkiMode) {
            headers.set(HttpHeaders.COOKIE, sessionCookie())
        }
        ResponseEntity<String> response = client.exchange("${skillsService.wsHelper.skillsService}/app/userInfo",
                HttpMethod.GET, new HttpEntity<>(headers), String)
        assert response.statusCode == HttpStatus.OK
        String cookie = response.headers.get(HttpHeaders.SET_COOKIE).find { it.startsWith('XSRF-TOKEN=') }
        assert cookie
        cookie.split(';')[0].substring('XSRF-TOKEN='.length())
    }

    private RestTemplate probeClient() {
        if (!isPkiMode) {
            return new RestTemplate(new HttpComponentsClientHttpRequestFactory())
        }
        Resource certificate = certificateRegistry.getCertificate(skillsService.userName)
        assert certificate
        KeyStore keyStore = KeyStore.getInstance('PKCS12')
        certificate.inputStream.withCloseable { stream -> keyStore.load(stream, 'skillspass'.toCharArray()) }
        KeyStore trustStore = KeyStore.getInstance('JKS')
        new ClassPathResource('/certs/truststore.jks').inputStream.withCloseable { stream ->
            trustStore.load(stream, 'skillspass'.toCharArray())
        }
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(trustStore, TrustAllStrategy.INSTANCE)
                .loadKeyMaterial(keyStore, 'skillspass'.toCharArray()).build()
        def sslSocketFactory = new SSLConnectionSocketFactory(sslContext, ['TLSv1.2'] as String[],
                null, NoopHostnameVerifier.INSTANCE)
        def connectionManager = new PoolingHttpClientConnectionManager(RegistryBuilder.create()
                .register('http', PlainConnectionSocketFactory.getSocketFactory())
                .register('https', sslSocketFactory).build())
        def httpClient = HttpClients.custom().setConnectionManager(connectionManager).disableCookieManagement().build()
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient))
    }
}
