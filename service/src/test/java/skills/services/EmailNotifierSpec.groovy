/**
 * Copyright 2021 SkillTree
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

import groovy.json.JsonOutput
import org.springframework.mail.javamail.JavaMailSender
import skills.notify.EmailNotifier
import skills.notify.builders.NotificationEmailBuilder
import skills.notify.builders.NotificationEmailBuilderManager
import skills.services.settings.SettingsService
import skills.settings.EmailSettingsService
import skills.storage.model.Notification
import skills.storage.repos.NotificationsRepo
import skills.storage.repos.UserAttrsRepo
import spock.lang.Specification

import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class EmailNotifierSpec extends Specification {


    def "failed recipients are added back to notification"() {
        EmailSendingService mockSendingService = Mock()
        UserAttrsRepo mockAttrRepo = Mock(UserAttrsRepo)
        SettingsService mockSettingsService = Mock(SettingsService)
        NotificationsRepo notificationsRepo = Mock(NotificationsRepo)
        LockingService lockingServiceMock = Mock(LockingService)
        FeatureService featureService = Mock(FeatureService)
        NotificationEmailBuilderManager notificationEmailBuilderManager = Mock(NotificationEmailBuilderManager)
        EmailSettingsService emailSettingsServiceMock = Mock(EmailSettingsService)
        SystemSettingsService systemSettingsServiceMock = Mock(SystemSettingsService)
        JavaMailSender mailSenderMock = Mock(JavaMailSender)

        Notification notification = new Notification(
                requestedOn: new Date(),
                created: new Date(),
                userId: JsonOutput.toJson(["fake1", "fake2"]),
                failedCount: 0,
                encodedParams: JsonOutput.toJson([
                        htmlBody    : "body",
                        emailSubject: "subject",
                        rawBody     : "body",
                ]),
                type: Notification.Type.ContactUsers,
                id: 1,
        )

        Stream stream = [notification].stream()

        notificationsRepo.streamNewNotifications() >> stream
        mockSettingsService.getGlobalSettingsByGroup(*_) >> []
        emailSettingsServiceMock.getMailSender(*_) >> mailSenderMock
        lockingServiceMock.lockForNotifying()
        notificationEmailBuilderManager.build(notification, _) >> new NotificationEmailBuilder.Res(subject: "subject", html: "body", plainText: "body")

        mockAttrRepo.findEmailByUserId(*_) >>> ['fake1@fake.fake', 'fake2@fake.fake']
        2 * mockSendingService.sendEmail(*_) >>> { throw new RuntimeException("sending failed ") } >>> null

        EmailNotifier emailNotifier = new EmailNotifier()
        emailNotifier.retainFailedNotificationsForNumSecs = TimeUnit.MINUTES.toSeconds(30)
        emailNotifier.notificationsRepo = notificationsRepo
        emailNotifier.emailSettingsService = emailSettingsServiceMock
        emailNotifier.featureService = featureService
        emailNotifier.lockingService = lockingServiceMock
        emailNotifier.notificationEmailBuilderManager = notificationEmailBuilderManager
        emailNotifier.settingsService = mockSettingsService
        emailNotifier.systemSettingsService = systemSettingsServiceMock
        emailNotifier.sendingService = mockSendingService
        emailNotifier.userAttrs = mockAttrRepo

        when:

        emailNotifier.dispatchNotifications()

        then:

        0 * notificationsRepo.deleteById(1)
        0 * notificationsRepo.flush()
        1 * notificationsRepo.save({ it.userId == JsonOutput.toJson(["fake1"]) && it.id == 1 && it.failedCount == 1})
    }

}
