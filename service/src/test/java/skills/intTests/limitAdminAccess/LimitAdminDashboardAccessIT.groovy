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
package skills.intTests.limitAdminAccess

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import skills.SpringBootApp
import skills.intTests.inviteOnly.InviteOnlyBaseSpec
import skills.intTests.utils.*
import skills.services.admin.InviteOnlyProjectService
import skills.utils.WaitFor

@Slf4j
@SpringBootTest(properties = [
        'skills.config.ui.limitAdminAccess=true',
        'skills.authorization.userInfoUri=https://localhost:8187/userInfo?dn={dn}',
        'skills.authorization.userQueryUri=https://localhost:8187/userQuery?query={query}',
        'skills.authorization.userInfoHealthCheckUri=https://localhost:8187/status'
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class LimitAdminDashboardAccessIT extends InviteOnlyBaseSpec {

    @Autowired
    InviteOnlyProjectService inviteOnlyProjectService

    def setup() {
        inviteOnlyProjectService.validateInviteEmail = false
    }

    def "userInfo adminDashboardAccess is true only for authorized users and root role"() {
        List<String> users = getRandomUsers(3)
        SkillsService rootSkillsService = createRootSkillService()
        SkillsService user1Service = createService(users[1])
        SkillsService user2Service = createService(users[2])
        rootSkillsService.grantDashboardAdminRole(users[1])

        when:
        def rootUserInfo = rootSkillsService.getCurrentUser()
        def user1Info = user1Service.getCurrentUser()
        def user2Info = user2Service.getCurrentUser()
        then:
        rootUserInfo.adminDashboardAccess == true
        user1Info.adminDashboardAccess == true
        user2Info.adminDashboardAccess == false
    }

    def "creation of projects or quizzes without Dashboard Admin role are forbidden"() {
        List<String> users = getRandomUsers(3)
        when:
        SkillsService user1Service = createService(users[0])

        SkillsService rootSkillsService = createRootSkillService()
        SkillsService user2Service = createService(users[1])
        rootSkillsService.grantDashboardAdminRole(users[1])
        then:
        // projects
        isForbidden { user1Service.createProject(SkillsFactory.createProject()) }
        user2Service.createProject(SkillsFactory.createProject())
        def proj2 = SkillsFactory.createProject(2)
        rootSkillsService.createProject(proj2)
        rootSkillsService.pinProject(proj2.projectId)

        isForbidden { user1Service.getProjects() }
        user2Service.getProjects()
        rootSkillsService.getProjects()

        isForbidden { user1Service.projectIdExists([projectId: "some"]) }
        user2Service.projectIdExists([projectId: "some"])
        rootSkillsService.projectIdExists([projectId: "some"])

        // quizzes
        isForbidden { user1Service.createQuizDef(QuizDefFactory.createQuiz(1)) }
        user2Service.createQuizDef(QuizDefFactory.createQuiz(1))
        rootSkillsService.createQuizDef(QuizDefFactory.createQuiz(2))

        isForbidden { user1Service.getQuizDefs() }
        user2Service.getQuizDefs()
        rootSkillsService.getQuizDefs()

        isForbidden { user1Service.quizIdExist("quizId") }
        user2Service.quizIdExist("quizId")
        rootSkillsService.quizIdExist("quizId")

        // user safe operations for any role
        def currentUser1Info = user1Service.getCurrentUser()
        def currentUser2Info = user2Service.getCurrentUser()

        user1Service.addOrUpdateUserSetting("one", "two")
        user2Service.addOrUpdateUserSetting("three", "four")

        user1Service.getUserSettings()
        user2Service.getUserSettings()

        user1Service.updateUserInfo(currentUser1Info)
        user2Service.updateUserInfo(currentUser2Info)

        user1Service.getPublicSettings("public_groupName1")
        user1Service.getPublicSetting("settingId1", "public_groupName1")
    }

    def "users without Dashboard Admin should be able to use validate method of private-invite-only projects"() {
        inviteOnlyProjectService.validateInviteEmail = true

        List<String> users = getRandomUsers(3)
        SkillsService userWithAdminRights = createService(users[0])
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.grantDashboardAdminRole(userWithAdminRights.userName)

        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200

        userWithAdminRights.createProject(proj)
        userWithAdminRights.createSubject(subj)
        userWithAdminRights.createSkill(skill)
        userWithAdminRights.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])


        when:
        def u0email = EmailUtils.generateEmaillAddressFor(users[2])
        SkillsService.UseParams params = new SkillsService.UseParams(
                username: users[2],
                email: u0email
        )
        def userService = createService(params)
        userWithAdminRights.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: [u0email]])
        WaitFor.wait { greenMail.getReceivedMessages().length > 0 }

        def email = EmailUtils.getEmail(greenMail, 0)
        String inviteCode = extractInviteFromEmail(email.html)

        def resp = userService.validateInvite(proj.projectId, inviteCode)

        then:
        resp.projectId == proj.projectId
        resp.valid == true

        cleanup:
        inviteOnlyProjectService.validateInviteEmail = false
        greenMail.reset()
    }

    def "users without Dashboard Admin should be able to join private-invite-only-projects"() {
        List<String> users = getRandomUsers(3)
        SkillsService userWithAdminRights = createService(users[0])
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.grantDashboardAdminRole(userWithAdminRights.userName)

        def proj = SkillsFactory.createProject(99)
        def subj = SkillsFactory.createSubject(99)
        subj.description = "subj descrip"
        def skill = SkillsFactory.createSkill(99, 1)
        skill.pointIncrement = 200
        def badge = SkillsFactory.createBadge(99, 1)
        badge.description = "badge descrip"

        userWithAdminRights.createProject(proj)
        userWithAdminRights.createSubject(subj)
        userWithAdminRights.createSkill(skill)
        userWithAdminRights.changeSetting(proj.projectId, "invite_only", [projectId: proj.projectId, setting: "invite_only", value: "true"])
        userWithAdminRights.createBadge(badge)
        userWithAdminRights.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skill.skillId])
        badge.enabled = true
        userWithAdminRights.createBadge(badge)

        def newService = createService(users[2])

        when:
        userWithAdminRights.inviteUsersToProject(proj.projectId, [validityDuration: "PT5M", recipients: ["someemail@email.foo"]])
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

    def "users without Dashboard Admin should be able to get icons"(){
        List<String> users = getRandomUsers(3)
        SkillsService userWithAdminRights = createService(users[0])
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.grantDashboardAdminRole(userWithAdminRights.userName)

        ClassPathResource resource = new ClassPathResource("/dot2.png")

        String projId = SkillsFactory.defaultProjId
        when:
        userWithAdminRights.createProject([projectId: projId, name: "Test Icon Project"])
        def file = resource.getFile()
        userWithAdminRights.uploadIcon([projectId:(projId)], file)
        def result = skillsService.getIconCssForProject([projectId:(projId)])
        def clientDisplayRes = skillsService.getCustomClientDisplayCss(projId)
        then:
        result == [[filename:'dot2.png', cssClassname:"${projId}-dot2png"]]
        clientDisplayRes.toString().startsWith(".TestProject1-dot2png {\tbackground-image: url(")
    }

    def "users without Dashboard Admin should to list versions"() {
        when:
        def versions = skillsService.listVersions(SkillsFactory.getDefaultProjId(1))

        then:
        versions != null
        versions == []
    }

    def "users without Dashboard Admin should be able to get project description"(){
        List<String> users = getRandomUsers(3)
        SkillsService userWithAdminRights = createService(users[0])
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.grantDashboardAdminRole(userWithAdminRights.userName)

        Map proj = SkillsFactory.createProject()
        proj.description = "first"
        userWithAdminRights.createProject(proj)

        when:
        def res = skillsService.getProjectDescription(proj.projectId)
        def res1 = rootSkillsService.getProjectDescription(proj.projectId)

        then:
        res.description == "first"
        res1.description == "first"
    }

    boolean isForbidden(Closure c) {
        try {
            c.call()
        } catch (SkillsClientException sk) {
            assert sk.httpStatus == HttpStatus.FORBIDDEN
            return true
        }

        return false
    }
}
