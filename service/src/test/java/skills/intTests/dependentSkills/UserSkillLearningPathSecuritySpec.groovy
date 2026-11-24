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
package skills.intTests.dependentSkills

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.settings.Settings
import skills.storage.model.Setting
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.UserRepo
import skills.storage.repos.UserRoleRepo
import spock.lang.IgnoreIf

class UserSkillLearningPathSecuritySpec extends DefaultIntSpec {

    @Autowired
    UserRepo userRepo

    @Autowired
    UserRoleRepo userRoleRepo

    def "learner cannot request another user's #endpoint"() {
        Map fixture = createLearningPathFixture()
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        SkillsService otherUser = createService(users[1])
        skillsService.addSkill(fixture.skills[0], otherUser.userName, new Date())
        String path = "api/projects/${fixture.project.projectId}/skills/${fixture.skills[1].skillId}/${endpoint}"

        expect:
        Map ownPath = learner.wsHelper.apiGet("/projects/${fixture.project.projectId}/skills/${fixture.skills[1].skillId}/${endpoint}")
        ownPath.dependencies.find { Map item -> item.dependsOn.skillId == fixture.skills[0].skillId && item.dependsOn.projectId == fixture.project.projectId }.achieved == false

        when:
        ResponseEntity<String> denied = learner.wsHelper.rawGet(path, [userId: otherUser.userName, idType: 'ID'])

        then:
        denied.statusCode == HttpStatus.FORBIDDEN
        !parseBody(denied).containsKey('dependencies')

        where:
        endpoint << ['learningPath', 'dependencies']
    }

    def "#endpoint protects the viewing #protection project"() {
        Map fixture = createLearningPathFixture()
        protectProject(fixture.project.projectId, protection)
        SkillsService learner = createService(getRandomUsers(1)[0])

        when:
        ResponseEntity<String> denied = learner.wsHelper.rawGet(
                "api/projects/${fixture.project.projectId}/skills/${fixture.skills[1].skillId}/${endpoint}", [:])

        then:
        denied.statusCode == HttpStatus.FORBIDDEN
        !parseBody(denied).containsKey('dependencies')

        where:
        endpoint       | protection
        'learningPath' | 'invite-only'
        'dependencies' | 'invite-only'
        'learningPath' | 'community'
        'dependencies' | 'community'
    }

    def "inaccessible shared sources have the same response for related unrelated and nonexistent skills"() {
        Map fixture = createLearningPathFixture()
        protectProject(fixture.sourceProject.projectId, 'invite-only')
        SkillsService learner = createService(getRandomUsers(1)[0])

        when:
        List<ResponseEntity<String>> responses = [fixture.sourceSkills[0].skillId, fixture.sourceSkills[1].skillId, 'missingSkill'].collect { String skillId ->
            learner.wsHelper.rawGet("api/projects/${fixture.project.projectId}/skills/${skillId}/learningPath",
                    [crossProjectId: fixture.sourceProject.projectId])
        }

        then:
        responses.every { ResponseEntity<String> response -> response.statusCode == HttpStatus.FORBIDDEN }
        responses.collect { ResponseEntity<String> response -> parseBody(response).explanation }.unique() == ['Access Denied']
        responses.every { ResponseEntity<String> response -> !parseBody(response).containsKey('nextItems') }
    }

    def "shared source access follows current #role membership"() {
        Map fixture = createLearningPathFixture()
        protectProject(fixture.sourceProject.projectId, 'invite-only')
        SkillsService learner = createService(getRandomUsers(1)[0])
        String path = "api/projects/${fixture.project.projectId}/skills/${fixture.sourceSkills[0].skillId}/learningPath"
        Map query = [crossProjectId: fixture.sourceProject.projectId]

        expect:
        learner.wsHelper.rawGet(path, query).statusCode == HttpStatus.FORBIDDEN

        when:
        grantSourceRole(learner, fixture.sourceProject.projectId, role)
        ResponseEntity<String> allowed = learner.wsHelper.rawGet(path, query)

        then:
        allowed.statusCode == HttpStatus.OK
        !parseBody(allowed).dependencies
        parseBody(allowed).nextItems*.skillId == [fixture.skills[1].skillId]
        parseBody(allowed).nextItems*.projectId == [fixture.project.projectId]

        when:
        skillsService.deleteUserRole(learner.getUsername(learner.userName).toLowerCase(), fixture.sourceProject.projectId, role.toString())

        then:
        learner.wsHelper.rawGet(path, query).statusCode == HttpStatus.FORBIDDEN

        where:
        role << [RoleName.ROLE_PRIVATE_PROJECT_USER, RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER]
    }

    def "accessible but unrelated shared skills are rejected"() {
        Map fixture = createLearningPathFixture()
        SkillsService learner = createService(getRandomUsers(1)[0])

        when:
        ResponseEntity<String> denied = learner.wsHelper.rawGet(
                "api/projects/${fixture.project.projectId}/skills/${fixture.sourceSkills[1].skillId}/learningPath",
                [crossProjectId: fixture.sourceProject.projectId])

        then:
        denied.statusCode == HttpStatus.BAD_REQUEST
        parseBody(denied).errorCode == 'BadParam'
        parseBody(denied).projectId == fixture.project.projectId
        parseBody(denied).skillId == fixture.sourceSkills[1].skillId
    }

    def "global badge membership does not bypass #protection source protection"() {
        Map fixture = createLearningPathFixture(true)
        protectProject(fixture.sourceProject.projectId, protection, true)
        SkillsService learner = createService(getRandomUsers(1)[0])
        String path = "api/projects/${fixture.project.projectId}/skills/${fixture.sourceSkills[0].skillId}/learningPath"
        Map query = [crossProjectId: fixture.sourceProject.projectId]

        when:
        ResponseEntity<String> denied = learner.wsHelper.rawGet(path, query)

        then:
        denied.statusCode == HttpStatus.FORBIDDEN
        !parseBody(denied).containsKey('nextItems')

        when:
        if (protection == 'invite-only') {
            grantSourceRole(learner, fixture.sourceProject.projectId, RoleName.ROLE_PRIVATE_PROJECT_USER)
        } else {
            createRootSkillService().saveUserTag(learner.userName, 'dragons', ['DivineDragon'])
        }
        ResponseEntity<String> allowed = learner.wsHelper.rawGet(path, query)

        then:
        allowed.statusCode == HttpStatus.OK
        !parseBody(allowed).dependencies
        !parseBody(allowed).nextItems

        where:
        protection << ['invite-only', 'community']
    }

    def "#endpoint filters restricted #protection prerequisites while retaining local prerequisites"() {
        Map fixture = createLearningPathFixture()
        protectProject(fixture.sourceProject.projectId, protection)
        SkillsService learner = createService(getRandomUsers(1)[0])

        when:
        Map result = learner.wsHelper.apiGet("/projects/${fixture.project.projectId}/skills/${fixture.skills[1].skillId}/${endpoint}")

        then:
        result.dependencies*.dependsOn*.projectId == [fixture.project.projectId]
        result.dependencies*.dependsOn*.skillId == [fixture.skills[0].skillId]

        where:
        endpoint       | protection
        'learningPath' | 'invite-only'
        'dependencies' | 'invite-only'
        'learningPath' | 'community'
        'dependencies' | 'community'
    }

    @IgnoreIf({ env['SPRING_PROFILES_ACTIVE'] == 'pki' })
    def "proxied #role identities cannot inherit private source roles"() {
        Map fixture = createLearningPathFixture()
        protectProject(fixture.sourceProject.projectId, 'invite-only')
        String userId = getRandomUsers(1)[0]
        SkillsService learner = role == RoleName.ROLE_SUPER_DUPER_USER ? createRootSkillService(userId) : createService(userId)
        if (role != RoleName.ROLE_SUPER_DUPER_USER) {
            grantSourceRole(learner, fixture.sourceProject.projectId, role)
        }
        String sharedPath = "api/projects/${fixture.project.projectId}/skills/${fixture.sourceSkills[0].skillId}/learningPath"
        Map query = [crossProjectId: fixture.sourceProject.projectId]

        expect:
        learner.wsHelper.rawGet(sharedPath, query).statusCode == HttpStatus.OK

        when:
        skillsService.setProxyCredentials(fixture.project.projectId, skillsService.getClientSecret(fixture.project.projectId))
        String token = skillsService.wsHelper.getTokenForUser(learner.userName)
        ResponseEntity<String> contextResponse = bearerGet(skillsService, token, "api/projects/${fixture.project.projectId}/summary")
        ResponseEntity<String> sharedResponse = bearerGet(skillsService, token, sharedPath, query)
        ResponseEntity<String> localResponse = bearerGet(skillsService, token,
                "api/projects/${fixture.project.projectId}/skills/${fixture.skills[1].skillId}/learningPath")
        ResponseEntity<String> legacyResponse = bearerGet(skillsService, token,
                "api/projects/${fixture.project.projectId}/skills/${fixture.skills[1].skillId}/dependencies")

        then:
        contextResponse.statusCode == HttpStatus.OK
        sharedResponse.statusCode == HttpStatus.FORBIDDEN
        localResponse.statusCode == HttpStatus.OK
        parseBody(localResponse).dependencies*.dependsOn*.projectId == [fixture.project.projectId]
        legacyResponse.statusCode == HttpStatus.OK
        parseBody(legacyResponse).dependencies*.dependsOn*.projectId == [fixture.project.projectId]

        where:
        role << [RoleName.ROLE_PRIVATE_PROJECT_USER, RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER, RoleName.ROLE_SUPER_DUPER_USER]
    }

    def "nested badge prerequisites use the viewing project in #endpoint"() {
        Map fixture = createNestedBadgeFixture()
        SkillsService learner = createService(getRandomUsers(1)[0])

        when:
        Map result = learner.wsHelper.apiGet("/projects/${fixture.projects[0].projectId}/skills/${fixture.skills[0][1].skillId}/${endpoint}")

        then:
        result.dependencies.collect { Map relationship ->
            [relationship.skill.projectId, relationship.skill.skillId, relationship.dependsOn.projectId, relationship.dependsOn.skillId]
        }.toSet() == [
                [fixture.projects[0].projectId, fixture.skills[0][1].skillId, fixture.projects[0].projectId, fixture.skills[0][0].skillId],
                [fixture.projects[0].projectId, fixture.skills[0][1].skillId, fixture.projects[1].projectId, fixture.skills[1][0].skillId],
                [fixture.projects[1].projectId, fixture.skills[1][0].skillId, fixture.projects[2].projectId, fixture.skills[2][0].skillId],
                [fixture.projects[2].projectId, fixture.skills[2][0].skillId, fixture.projects[2].projectId, fixture.skills[2][1].skillId]
        ].toSet()
        result.dependencies.every { Map relationship ->
            relationship.crossProject == (relationship.dependsOn.projectId != fixture.projects[0].projectId)
        }

        where:
        endpoint << ['learningPath', 'dependencies']
    }

    def "#endpoint removes orphan branches through a #protection intermediate project"() {
        Map fixture = createNestedBadgeFixture()
        SkillsService learner = createService(getRandomUsers(1)[0])
        String path = "/projects/${fixture.projects[0].projectId}/skills/${fixture.skills[0][1].skillId}/${endpoint}"

        expect:
        learner.wsHelper.apiGet(path).dependencies.size() == 4

        when:
        protectProject(fixture.projects[1].projectId, protection)
        Map denied = learner.wsHelper.apiGet(path)

        then:
        denied.dependencies*.skill*.skillId == [fixture.skills[0][1].skillId]
        denied.dependencies*.dependsOn*.skillId == [fixture.skills[0][0].skillId]
        denied.dependencies*.dependsOn*.projectId == [fixture.projects[0].projectId]
        denied.dependencies*.crossProject == [false]

        when:
        grantIntermediateAccess(learner, fixture.projects[1].projectId, protection)
        Map restored = learner.wsHelper.apiGet(path)

        then:
        restored.dependencies.size() == 4
        restored.dependencies.find { Map relationship -> relationship.dependsOn.skillId == fixture.skills[2][1].skillId }.dependsOn.projectId == fixture.projects[2].projectId

        where:
        endpoint       | protection
        'learningPath' | 'invite-only'
        'dependencies' | 'invite-only'
        'learningPath' | 'community'
        'dependencies' | 'community'
    }

    def "indirect shared prerequisites require a visible chain through a #protection intermediate"() {
        Map fixture = createNestedBadgeFixture()
        SkillsService learner = createService(getRandomUsers(1)[0])
        String path = "api/projects/${fixture.projects[0].projectId}/skills/${fixture.skills[2][0].skillId}/learningPath"
        Map query = [crossProjectId: fixture.projects[2].projectId]

        when:
        ResponseEntity<String> allowed = learner.wsHelper.rawGet(path, query)
        ResponseEntity<String> unrelated = learner.wsHelper.rawGet(
                "api/projects/${fixture.projects[0].projectId}/skills/${fixture.skills[2][2].skillId}/learningPath", query)
        List<ResponseEntity<String>> summaries = [false, true].collect { boolean withSubject ->
            loadIndirectSummary(learner, fixture, 0, withSubject)
        }
        ResponseEntity<String> unrelatedSummary = loadIndirectSummary(learner, fixture, 2, true)

        then:
        allowed.statusCode == HttpStatus.OK
        !parseBody(allowed).dependencies
        !parseBody(allowed).nextItems
        unrelated.statusCode == HttpStatus.BAD_REQUEST
        parseBody(unrelated).errorCode == 'BadParam'
        summaries.every { ResponseEntity<String> response ->
            response.statusCode == HttpStatus.OK && parseBody(response).skillId == fixture.skills[2][0].skillId &&
                    parseBody(response).projectId == fixture.projects[2].projectId && parseBody(response).crossProject
        }
        unrelatedSummary.statusCode == HttpStatus.BAD_REQUEST

        when:
        protectProject(fixture.projects[1].projectId, protection)
        ResponseEntity<String> hidden = learner.wsHelper.rawGet(path, query)
        List<ResponseEntity<String>> hiddenSummaries = [false, true].collect { boolean withSubject ->
            loadIndirectSummary(learner, fixture, 0, withSubject)
        }

        then:
        hidden.statusCode == HttpStatus.BAD_REQUEST
        parseBody(hidden).explanation == parseBody(unrelated).explanation
        hiddenSummaries.every { ResponseEntity<String> response -> response.statusCode == HttpStatus.BAD_REQUEST }

        when:
        grantIntermediateAccess(learner, fixture.projects[1].projectId, protection)
        ResponseEntity<String> restored = learner.wsHelper.rawGet(path, query)
        ResponseEntity<String> nestedLeaf = learner.wsHelper.rawGet(
                "api/projects/${fixture.projects[0].projectId}/skills/${fixture.skills[2][1].skillId}/learningPath", query)
        ResponseEntity<String> restoredSummary = loadIndirectSummary(learner, fixture, 0, true)
        ResponseEntity<String> leafSummary = loadIndirectSummary(learner, fixture, 1, false)

        then:
        restored.statusCode == HttpStatus.OK
        nestedLeaf.statusCode == HttpStatus.OK
        !parseBody(restored).dependencies
        !parseBody(nestedLeaf).dependencies
        restoredSummary.statusCode == HttpStatus.OK
        parseBody(restoredSummary).skill == fixture.skills[2][0].name
        leafSummary.statusCode == HttpStatus.OK
        parseBody(leafSummary).skill == fixture.skills[2][1].name

        when:
        if (protection == 'invite-only') {
            skillsService.deleteUserRole(learner.getUsername(learner.userName).toLowerCase(), fixture.projects[1].projectId,
                    RoleName.ROLE_PRIVATE_PROJECT_USER.toString())
        } else {
            // The tag API only adds tags; simulate membership revocation through the scoped repository.
            String userId = learner.getUsername(learner.userName).toLowerCase()
            userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(userId, 'dragons'))
        }

        then:
        learner.wsHelper.rawGet(path, query).statusCode == HttpStatus.BAD_REQUEST
        loadIndirectSummary(learner, fixture, 0, true).statusCode == HttpStatus.BAD_REQUEST

        where:
        protection << ['invite-only', 'community']
    }

    @IgnoreIf({ env['SPRING_PROFILES_ACTIVE'] == 'pki' })
    def "proxied identities cannot traverse an indirect private prerequisite chain"() {
        Map fixture = createNestedBadgeFixture()
        protectProject(fixture.projects[1].projectId, 'invite-only')
        SkillsService learner = createService(getRandomUsers(1)[0])
        grantSourceRole(learner, fixture.projects[1].projectId, RoleName.ROLE_PRIVATE_PROJECT_USER)
        String path = "api/projects/${fixture.projects[0].projectId}/skills/${fixture.skills[2][0].skillId}/learningPath"
        Map query = [crossProjectId: fixture.projects[2].projectId]

        expect:
        learner.wsHelper.rawGet(path, query).statusCode == HttpStatus.OK

        when:
        skillsService.setProxyCredentials(fixture.projects[0].projectId, skillsService.getClientSecret(fixture.projects[0].projectId))
        String token = skillsService.wsHelper.getTokenForUser(learner.userName)
        ResponseEntity<String> context = bearerGet(skillsService, token, "api/projects/${fixture.projects[0].projectId}/summary")
        ResponseEntity<String> indirect = bearerGet(skillsService, token, path, query)
        String summaryPath = "api/projects/${fixture.projects[0].projectId}/projects/${fixture.projects[2].projectId}/skills/${fixture.skills[2][0].skillId}/summary"
        ResponseEntity<String> indirectSummary = bearerGet(skillsService, token, summaryPath)

        then:
        context.statusCode == HttpStatus.OK
        indirect.statusCode == HttpStatus.BAD_REQUEST
        indirectSummary.statusCode == HttpStatus.BAD_REQUEST
    }

    private static ResponseEntity<String> loadIndirectSummary(SkillsService learner, Map fixture, int skillIndex, boolean withSubject) {
        String subjectPath = withSubject ? "subjects/${fixture.skills[2][skillIndex].subjectId}/" : ''
        return learner.wsHelper.rawGet(
                "api/projects/${fixture.projects[0].projectId}/projects/${fixture.projects[2].projectId}/${subjectPath}skills/${fixture.skills[2][skillIndex].skillId}/summary", [:])
    }

    private Map createNestedBadgeFixture() {
        List<Map> projects = (1..3).collect { int i -> SkillsFactory.createProject(i) }
        List<List<Map>> skills = [SkillsFactory.createSkills(2, 1, 1, 100),
                                 SkillsFactory.createSkills(2, 2, 1, 100),
                                 SkillsFactory.createSkillsStartingAt(3, 3, 3, 1, 100)]
        (0..2).each { int i ->
            skillsService.createProjectAndSubjectAndSkills(projects[i], SkillsFactory.createSubject(i + 1, 1), skills[i])
        }
        skillsService.addLearningPathPrerequisite(projects[0].projectId, skills[0][1].skillId, skills[0][0].skillId)

        Map publicBadge = SkillsFactory.createBadge(3, 1)
        skillsService.createBadge(publicBadge)
        skillsService.assignSkillToBadge([projectId: projects[2].projectId, badgeId: publicBadge.badgeId, skillId: skills[2][0].skillId])
        publicBadge.enabled = true
        skillsService.createBadge(publicBadge)
        skillsService.addLearningPathPrerequisite(projects[2].projectId, publicBadge.badgeId, skills[2][1].skillId)

        Map intermediateBadge = SkillsFactory.createBadge(2, 1)
        skillsService.createBadge(intermediateBadge)
        skillsService.assignSkillToBadge([projectId: projects[1].projectId, badgeId: intermediateBadge.badgeId, skillId: skills[1][0].skillId])
        intermediateBadge.enabled = true
        skillsService.createBadge(intermediateBadge)
        skillsService.shareSkill(projects[2].projectId, skills[2][0].skillId, projects[1].projectId)
        skillsService.addLearningPathPrerequisite(projects[1].projectId, intermediateBadge.badgeId, projects[2].projectId, skills[2][0].skillId)

        skillsService.shareSkill(projects[1].projectId, skills[1][0].skillId, projects[0].projectId)
        skillsService.addLearningPathPrerequisite(projects[0].projectId, skills[0][1].skillId, projects[1].projectId, skills[1][0].skillId)
        return [projects: projects, skills: skills]
    }

    private void grantIntermediateAccess(SkillsService learner, String projectId, String protection) {
        if (protection == 'invite-only') {
            grantSourceRole(learner, projectId, RoleName.ROLE_PRIVATE_PROJECT_USER)
        } else {
            createRootSkillService().saveUserTag(learner.userName, 'dragons', ['DivineDragon'])
        }
    }

    private Map createLearningPathFixture(boolean viaGlobalBadge = false) {
        Map project = SkillsFactory.createProject(1)
        List<Map> skills = SkillsFactory.createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(project, SkillsFactory.createSubject(1, 1), skills)
        skillsService.addLearningPathPrerequisite(project.projectId, skills[1].skillId, skills[0].skillId)
        Map sourceProject = SkillsFactory.createProject(2)
        List<Map> sourceSkills = SkillsFactory.createSkills(2, 2, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(sourceProject, SkillsFactory.createSubject(2, 1), sourceSkills)
        skillsService.addLearningPathPrerequisite(sourceProject.projectId, sourceSkills[1].skillId, sourceSkills[0].skillId)
        if (viaGlobalBadge) {
            SkillsService root = createRootSkillService()
            Map badge = SkillsFactory.createBadge()
            root.createGlobalBadge(badge)
            root.assignSkillToGlobalBadge(project.projectId, badge.badgeId, skills[0].skillId)
            root.assignSkillToGlobalBadge(sourceProject.projectId, badge.badgeId, sourceSkills[0].skillId)
            badge.enabled = true
            root.updateGlobalBadge(badge)
        } else {
            skillsService.shareSkill(sourceProject.projectId, sourceSkills[0].skillId, project.projectId)
            skillsService.addLearningPathPrerequisite(project.projectId, skills[1].skillId, sourceProject.projectId, sourceSkills[0].skillId)
        }
        return [project: project, skills: skills, sourceProject: sourceProject, sourceSkills: sourceSkills]
    }

    private void protectProject(String projectId, String protection, boolean existingGlobalBadge = false) {
        if (protection == 'invite-only' && !existingGlobalBadge) {
            skillsService.changeSetting(projectId, 'invite_only', [projectId: projectId, setting: 'invite_only', value: 'true'])
        } else {
            // Sharing and global badges restrict visibility changes through the admin API.
            // Simulate protection on an existing relationship to exercise read-time authorization.
            settingRepo.save(new Setting(type: Setting.SettingType.Project, projectId: projectId,
                    setting: protection == 'invite-only' ? 'invite_only' : Settings.USER_COMMUNITY_ONLY_PROJECT.settingName, value: 'true'))
        }
    }

    private void grantSourceRole(SkillsService learner, String projectId, RoleName role) {
        if (role == RoleName.ROLE_PRIVATE_PROJECT_USER) {
            // This role is issued by accepting an invitation, not the admin role endpoint.
            // Use a scoped repository fixture, as in UserLearningPathGraphSpec.
            String userId = learner.getUsername(learner.userName).toLowerCase()
            userRoleRepo.save(new UserRole(userId: userId, userRefId: userRepo.findByUserId(userId).id,
                    projectId: projectId, roleName: role))
        } else {
            skillsService.addUserRole(learner.userName, projectId, role.toString())
        }
    }

    private static Map parseBody(ResponseEntity<String> response) {
        return new JsonSlurper().parseText(response.body) as Map
    }

    private static ResponseEntity<String> bearerGet(SkillsService client, String token, String path, Map query = [:]) {
        client.wsHelper.restTemplateWrapper.authenticationToken = "Bearer ${token}"
        try {
            return client.wsHelper.rawGet(path, query)
        } finally {
            client.wsHelper.restTemplateWrapper.authenticationToken = null
        }
    }
}
