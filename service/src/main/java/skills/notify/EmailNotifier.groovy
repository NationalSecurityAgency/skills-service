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
import groovy.json.JsonException
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.time.StopWatch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.SettingsResult
import skills.notify.builders.Formatting
import skills.notify.builders.NotificationEmailBuilder
import skills.notify.builders.NotificationEmailBuilderManager
import skills.services.EmailSendingService
import skills.services.FeatureService
import skills.services.LockingService
import skills.services.SystemSettingsService
import skills.services.settings.SettingsService
import skills.settings.EmailSettingsService
import skills.storage.model.Notification
import skills.storage.repos.NotificationsRepo
import skills.storage.repos.UserAttrsRepo

import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@Component
@Slf4j
class EmailNotifier implements Notifier {

    @Value('#{"${skills.config.notifications.retainFailedNotificationsForNumSecs}"}')
    int retainFailedNotificationsForNumSecs

    @Value('#{"${skills.config.notifications.maxRecipients:50}"}')
    int maxRecipients = 50

    @Autowired
    EmailSendingService sendingService

    @Autowired
    UserAttrsRepo userAttrs

    @Autowired
    SettingsService settingsService

    @Autowired
    NotificationsRepo notificationsRepo

    @Autowired
    LockingService lockingService

    @Autowired
    FeatureService featureService

    @Autowired
    NotificationEmailBuilderManager notificationEmailBuilderManager

    @Autowired
    EmailSettingsService emailSettingsService

    @Autowired
    SystemSettingsService systemSettingsService

    @Transactional
    @Profile
    void sendNotification(Notifier.NotificationRequest notificationRequest) {

        assert notificationRequest.userIds
        assert notificationRequest.requestedOn
        assert notificationRequest.type
        assert notificationRequest.keyValParams

        if (!featureService.isEmailServiceFeatureEnabled()) {
            return
        }

        String serParams = JsonOutput.toJson(notificationRequest.keyValParams)

        Closure saveNotification = { List<String> userIds ->
            String json = JsonOutput.toJson(userIds)

            Notification notification = new Notification(
                    requestedOn: notificationRequest.requestedOn,
                    userId: json,
                    failedCount: 0,
                    encodedParams: serParams,
                    type: notificationRequest.type,
            )

            notificationsRepo.save(notification)
        }
        // save as semi-colon delimited when sending bcc batch?
        List<String> batch = []
        notificationRequest.userIds.each { String userId ->
            batch.add(userId)
            if (batch.size() == maxRecipients) {
                saveNotification(new ArrayList(batch))
                batch.clear()
            }
        }

        if (batch.size() > 0) {
            saveNotification(new ArrayList(batch))
            batch.clear()
        }

    }

    @Transactional
    void dispatchNotifications() {
        log.debug("Checking notifications to dispatch.")
        doDispatchNotifications { notificationsRepo.streamNewNotifications() }
    }

    @Transactional
    void attemptToDispatchErroredNotifications() {
        log.debug("Checking for errored notifications.")
        doDispatchNotifications("Retry: ") { notificationsRepo.streamFailedNotifications() }
    }

    private SettingsInit getEmailConfig() {
        List<SettingsResult> emailSettings = settingsService.getGlobalSettingsByGroup(EmailSettingsService.settingsGroup)
        JavaMailSender senderForBatch = emailSettingsService.getMailSender(emailSettingsService.convert(emailSettings))
        String fromEmail = systemSettingsService.get()?.fromEmail

        Formatting formatting = new Formatting(
                htmlHeader: emailSettings.find {it.setting == EmailSettingsService.htmlHeader }?.value ?: null,
                plaintextHeader: emailSettings.find { it.setting == EmailSettingsService.plaintextHeader }?.value ?: null,
                htmlFooter: emailSettings.find { it.setting == EmailSettingsService.htmlFooter }?.value ?: null,
                plaintextFooter: emailSettings.find { it.setting == EmailSettingsService.plaintextFooter }?.value ?:null
        )

        return new SettingsInit(formatting: formatting, fromEmail: fromEmail, mailSender: senderForBatch)
    }

    private void doDispatchNotifications(String prependToLogs = "", Closure<Stream<Notification>> streamCreator) {
        lockingService.lockForNotifying()

        StopWatch stopWatch = new StopWatch()
        stopWatch.start()

        int count = 0
        int errCount = 0
        String lastErrMsg

        streamCreator.call().withCloseable { Stream<Notification> notifications ->

            JavaMailSender senderForBatch = null
            String fromEmail = null
            Formatting formatting = null
            JsonSlurper slurper = new JsonSlurper()

            notifications.forEach({ Notification notification ->

                if (!senderForBatch) {
                    SettingsInit init = getEmailConfig()
                    senderForBatch = init.mailSender
                    fromEmail = init.fromEmail
                    formatting = init.formatting
                }

                def userIds = null
                try {
                    userIds = slurper.parseText(notification.userId)
                } catch (JsonException ex) {
                    log.warn("user id field [${notification.userId}] was not in the expected json format, this is expected for any notifications that existed prior to 1.6")
                    userIds = [notification.userId]
                }

                NotificationEmailBuilder.Res emailRes = notificationEmailBuilderManager.build(notification, formatting)
                assert notification.userId?.size() > 0

                List<String> failedUserIds = []

                userIds.each {
                    String email = userAttrs.findEmailByUserId(it)
                    if (email) {
                        boolean failed = false;
                        try {
                            sendingService.sendEmail(emailRes.subject, email, emailRes.html, emailRes.plainText, notification.requestedOn, senderForBatch, fromEmail)
                        } catch (Throwable t) {
                            // don't print the same message over and over again
                            if (!lastErrMsg?.equalsIgnoreCase(t.message)) {
                                log.error("${prependToLogs}Failed to sent notification with id [${notification.id}] and type [${notification.type}]. Updating notification to retry", t)
                                lastErrMsg = t.message
                            }
                            failed = true;
                        }
                        if (!failed) {
                            count++
                        } else {
                            failedUserIds.add(it)
                            errCount++
                        }
                    } else {
                        log.warn("unable to send notification to recipient [${it}], no email address found")
                    }
                }

                if (!failedUserIds) {
                    removeNotificationImmediately(notification.id)
                } else {
                    notification.failedCount = notification.failedCount + 1
                    //only some failed. Update the notification to only include the failed ids
                    notification.userId = JsonOutput.toJson(failedUserIds)
                    boolean removed = removeIfOlderThanConfiguredRetainPeriod(notification)
                    if (!removed) {
                        notificationsRepo.save(notification)
                    }
                }
            })
        }

        stopWatch.stop()
        if (count > 0 || errCount > 0) {
            int seconds = stopWatch.getTime(TimeUnit.SECONDS)
            log.info("${prependToLogs}Dispatched [${count}] notification(s) with [${errCount}] error(s) in [${seconds}] seconds")
        }
    }

    private boolean removeIfOlderThanConfiguredRetainPeriod(Notification notification) {
        boolean removed = false
        use(TimeCategory) {
            Date keepAfterDate = retainFailedNotificationsForNumSecs.seconds.ago
            if (notification.created.before(keepAfterDate)) {
                removeNotificationImmediately(notification.id)
                log.error("Removed notification because it failed and exceeded max retention of [${retainFailedNotificationsForNumSecs}] seconds! Notificaiton=[{}]", notification)
                removed = true
            }
        }

        return removed;
    }

    private void removeNotificationImmediately(Integer id) {
        assert id
        notificationsRepo.deleteById(id)
        notificationsRepo.flush()
    }

    private static class SettingsInit {
        Formatting formatting
        String fromEmail
        JavaMailSender mailSender
    }



}
