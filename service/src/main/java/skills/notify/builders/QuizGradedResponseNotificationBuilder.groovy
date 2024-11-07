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
class QuizGradedResponseNotificationBuilder implements NotificationEmailBuilder{

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Override
    String getId() {
        return Notification.Type.QuizGradedResponse.toString()
    }

    @Override
    Res build(Notification notification, Formatting formatting) {
        def parsed = jsonSlurper.parseText(notification.encodedParams)
        Context context = buildThymeleafContext(parsed, formatting)
        String htmlBody = thymeleafTemplateEngine.process("quiz_graded_response.html", context)
        String plainText = buildPlainText(parsed, formatting)
        return new Res(
                subject: "SkillTree Quiz Graded",
                html: htmlBody,
                plainText: plainText,
                replyToEmail: parsed.replyTo,
        )
    }
    private static Context buildThymeleafContext(Object parsed, Formatting formatting) {
        Context templateContext = new Context()
        templateContext.setVariable("passed", parsed.passed)
        templateContext.setVariable("quizName", parsed.quizName)
        templateContext.setVariable("quizUrl", parsed.quizUrl)
        templateContext.setVariable("htmlHeader", formatting.htmlHeader)
        templateContext.setVariable("htmlFooter", formatting.htmlFooter)

        return templateContext
    }

    private static String buildPlainText(Object parsed, Formatting formatting) {
        String pt = "SkillTree Quiz Graded.\n\n"
        if (parsed.passed) {
            pt += "Congratulations, you passed the quiz [${parsed.quizName}]!"
        } else {
            pt += "Unfortunately, you failed the quiz [${parsed.quizName}]."
        }
        pt += "\n\nQuiz Url: ${parsed.quizUrl}" +
                "\n\nAlways yours," +
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
