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
import skills.intTests.utils.SkillsService
import skills.services.admin.skillReuse.SkillReuseIdUtil

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

    def "reused skill cannot be added as a badge dependency"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        def badge = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge)

        when:
        skillsService.assignSkillToBadge(p1.projectId, badge.badgeId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill ID must not contain reuse tag")
    }

    def "reused skill cannot be added as a global badge dependency"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        SkillsService supervisorService = createSupervisor()
        def badge = SkillsFactory.createBadge(1, 1)
        supervisorService.createGlobalBadge(badge)

        when:
        supervisorService.assignSkillToGlobalBadge(p1.projectId, badge.badgeId, SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0))
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill ID must not contain reuse tag")
    }

    def "reused skill cannot be assigned as a dependency"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(1).skillId, SkillReuseIdUtil.addTag(p1Skills.get(0).skillId, 0))
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("From ID must not contain reuse tag")
    }

    def "reused skill cannot be assigned as a cross-project dependency"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        when:
        skillsService.shareSkill(p1.projectId, SkillReuseIdUtil.addTag("valu", 1), "other")
        skillsService.addLearningPathPrerequisite(p1.projectId, SkillReuseIdUtil.addTag("first", 2),"other", "second")
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill ID must not contain reuse tag")
    }

    def "reused skill cannot be in a cross-project dependency as dependentSkillId"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        when:
        skillsService.shareSkill(p1.projectId, SkillReuseIdUtil.addTag("valu", 1), "other")
        skillsService.addLearningPathPrerequisite(p1.projectId, "blah", "other", SkillReuseIdUtil.addTag("first", 2))
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill ID must not contain reuse tag")
    }

    def "reused skill cannot be shared for a cross-project dependency"() {
        def p1 = createProject(1)
        skillsService.createProject(p1)
        when:
        skillsService.shareSkill(p1.projectId, SkillReuseIdUtil.addTag("valu", 1), "other")
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill ID must not contain reuse tag")
    }


    def "reused skill cannot be assigned as a dependentSkillId"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, SkillReuseIdUtil.addTag(p1Skills.get(0).skillId, 0), p1Skills.get(1).skillId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("To ID must not contain reuse tag")
    }

    def "cannot reuse if a finalization is running"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p2Skills = createSkills(3, 1, 2, 100, 5)
        skillsService.createSkills(p2Skills)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skills = createSkills(10, 2, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2skills)
        def p2ExportedSkills = p2skills[3..7]
        p2ExportedSkills.each { skillsService.exportSkillToCatalog(it.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalog(p1.projectId, p1subj1.subjectId, p2ExportedSkills.collect { [projectId: it.projectId, skillId: it.skillId] })

        when:
        skillsService.finalizeSkillsImportFromCatalog(p1.projectId, false)
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot reuse skills while finalization is running")
    }

    def "cannot reuse if a finalization is pending"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skills = createSkills(10, 2, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2skills)
        def p2ExportedSkills = p2skills[3..7]
        p2ExportedSkills.each { skillsService.exportSkillToCatalog(it.projectId, it.skillId) }

        skillsService.bulkImportSkillsFromCatalog(p1.projectId, p1subj1.subjectId, p2ExportedSkills.collect { [projectId: it.projectId, skillId: it.skillId] })

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot reuse skills while finalization is pending")
    }

    def "cannot report skill events to reused skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        when:
        skillsService.addSkill([projectId: p1.projectId, skillId: SkillReuseIdUtil.addTag(p1Skills[0].skillId, 0)], getRandomUsers(1)[0], new Date() - 1)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skills imported from the catalog can only be reported if the original skill is configured for Self Reporting")
    }

    def "do not allow to reuse skill with a dependency"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(0).skillId, p1Skills.get(1).skillId)

        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill must have no dependencies in order to reuse; the skill [skill1] has [1] dependencie(s)")
    }

    def "do not allow to add dependency if skill is already reused in another subject/group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)

        when:
        skillsService.addLearningPathPrerequisite(p1.projectId, p1Skills.get(0).skillId, p1Skills.get(1).skillId)

        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill [skill1] was reused in another subject or group and cannot have prerequisites in the learning path")
    }

    def "do not allow reuse of a disabled skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills[0].enabled = false
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)


        when:
        skillsService.reuseSkills(p1.projectId, [p1Skills[0].skillId], p1subj2.subjectId)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Not allowed to reuse a disabled skill")
    }
}
