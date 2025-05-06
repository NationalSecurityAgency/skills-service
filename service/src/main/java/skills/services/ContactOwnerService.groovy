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
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.UIConfigProperties
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.exceptions.SkillsValidator
import skills.notify.EmailNotifier
import skills.notify.Notifier
import skills.services.admin.ProjAdminService
import skills.services.admin.UserCommunityService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.Notification
import skills.storage.model.ProjDef

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

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    UIConfigProperties uiConfigProperties

    @Value('#{"${skills.config.ui.docsHost}"}')
    String docsRootHost = ""

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

        Boolean isUcProject = userCommunityService.isUserCommunityOnlyProject(projectId)
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
                        communityHeaderDescriptor : isUcProject ? uiConfigProperties.ui.userCommunityRestrictedDescriptor : uiConfigProperties.ui.defaultCommunityDescriptor
                ]
        )

        emailNotifier.sendNotification(request)
    }

    @Transactional
    def sendInviteRequest(String projectId) {
        SkillsValidator.isTrue(featureService.isEmailServiceFeatureEnabled(), "Email has not been configured for this instance of SkillTree");

        String publicUrl = featureService.getPublicUrl()
        if(!publicUrl) {
            return
        }

        UserInfo currentUser = userInfoService.getCurrentUser()
        String displayName = currentUser.getUsernameForDisplay()
        String email = currentUser.getEmail()
        ProjDef projDef = projDefAccessor.getProjDef(projectId)

        List<String> projectAdmins = accessSettingsStorageService.getProjectAdminIds(projectId)
        assert projectAdmins, "a project should never have zero admins"

        String docHostNormalized = docsRootHost.endsWith("/") ? docsRootHost : "${docsRootHost}/"
        Notifier.NotificationRequest request = new Notifier.NotificationRequest(
                userIds: projectAdmins,
                type: Notification.Type.NewInviteRequest,
                keyValParams: [
                        emailSubject: "User Question regarding ${projDef.name}",
                        userDisplay: displayName,
                        projectName: projDef.name,
                        replyTo: email,
                        accessPageUrl: "${publicUrl}administrator/projects/${projDef.projectId}/access",
                        inviteOnlyDocsUrl: "${docHostNormalized}dashboard/user-guide/projects.html#invite-only",
                        communityHeaderDescriptor : uiConfigProperties.ui.defaultCommunityDescriptor
                ]
        )

        emailNotifier.sendNotification(request)
    }

}
