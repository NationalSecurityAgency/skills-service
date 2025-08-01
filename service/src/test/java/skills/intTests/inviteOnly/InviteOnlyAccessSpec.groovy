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

import groovy.json.JsonOutput
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.admin.InviteOnlyProjectService
import skills.storage.model.Attachment
import skills.storage.model.auth.RoleName
import skills.storage.repos.AttachmentRepo
import skills.utils.GroovyToJavaByteUtils
import skills.utils.WaitFor

class InviteOnlyAccessSpec extends InviteOnlyBaseSpec {

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    @Autowired
    AttachmentRepo attachmentRepo

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

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

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

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

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

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

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

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

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

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

        newService.joinProject(proj.projectId, invite)

        def badgeDescription = newService.getBadgeDescriptions(proj.projectId, badge.badgeId, false)
        skillsService.revokeInviteOnlyProjectAccess(proj.projectId, user)
        Thread.sleep(500)
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
        WaitFor.wait {
            greenMail.getReceivedMessages().length > 0
        }

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

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

        def email = EmailUtils.getEmail(greenMail, 0)
        def inviteCode = extractInviteFromEmail(email.html)
        userService.joinProject(proj.projectId, inviteCode)
        greenMail.reset()

        //to allow a user to accept a second invite we have to clear out the invite tables - accepted invites are retained for a configurable period of days
        //to allow better error handling if a user tries to re-use an already claimed invite
        inviteOnlyProjectService.removeClaimedInviteTokens(new Date())

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        email = EmailUtils.getEmail(greenMail, 0)
        inviteCode = extractInviteFromEmail(email.html)
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

        def email = EmailUtils.getEmail(greenMail, 0)
        def inviteCode = extractInviteFromEmail(email.html)
        userService.joinProject(proj.projectId, inviteCode)
        greenMail.reset()
        //to allow a user to accept a second invite we have to clear out the invite tables - accepted invites are retained for a configurable period of days
        //to allow better error handling if a user tries to re-use an already claimed invite
        inviteOnlyProjectService.removeClaimedInviteTokens(new Date())

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        email = EmailUtils.getEmail(greenMail, 0)
        inviteCode = extractInviteFromEmail(email.html)
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

        def email = EmailUtils.getEmail(greenMail, 0)
        def inviteCode = extractInviteFromEmail(email.html)
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

        def email = EmailUtils.getEmail(greenMail, 0)
        def inviteCode = extractInviteFromEmail(email.html)
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

        def email = EmailUtils.getEmail(greenMail, 0)
        def inviteCode = extractInviteFromEmail(email.html)
        def res = userService.joinProject(proj.projectId, inviteCode)

        then:
        res.success == true

        cleanup:
        inviteOnlyProjectService.validateInviteEmail = false
    }

    def "invite only project admins can be contacted if current user has not been granted access"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def user = getRandomUsers(1, true)[0]
        def shouldBeAbleToContact = createService(new SkillsService.UseParams(
                username: user,
                email: EmailUtils.generateEmaillAddressFor(user),
                firstName: "${user.toUpperCase()}_first",
                lastName: "${user.toUpperCase()}_last"
        ))

        when:
        shouldBeAbleToContact.contactProjectOwner(proj.projectId, "this should work")

        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def contactEmail = EmailUtils.getEmail(greenMail, 0)

        then:
        contactEmail
        contactEmail.html.contains("should work")
        contactEmail.plainText.contains("should work")
        contactEmail.fromEmail == [EmailUtils.generateEmaillAddressFor(user)]
    }

    def "invite only project admins can be contacted if current user has access"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkill(skill)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        def user = getRandomUsers(1, true)[0]
        def shouldBeAbleToContact = createService(new SkillsService.UseParams(
                username: user,
                email: EmailUtils.generateEmaillAddressFor(user),
                firstName: "${user.toUpperCase()}_first",
                lastName: "${user.toUpperCase()}_last"
        ))


        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: [EmailUtils.generateEmaillAddressFor(user)]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

        shouldBeAbleToContact.joinProject(proj.projectId, invite)
        greenMail.reset()

        when:
        shouldBeAbleToContact.contactProjectOwner(proj.projectId, "should work")

        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def contactEmail = EmailUtils.getEmail(greenMail, 0)

        then:
        contactEmail
        contactEmail.html.contains("should work")
        contactEmail.plainText.contains("should work")
        contactEmail.fromEmail == [EmailUtils.generateEmaillAddressFor(user)]
    }

    def "cannot suggest users for a project that has been configured for invite only without accepting invite"() {
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

        newService.suggestClientUsersForProject(proj.projectId, "")

        then:
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "cannot download attachment from invite only without accepting invite"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)
        def result = skillsService.uploadAttachment(resource, proj.projectId, null, null)
        String attachmentHref = result.href

        def skill = SkillsFactory.createSkill(99, 1)
        skill.description = "Here is a [Link](${attachmentHref})".toString()
        skill.pointIncrement = 200

        skillsService.createSkill(skill)

        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        List<Attachment> attachments = attachmentRepo.findAll()
        assert attachments.size() == 1
        String url = "/api/download/${attachments[0].uuid}"
        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment(url)
        File file = fileAndHeaders.file
        assert file
        assert file.bytes == contents.getBytes()

        def user = getRandomUsers(1, true)[0]
        def newService = createService(user)
        when:
        newService.downloadAttachment(url)
        then:
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "able to download attachment after invite was accepted"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        skillsService.createProject(proj)
        skillsService.createSubject(subj)

        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)
        def result = skillsService.uploadAttachment(resource, proj.projectId, null, null)
        String attachmentHref = result.href

        def skill = SkillsFactory.createSkill(99, 1)
        skill.description = "Here is a [Link](${attachmentHref})".toString()
        skill.pointIncrement = 200

        skillsService.createSkill(skill)

        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        List<Attachment> attachments = attachmentRepo.findAll()
        assert attachments.size() == 1
        String url = "/api/download/${attachments[0].uuid}"
        SkillsService.FileAndHeaders fileAndHeaders = skillsService.downloadAttachment(url)
        File file = fileAndHeaders.file
        assert file
        assert file.bytes == contents.getBytes()

        def user = getRandomUsers(1, true)[0]
        def shouldBeAbleToDownload = createService(new SkillsService.UseParams(
                username: user,
                email: EmailUtils.generateEmaillAddressFor(user),
                firstName: "${user.toUpperCase()}_first",
                lastName: "${user.toUpperCase()}_last"
        ))


        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: [EmailUtils.generateEmaillAddressFor(user)]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

        shouldBeAbleToDownload.joinProject(proj.projectId, invite)
        greenMail.reset()
        when:
        SkillsService.FileAndHeaders res = shouldBeAbleToDownload.downloadAttachment(url)
        File file1 = res.file

        then:
        file1
        file1.bytes == contents.getBytes()
    }

    def "root user can download attachment"() {
        def users = getRandomUsers(2, true)
        def newService = createService(users[0])

        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        newService.createProject(proj)
        newService.createSubject(subj)

        String filename = 'test-pdf.pdf'
        String contents = 'Test is a test'
        Resource resource = GroovyToJavaByteUtils.toByteArrayResource(contents, filename)
        def result = newService.uploadAttachment(resource, proj.projectId, null, null)
        String attachmentHref = result.href

        def skill = SkillsFactory.createSkill(99, 1)
        skill.description = "Here is a [Link](${attachmentHref})".toString()
        skill.pointIncrement = 200

        newService.createSkill(skill)

        newService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        List<Attachment> attachments = attachmentRepo.findAll()
        assert attachments.size() == 1
        String url = "/api/download/${attachments[0].uuid}"

        when:
        SkillsService.FileAndHeaders res = localRootSkillsService.downloadAttachment(url)
        File file1 = res.file

        then:
        file1
        file1.bytes == contents.getBytes()

    }

    def 'current user is allowed to remove ROLE_PRIVATE_PROJECT_USER from myself'() {
        def proj = SkillsFactory.createProject(99)
        skillsService.createProject(proj)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

        skillsService.joinProject(proj.projectId, invite)

        when:
        def before = skillsService.getUserRolesForProjectAndUser(proj.projectId, skillsService.userName)
        skillsService.deleteUserRole(skillsService.userName, proj.projectId, RoleName.ROLE_PRIVATE_PROJECT_USER.toString())
        def after = skillsService.getUserRolesForProjectAndUser(proj.projectId, skillsService.userName)

        then:
        before.roleName.sort() == [RoleName.ROLE_PROJECT_ADMIN.toString(), RoleName.ROLE_PRIVATE_PROJECT_USER.toString()].sort()
        after.roleName.sort() == [RoleName.ROLE_PROJECT_ADMIN.toString()]
    }

    def 'canAccess endpoint only returns true for if user has ROLE_PROJECT_ADMIN or ROLE_PRIVATE_PROJECT_USER role'() {
        def userNames = getRandomUsers(2, true)
        def otherUser1Service = createService(userNames[0])
        def otherUser2Service = createService(userNames[1])

        def proj = SkillsFactory.createProject(99)
        skillsService.createProject(proj)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

        otherUser1Service.joinProject(proj.projectId, invite)

        when:
        def canAccessProjAdmin = skillsService.canAccessProject(proj.projectId, skillsService.userName)
        def canAccessUser1 = skillsService.canAccessProject(proj.projectId, otherUser1Service.userName)
        def canAccessUser2 = skillsService.canAccessProject(proj.projectId, otherUser2Service.userName)

        then:
        canAccessProjAdmin == true // proj admin
        canAccessUser1 == true // accepted invite
        canAccessUser2 == false // does not belong to private project
    }

    def "invite only project appears in my projects and catalog after accepting invite"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        subj.description = "subj descrip"
        def skills = SkillsFactory.createSkills(3, 99, 1, 100, 1)
        def badge = SkillsFactory.createBadge(99, 1)
        def badge2 = SkillsFactory.createBadge(99, 2)
        def badge3 = SkillsFactory.createBadge(99, 3)

        Date twoWeekInFuture = new Date()+14
        Date twoWeeksAgo = new Date()-14
        def gem1 = SkillsFactory.createBadge(99, 4)
        gem1.enabled = false;
        gem1.startDate = twoWeeksAgo
        gem1.endDate = twoWeekInFuture

        def gem2 = SkillsFactory.createBadge(99, 5)
        gem2.enabled = false;
        gem2.startDate = twoWeeksAgo
        gem2.endDate = twoWeekInFuture

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge2.badgeId, skillId: skills[0].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)
        skillsService.createBadge(badge3)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge3.badgeId, skillId: skills[1].skillId])
        badge3.enabled = true
        skillsService.createBadge(badge3)

        skillsService.createBadge(gem1)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: gem1.badgeId, skillId: skills[0].skillId])
        gem1.enabled = true
        skillsService.createBadge(gem1)
        skillsService.createBadge(gem2)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: gem2.badgeId, skillId: skills[1].skillId])
        gem2.enabled = true
        skillsService.createBadge(gem2)

        def users = getRandomUsers(2, true)
        def newService = createService(users[0])
        def newService1 = createService(users[1])

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

        def projectsBeforeJoining = newService.getAvailableMyProjects()
        def myProjectsBeforeJoining = newService.getMyProgressSummary()
        newService.joinProject(proj.projectId, invite)
        newService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId]).body
        def projectsAfterJoining = newService.getAvailableMyProjects()
        def myProjectsAfterJoining = newService.getMyProgressSummary()

        greenMail.purgeEmailFromAllMailboxes()
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail1@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email2 = EmailUtils.getEmail(greenMail, 0)
        def invite2 = extractInviteFromEmail(email2.html)

        newService1.joinProject(proj.projectId, invite2)
        def myProjectsAfterJoining1 = newService1.getMyProgressSummary()

        skillsService.revokeInviteOnlyProjectAccess(proj.projectId, users[0])
        def myProjectsAfterRevoke = newService.getMyProgressSummary()

        then:
        projectsBeforeJoining == []
        myProjectsBeforeJoining.projectSummaries == []
        projectsAfterJoining[0].projectId == proj.projectId
        myProjectsAfterJoining.projectSummaries[0].projectId == proj.projectId

        myProjectsAfterJoining.totalBadges == 5
        myProjectsAfterJoining.gemCount == 2
        myProjectsAfterJoining.globalBadgeCount == 0

        myProjectsAfterJoining.numAchievedBadges == 3
        myProjectsAfterJoining.numAchievedGemBadges == 1
        myProjectsAfterJoining.numAchievedGlobalBadges == 0

        myProjectsAfterJoining1.projectSummaries[0].projectId == proj.projectId

        myProjectsAfterJoining1.totalBadges == 5
        myProjectsAfterJoining1.gemCount == 2
        myProjectsAfterJoining1.globalBadgeCount == 0

        myProjectsAfterJoining1.numAchievedBadges == 0
        myProjectsAfterJoining1.numAchievedGemBadges == 0
        myProjectsAfterJoining1.numAchievedGlobalBadges == 0

        myProjectsAfterRevoke.projectSummaries == []
        myProjectsAfterRevoke.totalBadges == 0
        myProjectsAfterRevoke.gemCount == 0
        myProjectsAfterRevoke.globalBadgeCount == 0
        myProjectsAfterRevoke.numAchievedBadges == 0
        myProjectsAfterRevoke.numAchievedGemBadges == 0
        myProjectsAfterRevoke.numAchievedGlobalBadges == 0
    }

    def "invite only project's badges are in my badges"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        subj.description = "subj descrip"
        def skills = SkillsFactory.createSkills(3, 99, 1, 100, 1)
        def badge = SkillsFactory.createBadge(99, 1)
        def badge2 = SkillsFactory.createBadge(99, 2)
        def badge3 = SkillsFactory.createBadge(99, 3)

        Date twoWeekInFuture = new Date() + 14
        Date twoWeeksAgo = new Date() - 14
        def gem1 = SkillsFactory.createBadge(99, 4)
        gem1.enabled = false;
        gem1.startDate = twoWeeksAgo
        gem1.endDate = twoWeekInFuture

        def gem2 = SkillsFactory.createBadge(99, 5)
        gem2.enabled = false;
        gem2.startDate = twoWeeksAgo
        gem2.endDate = twoWeekInFuture

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge2.badgeId, skillId: skills[0].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)
        skillsService.createBadge(badge3)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge3.badgeId, skillId: skills[1].skillId])
        badge3.enabled = true
        skillsService.createBadge(badge3)

        skillsService.createBadge(gem1)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: gem1.badgeId, skillId: skills[0].skillId])
        gem1.enabled = true
        skillsService.createBadge(gem1)
        skillsService.createBadge(gem2)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: gem2.badgeId, skillId: skills[1].skillId])
        gem2.enabled = true
        skillsService.createBadge(gem2)

        def users = getRandomUsers(2, true)
        def newService = createService(users[0])
        def newService1 = createService(users[1])

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

        def progressBadgesBeforeJoin = newService.getMyProgressBadges()
        newService.joinProject(proj.projectId, invite)
        newService.addSkill([projectId: proj.projectId, skillId: skills[0].skillId]).body
        def progressBadgesAfterJoin = newService.getMyProgressBadges()

        greenMail.purgeEmailFromAllMailboxes()
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail1@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email2 = EmailUtils.getEmail(greenMail, 0)
        def invite2 = extractInviteFromEmail(email2.html)

        newService1.joinProject(proj.projectId, invite2)
        def progressBadgesAfterJoin1 = newService.getMyProgressBadges()

        println JsonOutput.prettyPrint(JsonOutput.toJson(progressBadgesAfterJoin))
        then:
        progressBadgesBeforeJoin == []
        progressBadgesAfterJoin.size() == 5
        progressBadgesAfterJoin.find { it.badgeId == badge.badgeId }.badgeAchieved == true
        progressBadgesAfterJoin.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        progressBadgesAfterJoin.find { it.badgeId == badge3.badgeId }.badgeAchieved == false
        progressBadgesAfterJoin.find { it.badgeId == gem1.badgeId }.badgeAchieved == true
        progressBadgesAfterJoin.find { it.badgeId == gem2.badgeId }.badgeAchieved == false
        progressBadgesAfterJoin1.size() == 5
        progressBadgesAfterJoin1.find { it.badgeId == badge.badgeId }.badgeAchieved == true
        progressBadgesAfterJoin1.find { it.badgeId == badge2.badgeId }.badgeAchieved == true
        progressBadgesAfterJoin1.find { it.badgeId == badge3.badgeId }.badgeAchieved == false
        progressBadgesAfterJoin1.find { it.badgeId == gem1.badgeId }.badgeAchieved == true
        progressBadgesAfterJoin1.find { it.badgeId == gem2.badgeId }.badgeAchieved == false
    }

    def "users that are not invited must NOT be able to add invite-only project to my projects"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skills = SkillsFactory.createSkills(3, 99, 1, 100, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        def users = getRandomUsers(2, true)
        def newService = createService(users[0])

        when:
        newService.addMyHiddenProject(proj.projectId)
        then:
        def err = thrown(SkillsClientException)
        err.httpStatus == HttpStatus.FORBIDDEN
    }

    def "users that are invited must be able to add invite-only project to my projects"() {
        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skills = SkillsFactory.createSkills(3, 99, 1, 100, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skillsService.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        def users = getRandomUsers(2, true)
        def newService = createService(users[0])

        when:
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        def invite = extractInviteFromEmail(email.html)

        newService.joinProject(proj.projectId, invite)
        def myProjectsAfterJoining = newService.getMyProgressSummary()
        newService.removeMyProject(proj.projectId)
        def myProjectsAfterJoining_t1 = newService.getMyProgressSummary()
        newService.addMyHiddenProject(proj.projectId)
        def myProjectsAfterJoining_t2 = newService.getMyProgressSummary()
        then:
        myProjectsAfterJoining.projectSummaries[0].projectId == proj.projectId
        myProjectsAfterJoining_t1.projectSummaries == []
        myProjectsAfterJoining_t2.projectSummaries[0].projectId == proj.projectId
    }

}
