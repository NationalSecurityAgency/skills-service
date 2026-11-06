/**
 * Copyright 2026 SkillTree
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
package skills.intTests.approverRole

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.ProjectExpirationService
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName
import spock.lang.IgnoreIf

class ApproverProjectAuthorizationSpecs extends DefaultIntSpec {

    @Autowired
    ProjectExpirationService expirationService

    def "approver cannot read project credentials with invite-only [#inviteOnly]"() {
        Map project = SkillsFactory.createProject()
        skillsService.createProject(project)
        SkillsService approver = createApprover(project.projectId, inviteOnly)
        String clientSecret = skillsService.getClientSecret(project.projectId)

        when:
        ResponseEntity<String> response = approver.wsHelper.rawGet("admin/projects/${project.projectId}/clientSecret", [:])

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        !response.body?.contains(clientSecret)
        skillsService.getClientSecret(project.projectId) == clientSecret

        where:
        inviteOnly << [false, true]
    }

    @IgnoreIf({ env['SPRING_PROFILES_ACTIVE'] == 'pki' })
    def "approver cannot obtain another user's writable token"() {
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        SkillsService approver = createService(users[1])
        Map project = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill(1, 1, 1, 0, 1, 480, 100)
        skill.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skillsService.createProjectAndSubjectAndSkills(project, subject, [skill])
        skillsService.addUserRole(approver.userName, project.projectId, RoleName.ROLE_PROJECT_APPROVER.name())

        expect:
        learner.getSkillsSummaryForCurrentUser(project.projectId).points == 0

        when:
        ResponseEntity<String> response = approver.wsHelper.rawGet(
                "admin/projects/${project.projectId}/token/${learner.userName}", [:])

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        !response.body?.contains('"access_token"')
        learner.getSkillsSummaryForCurrentUser(project.projectId).points == 0
        learner.getSingleSkillSummaryForCurrentUser(project.projectId, skill.skillId).points == 0
        approver.getSkillSummary(learner.userName, project.projectId).points == 0
    }

    def "approver cannot cancel project expiration using [#method] with invite-only [#inviteOnly]"() {
        Map project = SkillsFactory.createProject()
        skillsService.createProject(project)
        SkillsService approver = createApprover(project.projectId, inviteOnly)
        expirationService.flagOldProjects(new Date() + 1)
        Map initialProject = skillsService.getProject(project.projectId)

        expect:
        initialProject.expiring
        initialProject.expirationTriggered

        when:
        ResponseEntity<String> response = approver.wsHelper.rawRequest(method,
                "admin/projects/${project.projectId}/cancelExpiration")

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        Map unchangedProject = skillsService.getProject(project.projectId)
        unchangedProject.expiring
        unchangedProject.expirationTriggered == initialProject.expirationTriggered

        where:
        method          | inviteOnly
        HttpMethod.GET  | false
        HttpMethod.GET  | true
        HttpMethod.POST | false
        HttpMethod.POST | true
    }

    def "[#role] can read credentials and cancel expiration only using POST"() {
        Map project = SkillsFactory.createProject()
        skillsService.createProject(project)
        SkillsService administrator = role == 'root' ? createRootSkillService(getRandomUsers(1)[0]) : skillsService
        String clientSecret = skillsService.getClientSecret(project.projectId)
        expirationService.flagOldProjects(new Date() + 1)
        Map initialProject = skillsService.getProject(project.projectId)

        expect:
        initialProject.expiring
        administrator.getClientSecret(project.projectId) == clientSecret

        when:
        ResponseEntity<String> getResponse = administrator.wsHelper.rawGet(
                "admin/projects/${project.projectId}/cancelExpiration", [:])

        then:
        getResponse.statusCode == HttpStatus.METHOD_NOT_ALLOWED
        skillsService.getProject(project.projectId).expiring
        skillsService.getProject(project.projectId).expirationTriggered == initialProject.expirationTriggered

        when:
        ResponseEntity<String> postResponse = administrator.wsHelper.rawRequest(HttpMethod.POST,
                "admin/projects/${project.projectId}/cancelExpiration")

        then:
        postResponse.statusCode == HttpStatus.OK
        new JsonSlurper().parseText(postResponse.body).success
        !skillsService.getProject(project.projectId).expiring

        where:
        role << ['admin', 'root']
    }

    @IgnoreIf({ env['SPRING_PROFILES_ACTIVE'] == 'pki' })
    def "[#role] can issue a project-scoped writable user token"() {
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        SkillsService administrator = role == 'root' ? createRootSkillService(users[1]) : skillsService
        Map project = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        Map skill = SkillsFactory.createSkill(1, 1, 1, 0, 1, 480, 100)
        skill.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skillsService.createProjectAndSubjectAndSkills(project, subject, [skill])

        when:
        ResponseEntity<String> response = administrator.wsHelper.rawGet(
                "admin/projects/${project.projectId}/token/${learner.userName}", [:])

        then:
        response.statusCode == HttpStatus.OK
        String token = new JsonSlurper().parseText(response.body).access_token
        token

        when:
        Map result
        administrator.wsHelper.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        try {
            result = administrator.addSkill(skill)
        } finally {
            administrator.wsHelper.restTemplateWrapper.authenticationToken = null
        }

        then:
        result.body.skillApplied
        learner.getSkillsSummaryForCurrentUser(project.projectId).points == 100
        learner.getSingleSkillSummaryForCurrentUser(project.projectId, skill.skillId).points == 100

        where:
        role << ['admin', 'root']
    }

    private SkillsService createApprover(String projectId, boolean inviteOnly) {
        SkillsService approver = createService(getRandomUsers(1)[0])
        skillsService.addUserRole(approver.userName, projectId, RoleName.ROLE_PROJECT_APPROVER.name())
        if (inviteOnly) {
            skillsService.configuredProjectAsInviteOnly(projectId)
        }
        return approver
    }
}
