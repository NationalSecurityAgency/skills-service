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
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class QuizSkillsAndInProjectReuseSpecs extends DefaultIntSpec {

    def "reuse quiz-skill"() {
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

        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)

        String userId = getRandomUsers(1).first()
        when:
        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[0].skillId, subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[1].skillId, subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[2].skillId, subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[3].skillId, subj2.subjectId)

        def skillsRes = skillsService.getSkillsForSubject(proj.projectId, subj2.subjectId, true)
        def skill1Res = skillsService.getSkill([projectId: proj.projectId, subjectId: subj2.subjectId, skillId: SkillReuseIdUtil.addTag(skills[0].skillId, 0)])
        def skill4Res = skillsService.getSkill([projectId: proj.projectId, subjectId: subj2.subjectId, skillId: SkillReuseIdUtil.addTag(skills[3].skillId, 0)])
        def apiSkills = skillsService.getSkillSummary(userId, proj.projectId, subj2.subjectId)
        def apiSkill1 = skillsService.getSingleSkillSummary(userId, proj.projectId, SkillReuseIdUtil.addTag(skills[0].skillId, 0))
        def apiSkill1WithSubj = skillsService.getSingleSkillSummaryWithSubject(userId, proj.projectId, subj2.subjectId, SkillReuseIdUtil.addTag(skills[0].skillId, 0))
        def apiSkill4 = skillsService.getSingleSkillSummary(userId, proj.projectId, SkillReuseIdUtil.addTag(skills[3].skillId, 0))
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

    def "reuse quiz-skill to a group in another subject"() {
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

        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)
        def subj2g1 = createSkillsGroup(1, 2, 11)
        skillsService.createSkill(subj2g1)

        String userId = getRandomUsers(1).first()
        when:
        skillsService.reuseSkills(proj.projectId, skills.collect { it.skillId }, subj2.subjectId, subj2g1.skillId)

        def skillsRes = skillsService.getSkillsForGroup(proj.projectId, subj2g1.skillId)
        def skill1Res = skillsService.getSkill([projectId: proj.projectId, subjectId: subj2.subjectId, skillId: SkillReuseIdUtil.addTag(skills[0].skillId, 0)])
        def skill4Res = skillsService.getSkill([projectId: proj.projectId, subjectId: subj2.subjectId, skillId: SkillReuseIdUtil.addTag(skills[3].skillId, 0)])
        def apiSkills = skillsService.getSkillSummary(userId, proj.projectId, subj2.subjectId)
        def apiSkill1 = skillsService.getSingleSkillSummary(userId, proj.projectId, SkillReuseIdUtil.addTag(skills[0].skillId, 0))
        def apiSkill1WithSubj = skillsService.getSingleSkillSummaryWithSubject(userId, proj.projectId, subj2.subjectId, SkillReuseIdUtil.addTag(skills[0].skillId, 0))
        def apiSkill4 = skillsService.getSingleSkillSummary(userId, proj.projectId, SkillReuseIdUtil.addTag(skills[3].skillId, 0))
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

        def groupChildren = apiSkills.skills.find { it.skillId == subj2g1.skillId }.children
        groupChildren.selfReporting?.type == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        groupChildren.selfReporting?.quizId == [quiz.quizId, null, null, survey.quizId]
        groupChildren.selfReporting?.quizName == [quiz.name, null, null, survey.name]
        groupChildren.selfReporting?.numQuizQuestions == [5, 0, null, 1]

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

    def "reuse quiz-skill to a group in the same subject"() {
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

        def subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createSkill(subj1g1)

        String userId = getRandomUsers(1).first()
        when:
        skillsService.reuseSkills(proj.projectId, skills.collect { it.skillId }, subj.subjectId, subj1g1.skillId)

        def skillsRes = skillsService.getSkillsForGroup(proj.projectId, subj1g1.skillId)
        def skill1Res = skillsService.getSkill([projectId: proj.projectId, subjectId: subj.subjectId, skillId: SkillReuseIdUtil.addTag(skills[0].skillId, 0)])
        def skill4Res = skillsService.getSkill([projectId: proj.projectId, subjectId: subj.subjectId, skillId: SkillReuseIdUtil.addTag(skills[3].skillId, 0)])
        def apiSkills = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)
        def apiSkill1 = skillsService.getSingleSkillSummary(userId, proj.projectId, SkillReuseIdUtil.addTag(skills[0].skillId, 0))
        def apiSkill1WithSubj = skillsService.getSingleSkillSummaryWithSubject(userId, proj.projectId, subj.subjectId, SkillReuseIdUtil.addTag(skills[0].skillId, 0))
        def apiSkill4 = skillsService.getSingleSkillSummary(userId, proj.projectId, SkillReuseIdUtil.addTag(skills[3].skillId, 0))
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

        def groupChildren = apiSkills.skills.find { it.skillId == subj1g1.skillId }.children
        groupChildren.selfReporting?.type == [SkillDef.SelfReportingType.Quiz.toString(), SkillDef.SelfReportingType.HonorSystem.toString(), null, QuizDefParent.QuizType.Survey.toString()]
        groupChildren.selfReporting?.quizId == [quiz.quizId, null, null, survey.quizId]
        groupChildren.selfReporting?.quizName == [quiz.name, null, null, survey.name]
        groupChildren.selfReporting?.numQuizQuestions == [5, 0, null, 1]

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

    def "re-used skill; update the original quiz-skill - Honor System to Quiz"() {
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

        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)

        String userId = getRandomUsers(1).first()

        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[0].skillId, subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[1].skillId, subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[2].skillId, subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[3].skillId, subj2.subjectId)

        def skillsRes_t0 = skillsService.getSkillsForSubject(proj.projectId, subj2.subjectId, true)
        def skill2Res_t0 = skillsService.getSkill([projectId: proj.projectId, subjectId: subj2.subjectId, skillId: SkillReuseIdUtil.addTag(skills[1].skillId, 0)])
        def apiSkills_t0 = skillsService.getSkillSummary(userId, proj.projectId, subj2.subjectId)
        def apiSkill2_t0 = skillsService.getSingleSkillSummary(userId, proj.projectId, SkillReuseIdUtil.addTag(skills[1].skillId, 0))

        when:
        skills[1].quizId = quiz.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skillsService.createSkill(skills[1])
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def skillsRes_t1 = skillsService.getSkillsForSubject(proj.projectId, subj2.subjectId, true)
        def skill2Res_t1 = skillsService.getSkill([projectId: proj.projectId, subjectId: subj2.subjectId, skillId: SkillReuseIdUtil.addTag(skills[1].skillId, 0)])
        def apiSkills_t1 = skillsService.getSkillSummary(userId, proj.projectId, subj2.subjectId)
        def p1_apiSkills_t1 = skillsService.getSkillSummary(userId, proj.projectId, subj.subjectId)
        def apiSkill2_t1 = skillsService.getSingleSkillSummary(userId, proj.projectId, SkillReuseIdUtil.addTag(skills[1].skillId, 0))
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

    def "accomplish reused skill in another subject by completing quiz"() {
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

        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)

        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[0].skillId, subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[1].skillId, subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[2].skillId, subj2.subjectId)
        String userId = getRandomUsers(1).first()
        SkillsService otherUserService = createService(userId)
        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  otherUserService.startQuizAttempt(quiz.quizId).body
        otherUserService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        otherUserService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = otherUserService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def p1_skillRes = skillsService.getSingleSkillSummary(otherUserService.userName, proj.projectId, skills[0].skillId)
        def p2_skillRes = skillsService.getSingleSkillSummary(otherUserService.userName, proj.projectId, SkillReuseIdUtil.addTag(skills[0].skillId, 0))
        then:
        gradedQuizAttempt.passed == true
        p1_skillRes.points ==  skills[0].pointIncrement
        p2_skillRes.points ==  skills[0].pointIncrement
    }

    def "accomplish reused skill in skill groups and subject by completing quiz"() {
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

        def subj2 = createSubject(1, 2)
        skillsService.createSubject(subj2)

        def subj1g1 = createSkillsGroup(1, 1, 11)
        skillsService.createSkill(subj1g1)

        def subj2g1 = createSkillsGroup(1, 2, 12)
        skillsService.createSkill(subj2g1)

        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[0].skillId, subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[1].skillId, subj2.subjectId)
        skillsService.reuseSkillInAnotherSubject(proj.projectId, skills[2].skillId, subj2.subjectId)

        skillsService.reuseSkills(proj.projectId, skills.collect { it.skillId }, subj.subjectId, subj1g1.skillId)
        skillsService.reuseSkills(proj.projectId, skills.collect { it.skillId }, subj2.subjectId, subj2g1.skillId)

        String userId = getRandomUsers(1).first()
        SkillsService otherUserService = createService(userId)
        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  otherUserService.startQuizAttempt(quiz.quizId).body
        otherUserService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        otherUserService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = otherUserService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        def p1_skillRes = skillsService.getSingleSkillSummary(otherUserService.userName, proj.projectId, skills[0].skillId)
        def p2_skillRes = skillsService.getSingleSkillSummary(otherUserService.userName, proj.projectId, SkillReuseIdUtil.addTag(skills[0].skillId, 0))
        def p1g1_skillRes = skillsService.getSingleSkillSummary(otherUserService.userName, proj.projectId, SkillReuseIdUtil.addTag(skills[0].skillId, 1))
        def p2g1_skillRes = skillsService.getSingleSkillSummary(otherUserService.userName, proj.projectId, SkillReuseIdUtil.addTag(skills[0].skillId, 2))
        then:
        gradedQuizAttempt.passed == true
        p1_skillRes.points ==  skills[0].pointIncrement
        p2_skillRes.points ==  skills[0].pointIncrement
        p1g1_skillRes.points ==  skills[0].pointIncrement
        p2g1_skillRes.points ==  skills[0].pointIncrement
    }

}


