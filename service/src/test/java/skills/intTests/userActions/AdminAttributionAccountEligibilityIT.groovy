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
package skills.intTests.userActions

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import skills.auth.adminAttribution.AdminAttributionProperties
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.settings.EmailSettingsService
import skills.storage.model.Setting
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import spock.lang.IgnoreIf

import static skills.intTests.utils.SkillsFactory.createProject

@IgnoreIf({ env['SPRING_PROFILES_ACTIVE'] == 'pki' })
@TestPropertySource(properties = [
        'skills.authorization.adminAttribution.enabled=true',
        'skills.authorization.verifyEmailAddresses=true'
])
class AdminAttributionAccountEligibilityIT extends DefaultIntSpec {
    @Autowired
    AdminAttributionProperties attributionProperties

    def doSetup() {
        // This context requires email settings before signup. The default client is unrelated to
        // these verified/unverified account fixtures, so initialize their SMTP prerequisites instead.
        dataResetHelper.resetData()
        greenMail = new GreenMail(ServerSetupTest.SMTP)
        greenMail.start()
        Map<String, String> emailSettings = [
                (EmailSettingsService.hostSetting): 'localhost',
                (EmailSettingsService.portSetting): ServerSetupTest.SMTP.port.toString(),
                (EmailSettingsService.protocolSetting): 'smtp',
                (EmailSettingsService.authSetting): 'false',
                (EmailSettingsService.tlsEnableSetting): 'false',
                (EmailSettingsService.fromEmail): 'attribution@skills.test',
                (EmailSettingsService.publicUrl): "http://localhost:${localPort}".toString(),
        ]
        settingRepo.saveAll(emailSettings.collect { String name, String value ->
            new Setting(setting: name, value: value, type: Setting.SettingType.Global,
                    settingGroup: EmailSettingsService.settingsGroup)
        })
    }

    def cleanup() {
        attributionProperties.allowedRequesters = [] as Set
    }

    def 'an unverified administrator cannot be attributed when email verification is required'() {
        given:
        List<String> userIds = getRandomUsers(2)
        createService(userIds[0])
        SkillsService actor = createService(userIds[1])
        UserAttrs requesterAttrs = userAttrsRepo.findByUserIdIgnoreCase(userIds[0])
        requesterAttrs.emailVerified = 'true'
        userAttrsRepo.save(requesterAttrs)
        SkillsService requester = createService(userIds[0])
        // Randomized client identities are bound to the allowlist for this dedicated context.
        attributionProperties.allowedRequesters = [requester.userName.toLowerCase(Locale.ROOT)] as Set
        Map resource = quizResource ? QuizDefFactory.createQuiz() : createProject()
        String id = quizResource ? resource.quizId : resource.projectId
        String path = "/admin/${quizResource ? 'quiz-definitions' : 'projects'}/${id}"
        if (quizResource) {
            requester.createQuizDef(resource)
            requester.addQuizUserRole(id, actor.userName, RoleName.ROLE_QUIZ_ADMIN.name())
        } else {
            requester.createProject(resource)
            requester.addProjectAdmin(id, actor.userName)
        }
        long historyCount = (quizResource ? requester.getUserActionsForQuiz(id) : requester.getUserActionsForProject(id)).count
        HttpHeaders headers = new HttpHeaders()
        headers.set('X-SkillTree-Acting-User', actor.userName)

        expect:
        actor.wsHelper.restTemplateWrapper.authResponse.statusCode == HttpStatus.UNAUTHORIZED

        when:
        ResponseEntity<Map> rejected = requester.requestWithHeaders(path, HttpMethod.POST,
                resource + [name: 'Must not change'], headers)

        then:
        rejected.statusCode == HttpStatus.FORBIDDEN
        (quizResource ? requester.getQuizDef(id) : requester.getProject(id)).name == resource.name
        (quizResource ? requester.getUserActionsForQuiz(id) : requester.getUserActionsForProject(id)).count == historyCount

        when:
        UserAttrs actorAttrs = userAttrsRepo.findByUserIdIgnoreCase(actor.userName)
        actorAttrs.emailVerified = 'true'
        userAttrsRepo.save(actorAttrs)
        ResponseEntity<Map> accepted = requester.requestWithHeaders(path, HttpMethod.POST,
                resource + [name: 'Eligible attributed edit'], headers)
        Map edit = (quizResource ? requester.getUserActionsForQuiz(id) : requester.getUserActionsForProject(id))
                .data.find { Map row -> row.action == 'Edit' }

        then:
        accepted.statusCode == HttpStatus.OK
        (quizResource ? requester.getQuizDef(id) : requester.getProject(id)).name == 'Eligible attributed edit'
        edit.userId == actor.userName.toLowerCase(Locale.ROOT)
        (quizResource ? requester.getQuizUserActionAttributes(id, edit.id as Long) :
                requester.getProjectUserActionAttributes(id, edit.id as Long)).requestingSystemUserId == requester.userName.toLowerCase(Locale.ROOT)

        where:
        quizResource << [false, true]
    }
}
