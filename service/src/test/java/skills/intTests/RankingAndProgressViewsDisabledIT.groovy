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
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

@Slf4j
@SpringBootTest(properties = ['skills.h2.port=9091', 'skills.config.ui.rankingAndProgressViewsEnabled=false', 'skills.config.ui.defaultLandingPage=progress',
        'skills.authorization.userInfoUri=https://localhost:8182/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8182/userQuery?query={query}',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8182/status'], webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class RankingAndProgressViewsDisabledIT extends DefaultIntSpec {

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
        currentUser1.landingPage == 'admin'  // default
        !res1

        currentUser2.landingPage == 'admin'  // must NOT be overridden
        res2.size() == 1
        res2.get(0).userId == 'skills@skills.org'
        res2.get(0).setting == 'home_page'
        res2.get(0).value == "progress"
        res2.get(0).settingGroup == "user.prefs"
        res2.get(0).projectId == null
    }

    def "get my project summary should throw an error"() {
        when:
        skillsService.getMyProgressSummary()
        then:
        SkillsClientException e = thrown()
        e.resBody.contains("Progress and Ranking Views are disabled for this installation of the SkillTree")
    }

    def "get available my projects should throw an error"() {
        when:
        skillsService.getAvailableMyProjects()
        then:
        SkillsClientException e = thrown()
        e.resBody.contains("Progress and Ranking Views are disabled for this installation of the SkillTree")
    }

    def "add my projects should throw an error"() {
       def proj = SkillsFactory.createProject();
        skillsService.createProject(proj)
        when:
        skillsService.addMyProject(proj.projectId)
        then:
        SkillsClientException e = thrown()
        e.getMessage().contains("Progress and Ranking Views are disabled for this installation of the SkillTree")
    }


}
