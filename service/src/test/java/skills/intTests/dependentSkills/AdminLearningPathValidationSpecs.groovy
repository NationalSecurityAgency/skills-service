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
package skills.intTests.dependentSkills

import skills.controller.result.model.DependencyCheckResult
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.*

class AdminLearningPathValidationSpecs extends DefaultIntSpec {

    def "skill1 -> skill2 -> skill1 circular dep"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[2].skillId)
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, p1Skills[0].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Discovered circular prerequisite [Skill:${p1Skills[2].skillId} -> Skill:${p1Skills[0].skillId} -> Skill:${p1Skills[2].skillId}]")
    }

    def "skill1 -> skill2 -> skill3 -> skill1 circular dep"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1.projectId, p1Skills[2].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[1].skillId)
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, p1Skills[0].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Discovered circular prerequisite [Skill:${p1Skills[2].skillId} -> Skill:${p1Skills[0].skillId} -> Skill:${p1Skills[1].skillId} -> Skill:${p1Skills[2].skillId}]")
    }

    def "badge(skill1) -> skill1 learning path: badge cannot contain the skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, badge.badgeId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("A badge cannot have a dependency with a skill it contains. Badge [ID:${badge.badgeId}] can not have a dependency with [ID:${p1Skills[0].skillId}]")
    }

    def "skill1 -> badge(skill2) -> skill1 learning path: badge cannot contain the skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        skillsService.addLearningPathPrerequisite(p1.projectId, badge.badgeId, p1.projectId, p1Skills[2].skillId)
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, badge.badgeId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Discovered circular prerequisite [Skill:${p1Skills[2].skillId} -> Badge:${badge.badgeId} -> Skill:${p1Skills[2].skillId}]")
    }

    def "badge(skill2) -> skill1 -> badge(skill2) learning path: badge cannot contain the skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])
        badge.enabled = true
        skillsService.createBadge(badge)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, badge.badgeId)
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, badge.badgeId, p1.projectId, p1Skills[2].skillId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Discovered circular prerequisite [Badge:${badge.badgeId} -> Skill:${p1Skills[2].skillId} -> Badge:${badge.badgeId}]")
    }

    def "[badge1, badge2] -> [badge3, badge4] -> skill1 -> badge2) learning path: badge cannot contain the skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[1].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        def badge3 = SkillsFactory.createBadge(1, 3)
        skillsService.createBadge(badge3)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[2].skillId])
        badge3.enabled = true
        skillsService.createBadge(badge3)

        def badge4 = SkillsFactory.createBadge(1, 4)
        skillsService.createBadge(badge4)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge4.badgeId, skillId: p1Skills[3].skillId])
        badge4.enabled = true
        skillsService.createBadge(badge4)

        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, p1.projectId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge4.badgeId, p1.projectId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, p1.projectId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge4.badgeId, p1.projectId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1.projectId, badge3.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1.projectId, badge4.badgeId)
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, p1Skills[4].skillId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        (e.getMessage().contains("Discovered circular prerequisite [Badge:${badge2.badgeId} -> Skill:${p1Skills[4].skillId} -> Badge:${badge4.badgeId} -> Badge:${badge2.badgeId}]")
                ||
                e.getMessage().contains("Discovered circular prerequisite [Badge:${badge2.badgeId} -> Skill:${p1Skills[4].skillId} -> Badge:${badge3.badgeId} -> Badge:${badge2.badgeId}]"))
    }

    def "badge1 -> badge2: two badges cannot contain the same skill in the same learning path"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[1].skillId]) // same skill
        badge2.enabled = true
        skillsService.createBadge(badge2)

        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, badge1.badgeId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Multiple badges on the same Learning path cannot have overlapping skills. Both badge [Test Badge 1] and [Test Badge 2] badge have [Test Skill 2] skill")
    }

    def "skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2: two badges cannot contain the same skill in the same learning path"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[1].skillId]) // same skill
        badge2.enabled = true
        skillsService.createBadge(badge2)

        // skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1.projectId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1.projectId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1.projectId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1.projectId, p1Skills[5].skillId)
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, p1Skills[6].skillId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Multiple badges on the same Learning path cannot have overlapping skills. Both badge [Test Badge 1] and [Test Badge 2] badge have [Test Skill 2] skill")
    }


    def "skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2: Cannot add a badge that already has one of its skills on the learning path"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[4].skillId]) // this skill already on the path
        badge2.enabled = true
        skillsService.createBadge(badge2)

        // skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1.projectId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1.projectId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1.projectId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1.projectId, p1Skills[5].skillId)
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, p1Skills[6].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Badge [${badge2.name}] has skill [${p1Skills[4].name}] which already exists on the Learning Path")
    }

    def "skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2: Cannot add a learning path item that already exist"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[4].skillId]) // this skill already on the path
        badge2.enabled = true
        skillsService.createBadge(badge2)

        // skill3 -> skill4 -> badge1 -> skill5 -> skill6 -> badge2
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1.projectId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1.projectId, p1Skills[4].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1.projectId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1.projectId, p1Skills[5].skillId)
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1.projectId, badge1.badgeId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Learning path from [${badge1.name}] to [${p1Skills[5].name}] already exists.")
    }

    def "skills exported to the catalog cannot have a prerequisite"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        skillsService.exportSkillToCatalog(p1.projectId, p1Skills[0].skillId)
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[1].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Skill [${p1Skills[0].skillId}] was exported to the Skills Catalog. A skill in the catalog cannot have prerequisites on the learning path.")
    }

    def "re-used skills cannot have a prerequisite"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[1].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Skill [${p1Skills[0].skillId}] was reused in another subject or group and cannot have prerequisites in the learning path.")
    }

    def "assigning dependent skills validates versions of the skills (dependency version must be less than or equal to the skill version)"() {
        List<Map> skills = SkillsFactory.createSkillsWithDifferentVersions([0, 0, 1])

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(SkillsFactory.createSubject())
        skillsService.createSkill(skills.get(0))
        skillsService.createSkill(skills.get(1))
        skillsService.createSkill(skills.get(2))

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, SkillsFactory.defaultProjId, skills.get(0).skillId)
        when:
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(1).skillId, SkillsFactory.defaultProjId, skills.get(2).skillId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Not allowed to depend on skill with a later version. Skill [ID:skill2, version 0] can not depend on [ID:skill3, version 1]")
    }

    def "attempt to depend on skill that was not shared"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        def proj2 = SkillsFactory.createProject(2)
        def proj2_subj = SkillsFactory.createSubject(2, 2)
        List<Map> proj2_skills = SkillsFactory.createSkills(2, 2, 2)

        skillsService.createProjectAndSubjectAndSkills(proj1, proj1_subj, proj1_skills)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, proj2_skills)

        when:
        skillsService.addLearningPathPrerequisite(proj2.projectId, proj2_skills.get(0).skillId, proj1.projectId, proj1_skills.get(0).skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Skill [TestProject1:skill1] is not shared (or does not exist) to [TestProject2] project")
    }

    def "skill-only learning path shall work even if skills belong to badges that theoretically would cause circular path if any of those badges were on the learning path"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(6, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge1 = createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[1].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[2].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[5].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        def badge2 = createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[4].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1Skills[5].skillId)

        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1Skills[1].skillId)
        then:
        noExceptionThrown()
        def graph = skillsService.getDependencyGraph(p1.projectId)
        def idMap = graph.nodes.collectEntries {[it.skillId, it.id]}
        graph.edges.collect { "${it.fromId}->${it.toId}".toString() }.sort() == [
                "${idMap.get(p1Skills[2].skillId)}->${idMap.get(p1Skills[3].skillId)}".toString(),
                "${idMap.get(p1Skills[4].skillId)}->${idMap.get(p1Skills[5].skillId)}".toString(),
                "${idMap.get(p1Skills[0].skillId)}->${idMap.get(p1Skills[1].skillId)}".toString(),
        ].sort()
    }
}
