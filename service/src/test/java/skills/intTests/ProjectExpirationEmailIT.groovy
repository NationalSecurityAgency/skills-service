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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.ProjectExpirationService
import skills.settings.EmailSettingsService
import skills.storage.model.UserAttrs
import skills.utils.WaitFor

import java.util.regex.Pattern

@SpringBootTest(properties = ['skills.config.expirationGracePeriod=0'],
        webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class ProjectExpirationEmailIT extends DefaultIntSpec {

    @Autowired
    ProjectExpirationService expirationService

    def setup() {
        startEmailServer()
    }

    def "email notification formatted appropriately"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        def proj1 = SkillsFactory.createProject(1)

        skillsService.createProject(proj1)
        Date flagForExpiration = new Date()
        expirationService.flagOldProjects(flagForExpiration)

        String otherUser = getRandomUsers(1, false, ['skills@skills.org', DEFAULT_ROOT_USER_ID]).first()

        createService(otherUser)
        skillsService.addProjectAdmin(proj1.projectId, otherUser)

        WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        greenMail.purgeEmailFromAllMailboxes()

        UserAttrs projectAdminUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        UserAttrs otherProjectAdminUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(otherUser)
        UserAttrs rootUserUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(DEFAULT_ROOT_USER_ID.toLowerCase())

        when:
        expirationService.notifyGracePeriodProjectAdmins(flagForExpiration.minus(1))
        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 2 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        Pattern plaintTextMatch = ~/(?s)For All Dragons Only.*Your SkillTree Project Inception, created on \d{4}-\d{2}-\d{2} hasn't been used in at least \d+ days?.*If you take no action, Project Inception will be deleted today \(\d{4}-\d{2}-\d{2}\).*If you wish to stop receiving these emails, visit http:\/\/localhost:\d+\/administrator\/projects\/Inception in the SkillTree dashboard and click the 'Keep' button or delete your Project.*For All Dragons Only/

        Pattern h1 = ~/(?s)For All Dragons Only.*<h1>Your SkillTree Project Isn't Being Used!<\/h1>.+?/
        Pattern p1 = ~/(?s)<p>Your SkillTree Project Test Project#1, created on \d{4}-\d{2}-\d{2} hasn't been used in at least \d+ days?.<\/p>.+?/
        Pattern p2 = ~/(?s)<p>If you take no action, Project Test Project#1 will be deleted today \(\d{4}-\d{2}-\d{2}\).<\/p>.+?/
        Pattern p3 = ~/(?s)<p>If you wish to stop receiving these emails, visit <a href="http:\/\/localhost:\d+\/administrator\/projects\/TestProject1">Test Project#1<\/a> in the SkillTree dashboard and click the 'Keep' button or delete your Project.<\/p>.*For All Dragons Only/

        then:
        emails.size() == 3
        emails.collect {it.recipients[0] }.sort() == [rootUserUserAttrs.email, projectAdminUserAttrs.email, otherProjectAdminUserAttrs.email].sort()
        emails.find { it.subj == "SkillTree Project is expiring!" }
        emails.find {it.recipients == [otherProjectAdminUserAttrs.email]}
        emails.find { plaintTextMatch.matcher(it.plainText).find() }
        emails.findAll {h1.matcher(it.html).find() }?.size() == 3
        emails.find { p1.matcher(it.html).find() }
        emails.find { p2.matcher(it.html).find() }
        emails.find { p3.matcher(it.html).find() }
    }

    def "email notification formatted appropriately for user community protected project"() {
        Map settingRequest = [
                settingGroup : EmailSettingsService.settingsGroup,
                setting : EmailSettingsService.htmlHeader,
                value : 'For {{ community.descriptor }} Only'
        ]
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextHeader
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.htmlFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)
        settingRequest.setting = EmailSettingsService.plaintextFooter
        rootSkillsService.addOrUpdateGlobalSetting(settingRequest.setting, settingRequest)

        List<String> users = getRandomUsers(2, false, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        SkillsService divineDragonSkillsService = createService(users.first())
        SkillsService regularDragonSkillsService = createService(users.last())
        divineDragonSkillsService.getPublicConfigs() // will create user_attrs entry
        regularDragonSkillsService.getPublicConfigs() // will create user_attrs entry

        rootSkillsService.saveUserTag(regularDragonSkillsService.userName, 'dragons', ['DivineDragon'])
        def proj1 = SkillsFactory.createProject(1)
        proj1.enableProtectedUserCommunity = true

        regularDragonSkillsService.createProject(proj1)
        Date flagForExpiration = new Date()
        expirationService.flagOldProjects(flagForExpiration)

        rootSkillsService.saveUserTag(divineDragonSkillsService.userName, 'dragons', ['DivineDragon'])
        regularDragonSkillsService.addProjectAdmin(proj1.projectId, divineDragonSkillsService.userName)

        WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        greenMail.purgeEmailFromAllMailboxes()

        UserAttrs projectAdminUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(regularDragonSkillsService.userName)
        UserAttrs otherProjectAdminUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(divineDragonSkillsService.userName)
        UserAttrs rootUserUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(DEFAULT_ROOT_USER_ID.toLowerCase())

        when:
        expirationService.notifyGracePeriodProjectAdmins(flagForExpiration.minus(1))
        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 2 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)


        Pattern plaintTextMatch = ~/(?s)For All Dragons Only.*Your SkillTree Project Inception, created on \d{4}-\d{2}-\d{2} hasn't been used in at least \d+ days?.*If you take no action, Project Inception will be deleted today \(\d{4}-\d{2}-\d{2}\).*If you wish to stop receiving these emails, visit http:\/\/localhost:\d+\/administrator\/projects\/Inception in the SkillTree dashboard and click the 'Keep' button or delete your Project.*For All Dragons Only/

        Pattern h1 = ~/(?s)For All Dragons Only.*<h1>Your SkillTree Project Isn't Being Used!<\/h1>.+?/
        Pattern p1 = ~/(?s)<p>Your SkillTree Project Test Project#1, created on \d{4}-\d{2}-\d{2} hasn't been used in at least \d+ days?.<\/p>.+?/
        Pattern p2 = ~/(?s)<p>If you take no action, Project Test Project#1 will be deleted today \(\d{4}-\d{2}-\d{2}\).<\/p>.+?/
        Pattern p3 = ~/(?s)<p>If you wish to stop receiving these emails, visit <a href="http:\/\/localhost:\d+\/administrator\/projects\/TestProject1">Test Project#1<\/a> in the SkillTree dashboard and click the 'Keep' button or delete your Project.<\/p>.*For All Dragons Only/
        
        then:
        emails.size() == 3
        emails.collect {it.recipients[0] }.sort() == [rootUserUserAttrs.email, projectAdminUserAttrs.email, otherProjectAdminUserAttrs.email].sort()
        emails.find { it.subj == "SkillTree Project is expiring!" }
        emails.find {it.recipients == [otherProjectAdminUserAttrs.email]}
        emails.find { plaintTextMatch.matcher(it.plainText).find() }
        emails.findAll {h1.matcher(it.html).find() }?.size() == 3
        emails.find { p1.matcher(it.html).find() }
        emails.find { p2.matcher(it.html).find() }
        emails.find { p3.matcher(it.html).find() }
    }

}
