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
import skills.intTests.utils.SkillsFactory

class AdminGraphDisplaySpec extends DefaultIntSpec {

    def "empty project graph"() {
        List<Map> skills = SkillsFactory.createSkills(2)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId)

        then:
        !graph.nodes
        !graph.edges
    }

    def "simple project graph with 2 nodes"() {
        List<Map> skills = SkillsFactory.createSkills(2)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId)

        then:
        graph.nodes.collect { it.skillId }.sort() == [skills.get(0).skillId, skills.get(1).skillId,]

        graph.edges.size() == 1
        graph.edges.get(0).fromId == graph.nodes.find { it.skillId == skills.get(0).skillId }.id
        graph.edges.get(0).toId == graph.nodes.find { it.skillId == skills.get(1).skillId }.id
        graph.nodes.subjectId == [subject.subjectId, subject.subjectId]
    }

    def "simple project graph with 2 nodes - different subjects"() {
        List<Map> skills = SkillsFactory.createSkills(1)
        List<Map> skills_subj2 = SkillsFactory.createSkills(1, 1, 2)
        def subject = SkillsFactory.createSubject()
        def subject2 = SkillsFactory.createSubject(1, 2)

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkills(skills)
        skillsService.createSkills(skills_subj2)

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills_subj2.get(0).skillId)

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId)

        then:
        graph.edges.size() == 1
        graph.edges.get(0).fromId == graph.nodes.find { it.skillId == skills.get(0).skillId }.id
        graph.edges.get(0).toId == graph.nodes.find { it.skillId == skills_subj2.get(0).skillId }.id
        graph.nodes.sort { it.subjectId }.subjectId == [subject.subjectId, subject2.subjectId]
        graph.nodes.sort { it.subjectId }.skillId == [skills.get(0).skillId, skills_subj2.get(0).skillId]
    }

    def "simple project graph with 2 nodes - different subjects - group skills"() {
        List<Map> skills = SkillsFactory.createSkills(1)
        List<Map> skills_subj2 = SkillsFactory.createSkills(1, 1, 2)
        def subject = SkillsFactory.createSubject()
        def subject2 = SkillsFactory.createSubject(1, 2)
        def group1 = SkillsFactory.createSkillsGroup(1, 1, 10)
        def group2 = SkillsFactory.createSkillsGroup(1, 2, 11)

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSubject(subject2)
        skillsService.createSkills([group1, group2])
        skills.each {
            skillsService.assignSkillToSkillsGroup(group1.skillId, it)
        }
        skills_subj2.each {
            skillsService.assignSkillToSkillsGroup(group2.skillId, it)
        }

        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills_subj2.get(0).skillId)

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId)

        then:
        graph.edges.size() == 1
        graph.edges.get(0).fromId == graph.nodes.find { it.skillId == skills.get(0).skillId }.id
        graph.edges.get(0).toId == graph.nodes.find { it.skillId == skills_subj2.get(0).skillId }.id
        graph.nodes.sort { it.subjectId }.subjectId == [subject.subjectId, subject2.subjectId]
        graph.nodes.sort { it.subjectId }.skillId == [skills.get(0).skillId, skills_subj2.get(0).skillId]
    }

    def "project graph with rich hierarchy"() {
        List<Map> skills = SkillsFactory.createSkills(7)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(3).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(3).skillId, skills.get(4).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(3).skillId, skills.get(5).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(5).skillId, skills.get(6).skillId)

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId)

        then:
        def idMap = graph.nodes.collectEntries {[it.skillId, it.id]}
        graph.nodes.collect { it.skillId }.sort() == (0..6).collect { skills.get(it).skillId }

        graph.edges.size() == 6
        def edges = graph.edges.collect { "${it.fromId}->${it.toId}" }
        edges.remove("${idMap.get(skills.get(0).skillId)}->${idMap.get(skills.get(1).skillId)}")
        edges.remove("${idMap.get(skills.get(0).skillId)}->${idMap.get(skills.get(2).skillId)}")
        edges.remove("${idMap.get(skills.get(0).skillId)}->${idMap.get(skills.get(3).skillId)}")
        edges.remove("${idMap.get(skills.get(3).skillId)}->${idMap.get(skills.get(4).skillId)}")
        edges.remove("${idMap.get(skills.get(3).skillId)}->${idMap.get(skills.get(5).skillId)}")
        edges.remove("${idMap.get(skills.get(5).skillId)}->${idMap.get(skills.get(6).skillId)}")
        !edges
    }

    def "project graph - verify attributes are populated in the nodes"() {
        List<Map> skills = SkillsFactory.createSkills(2)
        skills.get(1).pointIncrement = 20
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId)

        then:
        def node1 = graph.nodes.find{ it.skillId == skills.get(0).skillId}
        def node2 = graph.nodes.find{ it.skillId == skills.get(1).skillId}

        node1.projectId == SkillsFactory.defaultProjId
        node1.name ==  skills.get(0).name
        node1.pointIncrement ==  skills.get(0).pointIncrement
        node1.totalPoints == skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion
        node1.type ==  "Skill"

        node2.projectId == SkillsFactory.defaultProjId
        node2.name ==  skills.get(1).name
        node2.pointIncrement ==  skills.get(1).pointIncrement
        node2.totalPoints ==  skills.get(1).pointIncrement * skills.get(1).numPerformToCompletion
        node2.type ==  "Skill"
    }

    def "empty skill graph"() {
        List<Map> skills = SkillsFactory.createSkills(2)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId, skills.get(0).skillId)

        then:
        !graph.nodes
        !graph.edges
    }

    def "simple skill graph with 2 nodes"() {
        List<Map> skills = SkillsFactory.createSkills(2)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId, skills.get(0).skillId)

        then:
        graph.nodes.collect { it.skillId }.sort() == [skills.get(0).skillId, skills.get(1).skillId,]

        graph.edges.size() == 1
        graph.edges.get(0).fromId == graph.nodes.find{ it.skillId == skills.get(0).skillId}.id
        graph.edges.get(0).toId == graph.nodes.find{ it.skillId == skills.get(1).skillId}.id
    }

    def "skill graph with rich hierarchy"() {
        List<Map> skills = SkillsFactory.createSkills(7)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(2).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(3).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(3).skillId, skills.get(4).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(3).skillId, skills.get(5).skillId)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(5).skillId, skills.get(6).skillId)

        when:
        def graphSkill0 = skillsService.getDependencyGraph(SkillsFactory.defaultProjId, skills.get(0).skillId)
        def graphSkill1 = skillsService.getDependencyGraph(SkillsFactory.defaultProjId, skills.get(1).skillId)
        def graphSkill3 = skillsService.getDependencyGraph(SkillsFactory.defaultProjId, skills.get(3).skillId)
        def graphSkill5 = skillsService.getDependencyGraph(SkillsFactory.defaultProjId, skills.get(5).skillId)

        then:
        graphSkill0.nodes.collect { it.skillId }.sort() == (0..6).collect { skills.get(it).skillId }
        def idMap0 = graphSkill0.nodes.collectEntries {[it.skillId, it.id]}
        def edges0 = graphSkill0.edges.collect { "${it.fromId}->${it.toId}" }
        edges0.remove("${idMap0.get(skills.get(0).skillId)}->${idMap0.get(skills.get(1).skillId)}")
        edges0.remove("${idMap0.get(skills.get(0).skillId)}->${idMap0.get(skills.get(2).skillId)}")
        edges0.remove("${idMap0.get(skills.get(0).skillId)}->${idMap0.get(skills.get(3).skillId)}")
        edges0.remove("${idMap0.get(skills.get(3).skillId)}->${idMap0.get(skills.get(4).skillId)}")
        edges0.remove("${idMap0.get(skills.get(3).skillId)}->${idMap0.get(skills.get(5).skillId)}")
        edges0.remove("${idMap0.get(skills.get(5).skillId)}->${idMap0.get(skills.get(6).skillId)}")
        !edges0

        // -------------------------
        !graphSkill1.nodes
        !graphSkill1.edges

        // -------------------------
        graphSkill3.nodes.collect { it.skillId }.sort() == (3..6).collect { skills.get(it).skillId }
        graphSkill3.edges.size() == 3
        def idMap3 = graphSkill3.nodes.collectEntries {[it.skillId, it.id]}
        def edges3= graphSkill3.edges.collect { "${it.fromId}->${it.toId}" }
        edges3.remove("${idMap3.get(skills.get(3).skillId)}->${idMap3.get(skills.get(4).skillId)}")
        edges3.remove("${idMap3.get(skills.get(3).skillId)}->${idMap3.get(skills.get(5).skillId)}")
        edges3.remove("${idMap3.get(skills.get(5).skillId)}->${idMap3.get(skills.get(6).skillId)}")
        !edges3

        // -------------------------
        graphSkill5.nodes.collect { it.skillId }.sort() == (5..6).collect { skills.get(it).skillId }
        graphSkill5.edges.size() == 1
        graphSkill5.edges.get(0).fromId == graphSkill5.nodes.find{ it.skillId == skills.get(5).skillId}.id
        graphSkill5.edges.get(0).toId == graphSkill5.nodes.find{ it.skillId == skills.get(6).skillId}.id
    }

    def "skill graph - verify attributes are populated in the nodes"() {
        List<Map> skills = SkillsFactory.createSkills(2)
        skills.get(1).pointIncrement = 20
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.addLearningPathPrerequisite(SkillsFactory.defaultProjId, skills.get(0).skillId, skills.get(1).skillId)

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId, skills.get(0).skillId)

        then:
        def node1 = graph.nodes.find{ it.skillId == skills.get(0).skillId}
        def node2 = graph.nodes.find{ it.skillId == skills.get(1).skillId}

        node1.projectId == SkillsFactory.defaultProjId
        node1.name ==  skills.get(0).name
        node1.pointIncrement ==  skills.get(0).pointIncrement
        node1.totalPoints == skills.get(0).pointIncrement * skills.get(0).numPerformToCompletion
        node1.type ==  "Skill"

        node2.projectId == SkillsFactory.defaultProjId
        node2.name ==  skills.get(1).name
        node2.pointIncrement ==  skills.get(1).pointIncrement
        node2.totalPoints ==  skills.get(1).pointIncrement * skills.get(1).numPerformToCompletion
        node2.type ==  "Skill"
    }
}
