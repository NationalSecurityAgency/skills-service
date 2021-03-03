package skills.notify

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import skills.notify.builders.NotificationEmailBuilder
import skills.storage.model.Notification

@Component
class ForTestNotificationBuilder implements  NotificationEmailBuilder{

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

}
