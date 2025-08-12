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
package skills.intTests.badges

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.repos.UserRoleRepo

import static skills.intTests.utils.SkillsFactory.*

class GlobalBadgeAccessSpecs extends DefaultIntSpec {

    @Autowired
    UserRoleRepo userRoleRepo

    def "getAllGlobalBadges should only returns badges the current user is an admin for"() {
        // Create first user and a global badge
        def user1Service = createService("user1")
        def badge1 = createBadge(1, 1)
        user1Service.createGlobalBadge(badge1)

        // Create second user and global badge
        def user2Service = createService("user2")
        def badge2 = createBadge(1, 2)
        user2Service.createGlobalBadge(badge2)

        when:
        def user1Badges = user1Service.getAllGlobalBadges()
        def user2Badges = user2Service.getAllGlobalBadges()

        then:
        user1Badges.size() == 1
        user1Badges[0].badgeId == badge1.badgeId

        user2Badges.size() == 1
        user2Badges[0].badgeId == badge2.badgeId
    }

    def "getAvailableProjectsForGlobalBadge should only return projects the current user is an admin for"() {
        // Create first user and project
        def user1Service = createService("user1")
        def proj1 = createProject(1)
        user1Service.createProject(proj1)

        def badge1 = createBadge(1, 1)
        user1Service.createGlobalBadge(badge1)
        
        // Create second user and project
        def user2Service = createService("user2")
        def proj2 = createProject(2)
        user2Service.createProject(proj2)

        def badge2 = createBadge(1, 2)
        user2Service.createGlobalBadge(badge2)
        
        when:
        def availableProjectsBadge1 = user1Service.getAvailableProjectsForGlobalBadge(badge1.badgeId)
        def availableProjectsBadge2 = user2Service.getAvailableProjectsForGlobalBadge(badge2.badgeId)
        
        then:
        availableProjectsBadge1.projects.size() == 1
        availableProjectsBadge1.projects[0].projectId == proj1.projectId

        availableProjectsBadge2.projects.size() == 1
        availableProjectsBadge2.projects[0].projectId == proj2.projectId

        // Verify users don't see each other's projects
        !availableProjectsBadge1.projects.any { it.projectId == proj2.projectId }
        !availableProjectsBadge2.projects.any { it.projectId == proj1.projectId }

        // Verify users don't see each other's badges
        !availableProjectsBadge1.badges.any { it.badgeId == badge2.badgeId }
        !availableProjectsBadge2.badges.any { it.badgeId == badge1.badgeId }
    }

    def "getAvailableProjectsForGlobalBadge should return projects root pinned or project where they are an admin of"() {
        // Create first user and project
        def user1Service = createService("user1")
        def proj1 = createProject(1)
        user1Service.createProject(proj1)

        def badge1 = createBadge(1, 1)
        user1Service.createGlobalBadge(badge1)

        // Create second user and projects
        def user2Service = createService("user2")
        def proj2 = createProject(2)
        user2Service.createProject(proj2)

        def proj3 = createProject(3)
        user2Service.createProject(proj3)

        SkillsService rootUser = createRootSkillService()
        rootUser.pinProject(proj1.projectId)
        rootUser.pinProject(proj3.projectId)

        when:
        def availableProjectsForRootUser = rootUser.getAvailableProjectsForGlobalBadge(badge1.badgeId)

        then:
        availableProjectsForRootUser.projects.size() == 2
        availableProjectsForRootUser.projects.find { it.projectId == proj1.projectId }
        availableProjectsForRootUser.projects.find { it.projectId == proj3.projectId }
    }

    def "getAvailableSkillsForGlobalBadge should only return skills the current user is an admin for"() {
        // Create first user and skills
        def user1Service = createService("user1")
        def proj1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        user1Service.createProjectAndSubjectAndSkills(proj1, p1subj1, p1Skills)

        def badge1 = createBadge(1, 1)
        user1Service.createGlobalBadge(badge1)

        // Create second user and skills
        def user2Service = createService("user2")
        def proj2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(10, 2, 1, 100)
        user2Service.createProjectAndSubjectAndSkills(proj2, p2subj1, p2Skills)

        def badge2 = createBadge(1, 2)
        user2Service.createGlobalBadge(badge2)

        when:
        def availableSkillsBadge1 = user1Service.getAvailableSkillsForGlobalBadge(badge1.badgeId, "")
        def availableSkillsBadge2 = user2Service.getAvailableSkillsForGlobalBadge(badge2.badgeId, "")

        then:
        availableSkillsBadge1.suggestedSkills.size() == 10
        availableSkillsBadge1.suggestedSkills.findAll { it.projectId == proj1.projectId }.size() == 10

        availableSkillsBadge2.suggestedSkills.size() == 10
        availableSkillsBadge2.suggestedSkills.findAll { it.projectId == proj2.projectId }.size() == 10
    }

    def "user cannot update a global badge that they do not own"() {
        // Create first user and badge
        def user1Service = createService("user1")
        def badge1 = createBadge(1, 1)
        user1Service.createGlobalBadge(badge1)

        // Create second user
        def user2Service = createService("user2")

        when:
        user2Service.updateGlobalBadge(badge1)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "user cannot getAvailableProjectsForGlobalBadge for a global badge that they do not own"() {
        // Create first user and badge
        def user1Service = createService("user1")
        def badge1 = createBadge(1, 1)
        user1Service.createGlobalBadge(badge1)

        // Create second user
        def user2Service = createService("user2")

        when:
        user2Service.getAvailableProjectsForGlobalBadge(badge1.badgeId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "user cannot getAvailableSkillsForGlobalBadge for a global badge that they do not own"() {
        // Create first user and badge
        def user1Service = createService("user1")
        def badge1 = createBadge(1, 1)
        user1Service.createGlobalBadge(badge1)

        // Create second user
        def user2Service = createService("user2")

        when:
        user2Service.getAvailableSkillsForGlobalBadge(badge1.badgeId, "")

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "user cannot assignProjectLevelToGlobalBadge to a global badge that they do not own"() {
        // Create first user and badge
        def user1Service = createService("user1")
        def badge1 = createBadge(1, 1)
        user1Service.createGlobalBadge(badge1)

        // Create second user
        def user2Service = createService("user2")
        def proj2 = createProject(2)
        user2Service.createProject(proj2)

        when:
        user2Service.assignProjectLevelToGlobalBadge(projectId: proj2.projectId, badgeId: badge1.badgeId, level: "1")

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "global badge admins cannot add a skill to a global badge for a project that they do not own"() {
        // Create first user and global badge
        def user1Service = createService("user1")
        def badge1 = createBadge(1, 1)
        user1Service.createGlobalBadge(badge1)

        // Create second user and skills
        def user2Service = createService("user2")
        def proj2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(10, 2, 1, 100)
        user2Service.createProjectAndSubjectAndSkills(proj2, p2subj1, p2Skills)

        when:
        user1Service.assignSkillToGlobalBadge(projectId: proj2.projectId, badgeId: badge1.badgeId, skillId: p2Skills[0].skillId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "global badge admins cannot add a level to a global badge for a project that they do not own"() {
        // Create first user and badge
        def user1Service = createService("user1")
        def badge1 = createBadge(1, 1)
        user1Service.createGlobalBadge(badge1)

        // Create second user
        def user2Service = createService("user2")
        def proj2 = createProject(2)
        user2Service.createProject(proj2)

        when:
        user1Service.assignProjectLevelToGlobalBadge(projectId: proj2.projectId, badgeId: badge1.badgeId, level: "1")

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("code=403 FORBIDDEN")
    }

    def "global badge admins can remove a skill to the global badge for a project that they do not own"() {
        // Create first user and global badge
        def user1Service = createService("user1")

        // Create second user and skills
        def user2Service = createService("user2")
        def proj2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2Skills = createSkills(10, 2, 1, 100)
        user2Service.createProjectAndSubjectAndSkills(proj2, p2subj1, p2Skills)

        def badge1 = createBadge(1, 1)
        user2Service.createGlobalBadge(badge1)
        user2Service.assignSkillToGlobalBadge(projectId: proj2.projectId, badgeId: badge1.badgeId, skillId: p2Skills[0].skillId)
        user2Service.grantGlobalBadgeAdminRole(badge1.badgeId, user1Service.wsHelper.username)

        when:
        def result = user1Service.removeSkillFromGlobalBadge(projectId: proj2.projectId, badgeId: badge1.badgeId, skillId: p2Skills[0].skillId)

        then:
        result.success
    }

    def "global badge admins can remove a level from the global badge for a project that they do not own"() {
        // Create first user and badge
        def user1Service = createService("user1")

        // Create second user
        def user2Service = createService("user2")
        def proj2 = createProject(2)
        user2Service.createProject(proj2)
        def badge1 = createBadge(1, 1)
        user2Service.createGlobalBadge(badge1)
        user2Service.assignProjectLevelToGlobalBadge(projectId: proj2.projectId, badgeId: badge1.badgeId, level: "1")
        user2Service.grantGlobalBadgeAdminRole(badge1.badgeId, user1Service.wsHelper.username)

        when:
        def result = user1Service.removeProjectLevelFromGlobalBadge(projectId: proj2.projectId, badgeId: badge1.badgeId, level: "1")

        then:
        result.success
    }

    def "deleting a global badges also removes the user's admin role"() {
        // Create first user and global badge
        def user1Service = createService("user1")

        // Create second user and skills
        def user2Service = createService("user2")

        def badge1 = createBadge(1, 1)
        user2Service.createGlobalBadge(badge1)
        user2Service.grantGlobalBadgeAdminRole(badge1.badgeId, user1Service.wsHelper.username)


        when:
        List<UserRoleRepo.UserRoleWithAttrs> rolesBefore = userRoleRepo.findRoleWithAttrsByGlobalBadgeId(badge1.badgeId)
        def result = user1Service.deleteGlobalBadge(badge1.badgeId)
        List<UserRoleRepo.UserRoleWithAttrs> rolesAfter = userRoleRepo.findRoleWithAttrsByGlobalBadgeId(badge1.badgeId)

        then:
        result.success
        rolesBefore
        rolesBefore.role.userId.sort() == [user1Service.wsHelper.username, user2Service.wsHelper.username].sort()
        !rolesAfter
    }
}
