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
package skills.intTests.skillReuse

import groovy.json.JsonOutput
import skills.intTests.catalog.CatalogIntSpec
import skills.services.admin.skillReuse.SkillReuseIdUtil

import static skills.intTests.utils.SkillsFactory.*

class VariousEndpoinsAndSkillReuseSpec extends CatalogIntSpec {

    def "get skills for project filter reuse tag in the name"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(1, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        when:
        def skills = skillsService.getSkillsForProject(p1.projectId)
        then:
        skills.size() == 2
        skills.name == [p1Skills[0].name, p1Skills[0].name]
        skills.isReused == [false, true]
        skills.skillId == [p1Skills[0].skillId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)]
    }

    def "get skills for project filter with name search to not find records when searching for the reuse tag"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(1, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        when:
        def skills = skillsService.getSkillsForProject(p1.projectId, SkillReuseIdUtil.REUSE_TAG)
        def skills1 = skillsService.getSkillsForProject(p1.projectId, p1Skills[0].name.toString().substring(0, 2))
        then:
        !skills
        skills1.name == [p1Skills[0].name, p1Skills[0].name]
        skills1.isReused == [false, true]
    }

    def "get skills for project - return reused info for group skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj2g2 = createSkillsGroup(1, 2, 22)
        skillsService.createSkill(p1subj2g2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId, p1subj2g2.skillId)

        when:
        def skills1 = skillsService.getSkillsForProject(p1.projectId, p1Skills[0].name)
        println JsonOutput.prettyPrint(JsonOutput.toJson(skills1))
        then:
        skills1.groupName == [p1subj1g1.name, p1subj2g2.name]
        skills1.groupId == [p1subj1g1.skillId, p1subj2g2.skillId]
        skills1.isReused == [false, true]
    }

    def "get skills for project without reused skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(1, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)

        when:
        def skills = skillsService.getSkillsForProject(p1.projectId, "", false, false, true)
        then:
        skills.isReused == [false]
        skills.skillId == [p1Skills[0].skillId]
    }

}

