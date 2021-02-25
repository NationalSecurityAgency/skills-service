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

import org.thymeleaf.context.Context
import skills.notify.Notifier
import skills.storage.repos.SkillEventsSupportRepo

class ApprovalNotificationRequestBuilder {

    // inject
    String userRequesting
    String requestMsg
    SkillEventsSupportRepo.SkillDefMin skillDef
    List<String> userIds
    String publicUrl
    String projectName


    // private
    String approveUrl
    Notifier.NotificationRequest build() {
        assert userRequesting
        assert requestMsg
        assert skillDef
        assert userIds

        approveUrl = "${publicUrl}administrator/projects/${skillDef.projectId}/self-report"

        new Notifier.NotificationRequest(
                userIds: userIds,
                subject: "SkillTree Points Requested",
                plainTextBody: buildBody(),
                thymeleafTemplate: "skill_approval_request.html",
                thymeleafTemplateContext: getThymeleafContext(),
        )
    }

    private Context getThymeleafContext() {
        Context templateContext = new Context()
        templateContext.setVariable("userRequesting", userRequesting)
        templateContext.setVariable("numPoints", numPoints())
        templateContext.setVariable("skillName", skillDef.name)
        templateContext.setVariable("approveUrl", approveUrl)
        templateContext.setVariable("skillId", skillDef.skillId)
        templateContext.setVariable("requestMsg", requestMsg)
        templateContext.setVariable("projectId", skillDef.projectId)
        templateContext.setVariable("publicUrl", publicUrl)
        templateContext.setVariable("projectName", projectName)

        return templateContext
    }

    private String buildBody() {
        return "User ${userRequesting} requested points.\n" +
                "\n   Approval URL: ${approveUrl}" +
                "\n   User Requested: ${userRequesting}" +
                "\n   Project: ${projectName}" +
                "\n   Skill: ${skillDef.name} (${skillDef.skillId})" +
                "\n   Number of Points: ${numPoints()}" +
                "\n   Request Message: ${requestMsg}" +
                        "\n" +
                        "\nAs an approver for the '${skillDef.projectId}' project, you can approve or reject this request." +
                        "\n\n" +
                        "\nAlways yours," +
                        "\nSkillTree Bot"

    }

    private String numPoints() {
        return String.format("%,d", skillDef.pointIncrement)
    }

}
