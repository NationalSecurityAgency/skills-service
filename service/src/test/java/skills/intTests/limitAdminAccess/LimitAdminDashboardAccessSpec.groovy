/**
 * Copyright 2024 SkillTree
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
package skills.intTests.limitAdminAccess

import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.settings.Settings

class LimitAdminDashboardAccessSpec extends DefaultIntSpec {

    def "by default userInfo.adminDashboardAccess is always true for any role"() {
        List<String> users = getRandomUsers(3)
        SkillsService rootSkillsService = createRootSkillService(users[0])
        SkillsService user1Service = createService(users[1])
        SkillsService user2Service = createService(users[2])
        rootSkillsService.grantDashboardAdminRole(users[1])

        when:
        def rootUserInfo = rootSkillsService.getCurrentUser()
        def user1Info = user1Service.getCurrentUser()
        def user2Info = user2Service.getCurrentUser()
        then:
        rootUserInfo.adminDashboardAccess == true
        user1Info.adminDashboardAccess == true
        user2Info.adminDashboardAccess == true
    }

    boolean isForbidden(Closure c) {
        try {
            c.call()
        } catch (SkillsClientException sk) {
            assert sk.httpStatus == HttpStatus.FORBIDDEN
            return true
        }

        return false
    }

}
