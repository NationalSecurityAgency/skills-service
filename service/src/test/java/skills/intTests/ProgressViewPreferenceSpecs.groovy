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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.repos.UserRepo
import spock.lang.IgnoreIf

class ProgressViewPreferenceSpecs extends DefaultIntSpec {
    @Autowired
    UserRepo userRepo

    private static Map preference(String value, String group = 'user.prefs') {
        return [settingGroup: group, setting: 'sd_progress_view', value: value]
    }

    def "saved progress view applies across projects and survives a new session"() {
        Map project = SkillsFactory.createProject(1)
        Map otherProject = SkillsFactory.createProject(2)
        skillsService.createProject(project)
        skillsService.createProject(otherProject)
        SkillsService learner = createService(getRandomUsers(1)[0])
        skillsService.changeSetting(project.projectId, 'skills-display-compact-view-default',
                [projectId: project.projectId, setting: 'skills-display-compact-view-default', value: 'true'])

        expect:
        learner.getPublicClientDisplayConfigs(project.projectId).compactViewDefault == true
        learner.getPublicClientDisplayConfigs(project.projectId).compactViewPreference == null

        when:
        learner.changeUserSettings([preference('expanded')])

        then:
        learner.getUserSettings().find { Map setting -> setting.setting == 'sd_progress_view' }.value == 'expanded'
        learner.getPublicClientDisplayConfigs(project.projectId).compactViewPreference == false
        learner.getPublicClientDisplayConfigs(project.projectId).compactViewDefault == true
        learner.getPublicClientDisplayConfigs(otherProject.projectId).compactViewPreference == false
        createService(learner.userName).getPublicClientDisplayConfigs(project.projectId).compactViewPreference == false

        when:
        learner.changeUserSettings([preference('compact')])

        then:
        learner.getPublicClientDisplayConfigs(otherProject.projectId).compactViewPreference == true
        learner.getPublicClientDisplayConfigs().compactViewPreference == true

        when:
        learner.changeUserSettings([preference('project-default')])

        then:
        learner.getUserSettings().find { Map setting -> setting.setting == 'sd_progress_view' }.value == 'project-default'
        learner.getPublicClientDisplayConfigs(project.projectId).compactViewPreference == null
        learner.getPublicClientDisplayConfigs(otherProject.projectId).compactViewPreference == null
    }

    def "preferences belong to the authenticated learner regardless of supplied userId"() {
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        SkillsService otherLearner = createService(users[1])

        when:
        learner.changeUserSettings([preference('compact') + [userId: users[1]]])
        otherLearner.changeUserSettings([preference('expanded')])

        then:
        learner.getPublicClientDisplayConfigs().compactViewPreference == true
        otherLearner.getPublicClientDisplayConfigs().compactViewPreference == false
        skillsService.getPublicClientDisplayConfigs().compactViewPreference == null
    }

    def "validity check accepts #value"() {
        SkillsService learner = createService(getRandomUsers(1)[0])

        expect:
        learner.checkUserSettingsValidity([preference(value)]).valid

        where:
        value << ['project-default', 'compact', 'expanded']
    }

    def "reject invalid preference on both validation and direct save: #value #group"() {
        SkillsService learner = createService(getRandomUsers(1)[0])
        learner.changeUserSettings([preference('compact')])

        when:
        learner.checkUserSettingsValidity([preference(value, group)])

        then:
        SkillsClientException validationError = thrown()
        validationError.httpStatus == HttpStatus.BAD_REQUEST
        validationError.message.contains('errorCode:BadParam')

        when:
        learner.changeUserSettings([preference(value, group)])

        then:
        SkillsClientException saveError = thrown()
        saveError.httpStatus == HttpStatus.BAD_REQUEST
        saveError.message.contains('errorCode:BadParam')
        learner.getPublicClientDisplayConfigs().compactViewPreference == true

        where:
        value      | group
        'unknown'  | 'user.prefs'
        'true'     | 'user.prefs'
        ''         | 'user.prefs'
        null       | 'user.prefs'
        'Compact'  | 'user.prefs'
        'compact'  | 'other'
    }

    def "client configuration with a preference is not cacheable"() {
        SkillsService learner = createService(getRandomUsers(1)[0])
        learner.changeUserSettings([preference('compact')])

        when:
        ResponseEntity<Map> response = learner.requestWithHeaders('/public/clientDisplay/config', HttpMethod.GET, null, new HttpHeaders())

        then:
        response.body.compactViewPreference == true
        response.headers.getFirst(HttpHeaders.CACHE_CONTROL) == 'no-store'
    }

    @IgnoreIf({ env.SPRING_PROFILES_ACTIVE == 'pki' })
    def "anonymous client configuration contains no personal preference"() {
        skillsService.changeUserSettings([preference('compact')])

        when:
        ResponseEntity<Map> response = new RestTemplate().getForEntity("${skillsService.wsHelper.skillsService}/public/clientDisplay/config", Map)

        then:
        response.body.compactViewPreference == null
        response.headers.getFirst(HttpHeaders.CACHE_CONTROL) == 'no-store'
    }

    @IgnoreIf({ env.SPRING_PROFILES_ACTIVE == 'pki' })
    def "embedded configuration reads the bearer learner preference not the project owner's"() {
        Map project = SkillsFactory.createProject(1)
        skillsService.createProject(project)
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        learner.changeUserSettings([preference('compact')])
        skillsService.changeUserSettings([preference('expanded')])
        skillsService.setProxyCredentials(project.projectId, skillsService.getClientSecret(project.projectId))

        expect:
        skillsService.getPublicClientDisplayConfigsAsProxy(project.projectId, users[0]).compactViewPreference == true
        !userRepo.existsByUserIdIgnoreCase(users[1])

        when:
        Map config = skillsService.getPublicClientDisplayConfigsAsProxy(project.projectId, users[1])

        then:
        config.compactViewPreference == null
        !userRepo.existsByUserIdIgnoreCase(users[1])
        skillsService.getPublicClientDisplayConfigs(project.projectId).compactViewPreference == false
    }
}
