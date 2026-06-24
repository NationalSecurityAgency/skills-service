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
import skills.intTests.utils.QuizDefFactory
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.SkillDef

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
        subjSkills.collect { it.numPerformToCompletion } == p1skills.collect { it.numPerformToCompletion }
        subjSkills.collect { it.pointIncrementInterval } == p1skills.collect { it.pointIncrementInterval }


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
        subjSkills.collect { it.pointIncrementInterval } == p1skills.collect { it.pointIncrementInterval }

        subjects.collect { it.totalPoints } == [subject1Pts, subject2Pts]
        project.totalPoints == subject1Pts + subject2Pts
    }

    def "updating numPerformToCompletion is not applicable to Quiz and Video self-reportable skills"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100, 5)
        p1skills[0].selfReportingType = SkillDef.SelfReportingType.HonorSystem.toString()
        p1skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p1skills[1].quizId = quiz.quizId
        p1skills[1].numPerformToCompletion = 1
        // p1skills[2].selfReportingType = video has to be updated after initial skill is created
        p1skills[3].selfReportingType = SkillDef.SelfReportingType.Approval.toString()
        p1skills[3].selfReportingType = null // just to make it explicit
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        skillsService.saveSkillVideoAttributes(p1.projectId, p1skills[2].skillId, [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
        ])
        p1skills[2].selfReportingType = SkillDef.SelfReportingType.Video.toString()
        p1skills[2].numPerformToCompletion = 1
        skillsService.updateSkill(p1skills[2])

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        when:
        def subjSkills_before = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)

        skillsService.batchUpdateSkills(p1.projectId, [
                numPerformToCompletion: 5,
                skills: p1skills.skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        then:
        subjSkills_before.collect { it.pointIncrement } == [100, 100, 100, 100, 100]
        subjSkills_before.collect { it.totalPoints } == [500, 100, 100, 500, 500]
        subjSkills_before.collect { it.pointIncrementInterval } == p1skills.collect { it.pointIncrementInterval }

        subjSkills.collect { it.pointIncrement } == [100, 100, 100, 100, 100]
        subjSkills.collect { it.totalPoints } == [500, 100, 100, 500, 500]
        subjSkills.collect { it.pointIncrementInterval } == p1skills.collect { it.pointIncrementInterval }

    }

    def "batch update pointIncrementInterval - definition only"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100, 2)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        int subject1Pts = 200 * p1skills.size()
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
        subjSkills.collect { it.totalPoints } == [200, 200, 200, 200, 200]
        subjSkills.collect { it.numPerformToCompletion } == p1skills.collect { it.numPerformToCompletion }


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
        subjSkills.collect { it.pointIncrementInterval } == p1skills.collect { it.pointIncrementInterval }

        subjects.collect { it.totalPoints } == [subject1Pts, subject2Pts]
        project.totalPoints == subject1Pts + subject2Pts
    }

    def "batch update pointIncrementInterval and numMaxOccurrencesIncrementInterval at the same time - definition only"() {
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
                pointIncrementInterval: 720,
                numMaxOccurrencesIncrementInterval: 3,
                skills: p1skills[1..3].skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subjects = skillsService.getSubjects(p1.projectId)
        def project = skillsService.getProject(p1.projectId)
        then:
        subjSkills.collect { it.pointIncrementInterval } == [480, 720, 720, 720, 480]
        subjSkills.collect { it.numMaxOccurrencesIncrementInterval } == [1, 3, 3, 3, 1]
        subjSkills.collect { it.pointIncrement } == [100, 100, 100, 100, 100]
        subjSkills.collect { it.totalPoints } == [500, 500, 500, 500, 500]
        subjSkills.collect { it.numPerformToCompletion } == p1skills.collect { it.numPerformToCompletion }

        subjects.collect { it.totalPoints } == [subject1Pts, subject2Pts]
        project.totalPoints == subject1Pts + subject2Pts
    }

    def "updating pointIncrementInterval and numMaxOccurrencesIncrementInterval only applies to skills whose occurrences > numMaxOccurrencesIncrementInterval"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100, 5)
        p1skills[1].numPerformToCompletion = 1
        p1skills[2].numPerformToCompletion = 2
        p1skills[3].numPerformToCompletion = 3
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                pointIncrementInterval: 720,
                numMaxOccurrencesIncrementInterval: 3,
                skills: p1skills.skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        then:
        subjSkills.collect { it.pointIncrementInterval } == [720, 480, 480, 720, 720]
        subjSkills.collect { it.numMaxOccurrencesIncrementInterval } == [3, 1, 1, 3, 3]
        subjSkills.collect { it.pointIncrement } == [100, 100, 100, 100, 100]
        subjSkills.collect { it.totalPoints } == [500, 100, 200, 300, 500]
        subjSkills.collect { it.numPerformToCompletion } == p1skills.collect { it.numPerformToCompletion }
    }

    def "updating pointIncrementInterval only applies to skills whose occurrences > 1"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(5, 1, 1, 100, 5)
        p1skills[0].numPerformToCompletion = 5
        p1skills[1].numPerformToCompletion = 1
        p1skills[2].numPerformToCompletion = 1
        p1skills[3].numPerformToCompletion = 2
        p1skills[4].numPerformToCompletion = 3
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                pointIncrementInterval: 720,
                skills: p1skills.skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subjects = skillsService.getSubjects(p1.projectId)
        def project = skillsService.getProject(p1.projectId)
        then:
        subjSkills.collect { it.pointIncrementInterval } == [720, 480, 480, 720, 720]
        subjSkills.collect { it.numMaxOccurrencesIncrementInterval } == [1, 1, 1, 1, 1]
        subjSkills.collect { it.pointIncrement } == [100, 100, 100, 100, 100]
        subjSkills.collect { it.totalPoints } == [500, 100, 100, 200, 300]
        subjSkills.collect { it.numPerformToCompletion } == p1skills.collect { it.numPerformToCompletion }
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


    def "batch update all attributes at the same time"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        // create 5 skills: 100 pts, 5 max occurrences
        def p1skills = createSkills(5, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        // Calculate expected points for subject 1:
        // 2 skills remain (100 * 5) + 3 skills updated (555 * 10)
        int subject1Pts = (100 * 5 * 2) + (555 * 10 * 3)
        int subject2Pts = 22 * p1skillsSubj2.size()

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                pointIncrement: 555,
                numPerformToCompletion: 10,
                pointIncrementInterval: 720,
                numMaxOccurrencesIncrementInterval: 3,
                enabled: "true",
                skills: p1skills[1..3].skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId, true)
        def subjects = skillsService.getSubjects(p1.projectId)
        def project = skillsService.getProject(p1.projectId)

        then:
        // Verify specific attribute updates for the range [1..3]
        subjSkills.collect { it.pointIncrement } == [100, 555, 555, 555, 100]
        subjSkills.collect { it.totalPoints } == [500, 5550, 5550, 5550, 500]
        subjSkills.collect { it.pointIncrementInterval } == [480, 720, 720, 720, 480]
        subjSkills.collect { it.numMaxOccurrencesIncrementInterval } == [1, 3, 3, 3, 1]

        // Total points verification
        subjects.collect { it.totalPoints } == [subject1Pts, subject2Pts]
        project.totalPoints == subject1Pts + subject2Pts
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

    def "batch update all attributes with skills under a skill group"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        // Create 5 skills: 100 pts, 5 max occurrences
        def p1skills = createSkills(5, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def p1SkillGroup = createSkillsGroup(1, 1, 10)
        skillsService.createSkill(p1SkillGroup)
        p1skills.each {
            skillsService.assignSkillToSkillsGroup(p1SkillGroup.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        // Calculate expected points for subject 1:
        // 2 skills remain (100 * 5) + 3 skills updated (555 * 10)
        int subject1Pts = (100 * 5 * 2) + (555 * 10 * 3)
        int subject2Pts = 22 * p1skillsSubj2.size()

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                pointIncrement: 555,
                numPerformToCompletion: 10,
                pointIncrementInterval: 720,
                numMaxOccurrencesIncrementInterval: 3,
                enabled: "true",
                skills: p1skills[1..3].skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId, true)
        def subjects = skillsService.getSubjects(p1.projectId)
        def project = skillsService.getProject(p1.projectId)

        then:
        def filteredSkills = subjSkills.findAll { it.type == "Skill" }
        filteredSkills.collect { it.pointIncrement } == [100, 555, 555, 555, 100]
        filteredSkills.collect { it.totalPoints } == [500, 5550, 5550, 5550, 500]
        filteredSkills.collect { it.pointIncrementInterval } == [480, 720, 720, 720, 480]
        filteredSkills.collect { it.numMaxOccurrencesIncrementInterval } == [1, 3, 3, 3, 1]

        subjects.collect { it.totalPoints } == [subject1Pts, subject2Pts]
        project.totalPoints == subject1Pts + subject2Pts
    }

    def "batch update exported single skill should propagate updates to imported skills"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1skills = createSkills(2, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        // Export a skill to the catalog
        def sourceSkill = p1skills[0]
        skillsService.exportSkillToCatalog(p1.projectId, sourceSkill.skillId)

        // Setup another project to import the skill
        def p3 = createProject(3)
        def p3Subj1 = createSubject(3, 1)
        skillsService.createProjectAndSubjectAndSkills(p3, p3Subj1, [])

        // Import the skill and finalize it
        skillsService.importSkillFromCatalog(p3.projectId, p3Subj1.subjectId, p1.projectId, sourceSkill.skillId)
        skillsService.finalizeSkillsImportFromCatalog(p3.projectId)

        when:
        // Batch update the exported skill in the source project
        skillsService.batchUpdateSkills(p1.projectId, [
                numPerformToCompletion: 12,
                skills: [sourceSkill.skillId]
        ])

        // Verify source skill in p1
        def p1subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def p1UpdatedSkill = p1subjSkills.find { it.skillId == sourceSkill.skillId }

        // must wait for async propagation
        skillsService.waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        // Verify imported skill in p2
        def p2subjSkills = skillsService.getSkillsForSubject(p3.projectId, p3Subj1.subjectId)
        def p2ImportedSkill = p2subjSkills.find { it.skillId == sourceSkill.skillId }

        then:
        // Source updated
        p1UpdatedSkill.numPerformToCompletion == 12

        // Imported skill should have propagated the updates from the catalog source
        p2ImportedSkill.numPerformToCompletion == 12
    }

    def "batch update exported skills should propagate updates to imported skills"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        // Create a larger pool of skills
        def p1skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        // Export a subset of skills to the catalog (e.g., the first 3)
        def exportedSkills = p1skills.take(3)
        exportedSkills.each { skill ->
            skillsService.exportSkillToCatalog(p1.projectId, skill.skillId)
        }

        // Setup another project to import the exported skills
        def p3 = createProject(3)
        def p3Subj1 = createSubject(3, 1)
        skillsService.createProjectAndSubjectAndSkills(p3, p3Subj1, [])

        exportedSkills.each { skill ->
            skillsService.importSkillFromCatalog(p3.projectId, p3Subj1.subjectId, p1.projectId, skill.skillId)
        }
        skillsService.finalizeSkillsImportFromCatalog(p3.projectId)

        // Define which specific skills from the pool will be updated (e.g., the first 2)
        def skillsToUpdate = exportedSkills.take(2)
        def skillIdsToUpdate = skillsToUpdate.collect { it.skillId }
        def newValue = 12

        when:
        // Batch update only the selected skills in the source project
        skillsService.batchUpdateSkills(p1.projectId, [
                numPerformToCompletion: newValue,
                skills: skillIdsToUpdate
        ])

        // must wait for async propagation
        skillsService.waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        // Retrieve final states
        def p1subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def p3subjSkills = skillsService.getSkillsForSubject(p3.projectId, p3Subj1.subjectId)

        then:
        // 1. Verify source project: updated skills changed, others remained 100
        p1subjSkills.findAll { it.skillId in skillIdsToUpdate }.each { it.numPerformToCompletion == newValue }
        p1subjSkills.findAll { !(it.skillId in skillIdsToUpdate) }.each { it.numPerformToCompletion == 100 }

        // 2. Verify imported project: only the updated exported skills propagated the change
        p3subjSkills.findAll { it.skillId in skillIdsToUpdate }.each { it.numPerformToCompletion == newValue }

        // The 3rd exported skill was imported but NOT included in batch update; it should still be 100
        def nonUpdatedImportedSkill = p3subjSkills.find { it.skillId == exportedSkills[2].skillId }
        nonUpdatedImportedSkill.numPerformToCompletion == 1
    }

    def "batch update skills should propagate updates only to targeted reused skills in different subjects"() {
        given:
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        // Create 5 skills in the first subject
        def p1skills = createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1skills)

        // Create a second subject in the same project p1
        def p1subj2 = createSubject(1, 2)
        skillsService.createSubject(p1subj2)

        // Reuse all skills from subj1 into subj2 within project p1
        skillsService.reuseSkills(p1.projectId, p1skills.collect { it.skillId }, p1subj2.subjectId)

        // Define a subset to update (skills 1 and 3)
        def skillsToUpdate = [p1skills[1].skillId, p1skills[3].skillId]
        def skillsToKeep = [p1skills[0].skillId, p1skills[2].skillId, p1skills[4].skillId]

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                numPerformToCompletion: 12,
                skills: skillsToUpdate
        ])

        // must wait for async propagation
        skillsService.waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def subj1Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId)
        def subj2Skills = skillsService.getSkillsForSubject(p1.projectId, p1subj2.subjectId)

        then:
        subj1Skills.numPerformToCompletion == [1, 12, 1, 12, 1]
        subj2Skills.numPerformToCompletion == [1, 12, 1, 12, 1]
    }

    def "batch update user action must be saved for each skill"() {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        // Create 5 skills: 100 pts, 5 max occurrences
        def p1skills = createSkills(5, 1, 1, 100, 5)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, null)

        def p1SkillGroup = createSkillsGroup(1, 1, 10)
        skillsService.createSkill(p1SkillGroup)
        p1skills.each {
            skillsService.assignSkillToSkillsGroup(p1SkillGroup.skillId, it)
        }

        def p1subj2 = createSubject(1, 2)
        def p1skillsSubj2 = createSkills(4, 1, 2, 22)
        skillsService.createProjectAndSubjectAndSkills(null, p1subj2, p1skillsSubj2)

        when:
        skillsService.batchUpdateSkills(p1.projectId, [
                pointIncrement: 555,
                numPerformToCompletion: 10,
                pointIncrementInterval: 720,
                numMaxOccurrencesIncrementInterval: 3,
                selfReportingType: SkillDef.SelfReportingType.HonorSystem.toString(),
                enabled: "true",
                skills: p1skills[1..3].skillId
        ])

        def subjSkills = skillsService.getSkillsForSubject(p1.projectId, p1subj1.subjectId, true)

        def allActions = skillsService.getUserActionsForProject(p1.projectId, 10, 1, "created", false, DashboardItem.SkillsBatch)
        List batchSkillUpdateUserActions = skillsService.getUserActionsForProject(p1.projectId, 10, 1, "created", false, DashboardItem.SkillsBatch).data.sort( { it.itemId })

        then:
        def filteredSkills = subjSkills.findAll { it.type == "Skill" }
        filteredSkills.collect { it.pointIncrement } == [100, 555, 555, 555, 100]
        filteredSkills.collect { it.totalPoints } == [500, 5550, 5550, 5550, 500]
        filteredSkills.collect { it.pointIncrementInterval } == [480, 720, 720, 720, 480]
        filteredSkills.collect { it.numMaxOccurrencesIncrementInterval } == [1, 3, 3, 3, 1]

        batchSkillUpdateUserActions.size() == 3
        batchSkillUpdateUserActions.action == [DashboardAction.Edit.name(), DashboardAction.Edit.name(), DashboardAction.Edit.name()]
        batchSkillUpdateUserActions.itemId == p1skills[1..3].skillId.sort()

        batchSkillUpdateUserActions.each {
            def attributes = skillsService.getProjectUserActionAttributes(p1.projectId, it.id)
            assert attributes.skills?.sort() == p1skills[1..3].skillId.sort()
            assert attributes.enabled == Boolean.TRUE.toString().toLowerCase()
            assert attributes.pointIncrement == 555
            assert attributes.selfReportingType == SkillDef.SelfReportingType.HonorSystem.toString()
            assert attributes.numPerformToCompletion == 10
            assert attributes.numMaxOccurrencesIncrementInterval == 3
        }

    }

}
