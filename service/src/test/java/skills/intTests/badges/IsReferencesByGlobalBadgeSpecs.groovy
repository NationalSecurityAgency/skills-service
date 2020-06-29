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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

class IsReferencesByGlobalBadgeSpecs extends DefaultIntSpec {

    SkillsService supervisorService

    def setup() {
        supervisorService = createSupervisor()
    }

    def "is skill referenced by global badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        List<Map> skills = SkillsFactory.createSkills(3,)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        supervisorService.createGlobalBadge(badge)

        when:
        supervisorService.assignSkillToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId)

        then:
        skillsService.isSkillReferencedByGlobalBadge(proj.projectId,  skills.get(0).skillId)
        !skillsService.isSkillReferencedByGlobalBadge(proj.projectId,  skills.get(1).skillId)
        !skillsService.isSkillReferencedByGlobalBadge(proj.projectId,  skills.get(2).skillId)
    }

    def "is subject referenced by global badge"() {
        def proj = SkillsFactory.createProject()
        def subj = SkillsFactory.createSubject()
        def subj1 = SkillsFactory.createSubject(1, 2)
        List<Map> skills = SkillsFactory.createSkills(3, 1, 1)
        List<Map> skills1 = SkillsFactory.createSkills(3, 1, 2)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills)
        skillsService.createSkills(skills1)

        supervisorService.createGlobalBadge(badge)

        when:
        supervisorService.assignSkillToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId)

        then:
        skillsService.isSubjectReferencedByGlobalBadge(proj.projectId,  subj.subjectId)
        !skillsService.isSubjectReferencedByGlobalBadge(proj.projectId,  subj1.subjectId)
    }

    def "is project referenced by global badge"() {
        def proj = SkillsFactory.createProject()
        def proj1 = SkillsFactory.createProject(2)
        def subj = SkillsFactory.createSubject()
        def subj1 = SkillsFactory.createSubject(1, 2)
        List<Map> skills = SkillsFactory.createSkills(3, 1, 1)
        List<Map> skills1 = SkillsFactory.createSkills(3, 1, 2)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createProject(proj1)
        skillsService.createSubject(subj)
        skillsService.createSubject(subj1)
        skillsService.createSkills(skills)
        skillsService.createSkills(skills1)


        supervisorService.createGlobalBadge(badge)

        when:
        supervisorService.assignSkillToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills.get(0).skillId)

        then:
        skillsService.isProjectReferencedByGlobalBadge(proj.projectId)
        !skillsService.isProjectReferencedByGlobalBadge(proj1.projectId)
    }

    def "is project level referenced by global badge"() {
        def proj = SkillsFactory.createProject()
        def proj1 = SkillsFactory.createProject(2)
        def badge = SkillsFactory.createBadge()

        skillsService.createProject(proj)
        skillsService.createProject(proj1)

        supervisorService.createGlobalBadge(badge)

        when:
        supervisorService.assignProjectLevelToGlobalBadge(projectId: proj.projectId, badgeId: badge.badgeId, level: "1")

        then:
        skillsService.isProjectLevelReferencedByGlobalBadge(proj.projectId, 1)
        !skillsService.isProjectLevelReferencedByGlobalBadge(proj.projectId, 2)
        !skillsService.isProjectLevelReferencedByGlobalBadge(proj1.projectId, 1)
    }
}
