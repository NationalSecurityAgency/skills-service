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
import org.thymeleaf.spring6.SpringTemplateEngine
import skills.storage.model.Notification

@Component
class PasswordResetNotificationBuilder implements NotificationEmailBuilder {

    private static final String PASSWORD_RESET_TEMPLATE = "password_reset.html"

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Override
    String getId() {
        Notification.Type.PasswordReset.toString()
    }

    @Override
    Res build(Notification notification, Formatting formatParams) {
        def parsed = jsonSlurper.parseText(notification.encodedParams)
        Context context = buildThymeleafContext(parsed, formatParams)
        try {
            String htmlBody = thymeleafTemplateEngine.process(PASSWORD_RESET_TEMPLATE, context)
            String plainText = buildPlainText(parsed, formatParams)
            return new Res(
                    subject: "SkillTree Password Reset",
                    html: htmlBody,
                    plainText: plainText,
                    userIdsAreEmailAdresses: true,
            )
        } catch(Exception e) {
            e.printStackTrace()
            throw e
        }
    }

    private Context buildThymeleafContext(Object parsed, Formatting formatting) {
        Context templateContext = new Context()
        templateContext.setVariable("recipientName", parsed.recipientName)
        templateContext.setVariable("publicUrl", parsed.publicUrl)
        templateContext.setVariable("email", parsed.email)
        templateContext.setVariable("validTime", parsed.validTime)
        templateContext.setVariable("senderName", parsed.senderName)
        templateContext.setVariable("token", parsed.token)
        templateContext.setVariable("htmlHeader", formatting.htmlHeader)
        templateContext.setVariable("htmlFooter", formatting.htmlFooter)

        return templateContext
    }

    private String buildPlainText(Object parsed, Formatting formatting) {

        String pt = """
Hi ${parsed.recipientName},
                
You have requested a password reset. Please use the link below to reset your password. The link will be valid for ${parsed.validTime}.

${parsed.publicUrl}reset-password/${parsed.token}

Regards,
${parsed.senderName} at SkillTree
"""

        if (formatting.plaintextHeader) {
            pt = "${formatting.plaintextHeader}\n${pt}"
        }
        if (formatting.plaintextFooter) {
            pt = "${pt}\n${formatting.plaintextFooter}"
        }

        return pt

    }
}
