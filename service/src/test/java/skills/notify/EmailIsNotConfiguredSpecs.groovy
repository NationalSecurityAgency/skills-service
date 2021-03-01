package skills.notify

import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.context.Context
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService
import skills.storage.repos.NotificationsRepo

class EmailIsNotConfiguredSpecs extends DefaultIntSpec {

    @Autowired
    EmailNotifier emailNotifier

    @Autowired
    NotificationsRepo notificationsRepo

    def "only send notification if all of the required properties are configured"() {
        SkillsService rootSkillsService = createRootSkillService()

        when:
        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                subject: "Test Subject",
                plainTextBody: "As plain as day",
                thymeleafTemplate: "test-email-template.html",
                thymeleafTemplateContext: new Context(Locale.ENGLISH, [simpleParam: 'param value'])
        ))
        assert notificationsRepo.count() == 0
        rootSkillsService.addOrUpdateGlobalSetting("public_url",
                ["setting": "public_url", "value": "http://localhost:${localPort}/".toString()])

        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                subject: "Test Subject",
                plainTextBody: "As plain as day",
                thymeleafTemplate: "test-email-template.html",
                thymeleafTemplateContext: new Context(Locale.ENGLISH, [simpleParam: 'param value'])
        ))
        assert notificationsRepo.count() == 0

        rootSkillsService.addOrUpdateGlobalSetting("from_email",
                ["setting": "from_email", "value": "resetspec@skilltreetests".toString()])

        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                subject: "Test Subject",
                plainTextBody: "As plain as day",
                thymeleafTemplate: "test-email-template.html",
                thymeleafTemplateContext: new Context(Locale.ENGLISH, [simpleParam: 'param value'])
        ))
        assert notificationsRepo.count() == 0

        rootSkillsService.getWsHelper().rootPost("/saveEmailSettings", [
                "host"       : "localhost",
                "port"       : 3923,
                "protocol"   : "smtp",
                "authEnabled": false,
                "tlsEnabled" : false
        ])
        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [skillsService.userName],
                subject: "Test Subject",
                plainTextBody: "As plain as day",
                thymeleafTemplate: "test-email-template.html",
                thymeleafTemplateContext: new Context(Locale.ENGLISH, [simpleParam: 'param value'])
        ))
        then:
        notificationsRepo.count() == 1
    }

}
