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
package skills.services

import skills.services.IdFormatValidator
import spock.lang.Specification
import skills.controller.exceptions.SkillException

class IdFormatValidatorSpec extends Specification {

    def "allow alphanumeric and underscores"() {
        when:
        IdFormatValidator.validate(id)

        then:
        noExceptionThrown()

        where:
        id << ["Works_Well", "1sK20_dk28939", "1234"]
    }

    def "do not allow special characters"() {
        when:
        IdFormatValidator.validate(id)

        then:
        thrown(SkillException)

        where:
        id << ["Works_W\$ll", "1*K20_dk28939"]
    }

}
