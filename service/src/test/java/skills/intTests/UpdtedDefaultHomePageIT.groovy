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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec

@Slf4j
@SpringBootTest(properties = ['skills.h2.port=9092',
        'skills.config.ui.rankingAndProgressViewsEnabled=true',
        'skills.config.ui.defaultLandingPage=progress',
        'skills.authorization.userInfoUri=https://localhost:8184/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8184/userQuery?query={query}',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8184/actuator/health'], webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class UpdtedDefaultHomePageIT extends DefaultIntSpec {

    def "landingPage page is always admin when rankingAndProgressViewsDisabled=true"() {

        def currentUser1 = skillsService.getCurrentUser()
        def res1 = skillsService.getUserSettings()
        when:

        skillsService.changeUserSettings([
                [userId: 'skills@skills.org', settingGroup: 'user.prefs', setting: "home_page", value: "progress"],
        ])
        def currentUser2 = skillsService.getCurrentUser()
        def res2 = skillsService.getUserSettings()

        then:
        currentUser1.landingPage == 'progress'  // default is updated to progress
    }
}
