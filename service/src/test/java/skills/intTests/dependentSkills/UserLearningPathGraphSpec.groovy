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

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsService
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.services.settings.Settings
import skills.storage.model.Setting
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserRepo
import skills.storage.repos.UserRoleRepo

class UserLearningPathGraphSpec extends DefaultIntSpec {

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    UserAchievedLevelRepo userAchievedLevelRepo

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    UserRepo userRepo

    def "user graph has no nodes when the project has no learning path"() {
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(SkillsFactory.createSkills(2))

        when:
        def graph = skillsService.getUserDependencyGraph(SkillsFactory.defaultProjId)

        then:
        !graph.nodes
        !graph.edges
    }

    def "user graph limits nodes and progress to the requested skill version"() {
        String projectId = SkillsFactory.defaultProjId
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1, 2])
        skills.each { it.pointIncrement = 50 }
        skills.each { it.numPerformToCompletion = 1 }
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.addSkill([projectId: projectId, skillId: skills[0].skillId], 'user1', new Date())
        skillsService.addSkill([projectId: projectId, skillId: skills[2].skillId], 'user1', new Date())
        skillsService.addLearningPathPrerequisite(projectId, skills[1].skillId, skills[0].skillId)
        skillsService.addLearningPathPrerequisite(projectId, skills[2].skillId, skills[1].skillId)
        skillsService.addLearningPathPrerequisite(projectId, skills[3].skillId, skills[2].skillId)

        when:
        def version0Graph = skillsService.getUserDependencyGraph(projectId, 'user1', 0)
        def version1Graph = skillsService.getUserDependencyGraph(projectId, 'user1', 1)
        def version2Graph = skillsService.getUserDependencyGraph(projectId, 'user1', 2)
        def adminGraph = skillsService.getDependencyGraph(projectId)

        then:
        version0Graph.nodes.collectEntries { [(it.skillId): it.achieved] } == [(skills[0].skillId): true, (skills[1].skillId): false]
        version0Graph.edges.size() == 1
        version1Graph.nodes.collectEntries { [(it.skillId): it.achieved] } == [(skills[0].skillId): true, (skills[1].skillId): false, (skills[2].skillId): true]
        version1Graph.edges.size() == 2
        version2Graph.nodes*.skillId.toSet() == skills*.skillId.toSet()
        version2Graph.edges.size() == 3
        adminGraph.nodes*.skillId.toSet() == skills*.skillId.toSet()
    }

    def "project summary skill counts respect the requested version like the user graph"() {
        String projectId = SkillsFactory.defaultProjId
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1])
        skills.each { it.pointIncrement = 100 }
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.addSkill([projectId: projectId, skillId: skills[0].skillId], 'user1', new Date())
        skillsService.addSkill([projectId: projectId, skillId: skills[2].skillId], 'user1', new Date())
        skillsService.addLearningPathPrerequisite(projectId, skills[1].skillId, skills[0].skillId)
        skillsService.addLearningPathPrerequisite(projectId, skills[2].skillId, skills[1].skillId)

        when:
        def summary0 = skillsService.getSkillsSummaryForUser(projectId, 'user1', 0)
        def graph0 = skillsService.getUserDependencyGraph(projectId, 'user1', 0)
        def summary1 = skillsService.getSkillsSummaryForUser(projectId, 'user1', 1)
        def graph1 = skillsService.getUserDependencyGraph(projectId, 'user1', 1)

        then:
        summary0.totalSkills == 2
        summary0.skillsAchieved == 1
        graph0.nodes.size() == summary0.totalSkills
        graph0.nodes.count { it.achieved } == summary0.skillsAchieved
        summary1.totalSkills == 3
        summary1.skillsAchieved == 2
        graph1.nodes.size() == summary1.totalSkills
        graph1.nodes.count { it.achieved } == summary1.skillsAchieved
    }

    def "user graph only marks fully achieved learning path skills and keeps other users separate"() {
        String projectId = SkillsFactory.defaultProjId
        List<Map> skills = SkillsFactory.createSkills(4)
        skills.each { it.pointIncrement = 50 }
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(projectId, skills[1].skillId, skills[0].skillId)
        skillsService.addLearningPathPrerequisite(projectId, skills[2].skillId, skills[1].skillId)
        skillsService.addSkill([projectId: projectId, skillId: skills[0].skillId], 'user1', new Date())
        skillsService.addSkill([projectId: projectId, skillId: skills[1].skillId], 'user1', new Date())
        skillsService.addSkill([projectId: projectId, skillId: skills[0].skillId], 'user2', new Date())

        when:
        def user1Graph = skillsService.getUserDependencyGraph(projectId, 'user1')
        def user2Graph = skillsService.getUserDependencyGraph(projectId, 'user2')
        def currentUserGraph = skillsService.getUserDependencyGraph(projectId)
        def adminGraph = skillsService.getDependencyGraph(projectId)

        then:
        user1Graph.nodes.collectEntries { [(it.skillId): it.achieved] } == [skill1: true, skill2: true, skill3: false]
        user2Graph.nodes.collectEntries { [(it.skillId): it.achieved] } == [skill1: true, skill2: false, skill3: false]
        currentUserGraph.nodes.every { !it.achieved }
        adminGraph.nodes.every { !it.achieved }
        user1Graph.nodes*.skillId.toSet() == [skills[0].skillId, skills[1].skillId, skills[2].skillId].toSet()
        user1Graph.edges.size() == 2
    }

    def "user graph maps an imported learning path skill's achievement to its local node and badge entry"() {
        def sourceProject = SkillsFactory.createProject(1)
        def sourceSubject = SkillsFactory.createSubject(1, 1)
        def sourceSkills = SkillsFactory.createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(sourceProject, sourceSubject, sourceSkills)
        skillsService.exportSkillToCatalog(sourceProject.projectId, sourceSkills[0].skillId)

        def project = SkillsFactory.createProject(2)
        def subject = SkillsFactory.createSubject(2, 1)
        def skills = SkillsFactory.createSkillsStartingAt(2, 3, 2, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(project, subject, skills)
        skillsService.importSkillFromCatalogAndFinalize(project.projectId, subject.subjectId, sourceProject.projectId, sourceSkills[0].skillId)

        def badge = SkillsFactory.createBadge(2, 1)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: project.projectId, badgeId: badge.badgeId, skillId: sourceSkills[0].skillId])
        skillsService.assignSkillToBadge([projectId: project.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)
        skillsService.addLearningPathPrerequisite(project.projectId, skills[1].skillId, sourceSkills[0].skillId)
        skillsService.addLearningPathPrerequisite(project.projectId, skills[1].skillId, badge.badgeId)
        skillsService.addSkill([projectId: sourceProject.projectId, skillId: sourceSkills[0].skillId], 'user1', new Date())
        skillsService.addSkill([projectId: sourceProject.projectId, skillId: sourceSkills[0].skillId], 'user1', new Date())
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        def importedSkillDef = skillDefRepo.findByProjectIdAndSkillId(project.projectId, sourceSkills[0].skillId)
        def originalSkillDef = skillDefRepo.findByProjectIdAndSkillId(sourceProject.projectId, sourceSkills[0].skillId)
        def achievements = userAchievedLevelRepo.findAll().findAll { it.userId == 'user1' && it.skillId == sourceSkills[0].skillId }

        when:
        def graph = skillsService.getUserDependencyGraph(project.projectId, 'user1')

        then:
        importedSkillDef.copiedFrom == originalSkillDef.id
        achievements.find { it.projectId == project.projectId }?.skillRefId == importedSkillDef.id
        graph.nodes.find { it.skillId == sourceSkills[0].skillId && it.projectId == project.projectId }.achieved
        graph.nodes.find { it.skillId == badge.badgeId }.containedSkills.find { it.skillId == sourceSkills[0].skillId }.achieved
        !graph.nodes.find { it.skillId == badge.badgeId }.achieved
    }

    def "user graph marks a badge achieved only after all of its skills are completed"() {
        String projectId = SkillsFactory.defaultProjId
        List<Map> skills = SkillsFactory.createSkills(3)
        skills.each { it.pointIncrement = 50 }
        def badge = SkillsFactory.createBadge()
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: projectId, badgeId: badge.badgeId, skillId: skills[1].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)
        skillsService.addLearningPathPrerequisite(projectId, skills[2].skillId, badge.badgeId)
        skillsService.addSkill([projectId: projectId, skillId: skills[0].skillId], 'user1', new Date())
        skillsService.addSkill([projectId: projectId, skillId: skills[0].skillId], 'user1', new Date())

        when:
        def partial = skillsService.getUserDependencyGraph(projectId, 'user1')

        then:
        def partialBadge = partial.nodes.find { it.skillId == badge.badgeId }
        !partialBadge.achieved
        partialBadge.containedSkills.collectEntries { [(it.skillId): it.achieved] } == [skill1: true, skill2: false]
        partial.nodes*.skillId.toSet() == [badge.badgeId, skills[2].skillId].toSet()
        !partial.nodes.find { it.skillId == skills[2].skillId }.achieved

        when:
        skillsService.addSkill([projectId: projectId, skillId: skills[1].skillId], 'user1', new Date())
        skillsService.addSkill([projectId: projectId, skillId: skills[1].skillId], 'user1', new Date())
        def complete = skillsService.getUserDependencyGraph(projectId, 'user1')

        then:
        def completeBadge = complete.nodes.find { it.skillId == badge.badgeId }
        completeBadge.achieved
        completeBadge.containedSkills.every { it.achieved }
        complete.nodes.size() == 2
        !complete.nodes.find { it.skillId == skills[2].skillId }.achieved
    }

    def "badge skills in a user graph respect the requested version"() {
        String projectId = SkillsFactory.defaultProjId
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1])
        skills.each { it.pointIncrement = 100 }
        skills.each { it.numPerformToCompletion = 1 }
        def badge = SkillsFactory.createBadge()
        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkills(skills)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: projectId, badgeId: badge.badgeId, skillId: skills[2].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)
        skillsService.addLearningPathPrerequisite(projectId, skills[1].skillId, badge.badgeId)
        skillsService.addSkill([projectId: projectId, skillId: skills[2].skillId], 'user1', new Date())

        when:
        def version0Graph = skillsService.getUserDependencyGraph(projectId, 'user1', 0)
        def version1Graph = skillsService.getUserDependencyGraph(projectId, 'user1', 1)
        def adminGraph = skillsService.getDependencyGraph(projectId)

        then:
        version0Graph.nodes.find { it.skillId == badge.badgeId }.containedSkills*.skillId == [skills[0].skillId]
        version0Graph.nodes.find { it.skillId == badge.badgeId }.containedSkills*.achieved == [false]
        version1Graph.nodes.find { it.skillId == badge.badgeId }.containedSkills*.skillId == [skills[0].skillId, skills[2].skillId]
        version1Graph.nodes.find { it.skillId == badge.badgeId }.containedSkills*.achieved == [false, true]
        adminGraph.nodes.find { it.skillId == badge.badgeId }.containedSkills*.skillId == [skills[0].skillId, skills[2].skillId]
    }

    def "shared prerequisite achievements are shown for the owning project only"() {
        def project = SkillsFactory.createProject(1)
        def subject = SkillsFactory.createSubject(1, 1)
        def skills = SkillsFactory.createSkills(2, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(project, subject, skills)

        def sharedProject = SkillsFactory.createProject(2)
        def sharedSubject = SkillsFactory.createSubject(2, 1)
        def sharedSkills = SkillsFactory.createSkills(2, 2, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(sharedProject, sharedSubject, sharedSkills)
        skillsService.shareSkill(sharedProject.projectId, sharedSkills[0].skillId, project.projectId)
        skillsService.addLearningPathPrerequisite(project.projectId, skills[1].skillId, sharedProject.projectId, sharedSkills[0].skillId)
        skillsService.addSkill([projectId: sharedProject.projectId, skillId: sharedSkills[0].skillId], 'user1', new Date())

        when:
        def graph = skillsService.getUserDependencyGraph(project.projectId, 'user1')

        then:
        graph.nodes.size() == 2
        graph.nodes.find { it.projectId == sharedProject.projectId }.achieved
        !graph.nodes.find { it.projectId == project.projectId }.achieved
    }

    def "user graph omits invite-only prerequisites unless the caller can access the project"() {
        def project = SkillsFactory.createProject(1)
        def skills = SkillsFactory.createSkills(3, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(project, SkillsFactory.createSubject(1, 1), skills)

        def restrictedProject = SkillsFactory.createProject(2)
        def restrictedSkills = SkillsFactory.createSkills(2, 2, 1)
        skillsService.createProjectAndSubjectAndSkills(restrictedProject, SkillsFactory.createSubject(2, 1), restrictedSkills)
        skillsService.shareSkill(restrictedProject.projectId, restrictedSkills[0].skillId, project.projectId)
        skillsService.addLearningPathPrerequisite(project.projectId, skills[1].skillId, restrictedProject.projectId, restrictedSkills[0].skillId)
        skillsService.addLearningPathPrerequisite(project.projectId, skills[2].skillId, skills[0].skillId)
        skillsService.changeSetting(restrictedProject.projectId, 'invite_only', [projectId: restrictedProject.projectId, setting: 'invite_only', value: 'true'])

        String userId = getRandomUsers(1)[0]
        SkillsService viewer = createService(userId)

        when:
        def deniedGraph = viewer.getUserDependencyGraph(project.projectId)

        then:
        deniedGraph.nodes*.skillId.toSet() == [skills[0].skillId, skills[2].skillId].toSet()
        deniedGraph.edges.size() == 1
        !deniedGraph.nodes.find { it.projectId == restrictedProject.projectId }

        when:
        String normalizedUserId = viewer.getUsername(userId)
        userRoleRepo.save(new UserRole(userId: normalizedUserId, userRefId: userRepo.findByUserId(normalizedUserId).id,
                projectId: restrictedProject.projectId, roleName: RoleName.ROLE_PRIVATE_PROJECT_USER))
        def allowedGraph = viewer.getUserDependencyGraph(project.projectId)

        then:
        allowedGraph.nodes.find { it.projectId == restrictedProject.projectId && it.skillId == restrictedSkills[0].skillId }
        allowedGraph.edges.size() == 2
    }

    def "user graph omits community-only prerequisites for nonmembers"() {
        SkillsService root = createRootSkillService()
        String memberId = getRandomUsers(1)[0]
        SkillsService member = createService(memberId)
        root.saveUserTag(memberId, 'dragons', ['DivineDragon'])

        def project = SkillsFactory.createProject(1)
        def skills = SkillsFactory.createSkills(2, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(project, SkillsFactory.createSubject(1, 1), skills)

        def restrictedProject = SkillsFactory.createProject(2)
        def restrictedSkills = SkillsFactory.createSkills(2, 2, 1)
        member.createProjectAndSubjectAndSkills(restrictedProject, SkillsFactory.createSubject(2, 1), restrictedSkills)
        member.shareSkill(restrictedProject.projectId, restrictedSkills[0].skillId, project.projectId)
        skillsService.addLearningPathPrerequisite(project.projectId, skills[1].skillId, restrictedProject.projectId, restrictedSkills[0].skillId)
        // A project cannot enable community protection while it is sharing skills. Simulate an existing
        // relationship after its visibility changes to exercise the graph's read-time access check.
        settingRepo.save(new Setting(type: Setting.SettingType.Project, projectId: restrictedProject.projectId,
                setting: Settings.USER_COMMUNITY_ONLY_PROJECT.settingName, value: 'true'))

        when:
        def deniedGraph = skillsService.getUserDependencyGraph(project.projectId)
        def allowedGraph = member.getUserDependencyGraph(project.projectId)

        then:
        !deniedGraph.nodes
        !deniedGraph.edges
        allowedGraph.nodes.find { it.projectId == restrictedProject.projectId && it.skillId == restrictedSkills[0].skillId }
    }

    def "target badge includes its achieved child skills"() {
        def project = SkillsFactory.createProject()
        def subject = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(project, subject, skills)
        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: project.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)
        skillsService.addLearningPathPrerequisite(project.projectId, badge.badgeId, skills[1].skillId)
        skillsService.addSkill([projectId: project.projectId, skillId: skills[1].skillId], 'user1', new Date())
        skillsService.addSkill([projectId: project.projectId, skillId: skills[0].skillId], 'user1', new Date())

        when:
        def graph = skillsService.getUserDependencyGraph(project.projectId, 'user1')

        then:
        graph.nodes.find { it.skillId == badge.badgeId }.containedSkills.collect { it.skillId } == [skills[0].skillId]
        graph.nodes.find { it.skillId == badge.badgeId }.containedSkills.every { it.achieved }
    }
}
