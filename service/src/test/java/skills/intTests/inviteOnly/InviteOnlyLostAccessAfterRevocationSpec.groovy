/**
 * Copyright 2024 SkillTree
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
import skills.services.settings.Settings
import skills.storage.repos.AttachmentRepo
import skills.utils.WaitFor

class InviteOnlyLostAccessAfterRevocationSpec extends InviteOnlyBaseSpec {

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    @Autowired
    AttachmentRepo attachmentRepo

    def setup() {
        inviteOnlyProjectService.validateInviteEmail = false
    }

    def "user lost access to a project after invite-only project access was revoked"() {
        def proj = createProjForTheseTest(98)
        skillsService.configuredProjectAsInviteOnly(proj.projectId)

        def proj1 = createProjForTheseTest(99)
        skillsService.configuredProjectAsInviteOnly(proj1.projectId)

        List<SkillsService> otherUsers = getRandomUsers(2, true).collect { createService(it)}

        Closure joinProj = { def projToJoin ->
            skillsService.inviteUsersToProject(projToJoin.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo", "someemail2@email.foo"]])
            WaitFor.wait { greenMail.getReceivedMessages().length == 2 }

            def emails = EmailUtils.getEmails(greenMail)
            List invites = emails.collect { extractInviteFromEmail(it.html) }
            otherUsers[0].joinProject(projToJoin.projectId, invites[0])
            otherUsers[1].joinProject(projToJoin.projectId, invites[1])
            greenMail.purgeEmailFromAllMailboxes()
        }
        joinProj(proj)
        joinProj(proj1)

        when:
        def user0SkillsSummary_before = otherUsers[0].getSkillsSummaryForCurrentUser(proj.projectId)
        def user1SkillsSummary_before = otherUsers[1].getSkillsSummaryForCurrentUser(proj.projectId)
        def user0MyProgressSummary_before = otherUsers[0].getMyProgressSummary()
        def user1MyProgressSummary_before = otherUsers[1].getMyProgressSummary()

        def user0SkillsSummaryProj1_before = otherUsers[0].getSkillsSummaryForCurrentUser(proj1.projectId)
        def user1SkillsSummaryProj1_before = otherUsers[1].getSkillsSummaryForCurrentUser(proj1.projectId)

        skillsService.revokeInviteOnlyProjectAccess(proj.projectId, otherUsers[0].userName)
        validateInviteOnlyForbidden { otherUsers[0].getSkillsSummaryForCurrentUser(proj.projectId) }
        def user1SkillsSummary_after = otherUsers[1].getSkillsSummaryForCurrentUser(proj.projectId)
        def user0MyProgressSummary_after = otherUsers[0].getMyProgressSummary()
        def user1MyProgressSummary_after = otherUsers[1].getMyProgressSummary()

        def user0SkillsSummaryProj1_after = otherUsers[0].getSkillsSummaryForCurrentUser(proj1.projectId)
        def user1SkillsSummaryProj1_after = otherUsers[1].getSkillsSummaryForCurrentUser(proj1.projectId)
        then:
        user0SkillsSummary_before.projectId == proj.projectId
        user1SkillsSummary_before.projectId == proj.projectId
        user0MyProgressSummary_before.projectSummaries.projectId.sort() == [proj.projectId, proj1.projectId].sort()
        user1MyProgressSummary_before.projectSummaries.projectId.sort() == [proj.projectId, proj1.projectId].sort()

        user0SkillsSummaryProj1_before.projectId == proj1.projectId
        user1SkillsSummaryProj1_before.projectId == proj1.projectId

        user1SkillsSummary_after.projectId == proj.projectId
        user0MyProgressSummary_after.projectSummaries.projectId.sort() == [proj1.projectId]
        user1MyProgressSummary_after.projectSummaries.projectId.sort() == [proj.projectId, proj1.projectId].sort()

        user0SkillsSummaryProj1_after.projectId == proj1.projectId
        user1SkillsSummaryProj1_after.projectId == proj1.projectId
    }

    def "users lost access to a project after it was configured to be invite-only"() {
        def proj = createProjForTheseTest(98)
        def proj1 = createProjForTheseTest(99)
        skillsService.enableProdMode(proj)
        skillsService.enableProdMode(proj1)

        List<SkillsService> otherUsers = getRandomUsers(2, true).collect { createService(it)}
        otherUsers[0].addMyProject(proj.projectId)
        otherUsers[0].addMyProject(proj1.projectId)
        otherUsers[1].addMyProject(proj.projectId)
        otherUsers[1].addMyProject(proj1.projectId)
        when:
        def user0SkillsSummary_before = otherUsers[0].getSkillsSummaryForCurrentUser(proj.projectId)
        def user1SkillsSummary_before = otherUsers[1].getSkillsSummaryForCurrentUser(proj.projectId)
        def user0MyProgressSummary_before = otherUsers[0].getMyProgressSummary()
        def user1MyProgressSummary_before = otherUsers[1].getMyProgressSummary()

        def user0SkillsSummaryProj1_before = otherUsers[0].getSkillsSummaryForCurrentUser(proj1.projectId)
        def user1SkillsSummaryProj1_before = otherUsers[1].getSkillsSummaryForCurrentUser(proj1.projectId)

        skillsService.changeSettings(proj.projectId, [
                [projectId: proj.projectId, setting: Settings.PRODUCTION_MODE.settingName, value: Boolean.FALSE.toString().toLowerCase()],
                [projectId: proj.projectId, setting: Settings.INVITE_ONLY_PROJECT.settingName, value: Boolean.TRUE.toString().toLowerCase()]
        ])

        validateInviteOnlyForbidden { otherUsers[0].getSkillsSummaryForCurrentUser(proj.projectId) }
        validateInviteOnlyForbidden { otherUsers[1].getSkillsSummaryForCurrentUser(proj.projectId) }
        def user0MyProgressSummary_after = otherUsers[0].getMyProgressSummary()
        def user1MyProgressSummary_after = otherUsers[1].getMyProgressSummary()

        def user0SkillsSummaryProj1_after = otherUsers[0].getSkillsSummaryForCurrentUser(proj1.projectId)
        def user1SkillsSummaryProj1_after = otherUsers[1].getSkillsSummaryForCurrentUser(proj1.projectId)
        then:
        user0SkillsSummary_before.projectId == proj.projectId
        user1SkillsSummary_before.projectId == proj.projectId
        user0MyProgressSummary_before.projectSummaries.projectId.sort() == [proj.projectId, proj1.projectId].sort()
        user1MyProgressSummary_before.projectSummaries.projectId.sort() == [proj.projectId, proj1.projectId].sort()

        user0SkillsSummaryProj1_before.projectId == proj1.projectId
        user1SkillsSummaryProj1_before.projectId == proj1.projectId

        user0MyProgressSummary_after.projectSummaries.projectId.sort() == [proj1.projectId]
        user1MyProgressSummary_after.projectSummaries.projectId.sort() == [proj1.projectId].sort()

        user0SkillsSummaryProj1_after.projectId == proj1.projectId
        user1SkillsSummaryProj1_after.projectId == proj1.projectId
    }

    private def createProjForTheseTest(int projNum) {
        def proj = SkillsFactory.createProject(projNum)
        def subj = SkillsFactory.createSubject(projNum, 1)
        def skill = SkillsFactory.createSkill(projNum, 1, 1, 1, 2, 480, 200 )
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
        return proj
    }

    private static void validateInviteOnlyForbidden(Closure checkThisCode) {
        try {
            checkThisCode.call()
        } catch (SkillsClientException clientException) {
            boolean forbiddenCode = clientException.httpStatus == HttpStatus.FORBIDDEN
            boolean inviteOnlyDeniedMsg = clientException.resBody?.contains("This Project has been configured with Invite Only access requirements")
            if (forbiddenCode && inviteOnlyDeniedMsg) {
                return
            }
        }

        throw new RuntimeException("Provide call did not emit SkillsClientException with 403 status")
    }

}
