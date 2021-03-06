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
import skills.notify.builders.NotificationEmailBuilder
import skills.storage.model.Notification

@Component
class ForTestNotificationBuilder implements NotificationEmailBuilder {

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Override
    String getId() {
        return "ForTestNotificationBuilder"
    }

    @Override
    Res build(Notification notification) {
        def parsed = jsonSlurper.parseText(notification.encodedParams)
        Context context = buildThymeleafContext(parsed)
        String htmlBody = thymeleafTemplateEngine.process("test-email-template.html", context)
        String plainText = buildPlainText()
        return new Res(
                subject: "Test Subject",
                html: htmlBody,
                plainText: plainText
        )
    }

    private Context buildThymeleafContext(parsed) {
        Context templateContext = new Context()
        templateContext.setVariable("simpleParam", parsed.simpleParam)
        return templateContext
    }

    private String buildPlainText() {
        return "As plain as day"
    }

    static class Request implements Serializable{
        String userId
        Integer numRequests
    }

    @Override
    Map<String, Object> buildDigestParams(List<Notification> notifications) {
        List parsed =  notifications.collect { jsonSlurper.parseText(it.encodedParams) }
        Map<String, List<Object>> byUser = parsed.groupBy { it.userRequesting }

        return [
                numUsersRequestedPoints: notifications.size(),
                numRequestsForPoints: notifications.size(),
                approveUrl: "http://localhost/approve",
                requests: byUser.collect { new Request(userId: it.key, numRequests: it.value.size() )}
        ]
    }

    @Override
    String buildDigestPlainText(List<Notification> notifications) {
        return null
    }

}
