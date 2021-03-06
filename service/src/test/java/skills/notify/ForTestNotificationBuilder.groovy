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
