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

import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class QuizSkillsAndCatalogSpecs extends DefaultIntSpec {

    def "catalog returns quiz-skill info"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkills(skills)

        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [])

        when:
        def res = skillsService.getCatalogSkills(proj2.projectId, 10, 1, "name")
        then:
        res.data.selfReportingType == [QuizDefParent.QuizType.Quiz.toString(), null, null]
    }

    def "import quiz-skill from catalog"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skillsService.createSkills(skills)

        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [])

        String userId = getRandomUsers(1).first()
        when:
        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, proj2_subj.subjectId, [
                [projectId: proj.projectId, skillId: skills[0].skillId],
                [projectId: proj.projectId, skillId: skills[1].skillId],
                [projectId: proj.projectId, skillId: skills[2].skillId],
        ])
        def skillsRes = skillsService.getSkillsForSubject(proj2.projectId, proj2_subj.subjectId, true)
        def skill1Res = skillsService.getSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: skills[0].skillId])
        def apiSkills = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)
        then:
        skillsRes.selfReportingType == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null]
        skillsRes.quizId == [quiz.quizId, null, null]
        skillsRes.quizName == [quiz.name, null, null]
        skillsRes.quizType == [QuizDefParent.QuizType.Quiz.toString(), null, null]
        skillsRes.enabled == [false, false, false]

        skill1Res.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill1Res.quizId == quiz.quizId
        skill1Res.quizName == quiz.name
        skill1Res.quizType == QuizDefParent.QuizType.Quiz.toString()
        skill1Res.enabled == false

        !apiSkills.skills
    }

    def "import and finalize quiz-skill from catalog"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def survey = QuizDefFactory.createQuizSurvey(2)
        skillsService.createQuizDef(survey)
        def surveyQuestions = [QuizDefFactory.createSingleChoiceSurveyQuestion(2, 1, 2)]
        skillsService.createQuizQuestionDefs(surveyQuestions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(4, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[3].quizId = survey.quizId
        skillsService.createSkills(skills)

        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [])

        String userId = getRandomUsers(1).first()
        when:
        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, proj2_subj.subjectId, [
                [projectId: proj.projectId, skillId: skills[0].skillId],
                [projectId: proj.projectId, skillId: skills[1].skillId],
                [projectId: proj.projectId, skillId: skills[2].skillId],
                [projectId: proj.projectId, skillId: skills[3].skillId],
        ])
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId)
        def skillsRes = skillsService.getSkillsForSubject(proj2.projectId, proj2_subj.subjectId, true)
        def skill1Res = skillsService.getSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: skills[0].skillId])
        def skill4Res = skillsService.getSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: skills[3].skillId])
        def apiSkills = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)
        def apiSkill1 = skillsService.getSingleSkillSummary(userId, proj2.projectId, skills[0].skillId)
        def apiSkill1WithSubj = skillsService.getSingleSkillSummaryWithSubject(userId, proj2.projectId, proj2_subj.subjectId, skills[0].skillId)
        def apiSkill4 = skillsService.getSingleSkillSummary(userId, proj2.projectId, skills[3].skillId)

        then:
        skillsRes.selfReportingType == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, SkillDef.SelfReportingType.Quiz.toString()]
        skillsRes.quizId == [quiz.quizId, null, null, survey.quizId]
        skillsRes.quizName == [quiz.name, null, null, survey.name]
        skillsRes.quizType == [QuizDefParent.QuizType.Quiz.toString(), null, null, QuizDefParent.QuizType.Survey.toString()]
        skillsRes.enabled == [true, true, true, true]

        skill1Res.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill1Res.quizId == quiz.quizId
        skill1Res.quizName == quiz.name
        skill1Res.quizType == QuizDefParent.QuizType.Quiz.toString()
        skill1Res.enabled == true

        skill4Res.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill4Res.quizId == survey.quizId
        skill4Res.quizName == survey.name
        skill4Res.quizType == QuizDefParent.QuizType.Survey.toString()
        skill4Res.enabled == true

        apiSkills.skills.selfReporting?.type == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        apiSkills.skills.selfReporting?.quizId == [quiz.quizId, null, null, survey.quizId]
        apiSkills.skills.selfReporting?.quizName == [quiz.name, null, null, survey.name]
        apiSkills.skills.selfReporting?.numQuizQuestions == [5, 0, null, 1]

        apiSkill1.selfReporting?.type == SkillDef.SelfReportingType.Quiz.toString()
        apiSkill1.selfReporting?.quizId == quiz.quizId
        apiSkill1.selfReporting?.quizName == quiz.name
        apiSkill1.selfReporting?.numQuizQuestions == 5

        apiSkill4.selfReporting?.type == QuizDefParent.QuizType.Survey.toString()
        apiSkill4.selfReporting?.quizId == survey.quizId
        apiSkill4.selfReporting?.quizName == survey.name
        apiSkill4.selfReporting?.numQuizQuestions == 1

        apiSkill1WithSubj.selfReporting?.type == SkillDef.SelfReportingType.Quiz.toString()
        apiSkill1WithSubj.selfReporting?.quizId == quiz.quizId
        apiSkill1WithSubj.selfReporting?.quizName == quiz.name
        apiSkill1WithSubj.selfReporting?.numQuizQuestions == 5
    }

    def "update original quiz-skill - Honor System to Quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def survey = QuizDefFactory.createQuizSurvey(2)
        skillsService.createQuizDef(survey)
        def surveyQuestions = [QuizDefFactory.createSingleChoiceSurveyQuestion(2, 1, 2)]
        skillsService.createQuizQuestionDefs(surveyQuestions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(4, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[3].quizId = survey.quizId
        skillsService.createSkills(skills)

        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [])

        String userId = getRandomUsers(1).first()

        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, proj2_subj.subjectId, [
                [projectId: proj.projectId, skillId: skills[0].skillId],
                [projectId: proj.projectId, skillId: skills[1].skillId],
                [projectId: proj.projectId, skillId: skills[2].skillId],
                [projectId: proj.projectId, skillId: skills[3].skillId],
        ])
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId)
        def skillsRes_t0 = skillsService.getSkillsForSubject(proj2.projectId, proj2_subj.subjectId, true)
        def skill2Res_t0 = skillsService.getSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: skills[1].skillId])
        def apiSkills_t0 = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)
        def apiSkill2_t0 = skillsService.getSingleSkillSummary(userId, proj2.projectId, skills[1].skillId)

        when:
        skills[1].quizId = quiz.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skillsService.createSkill(skills[1])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def skillsRes_t1 = skillsService.getSkillsForSubject(proj2.projectId, proj2_subj.subjectId, true)
        def skill2Res_t1 = skillsService.getSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: skills[1].skillId])
        def apiSkills_t1 = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)
        def p1_apiSkills_t1 = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)
        def apiSkill2_t1 = skillsService.getSingleSkillSummary(userId, proj2.projectId, skills[1].skillId)
        then:
        skillsRes_t0.selfReportingType == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, SkillDef.SelfReportingType.Quiz.toString()]
        skillsRes_t0.quizId == [quiz.quizId, null, null, survey.quizId]
        skillsRes_t0.quizName == [quiz.name, null, null, survey.name]
        skillsRes_t0.quizType == [QuizDefParent.QuizType.Quiz.toString(), null, null, QuizDefParent.QuizType.Survey.toString()]
        skillsRes_t0.enabled == [true, true, true, true]

        skill2Res_t0.selfReportingType == SkillDef.SelfReportingType.HonorSystem.toString()
        skill2Res_t0.quizId == null
        skill2Res_t0.quizName == null
        skill2Res_t0.quizType == null
        skill2Res_t0.enabled == true

        apiSkills_t0.skills.selfReporting?.type == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        apiSkills_t0.skills.selfReporting?.quizId == [quiz.quizId, null, null, survey.quizId]
        apiSkills_t0.skills.selfReporting?.quizName == [quiz.name, null, null, survey.name]
        apiSkills_t0.skills.selfReporting?.numQuizQuestions == [5, 0, null, 1]

        apiSkill2_t0.selfReporting?.type == SkillDef.SelfReportingType.HonorSystem.toString()
        apiSkill2_t0.selfReporting?.quizId == null
        apiSkill2_t0.selfReporting?.quizName == null
        apiSkill2_t0.selfReporting?.numQuizQuestions == 0

        skillsRes_t1.selfReportingType == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.Quiz.toString(), null, SkillDef.SelfReportingType.Quiz.toString()]
        skillsRes_t1.quizId == [quiz.quizId, quiz.quizId, null, survey.quizId]
        skillsRes_t1.quizName == [quiz.name, quiz.name, null, survey.name]
        skillsRes_t1.quizType == [QuizDefParent.QuizType.Quiz.toString(), QuizDefParent.QuizType.Quiz.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        skillsRes_t1.enabled == [true, true, true, true]

        skill2Res_t1.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill2Res_t1.quizId == quiz.quizId
        skill2Res_t1.quizName == quiz.name
        skill2Res_t1.quizType == QuizDefParent.QuizType.Quiz.toString()
        skill2Res_t1.enabled == true

        p1_apiSkills_t1.skills.selfReporting?.type == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.Quiz.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        p1_apiSkills_t1.skills.selfReporting?.quizId == [quiz.quizId, quiz.quizId, null, survey.quizId]
        p1_apiSkills_t1.skills.selfReporting?.quizName == [quiz.name, quiz.name, null, survey.name]
        p1_apiSkills_t1.skills.selfReporting?.numQuizQuestions == [5, 5, null, 1]

        apiSkills_t1.skills.selfReporting?.type == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.Quiz.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        apiSkills_t1.skills.selfReporting?.quizId == [quiz.quizId, quiz.quizId, null, survey.quizId]
        apiSkills_t1.skills.selfReporting?.quizName == [quiz.name, quiz.name, null, survey.name]
        apiSkills_t1.skills.selfReporting?.numQuizQuestions == [5, 5, null, 1]

        apiSkill2_t1.selfReporting?.type == SkillDef.SelfReportingType.Quiz.toString()
        apiSkill2_t1.selfReporting?.quizId == quiz.quizId
        apiSkill2_t1.selfReporting?.quizName == quiz.name
        apiSkill2_t1.selfReporting?.numQuizQuestions == 5
    }

    def "update original quiz-skill - Quiz to Honor System"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def survey = QuizDefFactory.createQuizSurvey(2)
        skillsService.createQuizDef(survey)
        def surveyQuestions = [QuizDefFactory.createSingleChoiceSurveyQuestion(2, 1, 2)]
        skillsService.createQuizQuestionDefs(surveyQuestions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(4, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[3].quizId = survey.quizId
        skillsService.createSkills(skills)

        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [])

        String userId = getRandomUsers(1).first()

        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, proj2_subj.subjectId, [
                [projectId: proj.projectId, skillId: skills[0].skillId],
                [projectId: proj.projectId, skillId: skills[1].skillId],
                [projectId: proj.projectId, skillId: skills[2].skillId],
                [projectId: proj.projectId, skillId: skills[3].skillId],
        ])
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId)
        def skillsRes_t0 = skillsService.getSkillsForSubject(proj2.projectId, proj2_subj.subjectId, true)
        def skill1Res_t0 = skillsService.getSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: skills[0].skillId])
        def apiSkills_t0 = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)
        def apiSkill1_t0 = skillsService.getSingleSkillSummary(userId, proj2.projectId, skills[0].skillId)

        when:
        skills[0].quizId = null
        skills[0].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skillsService.createSkill(skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def skillsRes_t1 = skillsService.getSkillsForSubject(proj2.projectId, proj2_subj.subjectId, true)
        def skill1Res_t1 = skillsService.getSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: skills[0].skillId])
        def apiSkills_t1 = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)
        def p1_apiSkills_t1 = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)
        def apiSkill1_t1 = skillsService.getSingleSkillSummary(userId, proj2.projectId, skills[0].skillId)
        then:
        skillsRes_t0.selfReportingType == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, SkillDef.SelfReportingType.Quiz.toString()]
        skillsRes_t0.quizId == [quiz.quizId, null, null, survey.quizId]
        skillsRes_t0.quizName == [quiz.name, null, null, survey.name]
        skillsRes_t0.quizType == [QuizDefParent.QuizType.Quiz.toString(), null, null, QuizDefParent.QuizType.Survey.toString()]
        skillsRes_t0.enabled == [true, true, true, true]

        skill1Res_t0.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill1Res_t0.quizId == quiz.quizId
        skill1Res_t0.quizName == quiz.name
        skill1Res_t0.quizType == QuizDefParent.QuizType.Quiz.toString()
        skill1Res_t0.enabled == true

        apiSkills_t0.skills.selfReporting?.type == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        apiSkills_t0.skills.selfReporting?.quizId == [quiz.quizId, null, null, survey.quizId]
        apiSkills_t0.skills.selfReporting?.quizName == [quiz.name, null, null, survey.name]
        apiSkills_t0.skills.selfReporting?.numQuizQuestions == [5, 0, null, 1]

        apiSkill1_t0.selfReporting?.type == SkillDef.SelfReportingType.Quiz.toString()
        apiSkill1_t0.selfReporting?.quizId == quiz.quizId
        apiSkill1_t0.selfReporting?.quizName == quiz.name
        apiSkill1_t0.selfReporting?.numQuizQuestions == 5

        skillsRes_t1.selfReportingType == [SkillDef.SelfReportingType.HonorSystem.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, SkillDef.SelfReportingType.Quiz.toString()]
        skillsRes_t1.quizId == [null, null, null, survey.quizId]
        skillsRes_t1.quizName == [null, null, null, survey.name]
        skillsRes_t1.quizType == [null, null, null, QuizDefParent.QuizType.Survey.toString()]
        skillsRes_t1.enabled == [true, true, true, true]

        skill1Res_t1.selfReportingType == SkillDef.SelfReportingType.HonorSystem.toString()
        skill1Res_t1.quizId == null
        skill1Res_t1.quizName == null
        skill1Res_t1.quizType == null
        skill1Res_t1.enabled == true

        p1_apiSkills_t1.skills.selfReporting?.type == [SkillDef.SelfReportingType.HonorSystem.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        p1_apiSkills_t1.skills.selfReporting?.quizId == [null, null, null, survey.quizId]
        p1_apiSkills_t1.skills.selfReporting?.quizName == [null, null, null, survey.name]
        p1_apiSkills_t1.skills.selfReporting?.numQuizQuestions == [0, 0, null, 1]

        apiSkills_t1.skills.selfReporting?.type == [SkillDef.SelfReportingType.HonorSystem.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        apiSkills_t1.skills.selfReporting?.quizId == [null, null, null, survey.quizId]
        apiSkills_t1.skills.selfReporting?.quizName == [null, null, null, survey.name]
        apiSkills_t1.skills.selfReporting?.numQuizQuestions == [0, 0, null, 1]

        apiSkill1_t1.selfReporting?.type == SkillDef.SelfReportingType.HonorSystem.toString()
        apiSkill1_t1.selfReporting?.quizId == null
        apiSkill1_t1.selfReporting?.quizName == null
        apiSkill1_t1.selfReporting?.numQuizQuestions == 0
    }

    def "update original quiz-skill - Quiz to non-self-reportable skill"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def survey = QuizDefFactory.createQuizSurvey(2)
        skillsService.createQuizDef(survey)
        def surveyQuestions = [QuizDefFactory.createSingleChoiceSurveyQuestion(2, 1, 2)]
        skillsService.createQuizQuestionDefs(surveyQuestions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(4, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[3].quizId = survey.quizId
        skillsService.createSkills(skills)

        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [])

        String userId = getRandomUsers(1).first()

        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, proj2_subj.subjectId, [
                [projectId: proj.projectId, skillId: skills[0].skillId],
                [projectId: proj.projectId, skillId: skills[1].skillId],
                [projectId: proj.projectId, skillId: skills[2].skillId],
                [projectId: proj.projectId, skillId: skills[3].skillId],
        ])
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId)
        def skillsRes_t0 = skillsService.getSkillsForSubject(proj2.projectId, proj2_subj.subjectId, true)
        def skill1Res_t0 = skillsService.getSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: skills[0].skillId])
        def apiSkills_t0 = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)
        def apiSkill1_t0 = skillsService.getSingleSkillSummary(userId, proj2.projectId, skills[0].skillId)

        when:
        skills[0].quizId = null
        skills[0].selfReportingType = null
        skillsService.createSkill(skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def skillsRes_t1 = skillsService.getSkillsForSubject(proj2.projectId, proj2_subj.subjectId, true)
        def skill1Res_t1 = skillsService.getSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: skills[0].skillId])
        def apiSkills_t1 = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)
        def p1_apiSkills_t1 = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)
        def apiSkill1_t1 = skillsService.getSingleSkillSummary(userId, proj2.projectId, skills[0].skillId)
        then:
        skillsRes_t0.selfReportingType == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, SkillDef.SelfReportingType.Quiz.toString()]
        skillsRes_t0.quizId == [quiz.quizId, null, null, survey.quizId]
        skillsRes_t0.quizName == [quiz.name, null, null, survey.name]
        skillsRes_t0.quizType == [QuizDefParent.QuizType.Quiz.toString(), null, null, QuizDefParent.QuizType.Survey.toString()]
        skillsRes_t0.enabled == [true, true, true, true]

        skill1Res_t0.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill1Res_t0.quizId == quiz.quizId
        skill1Res_t0.quizName == quiz.name
        skill1Res_t0.quizType == QuizDefParent.QuizType.Quiz.toString()
        skill1Res_t0.enabled == true

        apiSkills_t0.skills.selfReporting?.type == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        apiSkills_t0.skills.selfReporting?.quizId == [quiz.quizId, null, null, survey.quizId]
        apiSkills_t0.skills.selfReporting?.quizName == [quiz.name, null, null, survey.name]
        apiSkills_t0.skills.selfReporting?.numQuizQuestions == [5, 0, null, 1]

        apiSkill1_t0.selfReporting?.type == SkillDef.SelfReportingType.Quiz.toString()
        apiSkill1_t0.selfReporting?.quizId == quiz.quizId
        apiSkill1_t0.selfReporting?.quizName == quiz.name
        apiSkill1_t0.selfReporting?.numQuizQuestions == 5

        skillsRes_t1.selfReportingType == [null, SkillDef.SelfReportingType.HonorSystem.toString(), null, SkillDef.SelfReportingType.Quiz.toString()]
        skillsRes_t1.quizId == [null, null, null, survey.quizId]
        skillsRes_t1.quizName == [null, null, null, survey.name]
        skillsRes_t1.quizType == [null, null, null, QuizDefParent.QuizType.Survey.toString()]
        skillsRes_t1.enabled == [true, true, true, true]

        skill1Res_t1.selfReportingType == null
        skill1Res_t1.quizId == null
        skill1Res_t1.quizName == null
        skill1Res_t1.quizType == null
        skill1Res_t1.enabled == true

        p1_apiSkills_t1.skills.selfReporting?.type == [null, SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        p1_apiSkills_t1.skills.selfReporting?.quizId == [null, null, null, survey.quizId]
        p1_apiSkills_t1.skills.selfReporting?.quizName == [null, null, null, survey.name]
        p1_apiSkills_t1.skills.selfReporting?.numQuizQuestions == [null, 0, null, 1]

        apiSkills_t1.skills.selfReporting?.type == [null, SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        apiSkills_t1.skills.selfReporting?.quizId == [null, null, null, survey.quizId]
        apiSkills_t1.skills.selfReporting?.quizName == [null, null, null, survey.name]
        apiSkills_t1.skills.selfReporting?.numQuizQuestions == [null, 0, null, 1]

        apiSkill1_t1.selfReporting?.type == null
        apiSkill1_t1.selfReporting?.quizId == null
        apiSkill1_t1.selfReporting?.quizName == null
        apiSkill1_t1.selfReporting?.numQuizQuestions == 0
    }

    def "update original quiz-skill - Quiz to Survey"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def survey = QuizDefFactory.createQuizSurvey(2)
        skillsService.createQuizDef(survey)
        def surveyQuestions = [QuizDefFactory.createSingleChoiceSurveyQuestion(2, 1, 2)]
        skillsService.createQuizQuestionDefs(surveyQuestions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(4, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[3].quizId = survey.quizId
        skillsService.createSkills(skills)

        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [])

        String userId = getRandomUsers(1).first()

        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, proj2_subj.subjectId, [
                [projectId: proj.projectId, skillId: skills[0].skillId],
                [projectId: proj.projectId, skillId: skills[1].skillId],
                [projectId: proj.projectId, skillId: skills[2].skillId],
                [projectId: proj.projectId, skillId: skills[3].skillId],
        ])
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId)
        def skillsRes_t0 = skillsService.getSkillsForSubject(proj2.projectId, proj2_subj.subjectId, true)
        def skill1Res_t0 = skillsService.getSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: skills[0].skillId])
        def apiSkills_t0 = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)
        def apiSkill1_t0 = skillsService.getSingleSkillSummary(userId, proj2.projectId, skills[0].skillId)

        when:
        skills[0].quizId = survey.quizId
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skillsService.createSkill(skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def skillsRes_t1 = skillsService.getSkillsForSubject(proj2.projectId, proj2_subj.subjectId, true)
        def skill1Res_t1 = skillsService.getSkill([projectId: proj2.projectId, subjectId: proj2_subj.subjectId, skillId: skills[0].skillId])
        def apiSkills_t1 = skillsService.getSkillSummary(userId, proj2.projectId, proj2_subj.subjectId)
        def p1_apiSkills_t1 = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)
        def apiSkill1_t1 = skillsService.getSingleSkillSummary(userId, proj2.projectId, skills[0].skillId)
        then:
        skillsRes_t0.selfReportingType == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, SkillDef.SelfReportingType.Quiz.toString()]
        skillsRes_t0.quizId == [quiz.quizId, null, null, survey.quizId]
        skillsRes_t0.quizName == [quiz.name, null, null, survey.name]
        skillsRes_t0.quizType == [QuizDefParent.QuizType.Quiz.toString(), null, null, QuizDefParent.QuizType.Survey.toString()]
        skillsRes_t0.enabled == [true, true, true, true]

        skill1Res_t0.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill1Res_t0.quizId == quiz.quizId
        skill1Res_t0.quizName == quiz.name
        skill1Res_t0.quizType == QuizDefParent.QuizType.Quiz.toString()
        skill1Res_t0.enabled == true

        apiSkills_t0.skills.selfReporting?.type == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        apiSkills_t0.skills.selfReporting?.quizId == [quiz.quizId, null, null, survey.quizId]
        apiSkills_t0.skills.selfReporting?.quizName == [quiz.name, null, null, survey.name]
        apiSkills_t0.skills.selfReporting?.numQuizQuestions == [5, 0, null, 1]

        apiSkill1_t0.selfReporting?.type == SkillDef.SelfReportingType.Quiz.toString()
        apiSkill1_t0.selfReporting?.quizId == quiz.quizId
        apiSkill1_t0.selfReporting?.quizName == quiz.name
        apiSkill1_t0.selfReporting?.numQuizQuestions == 5

        skillsRes_t1.selfReportingType == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, SkillDef.SelfReportingType.Quiz.toString()]
        skillsRes_t1.quizId == [survey.quizId, null, null, survey.quizId]
        skillsRes_t1.quizName == [survey.name, null, null, survey.name]
        skillsRes_t1.quizType == [QuizDefParent.QuizType.Survey.toString(), null, null, QuizDefParent.QuizType.Survey.toString()]
        skillsRes_t1.enabled == [true, true, true, true]

        skill1Res_t1.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill1Res_t1.quizId == survey.quizId
        skill1Res_t1.quizName == survey.name
        skill1Res_t1.quizType == QuizDefParent.QuizType.Survey.toString()
        skill1Res_t1.enabled == true

        p1_apiSkills_t1.skills.selfReporting?.type == [QuizDefParent.QuizType.Survey.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        p1_apiSkills_t1.skills.selfReporting?.quizId == [survey.quizId, null, null, survey.quizId]
        p1_apiSkills_t1.skills.selfReporting?.quizName == [survey.name, null, null, survey.name]
        p1_apiSkills_t1.skills.selfReporting?.numQuizQuestions == [1, 0, null, 1]

        apiSkills_t1.skills.selfReporting?.type == [QuizDefParent.QuizType.Survey.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        apiSkills_t1.skills.selfReporting?.quizId == [survey.quizId, null, null, survey.quizId]
        apiSkills_t1.skills.selfReporting?.quizName == [survey.name, null, null, survey.name]
        apiSkills_t1.skills.selfReporting?.numQuizQuestions == [1, 0, null, 1]

        apiSkill1_t1.selfReporting?.type == QuizDefParent.QuizType.Survey.toString()
        apiSkill1_t1.selfReporting?.quizId == survey.quizId
        apiSkill1_t1.selfReporting?.quizName == survey.name
        apiSkill1_t1.selfReporting?.numQuizQuestions == 1
    }

    def "accomplish imported skill by completing quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skillsService.createSkills(skills)

        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [])

        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, proj2_subj.subjectId, [
                [projectId: proj.projectId, skillId: skills[0].skillId],
                [projectId: proj.projectId, skillId: skills[1].skillId],
                [projectId: proj.projectId, skillId: skills[2].skillId],
        ])
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId)
        String userId = getRandomUsers(1).first()
        SkillsService otherUserService = createService(userId)
        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def userOverallProgress_t0 = skillsService.getSkillSummary(otherUserService.userName, proj2.projectId)
        def userOverallProgressProj2_t0 = skillsService.getSkillSummary(otherUserService.userName, proj2.projectId)

        def p1_skillRes_t0 = skillsService.getSingleSkillSummary(otherUserService.userName, proj.projectId, skills[0].skillId)
        def p2_skillRes_t0 = skillsService.getSingleSkillSummary(otherUserService.userName, proj2.projectId, skills[0].skillId)

        def quizAttempt =  otherUserService.startQuizAttempt(quiz.quizId).body
        otherUserService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        otherUserService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = otherUserService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def p1_skillRes = skillsService.getSingleSkillSummary(otherUserService.userName, proj.projectId, skills[0].skillId)
        def p2_skillRes = skillsService.getSingleSkillSummary(otherUserService.userName, proj2.projectId, skills[0].skillId)

        def userOverallProgress_t1 = skillsService.getSkillSummary(otherUserService.userName, proj.projectId)
        def userOverallProgressProj2_t1 = skillsService.getSkillSummary(otherUserService.userName, proj2.projectId)
        then:
        gradedQuizAttempt.passed == true
        p1_skillRes_t0.points ==  0
        p2_skillRes_t0.points ==  0
        p1_skillRes.points ==  skills[0].pointIncrement
        p2_skillRes.points ==  skills[0].pointIncrement

        userOverallProgress_t0.points == 0
        userOverallProgress_t0.skillsLevel == 0

        userOverallProgressProj2_t0.points == 0
        userOverallProgressProj2_t0.skillsLevel == 0

        userOverallProgress_t1.points == skills[0].pointIncrement
        userOverallProgress_t1.skillsLevel == 2

        userOverallProgressProj2_t1.skillsLevel == 2
        userOverallProgressProj2_t1.points == skills[0].pointIncrement
    }

    def "accomplish imported skill by assigning completed quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        String userId = getRandomUsers(1).first()
        SkillsService otherUserService = createService(userId)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  otherUserService.startQuizAttempt(quiz.quizId).body
        otherUserService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        otherUserService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = otherUserService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[1].selfReportingType = SkillDef.SelfReportingType.HonorSystem
        skillsService.createSkills(skills)

        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        skillsService.createProjectAndSubjectAndSkills(proj2, proj2_subj, [])

        skillsService.bulkImportSkillsFromCatalog(proj2.projectId, proj2_subj.subjectId, [
                [projectId: proj.projectId, skillId: skills[0].skillId],
                [projectId: proj.projectId, skillId: skills[1].skillId],
                [projectId: proj.projectId, skillId: skills[2].skillId],
        ])
        skillsService.finalizeSkillsImportFromCatalog(proj2.projectId)

        when:
        def p1_skillRes_t0 = skillsService.getSingleSkillSummary(otherUserService.userName, proj.projectId, skills[0].skillId)
        def p2_skillRes_t0 = skillsService.getSingleSkillSummary(otherUserService.userName, proj2.projectId, skills[0].skillId)
        def userOverallProgress_t0 = skillsService.getSkillSummary(otherUserService.userName, proj2.projectId)
        def userOverallProgressProj2_t0 = skillsService.getSkillSummary(otherUserService.userName, proj2.projectId)

        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkill(skills[0])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def p1_skillRes = skillsService.getSingleSkillSummary(otherUserService.userName, proj.projectId, skills[0].skillId)
        def p2_skillRes = skillsService.getSingleSkillSummary(otherUserService.userName, proj2.projectId, skills[0].skillId)
        def userOverallProgress_t1 = skillsService.getSkillSummary(otherUserService.userName, proj.projectId)
        def userOverallProgressProj2_t1 = skillsService.getSkillSummary(otherUserService.userName, proj2.projectId)
        def userOverallProgressProj2Subj1_t1 = skillsService.getSkillSummary(otherUserService.userName, proj2.projectId, proj2_subj.subjectId)
        then:
        gradedQuizAttempt.passed == true
        p1_skillRes_t0.points ==  0
        p2_skillRes_t0.points ==  0
        p1_skillRes.points ==  skills[0].pointIncrement
        p2_skillRes.points ==  skills[0].pointIncrement

        userOverallProgress_t0.points == 0
        userOverallProgress_t0.skillsLevel == 0

        userOverallProgressProj2_t0.points == 0
        userOverallProgressProj2_t0.skillsLevel == 0

        userOverallProgress_t1.points == skills[0].pointIncrement
        userOverallProgress_t1.skillsLevel == 2

        userOverallProgressProj2_t1.skillsLevel == 2
        userOverallProgressProj2_t1.points == skills[0].pointIncrement

        userOverallProgressProj2Subj1_t1.skillsLevel == 2
        userOverallProgressProj2Subj1_t1.points == skills[0].pointIncrement
    }


}


