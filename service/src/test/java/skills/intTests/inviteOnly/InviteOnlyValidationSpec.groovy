/**
 * Copyright 2022 SkillTree
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
package skills.intTests.inviteOnly

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.InviteOnlyProjectService
import skills.utils.WaitFor
import spock.lang.IgnoreRest

class InviteOnlyValidationSpec extends InviteOnlyBaseSpec {

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    def setup() {
        inviteOnlyProjectService.validateInviteEmail = false
    }

    def "if email validation is enabled, validate returns false if requesting user is different from invited user"() {
        inviteOnlyProjectService.validateInviteEmail = true

        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(2, true)
        def userService = createService(users[0])

        when:
        def u1Email = EmailUtils.generateEmaillAddressFor(users[1])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: [u1Email]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        String inviteCode = extractInviteFromEmail(email.html)
        def resp = userService.validateInvite(proj.projectId, inviteCode)

        then:
        resp.projectId == proj.projectId
        resp.valid == false
        resp.userAlreadyHasProjectAccess == false
        resp.message == "Project Invite is for a different user"

        cleanup:
        inviteOnlyProjectService.validateInviteEmail = false
        greenMail.reset()
    }

    def "if email validation is enabled, validate returns true if requesting user is the same as invited user"() {
        inviteOnlyProjectService.validateInviteEmail = true

        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(2, true)

        when:
        def u0email = EmailUtils.generateEmaillAddressFor(users[0])
        SkillsService.UseParams params = new SkillsService.UseParams(
                username: users[0],
                email: u0email
        )
        def userService = createService(params)
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: [u0email]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        String inviteCode = extractInviteFromEmail(email.html)

        def resp = userService.validateInvite(proj.projectId, inviteCode)

        then:
        resp.projectId == proj.projectId
        resp.valid == true
        resp.userAlreadyHasProjectAccess == false

        cleanup:
        inviteOnlyProjectService.validateInviteEmail = false
        greenMail.reset()
    }

    def "email validation should be case insensitive"() {
        inviteOnlyProjectService.validateInviteEmail = true

        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(2, true)

        when:
        def u0email = EmailUtils.generateEmaillAddressFor(users[0])
        SkillsService.UseParams params = new SkillsService.UseParams(
                username: users[0],
                email: u0email
        )
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: [u0email.toUpperCase()]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        String inviteCode = extractInviteFromEmail(email.html)
        def userService = createService(params)
        def resp = userService.validateInvite(proj.projectId, inviteCode)

        then:
        resp.projectId == proj.projectId
        resp.valid == true
        resp.userAlreadyHasProjectAccess == false

        cleanup:
        inviteOnlyProjectService.validateInviteEmail = false
    }

    def "invite code validation returns false if invite code is expired"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(2, true)
        def userService = createService(users[0])

        when:
        def u1Email = EmailUtils.generateEmaillAddressFor(users[1])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT1S", recipients: [u1Email]])
        Thread.sleep(1200)
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        String inviteCode = extractInviteFromEmail(email.html)
        def resp = userService.validateInvite(proj.projectId, inviteCode)

        then:
        resp.projectId == proj.projectId
        resp.valid == false
        resp.userAlreadyHasProjectAccess == false
        resp.message == "Project Invite has expired"
    }

    def "invite code validation returns false if invite code is for a different project id"() {
        def proj = SkillsFactory.createProject(99)
        def proj2 = SkillsFactory.createProject(98)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        skillsService.changeSetting(proj2.projectId, "invite_only", [projectId: proj2.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(2, true)
        def userService = createService(users[0])

        when:
        def u1Email = EmailUtils.generateEmaillAddressFor(users[1])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT3M", recipients: [u1Email]])

        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }
        def email = EmailUtils.getEmail(greenMail, 0)
        String inviteCode = extractInviteFromEmail(email.html)
        def resp = userService.validateInvite(proj2.projectId, inviteCode)

        then:
        resp.projectId == proj2.projectId
        resp.valid == false
        resp.userAlreadyHasProjectAccess == false
        resp.message == "Invalid Project Invite"
    }

    def "invite code validation returns false if invite code does not exist"() {
        def proj = SkillsFactory.createProject(99)
        def proj2 = SkillsFactory.createProject(98)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createProject(proj2)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        skillsService.changeSetting(proj2.projectId, "invite_only", [projectId: proj2.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(2, true)
        def userService = createService(users[0])

        when:
        String inviteCode = "entirelyfake"
        def resp = userService.validateInvite(proj2.projectId, inviteCode)

        then:
        resp.projectId == proj2.projectId
        resp.valid == false
        resp.userAlreadyHasProjectAccess == false
        resp.message == "Invalid Project Invite"
    }

    def "invite code validation after invite was accepted"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(2, true)
        def otherUser = createService(users[0])

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        String invite = extractInviteFromEmail(email.html)

        otherUser.joinProject(proj.projectId, invite)

        when:
        def resp = otherUser.validateInvite(proj.projectId, invite)

        then:
        resp.projectId == proj.projectId
        resp.valid == false
        resp.userAlreadyHasProjectAccess == true
        resp.message == "User already has permission to this project"
    }
}
