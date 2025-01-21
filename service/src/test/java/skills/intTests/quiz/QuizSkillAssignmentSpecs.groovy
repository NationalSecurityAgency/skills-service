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
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizDefService
import skills.storage.model.QuizDefParent
import skills.storage.model.QuizToSkillDef
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizToSkillDefRepo
import spock.lang.IgnoreIf

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class QuizSkillAssignmentSpecs extends DefaultIntSpec {

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizDefService quizDefService

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


    def "get skills for quiz"() {
        Closure associateQuizToSkill = { Integer projNum, Integer subjNum, Integer skillNum, String quizId ->
            def skillWithQuiz = createSkill(projNum, subjNum, skillNum, 1, 1, 480, 200)
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
        def proj2Subj1 = createSubject(2, 1)
        def proj2Subj2 = createSubject(2, 2)

        skillsService.createProjectAndSubjectAndSkills(proj, subj, [
                associateQuizToSkill.call(1, 1, 1, quiz.body.quizId),
                associateQuizToSkill.call(1, 1, 2, survey2.body.quizId),
                associateQuizToSkill.call(1, 1, 3, quiz3.body.quizId),
        ])
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2Subj1, [])
        skillsService.createSubject(proj2Subj2)
        skillsService.createSkills([
                associateQuizToSkill.call(2, 1, 1, survey2.body.quizId),
                associateQuizToSkill.call(2, 1, 2, quiz3.body.quizId),
                associateQuizToSkill.call(2, 2, 3, survey2.body.quizId),
                associateQuizToSkill.call(2, 1, 4, survey2.body.quizId),
        ])

        List<String> otherUsers = getRandomUsers(3)
        SkillsService onlyProj1Admin = createService(otherUsers[0])
        skillsService.addUserRole(onlyProj1Admin.userName, proj.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())

        SkillsService onlyProj2Admin = createService(otherUsers[1])
        skillsService.addUserRole(onlyProj2Admin.userName, proj2.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())

        SkillsService onlyQuizAdmin = createService(otherUsers[2])
        skillsService.addQuizUserRole(quiz.body.quizId, onlyQuizAdmin.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.addQuizUserRole(survey2.body.quizId, onlyQuizAdmin.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.addQuizUserRole(quiz3.body.quizId, onlyQuizAdmin.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        when:
        def quizSkills = skillsService.getSkillsForQuiz(quiz.body.quizId)
        def survey2Skills = skillsService.getSkillsForQuiz(survey2.body.quizId)
        def quiz3Skills = skillsService.getSkillsForQuiz(quiz3.body.quizId)

        def quizSkillsOnlyProject1Admin = onlyProj1Admin.getSkillsForQuiz(quiz.body.quizId)
        def survey2SkillsOnlyProject1Admin = onlyProj1Admin.getSkillsForQuiz(survey2.body.quizId)
        def quiz3SkillsOnlyProject1Admin = onlyProj1Admin.getSkillsForQuiz(quiz3.body.quizId)

        assert validateForbidden {onlyProj2Admin.getSkillsForQuiz(quiz.body.quizId)}
        def survey2SkillsOnlyProject2Admin = onlyProj2Admin.getSkillsForQuiz(survey2.body.quizId)
        def quiz3SkillsOnlyProject2Admin = onlyProj2Admin.getSkillsForQuiz(quiz3.body.quizId)

        def quizSkillsOnlyQuizAdmin = onlyQuizAdmin.getSkillsForQuiz(quiz.body.quizId)
        def survey2SkillsOnlyQuizAdmin = onlyQuizAdmin.getSkillsForQuiz(survey2.body.quizId)
        def quiz3SkillsOnlyQuizAdmin = onlyQuizAdmin.getSkillsForQuiz(quiz3.body.quizId)

        then:
        quizSkills.projectId == [proj.projectId]
        quizSkills.subjectId == [subj.subjectId]
        quizSkills.subjectName == [subj.name]
        quizSkills.skillId == ['skill1']
        quizSkills.skillName == ["Test Skill 1"]
        quizSkills.subjectPoints == [600]
        quizSkills.projectPoints == [600]
        quizSkills.canUserAccess == [true]

        survey2Skills.projectId == [proj.projectId, proj2.projectId, proj2.projectId, proj2.projectId]
        survey2Skills.subjectId == [subj.subjectId, proj2Subj1.subjectId, proj2Subj2.subjectId, proj2Subj1.subjectId]
        survey2Skills.subjectName == [subj.name, proj2Subj1.name, proj2Subj2.name, proj2Subj1.name]
        survey2Skills.skillId == ["skill2", "skill1", "skill3subj2", "skill4"]
        survey2Skills.skillName == ["Test Skill 2", "Test Skill 1", "Test Skill 3 Subject2", "Test Skill 4"]
        survey2Skills.subjectPoints == [600, 600, 200, 600]
        survey2Skills.projectPoints == [600, 800, 800, 800]
        survey2Skills.canUserAccess == [true, true, true, true]

        quiz3Skills.projectId == [proj.projectId, proj2.projectId]
        quiz3Skills.subjectId == [subj.subjectId, proj2Subj1.subjectId]
        quiz3Skills.subjectName == [subj.name, proj2Subj1.name]
        quiz3Skills.skillId == ["skill3", "skill2"]
        quiz3Skills.skillName == ["Test Skill 3", "Test Skill 2"]
        quiz3Skills.subjectPoints == [600, 600]
        quiz3Skills.projectPoints == [600, 800]
        quiz3Skills.canUserAccess == [true, true]

        // only proj 1 admin
        quizSkillsOnlyProject1Admin.projectId == [proj.projectId]
        quizSkillsOnlyProject1Admin.subjectId == [subj.subjectId]
        quizSkillsOnlyProject1Admin.subjectName == [subj.name]
        quizSkillsOnlyProject1Admin.skillId == ['skill1']
        quizSkillsOnlyProject1Admin.skillName == ["Test Skill 1"]
        quizSkillsOnlyProject1Admin.subjectPoints == [600]
        quizSkillsOnlyProject1Admin.projectPoints == [600]
        quizSkillsOnlyProject1Admin.canUserAccess == [true]

        survey2SkillsOnlyProject1Admin.projectId == [proj.projectId, proj2.projectId, proj2.projectId, proj2.projectId]
        survey2SkillsOnlyProject1Admin.subjectId == [subj.subjectId, proj2Subj1.subjectId, proj2Subj2.subjectId, proj2Subj1.subjectId]
        survey2SkillsOnlyProject1Admin.subjectName == [subj.name, proj2Subj1.name, proj2Subj2.name, proj2Subj1.name]
        survey2SkillsOnlyProject1Admin.skillId == ["skill2", "skill1", "skill3subj2", "skill4"]
        survey2SkillsOnlyProject1Admin.skillName == ["Test Skill 2", "Test Skill 1", "Test Skill 3 Subject2", "Test Skill 4"]
        survey2SkillsOnlyProject1Admin.subjectPoints == [600, 600, 200, 600]
        survey2SkillsOnlyProject1Admin.projectPoints == [600, 800, 800, 800]
        survey2SkillsOnlyProject1Admin.canUserAccess == [true, false, false, false]

        quiz3SkillsOnlyProject1Admin.projectId == [proj.projectId, proj2.projectId]
        quiz3SkillsOnlyProject1Admin.subjectId == [subj.subjectId, proj2Subj1.subjectId]
        quiz3SkillsOnlyProject1Admin.subjectName == [subj.name, proj2Subj1.name]
        quiz3SkillsOnlyProject1Admin.skillId == ["skill3", "skill2"]
        quiz3SkillsOnlyProject1Admin.skillName == ["Test Skill 3", "Test Skill 2"]
        quiz3SkillsOnlyProject1Admin.subjectPoints == [600, 600]
        quiz3SkillsOnlyProject1Admin.projectPoints == [600, 800]
        quiz3SkillsOnlyProject1Admin.canUserAccess == [true, false]

        // only proj 2 admin
        survey2SkillsOnlyProject2Admin.projectId == [proj.projectId, proj2.projectId, proj2.projectId, proj2.projectId]
        survey2SkillsOnlyProject2Admin.subjectId == [subj.subjectId, proj2Subj1.subjectId, proj2Subj2.subjectId, proj2Subj1.subjectId]
        survey2SkillsOnlyProject2Admin.subjectName == [subj.name, proj2Subj1.name, proj2Subj2.name, proj2Subj1.name]
        survey2SkillsOnlyProject2Admin.skillId == ["skill2", "skill1", "skill3subj2", "skill4"]
        survey2SkillsOnlyProject2Admin.skillName == ["Test Skill 2", "Test Skill 1", "Test Skill 3 Subject2", "Test Skill 4"]
        survey2SkillsOnlyProject2Admin.subjectPoints == [600, 600, 200, 600]
        survey2SkillsOnlyProject2Admin.projectPoints == [600, 800, 800, 800]
        survey2SkillsOnlyProject2Admin.canUserAccess == [false, true, true, true]

        quiz3SkillsOnlyProject2Admin.projectId == [proj.projectId, proj2.projectId]
        quiz3SkillsOnlyProject2Admin.subjectId == [subj.subjectId, proj2Subj1.subjectId]
        quiz3SkillsOnlyProject2Admin.subjectName == [subj.name, proj2Subj1.name]
        quiz3SkillsOnlyProject2Admin.skillId == ["skill3", "skill2"]
        quiz3SkillsOnlyProject2Admin.skillName == ["Test Skill 3", "Test Skill 2"]
        quiz3SkillsOnlyProject2Admin.subjectPoints == [600, 600]
        quiz3SkillsOnlyProject2Admin.projectPoints == [600, 800]
        quiz3SkillsOnlyProject2Admin.canUserAccess == [false, true]

        // only quiuz admin
        quizSkillsOnlyQuizAdmin.projectId == [proj.projectId]
        quizSkillsOnlyQuizAdmin.subjectId == [subj.subjectId]
        quizSkillsOnlyQuizAdmin.subjectName == [subj.name]
        quizSkillsOnlyQuizAdmin.skillId == ['skill1']
        quizSkillsOnlyQuizAdmin.skillName == ["Test Skill 1"]
        quizSkillsOnlyQuizAdmin.subjectPoints == [600]
        quizSkillsOnlyQuizAdmin.projectPoints == [600]
        quizSkillsOnlyQuizAdmin.canUserAccess == [false]

        survey2SkillsOnlyQuizAdmin.projectId == [proj.projectId, proj2.projectId, proj2.projectId, proj2.projectId]
        survey2SkillsOnlyQuizAdmin.subjectId == [subj.subjectId, proj2Subj1.subjectId, proj2Subj2.subjectId, proj2Subj1.subjectId]
        survey2SkillsOnlyQuizAdmin.subjectName == [subj.name, proj2Subj1.name, proj2Subj2.name, proj2Subj1.name]
        survey2SkillsOnlyQuizAdmin.skillId == ["skill2", "skill1", "skill3subj2", "skill4"]
        survey2SkillsOnlyQuizAdmin.skillName == ["Test Skill 2", "Test Skill 1", "Test Skill 3 Subject2", "Test Skill 4"]
        survey2SkillsOnlyQuizAdmin.subjectPoints == [600, 600, 200, 600]
        survey2SkillsOnlyQuizAdmin.projectPoints == [600, 800, 800, 800]
        survey2SkillsOnlyQuizAdmin.canUserAccess == [false, false, false, false]

        quiz3SkillsOnlyQuizAdmin.projectId == [proj.projectId, proj2.projectId]
        quiz3SkillsOnlyQuizAdmin.subjectId == [subj.subjectId, proj2Subj1.subjectId]
        quiz3SkillsOnlyQuizAdmin.subjectName == [subj.name, proj2Subj1.name]
        quiz3SkillsOnlyQuizAdmin.skillId == ["skill3", "skill2"]
        quiz3SkillsOnlyQuizAdmin.skillName == ["Test Skill 3", "Test Skill 2"]
        quiz3SkillsOnlyQuizAdmin.subjectPoints == [600, 600]
        quiz3SkillsOnlyQuizAdmin.projectPoints == [600, 800]
        quiz3SkillsOnlyQuizAdmin.canUserAccess == [false, false]
    }

    private static boolean validateForbidden(Closure c) {
        try {
            c.call()
            return false
        } catch (SkillsClientException skillsClientException) {
            return skillsClientException.httpStatus == HttpStatus.FORBIDDEN
        }
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

    def "reporting skill events directly are not allowed for a quiz-based skill"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId
        skillsService.createSkill(skillWithQuiz)

        when:
        skillsService.addSkill([projectId: proj.projectId, skillId: skillWithQuiz.skillId])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Cannot report skill events directly to a quiz-based skill")
        skillsClientException.message.contains("errorCode:SkillEventForQuizSkillIsNotAllowed")

    }

    def "can get all skills for a quiz"() {
        def quizDef = QuizDefFactory.createQuiz(1)
        def quiz = skillsService.createQuizDef(quizDef)

        def skills_t0 = skillsService.getSkillsForQuiz(quiz.body.quizId)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz)

        def skills_t1 = skillsService.getSkillsForQuiz(quiz.body.quizId)

        def proj2 = createProject(2)
        def subj2 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, [])

        def skillWithQuiz2 = createSkill(2, 1, 1, 1, 1, 480, 200)
        skillWithQuiz2.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz2.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz2)

        when:
        def skills_t2 = skillsService.getSkillsForQuiz(quiz.body.quizId)
        then:
        skills_t0.size() == 0
        skills_t1.size() == 1

        skills_t2.size() == 2
        skills_t2[0].skillId == 'skill1'
        skills_t2[0].skillName == 'Test Skill 1'
        skills_t2[0].subjectId == 'TestSubject1'
        skills_t2[0].subjectName == 'Test Subject #1'
        skills_t2[0].projectId == 'TestProject1'
        skills_t2[0].canUserAccess == true

        skills_t2[1].skillId == 'skill1'
        skills_t2[1].skillName == 'Test Skill 1'
        skills_t2[1].subjectId == 'TestSubject1'
        skills_t2[1].subjectName == 'Test Subject #1'
        skills_t2[1].projectId == 'TestProject2'
        skills_t2[1].canUserAccess == true
    }

    def "user that is not admin on a project does not get canUserAccess status on skill"() {
        def quizDef = QuizDefFactory.createQuiz(1)
        def quiz = skillsService.createQuizDef(quizDef)

        List<String> userIds = getRandomUsers(3)
        SkillsService user1 = createService(userIds[0])

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])
        skillsService.addProjectAdmin(proj.projectId, user1.userName)

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz)

        def user1Skills_t1 = user1.getSkillsForQuiz(quiz.body.quizId)

        def proj2 = createProject(2)
        def subj2 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, [])

        def skillWithQuiz2 = createSkill(2, 1, 1, 1, 1, 480, 200)
        skillWithQuiz2.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz2.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz2)

        when:
        def user1Skills_t2 = user1.getSkillsForQuiz(quiz.body.quizId)
        then:
        user1Skills_t1.size() == 1

        user1Skills_t2.size() == 2
        user1Skills_t2[0].skillId == 'skill1'
        user1Skills_t2[0].skillName == 'Test Skill 1'
        user1Skills_t2[0].subjectId == 'TestSubject1'
        user1Skills_t2[0].subjectName == 'Test Subject #1'
        user1Skills_t2[0].projectId == 'TestProject1'
        user1Skills_t2[0].canUserAccess == true

        user1Skills_t2[1].skillId == 'skill1'
        user1Skills_t2[1].skillName == 'Test Skill 1'
        user1Skills_t2[1].subjectId == 'TestSubject1'
        user1Skills_t2[1].subjectName == 'Test Subject #1'
        user1Skills_t2[1].projectId == 'TestProject2'
        user1Skills_t2[1].canUserAccess == false
    }

    def "user that is approver on a project can access skill"() {
        def quizDef = QuizDefFactory.createQuiz(1)
        def quiz = skillsService.createQuizDef(quizDef)

        List<String> userIds = getRandomUsers(3)
        SkillsService user1 = createService(userIds[0])

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])
        skillsService.addUserRole(user1.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.name())

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz)

        def user1Skills_t1 = user1.getSkillsForQuiz(quiz.body.quizId)

        def proj2 = createProject(2)
        def subj2 = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, subj2, [])

        def skillWithQuiz2 = createSkill(2, 1, 1, 1, 1, 480, 200)
        skillWithQuiz2.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz2.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz2)

        when:
        def user1Skills_t2 = user1.getSkillsForQuiz(quiz.body.quizId)
        then:
        user1Skills_t1.size() == 1

        user1Skills_t2.size() == 2
        user1Skills_t2[0].skillId == 'skill1'
        user1Skills_t2[0].skillName == 'Test Skill 1'
        user1Skills_t2[0].subjectId == 'TestSubject1'
        user1Skills_t2[0].subjectName == 'Test Subject #1'
        user1Skills_t2[0].projectId == 'TestProject1'
        user1Skills_t2[0].canUserAccess == true

        user1Skills_t2[1].skillId == 'skill1'
        user1Skills_t2[1].skillName == 'Test Skill 1'
        user1Skills_t2[1].subjectId == 'TestSubject1'
        user1Skills_t2[1].subjectName == 'Test Subject #1'
        user1Skills_t2[1].projectId == 'TestProject2'
        user1Skills_t2[1].canUserAccess == false
    }
}



