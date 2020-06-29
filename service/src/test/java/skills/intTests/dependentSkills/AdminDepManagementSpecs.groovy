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
        ].sort()

        graphAfterRemoval.nodes.collect { it.skillId }.sort() == [skills.get(0).skillId, skills.get(1).skillId]
        graphAfterRemoval.edges.size() == 1
        graphAfterRemoval.edges.collect { it.toId }.sort() == [
                graphAfterRemoval.nodes.find{ it.skillId == skills.get(1).skillId}.id,
        ]
    }

}
