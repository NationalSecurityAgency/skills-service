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

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAttrs
import skills.storage.repos.UserRepo
import skills.utils.WaitFor

class ContactOwnersSpec extends DefaultIntSpec {

    @Autowired
    UserRepo userRepo

    def setup() {
        startEmailServer()
    }

    def "can contact project administrators"() {
        def users = getRandomUsers(4, true)

        Map<String, SkillsService.UseParams> params = [:]
        users.each {
            SkillsService.UseParams up = new SkillsService.UseParams(
                    username: it,
                    email: EmailUtils.generateEmaillAddressFor(it),
                    firstName: "${it.toUpperCase()}_first",
                    lastName: "${it.toUpperCase()}_last"
            )
            params[it] = up
        }
        SkillsService proj1Serv = createService(params[users[0]])

        def proj1 = SkillsFactory.createProject(1)
        def subj1 = SkillsFactory.createSubject()
        def skill = SkillsFactory.createSkill()
        skill.pointIncrement = 200

        proj1Serv.createProject(proj1)
        proj1Serv.createSubject(subj1)
        proj1Serv.createSkill(skill)

        //make sure the other users get created so that they can be added as admins
        createService(params[users[1]])
        createService(params[users[2]])

        proj1Serv.addSkill(skill, users[1])
        proj1Serv.addSkill(skill, users[2])

        proj1Serv.addUserRole(users[1], proj1.projectId, "ROLE_PROJECT_ADMIN")
        proj1Serv.addUserRole(users[2], proj1.projectId, "ROLE_PROJECT_ADMIN")
        WaitFor.wait { greenMail.getReceivedMessages().length > 1 }
        greenMail.getReceivedMessages()
        greenMail.reset()

        def userService = createService(params[users[3]])

        when:
        userService.contactProjectOwner(proj1.projectId, "a message")
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(users[3])
        def displayName = userAttrs.getUserIdForDisplay()

        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)

        then:
        email.recipients.size() == 3
        email.recipients.find { it.contains(users[1]) }
        email.recipients.find { it.contains(users[2]) }
        email.recipients.find { it.contains(users[0]) }
        email.fromEmail.find { it.contains(users[3]) }
        email.plainText.contains("You have received the following question from user ${displayName} about SkillTree Project Test Project#1:")
        email.plainText.contains("a message")
        email.html.contains("You have received the following question from user ${displayName} about SkillTree Project Test Project#1:")
        email.html.contains("a message")
        email.subj == 'User Question regarding Test Project#1'
    }

    def "when configured headers and footer is included in the  contact project administrators email"() {
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.saveEmailHeaderAndFooterSettings(
                '<p>Header attention {{ community.descriptor }} Members</p>',
                '<p>Footer attention {{ community.descriptor }} Members</p>',
                'Plain Text Header Attention {{ community.descriptor }} Members',
                'Plain Text Footer Attention {{ community.descriptor }} Members')

        def users = getRandomUsers(4, true)

        Map<String, SkillsService.UseParams> params = [:]
        users.each {
            SkillsService.UseParams up = new SkillsService.UseParams(
                    username: it,
                    email: EmailUtils.generateEmaillAddressFor(it),
                    firstName: "${it.toUpperCase()}_first",
                    lastName: "${it.toUpperCase()}_last"
            )
            params[it] = up
        }
        SkillsService proj1Serv = createService(params[users[0]])

        def proj1 = SkillsFactory.createProject(1)
        def subj1 = SkillsFactory.createSubject()
        def skill = SkillsFactory.createSkill()
        skill.pointIncrement = 200

        proj1Serv.createProject(proj1)
        proj1Serv.createSubject(subj1)
        proj1Serv.createSkill(skill)

        //make sure the other users get created so that they can be added as admins
        createService(params[users[1]])
        createService(params[users[2]])

        proj1Serv.addSkill(skill, users[1])
        proj1Serv.addSkill(skill, users[2])

        proj1Serv.addUserRole(users[1], proj1.projectId, "ROLE_PROJECT_ADMIN")
        proj1Serv.addUserRole(users[2], proj1.projectId, "ROLE_PROJECT_ADMIN")
        WaitFor.wait { greenMail.getReceivedMessages().length > 1 }
        greenMail.getReceivedMessages()
        greenMail.reset()

        def userService = createService(params[users[3]])

        when:
        userService.contactProjectOwner(proj1.projectId, "a message")
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(users[3])
        def displayName = userAttrs.getUserIdForDisplay()

        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)

        then:
        email.recipients.size() == 3
        email.recipients.find { it.contains(users[1]) }
        email.recipients.find { it.contains(users[2]) }
        email.recipients.find { it.contains(users[0]) }
        email.fromEmail.find { it.contains(users[3]) }
        email.plainText.contains("You have received the following question from user ${displayName} about SkillTree Project Test Project#1:")
        email.plainText.contains("a message")
        email.html.contains("You have received the following question from user ${displayName} about SkillTree Project Test Project#1:")
        email.html.contains("a message")
        email.subj == 'User Question regarding Test Project#1'

        email.plainText.startsWith("Plain Text Header Attention All Dragons Members")
        email.plainText.endsWith("Plain Text Footer Attention All Dragons Members")

        email.html.contains("<body class=\"overall-container\">\r\n<p>Header attention All Dragons Members</p>\r\n<p>You have received the following question")
        email.html.contains("<p>Footer attention All Dragons Members</p>\r\n</body>")
    }

    def "contact project administrators enforces configured custom validation"() {
        def users = getRandomUsers(4, true)

        Map<String, SkillsService.UseParams> params = [:]
        users.each {
            SkillsService.UseParams up = new SkillsService.UseParams(
                    username: it,
                    email: EmailUtils.generateEmaillAddressFor(it),
                    firstName: "${it.toUpperCase()}_first",
                    lastName: "${it.toUpperCase()}_last"
            )
            params[it] = up
        }
        SkillsService proj1Serv = createService(params[users[0]])

        def proj1 = SkillsFactory.createProject(1)
        def subj1 = SkillsFactory.createSubject()
        def skill = SkillsFactory.createSkill()
        skill.pointIncrement = 200

        proj1Serv.createProject(proj1)
        proj1Serv.createSubject(subj1)
        proj1Serv.createSkill(skill)

        //make sure the other users get created so that they can be added as admins
        createService(params[users[1]])
        createService(params[users[2]])

        proj1Serv.addSkill(skill, users[1])
        proj1Serv.addSkill(skill, users[2])

        proj1Serv.addUserRole(users[1], proj1.projectId, "ROLE_PROJECT_ADMIN")
        proj1Serv.addUserRole(users[2], proj1.projectId, "ROLE_PROJECT_ADMIN")
        WaitFor.wait { greenMail.getReceivedMessages().length > 1 }
        greenMail.getReceivedMessages()
        greenMail.reset()

        def userService = createService(params[users[3]])

        when:
        userService.contactProjectOwner(proj1.projectId, "a message that contains jabberwocky")

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("paragraphs may not contain jabberwocky")
    }

    def "contact project administrators includes user supplied newlines"() {
        def users = getRandomUsers(4, true)

        Map<String, SkillsService.UseParams> params = [:]
        users.each {
            SkillsService.UseParams up = new SkillsService.UseParams(
                    username: it,
                    email: EmailUtils.generateEmaillAddressFor(it),
                    firstName: "${it.toUpperCase()}_first",
                    lastName: "${it.toUpperCase()}_last"
            )
            params[it] = up
        }
        SkillsService proj1Serv = createService(params[users[0]])

        def proj1 = SkillsFactory.createProject(1)
        def subj1 = SkillsFactory.createSubject()
        def skill = SkillsFactory.createSkill()
        skill.pointIncrement = 200

        proj1Serv.createProject(proj1)
        proj1Serv.createSubject(subj1)
        proj1Serv.createSkill(skill)

        //make sure the other users get created so that they can be added as admins
        createService(params[users[1]])
        createService(params[users[2]])

        proj1Serv.addSkill(skill, users[1])
        proj1Serv.addSkill(skill, users[2])

        proj1Serv.addUserRole(users[1], proj1.projectId, "ROLE_PROJECT_ADMIN")
        proj1Serv.addUserRole(users[2], proj1.projectId, "ROLE_PROJECT_ADMIN")
        WaitFor.wait { greenMail.getReceivedMessages().length > 1 }
        greenMail.getReceivedMessages()
        greenMail.reset()

        def userService = createService(params[users[3]])

        when:
        userService.contactProjectOwner(proj1.projectId, "a message\n\nthat has\n\nmultiple\n\nnewlines")
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(users[3])
        def displayName = userAttrs.getUserIdForDisplay()

        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)

        then:
        email.recipients.size() == 3
        email.recipients.find { it.contains(users[1]) }
        email.recipients.find { it.contains(users[2]) }
        email.recipients.find { it.contains(users[0]) }
        email.fromEmail.find { it.contains(users[3]) }
        email.html.contains("You have received the following question from user ${displayName} about SkillTree Project Test Project#1:")
        email.html.contains("<p>a message</p>")
        email.html.contains("<p>that has</p>")
        email.html.contains("<p>multiple</p>")
        email.html.contains("<p>newlines</p>")
        email.subj == 'User Question regarding Test Project#1'
    }

}
