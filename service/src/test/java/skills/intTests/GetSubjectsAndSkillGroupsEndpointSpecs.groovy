/**
 * Copyright 2024 SkillTree
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

import skills.intTests.copyProject.CopyIntSpec
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class GetSubjectsAndSkillGroupsEndpointSpecs extends CopyIntSpec {

    def "project with subjects"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def p2subj2 = createSubject(2, 3)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [])
        skillsService.createSubject(p2subj2)

        when:
        def p1Res = skillsService.getSubjectsAndSkillGroups(p1.projectId)
        def p2Res = skillsService.getSubjectsAndSkillGroups(p2.projectId)
        then:
        p1Res.skillId == [p1subj1.subjectId]
        p1Res.name == [p1subj1.name]
        p1Res.type == [SkillDef.ContainerType.Subject.toString()]

        p2Res.skillId == [p2subj1.subjectId, p2subj2.subjectId]
        p2Res.name == [p2subj1.name, p2subj2.name]
        p2Res.type == [SkillDef.ContainerType.Subject.toString(), SkillDef.ContainerType.Subject.toString()]
    }

    def "project with subjects and groups"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Group1 = createSkillsGroup(1, 1, 4)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1Group1])
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.assignSkillToSkillsGroup(p1Group1.skillId, p1Skills[0])
        skillsService.assignSkillToSkillsGroup(p1Group1.skillId, p1Skills[1])
        skillsService.assignSkillToSkillsGroup(p1Group1.skillId, p1Skills[2])

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 2)
        def destGroup = createSkillsGroup(2, 2, 4)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, [destGroup])

        when:
        def p1Res = skillsService.getSubjectsAndSkillGroups(p1.projectId)
        then:
        p1Res.skillId == [p1subj1.subjectId, p1Group1.skillId]
        p1Res.name == [p1subj1.name, p1Group1.name]
        p1Res.type == [SkillDef.ContainerType.Subject.toString(), SkillDef.ContainerType.SkillsGroup.toString()]
    }

}
