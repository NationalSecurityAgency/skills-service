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
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.*

class SkillReuseSupportEntpointsSpec extends CatalogIntSpec {

    def "zero destinations available for reuse"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
//        skillsService.createSubject(p1subj2)

        when:
        def dest = skillsService.getReuseDestinationsForASkill(p1.projectId, p1Skills[0].skillId)
        then:
        !dest
    }

    def "one subject destinations available for reuse"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        when:
        def dest = skillsService.getReuseDestinationsForASkill(p1.projectId, p1Skills[0].skillId)
        then:
        dest.size() == 1
        dest[0].subjectId == "TestSubject2"
        !dest[0].groupId
    }

    def "multiple subjects destinations available for reuse"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj3 = createSubject(1, 3)
        skillsService.createSubject(p1subj3)
        def p1subj4 = createSubject(1, 4)
        skillsService.createSubject(p1subj4)

        when:
        def dest = skillsService.getReuseDestinationsForASkill(p1.projectId, p1Skills[0].skillId).sort({ it.subjectId })
        then:
        dest.size() == 3
        dest[0].subjectId == p1subj2.subjectId
        !dest[0].groupId

        dest[1].subjectId == p1subj3.subjectId
        !dest[1].groupId

        dest[2].subjectId == p1subj4.subjectId
        !dest[2].groupId
    }

    def "multiple subjects and groups destinations available for reuse"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)

        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)
        def p1subj3 = createSubject(1, 3)
        skillsService.createSubject(p1subj3)
        def p1subj4 = createSubject(1, 4)
        skillsService.createSubject(p1subj4)

        def s1_g1 = SkillsFactory.createSkillsGroup(1, 1, 10)
        def s1_g2 = SkillsFactory.createSkillsGroup(1, 1, 11)
        def s2_g1 = SkillsFactory.createSkillsGroup(1, 2, 12)
        def s3_g1 = SkillsFactory.createSkillsGroup(1, 3, 13)
        def s3_g2 = SkillsFactory.createSkillsGroup(1, 3, 14)
        skillsService.createSkills([s1_g1, s1_g2, s2_g1, s3_g1, s3_g2])

        when:
        def dest = skillsService.getReuseDestinationsForASkill(p1.projectId, p1Skills[0].skillId).sort { "${it.subjectId}-${it.groupId}" }
        then:
        dest.size() == 8

        dest[0].subjectId == p1subj1.subjectId
        dest[0].groupId == s1_g1.skillId
        dest[0].subjectName == p1subj1.name
        dest[0].groupName == s1_g1.name

        dest[1].subjectId == p1subj1.subjectId
        dest[1].groupId == s1_g2.skillId

        dest[2].subjectId == p1subj2.subjectId
        !dest[2].groupId

        dest[3].subjectId == p1subj2.subjectId
        dest[3].groupId == s2_g1.skillId

        dest[4].subjectId == p1subj3.subjectId
        !dest[4].groupId

        dest[5].subjectId == p1subj3.subjectId
        dest[5].groupId == s3_g1.skillId

        dest[6].subjectId == p1subj3.subjectId
        dest[6].groupId == s3_g2.skillId

        dest[7].subjectId == p1subj4.subjectId
        !dest[7].groupId
    }

    def "parent group should not be provided as destination option"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Skills = createSkills(3, 1, 1, 100)
        def s1_g1 = SkillsFactory.createSkillsGroup(1, 1, 10)
        def s1_g2 = SkillsFactory.createSkillsGroup(1, 1, 11)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [p1Skills, s1_g1, s1_g2].flatten())
        def skillUnderGroup = createSkill(1, 1, 26)
        skillsService.assignSkillToSkillsGroup(s1_g1.skillId, skillUnderGroup)
        when:
        def dest = skillsService.getReuseDestinationsForASkill(skillUnderGroup.projectId, skillUnderGroup.skillId).sort { it.groupId }
        then:
        dest.size() == 2

        dest[0].subjectId == p1subj1.subjectId
        !dest[0].groupId
        dest[0].subjectName == p1subj1.name
        !dest[0].groupName

        dest[1].subjectId == p1subj1.subjectId
        dest[1].groupId == s1_g2.skillId
        dest[1].subjectName == p1subj1.name
        dest[1].groupName == s1_g2.name
    }

    def "get reused skill usage stats"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        def p1subj3 = createSubject(1, 3)
        def p1subj4 = createSubject(1, 4)
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)
        skillsService.createSubject(p1subj3)
        def p1Skills_subj3 = createSkills(1, 1, 3, 100)
        skillsService.createSkills(p1Skills_subj3)
        skillsService.createSubject(p1subj4)

        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj3.subjectId)
        skillsService.reuseSkillInAnotherSubject(p1.projectId, p1Skills[0].skillId, p1subj4.subjectId)

        when:
        def res = skillsService.getExportedSkillStats(p1.projectId, p1Skills[0].skillId)
        def res1 = skillsService.getExportedSkillStats(p1.projectId, p1Skills[1].skillId)
        then:
        res.projectId == p1.projectId
        res.skillId == p1Skills[0].skillId
        !res.isExported
        !res.users
        !res.exportedOn
        res.isReusedLocally

        res1.projectId == p1.projectId
        res1.skillId == p1Skills[1].skillId
        !res1.isExported
        !res1.users
        !res1.exportedOn
        !res1.isReusedLocally
    }

    def "get reused group skill usage stats"() {
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
        def res = skillsService.getExportedSkillStats(p1.projectId, p1Skills[0].skillId)
        def res1 = skillsService.getExportedSkillStats(p1.projectId, p1Skills[1].skillId)
        then:
        res.projectId == p1.projectId
        res.skillId == p1Skills[0].skillId
        !res.isExported
        !res.users
        !res.exportedOn
        res.isReusedLocally

        res1.projectId == p1.projectId
        res1.skillId == p1Skills[1].skillId
        !res1.isExported
        !res1.users
        !res1.exportedOn
        !res1.isReusedLocally
    }

    def "disabled subject destinations are not available for reuse if the skill being moved is enabled "() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        p1subj2.enabled = false
        def p1Skills = createSkills(3, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        when:
        def dest = skillsService.getReuseDestinationsForASkill(p1.projectId, p1Skills[0].skillId)
        then:
        !dest
    }

    def "disabled subject destinations are available for reuse if the skill being moved is disabled "() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1subj2 = createSubject(1, 2)
        p1subj2.enabled = false
        def p1Skills = createSkills(3, 1, 1, 100)
        p1Skills[0].enabled = false
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Skills)
        skillsService.createSubject(p1subj2)

        when:
        def dest = skillsService.getReuseDestinationsForASkill(p1.projectId, p1Skills[0].skillId)
        then:
        dest.size() == 1
        dest[0].subjectId == "TestSubject2"
        !dest[0].groupId
    }
}
