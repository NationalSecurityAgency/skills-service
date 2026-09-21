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
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.NoOpResponseErrorHandler
import org.springframework.web.client.RestTemplate
import skills.SpringBootApp
import skills.intTests.utils.*
import skills.storage.model.auth.UserToken
import skills.storage.repos.PasswordResetTokenRepo
import skills.utils.WaitFor
import spock.lang.IgnoreIf
import spock.lang.Specification

import java.util.regex.Matcher

@Slf4j
@SpringBootTest(properties = ['skills.authorization.verifyEmailAddresses=true'],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class EmailVerificationIT extends Specification {

    @Autowired
    DataResetHelper dataResetHelper

    @Autowired
    SkillsServiceFactory skillsServiceFactory

    @Autowired
    WaitForAsyncTasksCompletion waitForAsyncTasksCompletion

    @LocalServerPort
    int localPort

    @Autowired
    JdbcTemplate jdbcTemplate

    @Autowired
    PasswordResetTokenRepo tokenRepo

    GreenMail greenMail

    def setup() {
        dataResetHelper.resetData()
        greenMail = new GreenMail(ServerSetupTest.SMTP)
        greenMail.start()

        List<String> sqlStatements = [
                "INSERT INTO public.settings (setting, value, type, project_id, setting_group, user_ref_id) VALUES ('email.host', 'localhost', 'Global', null, 'GLOBAL.EMAIL', null);",
                "INSERT INTO public.settings (setting, value, type, project_id, setting_group, user_ref_id) VALUES ('email.port', '${ServerSetupTest.SMTP.port}', 'Global', null, 'GLOBAL.EMAIL', null);",
                "INSERT INTO public.settings (setting, value, type, project_id, setting_group, user_ref_id) VALUES ('email.protocol', 'smtp', 'Global', null, 'GLOBAL.EMAIL', null);",
                "INSERT INTO public.settings (setting, value, type, project_id, setting_group, user_ref_id) VALUES ('email.auth', 'false', 'Global', null, 'GLOBAL.EMAIL', null);",
                "INSERT INTO public.settings (setting, value, type, project_id, setting_group, user_ref_id) VALUES ('email.tls.enable', 'false', 'Global', null, 'GLOBAL.EMAIL', null);",
                "INSERT INTO public.settings (setting, value, type, project_id, setting_group, user_ref_id) VALUES ('email.fromEmail', 'resetspec@skilltreetests', 'Global', null, 'GLOBAL.EMAIL', null);",
                "INSERT INTO public.settings (setting, value, type, project_id, setting_group, user_ref_id) VALUES ('email.publicUrl', 'http://localhost:${localPort}/', 'Global', null, 'GLOBAL.EMAIL', null);",
        ]

        sqlStatements.each {
            jdbcTemplate.execute(it)
        }
    }

    def cleanup() {
        if (greenMail) {
            log.info('Stopping email service')
            greenMail.stop()
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "verify email address is sent"() {
        skillsServiceFactory.createService(
                "first@user.org",
                'aaaaaaaa',
                "Skills",
                "Test",
                "http://localhost:${localPort}".toString())

        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        when:
        def emailRes = EmailUtils.getEmail(greenMail)
        then:
        greenMail.getReceivedMessages().length == 1
        emailRes.recipients == ["first@user.org"]
        emailRes.subj == "Please verify your email address"
        emailRes.html.contains("<p>Hi Skills Test,</p>")
        emailRes.html.contains("<p>We're happy you created a SkillTree account! Please use the link below to confirm your email address so you can start exploring SkillTree. The link will be valid for 2 hours.</p>")

        emailRes.plainText.contains("Hi Skills Test,")
        emailRes.plainText.contains("We're happy you created a SkillTree account! Please use the link below to confirm your email address so you can start exploring SkillTree.")
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "resend verification does not disclose whether the user exists"() {
        SkillsService existingUser = skillsServiceFactory.createService(
                "existing@user.org",
                'aaaaaaaa',
                "Skills",
                "Test",
                "http://localhost:${localPort}".toString())
        WaitFor.wait { greenMail.getReceivedMessages().length == 1 }
        greenMail.purgeEmailFromAllMailboxes()

        when:
        def existingResult = existingUser.wsHelper.rawPost("resendEmailVerification/existing@user.org", null)
        def missingResult = existingUser.wsHelper.rawPost("resendEmailVerification/missing@user.org", null)

        then:
        existingResult.success
        missingResult.success
        missingResult.body == existingResult.body
        WaitFor.wait { greenMail.getReceivedMessages().length == 1 }
        EmailUtils.getEmail(greenMail).recipients == ["existing@user.org"]
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "public account-state lookup endpoints are not exposed"() {
        RestTemplate restTemplate = new RestTemplate()
        restTemplate.errorHandler = new NoOpResponseErrorHandler()

        when:
        ResponseEntity<String> userExistsResponse = restTemplate.getForEntity(
                "http://localhost:${localPort}/userExists/test@skills.org".toString(), String)
        ResponseEntity<String> emailVerifiedResponse = restTemplate.getForEntity(
                "http://localhost:${localPort}/userEmailIsVerified/test@skills.org".toString(), String)

        then:
        userExistsResponse.statusCode == HttpStatus.NOT_FOUND
        emailVerifiedResponse.statusCode == HttpStatus.NOT_FOUND
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "when configured headers and footer is included in the verify email"() {
        def createUser = { String userName ->
            return skillsServiceFactory.createService(
                    userName,
                    'aaaaaaaa',
                    "Skills",
                    "Test",
                    "http://localhost:${localPort}".toString())
        }
        SkillsService firstUser = createUser('first@user.org')
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }
        def emailResOrig = EmailUtils.getEmail(greenMail)
        def (email, token) = getTokenFromEmail(emailResOrig.html)
        firstUser.verifyEmail(email, token)

        firstUser = createUser('first@user.org')
        firstUser.grantRoot()
        firstUser.saveEmailHeaderAndFooterSettings(
                '<p>Header attention {{ community.descriptor }} Members</p>',
                '<p>Footer attention {{ community.descriptor }} Members</p>',
                'Plain Text Header Attention {{ community.descriptor }} Members',
                'Plain Text Footer Attention {{ community.descriptor }} Members')

        greenMail.purgeEmailFromAllMailboxes()

        SkillsService secondUser = createUser('second@user.org')
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }
        when:
        def emailRes = EmailUtils.getEmail(greenMail)
        println emailRes.plainText
        println emailRes.html
        then:
        greenMail.getReceivedMessages().length == 1
        emailRes.recipients == [secondUser.userName]
        emailRes.subj == "Please verify your email address"
        emailRes.html.contains("<p>Hi Skills Test,</p>")
        emailRes.html.contains("<p>We're happy you created a SkillTree account! Please use the link below to confirm your email address so you can start exploring SkillTree. The link will be valid for 2 hours.</p>")

        emailRes.plainText.contains("Hi Skills Test,")
        emailRes.plainText.contains("We're happy you created a SkillTree account! Please use the link below to confirm your email address so you can start exploring SkillTree.")


        emailRes.plainText.startsWith("Plain Text Header Attention All Dragons Members")
        emailRes.plainText.endsWith("Plain Text Footer Attention All Dragons Members")

        emailRes.html.contains("<body>\r\n<p>Header attention All Dragons Members</p>\r\n<p>Hi Skills Test,</p>")
        emailRes.html.contains("<p>Footer attention All Dragons Members</p>\r\n</body>")
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "verify user to get permission to call endpoints"() {
        def createSameUser = {
            return skillsServiceFactory.createService(
                    "first@user.org",
                    'aaaaaaaa',
                    "Skills",
                    "Test",
                    "http://localhost:${localPort}".toString())
        }
        SkillsService firstUser = createSameUser()
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }
        def emailRes = EmailUtils.getEmail(greenMail)
        def (email, token) = getTokenFromEmail(emailRes.html)
        firstUser.verifyEmail(email, token)

        SkillsService stillFirstUser = createSameUser()
        when:
        def projects = stillFirstUser.getProjects()
        def userInfo = stillFirstUser.getCurrentUser()
        then:
        projects == []
        userInfo.first == "Skills"
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "email verification token cannot be used twice"() {
        SkillsService user = skillsServiceFactory.createService(
                "first@user.org",
                'aaaaaaaa',
                "Skills",
                "Test",
                "http://localhost:${localPort}".toString())
        WaitFor.wait { greenMail.getReceivedMessages().length == 1 }
        def (email, token) = getTokenFromEmail(EmailUtils.getEmail(greenMail).html)

        when:
        user.verifyEmail(email, token)
        def secondResult = user.wsHelper.rawPost('verifyEmail', [email: email, token: token], HttpStatus.BAD_REQUEST, false)

        then:
        secondResult.statusCode == HttpStatus.BAD_REQUEST
        secondResult.body.explanation == 'The supplied email verification token does not exist or is not for the specified user.'
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "expired email verification token can be replaced and the new token used"() {
        SkillsService user = skillsServiceFactory.createService(
                "first@user.org",
                'aaaaaaaa',
                "Skills",
                "Test",
                "http://localhost:${localPort}".toString())
        WaitFor.wait { greenMail.getReceivedMessages().length == 1 }
        def (email, expiredToken) = getTokenFromEmail(EmailUtils.getEmail(greenMail).html)
        UserToken storedToken = tokenRepo.findByToken(expiredToken)
        storedToken.expires = new Date() - 1
        tokenRepo.save(storedToken)
        greenMail.purgeEmailFromAllMailboxes()

        when:
        def expiredResult = user.wsHelper.rawPost('verifyEmail', [email: email, token: expiredToken], HttpStatus.BAD_REQUEST, false)

        then:
        expiredResult.statusCode == HttpStatus.BAD_REQUEST
        expiredResult.body.errorCode == 'UserTokenExpired'
        expiredResult.body.explanation == 'Your email verification code has expired.'

        when:
        def resendResult = user.wsHelper.rawPost("resendEmailVerification/${email}", null)
        WaitFor.wait { greenMail.getReceivedMessages().length == 1 }
        def (resentEmail, replacementToken) = getTokenFromEmail(EmailUtils.getEmail(greenMail).html)
        def verifyResult = user.wsHelper.rawPost('verifyEmail', [email: resentEmail, token: replacementToken])

        then:
        resendResult.success
        replacementToken != expiredToken
        verifyResult.success

        when:
        SkillsService verifiedUser = skillsServiceFactory.createService(
                "first@user.org",
                'aaaaaaaa',
                "Skills",
                "Test",
                "http://localhost:${localPort}".toString())

        then:
        verifiedUser.getProjects() == []
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "unverified user login does not disclose account state"() {
        skillsServiceFactory.createService(
                "unverified@user.org",
                'aaaaaaaa',
                "Skills",
                "Test",
                "http://localhost:${localPort}".toString())

        when:
        ResponseEntity<String> unverifiedResponse = login('unverified@user.org', 'aaaaaaaa')
        ResponseEntity<String> invalidCredentialsResponse = login('unverified@user.org', 'wrong-password')
        ResponseEntity<String> missingUserResponse = login('missing@user.org', 'aaaaaaaa')

        then:
        unverifiedResponse.statusCode == HttpStatus.UNAUTHORIZED
        invalidCredentialsResponse.statusCode == HttpStatus.UNAUTHORIZED
        missingUserResponse.statusCode == HttpStatus.UNAUTHORIZED
        unverifiedResponse.body == invalidCredentialsResponse.body
        unverifiedResponse.body == missingUserResponse.body
        unverifiedResponse.headers.location == invalidCredentialsResponse.headers.location
        unverifiedResponse.headers.location == missingUserResponse.headers.location
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "now allowed to execute endpoints if user not verified"() {
        def createUser = { String userName ->
            return skillsServiceFactory.createService(
                    userName,
                    'aaaaaaaa',
                    "Skills",
                    "Test",
                    "http://localhost:${localPort}".toString())
        }
        SkillsService firstUser = createUser('user1@email.org')
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }
        def emailRes = EmailUtils.getEmail(greenMail)
        def (email, token) = getTokenFromEmail(emailRes.html)
        firstUser.verifyEmail(email, token)

        when:
        SkillsService authorizedUser = createUser('user1@email.org')
        SkillsService notAuthorizedUser = createUser('user2@email.org')

        def callAndValidateNotAuthorized = { Closure c ->
            try {
                c.call()
                return false
            } catch (SkillsClientException t) {
                return t.httpStatus == HttpStatus.UNAUTHORIZED
            }
        }

        then:
        authorizedUser.getProjects() == []
        callAndValidateNotAuthorized { notAuthorizedUser.getProjects() }

        authorizedUser.getQuizDefs() == []
        callAndValidateNotAuthorized { notAuthorizedUser.getQuizDefs() }
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "now allowed to verify with another users token"() {
        def createUser = { String userName ->
            return skillsServiceFactory.createService(
                    userName,
                    'aaaaaaaa',
                    "Skills",
                    "Test",
                    "http://localhost:${localPort}".toString())
        }
        SkillsService firstUser = createUser('user1@email.org')
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }
        def emailRes1 = EmailUtils.getEmail(greenMail)
        def (email1, token1) = getTokenFromEmail(emailRes1.html)

        greenMail.purgeEmailFromAllMailboxes()
        SkillsService secondUser = createUser('user2@email.org')
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }
        def emailRes2 = EmailUtils.getEmail(greenMail)
        def (email2, token2) = getTokenFromEmail(emailRes2.html)

        when:
        firstUser.verifyEmail(email2, token1)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("The supplied email verification token does not exist or is not for the specified user")
    }


    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "must provide a valid token"() {
        def createUser = { String userName ->
            return skillsServiceFactory.createService(
                    userName,
                    'aaaaaaaa',
                    "Skills",
                    "Test",
                    "http://localhost:${localPort}".toString())
        }
        SkillsService firstUser = createUser('user1@email.org')
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }
        def emailRes1 = EmailUtils.getEmail(greenMail)
        def (email1, token1) = getTokenFromEmail(emailRes1.html)

        when:
        firstUser.verifyEmail(email1, "someFakeToken")

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("The supplied email verification token does not exist or is not for the specified user")
    }

    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "validate token is supplied"() {
        def createUser = { String userName ->
            return skillsServiceFactory.createService(
                    userName,
                    'aaaaaaaa',
                    "Skills",
                    "Test",
                    "http://localhost:${localPort}".toString())
        }
        SkillsService firstUser = createUser('user1@email.org')
        when:
        firstUser.verifyEmail("email", null)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("The supplied email verification token is blank")
    }


    @IgnoreIf({ env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "validate email is supplied"() {
        def createUser = { String userName ->
            return skillsServiceFactory.createService(
                    userName,
                    'aaaaaaaa',
                    "Skills",
                    "Test",
                    "http://localhost:${localPort}".toString())
        }
        SkillsService firstUser = createUser('user1@email.org')
        when:
        firstUser.verifyEmail(null, "token")

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("The supplied email is blank")
    }

    private static List<String> getTokenFromEmail(String email) {
        Matcher href = email =~ /href="([^"]+)"/
        String url = href[0][1]
        String userEmail = url.tokenize('/')[4]
        String token = url.tokenize('/')[3]
        return [userEmail, token]
    }

    private ResponseEntity<String> login(String username, String password) {
        HttpHeaders headers = new HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>()
        form.add('username', username)
        form.add('password', password)
        RestTemplate restTemplate = new RestTemplate()
        restTemplate.errorHandler = new NoOpResponseErrorHandler()
        restTemplate.postForEntity(
                "http://localhost:${localPort}/performLogin".toString(),
                new HttpEntity<>(form, headers),
                String)
    }



}
