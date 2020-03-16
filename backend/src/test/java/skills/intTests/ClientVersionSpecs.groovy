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
package skills.intTests

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class ClientVersionSpecs extends DefaultIntSpec {

    def "report skills-client version"() {
        when:
        def result = skillsService.reportClientVersion(SkillsFactory.defaultProjId, "skill-fake-version-1.0.0")
        then:
        result
        result.statusCode == HttpStatus.OK
        result.body.success == true
    }
}
