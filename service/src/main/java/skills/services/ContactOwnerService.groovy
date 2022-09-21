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
package skills.services

import groovy.util.logging.Slf4j
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.exceptions.SkillsValidator
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.services.admin.ProjAdminService
import skills.storage.model.Notification

@Slf4j
@Component
class ContactOwnerService {

    private Parser parser = Parser.builder().build()
    private HtmlRenderer renderer = HtmlRenderer.builder().build()

    @Autowired
    EmailNotifier emailNotifier

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    UserInfoService userInfoService

    @Autowired
    FeatureService featureService

    @Autowired
    ProjAdminService projAdminService

    @Transactional
    def contactProjectOwner(String projectId, String msg) {
        SkillsValidator.isTrue(featureService.isEmailServiceFeatureEnabled(), "Email has not been configured for this instance of SkillTree");

        //msg comes from currently authenticated user
        UserInfo currentUser = userInfoService.getCurrentUser()
        String displayName = currentUser.getUsernameForDisplay()
        String email = currentUser.getEmail()
        String projectName = projAdminService.lookupProjectName(projectId)

        List<String> projectAdmins = accessSettingsStorageService.getProjectAdminIds(projectId)
        assert projectAdmins, "a project should never have zero admins"

        def markdown = parser.parse(msg)
        String parsedBody = renderer.render(markdown)

        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                userIds: projectAdmins,
                type: Notification.Type.ContactOwner,
                keyValParams: [
                        body    : parsedBody,
                        emailSubject: "User Question regarding ${projectName}",
                        rawBody     : msg,
                        userDisplay: displayName,
                        projectName: projectName,
                        replyTo: email,
                ]
        )

        emailNotifier.sendNotification(request)
    }
}
