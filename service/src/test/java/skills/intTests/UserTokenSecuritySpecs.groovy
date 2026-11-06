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
package skills.intTests

import groovy.json.JsonSlurper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef
import spock.lang.IgnoreIf

@IgnoreIf({ env['SPRING_PROFILES_ACTIVE'] == 'pki' })
class UserTokenSecuritySpecs extends DefaultIntSpec {

    def "self-token retains the requester identity with caller parameters [#query]"() {
        List<String> users = getRandomUsers(2)
        SkillsService requester = createService(users[0])
        SkillsService otherUser = createService(users[1])
        Map project = createProgressFixture(requester, otherUser)
        String parameters = query.replace('OTHER_USER', otherUser.userName).replace('REQUESTER', requester.userName)

        expect:
        requester.getSkillsSummaryForCurrentUser(project.projectId).points == 100
        otherUser.getSkillsSummaryForCurrentUser(project.projectId).points == 200

        when:
        ResponseEntity<String> tokenResponse = requester.wsHelper.restTemplateWrapper.getForEntity(
                URI.create("${requester.wsHelper.skillsService}/api/projects/${project.projectId}/token${parameters ? '?' + parameters : ''}"), String)

        then:
        tokenResponse.statusCode == HttpStatus.OK
        String token = new JsonSlurper().parseText(tokenResponse.body).access_token
        token

        when:
        ResponseEntity<String> summary = bearerGet(requester, token, "api/projects/${project.projectId}/summary")

        then:
        summary.statusCode == HttpStatus.OK
        new JsonSlurper().parseText(summary.body).points == 100

        when:
        Map result
        requester.wsHelper.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        try {
            result = requester.addSkill(SkillsFactory.createSkill(1, 1, 3))
        } finally {
            requester.wsHelper.restTemplateWrapper.authenticationToken = null
        }

        then:
        result.body.skillApplied
        requester.getSkillsSummaryForCurrentUser(project.projectId).points == 200
        otherUser.getSkillsSummaryForCurrentUser(project.projectId).points == 200
        requester.getSingleSkillSummaryForCurrentUser(project.projectId, 'skill3').points == 100
        otherUser.getSingleSkillSummaryForCurrentUser(project.projectId, 'skill3').points == 0

        where:
        query << [
                '',
                'proxy_user=OTHER_USER',
                '%70roxy_user=OTHER_USER',
                'proxy_user=OTHER_USER&proxy_user=REQUESTER',
                'proxy_user=REQUESTER&proxy_user=OTHER_USER',
                'grant_type=password',
                'grant_type=password&grant_type=client_credentials',
                'proxy_user=OTHER_USER&grant_type=password&scope=untrusted'
        ]
    }

    def "self-proxy tokens cannot access privileged or other-project endpoints for a [#role] requester"() {
        List<String> users = getRandomUsers(2)
        SkillsService requester = role == 'root' ? createRootSkillService(users[0]) : createService(users[0])
        SkillsService otherUser = createService(users[1])
        Map project = createProgressFixture(requester, otherUser)
        Map otherProject = SkillsFactory.createProject(2)
        skillsService.createProject(otherProject)
        if (role == 'admin') {
            skillsService.addProjectAdmin(project.projectId, requester.userName)
        }

        when:
        ResponseEntity<String> tokenResponse = requester.wsHelper.rawGet(
                "api/projects/${project.projectId}/token?proxy_user=${otherUser.userName}", [:])

        then:
        tokenResponse.statusCode == HttpStatus.OK
        String token = new JsonSlurper().parseText(tokenResponse.body).access_token
        bearerGet(requester, token, "api/projects/${project.projectId}/summary").statusCode == HttpStatus.OK
        bearerGet(requester, token, "admin/projects/${project.projectId}").statusCode == HttpStatus.FORBIDDEN
        bearerGet(requester, token, 'root/users').statusCode == HttpStatus.FORBIDDEN
        bearerGet(requester, token, "api/projects/${otherProject.projectId}/summary").statusCode == HttpStatus.FORBIDDEN

        where:
        role << ['learner', 'admin', 'root']
    }

    def "admin-issued token retains the path-selected identity with caller parameters [#query]"() {
        List<String> users = getRandomUsers(2)
        SkillsService selectedUser = createService(users[0])
        SkillsService otherUser = createService(users[1])
        Map project = createProgressFixture(selectedUser, otherUser)
        String parameters = query.replace('OTHER', otherUser.userName)

        when:
        ResponseEntity<String> tokenResponse = skillsService.wsHelper.rawGet(
                "admin/projects/${project.projectId}/token/${selectedUser.userName}${parameters ? '?' + parameters : ''}", [:])

        then:
        tokenResponse.statusCode == HttpStatus.OK
        String token = new JsonSlurper().parseText(tokenResponse.body).access_token
        ResponseEntity<String> summary = bearerGet(selectedUser, token, "api/projects/${project.projectId}/summary")
        summary.statusCode == HttpStatus.OK
        new JsonSlurper().parseText(summary.body).points == 100

        where:
        query << ['', 'proxy_user=OTHER', 'proxy_user=OTHER&proxy_user=OTHER&grant_type=password&grant_type=password']
    }

    private Map createProgressFixture(SkillsService requester, SkillsService otherUser) {
        Map project = SkillsFactory.createProject()
        skillsService.createProject(project)
        skillsService.createSubject(SkillsFactory.createSubject())
        Map requesterSkill = SkillsFactory.createSkill(1, 1, 1, 0, 1, 480, 100)
        Map otherUserSkill = SkillsFactory.createSkill(1, 1, 2, 0, 1, 480, 200)
        skillsService.createSkill(requesterSkill)
        skillsService.createSkill(otherUserSkill)
        Map learnerSkill = SkillsFactory.createSkill(1, 1, 3, 0, 1, 480, 100)
        learnerSkill.selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skillsService.createSkill(learnerSkill)
        skillsService.addSkill(requesterSkill, requester.userName)
        skillsService.addSkill(otherUserSkill, otherUser.userName)
        return project
    }

    private ResponseEntity<String> bearerGet(SkillsService client, String token, String path) {
        client.wsHelper.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        try {
            return client.wsHelper.rawGet(path, [:])
        } finally {
            client.wsHelper.restTemplateWrapper.authenticationToken = null
        }
    }
}
