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
                userIds: [rootSkillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))

        assert notificationsRepo.count() == 0
        rootSkillsService.addOrUpdateGlobalSetting("public_url",
                ["setting": "public_url", "value": "http://localhost:${localPort}/".toString()])

        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [rootSkillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))
        assert notificationsRepo.count() == 0

        rootSkillsService.addOrUpdateGlobalSetting("from_email",
                ["setting": "from_email", "value": "resetspec@skilltreetests".toString()])
        emailNotifier.sendNotification(new Notifier.NotificationRequest(
                userIds: [rootSkillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
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
                userIds: [rootSkillsService.userName],
                type: "ForTestNotificationBuilder",
                keyValParams: [simpleParam: 'param value']
        ))
        then:
        notificationsRepo.count() == 1
    }

}
