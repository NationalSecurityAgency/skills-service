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
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(1).skillId])

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId)

        then:
        graph.nodes.collect { it.skillId }.sort() == [skills.get(0).skillId, skills.get(1).skillId,]

        graph.edges.size() == 1
        graph.edges.get(0).fromId == graph.nodes.find{ it.skillId == skills.get(0).skillId}.id
        graph.edges.get(0).toId == graph.nodes.find{ it.skillId == skills.get(1).skillId}.id
    }

    def "project graph with rich hierarchy"() {
        List<Map> skills = SkillsFactory.createSkills(7)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(1).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(2).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(3).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId, dependentSkillId: skills.get(4).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId, dependentSkillId: skills.get(5).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId, dependentSkillId: skills.get(6).skillId])

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId)

        then:
        graph.nodes.collect { it.skillId }.sort() == (0..6).collect { skills.get(it).skillId }

        graph.edges.size() == 6
        def sortedEdges = graph.edges.sort { a,b -> a.fromId <=> b.fromId ?: a.toId <=> b.toId }
        sortedEdges.get(0).fromId == graph.nodes.find{ it.skillId == skills.get(0).skillId}.id
        sortedEdges.get(0).toId == graph.nodes.find{ it.skillId == skills.get(1).skillId}.id

        sortedEdges.get(1).fromId == graph.nodes.find{ it.skillId == skills.get(0).skillId}.id
        sortedEdges.get(1).toId == graph.nodes.find{ it.skillId == skills.get(2).skillId}.id

        sortedEdges.get(2).fromId == graph.nodes.find{ it.skillId == skills.get(0).skillId}.id
        sortedEdges.get(2).toId == graph.nodes.find{ it.skillId == skills.get(3).skillId}.id

        sortedEdges.get(3).fromId == graph.nodes.find{ it.skillId == skills.get(3).skillId}.id
        sortedEdges.get(3).toId == graph.nodes.find{ it.skillId == skills.get(4).skillId}.id

        sortedEdges.get(4).fromId == graph.nodes.find{ it.skillId == skills.get(3).skillId}.id
        sortedEdges.get(4).toId == graph.nodes.find{ it.skillId == skills.get(5).skillId}.id

        sortedEdges.get(5).fromId == graph.nodes.find{ it.skillId == skills.get(5).skillId}.id
        sortedEdges.get(5).toId == graph.nodes.find{ it.skillId == skills.get(6).skillId}.id
    }

    def "project graph - verify attributes are populated in the nodes"() {
        List<Map> skills = SkillsFactory.createSkills(2)
        skills.get(1).pointIncrement = 20
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(1).skillId])

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
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(1).skillId])

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
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(1).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(2).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(3).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId, dependentSkillId: skills.get(4).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(3).skillId, dependentSkillId: skills.get(5).skillId])
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(5).skillId, dependentSkillId: skills.get(6).skillId])

        when:
        def graphSkill0 = skillsService.getDependencyGraph(SkillsFactory.defaultProjId, skills.get(0).skillId)
        def graphSkill1 = skillsService.getDependencyGraph(SkillsFactory.defaultProjId, skills.get(1).skillId)
        def graphSkill3 = skillsService.getDependencyGraph(SkillsFactory.defaultProjId, skills.get(3).skillId)
        def graphSkill5 = skillsService.getDependencyGraph(SkillsFactory.defaultProjId, skills.get(5).skillId)

        then:
        graphSkill0.nodes.collect { it.skillId }.sort() == (0..6).collect { skills.get(it).skillId }

        graphSkill0.edges.size() == 6
        def sortedEdgesSkill0 = graphSkill0.edges.sort { a,b -> a.fromId <=> b.fromId ?: a.toId <=> b.toId }
        sortedEdgesSkill0.get(0).fromId == graphSkill0.nodes.find{ it.skillId == skills.get(0).skillId}.id
        sortedEdgesSkill0.get(0).toId == graphSkill0.nodes.find{ it.skillId == skills.get(1).skillId}.id

        sortedEdgesSkill0.get(1).fromId == graphSkill0.nodes.find{ it.skillId == skills.get(0).skillId}.id
        sortedEdgesSkill0.get(1).toId == graphSkill0.nodes.find{ it.skillId == skills.get(2).skillId}.id

        sortedEdgesSkill0.get(2).fromId == graphSkill0.nodes.find{ it.skillId == skills.get(0).skillId}.id
        sortedEdgesSkill0.get(2).toId == graphSkill0.nodes.find{ it.skillId == skills.get(3).skillId}.id

        sortedEdgesSkill0.get(3).fromId == graphSkill0.nodes.find{ it.skillId == skills.get(3).skillId}.id
        sortedEdgesSkill0.get(3).toId == graphSkill0.nodes.find{ it.skillId == skills.get(4).skillId}.id

        sortedEdgesSkill0.get(4).fromId == graphSkill0.nodes.find{ it.skillId == skills.get(3).skillId}.id
        sortedEdgesSkill0.get(4).toId == graphSkill0.nodes.find{ it.skillId == skills.get(5).skillId}.id

        sortedEdgesSkill0.get(5).fromId == graphSkill0.nodes.find{ it.skillId == skills.get(5).skillId}.id
        sortedEdgesSkill0.get(5).toId == graphSkill0.nodes.find{ it.skillId == skills.get(6).skillId}.id

        // -------------------------
        !graphSkill1.nodes
        !graphSkill1.edges

        // -------------------------
        graphSkill3.nodes.collect { it.skillId }.sort() == (3..6).collect { skills.get(it).skillId }
        graphSkill3.edges.size() == 3
        def sortedEdgesSkill3 = graphSkill3.edges.sort { a,b -> a.fromId <=> b.fromId ?: a.toId <=> b.toId }
        sortedEdgesSkill3.get(0).fromId == graphSkill3.nodes.find{ it.skillId == skills.get(3).skillId}.id
        sortedEdgesSkill3.get(0).toId == graphSkill3.nodes.find{ it.skillId == skills.get(4).skillId}.id

        sortedEdgesSkill3.get(1).fromId == graphSkill3.nodes.find{ it.skillId == skills.get(3).skillId}.id
        sortedEdgesSkill3.get(1).toId == graphSkill3.nodes.find{ it.skillId == skills.get(5).skillId}.id

        sortedEdgesSkill3.get(2).fromId == graphSkill3.nodes.find{ it.skillId == skills.get(5).skillId}.id
        sortedEdgesSkill3.get(2).toId == graphSkill3.nodes.find{ it.skillId == skills.get(6).skillId}.id

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
        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(1).skillId])

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
