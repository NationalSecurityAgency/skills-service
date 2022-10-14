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
package skills.intTests.inviteOnly

import skills.intTests.utils.EmailUtils
import skills.intTests.utils.SkillsFactory
import skills.utils.WaitFor
import spock.lang.IgnoreRest

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

class InviteOnlyManagementSpec extends InviteOnlyBaseSpec {

    def "can get pending invites status"() {
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

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT10M", recipients: ["someemail2@email.foo"]])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT15M", recipients: ["someemail3@email.foo"]])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT20M", recipients: ["someemail4@email.foo"]])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT25M", recipients: ["someemail5@email.foo"]])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT30M", recipients: ["someemail6@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 5 }

        when:
        def page1 = skillsService.getPendingProjectInvites(proj.projectId, 5, 1, "expires", true)
        def page2 = skillsService.getPendingProjectInvites(proj.projectId, 5, 2, "expires", true)

        then:
        page1.data.size() == 5
        page1.count == 5
        page1.totalCount == 6
        page1.data[0].recipientEmail == "someemail@email.foo"
        page1.data[1].recipientEmail == "someemail2@email.foo"
        page1.data[2].recipientEmail == "someemail3@email.foo"
        page1.data[3].recipientEmail == "someemail4@email.foo"
        page1.data[4].recipientEmail == "someemail5@email.foo"

        page2.data.size() == 1
        page2.count == 1
        page2.totalCount == 6
        page2.data[0].recipientEmail == "someemail6@email.foo"
    }

    def "can extend expiration for pending invite"() {
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

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT1M", recipients: ["someemail@email.foo"]])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT10M", recipients: ["someemail2@email.foo"]])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT15M", recipients: ["someemail3@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 2 }

        Date nowPlus45Min = LocalDateTime.now().plus(45, ChronoUnit.MINUTES).toDate()

        when:
        skillsService.extendProjectInviteExpiration(proj.projectId, "someemail@email.foo", "PT1H")
        def page1 = skillsService.getPendingProjectInvites(proj.projectId, 5, 1, "expires", true)

        then:
        page1.data.size() == 3
        page1.count == 3
        page1.totalCount == 3
        page1.data[0].recipientEmail == "someemail2@email.foo"
        page1.data[1].recipientEmail == "someemail3@email.foo"
        page1.data[2].recipientEmail == "someemail@email.foo"
        LocalDateTime.parse(page1.data[2].expires, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toDate().after(nowPlus45Min)
    }

    def "can remind user of pending invite"() {
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

        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT1M", recipients: ["someemail@email.foo"]])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT10M", recipients: ["someemail2@email.foo"]])
        skillsService.inviteUsersToProject(proj.projectId, [validityDuration: "PT15M", recipients: ["someemail3@email.foo"]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 2 }
        greenMail.reset()

        when:
        skillsService.remindUserOfPendingInvite(proj.projectId, "someemail@email.foo")
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail, 1)

        then:
        emailRes.subj == "SkillTree Project Invitation Reminder"
        emailRes.html.contains == "This is a friendly reminder that you have been invited to join the [[${proj.name}]] project. Please use the link below to accept the invitation. Your invitation will expire approximately"
    }

    def "cannot remind user of pending invite if invite ie expired"() {

    }

    def "can delete pending invite"() {

    }

    def "can delete expired pending invite"() {

    }
}
