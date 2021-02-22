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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.EmailSendingService
import skills.services.settings.SettingsService
import skills.storage.model.UserAttrs
import skills.storage.repos.UserAttrsRepo

@Component
@Slf4j
class EmailNotifier implements Notifier{

    @Autowired
    EmailSendingService emailSettings

    @Autowired
    UserAttrsRepo userAttrs

    @Autowired
    SettingsService settingsService

    void sendNotification(Notifier.NotificationRequest notificationRequest) {
        List<String> emails = notificationRequest.userIds.each {
            UserAttrs userAttrs = userAttrs.findByUserId(it)
            return userAttrs.email
        }

        emailSettings.sendEmailWithThymeleafTemplate(notificationRequest.subject, emails,
                notificationRequest.thymeleafTemplate, notificationRequest.thymeleafTemplateContext, notificationRequest.plainTextBody)
    }


}
