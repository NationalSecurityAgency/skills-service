/**
 * Copyright 2026 SkillTree
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
package skills.intTests.batchUpdateSkills

import skills.intTests.utils.DefaultIntSpec

import static skills.intTests.utils.SkillsFactory.*

class BatchUpdateSkillsSpec extends DefaultIntSpec {

    def setup() {

        // project 2 shouldn't affect project 1 used in the actual tests
        def p2 = createProject(2)
        def p2subj1 = createSubject(2, 1)
        def p2skills = createSkills(5, 2, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p2, p2subj1, p2skills)
    }


    def "batch update point increment - definition only"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)


        int subject1Pts = 100*2 + 555*3
        int subject2Pts  = 22 * p1skillsSubj2.size()
        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                pointIncrement: 555,
                skills: p1skills[1..3].skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subjects = skillsService.getSubjects(p1.projectId)
        def project = skillsService.getProject(p1.projectId)
        then:
        subjSkills.collect { it.pointIncrement } == [100, 555, 555, 555, 100]

        subjects.collect { it.totalPoints } == [subject1Pts, subject2Pts]
        project.totalPoints == subject1Pts + subject2Pts
    }

    def "batch update numPerformToCompletion - definition only"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        int subject1Pts = 100*2 + 100*5*3
        int subject2Pts = 22 * p1skillsSubj2.size()
        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                numPerformToCompletion: 5,
                skills: p1skills[1..3].skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subjects = skillsService.getSubjects(p1.projectId)
        def project = skillsService.getProject(p1.projectId)
        then:
        subjSkills.collect { it.pointIncrement } == [100, 100, 100, 100, 100]
        subjSkills.collect { it.totalPoints } == [100, 500, 500, 500, 100]

        subjects.collect { it.totalPoints } == [subject1Pts, subject2Pts]
        project.totalPoints == subject1Pts + subject2Pts
    }

    def "batch update pointIncrementInterval - definition only"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        int subject1Pts = 100 * p1skills.size()
        int subject2Pts = 22 * p1skillsSubj2.size()
        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                pointIncrementInterval: 720,
                skills: p1skills[1..3].skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subjects = skillsService.getSubjects(p1.projectId)
        def project = skillsService.getProject(p1.projectId)
        then:
        subjSkills.collect { it.pointIncrementInterval } == [480, 720, 720, 720, 480]
        subjSkills.collect { it.pointIncrement } == [100, 100, 100, 100, 100]
        subjSkills.collect { it.totalPoints } == [100, 100, 100, 100, 100]

        subjects.collect { it.totalPoints } == [subject1Pts, subject2Pts]
        project.totalPoints == subject1Pts + subject2Pts
    }

    def "batch update numMaxOccurrencesIncrementInterval - definition only"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        int subject1Pts = 100 * 5 * p1skills.size()
        int subject2Pts = 22 * p1skillsSubj2.size()
        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                numMaxOccurrencesIncrementInterval: 3,
                skills: p1skills[1..3].skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subjects = skillsService.getSubjects(p1.projectId)
        def project = skillsService.getProject(p1.projectId)
        then:
        subjSkills.collect { it.numMaxOccurrencesIncrementInterval } == [1, 3, 3, 3, 1]
        subjSkills.collect { it.pointIncrement } == [100, 100, 100, 100, 100]
        subjSkills.collect { it.totalPoints } == [500, 500, 500, 500, 500]

        subjects.collect { it.totalPoints } == [subject1Pts, subject2Pts]
        project.totalPoints == subject1Pts + subject2Pts
    }

    def "batch update enabled - definition only"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        // create a disabled group with 5 disabled skills, then enable the group so child skills become eligible to be enabled
        def p1SkillGroup = createSkillsGroup(1, 1, 10)
        skillsService.createSkill(p1SkillGroup)

        def p1skills = createSkills(5, 1, 1, 100)
        p1skills.each {
            it.enabled = false
            skillsService.assignSkillToSkillsGroup(p1SkillGroup.skillId, it)
        }
        p1SkillGroup.enabled = true
        skillsService.updateSkill(p1SkillGroup)

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                enabled: "true",
                skills: p1skills[1..3].skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId, true)

        then:
        subjSkills.findAll { it.type == "Skill" }.collect { it.enabled } == [false, true, true, true, false]
    }

    def "batch update point increment with skills under a skill group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def p1SkillGroup = createSkillsGroup(1, 1, 10)
        skillsService.createSkill(p1SkillGroup)
        p1skills.each {
            skillsService.assignSkillToSkillsGroup(p1SkillGroup.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        int subject1Pts = 100*2 + 555*3
        int subject2Pts = 22 * p1skillsSubj2.size()

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                pointIncrement: 555,
                skills: p1skills[1..3].skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId, true)
        def subjects = skillsService.getSubjects(p1.projectId)
        def project = skillsService.getProject(p1.projectId)

        then:
        subjSkills.findAll { it.type == "Skill" }.collect { it.pointIncrement } == [100, 555, 555, 555, 100]

        subjects.collect { it.totalPoints } == [subject1Pts, subject2Pts]
        project.totalPoints == subject1Pts + subject2Pts
    }




}
