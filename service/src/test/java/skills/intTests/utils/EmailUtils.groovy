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
package skills.intTests.utils

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.GreenMailUtil

import javax.mail.BodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class EmailUtils {

    static class EmailRes {
        String subj
        List<String> recipients

        String html
        String plainText

    }

    static List<EmailRes> getEmails(GreenMail greenMail) {
        MimeMessage[] msgs = greenMail.getReceivedMessages()
        return msgs.collect { convert(it) }
    }

    static EmailRes getEmail(GreenMail greenMail, int msgNum = 0) {
        MimeMessage msg = greenMail.getReceivedMessages()[msgNum]
        return convert(msg)
    }

    static EmailRes convert(MimeMessage msg) {
        EmailRes emailRes = new EmailRes()
        emailRes.subj = msg.subject
        emailRes.recipients = msg.getAllRecipients().collect { it.toString() }

        MimeMultipart multipart = msg.getContent().getBodyPart(0).getContent().getBodyPart(0).getContent()
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i)
            if (bodyPart.isMimeType("text/html")) {
                emailRes.html = GreenMailUtil.getBody(bodyPart)
            } else if (bodyPart.isMimeType("text/plain")) {
                emailRes.plainText = GreenMailUtil.getBody(bodyPart)
            }
        }

        return emailRes
    }

    static String prepBodyForComparison(String body, Integer localPort = null) {
        String res = body
        if (localPort != null) {
            res = res.toString().replaceAll("\\{\\{port\\}\\}", localPort.toString())
        }
        res = res.replaceAll("[\r\n]", "")
        return res
    }
}
