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
package skills.notify.builders

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import skills.storage.model.Notification

@Component
class ProjectExpirationNotificationBuilder implements NotificationEmailBuilder {

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Override
    String getId() {
        Notification.Type.ProjectExpiration.toString()
    }

    @Override
    Res build(Notification notification, Formatting formatParams) {
        def parsed = jsonSlurper.parseText(notification.encodedParams)
        Context context = buildThymeleafContext(parsed, formatParams)
        try {
            String htmlBody = thymeleafTemplateEngine.process("project_expiration.html", context)
            String plainText = buildPlainText(parsed, formatParams)
            return new Res(
                    subject: "SkillTree Project is expiring!",
                    html: htmlBody,
                    plainText: plainText
            )
        } catch(Exception e) {
            e.printStackTrace()
            throw e
        }
    }

    private Context buildThymeleafContext(Object parsed, Formatting formatting) {
        Context templateContext = new Context()
        // projectId, projectName, expiring in days, ... and urls?
        templateContext.setVariable("projectId", parsed.projectId)
        templateContext.setVariable("publicUrl", parsed.publicUrl)
        templateContext.setVariable("projectName", parsed.projectName)
        templateContext.setVariable("expiringIn", parsed.expiringIn)
        templateContext.setVariable("expiresOn", parsed.expiresOn);
        templateContext.setVariable("createdOn", parsed.createdOn);
        templateContext.setVariable("unusedProjectExpirationInDays", parsed.unusedProjectExpirationInDays);
        templateContext.setVariable("htmlHeader", formatting.htmlHeader)
        templateContext.setVariable("htmlFooter", formatting.htmlFooter)

        return templateContext
    }

    private String buildPlainText(Object parsed, Formatting formatting) {

        String pt = "Your SkillTree Project ${parsed.projectName}, created on ${parsed.createdOn} hasn't been used in at least ${parsed.unusedProjectExpirationInDays} days.\n" +
                "\nIf you take no action, Project ${parsed.projectName} will be deleted in ${parsed.expiringIn} (${parsed.expiresOn})." +
                "\n" +
                "\nTo keep your Project, visit ${parsed.publicUrl}administrator/projects/${parsed.projectId} in the SkillTree dashboard and click the 'Keep' button." +
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
