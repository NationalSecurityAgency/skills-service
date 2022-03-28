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
import skills.intTests.utils.SkillsService
import spock.lang.IgnoreIf

@SpringBootTest(properties = ['skills.config.sameSiteNoneEnabled=false','skills.h2.port=9094'], webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class SameSiteCookieAttrDisabledSpec extends DefaultIntSpec {

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "SameSite=None attribute is not on the Set-Cookie header" () {
        setup:
        SkillsService ss = super.createService()

        when:
        ResponseEntity<String> responseEntity = ss.wsHelper.restTemplateWrapper.authResponse

        then:
        responseEntity.statusCode.is2xxSuccessful()
        responseEntity.headers.get('Set-Cookie')
        !responseEntity.headers.get('Set-Cookie').first().contains('SameSite=None')
    }
}
