/**
 * Copyright 2021 SkillTree
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
import org.springframework.http.ResponseEntity
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec

@SpringBootTest(properties = ['skills.prof.serverTimingAPI.enabled=false',
        'skills.authorization.userInfoUri=https://localhost:8183/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8183/userQuery?query={query}',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8183/status'],
        webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class ServerTimingApiIT extends DefaultIntSpec {

    def "server timing is disabled" () {
        when:
        ResponseEntity<String> responseEntity = skillsService.wsHelper.rawGet("/app/projects", [:])

        then:
        responseEntity.statusCode.is2xxSuccessful()
        !responseEntity.headers.get('Server-Timing')
    }


}
