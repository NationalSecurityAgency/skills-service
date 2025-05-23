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
import skills.utils.PatternsUtil

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

        List<String> batch = []
        notificationRequest.userIds.each { String userId ->
            batch.add(userId.toLowerCase())
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
        String fromEmail = emailSettings.find { it.setting == EmailSettingsService.fromEmail }?.value ?: null

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

        DispatchState dispatchState = new DispatchState(prepend: prependToLogs, wrappedLog: log)

        streamCreator.call().withCloseable { Stream<Notification> notifications ->

            SettingsInit init = getEmailConfig()
            JavaMailSender senderForBatch = init.mailSender
            Formatting formatting = init.formatting
            JsonSlurper slurper = new JsonSlurper()

            notifications.forEach({ Notification notification ->
                String fromEmail =  init.fromEmail

                def userIds = null
                try {
                    userIds = slurper.parseText(notification.userId)
                } catch (JsonException ex) {
                    log.warn("user id field [${notification.userId}] was not in the expected json format, this is expected for any notifications that existed prior to 1.6")
                    userIds = [notification.userId]
                }

                NotificationEmailBuilder.Res emailRes = notificationEmailBuilderManager.build(notification, formatting)
                assert notification.userId?.size() > 0

                if (PatternsUtil.isValidEmail(emailRes.replyToEmail)) {
                    fromEmail = emailRes.replyToEmail
                } else if (emailRes.replyToEmail) {
                    log.warn("NotificationBuilder produced a replyTo email that is not a valid email address [{}], using the default configured value of [{}]", emailRes.replyToEmail, fromEmail)
                }

                log.info("Sending notification: from [{}], to [{}], subject [{}]", fromEmail, userIds, emailRes.subject)
                log.debug("sending notification [{}] to [{}]", emailRes.html, userIds)
                List<String> failedUserIds = []

                if (!emailRes.singleEmailToAllRecipients) {
                    userIds.each {
                        String email = emailRes.userIdsAreEmailAdresses ? it : getEmail(it)
                        if (email) {
                            boolean failed = !dispatchState.doWithErrHandling(notification, {
                                sendingService.sendEmail(emailRes.subject, email, emailRes.html, emailRes.plainText, notification.requestedOn, senderForBatch, fromEmail, emailRes.ccRecipients)
                            })
                            if (failed) {
                                failedUserIds.add(it)
                            }
                        }
                    }
                } else {
                    List<String> emails = emailRes.userIdsAreEmailAdresses ? userIds : getEmails(userIds)
                    if (emails) {
                        boolean failed = !dispatchState.doWithErrHandling(notification, {
                            sendingService.sendEmail(emailRes.subject, emails, emailRes.html, emailRes.plainText, notification.requestedOn, senderForBatch, fromEmail, emailRes.ccRecipients)
                        })
                        if (failed) {
                            failedUserIds = userIds
                        }
                    }
                }

                if (!failedUserIds) {
                    removeNotificationImmediately(notification.id)
                } else {
                    log.info("failed to send notification to [{}]", failedUserIds)
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
        if (dispatchState.count > 0 || dispatchState.errCount > 0) {
            int seconds = stopWatch.getTime(TimeUnit.SECONDS)
            log.info("${prependToLogs}Dispatched [${dispatchState.count}] notification(s) with [${dispatchState.errCount}] error(s) in [${seconds}] seconds")
        }
    }

    private String getEmail(String userId) {
        String email = userAttrs.findEmailByUserId(userId)
        if (!email) {
            log.warn("unable to send notification to recipient [${userId}], no email address found")
        }
        return email
    }

    private List<String> getEmails(List<String> userIds) {
        List<String> emails = []
        userIds?.each {
            String email = getEmail(it)
            if (email) {
                emails.add(email)
            }
        }
        return emails
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
