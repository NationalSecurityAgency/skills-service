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
        proj1Serv.addUserRole(users[1], proj1.projectId, "ROLE_PROJECT_ADMIN")

        when:
        WaitFor.wait { greenMail.getReceivedMessages().size() >= 2 }
        def messages = EmailUtils.getEmails(greenMail)

        then:
        messages.size() > 0
        def message = messages.find {it.recipients.find {it.contains(users[1])}}
        message.subj == "SkillTree - You've been added as an admin"
        message.plainText == "You've been added as an admin on the project Test Project#1"
    }

    def "contact user when added as root"() {
        def users = getRandomUsers(2, true)
        SkillsService proj1Serv = createService(users[0])
        def proj1 = SkillsFactory.createProject(2)

        proj1Serv.createProject(proj1)
        createService(users[1])
        rootServiceOne.addRootRole(users[1])

        when:
        WaitFor.wait { greenMail.getReceivedMessages().size() >= 3 }
        def messages = EmailUtils.getEmails(greenMail)

        then:
        messages.size() > 0
        def message = messages.find {it.recipients.find {it.contains(users[1])}}
        message.subj == "SkillTree - You've been added as root"
        message.plainText == "You've been added as a root user to SkillTree"
    }
}
