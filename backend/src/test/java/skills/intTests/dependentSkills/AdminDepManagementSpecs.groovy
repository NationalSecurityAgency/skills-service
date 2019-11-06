package skills.intTests.dependentSkills

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class AdminDepManagementSpecs extends DefaultIntSpec {

    def "remove dependency"() {
        List<Map> skills = SkillsFactory.createSkills(3)
        def subject = SkillsFactory.createSubject()

        skillsService.createProject(SkillsFactory.createProject())
        skillsService.createSubject(subject)
        skillsService.createSkills(skills)

        skillsService.assignDependency([projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(1).skillId])
        Map dep2 = [projectId: SkillsFactory.defaultProjId, skillId: skills.get(0).skillId, dependentSkillId: skills.get(2).skillId]
        skillsService.assignDependency(dep2)

        when:
        def graph = skillsService.getDependencyGraph(SkillsFactory.defaultProjId)
        skillsService.removeDependency(dep2)
        def graphAfterRemoval = skillsService.getDependencyGraph(SkillsFactory.defaultProjId)

        then:
        graph.nodes.collect { it.skillId }.sort() == [skills.get(0).skillId, skills.get(1).skillId, skills.get(2).skillId,]

        graph.edges.size() == 2
        graph.edges.collect { it.toId }.sort() == [
                graph.nodes.find{ it.skillId == skills.get(1).skillId}.id,
                graph.nodes.find{ it.skillId == skills.get(2).skillId}.id
        ]

        graphAfterRemoval.nodes.collect { it.skillId }.sort() == [skills.get(0).skillId, skills.get(1).skillId]
        graphAfterRemoval.edges.size() == 1
        graphAfterRemoval.edges.collect { it.toId }.sort() == [
                graph.nodes.find{ it.skillId == skills.get(1).skillId}.id,
        ]
    }

}
