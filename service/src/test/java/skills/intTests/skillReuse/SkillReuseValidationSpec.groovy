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

import skills.intTests.catalog.CatalogIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.services.admin.skillReuse.SkillReuseIdUtil
import spock.lang.IgnoreRest

import static skills.intTests.utils.SkillsFactory.*

class SkillReuseValidationSpec extends CatalogIntSpec {

    def "do not allow to reuse skill within the same subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Not allowed to reuse skill into the same subject [TestSubject1]")
    }

    def "do not allow to reuse skill within the same group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1])
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills.each {
            skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, it)
        }

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId, p1subj1g1.skillId)
        then:
        true
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Not allowed to reuse skill into the same group [skill11]")
    }

    def "skill can only be reused once per subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill with skillIds of [skill1] are already reused in [TestSubject2]")
    }

    def "skill can only be reused once per group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1Skills, p1subj1g1].flatten())

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId, p1subj1g1.skillId)
        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj1.subjectId, p1subj1g1.skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill with skillIds of [skill1] are already reused in [skill11]")
    }

    def "cannot create group skill with the reuse tag in its skillId"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1].flatten())
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills[0].skillId = "skill${SkillReuseIdUtil.REUSE_TAG}".toString()
        when:
        skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, p1Skills[0])
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill ID must not contain reuse tag")
    }

    def "cannot create group skill with the reuse tag in its name"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1subj1g1].flatten())
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills[0].name = "skill ${SkillReuseIdUtil.REUSE_TAG}".toString()
        when:
        skillsService.assignSkillToSkillsGroup(p1subj1g1.skillId, p1Skills[0])
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill Name must not contain reuse tag")
    }

    def "cannot create skill with the reuse tag in its skillId"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [].flatten())
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills[0].skillId = "sk${SkillReuseIdUtil.REUSE_TAG}blja".toString()
        when:
        skillsService.createSkill(p1Skills[0])
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill ID must not contain reuse tag")
    }

    def "cannot create skill with the reuse tag in its name"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [].flatten())
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        p1Skills[0].name = "${SkillReuseIdUtil.REUSE_TAG} blja".toString()
        when:
        skillsService.createSkill(p1Skills[0])
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill Name must not contain reuse tag")
    }

}
