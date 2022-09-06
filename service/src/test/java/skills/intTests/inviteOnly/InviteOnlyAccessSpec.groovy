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
import org.springframework.http.HttpStatus
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.InviteOnlyProjectService
import skills.utils.WaitFor
import spock.lang.IgnoreRest

class InviteOnlyAccessSpec extends InviteOnlyBaseSpec {

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    def setup() {
        inviteOnlyProjectService.validateInviteEmail = false
    }

    def "cannot access a project that has been configured for invite only without accepting invite"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        when:

        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        newService.apiGetUserLevelForProject(proj.projectId, user)

        then:
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "cannot report a skill event for a project that has been configured for invite only without accepting invite"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)

        when:

        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        newService.addSkill([projectId: proj.projectId, skillId: skill.skillId])

        then:
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "can access invite only project after accepting invite"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        subj.description = "subj descrip"
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200
        def badge = SkillsFactory.createBadge(99, 1)
        badge.description = "badge descrip"

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skill.skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()[0]
        def invite = extractInviteFromEmail(email.content)

        newService.joinProject(proj.projectId, invite)

        def res = newService.apiGetUserLevelForProject(proj.projectId, null)
        def description = newService.getSubjectDescriptions(proj.projectId, subj.subjectId)
        def badgeDescription = newService.getBadgeDescriptions(proj.projectId, badge.badgeId, false)
        def summary = newService.getSkillsSummaryForCurrentUser(proj.projectId)

        then:
        res == 0
        description
        badgeDescription
        summary
    }

    def "can report a skill event for a project that has been configured for invite only without accepting invite"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()[0]
        def invite = extractInviteFromEmail(email.content)

        newService.joinProject(proj.projectId, invite)
        def res = newService.addSkill([projectId: proj.projectId, skillId: skill.skillId])

        then:
        res
        res.statusCode == HttpStatus.OK
        res.body.success == true
    }

    def "cannot access invite only project after access is revoked"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()[0]
        def invite = extractInviteFromEmail(email.content)

        newService.joinProject(proj.projectId, invite)

        def res = newService.apiGetUserLevelForProject(proj.projectId, null)
        skillsService.revokeInviteOnlyProjectAccess(proj.projectId, user)
        def res2 = newService.apiGetUserLevelForProject(proj.projectId, null)

        then:
        res == 0
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "cannot access invite only project subject descriptions after access is revoked"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        subj.description = "subj descrip"
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200
        def badge = SkillsFactory.createBadge(99, 1)
        badge.description = "badge descrip"

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skill.skillId])
        badge.enabled = true
        skillsService.createBadge(badge)


        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()[0]
        def invite = extractInviteFromEmail(email.content)

        newService.joinProject(proj.projectId, invite)

        def description = newService.getSubjectDescriptions(proj.projectId, subj.subjectId)
        skillsService.revokeInviteOnlyProjectAccess(proj.projectId, user)
        def description2 = newService.getSubjectDescriptions(proj.projectId, subj.subjectId)

        then:
        description
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "cannot access invite only project badge descriptions after access is revoked"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        subj.description = "subj descrip"
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200
        def badge = SkillsFactory.createBadge(99, 1)
        badge.description = "badge descrip"

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skill.skillId])
        badge.enabled = true
        skillsService.createBadge(badge)


        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()[0]
        def invite = extractInviteFromEmail(email.content)

        newService.joinProject(proj.projectId, invite)

        def badgeDescription = newService.getBadgeDescriptions(proj.projectId, badge.badgeId, false)
        skillsService.revokeInviteOnlyProjectAccess(proj.projectId, user)
        def badgeDescription2 = newService.getBadgeDescriptions(proj.projectId, badge.badgeId, false)

        then:
        badgeDescription
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "cannot access invite only project skill summary after access is revoked"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        subj.description = "subj descrip"
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200
        def badge = SkillsFactory.createBadge(99, 1)
        badge.description = "badge descrip"

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skill.skillId])
        badge.enabled = true
        skillsService.createBadge(badge)


        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()[0]
        def invite = extractInviteFromEmail(email.content)

        newService.joinProject(proj.projectId, invite)

        def summary = newService.getSkillsSummaryForCurrentUser(proj.projectId)
        skillsService.revokeInviteOnlyProjectAccess(proj.projectId, user)
        def summary2 = newService.getSkillsSummaryForCurrentUser(proj.projectId)

        then:
        summary
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "user with access accepting a second invite has no effect"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(1, true)
        def userService = createService(users[0])

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()
        String inviteCode = extractInviteFromEmail(email[0].content.toString())
        userService.joinProject(proj.projectId, inviteCode)
        greenMail.reset()

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        email = greenMail.getReceivedMessages()
        inviteCode = extractInviteFromEmail(email[0].content.toString())
        userService.joinProject(proj.projectId, inviteCode)

        when:
        def summary = userService.getMyProgressSummary()

        then:
        summary
    }

    def "user with access accepting a second invite does not interfere with revoking access"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def users = getRandomUsers(1, true)
        def userService = createService(users[0])

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()
        String inviteCode = extractInviteFromEmail(email[0].content.toString())
        userService.joinProject(proj.projectId, inviteCode)
        greenMail.reset()

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        email = greenMail.getReceivedMessages()
        inviteCode = extractInviteFromEmail(email[0].content.toString())
        userService.joinProject(proj.projectId, inviteCode)
        def summary = userService.getMyProgressSummary()

        when:
        skillsService.revokeInviteOnlyProjectAccess(proj.projectId, users[0])
        userService.apiGetUserLevelForProject(proj.projectId, null)

        then:
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "if email validation is enabled, cannot use invite code generated for another user"() {
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

        def email = greenMail.getReceivedMessages()
        String inviteCode = extractInviteFromEmail(email[0].content.toString())
        userService.joinProject(proj.projectId, inviteCode)

        then:
        def e = thrown(SkillsClientException)
        e.message.contains("explanation:Invitation Code is for a different user, errorCode:NotYourInvitationCode, success:false")

        cleanup:
        inviteOnlyProjectService.validateInviteEmail = false
    }

    def "if email validation is enabled, can use invite code generated for joining user"() {
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
        String addy = EmailUtils.generateEmaillAddressFor(users[0])
        SkillsService.UseParams params = new SkillsService.UseParams(
                username: users[0],
                email: addy
        )
        def userService = createService(params)

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: [addy]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()
        println email
        println userAttrsRepo.findEmailByUserId(users[0].toLowerCase())
        String inviteCode = extractInviteFromEmail(email[0].content.toString())
        def res = userService.joinProject(proj.projectId, inviteCode)

        then:
        res.success == true

        cleanup:
        inviteOnlyProjectService.validateInviteEmail = false
    }

    def "if email validation is enabled, can use invite code generated for joining user even if case does not match"() {
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
        String addy = userAttrsRepo.findEmailByUserId(users[0].toLowerCase())
        SkillsService.UseParams params = new SkillsService.UseParams(
                username: users[0],
                email: addy
        )
        def userService = createService(params)

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: [addy.toUpperCase()]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = greenMail.getReceivedMessages()
        println email
        println userAttrsRepo.findEmailByUserId(users[0].toLowerCase())
        String inviteCode = extractInviteFromEmail(email[0].content.toString())
        def res = userService.joinProject(proj.projectId, inviteCode)

        then:
        res.success == true

        cleanup:
        inviteOnlyProjectService.validateInviteEmail = false
    }

}
