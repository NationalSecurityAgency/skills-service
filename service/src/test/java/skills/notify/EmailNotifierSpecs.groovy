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
package skills.notify

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.thymeleaf.context.Context
import skills.controller.request.model.GlobalSettingsRequest
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.services.settings.SettingsService
import skills.settings.EmailSettingsService
import skills.storage.model.Notification
import skills.storage.model.Setting
import skills.storage.repos.NotificationsRepo
import skills.storage.repos.SettingRepo
import skills.storage.repos.UserAttrsRepo
import skills.utils.LoggerHelper
import skills.utils.WaitFor
import spock.lang.IgnoreRest

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender
import spock.mock.AutoAttach;

class EmailNotifierSpecs extends DefaultIntSpec {

    @Autowired
    EmailNotifier emailNotifier

    @Autowired
    NotificationsRepo notificationsRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    String email
    def setup() {
        startEmailServer()

        // little trick to force PKI-based runs to create UserAttrs record
        skillsService.isRoot()
        email = userAttrsRepo.findByUserId(skillsService.userName).email
        assert email
    }

    def "send email"() {
        when:
        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))
        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 0 }

        then:
        greenMail.getReceivedMessages().length == 1
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)
        emailRes.subj == "Test Subject"
        emailRes.recipients == [email]
        emailRes.plainText == "As plain as day"
        EmailUtils.prepBodyForComparison(emailRes.html) == EmailUtils.prepBodyForComparison('''<!--

    Copyright 2020 SkillTree

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<!DOCTYPE html>
<html lang="en"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.thymeleaf.org http://www.thymeleaf.org">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
</head>
<body class="overall-container">
<h1>Test Template param value</h1>
</body>
</head>
</html>
''')

        notificationsRepo.count() == 0
    }

    def "send email with headers and footers configured"() {
        settingRepo.save(new Setting(
                settingGroup: EmailSettingsService.settingsGroup,
                setting: EmailSettingsService.htmlHeader,
                value: '<div class="header">i\'m a header</div>',
                type: Setting.SettingType.Global
        ))
        settingRepo.save(new Setting(
                settingGroup: EmailSettingsService.settingsGroup,
                setting: EmailSettingsService.htmlFooter,
                value: '<div class="footer">i\'m a footer</div>',
                type: Setting.SettingType.Global
        ))
        settingRepo.save(new Setting(
                settingGroup: EmailSettingsService.settingsGroup,
                setting: EmailSettingsService.plaintextHeader,
                value: 'HEADERHEADERHEADER',
                type: Setting.SettingType.Global
        ))
        settingRepo.save(new Setting(
                settingGroup: EmailSettingsService.settingsGroup,
                setting: EmailSettingsService.plaintextFooter,
                value: 'FOOTERFOOTERFOOTER',
                type: Setting.SettingType.Global
        ))

        when:
        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))
        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 0 }

        then:
        greenMail.getReceivedMessages().length == 1
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)
        emailRes.subj == "Test Subject"
        emailRes.recipients == [email]
        emailRes.plainText == """HEADERHEADERHEADER\r\nAs plain as day\r\nFOOTERFOOTERFOOTER"""
        EmailUtils.prepBodyForComparison(emailRes.html) == EmailUtils.prepBodyForComparison('''<!--

    Copyright 2020 SkillTree

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<!DOCTYPE html>
<html lang="en"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.thymeleaf.org http://www.thymeleaf.org">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
</head>
<body class="overall-container">
<div class="header">i'm a header</div>
<h1>Test Template param value</h1>
<div class="footer">i'm a footer</div>
</body>
</head>
</html>
''')

        notificationsRepo.count() == 0
    }

    def "notification fails if used id doesn't exist"() {

        when:
        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: ['doNotExist'],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))
        then:
        thrown(DataIntegrityViolationException)

        notificationsRepo.count() == 0
    }

    def "send multiple notification when service is down"() {
        setup:
        LoggerHelper loggerHelper = new LoggerHelper(EmailNotifier.class)

        greenMail.stop()

        when:
        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))

        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))

        WaitFor.wait { loggerHelper.hasLogMsgStartsWith("Dispatched ") }

        List<ILoggingEvent> logsList = loggerHelper.logEvents;

        then:
        // should be only 1 notification of failure
        logsList.findAll { it.message.startsWith("Failed to sent notification") }.size() == 1

        // dispatch should only happen 1 time
        logsList.findAll { it.message.startsWith("Dispatched ") }.size() == 1
        logsList.find { it.message.startsWith("Dispatched [0] notification(s) with [2] error(s)") }

        notificationsRepo.count() == 2

        cleanup:
        loggerHelper.stop()
    }

    def "failed emails must be retried when the email server is back functioning"() {
        setup:
        LoggerHelper loggerHelper = new LoggerHelper(EmailNotifier.class)

        greenMail.stop()

        when:
        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))

        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))

        WaitFor.wait { loggerHelper.hasLogMsgStartsWith("Dispatched ") }
        assert loggerHelper.logEvents.find { it.message.startsWith("Dispatched [0] notification(s) with [2] error(s)") }

        greenMail.start()

        assert WaitFor.wait { greenMail.getReceivedMessages().length > 1 }
        assert WaitFor.wait { loggerHelper.logEvents.find { it.message.startsWith("Retry: Dispatched [2] notification(s) with [0] error(s)") } }

        then:
        greenMail.getReceivedMessages().length == 2
        EmailUtils.getEmails(greenMail).collect { it.subj } == ["Test Subject", "Test Subject"]

        notificationsRepo.count() == 0

        cleanup:
        loggerHelper.stop()
    }

    def "remove notification if server never returns"() {
        setup:
        LoggerHelper loggerHelper = new LoggerHelper(EmailNotifier.class)

        greenMail.stop()

        when:
        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))

        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))

        WaitFor.wait(120) { loggerHelper.logEvents.findAll { it.message.startsWith("Removed notification because it failed and exceeded max retention of [30] seconds") }.size() == 2 }

        WaitFor.wait(10) { notificationsRepo.count() == 0 }

        then:
        notificationsRepo.count() == 0

        cleanup:
        loggerHelper.stop()
    }

}
