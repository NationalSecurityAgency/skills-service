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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class UserLearningPathGraphSpec extends DefaultIntSpec {

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
        partial.nodes*.skillId.toSet() == [badge.badgeId, skills[2].skillId].toSet()
        !partial.nodes.find { it.skillId == skills[2].skillId }.achieved

        when:
        skillsService.addSkill([projectId: projectId, skillId: skills[1].skillId], 'user1', new Date())
        skillsService.addSkill([projectId: projectId, skillId: skills[1].skillId], 'user1', new Date())
        def complete = skillsService.getUserDependencyGraph(projectId, 'user1')

        then:
        def completeBadge = complete.nodes.find { it.skillId == badge.badgeId }
        completeBadge.achieved
        complete.nodes.size() == 2
        !complete.nodes.find { it.skillId == skills[2].skillId }.achieved
    }
}
