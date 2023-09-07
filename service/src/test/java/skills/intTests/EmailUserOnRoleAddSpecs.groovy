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
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName
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
        proj1Serv.addUserRole(users[1], proj1.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())

        when:
        WaitFor.wait { greenMail.getReceivedMessages().size() > 0 }
        def messages = EmailUtils.getEmails(greenMail)

        then:
        messages.size() > 0
        def message = messages.find {it.recipients.find {it.contains(users[1])}}
        message.subj == "SkillTree - You've been added as an admin"
        message.plainText.contains("Congratulations!  You've just been added as a Project Administrator for the SkillTree project [Test Project#1](http://localhost:${localPort}/administrator/projects/TestProject1).")
        message.fromEmail[0] == defaultFromEmail
    }

    def "from email is properly updated when processing a batch of emails"() {
        def users = getRandomUsers(3, true)
        SkillsService proj1Serv = createService(users[0])

        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(1,)
        skills[0].pointIncrement = 200
        skills[0].selfReportingType = SkillDef.SelfReportingType.Approval

        proj1Serv.createProject(proj)
        proj1Serv.createSubject(subj)
        proj1Serv.createSkills(skills)

        createService(users[1])

        SkillsService reportSkillUser = createService(new SkillsService.UseParams(username: users[2], email: "report@email.com"))
        String reportUserEmail = userAttrsRepo.findByUserIdIgnoreCase(users[2].toLowerCase()).email
        assert reportUserEmail.contains("@")

        // report back to back to get in the same notification batch
        reportSkillUser.addSkill([projectId: proj.projectId, skillId: skills[0].skillId])
        proj1Serv.addUserRole(users[1], proj.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())

        when:
        WaitFor.wait { greenMail.getReceivedMessages().size() > 1 }
        def messages = EmailUtils.getEmails(greenMail)

        def newAdminMessage = messages.find { it.subj?.contains("added as an admin")}
        def pointsRequested = messages.find { it.subj?.contains("Points Requested")}
        then:
        messages.size() > 1
        pointsRequested.fromEmail[0] == reportUserEmail
        newAdminMessage.fromEmail[0] == defaultFromEmail
    }


    def "contact user when added as approver"() {
        def users = getRandomUsers(2, true)
        SkillsService proj1Serv = createService(users[0])

        def proj1 = SkillsFactory.createProject(1)

        proj1Serv.createProject(proj1)
        createService(users[1])
        proj1Serv.addUserRole(users[1], proj1.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        WaitFor.wait { greenMail.getReceivedMessages().size() > 0 }
        def messages = EmailUtils.getEmails(greenMail)

        then:
        messages.size() > 0
        def message = messages.find {it.recipients.find {it.contains(users[1])}}
        message.subj == "SkillTree - You've been added as an approver"
        message.plainText.contains("Congratulations!  You've just been added as a Project Approver for the SkillTree project [Test Project#1](http://localhost:${localPort}/administrator/projects/TestProject1).")
    }

    def "contact user when added as root"() {
        def users = getRandomUsers(2, true)
        SkillsService proj1Serv = createService(users[0])
        def proj1 = SkillsFactory.createProject(2)

        proj1Serv.createProject(proj1)
        createService(users[1])
        rootServiceOne.addRootRole(users[1])

        when:
        WaitFor.wait { greenMail.getReceivedMessages().size() > 0 }
        def messages = EmailUtils.getEmails(greenMail)

        then:
        messages.size() > 0
        def message = messages.find {it.recipients.find {it.contains(users[1])}}
        message.subj == "SkillTree - You've been added as root"
        message.plainText.contains("Congratulations! You've been just added as a Root Administrator for the [SkillTree Dashboard](http://localhost:${localPort}/administrator).")

    }
}
