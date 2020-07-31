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
package skills.intTests

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import org.apache.commons.lang3.time.DurationFormatUtils
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsService

import javax.mail.internet.MimeMessage
import java.time.Duration

class PasswordResetSpec extends DefaultIntSpec {

    GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP)

    SkillsService rootSkillsService


    def setup() {
        greenMail.start()

        rootSkillsService = createService("rootUser", 'aaaaaaaa')
        if (!rootSkillsService.isRoot()) {
            rootSkillsService.grantRoot()
        }

        rootSkillsService.getWsHelper().rootPost("/saveEmailSettings", [
                "host"       : "localhost",
                "port"       : ServerSetupTest.SMTP.port,
                "protocol"   : "smtp",
                "authEnabled": false,
                "tlsEnabled" : false
        ])
        rootSkillsService.addOrUpdateGlobalSetting("public_url",
                ["setting": "public_url", "value": "http://localhost:${localPort}/".toString()])

        rootSkillsService.addOrUpdateGlobalSetting("from_email",
                ["setting": "from_email", "value": "resetspec@skilltreetests".toString()])
    }

    def cleanup(){
        greenMail.stop()
    }

    def "password reset request sends email"() {
        SkillsService aUser = createService("randomuser@skills.org", "somepassword",)

        when:
        //post request with an unauthenticated client to ensure that the url is publicly available
        RestTemplate template = new RestTemplate()
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.MULTIPART_FORM_DATA)
        MultiValueMap body = new LinkedMultiValueMap<>()
        body.add("userId", "randomuser@skills.org")
        HttpEntity entity = new HttpEntity(body, headers)

        String url = "http://localhost:${localPort}/resetPassword"

        template.postForEntity(url, entity, String.class)

        then:
        greenMail.getReceivedMessages().length == 1
        MimeMessage msg = greenMail.getReceivedMessages()[0]
        msg.getAllRecipients()[0].toString() == "randomuser@skills.org"
        msg.getSubject() == "SkillTree Password Reset"
        msg.getContent().toString().contains('href="http://localhost:' + localPort + '/reset-password/')
        msg.getFrom()[0].toString() == "resetspec@skilltreetests"
    }

    def "reset password with token from email"() {
        SkillsService aUser = createService("randomuser@skills.org", "somepassword")
        //post request with an unauthenticated client to ensure that the url is publicly available
        RestTemplate template = new RestTemplate()
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.MULTIPART_FORM_DATA)
        MultiValueMap body = new LinkedMultiValueMap<>()
        body.add("userId", "randomuser@skills.org")
        HttpEntity entity = new HttpEntity(body, headers)
        String url = "http://localhost:${localPort}/resetPassword"
        template.postForEntity(url, entity, String.class)

        MimeMessage msg = greenMail.getReceivedMessages()[0]

        def match = msg.content.toString() =~ /href=".*\/reset-password\/([^"]+)"/
        String token = match[0][1]

        when:

        url = "http://localhost:${localPort}/performPasswordReset"
        def reset = ["userId": "randomuser@skills.org", "password": "newpassword", "resetToken": token]
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        entity = new HttpEntity(reset, headers)
        template.postForEntity(url, entity, String.class)
        //we expect this to fail as it is using the old password
        createService("randomuser@skills.org", "somepassword")

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("401 UNAUTHORIZED")
        //we expect this to succeed as it uses the password specified in the reset
        createService("randomuser@skills.org", "newpassword")
    }

    def "reset password with invalid token fails"() {
        SkillsService aUser = createService("randomuser@skills.org", "somepassword")
        //post request with an unauthenticated client to ensure that the url is publicly available
        RestTemplate template = new RestTemplate()
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.MULTIPART_FORM_DATA)
        MultiValueMap body = new LinkedMultiValueMap<>()
        body.add("userId", "randomuser@skills.org")
        HttpEntity entity = new HttpEntity(body, headers)
        String url = "http://localhost:${localPort}/resetPassword"
        template.postForEntity(url, entity, String.class)

        MimeMessage msg = greenMail.getReceivedMessages()[0]
        String token = "fake"

        when:

        url = "http://localhost:${localPort}/performPasswordReset"
        def reset = ["userId": "randomuser@skills.org", "password": "newpassword", "resetToken": token]
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        entity = new HttpEntity(reset, headers)
        template.postForEntity(url, entity, String.class)

        then:
        thrown(HttpClientErrorException)
    }

    def "reset with expired token fails"(){
        rootSkillsService.addOrUpdateGlobalSetting("password_reset_token_expiration",
                ["setting": "password_reset_token_expiration", "value": "PT0.001S"])

        SkillsService aUser = createService("randomuser@skills.org", "somepassword")
        //post request with an unauthenticated client to ensure that the url is publicly available
        RestTemplate template = new RestTemplate()
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.MULTIPART_FORM_DATA)
        MultiValueMap body = new LinkedMultiValueMap<>()
        body.add("userId", "randomuser@skills.org")
        HttpEntity entity = new HttpEntity(body, headers)
        String url = "http://localhost:${localPort}/resetPassword"
        template.postForEntity(url, entity, String.class)

        MimeMessage msg = greenMail.getReceivedMessages()[0]

        def match = msg.content.toString() =~ /href=".*\/reset-password\/([^"]+)"/
        String token = match[0][1]
        Thread.currentThread().sleep(500) //wait for token to become invalid

        when:
        url = "http://localhost:${localPort}/performPasswordReset"
        def reset = ["userId": "randomuser@skills.org", "password": "newpassword", "resetToken": token]
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        entity = new HttpEntity(reset, headers)
        template.postForEntity(url, entity, String.class)

        then:
        thrown(HttpClientErrorException)
    }

}
