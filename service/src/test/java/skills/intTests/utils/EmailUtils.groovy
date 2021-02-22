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
        return msgs.collect { convert(it)}
    }

    static EmailRes getEmail(GreenMail greenMail, int msgNum=0) {
        MimeMessage msg = greenMail.getReceivedMessages()[msgNum]
        return convert(msg)
    }

    static EmailRes convert(MimeMessage msg) {
        EmailRes emailRes = new EmailRes()
        emailRes.subj = "SkillTree Points Requested"
        emailRes.recipients = msg.getAllRecipients().collect { it.toString() }

        MimeMultipart multipart = msg.getContent().getBodyPart(0).getContent().getBodyPart(0).getContent()
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i)
            if (bodyPart.isMimeType("text/html")) {
                emailRes.html = GreenMailUtil.getBody(bodyPart)
            } else if(bodyPart.isMimeType("text/plain")) {
                emailRes.plainText = GreenMailUtil.getBody(bodyPart)
            }
        }

        return emailRes
    }
}
