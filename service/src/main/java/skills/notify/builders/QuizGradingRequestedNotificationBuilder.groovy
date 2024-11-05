/**
 * Copyright 2024 SkillTree
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
class QuizGradingRequestedNotificationBuilder implements NotificationEmailBuilder{

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Override
    String getId() {
        return Notification.Type.QuizGradingRequested.toString()
    }

    @Override
    Res build(Notification notification, Formatting formatting) {
        def parsed = jsonSlurper.parseText(notification.encodedParams)
        Context context = buildThymeleafContext(parsed, formatting)
        String htmlBody = thymeleafTemplateEngine.process("quiz_grading_request.html", context)
        String plainText = buildPlainText(parsed, formatting)
        return new Res(
                subject: "SkillTree Quiz Grading Requested",
                html: htmlBody,
                plainText: plainText,
                replyToEmail: parsed.replyTo,
        )
    }
    private static Context buildThymeleafContext(Object parsed, Formatting formatting) {
        Context templateContext = new Context()
        templateContext.setVariable("userRequesting", parsed.userRequesting)
        templateContext.setVariable("quizId", parsed.quizId)
        templateContext.setVariable("gradingUrl", parsed.gradingUrl)
        templateContext.setVariable("publicUrl", parsed.publicUrl)
        templateContext.setVariable("htmlHeader", formatting.htmlHeader)
        templateContext.setVariable("htmlFooter", formatting.htmlFooter)

        return templateContext
    }

    private static String buildPlainText(Object parsed, Formatting formatting) {
        String pt = ">SkillTree Quiz Grading Request!\n\n" +
                "User [[${parsed.userRequesting}]] has completed the [${parsed.quizName}] quiz which requires manual grading. As a quiz administrator, please review the submitted answers and evaluate them as passed or failed.\n\n" +
                "Grading URL: ${parsed.gradingUrl}" +
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
