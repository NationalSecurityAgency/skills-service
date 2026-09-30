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
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsFactory
import spock.lang.Unroll

import javax.net.ssl.SSLContext
import java.security.KeyStore

class AdminCsrfSpecs extends DefaultIntSpec {

    @Autowired
    RequestMappingHandlerMapping requestMappingHandlerMapping

    Integer quizQuestionId

    private static final List<List> MULTIPART_ENDPOINTS = [
            [HttpMethod.POST, '/admin/projects/TestProject1/upload', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]],
            [HttpMethod.PUT, '/admin/projects/TestProject1/upload', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]],
            [HttpMethod.POST, '/admin/projects/TestProject1/icons/upload', [customIcon: new ClassPathResource('/dot.png')]],
            [HttpMethod.PUT, '/admin/projects/TestProject1/icons/upload', [customIcon: new ClassPathResource('/dot.png')]],
            [HttpMethod.POST, '/admin/badges/csrfBadge/icons/upload', [customIcon: new ClassPathResource('/dot.png')]],
            [HttpMethod.PUT, '/admin/badges/csrfBadge/icons/upload', [customIcon: new ClassPathResource('/dot.png')]],
            [HttpMethod.POST, '/admin/badges/csrfBadge/upload', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]],
            [HttpMethod.PUT, '/admin/badges/csrfBadge/upload', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]],
            [HttpMethod.POST, '/admin/quiz-definitions/TestQuiz1/upload', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]],
            [HttpMethod.PUT, '/admin/quiz-definitions/TestQuiz1/upload', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]],
            [HttpMethod.POST, '/admin/projects/TestProject1/skills/skill1/video', [file: new ClassPathResource('/testVideos/create-project.webm')]],
            [HttpMethod.PUT, '/admin/projects/TestProject1/skills/skill1/video', [file: new ClassPathResource('/testVideos/create-project.webm')]],
            [HttpMethod.POST, '/admin/projects/TestProject1/skills/skill1/slides', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]],
            [HttpMethod.PUT, '/admin/projects/TestProject1/skills/skill1/slides', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]],
            [HttpMethod.POST, '/admin/quiz-definitions/TestQuiz1/slides', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]],
            [HttpMethod.PUT, '/admin/quiz-definitions/TestQuiz1/slides', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]],
            [HttpMethod.POST, '/admin/quiz-definitions/TestQuiz1/questions/{questionId}/video', [file: new ClassPathResource('/testVideos/empty-quiz.mp4')]],
            [HttpMethod.PUT, '/admin/quiz-definitions/TestQuiz1/questions/{questionId}/video', [file: new ClassPathResource('/testVideos/empty-quiz.mp4')]],
            [HttpMethod.POST, '/api/projects/TestProject1/skills/skill1/upload', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]],
            [HttpMethod.PUT, '/api/projects/TestProject1/skills/skill1/upload', [file: new ClassPathResource('/testSlides/test-slides-1.pdf')]]
    ]

    def 'every multipart POST and PUT mapping has a CSRF policy probe'() {
        given:
        Set<List> covered = MULTIPART_ENDPOINTS.collect { HttpMethod method, String path, Map parts ->
            [method, path.replace('TestProject1', '{projectId}')
                    .replace('TestQuiz1', '{quizId}')
                    .replace('csrfBadge', '{badgeId}')
                    .replace('skill1', '{skillId}')]
        } as Set
        Set<List> mappings = requestMappingHandlerMapping.handlerMethods.findAll { RequestMappingInfo info, handler ->
            handler.method.parameterTypes.any { MultipartFile.isAssignableFrom(it) }
        }.keySet().collectMany { RequestMappingInfo info ->
            info.pathPatternsCondition.patternValues.collectMany { String path ->
                info.methodsCondition.methods.findAll { it.name() in ['POST', 'PUT'] }
                        .collect { requestMethod -> [HttpMethod.valueOf(requestMethod.name()), path] }
            }
        } as Set

        expect:
        mappings == covered
    }

    def setup() {
        if (!isPkiMode) {
            // Login rotates the CSRF token. Obtain its replacement before preparing fixtures.
            skillsService.wsHelper.restTemplateWrapper.getForEntity("${skillsService.wsHelper.skillsService}/app/userInfo", String)
        }
        skillsService.createProjectAndSubjectAndSkills(SkillsFactory.createProject(), SkillsFactory.createSubject(),
                [SkillsFactory.createSkill(1, 1, 1, 0, 1, 0, 10)])
        skillsService.createQuizDef(QuizDefFactory.createQuiz())
        quizQuestionId = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion()).body.id
        skillsService.createGlobalBadge([badgeId: 'csrfBadge', name: 'CSRF badge'])
    }

    @Unroll
    def '#method #path rejects a multipart request without an XSRF header and accepts one with it'() {
        given:
        String url = "${skillsService.wsHelper.skillsService}${path.replace('{questionId}', quizQuestionId.toString())}"
        RestTemplate client = probeClient()
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>()
        parts.each { key, value -> body.add(key, value) }
        HttpHeaders headers = new HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        headers.accept = [MediaType.APPLICATION_JSON]
        String token = csrfToken()
        headers.set(HttpHeaders.COOKIE, isPkiMode ? "XSRF-TOKEN=${token}" : "${sessionCookie()}; XSRF-TOKEN=${token}")

        when:
        exchange(client, url, method, body, headers)

        then:
        HttpClientErrorException missingToken = thrown()
        missingToken.statusCode == HttpStatus.FORBIDDEN

        when:
        HttpHeaders protectedHeaders = new HttpHeaders()
        protectedHeaders.addAll(headers)
        protectedHeaders.set('X-XSRF-TOKEN', token)
        ResponseEntity<String> permitted = exchange(client, url, method, body, protectedHeaders)

        then:
        permitted.statusCode == HttpStatus.OK

        where:
        [method, path, parts] << MULTIPART_ENDPOINTS
    }

    @Unroll
    def '#method #path requires an XSRF header for a non-multipart admin request'() {
        given:
        String url = "${skillsService.wsHelper.skillsService}${path}"
        RestTemplate client = probeClient()
        String token = csrfToken()
        HttpHeaders headers = new HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = [MediaType.APPLICATION_JSON]
        headers.set(HttpHeaders.COOKIE, isPkiMode ? "XSRF-TOKEN=${token}" : "${sessionCookie()}; XSRF-TOKEN=${token}")

        when:
        client.exchange(url, method, new HttpEntity<>(body, headers), String)

        then:
        HttpClientErrorException missingHeader = thrown()
        missingHeader.statusCode == HttpStatus.FORBIDDEN

        when:
        HttpHeaders protectedHeaders = new HttpHeaders()
        protectedHeaders.addAll(headers)
        protectedHeaders.set('X-XSRF-TOKEN', token)
        ResponseEntity<String> permitted = client.exchange(url, method, new HttpEntity<>(body, protectedHeaders), String)

        then:
        permitted.statusCode == HttpStatus.OK

        where:
        method          | path                                                    | body
        HttpMethod.POST | '/admin/projects/TestProject1/subjects/Subject2'        | '{"subjectId":"Subject2","name":"New Subject"}'
        HttpMethod.PUT  | '/admin/projects/TestProject1/subjects/TestSubject1'    | '{"subjectId":"TestSubject1","name":"Updated Subject"}'
        HttpMethod.POST | '/admin/projects/TestProject1/reportSkillEvents'        | '{"skillIds":["skill1"],"userIds":["skills@skills.org"]}'
        HttpMethod.PUT  | '/admin/projects/TestProject1/reportSkillEvents'        | '{"skillIds":["skill1"],"userIds":["skills@skills.org"]}'
    }

    private ResponseEntity<String> exchange(RestTemplate client, String url, HttpMethod method,
                                            MultiValueMap<String, Object> body, HttpHeaders headers) {
        client.exchange(url, method, new HttpEntity<>(body, headers), String)
    }

    private String sessionCookie() {
        skillsService.wsHelper.restTemplateWrapper.authResponse.headers.get(HttpHeaders.SET_COOKIE)
                .find { it.startsWith('JSESSIONID=') }.split(';')[0]
    }

    private String csrfToken() {
        RestTemplate client = probeClient()
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
