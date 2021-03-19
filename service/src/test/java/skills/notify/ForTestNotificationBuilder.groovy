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
package skills.notify

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import skills.notify.builders.Formatting
import skills.notify.builders.SkillApprovalRequestedNotificationBuilder
import skills.storage.model.Notification

@Component
class ForTestNotificationBuilder extends SkillApprovalRequestedNotificationBuilder {

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Override
    String getId() {
        return "ForTestNotificationBuilder"
    }

    @Override
    Res build(Notification notification, Formatting formatting) {
        def parsed = jsonSlurper.parseText(notification.encodedParams)
        Context context = buildThymeleafContext(parsed, formatting)
        String htmlBody = thymeleafTemplateEngine.process("test-email-template.html", context)
        String plainText = buildPlainText(formatting)
        return new Res(
                subject: "Test Subject",
                html: htmlBody,
                plainText: plainText
        )
    }

    private Context buildThymeleafContext(parsed, Formatting formatting) {
        Context templateContext = new Context()
        templateContext.setVariable("simpleParam", parsed.simpleParam)
        templateContext.setVariable("htmlHeader", formatting.htmlHeader)
        templateContext.setVariable("htmlFooter", formatting.htmlFooter)
        return templateContext
    }

    private String buildPlainText(Formatting formatting) {
        String pt =  "As plain as day"
        if (formatting.plaintextHeader) {
            pt = "${formatting.plaintextHeader}\n${pt}"
        }
        if (formatting.plaintextFooter) {
            pt = "${pt}\n${formatting.plaintextFooter}"
        }

        return pt
    }

    static class Request implements Serializable{
        String userId
        Integer numRequests
    }

}
