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
package skills.intTests

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

import static skills.intTests.utils.SkillsFactory.*

class DisabledSubjectSpecs extends DefaultIntSpec {

    def "can create an initially disabled skill to a disabled subject"() {
        def proj1 = createProject(1)
        skillsService.createProject(proj1)

        when:
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = false
        skillsService.createSubject(proj1_subj)
        def subject = skillsService.getSubject(proj1_subj)

        then:
        subject
        subject.enabled == false
    }

    def "can add a disabled skill to a disabled subject"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)

        when:
        def proj1_skill = createSkill(1, 1, 1)
        proj1_skill.enabled = false
        skillsService.createSkill(proj1_skill)
        def projects = skillsService.getProjects()

        then:
        projects
        projects.size() == 1
        projects[0].numSkills == 0
        projects[0].numSkillsDisabled == 1
        projects[0].totalPoints == 0
    }

    def "cannot add an enabled skill to a disabled subject"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)

        when:
        def proj1_skill = createSkill(1, 1, 1)
        proj1_skill.enabled = true
        skillsService.createSkill(proj1_skill)

        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot enable Skill [${proj1_skill.skillId}] because it's Subject [${proj1_subj.subjectId}] is disabled")
    }

    def "can add a disabled skill group to a disabled subject"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)

        when:
        def skillsGroup = createSkillsGroup(1,1,2)
        skillsGroup.enabled = false
        skillsService.createSkill(skillsGroup)

        def proj1_skill = createSkill(1, 1, 1)
        proj1_skill.enabled = false
        skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, proj1_skill)
        def projects = skillsService.getProjects()

        def subject = skillsService.getSubject(proj1_subj)

        then:
        projects
        projects.size() == 1
        projects[0].numSkills == 0
        projects[0].numSkillsDisabled == 1
        projects[0].totalPoints == 0
        subject
        subject.enabled == false
        subject.numSkills == 0
        subject.numSkillsDisabled == 1
        subject.numGroups == 0
        subject.numGroupsDisabled == 1
        subject.totalPoints == 0
    }

    def "cannot add an enabled skill group to a disabled subject"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)

        when:
        def proj1_skill = createSkill(1, 1, 1)
        proj1_skill.enabled = true
        skillsService.createSkill(proj1_skill)

        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot enable Skill [${proj1_skill.skillId}] because it's Subject [${proj1_subj.subjectId}] is disabled")
    }

    def "cannot enable a disabled skill that is part of a disabled subject"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        def proj1_skill = createSkill(1, 1, 1)
        proj1_skill.enabled = false
        skillsService.createSkill(proj1_skill)

        when:
        proj1_skill.enabled = true
        skillsService.updateSkill(proj1_skill, proj1_skill.skillId)

        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot enable Skill [${proj1_skill.skillId}] because it's Subject [${proj1_subj.subjectId}] is disabled")
    }

    def "cannot enable a disabled skill group that is part of a disabled subject"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = false
        def skillsGroup = createSkillsGroup(1, 1, 5)
        skillsGroup.enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkill(skillsGroup)

        when:
        skillsGroup.enabled = true
        skillsService.updateSkill(skillsGroup, skillsGroup.skillId)

        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot enable Skill [${skillsGroup.skillId}] because it's Subject [${proj1_subj.subjectId}] is disabled")
    }

    def "cannot disable an already enabled subject"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, [])

        when:
        p1subj1.enabled = false
        skillsService.updateSubject(p1subj1, p1subj1.subjectId)
        then:

        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot disable an existing enabled Subject. SubjectId=[${p1subj1.subjectId}]")
    }

    def "disabled subject from getProjects"() {
        def proj1 = createProject(1)
        def proj1Subj1 = createSubject(1, 1)
        proj1Subj1.enabled = false
        List<Map> proj1Subj1Skills = createSkills(3, 1, 1)
        proj1Subj1Skills.each { it.enabled = false }

        def proj1Subj2 = createSubject(1, 2)
        List<Map> proj1Subj2Skills = createSkills(3, 1, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1Subj1)
        skillsService.createSkills(proj1Subj1Skills)
        skillsService.createSubject(proj1Subj2)
        skillsService.createSkills(proj1Subj2Skills)

        def skillsGroup = createSkillsGroup(1,1,4)
        skillsGroup.enabled = false
        skillsService.createSkill(skillsGroup)

        def childSkill = createSkill(1, 1, 5)
        childSkill.enabled = false
        skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, childSkill)

        when:
        def projects = skillsService.getProjects()
        def projectsSummary = skillsService.getSkillsSummaryForCurrentUser(proj1.projectId)
        proj1Subj1.enabled = true
        skillsService.updateSubject(proj1Subj1, proj1Subj1.subjectId)
        def projectsAfter = skillsService.getProjects()
        def projectsSummaryAfter = skillsService.getSkillsSummaryForCurrentUser(proj1.projectId)

        then:
        projects
        projects.size() == 1
        projects[0].numSkills == 3
        projects[0].numSkillsDisabled == 4
        projects[0].totalPoints == 30

        projectsSummary
        projectsSummary.projectId == proj1.projectId
        projectsSummary.totalPoints == 30
        projectsSummary.subjects.size() == 1
        projectsSummary.subjects[0].subjectId == proj1Subj2.subjectId
        projectsSummary.subjects[0].totalPoints == 30
        projectsSummary.subjects[0].totalSkills == 3

        projectsAfter
        projectsAfter.size() == 1
        projectsAfter[0].numSkills == 7
        projectsAfter[0].numSkillsDisabled == 0
        projectsAfter[0].totalPoints == 70

        projectsSummaryAfter
        projectsSummaryAfter.projectId == proj1.projectId
        projectsSummaryAfter.totalPoints == 70
        projectsSummaryAfter.subjects.size() == 2
        projectsSummaryAfter.subjects[0].subjectId == proj1Subj1.subjectId
        projectsSummaryAfter.subjects[0].totalPoints == 40
        projectsSummaryAfter.subjects[0].totalSkills == 4
        projectsSummaryAfter.subjects[1].subjectId == proj1Subj2.subjectId
        projectsSummaryAfter.subjects[1].totalPoints == 30
        projectsSummaryAfter.subjects[1].totalSkills == 3
    }

    def "cannot get subject summary for a disabled subject"() {
        def proj1 = createProject(1)
        def proj1Subj1 = createSubject(1, 1)
        proj1Subj1.enabled = false
        List<Map> proj1Subj1Skills = createSkills(3, 1, 1)
        proj1Subj1Skills.each { it.enabled = false }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1Subj1)
        skillsService.createSkills(proj1Subj1Skills)

        when:
        skillsService.getSubjectSummaryForCurrentUser(proj1.projectId, proj1Subj1.subjectId)

        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.resBody.contains("Skill with id [TestSubject1] is not enabled")
    }

    def "disabled subject from getSubject"() {
        def proj1 = createProject(1)
        def proj1Subj1 = createSubject(1, 1)
        proj1Subj1.enabled = false
        List<Map> proj1Subj1Skills = createSkills(3, 1, 1)
        proj1Subj1Skills.each { it.enabled = false }

        def proj1Subj2 = createSubject(1, 2)
        List<Map> proj1Subj2Skills = createSkills(3, 1, 2)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1Subj1)
        skillsService.createSkills(proj1Subj1Skills)
        skillsService.createSubject(proj1Subj2)
        skillsService.createSkills(proj1Subj2Skills)

        def skillsGroup = createSkillsGroup(1,1,4)
        skillsGroup.enabled = false
        skillsService.createSkill(skillsGroup)

        def childSkill = createSkill(1, 1, 5)
        childSkill.enabled = false
        skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, childSkill)

        when:

        def subject = skillsService.getSubject(proj1Subj1)
        proj1Subj1.enabled = true
        skillsService.updateSubject(proj1Subj1, proj1Subj1.subjectId)
        def subjectSummaryAfter = skillsService.getSubjectSummaryForCurrentUser(proj1.projectId, proj1Subj1.subjectId)

        def subjectAfter = skillsService.getSubject(proj1Subj1)
        then:
        subject
        subject.numSkills == 0
        subject.numSkillsDisabled == 4
        subject.numGroups == 0
        subject.numGroupsDisabled == 1
        subject.totalPoints == 0

        subjectAfter
        subjectAfter.numSkills == 4
        subjectAfter.numSkillsDisabled == 0
        subjectAfter.numGroups == 1
        subjectAfter.numGroupsDisabled == 0
        subjectAfter.totalPoints == 40

        subjectSummaryAfter
        subjectSummaryAfter.subjectId == proj1Subj1.subjectId
        subjectSummaryAfter.totalPoints == 40
        subjectSummaryAfter.totalSkills == 4
        subjectSummaryAfter.skills.size() == 4
    }

    def "enabling a subject with imported skills does not enabled the imported skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)
        skillsService.bulkExportSkillsToCatalog(p1.projectId, p1skills.collect { it.skillId })

        def p2 = createProject(2)
        def p2subj2 = createSubject(2, 2)
        p2subj2.enabled = 'false'
        def p2skillsGroup = createSkillsGroup(2, 2, 10)
        p2skillsGroup.enabled = 'false'
        def p2skills = createSkills(5, 2, 2, 100)
        p2skills.each { it.enabled = 'false' }
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj2, [p2skills, p2skillsGroup].flatten())

        def nonImportedChildSkill = createSkill(2, 2, 55)
        nonImportedChildSkill.enabled = 'false'
        skillsService.assignSkillToSkillsGroup(p2skillsGroup.skillId, nonImportedChildSkill)

        skillsService.bulkImportSkillsIntoGroupFromCatalog(p2.projectId, p2subj2.subjectId, p2skillsGroup.skillId,
                p1skills.collect { [projectId: it.projectId, skillId: it.skillId] })

        when:

        def subject = skillsService.getSubject(p2subj2)
        p2subj2.enabled = 'true'
        skillsService.updateSubject(p2subj2, p2subj2.subjectId)
        def subjectSummaryAfter = skillsService.getSubjectSummaryForCurrentUser(p2.projectId, p2subj2.subjectId)

        def subjectAfter = skillsService.getSubject(p2subj2)
        then:
        subject
        subject.numSkills == 0
        subject.numSkillsDisabled == 11
        subject.numSkillsImportedAndDisabled == 5
        subject.numGroups == 0
        subject.numGroupsDisabled == 1
        subject.totalPoints == 0

        subjectAfter
        subjectAfter.numSkills == 6
        subjectAfter.numSkillsDisabled == 5  // imported skills are still disabled
        subjectAfter.numSkillsImportedAndDisabled == 5
        subjectAfter.numGroups == 1
        subjectAfter.numGroupsDisabled == 0
        subjectAfter.totalPoints == 510

        subjectSummaryAfter
        subjectSummaryAfter.subjectId == p2subj2.subjectId
        subjectSummaryAfter.totalPoints == 510
        subjectSummaryAfter.totalSkills == 6
        subjectSummaryAfter.skills.size() == 6
    }
}
