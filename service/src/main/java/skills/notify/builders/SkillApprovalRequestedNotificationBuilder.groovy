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
package skills.notify.builders

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import skills.storage.model.Notification

@Component
class SkillApprovalRequestedNotificationBuilder implements NotificationEmailBuilder{

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Override
    String getId() {
        return Notification.Type.SkillApprovalRequested.toString()
    }

    @Override
    Res build(Notification notification, Formatting formatting) {
        def parsed = jsonSlurper.parseText(notification.encodedParams)
        Context context = buildThymeleafContext(parsed, formatting)
        String htmlBody = thymeleafTemplateEngine.process("skill_approval_request.html", context)
        String plainText = buildPlainText(parsed, formatting)
        return new Res(
                subject: "SkillTree Points Requested",
                html: htmlBody,
                plainText: plainText
        )
    }

    private Context buildThymeleafContext(Object parsed, Formatting formatting) {
        Context templateContext = new Context()
        templateContext.setVariable("userRequesting", parsed.userRequesting)
        templateContext.setVariable("numPoints", String.format("%,d", parsed.numPoints))
        templateContext.setVariable("skillName", parsed.skillName)
        templateContext.setVariable("approveUrl", parsed.approveUrl)
        templateContext.setVariable("skillId", parsed.skillId)
        templateContext.setVariable("requestMsg", parsed.requestMsg)
        templateContext.setVariable("projectId", parsed.projectId)
        templateContext.setVariable("publicUrl", parsed.publicUrl)
        templateContext.setVariable("projectName", parsed.projectName)
        templateContext.setVariable("htmlHeader", formatting.htmlHeader)
        templateContext.setVariable("htmlFooter", formatting.htmlFooter)

        return templateContext
    }

    private String buildPlainText(Object parsed, Formatting formatting) {
        String pt = "User ${parsed.userRequesting} requested points.\n" +
                "\n   Approval URL: ${parsed.approveUrl}" +
                "\n   User Requested: ${parsed.userRequesting}" +
                "\n   Project: ${parsed.projectName}" +
                "\n   Skill: ${parsed.skillName} (${parsed.skillId})" +
                "\n   Number of Points: ${String.format("%,d", parsed.numPoints)}" +
                "\n   Request Message: ${parsed.requestMsg}" +
                "\n" +
                "\nAs an approver for the '${parsed.projectId}' project, you can approve or reject this request." +
                "\n\n" +
                "\nAlways yours," +
                "\nSkillTree Bot"

        if (formatting.plaintextHeader) {
            pt = "${formatting.plaintextHeader}\n${pt}"
        }
        if (formatting.plaintextFooter) {
            pt = "${pt}\n${formatting.plaintextFooter}"
        }

        return pt

    }

}
