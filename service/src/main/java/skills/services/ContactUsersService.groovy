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
import org.apache.commons.lang3.StringUtils
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.request.model.ContactUsersRequest
import skills.controller.request.model.QueryUsersCriteriaRequest
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.storage.model.Notification
import skills.storage.model.QueryUsersCriteria
import skills.storage.model.SubjectLevelCriteria
import skills.storage.model.auth.RoleName
import skills.storage.repos.nativeSql.NativeQueriesRepo
import skills.utils.Props
import org.commonmark.parser.Parser

import java.util.stream.Stream

@Slf4j
@Service
class ContactUsersService {

    private Parser parser = Parser.builder().build()
    private HtmlRenderer renderer = HtmlRenderer.builder().build()

    @Autowired
    NativeQueriesRepo nativeQueriesRepo

    @Autowired
    EmailNotifier emailNotifier

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Transactional(readOnly = true)
    Long countAllProjectAdminsWithEmail() {
        getAllProjectAdminsEmail()?.size()
    }

    @Transactional
    contactAllProjectAdmins(String emailSubject, String emailBody) {
        List<String> projectAdminEmails = getAllProjectAdminsEmail()
        Parser parser = Parser.builder().build()
        HtmlRenderer renderer = HtmlRenderer.builder().build()
        def markdown = parser.parse(emailBody)
        String parsedBody = renderer.render(markdown)

        projectAdminEmails.each {
            Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                    userIds: [it],
                    type: Notification.Type.ContactUsers,
                    keyValParams: [
                            htmlBody    : parsedBody,
                            emailSubject: emailSubject,
                            rawBody     : emailBody,
                    ]
            )
            emailNotifier.sendNotification(request)
        }
    }

    @Transactional(readOnly = true)
    Integer countMatchingUsers(QueryUsersCriteriaRequest contactUsersCriteria) {
        QueryUsersCriteria queryUsersCriteria = convert(contactUsersCriteria)
        return nativeQueriesRepo.countUsers(queryUsersCriteria)
    }

    @Transactional(readOnly = true)
    Stream<String> retrieveMatchingUserIds(QueryUsersCriteriaRequest queryUsersCriteriaRequest) {
        QueryUsersCriteria queryUsersCriteria = convert(queryUsersCriteriaRequest)
        return nativeQueriesRepo.getUserIds(queryUsersCriteria)
    }

    @Transactional
    void contactUsers(ContactUsersRequest contactUsersRequest) {

        retrieveMatchingUserIds(contactUsersRequest.queryCriteria).withCloseable { Stream<String> userIds ->
            def markdown = parser.parse(contactUsersRequest.emailBody)
            String parsedBody = renderer.render(markdown)
            userIds.forEach({
                Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                        userIds: [it],
                        type: Notification.Type.ContactUsers,
                        keyValParams: [
                                projectId   : contactUsersRequest.queryCriteria.projectId,
                                htmlBody    : parsedBody,
                                emailSubject: contactUsersRequest.emailSubject,
                                rawBody     : contactUsersRequest.emailBody,
                        ]
                )
                emailNotifier.sendNotification(request)
            })
        }
    }

    private List<String> getAllProjectAdminsEmail() {
        accessSettingsStorageService.getUserRolesWithRole(RoleName.ROLE_PROJECT_ADMIN)?.findAll {
            StringUtils.isNotEmpty(it.email)
        }?.collect {
            it.email
        }?.unique()
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
