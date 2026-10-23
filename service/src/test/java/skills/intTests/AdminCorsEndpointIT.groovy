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
        'skills.authorization.corsAdminEndpointsCredentialedAllowedOriginPatterns= https://admin.example , https://*.integrator.example , ',
        'skills.authorization.corsAllowedOriginPatterns=https://client.example',
        'skills.authorization.corsConf.allowCredentials=false'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class AdminCorsEndpointIT extends DefaultIntSpec {

    def setup() {
        skillsService.wsHelper.restTemplateWrapper.setRequestFactory(isPkiMode
                ? skillsService.wsHelper.oAuthRestTemplate.requestFactory
                : new HttpComponentsClientHttpRequestFactory())
    }

    def 'authenticated GET #path allows admin origin #origin'() {
        given:
        Map project = SkillsFactory.createProject()
        skillsService.createProject(project)
        HttpHeaders headers = new HttpHeaders()
        headers.setOrigin(origin)
        if (!isPkiMode) {
            String sessionCookie = skillsService.getSessionCookie()
            assert sessionCookie
            headers.set(HttpHeaders.COOKIE, sessionCookie)
        }
        String endpoint = "${skillsService.wsHelper.skillsService}${path.replace('PROJECT_ID', project.projectId)}"

        when:
        ResponseEntity<String> response = skillsService.exchange(
                endpoint, HttpMethod.GET, new HttpEntity<>(headers), String)

        then:
        response.statusCode == HttpStatus.OK
        response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == origin
        response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'
        response.body.contains(expectedContent)

        where:
        path                         | origin                             | expectedContent
        '/admin/projects/PROJECT_ID' | 'https://admin.example'             | 'TestProject1'
        '/app/projects'              | 'https://console.integrator.example' | 'TestProject1'
    }

    def 'admin preflight #method #path accepts configured origins and headers'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.setOrigin('https://console.integrator.example')
        headers.setAccessControlRequestMethod(method)
        headers.setAccessControlRequestHeaders(['authorization', 'content-type', 'x-xsrf-token'])

        when:
        ResponseEntity<String> response = skillsService.exchange(
                "${skillsService.wsHelper.skillsService}${path}".toString(), HttpMethod.OPTIONS, new HttpEntity<>(headers), String)

        then:
        response.statusCode == HttpStatus.OK
        response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == 'https://console.integrator.example'
        response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'
        response.headers.accessControlAllowMethods.contains(method)
        response.headers.accessControlAllowHeaders*.toLowerCase().containsAll(['authorization', 'content-type', 'x-xsrf-token'])

        where:
        path                         | method
        '/admin/projects/TestProject' | HttpMethod.POST
        '/admin/projects/TestProject' | HttpMethod.PUT
        '/admin/projects/TestProject' | HttpMethod.DELETE
        '/app/projects'               | HttpMethod.GET
        '/app/quiz-definitions'       | HttpMethod.GET
        '/app/badges'                 | HttpMethod.GET
        '/performLogin'               | HttpMethod.POST
        '/logout'                     | HttpMethod.POST
    }

    def 'origin #origin cannot access #path with #method'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.setOrigin(origin)
        if (method == HttpMethod.OPTIONS) {
            headers.setAccessControlRequestMethod(HttpMethod.GET)
        }

        when:
        skillsService.exchange(
                "${skillsService.wsHelper.skillsService}${path}".toString(), method, new HttpEntity<>(headers), String)

        then:
        HttpClientErrorException exception = thrown()
        exception.statusCode == HttpStatus.FORBIDDEN
        exception.responseBodyAsString == 'Invalid CORS request'
        exception.responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == null

        where:
        path                         | method             | origin
        '/admin/projects/TestProject' | HttpMethod.GET     | 'https://untrusted.example'
        '/app/projects'               | HttpMethod.GET     | 'https://untrusted.example'
        '/admin/projects/TestProject' | HttpMethod.OPTIONS | 'https://client.example'
        '/app/userInfo'               | HttpMethod.OPTIONS | 'https://admin.example'
        '/api/projects/TestProject'   | HttpMethod.OPTIONS | 'https://admin.example'
        '/public/status'             | HttpMethod.OPTIONS | 'https://admin.example'
        '/performLogin'               | HttpMethod.OPTIONS | 'https://untrusted.example'
        '/logout'                     | HttpMethod.OPTIONS | 'https://untrusted.example'
    }

    def 'userInfo retains the separate client origin policy'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.setOrigin('https://client.example')
        headers.setAccessControlRequestMethod(HttpMethod.GET)

        when:
        ResponseEntity<String> response = skillsService.exchange(
                "${skillsService.wsHelper.skillsService}/app/userInfo".toString(),
                HttpMethod.OPTIONS, new HttpEntity<>(headers), String)

        then:
        response.statusCode == HttpStatus.OK
        response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == 'https://client.example'
        response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) == null
    }

    def 'app listing #path rejects preflight for mutation #method'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.setOrigin('https://admin.example')
        headers.setAccessControlRequestMethod(method)

        when:
        skillsService.exchange("${skillsService.wsHelper.skillsService}${path}".toString(),
                HttpMethod.OPTIONS, new HttpEntity<>(headers), String)

        then:
        HttpClientErrorException exception = thrown()
        exception.statusCode == HttpStatus.FORBIDDEN
        exception.responseBodyAsString == 'Invalid CORS request'
        exception.responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == null

        where:
        [path, method] << [['/app/projects', '/app/quiz-definitions', '/app/badges'],
                          [HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE]].combinations()
    }

    def 'excluded app endpoint #path does not grant admin CORS access'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.setOrigin('https://admin.example')
        headers.setAccessControlRequestMethod(HttpMethod.GET)

        when:
        ResponseEntity<String> response = skillsService.exchange(
                "${skillsService.wsHelper.skillsService}${path}".toString(),
                HttpMethod.OPTIONS, new HttpEntity<>(headers), String)

        then:
        response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) == null
        response.headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) == null

        where:
        path << ['/app/admin-group-definitions', '/app/userInfo/hasRole/ROLE_PROJECT_ADMIN',
                 '/app/userInfo/settings', '/app/projects/TestProject', '/app/quiz-definitions/TestQuiz',
                 '/app/badges/TestBadge', '/app/projectExist']
    }
}
