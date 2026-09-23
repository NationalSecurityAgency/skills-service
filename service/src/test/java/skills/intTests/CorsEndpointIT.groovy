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
package skills.intTests

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.HttpClientErrorException
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

@SpringBootTest(properties = [
        'skills.authorization.corsAllowedOriginPatterns=https://trusted.example',
        'skills.authorization.corsConf.allowCredentials=false',
        'skills.h2.port=9096'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class CorsEndpointIT extends DefaultIntSpec {

    def setup() {
        // Preserve the facade's client certificate and trust configuration in PKI mode.
        // In FORM mode use Apache HTTP: the JDK transport silently drops Origin.
        skillsService.wsHelper.restTemplateWrapper.setRequestFactory(isPkiMode
                ? skillsService.wsHelper.oAuthRestTemplate.requestFactory
                : new HttpComponentsClientHttpRequestFactory())
    }

    def 'API requests from an untrusted origin are denied'() {
        given:
        def project = SkillsFactory.createProject()
        skillsService.createProject(project)
        String token
        if (!isPkiMode) {
            skillsService.setProxyCredentials(project.projectId, skillsService.getClientSecret(project.projectId))
            token = skillsService.wsHelper.getTokenForUser(skillsService.userName)
        }
        HttpHeaders headers = new HttpHeaders()
        headers.setOrigin('http://localhost:8080')
        if (token) {
            headers.setBearerAuth(token)
        }
        HttpEntity<?> request = new HttpEntity<>(headers)

        HttpHeaders trustedHeaders = new HttpHeaders()
        trustedHeaders.setOrigin('https://trusted.example')
        if (token) {
            trustedHeaders.setBearerAuth(token)
        }
        String endpoint = "${skillsService.wsHelper.skillsService}/api/projects/${project.projectId}/summary"
        ResponseEntity<String> trustedResponse = skillsService.wsHelper.restTemplateWrapper.exchange(
                endpoint, HttpMethod.GET, new HttpEntity<>(trustedHeaders), String)
        assert trustedResponse.statusCode == HttpStatus.OK
        assert trustedResponse.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == 'https://trusted.example'

        when:
        skillsService.wsHelper.restTemplateWrapper.exchange(
                endpoint,
                HttpMethod.GET,
                request,
                String)

        then:
        HttpClientErrorException exception = thrown()
        exception.statusCode == HttpStatus.FORBIDDEN
        exception.responseBodyAsString == 'Invalid CORS request'
        exception.responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == null
    }

    def 'portal endpoint #method #path allows trusted origins and rejects untrusted origins'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.setOrigin('https://trusted.example')
        String endpoint = "${skillsService.wsHelper.skillsService}${path}"
        ResponseEntity<String> response = skillsService.wsHelper.restTemplateWrapper.exchange(
                endpoint, method, new HttpEntity<>(body, headers), String)
        assert response.statusCode == HttpStatus.OK
        assert response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == 'https://trusted.example'
        assert response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) == credentials

        when:
        headers.setOrigin('http://localhost:8080')
        skillsService.wsHelper.restTemplateWrapper.exchange(
                endpoint, method, new HttpEntity<>(body, headers), String)

        then:
        HttpClientErrorException exception = thrown()
        exception.statusCode == HttpStatus.FORBIDDEN
        exception.responseBodyAsString == 'Invalid CORS request'
        exception.responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == null
        exception.responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) == null

        where:
        path                           | method          | credentials | body
        '/app/userInfo'                 | HttpMethod.GET  | null        | null
        '/public/status'                | HttpMethod.GET  | null        | null
        '/public/clientDisplay/config'  | HttpMethod.GET  | null        | null
        '/public/log'                   | HttpMethod.POST | null        | [message: 'CORS test', level: [value: 3, name: 'INFO']]
        '/public/log'                   | HttpMethod.PUT  | null        | [message: 'CORS test', level: [value: 3, name: 'INFO']]
    }

    def 'preflight without bearer credentials for #path honors configured origins'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.setOrigin('https://trusted.example')
        headers.setAccessControlRequestMethod(method)
        headers.setAccessControlRequestHeaders(['authorization', 'content-type'])
        String endpoint = "${skillsService.wsHelper.skillsService}${path}"
        ResponseEntity<String> response = skillsService.wsHelper.restTemplateWrapper.exchange(
                endpoint, HttpMethod.OPTIONS, new HttpEntity<>(headers), String)
        assert response.statusCode == HttpStatus.OK
        assert response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == 'https://trusted.example'
        assert response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) == credentials
        assert response.headers.accessControlAllowMethods.contains(method)
        assert response.headers.accessControlAllowHeaders*.toLowerCase().containsAll(['authorization', 'content-type'])

        when:
        headers.setOrigin('http://localhost:8080')
        skillsService.wsHelper.restTemplateWrapper.exchange(
                endpoint, HttpMethod.OPTIONS, new HttpEntity<>(headers), String)

        then:
        HttpClientErrorException exception = thrown()
        exception.statusCode == HttpStatus.FORBIDDEN
        exception.responseBodyAsString == 'Invalid CORS request'
        exception.responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == null

        where:
        path                           | method          | credentials
        '/api/projects/TestProject/summary' | HttpMethod.GET | null
        '/app/userInfo'                 | HttpMethod.GET  | null
        '/public/status'                | HttpMethod.GET  | null
        '/public/clientDisplay/config'  | HttpMethod.GET  | null
        '/public/log'                   | HttpMethod.POST | null
    }
}
