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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.utils.WaitFor

class EmailUserOnRoleAddSpecs extends DefaultIntSpec {
    SkillsService rootServiceOne

    def setup() {
        startEmailServer()
        rootServiceOne = createRootSkillService()
    }

    def "contact user when added as admin"() {
        def users = getRandomUsers(2, true)
        SkillsService proj1Serv = createService(users[0])

        def proj1 = SkillsFactory.createProject(1)

        proj1Serv.createProject(proj1)
        createService(users[1])
        greenMail.purgeEmailFromAllMailboxes()
        proj1Serv.addUserRole(users[1], proj1.projectId, "ROLE_PROJECT_ADMIN")

        when:
        def count = rootServiceOne.countAllProjectAdminsWithEmail()
        WaitFor.wait { greenMail.getReceivedMessages().size() >= 1 }
        def messages = EmailUtils.getEmails(greenMail)

        then:
        count == 3
        messages.size() > 0
        messages.find { it.recipients.find {it.contains(users[1])}}
        messages[1].subj == "Test email"
        messages[1].plainText == "Test email"
    }

    def "contact user when added as root"() {
        def users = getRandomUsers(2, true)
        SkillsService proj1Serv = createService(users[0])

        def proj1 = SkillsFactory.createProject(1)

        proj1Serv.createProject(proj1)
        createService(users[1])
        greenMail.purgeEmailFromAllMailboxes()
        rootServiceOne.addRootRole(users[1])

        when:
        def count = rootServiceOne.countAllProjectAdminsWithEmail()
        WaitFor.wait { greenMail.getReceivedMessages().size() >= 1 }
        def messages = EmailUtils.getEmails(greenMail)

        then:
        count == 3
        messages.size() > 0
        messages.find { it.recipients.find {it.contains(users[1])}}
        messages[0].subj == "Test email"
        messages[0].plainText == "Test email"
    }
}
