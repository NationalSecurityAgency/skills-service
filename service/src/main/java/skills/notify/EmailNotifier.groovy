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

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.time.StopWatch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.spring5.SpringTemplateEngine
import skills.services.EmailSendingService
import skills.services.LockingService
import skills.services.settings.SettingsService
import skills.storage.model.Notification
import skills.storage.model.UserAttrs
import skills.storage.repos.NotificationsRepo
import skills.storage.repos.UserAttrsRepo

import java.util.concurrent.TimeUnit

@Component
@Slf4j
class EmailNotifier implements Notifier {

    @Autowired
    EmailSendingService emailSettings

    @Autowired
    UserAttrsRepo userAttrs

    @Autowired
    SettingsService settingsService

    @Autowired
    NotificationsRepo notificationsRepo

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Autowired
    LockingService lockingService

    @Transactional
    @Profile
    void sendNotification(Notifier.NotificationRequest notificationRequest) {

        assert notificationRequest.userIds
        assert notificationRequest.requestedOn
        assert notificationRequest.subject
        assert notificationRequest.plainTextBody
        assert notificationRequest.thymeleafTemplate
        assert notificationRequest.thymeleafTemplateContext

        String htmlBody = thymeleafTemplateEngine.process(notificationRequest.thymeleafTemplate, notificationRequest.thymeleafTemplateContext)
        notificationRequest.userIds.each { String userId ->
            Notification notification = new Notification(
                    body: notificationRequest.plainTextBody,
                    htmlBody: htmlBody,
                    requestedOn: notificationRequest.requestedOn,
                    subject: notificationRequest.subject,
                    userId: userId,
            )

            notificationsRepo.save(notification)
        }

    }



    @Scheduled(cron='#{"${skills.config.notifications.dispatchSchedule:* * * * * ?}"}')
    @Transactional
    void dispatchNotifications() {
        lockingService.lockForNotifying()
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()

        int count = 0
        notificationsRepo.streamNotifications().forEach( { Notification notification ->
            UserAttrs userAttrs = userAttrs.findByUserId(notification.userId)
            emailSettings.sendEmail(notification.subject, userAttrs.email, notification.htmlBody, notification.body, notification.requestedOn)
            removeNotificationImmediately(notification.id)
            count++
        })

        stopWatch.stop()
        if (count > 0) {
            log.info("Dispatched {} notifications in [{}] seconds", count, stopWatch.getTime(TimeUnit.SECONDS))
        }
    }

    private void removeNotificationImmediately(Integer id) {
        notificationsRepo.deleteById(id)
        notificationsRepo.flush()
    }


}
