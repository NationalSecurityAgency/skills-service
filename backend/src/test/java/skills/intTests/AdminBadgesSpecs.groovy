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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class AdminBadgesSpecs extends DefaultIntSpec {

    def "get badge that have skills assigned"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.assignDependency([projectId: proj.projectId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(1).skillId])
        skillsService.assignDependency([projectId: proj.projectId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(2).skillId])

        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(3).skillId])

        when:
        def res = skillsService.getBadge(badge)

        then:
        res
        res.numSkills == 2
        res.requiredSkills.size() == 2
        res.requiredSkills.collect { it.skillId }.sort() == ["skill1", "skill4"]
        res.totalPoints == 20
        res.badgeId == badge.badgeId
        res.name == badge.name
        res.projectId == proj.projectId
    }

    def "assign skills to inactive badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)
        skillsService.assignDependency([projectId: proj.projectId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(1).skillId])
        skillsService.assignDependency([projectId: proj.projectId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(2).skillId])

        badge.enabled = false
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(3).skillId])

        when:
        def res = skillsService.getBadge(badge)

        then:
        res
        res.numSkills == 2
        res.requiredSkills.size() == 2
        res.requiredSkills.collect { it.skillId }.sort() == ["skill1", "skill4"]
        res.totalPoints == 20
        res.badgeId == badge.badgeId
        res.name == badge.name
        res.projectId == proj.projectId
        res.enabled == 'false'
    }

    def "remove skills from a badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId])
        Map badgeSkillDeclaration = [projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(3).skillId]
        skillsService.assignSkillToBadge(badgeSkillDeclaration)

        when:
        def res = skillsService.getBadge(badge)
        skillsService.removeSkillFromBadge(badgeSkillDeclaration)
        def resAfterDeletion = skillsService.getBadge(badge)
        then:
        res
        res.numSkills == 2
        res.requiredSkills.collect { it.skillId }.sort() == ["skill1", "skill4"]

        resAfterDeletion
        resAfterDeletion.numSkills == 1
        resAfterDeletion.requiredSkills.collect { it.skillId }.sort() == ["skill1"]
    }

    def "remove badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def skills = SkillsFactory.createSkills(4)
        def badge = SkillsFactory.createBadge()
        def badge1 = SkillsFactory.createBadge(1, 2)

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        skillsService.createBadge(badge)
        skillsService.createBadge(badge1)

        when:
        def res = skillsService.getBadges(proj.projectId)
        skillsService.removeBadge(badge1)
        def resAfterDeletion = skillsService.getBadges(proj.projectId)
        then:
        res
        res.collect { it.badgeId }.sort() == [badge.badgeId, badge1.badgeId].sort()

        resAfterDeletion
        resAfterDeletion.collect { it.badgeId } == [badge.badgeId]
    }
}
