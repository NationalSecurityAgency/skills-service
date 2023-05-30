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


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.*

class AdminLearningPathBadgeSpecs extends DefaultIntSpec {

    def "badge -> skill learning path"() {
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
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, badge.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId, p1.projectId, badge.badgeId)

        def graph = skillsService.getDependencyGraph(p1.projectId)
        then:
        edges(graph) == [
                "[Badge:${badge.badgeId}] prerequisite for [Skill:${p1Skills[2].skillId}]",
                "[Badge:${badge.badgeId}] prerequisite for [Skill:${p1Skills[3].skillId}]",
        ].sort()
    }

    def "skill -> badge learning path"() {
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
        skillsService.addLearningPathPrerequisite(p1.projectId, badge.badgeId, p1.projectId, p1Skills[2].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge.badgeId, p1.projectId, p1Skills[3].skillId)

        def graph = skillsService.getDependencyGraph(p1.projectId)
        then:
        edges(graph) == [
                "[Skill:${p1Skills[2].skillId}] prerequisite for [Badge:${badge.badgeId}]",
                "[Skill:${p1Skills[3].skillId}] prerequisite for [Badge:${badge.badgeId}]",
        ].sort()
    }

    def "badge -> badge learning path"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
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
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1.projectId, badge2.badgeId)

        def graph = skillsService.getDependencyGraph(p1.projectId)
        then:
        edges(graph) == [
                "[Badge:${badge2.badgeId}] prerequisite for [Badge:${badge1.badgeId}]",
        ].sort()
    }

    def "skill -> skill learning path"() {
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
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[2].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId, p1Skills[3].skillId)

        def graph = skillsService.getDependencyGraph(p1.projectId)
        then:
        edges(graph) == [
                "[Skill:${p1Skills[2].skillId}] prerequisite for [Skill:${p1Skills[0].skillId}]",
                "[Skill:${p1Skills[3].skillId}] prerequisite for [Skill:${p1Skills[0].skillId}]",
        ].sort()
    }

    def "complex learning path"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(15, 1, 1, 100)
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
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[3].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        def badge3 = SkillsFactory.createBadge(1, 3)
        skillsService.createBadge(badge3)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[4].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge3.badgeId, skillId: p1Skills[5].skillId])
        badge3.enabled = true
        skillsService.createBadge(badge3)

        when:
        // [skill6, skill7] -> badge2 -> badge1
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1.projectId, badge2.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, p1Skills[6].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge2.badgeId, p1.projectId, p1Skills[7].skillId)

        // [badge3] -> [skill7, skill8]
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[7].skillId, p1.projectId, badge3.badgeId,)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[8].skillId, p1.projectId, badge3.badgeId,)

        // skill9 -> badge3
        skillsService.addLearningPathPrerequisite(p1.projectId, badge3.badgeId, p1.projectId, p1Skills[9].skillId)

        // [skill10, skill11] -> skill9
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[9].skillId, p1.projectId, p1Skills[10].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[9].skillId, p1.projectId, p1Skills[11].skillId)

        def graph = skillsService.getDependencyGraph(p1.projectId)

        then:
        edges(graph) == [
                "[Badge:${badge2.badgeId}] prerequisite for [Badge:${badge1.badgeId}]",
                "[Skill:${p1Skills[6].skillId}] prerequisite for [Badge:${badge2.badgeId}]",
                "[Skill:${p1Skills[7].skillId}] prerequisite for [Badge:${badge2.badgeId}]",
                "[Badge:${badge3.badgeId}] prerequisite for [Skill:${p1Skills[7].skillId}]",
                "[Badge:${badge3.badgeId}] prerequisite for [Skill:${p1Skills[8].skillId}]",
                "[Skill:${p1Skills[9].skillId}] prerequisite for [Badge:${badge3.badgeId}]",
                "[Skill:${p1Skills[10].skillId}] prerequisite for [Skill:${p1Skills[9].skillId}]",
                "[Skill:${p1Skills[11].skillId}] prerequisite for [Skill:${p1Skills[9].skillId}]",
        ].sort()

    }

    def "only live badge can participate in the learning path; skill -> badge path"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])

        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, badge.badgeId, p1.projectId, p1Skills[2].skillId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Disabled nodes cannot be added")
    }


    def "only live badge can participate in the learning path; badge -> skill path"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def badge = SkillsFactory.createBadge()
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge.badgeId, skillId: p1Skills[1].skillId])

        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1.projectId, badge.badgeId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Disabled nodes cannot be added")
    }

    def "remove skill->skill learning path item - no data left"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1Skills[0].skillId)
        when:
        def graph_before = skillsService.getDependencyGraph(p1.projectId)
        skillsService.deleteLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1Skills[0].skillId)

        def graph = skillsService.getDependencyGraph(p1.projectId)
        then:
        edges(graph_before) == [
                "[Skill:${p1Skills[0].skillId}] prerequisite for [Skill:${p1Skills[1].skillId}]",
        ].sort()
        !graph.nodes
        !graph.edges
    }

    def "remove skill->skill learning path item"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1Skills[0].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1Skills[1].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId, p1Skills[1].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1Skills[1].skillId)
        when:
        def graph_before = skillsService.getDependencyGraph(p1.projectId)
        skillsService.deleteLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId, p1Skills[1].skillId)

        def graph = skillsService.getDependencyGraph(p1.projectId)
        then:
        edges(graph_before) == [
                "[Skill:${p1Skills[0].skillId}] prerequisite for [Skill:${p1Skills[1].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[2].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[3].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[4].skillId}]",
        ].sort()
        edges(graph) == [
                "[Skill:${p1Skills[0].skillId}] prerequisite for [Skill:${p1Skills[1].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[2].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[4].skillId}]",
        ].sort()
    }

    def "remove skill->skill learning path item - splits graph into 2 "() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(8, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1Skills[0].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1Skills[1].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId, p1Skills[1].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1Skills[1].skillId)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[5].skillId, p1Skills[3].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[6].skillId, p1Skills[5].skillId)
        when:
        def graph_before = skillsService.getDependencyGraph(p1.projectId)
        skillsService.deleteLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId, p1Skills[1].skillId)

        def graph = skillsService.getDependencyGraph(p1.projectId)
        then:
        edges(graph_before) == [
                "[Skill:${p1Skills[0].skillId}] prerequisite for [Skill:${p1Skills[1].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[2].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[3].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[4].skillId}]",
                "[Skill:${p1Skills[3].skillId}] prerequisite for [Skill:${p1Skills[5].skillId}]",
                "[Skill:${p1Skills[5].skillId}] prerequisite for [Skill:${p1Skills[6].skillId}]",
        ].sort()
        edges(graph) == [
                "[Skill:${p1Skills[0].skillId}] prerequisite for [Skill:${p1Skills[1].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[2].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[4].skillId}]",
                "[Skill:${p1Skills[3].skillId}] prerequisite for [Skill:${p1Skills[5].skillId}]",
                "[Skill:${p1Skills[5].skillId}] prerequisite for [Skill:${p1Skills[6].skillId}]",
        ].sort()
    }

    def "remove skill->badge learning path item"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)


        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[9].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[8].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1Skills[0].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1Skills[1].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[1].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1Skills[1].skillId)


        when:
        def graph_before = skillsService.getDependencyGraph(p1.projectId)
        skillsService.deleteLearningPathPrerequisite(p1.projectId, badge1.badgeId, p1Skills[1].skillId)

        def graph = skillsService.getDependencyGraph(p1.projectId)
        then:
        edges(graph_before) == [
                "[Skill:${p1Skills[0].skillId}] prerequisite for [Skill:${p1Skills[1].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[2].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Badge:${badge1.badgeId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[4].skillId}]",
        ].sort()
        edges(graph) == [
                "[Skill:${p1Skills[0].skillId}] prerequisite for [Skill:${p1Skills[1].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[2].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[4].skillId}]",
        ].sort()
    }

    def "remove badge->skill learning path item"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(10, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)


        def badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge1)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[9].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge1.badgeId, skillId: p1Skills[8].skillId])
        badge1.enabled = true
        skillsService.createBadge(badge1)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[1].skillId, p1Skills[0].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[2].skillId, p1Skills[1].skillId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId, badge1.badgeId)
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[4].skillId, p1Skills[1].skillId)


        when:
        def graph_before = skillsService.getDependencyGraph(p1.projectId)
        skillsService.deleteLearningPathPrerequisite(p1.projectId, p1Skills[3].skillId, badge1.badgeId)

        def graph = skillsService.getDependencyGraph(p1.projectId)
        then:
        edges(graph_before) == [
                "[Skill:${p1Skills[0].skillId}] prerequisite for [Skill:${p1Skills[1].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[2].skillId}]",
                "[Badge:${badge1.badgeId}] prerequisite for [Skill:${p1Skills[3].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[4].skillId}]",
        ].sort()
        edges(graph) == [
                "[Skill:${p1Skills[0].skillId}] prerequisite for [Skill:${p1Skills[1].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[2].skillId}]",
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[4].skillId}]",
        ].sort()
    }

    def "able to add skill1->skill2 when both skills are under the same badge"() {
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
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId,  p1Skills[1].skillId)

        def graph = skillsService.getDependencyGraph(p1.projectId)
        then:
        edges(graph) == [
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[0].skillId}]",
        ].sort()
    }

    def "able to add skill1->skill2 when both skills are under 2 separate badges"() {
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

        def badge2 = SkillsFactory.createBadge(1, 2)
        skillsService.createBadge(badge2)
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[0].skillId])
        skillsService.assignSkillToBadge([projectId: p1.projectId, badgeId: badge2.badgeId, skillId: p1Skills[1].skillId])
        badge2.enabled = true
        skillsService.createBadge(badge2)

        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills[0].skillId, p1.projectId,  p1Skills[1].skillId)

        def graph = skillsService.getDependencyGraph(p1.projectId)
        then:
        edges(graph) == [
                "[Skill:${p1Skills[1].skillId}] prerequisite for [Skill:${p1Skills[0].skillId}]",
        ].sort()
    }

    private List<String> edges(def graph) {
        def idToSkillIdMap = graph.nodes.collectEntries {[it.id, it]}
        return graph.edges.collect {
            def from = idToSkillIdMap[it.fromId]
            def to = idToSkillIdMap[it.toId]
            "[${to.type}:${to.skillId}] prerequisite for [${from.type}:${from.skillId}]"
        }.sort()
    }
}
