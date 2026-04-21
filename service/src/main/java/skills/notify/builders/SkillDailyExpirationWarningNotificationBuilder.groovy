/**
 * Copyright 2026 SkillTree
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
class SkillDailyExpirationWarningNotificationBuilder implements NotificationEmailBuilder {

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Override
    String getId() {
        Notification.Type.SkillDailyExpirationWarning.toString()
    }

    @Override
    Res build(Notification notification, Formatting formatParams) {
        def parsed = jsonSlurper.parseText(notification.encodedParams)
        Context context = buildThymeleafContext(parsed, formatParams)
        try {
            String htmlBody = thymeleafTemplateEngine.process("skill_daily_expiration_warning.html", context)
            String plainText = buildPlainText(parsed, formatParams)
            return new Res(
                    subject: "Skill Achievement Expiring Soon",
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
        templateContext.setVariable("userName", parsed.userName)
        templateContext.setVariable("skillName", parsed.skillName)
        templateContext.setVariable("projectName", parsed.projectName)
        templateContext.setVariable("retentionDeadline", parsed.retentionDeadline)
        templateContext.setVariable("daysUntilExpiration", parsed.daysUntilExpiration)
        templateContext.setVariable("skillTrainingUrl", parsed.skillTrainingUrl)
        templateContext.setVariable("htmlHeader", formatting.htmlHeader)
        templateContext.setVariable("htmlFooter", formatting.htmlFooter)

        return templateContext
    }

    private String buildPlainText(Object parsed, Formatting formatting) {

        String pt = "Hello ${parsed.userName},\n" +
                "\nThis is a reminder that your achievement for the skill ${parsed.skillName} in project ${parsed.projectName} is approaching its expiration deadline." +
                "\n" +
                "\nSkill Details:" +
                "\n- Skill: ${parsed.skillName}" +
                "\n- Project: ${parsed.projectName}" +
                "\n- Retention Deadline: ${parsed.retentionDeadline}" +
                "\n- Days Until Expiration: ${parsed.daysUntilExpiration}" +
                "\n" +
                "\nHow to Keep Your Achievement:" +
                "\nYou need to perform the skill activity again before the deadline to retain your achievement and points. If you don't take action by the deadline, you will lose the achievement and associated points, but you'll be able to re-achieve the skill afterward." +
                "\n" +
                "\nTake Action Now:" +
                "\n${parsed.skillTrainingUrl}" +
                "\n" +
                "\nWhat Happens If You Miss the Deadline?" +
                "\nIf you don't perform the skill by ${parsed.retentionDeadline}, your achievement will expire and you'll lose the points. However, you can always re-achieve the skill by completing the required activities again." +
                "\n" +
                "\nNeed Help?" +
                "\nEach training is managed by dedicated administrators who have specialized knowledge of that training. To reach them, click the 'Contact' button in the top right of the training page title." +
                "\n\n" +
                "\nBest regards," +
                "\nThe SkillTree Team"

        if (formatting.plaintextHeader) {
            pt = "${formatting.plaintextHeader}\n${pt}"
        }
        if (formatting.plaintextFooter) {
            pt = "${pt}\n${formatting.plaintextFooter}"
        }

        return pt

    }
}
