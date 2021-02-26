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
package skills.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import skills.settings.EmailSettingsService

import javax.mail.internet.MimeMessage

@Slf4j
@Component
class EmailSendingService {

    private static final String FROM = "no_reply@skilltree"

    @Autowired
    EmailSettingsService emailSettings

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

    @Autowired
    SystemSettingsService systemSettingsService

    void sendEmail(String subject, String to, String htmlBody, String plainTextBody = null) {
        sendEmail(subject, [to], htmlBody, plainTextBody)
    }

    void sendEmail(String subject, List<String> to, String htmlBody, String plainTextBody = null) {

        String fromEmail = systemSettingsService.get()?.fromEmail
        if (!fromEmail) {
            fromEmail = FROM
        }

        MimeMessage message = emailSettings.mailSender.createMimeMessage()
        MimeMessageHelper helper = plainTextBody ?
                new MimeMessageHelper(message, true) :
                new MimeMessageHelper(message, "UTF-8")
        helper.setSubject(subject)
        String [] toArr = to.toArray()
        helper.setTo(toArr)
        helper.setFrom(fromEmail)
        helper.setReplyTo(fromEmail)
        if (plainTextBody) {
            helper.setText(plainTextBody, htmlBody)
        } else {
            helper.setText(htmlBody, true)
        }
        emailSettings.mailSender.send(message)
    }

    void sendEmailWithThymeleafTemplate(String subject, String to, String templateFileName, Context thymeleafContext, String plainTextBodyAlt = null) {
        sendEmailWithThymeleafTemplate(subject, [to], templateFileName, thymeleafContext, plainTextBodyAlt)
    }

    void sendEmailWithThymeleafTemplate(String subject, List<String> to, String templateFileName, Context thymeleafContext, String plainTextBodyAlt = null) {
        String htmlBody = thymeleafTemplateEngine.process(templateFileName, thymeleafContext)
        sendEmail(subject, to, htmlBody, plainTextBodyAlt)
    }
}
