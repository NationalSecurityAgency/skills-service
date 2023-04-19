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

import groovy.util.logging.Slf4j
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.request.model.ContactUsersRequest
import skills.controller.request.model.QueryUsersCriteriaRequest
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.storage.model.Notification
import skills.storage.model.QueryUsersCriteria
import skills.storage.model.SubjectLevelCriteria
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.repos.nativeSql.PostgresQlNativeRepo
import skills.utils.Props
import org.commonmark.parser.Parser

import java.util.stream.Stream

@Slf4j
@Service
class ContactUsersService {

    @Autowired
    PostgresQlNativeRepo PostgresQlNativeRepo

    @Autowired
    EmailNotifier emailNotifier

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Value('#{"${skills.config.notifications.maxRecipients:50}"}')
    int batchSize

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserAttrsService userAttrsService

    @Transactional(readOnly = true)
    Long countAllProjectAdminsWithEmail() {
        return accessSettingsStorageService.countUserIdsWithRoleAndEmail(RoleName.ROLE_PROJECT_ADMIN)
    }

    @Transactional
    contactAllProjectAdmins(String emailSubject, String emailBody) {
        List<String> batch = []
        Closure sendNotifications = { List<String> userIds ->
            Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                    userIds: new ArrayList(batch),
                    type: Notification.Type.ContactUsers,
                    keyValParams: [
                            htmlBody    : emailBody,
                            emailSubject: emailSubject,
                            rawBody     : emailBody,
                    ]
            )
            emailNotifier.sendNotification(request)
            batch.clear()
        }
        getAllProjectAdminsWithEmail().withCloseable { Stream<String> userIds ->
            userIds.forEach {
                batch.add(it)
                if (batch.size() == batchSize) {
                    sendNotifications(new ArrayList(batch))
                    batch.clear()
                }
            }
        }

        if (batch.size() > 0) {
            sendNotifications(new ArrayList(batch))
        }
    }

    @Transactional
    void sendEmail(String emailSubject, String emailBodyAsMarkdown, String userId) {
        Parser parser = Parser.builder().build()
        HtmlRenderer renderer = HtmlRenderer.builder().build()
        def markdown = parser.parse(emailBodyAsMarkdown)
        String parsedBody = renderer.render(markdown)

        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                userIds: [userId],
                type: Notification.Type.ContactUsers,
                keyValParams: [
                        htmlBody    : parsedBody,
                        emailSubject: emailSubject,
                        rawBody     : emailBodyAsMarkdown,
                ]
        )
        emailNotifier.sendNotification(request)
    }

    @Transactional(readOnly = true)
    Integer countMatchingUsers(QueryUsersCriteriaRequest contactUsersCriteria) {
        QueryUsersCriteria queryUsersCriteria = convert(contactUsersCriteria)
        return PostgresQlNativeRepo.countUsers(queryUsersCriteria)
    }

    @Transactional(readOnly = true)
    Stream<String> retrieveMatchingUserIds(QueryUsersCriteriaRequest queryUsersCriteriaRequest) {
        QueryUsersCriteria queryUsersCriteria = convert(queryUsersCriteriaRequest)
        return PostgresQlNativeRepo.getUserIds(queryUsersCriteria)
    }

    @Transactional
    void contactUsers(ContactUsersRequest contactUsersRequest) {
        UserInfo currentUser = userInfoService.getCurrentUser()
        String sender = currentUser.getEmail()

        retrieveMatchingUserIds(contactUsersRequest.queryCriteria).withCloseable { Stream<String> userIds ->
            Closure sendNotification = { List<String> batch ->
                Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                        userIds: batch,
                        type: Notification.Type.ContactUsers,
                        keyValParams: [
                                projectId   : contactUsersRequest.queryCriteria.projectId,
                                htmlBody    : contactUsersRequest.emailBody,
                                emailSubject: contactUsersRequest.emailSubject,
                                rawBody     : contactUsersRequest.emailBody,
                                replyTo     : sender,
                        ]
                )
                emailNotifier.sendNotification(request)
            }

            List<String> batch = []
            userIds.forEach({
                batch.add(it)
                if (batch.size() == 150) {
                    sendNotification(new ArrayList(batch))
                    batch.clear()
                }
            })

            if (batch.size() > 0) {
                sendNotification(new ArrayList(batch))
                batch.clear()
            }
        }
    }

    private Stream<String> getAllProjectAdminsWithEmail() {
        return accessSettingsStorageService.getUsersIdsWithRoleAndEmail(RoleName.ROLE_PROJECT_ADMIN)
    }

    private List<String> getProjectAdminEmails(String projectId) {
        List<String> adminEmails = []
        List<String> projectAdminUserIds = accessSettingsStorageService.getProjectAdminIds(projectId)
        projectAdminUserIds?.each {
            UserAttrs attrs = userAttrsService.findByUserId(it)
            String email = attrs.getEmail()
            if (email) {
                adminEmails.add(email)
            }
        }

        return adminEmails
    }

    private QueryUsersCriteria convert(QueryUsersCriteriaRequest request) {
        QueryUsersCriteria queryUsersCriteria = new QueryUsersCriteria()
        Props.copy(request, queryUsersCriteria, "subjectLevels")
        request.subjectLevels?.each {
            SubjectLevelCriteria subjectLevelCriteria = new SubjectLevelCriteria()
            Props.copy(it, subjectLevelCriteria)
            queryUsersCriteria.subjectLevels.add(subjectLevelCriteria)
        }
        queryUsersCriteria.achievedSkillIds?.unique(true)
        queryUsersCriteria.badgeIds?.unique(true)
        queryUsersCriteria.notAchievedSkillIds?.unique(true)
        return queryUsersCriteria
    }

}
