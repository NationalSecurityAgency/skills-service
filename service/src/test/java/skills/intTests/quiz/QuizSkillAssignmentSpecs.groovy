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
package skills.intTests.quiz

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.storage.model.QuizDefParent
import skills.storage.model.QuizToSkillDef
import skills.storage.model.SkillDef
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizToSkillDefRepo

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class QuizSkillAssignmentSpecs extends DefaultIntSpec {

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    QuizDefRepo quizDefRepo

    def "assign quiz to skill"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        when:
        skillsService.createSkill(skillWithQuiz)

        def skill = skillsService.getSkill(skillWithQuiz)
        def skills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill.quizId == quiz.body.quizId
        skill.quizName == quiz.body.name
        skill.quizType == QuizDefParent.QuizType.Quiz.toString()

        skills[0].selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skills[0].quizId == quiz.body.quizId
        skills[0].quizName == quiz.body.name
        skills[0].quizType == QuizDefParent.QuizType.Quiz.toString()
    }

    def "assign survey to skill"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuizSurvey(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        when:
        skillsService.createSkill(skillWithQuiz)

        def skill = skillsService.getSkill(skillWithQuiz)
        def skills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill.quizId == quiz.body.quizId
        skill.quizName == quiz.body.name
        skill.quizType == QuizDefParent.QuizType.Survey.toString()

        skills[0].selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skills[0].quizId == quiz.body.quizId
        skills[0].quizName == quiz.body.name
        skills[0].quizType == QuizDefParent.QuizType.Survey.toString()
    }

    def "update quiz for an existing skill"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))
        def quiz1 = skillsService.createQuizDef(QuizDefFactory.createQuiz(2))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz)

        when:
        def skill_before = skillsService.getSkill(skillWithQuiz)
        def skills_before = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillWithQuiz.quizId = quiz1.body.quizId
        skillsService.createSkill(skillWithQuiz)
        def skill_after = skillsService.getSkill(skillWithQuiz)
        def skills_after = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        skill_before.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill_before.quizId == quiz.body.quizId
        skill_before.quizName == quiz.body.name

        skills_before[0].selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skills_before[0].quizId == quiz.body.quizId
        skills_before[0].quizName == quiz.body.name

        skill_after.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill_after.quizId == quiz1.body.quizId
        skill_after.quizName == quiz1.body.name

        skills_after[0].selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skills_after[0].quizId == quiz1.body.quizId
        skills_after[0].quizName == quiz1.body.name
    }

    def "skill with a quiz must only have 1 occurrence"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 2, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        when:
        skillsService.createSkill(skillWithQuiz)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("When quizId is provided numPerformToCompletion must be equal 1")
        e.message.contains("skillId:${skillWithQuiz.skillId}")
    }

    def "quiz assignment is removed"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz)

        def skill = skillsService.getSkill(skillWithQuiz)
        List<QuizToSkillDef> quizToSkillDefs = quizToSkillDefRepo.findAll()
        when:
        skillWithQuiz.quizId = null
        skillWithQuiz.selfReportingType = null

        skillsService.createSkill(skillWithQuiz)
        def skill1 = skillsService.getSkill(skillWithQuiz)
        List<QuizToSkillDef> quizToSkillDefs_t1 = quizToSkillDefRepo.findAll()
        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill.quizId == quiz.body.quizId
        skill.quizName == quiz.body.name
        quizToSkillDefs
        quizToSkillDefs.collect { skillDefRepo.findById(it.skillRefId).get().skillId } == [skill.skillId]

        !skill1.selfReportingType
        !skill1.quizId
        !skill1.quizName
        !quizToSkillDefs_t1
    }

    def "quiz assignment is changed to Approval self-report type"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz)

        def skill = skillsService.getSkill(skillWithQuiz)
        List<QuizToSkillDef> quizToSkillDefs = quizToSkillDefRepo.findAll()
        when:
        skillWithQuiz.quizId = null
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Approval
        skillsService.createSkill(skillWithQuiz)
        def skill1 = skillsService.getSkill(skillWithQuiz)
        List<QuizToSkillDef> quizToSkillDefs_t1 = quizToSkillDefRepo.findAll()

        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill.quizId == quiz.body.quizId
        skill.quizName == quiz.body.name
        quizToSkillDefs
        quizToSkillDefs.collect { skillDefRepo.findById(it.skillRefId).get().skillId } == [skill.skillId]

        skill1.selfReportingType  == SkillDef.SelfReportingType.Approval.toString()
        !skill1.quizId
        !skill1.quizName
        !quizToSkillDefs_t1
    }

    def "quiz assignment is changed to Honor self-report type"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz)

        def skill = skillsService.getSkill(skillWithQuiz)
        List<QuizToSkillDef> quizToSkillDefs = quizToSkillDefRepo.findAll()
        when:
        skillWithQuiz.quizId = null
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.HonorSystem

        skillsService.createSkill(skillWithQuiz)
        def skill1 = skillsService.getSkill(skillWithQuiz)
        List<QuizToSkillDef> quizToSkillDefs_t1 = quizToSkillDefRepo.findAll()
        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill.quizId == quiz.body.quizId
        skill.quizName == quiz.body.name
        quizToSkillDefs
        quizToSkillDefs.collect { skillDefRepo.findById(it.skillRefId).get().skillId } == [skill.skillId]

        skill1.selfReportingType  == SkillDef.SelfReportingType.HonorSystem.toString()
        !skill1.quizId
        !skill1.quizName
        !quizToSkillDefs_t1
    }

    def "quiz assignment is changed to survey"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))
        def survey = skillsService.createQuizDef(QuizDefFactory.createQuizSurvey(2))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz)

        def skill = skillsService.getSkill(skillWithQuiz)
        List<QuizToSkillDef> quizToSkillDefs = quizToSkillDefRepo.findAll()
        when:
        skillWithQuiz.quizId = survey.body.quizId
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz

        skillsService.createSkill(skillWithQuiz)
        def skill1 = skillsService.getSkill(skillWithQuiz)
        List<QuizToSkillDef> quizToSkillDefs_t1 = quizToSkillDefRepo.findAll()
        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill.quizId == quiz.body.quizId
        skill.quizName == quiz.body.name
        quizToSkillDefs
        quizToSkillDefs.collect { skillDefRepo.findById(it.skillRefId).get().skillId } == [skill.skillId]
        quizToSkillDefs.collect { quizDefRepo.findById(it.quizRefId).get().quizId } == [quiz.body.quizId]

        skill1.selfReportingType  == SkillDef.SelfReportingType.Quiz.toString()
        skill1.quizId == survey.body.quizId
        skill1.quizName == survey.body.name
        quizToSkillDefs_t1.collect { skillDefRepo.findById(it.skillRefId).get().skillId } == [skill.skillId]
        quizToSkillDefs_t1.collect { quizDefRepo.findById(it.quizRefId).get().quizId } == [survey.body.quizId]
    }

    def "when associating to a skill if quizId is set then selfReportType must equal 'Quiz'"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.quizId = quiz.body.quizId

        when:
        skillWithQuiz.selfReportingType = null
        skillsService.createSkill(skillWithQuiz)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("When quizId is provided then selfReportingType must equal 'Quiz'")
    }

    def "when associating to a skill if selfReportType='Quiz' then quizId must be set"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()

        when:
        skillWithQuiz.quizId = null
        skillsService.createSkill(skillWithQuiz)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("When selfReportingType=Quiz then quizId param must not be blank")
    }

    def "count num of skills quiz is assigned to"() {
        Closure associateQuizToSkill = { Integer projNum, Integer skillNum, String quizId ->
            def skillWithQuiz = createSkill(projNum, 1, skillNum, 1, 1, 480, 200)
            skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
            skillWithQuiz.quizId = quizId
            return skillWithQuiz
        }

        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))
        def survey2 = skillsService.createQuizDef(QuizDefFactory.createQuizSurvey(2))
        def quiz3 = skillsService.createQuizDef(QuizDefFactory.createQuiz(3))

        def proj = createProject(1)
        def subj = createSubject(1, 1)

        def proj2 = createProject(2)
        def subj2 = createSubject(2, 1)

        when:
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [
                associateQuizToSkill.call(1, 1, quiz.body.quizId),
                associateQuizToSkill.call(1, 2, survey2.body.quizId),
                associateQuizToSkill.call(1, 3, quiz3.body.quizId),
        ])
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, [
                associateQuizToSkill.call(2, 1, survey2.body.quizId),
                associateQuizToSkill.call(2, 2, quiz3.body.quizId),
                associateQuizToSkill.call(2, 3, survey2.body.quizId),
        ])

        then:
        skillsService.countSkillsForQuiz(quiz.body.quizId) == 1
        skillsService.countSkillsForQuiz(survey2.body.quizId) == 3
        skillsService.countSkillsForQuiz(quiz3.body.quizId) == 2
    }

    def "skill with quiz assignment is removed"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId
        skillsService.createSkill(skillWithQuiz)

        def skill2WithQuiz = createSkill(1, 1, 2, 1, 1, 480, 200)
        skill2WithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skill2WithQuiz.quizId = quiz.body.quizId
        skillsService.createSkill(skill2WithQuiz)
        when:
        List<SkillDef> quizToSkillDefsSkills = quizToSkillDefRepo.findAll().collect { skillDefRepo.findById(it.skillRefId).get() }
        def skills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillsService.deleteSkill(skillWithQuiz)
        def skills_t1 = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        List<QuizToSkillDef> quizToSkillDefs_t1 = quizToSkillDefRepo.findAll()
        then:
        skills.quizId == [quiz.body.quizId, quiz.body.quizId]
        skills_t1.quizId == [quiz.body.quizId]

        quizToSkillDefsSkills.skillId == [skillWithQuiz.skillId, skill2WithQuiz.skillId].sort()

        quizToSkillDefs_t1.collect { skillDefRepo.findById(it.skillRefId).get().skillId } == [skill2WithQuiz.skillId]
        quizToSkillDefs_t1.collect { quizDefRepo.findById(it.quizRefId).get().quizId } == [quiz.body.quizId]
    }
}



