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
package skills.intTests.root

import org.apache.commons.lang3.RandomStringUtils
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService

class BootstrapAccessSpec extends DefaultIntSpec {

    String ultimateRoot = 'jh@dojo.com'
    SkillsService rootSkillsService

    def setup() {
        rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
    }

    def 'bootstrap root user also get SUPERVISOR role'() {
        setup:

        // root user does not yet exist
        assert !rootSkillsService.isRoot()
        // need to call DefaultIntSpec.getRandomUsers so that tests will work in ssl mode
        String userId = getRandomUsers(1)[0]

        //we need a different userId from the default root user for this test
        while (userId.contains("jh@dojo")) {
            userId = getRandomUsers(1)[0]
        }
        Map<String, String> userInfo = [
                firstName: 'A',
                lastName : 'B',
                email    : userId,
                password : 'aaaaaaaa',
        ]

        when:
        // this calls the bootstrap endpoint
        rootSkillsService.createRootAccount(userInfo)

        def result = rootSkillsService.getUsersWithRole('ROLE_SUPERVISOR')

        then:
        result
        result.find { it.userId.toLowerCase() == userId.toLowerCase() }
    }
}
