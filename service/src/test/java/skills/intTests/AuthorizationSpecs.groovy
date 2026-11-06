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

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import skills.controller.request.model.CopyToAnotherProjectRequestType
import skills.intTests.utils.AdminGroupDefFactory
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.settings.Settings
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserRoleRepo
import skills.storage.repos.UserTagRepo
import skills.utils.GroovyToJavaByteUtils
import skills.utils.WaitFor
import spock.lang.IgnoreIf
import spock.lang.Unroll

import static org.springframework.http.HttpMethod.*

class AuthorizationSpecs extends DefaultIntSpec {

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    UserTagRepo userTagRepo

    String projId = "myProject"
    String projId2 = "myProject2"
    def project
    def project2

    def setup() {
        skillsService.deleteProjectIfExist(projId)
        skillsService.deleteProjectIfExist(projId2)
        project = skillsService.createProject([projectId: projId, name: "My Project"]).body
        assert project.success == true
        project = skillsService.getProject(projId)
        project2 = skillsService.createProject([projectId: projId2, name: "My Project2"]).body
        assert project2.success == true
        project2 = skillsService.getProject(projId2)
    }

    def "project summaries enforce project access with community protection #communityProtected and view #view"() {
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        SkillsService owner = createService(users[1])
        Map accessProject = createProjectForAccessChecks(owner, communityProtected, false)

        expect:
        learner.getSkillsSummaryForCurrentUser(projId2).projectName == 'My Project2'

        when:
        ResponseEntity<String> response = learner.wsHelper.rawGet(
                "api/projects/${accessProject.projectId}/summary", view ? [view: view] : [:])

        then:
        verifyAll {
            if (communityProtected) {
                response.statusCode.value() == HttpStatus.FORBIDDEN.value()
                !response.body.contains(accessProject.name)
            } else {
                response.statusCode.value() == HttpStatus.OK.value()
            }
        }

        where:
        communityProtected | view
        false              | ''
        false              | 'overview'
        false              | '/projects/myProject2/'
        false              | 'api/projects/TestProject1/contact'
        true               | ''
        true               | 'overview'
        true               | '/projects/myProject2/'
        true               | 'api/projects/TestProject1/contact'
    }

    def "project summaries enforce project access with invite-only protection [#inviteOnly] and view [#view]"() {
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        SkillsService owner = createService(users[1])
        Map accessProject = createProjectForAccessChecks(owner, false, inviteOnly)

        expect:
        learner.getSkillsSummaryForCurrentUser(projId2).projectName == 'My Project2'

        when:
        ResponseEntity<String> response = learner.wsHelper.rawGet(
                "api/projects/${accessProject.projectId}/summary", view ? [view: view] : [:])

        then:
        verifyAll {
            if (inviteOnly) {
                response.statusCode.value() == HttpStatus.FORBIDDEN.value()
                !response.body.contains(accessProject.name)
            } else {
                response.statusCode.value() == HttpStatus.OK.value()
            }
        }

        where:
        inviteOnly         | view
        false              | ''
        false              | 'overview'
        false              | '/projects/myProject2/'
        false              | 'api/projects/TestProject1/contact'
        true               | ''
        true               | 'overview'
        true               | '/projects/myProject2/'
        true               | 'api/projects/TestProject1/contact'
    }

    private Map createProjectForAccessChecks(SkillsService owner, boolean communityProtected, boolean inviteOnly) {
        Map accessProject = SkillsFactory.createProject(1)
        if (communityProtected) {
            SkillsService root = createRootSkillService()
            root.saveUserTag(owner.userName, 'dragons', ['DivineDragon'])
            accessProject.enableProtectedUserCommunity = true
        }
        owner.createProject(accessProject)
        if (inviteOnly) {
            owner.changeSetting(accessProject.projectId, 'invite_only',
                    [projectId: accessProject.projectId, setting: 'invite_only', value: 'true'])
        }
        return accessProject
    }

    def "project read endpoints enforce #protection access for authorized #authorized callers with view #view"() {
        Map fixture = createProjectEndpointFixture(protection)
        SkillsService caller = authorized ? fixture.owner : fixture.caller

        expect:
        fixture.owner.getSkillsSummaryForCurrentUser(fixture.project.projectId).projectName == fixture.project.name
        assertEndpointStatuses(caller, projectReadRequests(fixture), view, authorized ? 200 : 403)

        where:
        protection    | authorized | view
        'community'   | true       | ''
        'community'   | false      | ''
        'community'   | false      | 'overview'
        'community'   | false      | '/projects/myProject2/'
        'community'   | false      | '/myprojects/myProject2/'
        'invite-only' | true       | ''
        'invite-only' | false      | ''
        'invite-only' | false      | 'overview'
        'invite-only' | false      | '/projects/myProject2/'
        'invite-only' | false      | '/myprojects/myProject2/'
        'invite-only' | false      | 'api/projects/TestProject1/contact'
        'invite-only' | false      | 'api/projects/TestProject1/newInviteRequest'
        'invite-only' | false      | 'app/projects/TestProject1/join/token'
        'invite-only' | false      | 'app/projects/TestProject1/validateInvite/token'
    }

    def "project mutation endpoints enforce #protection access for authorized #authorized callers with view #view"() {
        Map fixture = createProjectEndpointFixture(protection)
        SkillsService caller = authorized ? fixture.owner : fixture.caller

        expect:
        assertEndpointStatuses(caller, projectMutationRequests(fixture), view, authorized ? 200 : 403)

        where:
        protection    | authorized | view
        'community'   | true       | ''
        'community'   | false      | ''
        'community'   | false      | 'overview'
        'community'   | false      | '/projects/myProject2/'
        'community'   | false      | '/myprojects/myProject2/'
        'invite-only' | true       | ''
        'invite-only' | false      | ''
        'invite-only' | false      | 'overview'
        'invite-only' | false      | '/projects/myProject2/'
        'invite-only' | false      | '/myprojects/myProject2/'
        'invite-only' | false      | 'api/projects/TestProject1/contact'
        'invite-only' | false      | 'api/projects/TestProject1/newInviteRequest'
        'invite-only' | false      | 'app/projects/TestProject1/join/token'
        'invite-only' | false      | 'app/projects/TestProject1/validateInvite/token'
    }

    def "root requests enforce project community access with view #view"() {
        Map fixture = createProjectEndpointFixture('community')
        SkillsService root = createRootSkillService()

        expect:
        root.isRoot()
        assertEndpointStatuses(root, projectReadRequests(fixture), view, 403)

        where:
        view << ['', 'overview', '/projects/myProject2/']
    }

    def "quiz endpoints enforce community access for authorized #authorized callers with view #view"() {
        Map fixture = createCommunityEndpointFixture('quiz')
        SkillsService caller = authorized ? fixture.owner : fixture.caller
        String quizId = fixture.quiz.quizId
        String admin = "admin/quiz-definitions/${quizId}"
        String api = "api/quizzes/${quizId}"
        Map paging = [query: '', limit: 10, page: 1, orderBy: 'started', ascending: true]
        List<Map> requests = [
                endpoint(GET, fixture.attachment.href.replaceFirst('^/', '')),
                endpoint(GET, api),
                endpoint(GET, admin),
                endpoint(GET, "${admin}/summary"),
                endpoint(GET, "${admin}/skills-count"),
                endpoint(GET, "${admin}/skills"),
                endpoint(GET, "${admin}/questions"),
                endpoint(GET, "${admin}/questions/${fixture.question.id}"),
                endpoint(GET, "${admin}/questions/${fixture.question.id}/video"),
                endpoint(GET, "${admin}/slides"),
                endpoint(GET, "${admin}/metrics"),
                endpoint(GET, "${admin}/runs", paging),
                endpoint(GET, "${admin}/settings"),
                endpoint(GET, "${admin}/preferences"),
                endpoint(GET, "${admin}/userRoles"),
                endpoint(GET, "${admin}/adminGroups"),
                endpoint(GET, "${admin}/validateEnablingCommunity"),
                endpoint(POST, "${api}/attempt"),
                endpoint(POST, "${admin}/preferences/DisableGradingRequestNotification", [:], [value: 'true']),
                endpoint(PUT, "${admin}/settings", [:], []),
                endpoint(POST, "${admin}/create-question", [:], QuizDefFactory.createChoiceQuestion(1, 2)),
                endpoint(DELETE, "${admin}/questions/${fixture.question.id}")
        ]

        expect:
        assertEndpointStatuses(caller, requests, view, authorized ? 200 : 403)

        where:
        authorized | view
        true       | ''
        false      | ''
        false      | 'overview'
        false      | '/quizzes/TestQuiz2/'
        false      | '/quiz-definitions/TestQuiz2/'
    }

    def "global badge endpoints enforce community access for authorized #authorized callers with view #view"() {
        Map fixture = createCommunityEndpointFixture('badge')
        SkillsService caller = authorized ? fixture.owner : fixture.caller
        String badgeId = fixture.badge.badgeId
        String admin = "admin/badges/${badgeId}"
        String projectId = fixture.project.projectId
        Map paging = [query: '', limit: 10, page: 1, orderBy: 'userId', ascending: true]
        List<Map> requests = [
                endpoint(GET, fixture.attachment.href.replaceFirst('^/', '')),
                endpoint(GET, "api/badges/${badgeId}/customIconCss"),
                endpoint(GET, "api/projects/${projectId}/badges/${badgeId}/summary", [global: true]),
                endpoint(GET, "api/projects/${projectId}/badges/${badgeId}/descriptions", [global: true]),
                endpoint(GET, admin),
                endpoint(GET, "${admin}/skills"),
                endpoint(GET, "${admin}/skills/available", [query: '']),
                endpoint(GET, "${admin}/projects/available", [query: '']),
                endpoint(GET, "${admin}/icons/customIcons"),
                endpoint(GET, "${admin}/userRoles"),
                endpoint(GET, "${admin}/adminGroups"),
                endpoint(GET, "${admin}/users", paging),
                endpoint(GET, "${admin}/validateEnablingCommunity"),
                endpoint(PUT, "${admin}/projects/${projectId}/level/1"),
                endpoint(DELETE, "${admin}/projects/${projectId}/level/1"),
                endpoint(POST, admin, [:], fixture.badge + [enableProtectedUserCommunity: null]),
                endpoint(DELETE, admin)
        ]

        expect:
        assertEndpointStatuses(caller, requests, view, authorized ? 200 : 403)

        where:
        authorized | view
        true       | ''
        false      | ''
        false      | 'overview'
        false      | '/badges/globalBadge2/'
        false      | '/badge/globalBadge2/'
    }

    def "quiz attempt #operation enforces community access for authorized #authorized callers with view #view"() {
        Map fixture = createCommunityEndpointFixture('quiz', true)
        SkillsService caller = authorized ? fixture.owner : fixture.caller
        Map attempt = authorized ? fixture.ownerAttempt : fixture.callerAttempt
        String path = "api/quizzes/${fixture.quiz.quizId}/attempt/${attempt.id}"
        Map request = operation == 'answer' ?
                endpoint(POST, "${path}/answers/${fixture.answerId}", [:], [isSelected: true]) :
                endpoint(POST, "${path}/${operation}")

        expect:
        assertEndpointStatuses(caller, [request], view, authorized ? 200 : 403)

        where:
        operation  | authorized | view
        'answer'   | true       | ''
        'complete' | true       | ''
        'fail'     | true       | ''
        'answer'   | false      | ''
        'complete' | false      | ''
        'fail'     | false      | ''
        'answer'   | false      | 'overview'
        'complete' | false      | 'overview'
        'fail'     | false      | 'overview'
        'answer'   | false      | '/quizzes/TestQuiz2/'
        'complete' | false      | '/quizzes/TestQuiz2/'
        'fail'     | false      | '/quizzes/TestQuiz2/'
        'answer'   | false      | '/quiz-definitions/TestQuiz2/'
        'complete' | false      | '/quiz-definitions/TestQuiz2/'
        'fail'     | false      | '/quiz-definitions/TestQuiz2/'
    }

    def "admin group endpoints enforce community access for authorized #authorized callers with view #view"() {
        Map fixture = createCommunityEndpointFixture('admin-group', false, !authorized)
        SkillsService caller = authorized ? fixture.owner : fixture.caller
        String admin = "admin/admin-group-definitions/${fixture.adminGroup.adminGroupId}"
        List<Map> requests = [
                endpoint(GET, admin),
                endpoint(GET, "${admin}/members"),
                endpoint(GET, "${admin}/projects"),
                endpoint(GET, "${admin}/quizzes"),
                endpoint(GET, "${admin}/badges"),
                endpoint(GET, "${admin}/validateEnablingCommunity"),
                endpoint(POST, admin, [:], fixture.adminGroup),
                endpoint(DELETE, admin)
        ]

        expect:
        assertEndpointStatuses(caller, requests, view, authorized ? 200 : 403)

        where:
        authorized | view
        true       | ''
        false      | ''
        false      | 'overview'
        false      | '/admin-group-definitions/TestAdminGroup2/'
    }

    def "attachment downloads enforce #protection access with view #view"() {
        Map fixture = createProjectEndpointFixture(protection)
        String contents = 'Access-controlled attachment contents'
        Map attachment = fixture.owner.uploadAttachment(
                GroovyToJavaByteUtils.toByteArrayResource(contents, 'access.pdf'), fixture.project.projectId)
        String path = attachment.href.replaceFirst('^/', '')

        expect:
        fixture.owner.wsHelper.rawGet(path, [:]).body == contents
        assertEndpointStatuses(fixture.caller, [endpoint(GET, path)], view, 403)

        where:
        protection    | view
        'community'   | ''
        'community'   | 'overview'
        'community'   | '/projects/myProject2/'
        'invite-only' | ''
        'invite-only' | 'overview'
        'invite-only' | '/projects/myProject2/'
        'invite-only' | 'api/projects/TestProject1/contact'
    }

    def "invite-only exception routes require their controller methods and complete paths"() {
        Map fixture = createProjectEndpointFixture('invite-only')
        String projectId = fixture.project.projectId
        List<Map> requests = [
                endpoint(GET, "api/projects/${projectId}/contact"),
                endpoint(PUT, "api/projects/${projectId}/contact", [:], [message: 'Not a POST']),
                endpoint(POST, "api/projects/${projectId}/contact/extra", [:], [message: 'Not the contact route']),
                endpoint(GET, "api/projects/${projectId}/newInviteRequest"),
                endpoint(POST, "api/projects/${projectId}/newInviteRequestExtra"),
                endpoint(GET, "app/projects/${projectId}/join/token"),
                endpoint(PUT, "app/projects/${projectId}/join/token"),
                endpoint(POST, "app/projects/${projectId}/join/token/extra"),
                endpoint(POST, "app/projects/${projectId}/validateInvite/token"),
                endpoint(GET, "app/projects/${projectId}/validateInvite/token/extra"),
        ]

        expect:
        assertEndpointStatuses(fixture.caller, requests, 'api/projects/TestProject1/contact', 403)
    }

    def "legitimate invite-only exception flows work with unrelated query paths"() {
        startEmailServer()
        Map fixture = createProjectEndpointFixture('invite-only')
        String projectId = fixture.project.projectId
        Map query = [view: '/projects/myProject2/']
        String skillId = fixture.skills[0].skillId
        fixture.owner.shareSkill(projectId, skillId, projId)
        String crossPath = "api/projects/${projId}/projects/${projectId}/skills/${skillId}/summary"

        expect:
        fixture.caller.wsHelper.rawGet("api/projects/${projectId}/summary", query).statusCode.value() == 403

        when:
        ResponseEntity<String> contact = fixture.caller.wsHelper.rawRequest(POST,
                "api/projects/${projectId}/contact", [message: 'Path-only authorization contact'], query)
        WaitFor.wait { greenMail.receivedMessages.length == 1 }

        then:
        contact.statusCode.value() == 200
        EmailUtils.getEmail(greenMail).plainText.contains('Path-only authorization contact')

        when:
        greenMail.purgeEmailFromAllMailboxes()
        ResponseEntity<String> inviteRequest = fixture.caller.wsHelper.rawRequest(POST,
                "api/projects/${projectId}/newInviteRequest", null, query)
        WaitFor.wait { greenMail.receivedMessages.length == 1 }

        then:
        inviteRequest.statusCode.value() == 200
        EmailUtils.getEmail(greenMail).subj == 'New Invite Request for SkillTree Project'

        when:
        greenMail.purgeEmailFromAllMailboxes()
        fixture.owner.inviteUsersToProject(projectId,
                [validityDuration: 'PT5M', recipients: [EmailUtils.generateEmaillAddressFor(fixture.caller.userName)]])
        WaitFor.wait { greenMail.receivedMessages.length == 1 }
        String inviteCode = (EmailUtils.getEmail(greenMail).html =~ /join-project\/[^\/]+\/([^?]+)/)[0][1]
        ResponseEntity<String> validation = fixture.caller.wsHelper.rawGet(
                "app/projects/${projectId}/validateInvite/${inviteCode}", query)

        then:
        validation.statusCode.value() == 200
        new JsonSlurper().parseText(validation.body).valid
        fixture.caller.wsHelper.rawRequest(HEAD, "app/projects/${projectId}/validateInvite/${inviteCode}", null, query).statusCode.value() == 200
        fixture.caller.wsHelper.rawGet("api/projects/${projectId}/summary", query).statusCode.value() == 403

        when:
        ResponseEntity<String> joined = fixture.caller.wsHelper.rawRequest(POST,
                "app/projects/${projectId}/join/${inviteCode}", null, query)

        then:
        joined.statusCode.value() == 200
        fixture.caller.getSkillsSummaryForCurrentUser(projectId).projectName == fixture.project.name
        new JsonSlurper().parseText(fixture.caller.wsHelper.rawGet(crossPath, query).body).projectId == projectId

        when:
        fixture.owner.deleteUserRole(fixture.caller.userName, projectId, RoleName.ROLE_PRIVATE_PROJECT_USER.toString())

        then:
        fixture.caller.wsHelper.rawGet("api/projects/${projectId}/summary", query).statusCode.value() == 403
        fixture.caller.wsHelper.rawGet(crossPath, query).statusCode.value() == 403
    }

    def "cross-project summaries authorize the loaded invite-only project with subject #withSubject"() {
        Map fixture = createProjectEndpointFixture('invite-only')
        String projectId = fixture.project.projectId
        String skillId = fixture.skills[0].skillId
        fixture.owner.shareSkill(projectId, skillId, projId)
        skillsService.addProjectAdmin(projId, fixture.caller.userName)
        String subjectPath = withSubject ? "subjects/${fixture.subject.subjectId}/" : ''
        String path = "api/projects/${projId}/projects/${projectId}/${subjectPath}skills/${skillId}/summary"
        Map query = [view: '/projects/myProject2/']

        expect:
        fixture.caller.getProject(projId).projectId == projId
        fixture.caller.wsHelper.rawGet(path, query).statusCode.value() == 403

        when:
        // The owner has access to the loaded project, not the primary project.
        ResponseEntity<String> allowed = fixture.owner.wsHelper.rawGet(path, query)

        then:
        allowed.statusCode.value() == 200
        new JsonSlurper().parseText(allowed.body).projectId == projectId
        new JsonSlurper().parseText(allowed.body).skillId == skillId

        where:
        withSubject << [false, true]
    }

    def "cross-project summaries allow the loaded invite-only project's approver with subject #withSubject"() {
        List<String> users = getRandomUsers(2)
        SkillsService owner = createService(users[0])
        SkillsService caller = createService(users[1])
        Map context = SkillsFactory.createProject(1)
        Map loaded = SkillsFactory.createProject(2)
        Map subject = SkillsFactory.createSubject(2, 1)
        Map skill = SkillsFactory.createSkill(2, 1, 1)
        owner.createProject(context)
        owner.createProjectAndSubjectAndSkills(loaded, subject, [skill])
        owner.changeSetting(loaded.projectId, 'invite_only',
                [projectId: loaded.projectId, setting: 'invite_only', value: 'true'])
        owner.shareSkill(loaded.projectId, skill.skillId, context.projectId)
        owner.addProjectAdmin(context.projectId, caller.userName)
        owner.addUserRole(caller.userName, loaded.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        String subjectPath = withSubject ? "subjects/${subject.subjectId}/" : ''
        String path = "api/projects/${context.projectId}/projects/${loaded.projectId}/${subjectPath}skills/${skill.skillId}/summary"

        expect:
        caller.getProject(context.projectId).projectId == context.projectId
        ResponseEntity<String> direct = caller.wsHelper.rawGet(
                "api/projects/${loaded.projectId}/skills/${skill.skillId}/summary", [:])
        direct.statusCode.value() == 200
        new JsonSlurper().parseText(direct.body).projectId == loaded.projectId
        new JsonSlurper().parseText(direct.body).skillId == skill.skillId

        when:
        ResponseEntity<String> response = caller.wsHelper.rawGet(path, [:])

        then:
        verifyAll {
            response.statusCode.value() == 200
            new JsonSlurper().parseText(response.body).projectId == loaded.projectId
            new JsonSlurper().parseText(response.body).skillId == skill.skillId
        }

        when:
        owner.deleteUserRole(caller.userName, loaded.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        then:
        caller.getProject(context.projectId).projectId == context.projectId
        caller.wsHelper.rawGet(path, [:]).statusCode.value() == 403
        caller.wsHelper.rawGet("api/projects/${loaded.projectId}/skills/${skill.skillId}/summary", [:]).statusCode.value() == 403

        where:
        withSubject << [false, true]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "cross-project summaries do not grant a proxied approver access with subject #withSubject"() {
        Map fixture = createProjectEndpointFixture('invite-only')
        SkillsService owner = fixture.owner
        SkillsService approver = fixture.caller
        String loadedProjectId = fixture.project.projectId
        String skillId = fixture.skills[0].skillId
        owner.shareSkill(loadedProjectId, skillId, projId)
        skillsService.addProjectAdmin(projId, approver.userName)
        owner.addUserRole(approver.userName, loadedProjectId, RoleName.ROLE_PROJECT_APPROVER.toString())
        String subjectPath = withSubject ? "subjects/${fixture.subject.subjectId}/" : ''
        String path = "api/projects/${projId}/projects/${loadedProjectId}/${subjectPath}skills/${skillId}/summary"
        skillsService.setProxyCredentials(projId, skillsService.getClientSecret(projId))
        String token = skillsService.wsHelper.getTokenForUser(approver.userName)

        expect:
        ResponseEntity<String> direct = approver.wsHelper.rawGet(path, [:])
        direct.statusCode.value() == 200
        new JsonSlurper().parseText(direct.body).skillId == skillId

        when:
        ResponseEntity<String> contextResponse
        ResponseEntity<String> crossProjectResponse
        skillsService.wsHelper.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        try {
            // Prove that the proxy token works for its own public project.
            contextResponse = skillsService.wsHelper.rawGet("api/projects/${projId}/summary", [:])
            crossProjectResponse = skillsService.wsHelper.rawGet(path, [:])
        } finally {
            skillsService.wsHelper.restTemplateWrapper.authenticationToken = null
        }

        then:
        contextResponse.statusCode.value() == 200
        new JsonSlurper().parseText(contextResponse.body).projectName == 'My Project'
        crossProjectResponse.statusCode.value() == 403
        !(crossProjectResponse.body ?: '').contains(fixture.project.name)

        when:
        owner.changeSetting(loadedProjectId, 'invite_only',
                [projectId: loadedProjectId, setting: 'invite_only', value: 'false'])
        ResponseEntity<String> publicTargetResponse
        skillsService.wsHelper.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        try {
            publicTargetResponse = skillsService.wsHelper.rawGet(path, [:])
        } finally {
            skillsService.wsHelper.restTemplateWrapper.authenticationToken = null
        }

        then:
        publicTargetResponse.statusCode.value() == 200
        new JsonSlurper().parseText(publicTargetResponse.body).projectId == loadedProjectId
        new JsonSlurper().parseText(publicTargetResponse.body).skillId == skillId

        where:
        withSubject << [false, true]
    }

    def "cross-project summaries authorize the loaded community project with subject #withSubject"() {
        // Global-badge skills are valid cross-project summary targets without
        // externally sharing a community-protected project's skills.
        Map fixture = createCommunityEndpointFixture('badge')
        String subjectPath = withSubject ? 'subjects/subj1/' : ''
        String path = "api/projects/${projId}/projects/${fixture.project.projectId}/${subjectPath}skills/skill1/summary"
        Map query = [view: '/projects/myProject2/']

        expect:
        fixture.caller.wsHelper.rawGet(path, query).statusCode.value() == 403

        when:
        ResponseEntity<String> allowed = fixture.owner.wsHelper.rawGet(path, query)

        then:
        allowed.statusCode.value() == 200
        new JsonSlurper().parseText(allowed.body).projectId == fixture.project.projectId
        new JsonSlurper().parseText(allowed.body).skillId == 'skill1'

        where:
        withSubject << [false, true]
    }

    def "nested copy routes authorize the protected source project"() {
        Map fixture = createProjectEndpointFixture('community')
        String sourceId = fixture.project.projectId
        String path = "admin/projects/${sourceId}/copy/projects/${projId2}/validateCopy"
        Map body = [copyType: 'EntireSubject', fromSubjectId: fixture.subject.subjectId]

        expect:
        fixture.caller.wsHelper.rawRequest(POST, path, body).statusCode.value() == 403

        when:
        // Community-to-public copying remains rejected by the existing copy
        // validation, rather than losing source authorization to the later ID.
        skillsService.addProjectAdmin(projId2, fixture.owner.userName)
        ResponseEntity<String> allowed = fixture.owner.wsHelper.rawRequest(POST, path, body)

        then:
        allowed.statusCode.value() == 200
        !new JsonSlurper().parseText(allowed.body).isAllowed
        new JsonSlurper().parseText(allowed.body).validationErrors.any { String error -> error.contains('cannot be copied') }
    }

    def "cross-project copy #method #suffix rejects revoked destination community access"() {
        List<String> users = getRandomUsers(2)
        SkillsService owner = createService(users[0])
        SkillsService caller = createService(users[1])
        SkillsService root = createRootSkillService()
        root.saveUserTag(owner.userName, 'dragons', ['DivineDragon'])
        root.saveUserTag(caller.userName, 'dragons', ['DivineDragon'])

        Map source = SkillsFactory.createProject(1)
        Map sourceSubject = SkillsFactory.createSubject(1, 1)
        Map sourceSkill = SkillsFactory.createSkill(1, 1, 1)
        Map destination = SkillsFactory.createProject(2) + [enableProtectedUserCommunity: true]
        owner.createProjectAndSubjectAndSkills(source, sourceSubject, [sourceSkill])
        owner.createProject(destination)
        owner.addProjectAdmin(source.projectId, caller.userName)
        owner.addProjectAdmin(destination.projectId, caller.userName)

        String path = "admin/projects/${source.projectId}/copy/projects/${destination.projectId}"
        Map body = [copyType: CopyToAnotherProjectRequestType.EntireSubject, fromSubjectId: sourceSubject.subjectId]

        expect:
        caller.getProject(destination.projectId).projectId == destination.projectId
        owner.getSubjects(destination.projectId) == []
        ResponseEntity<String> validation = caller.wsHelper.rawRequest(HttpMethod.POST, "${path}/validateCopy", body)
        validation.statusCode.value() == 200
        new JsonSlurper().parseText(validation.body).isAllowed

        when:
        // Keep both admin roles; revoke only the community membership.
        userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(caller.userName, 'dragons'))

        then:
        caller.getProject(source.projectId).projectId == source.projectId
        caller.wsHelper.rawGet("admin/projects/${destination.projectId}", [:]).statusCode.value() == 403

        when:
        ResponseEntity<String> response = caller.wsHelper.rawRequest(method, "${path}${suffix}", body)
        List<Map> destinationSubjects = owner.getSubjects(destination.projectId)

        then:
        verifyAll {
            response.statusCode.value() == 403
            destinationSubjects == []
        }

        when:
        root.saveUserTag(caller.userName, 'dragons', ['DivineDragon'])
        ResponseEntity<String> restored = caller.wsHelper.rawRequest(method, "${path}${suffix}", body)

        then:
        restored.statusCode.value() == 200
        if (suffix) {
            assert new JsonSlurper().parseText(restored.body).isAllowed
            assert owner.getSubjects(destination.projectId) == []
        } else {
            List<Map> copiedSubjects = owner.getSubjects(destination.projectId)
            assert copiedSubjects.subjectId == [sourceSubject.subjectId]
            assert copiedSubjects.name == [sourceSubject.name]
            assert copiedSubjects.numSkills == [1]
            assert owner.getSkill(sourceSkill + [projectId: destination.projectId]).name == sourceSkill.name
        }

        where:
        method          | suffix
        HttpMethod.POST | '/validateCopy'
        HttpMethod.POST | ''
        HttpMethod.PUT  | ''
    }

    def "a skill named projects cannot change project authorization"() {
        Map fixture = createProjectEndpointFixture('community')
        Map skill = SkillsFactory.createSkill(1, 1, 20)
        skill.skillId = 'projects'
        fixture.owner.createSkill(skill)
        String path = "admin/projects/${fixture.project.projectId}/skills/projects/video"

        expect:
        fixture.owner.wsHelper.rawGet(path, [:]).statusCode.value() == 200
        fixture.caller.wsHelper.rawGet(path, [:]).statusCode.value() == 403
        fixture.caller.wsHelper.rawRequest(DELETE, path).statusCode.value() == 403
        fixture.owner.getSkill(skill).skillId == 'projects'
    }

    def "nested project associations retain the admin group's community protection"() {
        Map fixture = createCommunityEndpointFixture('admin-group', false, false)
        String groupId = fixture.adminGroup.adminGroupId
        skillsService.addProjectAdmin(projId, fixture.owner.userName)
        fixture.owner.addProjectToAdminGroup(groupId, projId)
        userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(fixture.caller.userName, 'dragons'))
        String path = "admin/admin-group-definitions/${groupId}/projects/${projId}"

        expect:
        fixture.owner.getAdminGroupProjects(groupId).assignedProjects.projectId == [projId]
        fixture.caller.getProject(projId).projectId == projId
        fixture.caller.wsHelper.rawRequest(DELETE, path, null, [view: '/projects/myProject2/']).statusCode.value() == 403
        fixture.owner.getAdminGroupProjects(groupId).assignedProjects.projectId == [projId]

        when:
        fixture.owner.deleteProjectFromAdminGroup(groupId, projId)

        then:
        fixture.owner.getAdminGroupProjects(groupId).assignedProjects == []
    }

    def "quiz caption and transcript paths retain community protection"() {
        Map fixture = createCommunityEndpointFixture('quiz')
        String path = "api/quiz-definitions/${fixture.quiz.quizId}/questions/${fixture.question.id}"
        List<Map> requests = ['videoCaptions', 'videoTranscript'].collect { String suffix -> endpoint(GET, "${path}/${suffix}") }

        expect:
        assertEndpointStatuses(fixture.owner, requests, '/quiz-definitions/TestQuiz2/', 200)
        assertEndpointStatuses(fixture.caller, requests, '/quiz-definitions/TestQuiz2/', 403)
    }

    def "encoded project paths enforce #protection access without double encoding"() {
        Map fixture = createProjectEndpointFixture(protection)
        URI uri = new URI("${fixture.owner.wsHelper.skillsService}/api/projects/%54estProject1/summary?view=/projects/myProject2/")

        when:
        ResponseEntity<String> allowed = fixture.owner.wsHelper.restTemplateWrapper.getForEntity(uri, String)
        ResponseEntity<String> denied = fixture.caller.wsHelper.restTemplateWrapper.getForEntity(uri, String)

        then:
        allowed.statusCode.value() == 200
        new JsonSlurper().parseText(allowed.body).projectName == fixture.project.name
        denied.statusCode.value() == 403
        !denied.body.contains(fixture.project.name)

        where:
        protection << ['community', 'invite-only']
    }

    def "null-like resource paths cannot bypass #protection authorization"() {
        Map fixture = createProjectEndpointFixture(protection)
        Map query = [view: "/projects/${fixture.project.projectId}/"]

        expect:
        fixture.owner.getSkillsSummaryForCurrentUser(fixture.project.projectId).projectName == fixture.project.name
        fixture.caller.wsHelper.rawGet('api/projects/null/summary', query).statusCode.value() == 403
        fixture.owner.wsHelper.rawGet('api/projects/null/summary', query).statusCode.value() == 403

        where:
        protection << ['community', 'invite-only']
    }

    def "invalid creation IDs retain controller validation for #method"() {
        List<Map> requests = [
                endpoint(method, 'app/projects/null', [:], SkillsFactory.createProject(55) + [projectId: 'null']),
                endpoint(method, 'app/quiz-definitions/null', [:], QuizDefFactory.createQuiz(55) + [quizId: 'null']),
                endpoint(method, 'app/admin-group-definitions/null', [:], AdminGroupDefFactory.createAdminGroup(55) + [adminGroupId: 'null']),
                endpoint(method, 'app/badges/null', [:], SkillsFactory.createBadge(55) + [badgeId: 'null']),
        ]

        expect:
        assertEndpointStatuses(skillsService, requests, '/projects/myProject2/', 400)

        where:
        method << [POST, PUT]
    }

    private Map createProjectEndpointFixture(String protection) {
        List<String> users = getRandomUsers(2)
        SkillsService owner = createService(users[0])
        SkillsService caller = createService(users[1])
        SkillsService root = createRootSkillService()
        boolean community = protection == 'community'
        if (community) {
            root.saveUserTag(owner.userName, 'dragons', ['DivineDragon'])
            root.saveUserTag(caller.userName, 'dragons', ['DivineDragon'])
        }
        Map accessProject = SkillsFactory.createProject(1)
        accessProject.description = 'Access project description'
        accessProject.enableProtectedUserCommunity = community
        Map subject = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(2, 1, 1)
        skills.each { Map skill ->
            skill.description = 'Access skill description'
            skill.pointIncrement = 500
            skill.numPerformToCompletion = 1
        }
        owner.createProjectAndSubjectAndSkills(accessProject, subject, skills)
        Map group = SkillsFactory.createSkillsGroup(1, 1, 3)
        owner.createSkill(group)
        Map groupSkill = SkillsFactory.createSkill(1, 1, 4)
        groupSkill.groupId = group.skillId
        groupSkill.pointIncrement = 500
        groupSkill.numPerformToCompletion = 1
        owner.createSkill(groupSkill)
        Map badge = SkillsFactory.createBadge(1, 1)
        owner.createBadge(badge)
        owner.assignSkillToBadge(accessProject.projectId, badge.badgeId, skills[0].skillId)
        badge.enabled = true
        owner.createBadge(badge)
        owner.addTagToSkills(accessProject.projectId, [skills[0].skillId], 'access')
        owner.addSkill(skills[0], owner.userName)
        owner.changeSetting(accessProject.projectId, Settings.SHOW_PROJECT_DESCRIPTION_EVERYWHERE.settingName,
                [projectId: accessProject.projectId, setting: Settings.SHOW_PROJECT_DESCRIPTION_EVERYWHERE.settingName, value: 'true'])
        if (community) {
            owner.addProjectAdmin(accessProject.projectId, caller.userName)
            // Keep the project role; only community access is removed.
            userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(caller.userName, 'dragons'))
        } else {
            owner.changeSetting(accessProject.projectId, 'invite_only',
                    [projectId: accessProject.projectId, setting: 'invite_only', value: 'true'])
        }
        return [owner: owner, caller: caller, project: accessProject, subject: subject,
                skills: skills, group: group, groupSkill: groupSkill, badge: badge]
    }

    private Map createCommunityEndpointFixture(String type, boolean withAttempts = false, boolean removeMembership = true) {
        List<String> users = getRandomUsers(2)
        SkillsService owner = createService(users[0])
        SkillsService caller = createService(users[1])
        SkillsService root = createRootSkillService()
        [owner, caller].each { SkillsService client -> root.saveUserTag(client.userName, 'dragons', ['DivineDragon']) }
        Map fixture = [owner: owner, caller: caller]
        switch (type) {
            case 'quiz':
                Map quiz = QuizDefFactory.createQuiz(1, 'Access quiz description')
                quiz.enableProtectedUserCommunity = true
                owner.createQuizDef(quiz)
                fixture.question = owner.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1)).body
                owner.addQuizUserRole(quiz.quizId, caller.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
                owner.createQuizDef(QuizDefFactory.createQuiz(2))
                fixture.quiz = quiz
                fixture.attachment = owner.uploadAttachment(
                        GroovyToJavaByteUtils.toByteArrayResource('Access quiz attachment', 'access.pdf'),
                        null, null, quiz.quizId)
                if (withAttempts) {
                    Map question = owner.getQuizQuestionDef(quiz.quizId, fixture.question.id)
                    fixture.answerId = question.answers[0].id
                    fixture.ownerAttempt = owner.startQuizAttempt(quiz.quizId).body
                    fixture.callerAttempt = caller.startQuizAttempt(quiz.quizId).body
                    owner.reportQuizAnswer(quiz.quizId, fixture.ownerAttempt.id, fixture.answerId)
                    caller.reportQuizAnswer(quiz.quizId, fixture.callerAttempt.id, fixture.answerId)
                }
                break
            case 'badge':
                Map project = SkillsFactory.createProject(1)
                project.enableProtectedUserCommunity = true
                Map subject = SkillsFactory.createSubject(1, 1)
                Map skill = SkillsFactory.createSkill(1, 1, 1)
                owner.createProjectAndSubjectAndSkills(project, subject, [skill])
                Map badge = SkillsFactory.createBadge(1, 1)
                badge.badgeId = 'globalBadge1'
                badge.enableProtectedUserCommunity = true
                owner.createGlobalBadge(badge)
                owner.assignSkillToGlobalBadge(project.projectId, badge.badgeId, skill.skillId)
                badge.enabled = true
                owner.updateGlobalBadge(badge)
                owner.grantGlobalBadgeAdminRole(badge.badgeId, caller.userName)
                Map otherBadge = SkillsFactory.createBadge(1, 2)
                otherBadge.badgeId = 'globalBadge2'
                owner.createGlobalBadge(otherBadge)
                fixture.project = project
                fixture.badge = badge
                fixture.attachment = owner.uploadAttachment(
                        GroovyToJavaByteUtils.toByteArrayResource('Access badge attachment', 'access.pdf'),
                        null, badge.badgeId)
                break
            case 'admin-group':
                Map adminGroup = AdminGroupDefFactory.createAdminGroup(1)
                adminGroup.enableProtectedUserCommunity = true
                owner.createAdminGroupDef(adminGroup)
                owner.addAdminGroupOwner(adminGroup.adminGroupId, caller.userName)
                owner.createAdminGroupDef(AdminGroupDefFactory.createAdminGroup(2))
                fixture.adminGroup = adminGroup
                break
            default:
                throw new IllegalArgumentException("Unknown access fixture [${type}]")
        }
        if (removeMembership) {
            userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(caller.userName, 'dragons'))
        }
        return fixture
    }

    private static Map endpoint(HttpMethod method, String path, Map params = [:], Object body = [:]) {
        return [method: method, path: path.toString(), params: params, body: body]
    }

    private static boolean assertEndpointStatuses(SkillsService caller, List<Map> requests, String view, int expectedStatus) {
        // Collect every outcome before asserting so one missing guard cannot hide the other routes.
        List<Map> outcomes = requests.collect { Map request ->
            Map params = new LinkedHashMap(request.params)
            if (view) { params.view = view }
            ResponseEntity<String> response = caller.wsHelper.rawRequest(request.method, request.path, request.body, params)
            [method: request.method.toString(), path: request.path, status: response.statusCode.value()]
        }
        List<Map> unexpected = outcomes.findAll { Map outcome -> outcome.status != expectedStatus }
        assert unexpected == [] : "Expected HTTP ${expectedStatus} for view [${view}]"
        return true
    }

    private static List<Map> projectReadRequests(Map fixture) {
        String projectId = fixture.project.projectId
        String subjectId = fixture.subject.subjectId
        String skillId = fixture.skills[0].skillId
        String groupId = fixture.group.skillId
        String badgeId = fixture.badge.badgeId
        String api = "api/projects/${projectId}"
        String admin = "admin/projects/${projectId}"
        Map paging = [query: '', limit: 10, page: 1, orderBy: 'userId', ascending: true,
                      minimumPoints: 0, userTagFilter: '']
        List<String> apiPaths = [
                'summary', 'level', 'navigableItems', 'pointHistory', 'rank', 'rankDistribution',
                'rankDistribution/usersPerLevel', 'leaderboard', 'customIconCss', 'dependency/graph',
                'skills/tags', "skills/${skillId}/summary", "skills/${skillId}/description",
                "skills/${skillId}/dependencies", "subjects/${subjectId}/summary",
                "subjects/${subjectId}/descriptions", "subjects/${subjectId}/pointHistory",
                "subjects/${subjectId}/rank", "subjects/${subjectId}/rankDistribution",
                "subjects/${subjectId}/rankDistribution/usersPerLevel", "subjects/${subjectId}/leaderboard",
                "subjects/${subjectId}/skills/tags", "subjects/${subjectId}/skills/${skillId}/summary",
                "groups/${groupId}/summary", "groups/${groupId}/descriptions", 'badges/summary',
                "badges/${badgeId}/summary", "badges/${badgeId}/descriptions", 'tags/summary',
                'tags/access/summary', 'tags/access/descriptions'
        ]
        List<String> adminPaths = [
                '', '/subjects', '/subjectsAndSkillsGroups', "/subjects/${subjectId}",
                "/subjects/${subjectId}/skills", "/subjects/${subjectId}/skills/${skillId}",
                "/groups/${groupId}/skills", "/groups/${groupId}/subject", '/skills', '/skillsAndBadges',
                "/skills/${skillId}", '/badges', "/badges/${badgeId}", "/badge/${badgeId}/skills",
                '/navigableItems', '/levels', "/subjects/${subjectId}/levels", '/users/count',
                '/settings', '/clientSecret', '/latestVersion', '/dependency/graph', '/shared', '/sharedWithMe',
                '/catalog/finalize/info', '/pendingFinalization/pointTotals',
                '/skills/imported/stats', '/skills/exported/stats', '/skills/tags', '/skills/tags/access',
                '/lastSkillEvent', '/validateEnablingCommunity', '/adminGroups',
                "/users/${fixture.owner.userName}/stats", "/users/${fixture.owner.userName}",
                "/${fixture.owner.userName}/canAccess", "/users/${fixture.owner.userName}/isArchived",
                "/skills/${skillId}/globalBadge/exists", "/subjects/${subjectId}/globalBadge/exists",
                '/globalBadge/exists', '/levels/1/globalBadge/exists'
        ]
        List<Map> requests = apiPaths.collect { String path -> endpoint(GET, "${api}/${path}") } +
                adminPaths.collect { String path -> endpoint(GET, "${admin}${path}") }
        ['users', "subjects/${subjectId}/users", "skills/${skillId}/users", "badges/${badgeId}/users",
         "groups/${groupId}/users", 'skills/tags/access/users'].each { String path ->
            requests.add(endpoint(GET, "${admin}/${path}", paging))
        }
        requests.addAll([
                endpoint(GET, "${admin}/performedSkills/${fixture.owner.userName}",
                        [limit: 10, page: 1, query: '', orderBy: 'performedOn', ascending: true]),
                endpoint(GET, "${admin}/skills/exported", [limit: 10, page: 1, orderBy: 'skillId', ascending: true]),
                endpoint(GET, "${admin}/skills/imported", [limit: 10, page: 1, orderBy: 'skillId', ascending: true]),
                endpoint(GET, "${admin}/projectSearch", [nameQuery: '']),
                endpoint(GET, "${admin}/users/archive", [limit: 10, page: 1, orderBy: 'userId', ascending: true, query: '']),
                endpoint(GET, "app/projects/${projectId}/description"),
                endpoint(GET, "app/projects/${projectId}/versions"),
                endpoint(GET, "app/projects/${projectId}/customIcons"),
                endpoint(GET, "api/myprojects/${projectId}/name"),
                endpoint(GET, "api/myprojects/${projectId}/isMyProject")
        ])
        return requests
    }

    private static List<Map> projectMutationRequests(Map fixture) {
        String projectId = fixture.project.projectId
        String api = "api/projects/${projectId}"
        String admin = "admin/projects/${projectId}"
        Map newSubject = SkillsFactory.createSubject(1, 2)
        Map newSkill = SkillsFactory.createSkill(1, 1, 5)
        String setting = Settings.SHOW_PROJECT_DESCRIPTION_EVERYWHERE.settingName
        return [
                endpoint(POST, "${api}/skills/${fixture.skills[0].skillId}"),
                endpoint(PUT, "${api}/skills/${fixture.skills[1].skillId}"),
                endpoint(POST, "${api}/skills/visited/${fixture.skills[0].skillId}"),
                endpoint(PUT, "${api}/skillsClientVersion", [:], [skillsClientVersion: '@skilltree/skills-client-fake-1.0.0']),
                endpoint(POST, "api/myprojects/${projectId}"),
                endpoint(DELETE, "api/myprojects/${projectId}"),
                endpoint(POST, "${admin}/subjects/${newSubject.subjectId}", [:], newSubject),
                endpoint(PUT, "${admin}/subjects/${fixture.subject.subjectId}/skills/${newSkill.skillId}", [:], newSkill),
                endpoint(POST, "${admin}/settings/${setting}", [:], [projectId: projectId, setting: setting, value: 'true']),
                endpoint(POST, "${admin}/skills/tag", [:],
                        [skillIds: [fixture.skills[0].skillId], tagId: 'accessMutation', tagValue: 'Access Mutation']),
                endpoint(DELETE, "${admin}/subjects/${fixture.subject.subjectId}/skills/${fixture.skills[1].skillId}")
        ]
    }

    def 'user cannot get project they are not an admin for'() {
        when:

        SkillsService skillsServiceUser2 = createService("newUser")
        def result = skillsServiceUser2.getProject(projId)

        then:
        !result
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'user cannot create subject for project they are not an admin for'() {
        when:

        SkillsService skillsServiceUser2 = createService("newUser")
        skillsServiceUser2.createSubject([projectId: projId, subjectId: "subject", name: "Test Subject 1"])

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }


    def 'user cannot create badge for project they are not an admin for'() {
        when:

        SkillsService skillsServiceUser2 = createService("newUser")
        Map badge = [projectId: projId, badgeId: 'badge1', name: 'Test Badge 1']
        skillsServiceUser2.createBadge(badge)

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'admin can add another user as an admin for their project'() {
        when:

        String newUser = "user2"
        SkillsService skillsServiceUser2 = createService(newUser)
        Exception expected
        try {
            // verify this 'user2' cannot see the project
            skillsServiceUser2.getProject(projId)
        } catch (Exception e) {
            expected = e
        }
        assert expected

        // add 'user2' as an admin to projId
        skillsService.addProjectAdmin(projId, newUser)

        // now verify that 'user2' can see the project
        def result = skillsServiceUser2.getProject(projId)

        then:
        true
        result
        result.projectId == projId
    }

    def 'current user cannot delete themself if they are the only admin'() {

        when:
        skillsService.deleteUserRole("skills@skills.org", projId, RoleName.ROLE_PROJECT_ADMIN.toString())

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.BAD_REQUEST
    }

    def 'current user can delete themself if they are not the only admin'() {

        String newUser = "user2"
        SkillsService skillsServiceUser2 = createService(newUser)
        // add 'user2' as an admin to projId
        skillsService.addProjectAdmin(projId, newUser)

        when:
        List<UserRole> rolesBefore = userRoleRepo.findAllByProjectIdIgnoreCase(projId)
        skillsService.deleteUserRole("skills@skills.org", projId, RoleName.ROLE_PROJECT_ADMIN.toString())
        List<UserRole> rolesAfter = userRoleRepo.findAllByProjectIdIgnoreCase(projId)

        then:
        rolesBefore.userId.sort() == ['skills@skills.org', 'user2'].sort()
        rolesAfter.userId == ['user2']
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def 'use proxy to add skill for multiple users and then get summaries and verify admin user is still logged in via http session'() {
        when:

        List<String> sampleUserIds = ['jim@email.com', 'bob@email.com']
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map subj2 = [projectId: projId, subjectId: "subj2", skillId: "skill21".toString(), name: "Test Subject 2".toString(), type: "Skill", pointIncrement: 200, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)

        skillsService.createSkill(subj1)
        skillsService.createSkill(subj2)

        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)

        // subj1 - 2 users
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(0))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(1))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(1))

        // subj2 - 1 users
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj2.skillId], sampleUserIds.get(1))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj2.skillId], sampleUserIds.get(1))

        // load summary for user1 and user2
        def summary1 = skillsService.getSkillSummaryAsProxy(sampleUserIds.get(0), projId)
        def summary2 = skillsService.getSkillSummaryAsProxy(sampleUserIds.get(1), projId)

        // now make sure the original admin user is still logged in via the http session and can hit an admin endpoint
        def adminResult = skillsService.getProject(projId)

        then:
        adminResult
        summary1
        summary1.subjects.find {it.subject == subj1.name && it.skillsLevel == 5 && it.totalPoints == 100 && it.todaysPoints == 100}
        summary1.subjects.find {it.subject == subj2.name && it.skillsLevel == 0 && it.totalPoints == 200 && it.todaysPoints == 0}
        summary2
        summary2.subjects.find {it.subject ==  subj1.name && it.skillsLevel == 5 && it.totalPoints == 100 && it.todaysPoints == 100}
        summary2.subjects.find {it.subject == subj2.name && it.skillsLevel == 5 && it.totalPoints == 200 && it.todaysPoints == 200}
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "admin cannot complete a skill for another user using proxy token"() {
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)

        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)

        when:
        def res = skillsService.addSkillAsAdminProxy([projectId: projId, skillId: subj1.skillId], skillsService.wsHelper.username, 'jim@email.com', new Date()-1)

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "root cannot call /root endpoints using proxy token"() {
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)

        SkillsService rootUser = createRootSkillService()

        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)

        def wsHelper = skillsService.wsHelper

        when:
        wsHelper.proxyApiGet(wsHelper.getTokenForUser(rootUser.userName), '/users', 'root')

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "reset client secret"() {
        when:
        List<String> sampleUserIds = ['jim@email.com', 'bob@email.com']
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        Map subj2 = [projectId: projId, subjectId: "subj2", skillId: "skill21".toString(), name: "Test Subject 2".toString(), type: "Skill", pointIncrement: 200, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSubject(subj2)

        skillsService.createSkill(subj1)
        skillsService.createSkill(subj2)

        String secretOld = skillsService.getClientSecret(projId)
        skillsService.resetClientSecret(projId)
        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)

        // subj1 - 2 users
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(0))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(1))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], sampleUserIds.get(1))

        // subj2 - 1 users
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj2.skillId], sampleUserIds.get(1))
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj2.skillId], sampleUserIds.get(1))

        // load summary for user1 and user2
        def summary1 = skillsService.getSkillSummaryAsProxy(sampleUserIds.get(0), projId)
        def summary2 = skillsService.getSkillSummaryAsProxy(sampleUserIds.get(1), projId)

        // now make sure the original admin user is still logged in via the http session and can hit an admin endpoint
        def adminResult = skillsService.getProject(projId)

        then:
        secretOld != secret
        adminResult
        summary1
        summary1.subjects.find {it.subject == subj1.name && it.skillsLevel == 5 && it.totalPoints == 100 && it.todaysPoints == 100}
        summary1.subjects.find {it.subject == subj2.name && it.skillsLevel == 0 && it.totalPoints == 200 && it.todaysPoints == 0}
        summary2
        summary2.subjects.find {it.subject ==  subj1.name && it.skillsLevel == 5 && it.totalPoints == 100 && it.todaysPoints == 100}
        summary2.subjects.find {it.subject == subj2.name && it.skillsLevel == 5 && it.totalPoints == 200 && it.todaysPoints == 200}
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "a proxied user from project A cannot use their token for a request to project B"() {
        when:
        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)
        skillsService.getSkillSummaryAsProxy('bob', projId2)

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "requesting a token without a proxy_user attribute will return an error"() {
        when:
        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], 'bob', true, false)

        then:
        SkillsClientException ex = thrown()
        ex.res['WWW-Authenticate'].contains('error="invalid_token"')
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "requesting a token without a grant_type attribute will return an error"() {
        when:
        String secret = skillsService.getClientSecret(projId)
        skillsService.setProxyCredentials(projId, secret)
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)
        skillsService.addSkillAsProxy([projectId: projId, skillId: subj1.skillId], 'bob', false, true)

        then:
        SkillsClientException ex = thrown()
        ex.res['WWW-Authenticate'].contains('error="invalid_token"')
    }

    def 'admin - user cannot get another user\'s project level if they are not an admin for said project'() {
        when:

        SkillsService skillsServiceUser2 = createService("newUser")
        skillsServiceUser2.adminGetUserLevelForProject(projId, 'aUser')

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'api - user cannot get another user\'s project level if they are not an admin for said project'() {
        when:

        SkillsService skillsServiceUser2 = createService("newUser")
        skillsServiceUser2.apiGetUserLevelForProject(projId, 'aUser')

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'must be an project admins in order to get its client secret'(){
        SkillsService skillsServiceUser2 = createService("newUser")
        when:
        skillsServiceUser2.getClientSecret(projId)
        then:
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.FORBIDDEN
    }

    def "admin can complete a skill for themself"() {
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)

        when:
        def res = skillsService.addSkill([projectId: projId, skillId: subj1.skillId])

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"
    }

    def "admin can complete a skill for another user"() {
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)

        when:
        def res = skillsService.addSkill([projectId: projId, skillId: subj1.skillId], 'jim@email.com', new Date()-1)

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"
    }

    def "non admin can complete a skill for themself"() {
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)

        SkillsService skillsServiceUser2 = createService("newUser")

        when:
        def res = skillsServiceUser2.addSkill([projectId: projId, skillId: subj1.skillId])

        then:
        res.body.skillApplied
        res.body.explanation == "Skill event was applied"
    }

    def "non admin CANNOT complete a skill for another user"() {
        Map subj1 = [projectId: projId, subjectId: "subj1", skillId: "skill11".toString(), name: "Test Subject 1".toString(), type: "Skill", pointIncrement: 100, numPerformToCompletion: 1, pointIncrementInterval: 8*60, numMaxOccurrencesIncrementInterval: 1]
        skillsService.createSubject(subj1)
        skillsService.createSkill(subj1)

        SkillsService skillsServiceUser2 = createService("newUser")
        when:
        def res = skillsServiceUser2.addSkill([projectId: projId, skillId: subj1.skillId], 'jim@email.com', new Date()-1)

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    def 'api - user cannot get another user\'s project summary if they are not an admin for said project, even though they are an admin for a different project'() {
        when:

        SkillsService skillsServiceUser2 = createService("newUser")

        // make sure skillsServiceUser2 is a project admin, but not for projId
        String projId3 = 'skillsServiceUser2Project'
        def project3 = skillsServiceUser2.createProject([projectId: projId3, name: "My Project3"]).body
        assert project3.success == true
        project3 = skillsServiceUser2.getProject(projId3)

        skillsServiceUser2.getSkillsSummaryForUser(projId, 'aUser')

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN
    }

    @Unroll
    def 'api - user cannot access another user\'s #endpointName if they are not an admin for said project, even though they are an admin for a different project'() {

        Map subj1 = [
                projectId: projId,
                subjectId: "subj1",
                name: "Test Subject 1"
        ]
        Map skill1 = [
                projectId: projId,
                subjectId: subj1.subjectId,
                skillId: "skill1",
                name: "Test Skill 1",
                type: "Skill",
                pointIncrement: 100,
                numPerformToCompletion: 1,
                pointIncrementInterval: 0,
                numMaxOccurrencesIncrementInterval: 1
        ]
        skillsService.createSubject(subj1)
        skillsService.createSkill(skill1)

        // make sure skillsServiceUser2 is a project admin, but not for projId
        SkillsService skillsServiceUser2 = createService("newUser")
        String projId3 = 'skillsServiceUser2Project'
        def project3 = skillsServiceUser2.createProject([projectId: projId3, name: "My Project3"]).body
        assert project3.success == true
        project3 = skillsServiceUser2.getProject(projId3)

        when:
        endpointCall(skillsServiceUser2, projId)

        then:
        SkillsClientException ex = thrown()
        ex.httpStatus == HttpStatus.FORBIDDEN

        where:
        endpointName                         | endpointCall
        'project summary'                    | { SkillsService svc, String otherProjectId -> svc.getSkillsSummaryForUser(otherProjectId, 'aUser') }
        'subject summary'                    | { SkillsService svc, String otherProjectId -> svc.getSubjectSummaryForUser('aUser', otherProjectId, 'subj1') }
        'skill summary'                      | { SkillsService svc, String otherProjectId -> svc.getSingleSkillSummary('aUser', otherProjectId, 'skill1') }
        'point history'                      | { SkillsService svc, String otherProjectId -> svc.getPointHistory('aUser', otherProjectId) }
        'rank'                               | { SkillsService svc, String otherProjectId -> svc.getRank('aUser', otherProjectId) }
        'rank distribution'                  | { SkillsService svc, String otherProjectId -> svc.getRankDistribution('aUser', otherProjectId) }
        'leaderboard'                        | { SkillsService svc, String otherProjectId -> svc.getLeaderboard('aUser', otherProjectId) }
        'badges summary'                     | { SkillsService svc, String otherProjectId -> svc.getBadgesSummary('aUser', otherProjectId) }
        'tags summary'                       | { SkillsService svc, String otherProjectId -> svc.wsHelper.apiGet("/projects/${otherProjectId}/tags/summary?userId=aUser") }
        'remove rejection from view'         | { SkillsService svc, String otherProjectId -> svc.removeRejectionFromView(otherProjectId, 1, 'aUser') }
    }
}
