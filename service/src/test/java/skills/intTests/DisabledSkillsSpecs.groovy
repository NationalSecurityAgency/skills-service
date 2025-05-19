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
import skills.services.admin.skillReuse.SkillReuseIdUtil

import static skills.intTests.utils.SkillsFactory.*

class DisabledSkillsSpecs extends DefaultIntSpec {

    def "disabled skills from getProjects"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:

        def projects = skillsService.getProjects()
        proj1_skills[0].enabled = true
        skillsService.updateSkill(proj1_skills[0], proj1_skills[0].skillId)

        def projectsAfter = skillsService.getProjects()
        then:
        projects
        projects.size() == 1
        projects[0].numSkills == 2
        projects[0].numSkillsDisabled == 1
        projects[0].totalPoints == 20

        projectsAfter
        projectsAfter.size() == 1
        projectsAfter[0].numSkills == 3
        projectsAfter[0].numSkillsDisabled == 0
        projectsAfter[0].totalPoints == 30
    }

    def "disabled skills from getProject"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:

        def project = skillsService.getProject(proj1.projectId)
        proj1_skills[0].enabled = true
        skillsService.updateSkill(proj1_skills[0], proj1_skills[0].skillId)

        def projectAfter = skillsService.getProject(proj1.projectId)
        then:
        project
        project.numSkills == 2
        project.numSkillsDisabled == 1
        project.totalPoints == 20

        projectAfter
        projectAfter.numSkills == 3
        projectAfter.numSkillsDisabled == 0
        projectAfter.totalPoints == 30
    }

    def "disabled skills from getSubject"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:

        def subject = skillsService.getSubject(proj1_subj)
        proj1_skills[0].enabled = true
        skillsService.updateSkill(proj1_skills[0], proj1_skills[0].skillId)

        def subjectAfter = skillsService.getSubject(proj1_subj)
        then:
        subject
        subject.numSkills == 2
        subject.numSkillsDisabled == 1
        subject.totalPoints == 20

        subjectAfter
        subjectAfter.numSkills == 3
        subjectAfter.numSkillsDisabled == 0
        subjectAfter.totalPoints == 30
    }

    def "disabled skills from getSkillsForSubject"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:

        def skills = skillsService.getSkillsForSubject(proj1_subj.projectId, proj1_subj.subjectId)
        proj1_skills[0].enabled = true
        skillsService.updateSkill(proj1_skills[0], proj1_skills[0].skillId)

        def skillsAfter = skillsService.getSkillsForSubject(proj1_subj.projectId, proj1_subj.subjectId)
        then:
        skills
        skills.size() == 3
        skills.findAll { it.enabled == false }.size() == 1
        skills.findAll { it.enabled == true }.size() == 2

        skillsAfter
        skillsAfter.size() == 3
        skillsAfter.findAll { it.enabled == false }.size() == 0
        skillsAfter.findAll { it.enabled == true }.size() == 3
    }

    def "getSkillsForProject does not include disabled skills"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = false

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:

        def skills = skillsService.getSkillsForProject(proj1.projectId)
        proj1_skills[0].enabled = true
        skillsService.updateSkill(proj1_skills[0], proj1_skills[0].skillId)

        def skillsAfter = skillsService.getSkillsForProject(proj1.projectId)
        then:
        skills
        skills.size() == 2

        skillsAfter
        skillsAfter.size() == 3
    }

    def "cannot report skill events for disabled skills"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = false
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, proj1_skills)

        when:
        skillsService.addSkill([projectId: p1.projectId, skillId: proj1_skills[0].skillId], getRandomUsers(1)[0], new Date() - 1)
        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot report skill events for a skill that is disabled.")
    }

    def "disabled skill cannot be added to a badge"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = false
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, proj1_skills)
        def badge = createBadge()
        badge.enabled = false
        skillsService.createBadge(badge)

        when:
        skillsService.assignSkillToBadge(projectId: p1.projectId, badgeId: badge.badgeId, skillId: proj1_skills[0].skillId)

        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill [skill1] is not enabled,")
    }

    def "cannot disable an already enabled skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        List<Map> proj1_skills = createSkills(3, 1, 1)
        proj1_skills[0].enabled = true
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, proj1_skills)

        when:
        proj1_skills[0].enabled = false
        skillsService.updateSkill(proj1_skills[0], proj1_skills[0].skillId)
        then:

        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Skill [${proj1_skills[0].skillId}] has already been enabled and cannot be disabled.")
    }

    def "can add a disabled skill to an enabled skill group"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = true

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)

        when:
        def skillsGroup = createSkillsGroup(1,1,2)
        skillsGroup.enabled = true
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
        subject.enabled == true
        subject.numSkills == 0
        subject.numSkillsDisabled == 1
        subject.numGroups == 1
        subject.numGroupsDisabled == 0
        subject.totalPoints == 0
    }

    def "cannot add an enabled skill to a disabled skill group"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = true

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)

        when:
        def skillsGroup = createSkillsGroup(1,1,2)
        skillsGroup.enabled = false
        skillsService.createSkill(skillsGroup)

        def proj1_skill = createSkill(1, 1, 1)
        proj1_skill.enabled = true
        skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, proj1_skill)

        then:
        SkillsClientException ex = thrown(SkillsClientException)
        ex.message.contains("Cannot enable Skill [skill1] becuase it's SkillsGroup [skill2] is disabled")
    }

    def "editing a skill group with a disabled skill does not miscalculate the total points"() {
        def proj1 = createProject(1)
        def proj1_subj = createSubject(1, 1)
        proj1_subj.enabled = true

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)

        when:
        def skillsGroup = createSkillsGroup(1,1,3)
        skillsGroup.enabled = true
        skillsService.createSkill(skillsGroup)

        def proj1_skill1 = createSkill(1, 1, 1)
        proj1_skill1.enabled = false
        skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, proj1_skill1)
        def proj1_skill2 = createSkill(1, 1, 2)
        skillsService.assignSkillToSkillsGroup(skillsGroup.skillId, proj1_skill2)
        def projects = skillsService.getProjects()
        def subject = skillsService.getSubject(proj1_subj)
        def skillGroupBeforeUpdate = skillsService.getSkill(skillsGroup)

        skillsGroup.description = 'updated'
        skillsService.updateSkill(skillsGroup)
        def subjectAfterUpdate = skillsService.getSubject(proj1_subj)

        def skillGroupAfterUpdate = skillsService.getSkill(skillsGroup)

        then:
        projects
        projects.size() == 1
        projects[0].numSkills == 1
        projects[0].numSkillsDisabled == 1
        projects[0].totalPoints == 10
        subject
        subject.enabled == true
        subject.numSkills == 1
        subject.numSkillsDisabled == 1
        subject.numGroups == 1
        subject.numGroupsDisabled == 0
        subject.totalPoints == 10
        subjectAfterUpdate
        subjectAfterUpdate.enabled == true
        subjectAfterUpdate.numSkills == 1
        subjectAfterUpdate.numSkillsDisabled == 1
        subjectAfterUpdate.numGroups == 1
        subjectAfterUpdate.numGroupsDisabled == 0
        subjectAfterUpdate.totalPoints == 10
        skillGroupBeforeUpdate
        skillGroupBeforeUpdate.totalPoints == 10
        skillGroupAfterUpdate
        skillGroupAfterUpdate.totalPoints == 10
    }

}
