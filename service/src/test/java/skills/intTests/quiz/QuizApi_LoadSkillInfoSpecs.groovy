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
import skills.quizLoading.QuizSettings
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef

import java.time.Instant

import static skills.intTests.utils.SkillsFactory.*

class QuizApi_LoadSkillInfoSpecs extends DefaultIntSpec {

    def "return quiz information with the skills"() {
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

        List<String> users = getRandomUsers(1)

        when:
        def skillsRes = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def skillRes = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[0].skillId)
        then:
        skillsRes.skills[0].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillsRes.skills[0].selfReporting.quizId == quiz.quizId
        skillsRes.skills[0].selfReporting.quizName == quiz.name
        skillsRes.skills[0].selfReporting.numQuizQuestions == 5

        !skillsRes.skills[1].selfReporting.enabled
        !skillsRes.skills[2].selfReporting.enabled

        skillRes.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillRes.selfReporting.quizId == quiz.quizId
        skillRes.selfReporting.quizName == quiz.name
        skillRes.selfReporting.numQuizQuestions == 5
    }

    def "return quiz information with the skills - MinNumQuestionsToPass is set"() {
        def createQuiz = { int quizNum ->
            def quiz = QuizDefFactory.createQuiz(quizNum, "Fancy Description")
            skillsService.createQuizDef(quiz)
            def questions = QuizDefFactory.createChoiceQuestions(quizNum, 5, 2)
            skillsService.createQuizQuestionDefs(questions)

            return quiz
        }

        def quiz1 = createQuiz(1)
        skillsService.saveQuizSettings(quiz1.quizId, [
                [setting: QuizSettings.QuizLength.setting, value: '1'],
        ])

        def quiz2 = createQuiz(2)
        skillsService.saveQuizSettings(quiz2.quizId, [
                [setting: QuizSettings.QuizLength.setting, value: '3'],
        ])

        def quiz3 = createQuiz(3)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[2].quizId = quiz3.quizId
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(1)

        when:
        def skillsRes = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def skillRes = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[0].skillId)
        def skill2Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[1].skillId)
        def skill3Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[2].skillId)
        then:
        skillsRes.skills[0].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillsRes.skills[0].selfReporting.quizId == quiz1.quizId
        skillsRes.skills[0].selfReporting.quizName == quiz1.name
        skillsRes.skills[0].selfReporting.numQuizQuestions == 1

        skillsRes.skills[1].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillsRes.skills[1].selfReporting.quizId == quiz2.quizId
        skillsRes.skills[1].selfReporting.quizName == quiz2.name
        skillsRes.skills[1].selfReporting.numQuizQuestions == 3

        skillsRes.skills[2].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillsRes.skills[2].selfReporting.quizId == quiz3.quizId
        skillsRes.skills[2].selfReporting.quizName == quiz3.name
        skillsRes.skills[2].selfReporting.numQuizQuestions == 5

        skillRes.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillRes.selfReporting.quizId == quiz1.quizId
        skillRes.selfReporting.quizName == quiz1.name
        skillRes.selfReporting.numQuizQuestions == 1

        skill2Res.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skill2Res.selfReporting.quizId == quiz2.quizId
        skill2Res.selfReporting.quizName == quiz2.name
        skill2Res.selfReporting.numQuizQuestions == 3

        skill3Res.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skill3Res.selfReporting.quizId == quiz3.quizId
        skill3Res.selfReporting.quizName == quiz3.name
        skill3Res.selfReporting.numQuizQuestions == 5
    }


    def "return quiz information with the skills - same quiz is associated to multiple skills"() {
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
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz.quizId
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(1)

        when:
        def skillsRes = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def skillRes = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[0].skillId)
        def skill1Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[1].skillId)
        then:
        skillsRes.skills[0].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillsRes.skills[0].selfReporting.quizId == quiz.quizId
        skillsRes.skills[0].selfReporting.quizName == quiz.name
        skillsRes.skills[0].selfReporting.numQuizQuestions == 5

        skillsRes.skills[1].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillsRes.skills[1].selfReporting.quizId == quiz.quizId
        skillsRes.skills[1].selfReporting.quizName == quiz.name
        skillsRes.skills[1].selfReporting.numQuizQuestions == 5

        !skillsRes.skills[2].selfReporting.enabled

        skillRes.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillRes.selfReporting.quizId == quiz.quizId
        skillRes.selfReporting.quizName == quiz.name
        skillRes.selfReporting.numQuizQuestions == 5

        skill1Res.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skill1Res.selfReporting.quizId == quiz.quizId
        skill1Res.selfReporting.quizName == quiz.name
        skill1Res.selfReporting.numQuizQuestions == 5
    }

    def "return survey information with the skills"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3, QuizDefParent.QuizType.Survey),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4, QuizDefParent.QuizType.Survey),
                QuizDefFactory.createTextInputQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(1)

        when:
        def skillsRes = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def skillRes = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[0].skillId)
        then:
        skillsRes.skills[0].selfReporting.type == "Survey"
        skillsRes.skills[0].selfReporting.quizId == quiz.quizId
        skillsRes.skills[0].selfReporting.quizName == quiz.name
        skillsRes.skills[0].selfReporting.numQuizQuestions == 3
        !skillsRes.skills[1].selfReporting.enabled
        !skillsRes.skills[2].selfReporting.enabled

        skillRes.selfReporting.type == "Survey"
        skillRes.selfReporting.quizId == quiz.quizId
        skillRes.selfReporting.quizName == quiz.name
        skillsRes.skills[0].selfReporting.numQuizQuestions == 3
    }

    def "return quiz num questions with the skills"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(1, 1, 2))

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, 2, 2))

        def quiz3 = QuizDefFactory.createQuiz(3)
        skillsService.createQuizDef(quiz3)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(3, 3, 2))

        def quiz4 = QuizDefFactory.createQuiz(4)
        skillsService.createQuizDef(quiz4)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(4, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[2].quizId = quiz3.quizId
        skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[3].quizId = quiz4.quizId
        skillsService.createSkills(skills)


        Map badge = [projectId: proj.projectId, badgeId: 'badge1', name: 'Test Badge 1']
        skillsService.addBadge(badge)
        skills.each {
            skillsService.assignSkillToBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: it.skillId)
        }
        badge.enabled = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        List<String> users = getRandomUsers(1)

        when:
        def skillsRes = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def skill1Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[0].skillId)
        def skill2Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[1].skillId)
        def skill3Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[2].skillId)
        def skill4Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[3].skillId)
        def badgeSummary = skillsService.getBadgeSummary(users[0], proj.projectId, badge.badgeId)
        then:
        skillsRes.skills[0].selfReporting.numQuizQuestions == 1
        skillsRes.skills[0].selfReporting.quizName == quiz1.name
        skillsRes.skills[1].selfReporting.numQuizQuestions == 2
        skillsRes.skills[1].selfReporting.quizName == quiz2.name
        skillsRes.skills[2].selfReporting.numQuizQuestions == 3
        skillsRes.skills[2].selfReporting.quizName == quiz3.name
        skillsRes.skills[3].selfReporting.numQuizQuestions == 0
        skillsRes.skills[3].selfReporting.quizName == quiz4.name
        skill1Res.selfReporting.numQuizQuestions == 1
        skill1Res.selfReporting.quizName == quiz1.name
        skill2Res.selfReporting.numQuizQuestions == 2
        skill2Res.selfReporting.quizName == quiz2.name
        skill3Res.selfReporting.numQuizQuestions == 3
        skill3Res.selfReporting.quizName == quiz3.name
        skill4Res.selfReporting.numQuizQuestions == 0
        skill4Res.selfReporting.quizName == quiz4.name

        badgeSummary.skills[0].selfReporting.numQuizQuestions == 1
        badgeSummary.skills[0].selfReporting.quizName == quiz1.name
        badgeSummary.skills[1].selfReporting.numQuizQuestions == 2
        badgeSummary.skills[1].selfReporting.quizName == quiz2.name
        badgeSummary.skills[2].selfReporting.numQuizQuestions == 3
        badgeSummary.skills[2].selfReporting.quizName == quiz3.name
        badgeSummary.skills[3].selfReporting.numQuizQuestions == 0
        badgeSummary.skills[3].selfReporting.quizName == quiz4.name
    }

    def "return quiz grading information for a single skill"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputQuestion(1, 1),
                QuizDefFactory.createTextInputQuestion(1, 2)
        ])

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkills(skills)

        String user = skillsService.userName
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body

        when:
        def skillRes = skillsService.getSingleSkillSummary(user, proj.projectId, skills[0].skillId)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        def skillRes_t1 = skillsService.getSingleSkillSummary(user, proj.projectId, skills[0].skillId)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        def skillRes_t2 = skillsService.getSingleSkillSummary(user, proj.projectId, skills[0].skillId)
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        def skillRes_t3 = skillsService.getSingleSkillSummary(user, proj.projectId, skills[0].skillId)
        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, true, "Good answer")
        def skillRes_t4 = skillsService.getSingleSkillSummary(user, proj.projectId, skills[0].skillId)
        skillsService.gradeAnswer(skillsService.userName, quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id, true, "Good answer")
        def skillRes_t5 = skillsService.getSingleSkillSummary(user, proj.projectId, skills[0].skillId)
        then:
        skillRes.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillRes.selfReporting.quizId == quiz.quizId
        skillRes.selfReporting.quizName == quiz.name
        skillRes.selfReporting.numQuizQuestions == 2
        skillRes.selfReporting.quizNeedsGrading == false
        skillRes.selfReporting.quizNeedsGradingAttemptDate == null
        skillRes.selfReporting.quizOrSurveyPassed == false
        skillRes.selfReporting.quizAttemptId == null

        skillRes_t1.selfReporting.quizNeedsGrading == false
        skillRes_t1.selfReporting.quizNeedsGradingAttemptDate == null
        skillRes_t1.selfReporting.quizOrSurveyPassed == false
        skillRes_t1.selfReporting.quizAttemptId == null

        skillRes_t2.selfReporting.quizNeedsGrading == false
        skillRes_t2.selfReporting.quizNeedsGradingAttemptDate == null
        skillRes_t2.selfReporting.quizOrSurveyPassed == false
        skillRes_t2.selfReporting.quizAttemptId == null

        skillRes_t3.selfReporting.quizNeedsGrading == true
        skillRes_t3.selfReporting.quizNeedsGradingAttemptDate != null
        skillRes_t3.selfReporting.quizOrSurveyPassed == false
        skillRes_t3.selfReporting.quizAttemptId == quizAttempt.id

        skillRes_t4.selfReporting.quizNeedsGrading == true
        skillRes_t4.selfReporting.quizNeedsGradingAttemptDate != null
        skillRes_t4.selfReporting.quizOrSurveyPassed == false
        skillRes_t4.selfReporting.quizAttemptId == quizAttempt.id

        skillRes_t5.selfReporting.quizNeedsGrading == false
        skillRes_t5.selfReporting.quizNeedsGradingAttemptDate == null
        skillRes_t2.selfReporting.quizOrSurveyPassed == false
        skillRes_t2.selfReporting.quizAttemptId == null
    }

    def "return quiz grading information for a single skill - passed"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkills(skills)

        String user = skillsService.userName
        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        when:
        def skillRes = skillsService.getSingleSkillSummary(user, proj.projectId, skills[0].skillId)
        then:
        skillRes.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillRes.selfReporting.quizId == quiz.quizId
        skillRes.selfReporting.quizName == quiz.name
        skillRes.selfReporting.numQuizQuestions == 1
        skillRes.selfReporting.quizNeedsGrading == false
        skillRes.selfReporting.quizNeedsGradingAttemptDate == null
        skillRes.selfReporting.quizOrSurveyPassed == true
        skillRes.selfReporting.quizAttemptId == quizAttempt.id
    }

    def "return quiz grading information for a single skill - multiple users one failed - one passed - one needs grading"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputQuestion(1, 1),
        ])

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(3)
        SkillsService userWithFailedAttempt = createService(users[0])
        SkillsService userWithPassedAttempt = createService(users[1])
        SkillsService userWithNeedsGradingAttempt = createService(users[2])

        def quizAttempt = userWithFailedAttempt.startQuizAttempt(quiz.quizId).body
        userWithFailedAttempt.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        userWithFailedAttempt.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        skillsService.gradeAnswer(userWithFailedAttempt.userName, quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, false, "Good answer")

        def quizAttempt1 = userWithPassedAttempt.startQuizAttempt(quiz.quizId).body
        userWithPassedAttempt.reportQuizAnswer(quiz.quizId, quizAttempt1.id, quizAttempt1.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        userWithPassedAttempt.completeQuizAttempt(quiz.quizId, quizAttempt1.id).body
        skillsService.gradeAnswer(userWithPassedAttempt.userName, quiz.quizId, quizAttempt1.id, quizAttempt1.questions[0].answerOptions[0].id, true, "Good answer")

        def quizAttempt2 = userWithNeedsGradingAttempt.startQuizAttempt(quiz.quizId).body
        userWithNeedsGradingAttempt.reportQuizAnswer(quiz.quizId, quizAttempt2.id, quizAttempt2.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])
        userWithNeedsGradingAttempt.completeQuizAttempt(quiz.quizId, quizAttempt2.id).body

        when:
        def failedRes = skillsService.getSingleSkillSummary(userWithFailedAttempt.userName, proj.projectId, skills[0].skillId)
        def passedRes = skillsService.getSingleSkillSummary(userWithPassedAttempt.userName, proj.projectId, skills[0].skillId)
        def needsGradingRes = skillsService.getSingleSkillSummary(userWithNeedsGradingAttempt.userName, proj.projectId, skills[0].skillId)
        then:
        failedRes.selfReporting.quizNeedsGrading == false
        failedRes.selfReporting.quizNeedsGradingAttemptDate == null
        failedRes.selfReporting.quizOrSurveyPassed == false
        failedRes.selfReporting.quizAttemptId == null

        passedRes.selfReporting.quizNeedsGrading == false
        passedRes.selfReporting.quizNeedsGradingAttemptDate == null
        passedRes.selfReporting.quizOrSurveyPassed == true
        passedRes.selfReporting.quizAttemptId == quizAttempt1.id

        needsGradingRes.selfReporting.quizNeedsGrading == true
        needsGradingRes.selfReporting.quizNeedsGradingAttemptDate != null
        needsGradingRes.selfReporting.quizOrSurveyPassed == false
        needsGradingRes.selfReporting.quizAttemptId == quizAttempt2.id
    }

    def "return latest attempt info in a subject summary when quizzes are associated"() {
        def now = new Date()
        def fiveMinutesAgo = now - 5 * 60 * 1000
        def fiveMinutesFromNow = now + 5 * 60 * 1000

        def createQuiz = { Integer quizNum, List<QuizQuestionType> questionTypes = [QuizQuestionType.TextInput] ->
            def quiz = QuizDefFactory.createQuiz(quizNum, "Fancy Description")
            skillsService.createQuizDef(quiz)
            def questions = []
            questionTypes.eachWithIndex { it, index ->
                if (it == QuizQuestionType.TextInput) {
                    questions << QuizDefFactory.createTextInputQuestion(quizNum, index)
                } else if (it == QuizQuestionType.SingleChoice) {
                    questions << QuizDefFactory.createChoiceQuestion(quizNum, index, 5, it)
                }
            }
            skillsService.createQuizQuestionDefs(questions)
            return quiz
        }

        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2, [QuizQuestionType.TextInput,QuizQuestionType.SingleChoice])
        def quiz3 = createQuiz(3, [QuizQuestionType.TextInput,QuizQuestionType.SingleChoice, QuizQuestionType.SingleChoice])
        def quiz4 = createQuiz(4, [QuizQuestionType.SingleChoice, QuizQuestionType.SingleChoice, QuizQuestionType.SingleChoice, QuizQuestionType.SingleChoice])
        def quiz5 = createQuiz(5)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(8, 1, 1, 100, 1)
        skills.each { it.selfReportingType = SkillDef.SelfReportingType.Quiz }
        skills[0].quizId = quiz1.quizId
        skills[1].quizId = quiz2.quizId
        skills[2].quizId = quiz3.quizId
        skills[3].quizId = quiz4.quizId
        skills[4].quizId = quiz5.quizId
        skills[5].quizId = quiz1.quizId
        skills[6].quizId = quiz2.quizId
        skills[7].quizId = quiz3.quizId
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(3)
        SkillsService user1 = createService(users[0])
        SkillsService user2 = createService(users[1])
        SkillsService user3 = createService(users[2])

        def runQuiz = { SkillsService user, def quiz, boolean grade, boolean pass ->
            def quizInfo = user.getQuizInfo(quiz.quizId)
            def quizAttempt = user.startQuizAttempt(quiz.quizId).body
            quizAttempt.questions.eachWithIndex { it, index ->
                int answerIndex = 0
                if (it.questionType == QuizQuestionType.SingleChoice.toString()) {
                    answerIndex = pass ? 0 : 1
                }
                user.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[index].answerOptions[answerIndex].id, [isSelected: true, answerText: 'This is user provided answer'])
            }

            user.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
            if (grade) {
                skillsService.gradeAnswer(user.userName, quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, pass, "Good answer")
            }

            return [quizInfo, quizAttempt]
        }

        def (quizInfo1, quizAttempt1) = runQuiz(user1, quiz1, true, true)
        def (quizInfo2, quizAttempt2) = runQuiz(user1, quiz2, true, false)
        def (quizInfo3, quizAttempt3) = runQuiz(user1, quiz3, false, true)
        def (quizInfo4, quizAttempt4) = runQuiz(user1, quiz4, false, true)
        def (quizInfo5, quizAttempt5) = runQuiz(user1, quiz5, false, true)

        runQuiz(user2, quiz2, true, false)
        runQuiz(user2, quiz2, true, false)
        def (quizInfo6, quizAttempt6) = runQuiz(user2, quiz2, false, true)

        runQuiz(user2, quiz3, true, false)
        runQuiz(user2, quiz3, true, false)
        def (quizInfo7, quizAttempt7) = runQuiz(user2, quiz3, true, true)

        runQuiz(user2, quiz5, true, false)
        runQuiz(user2, quiz5, true, false)
        def (quizInfo8, quizAttempt8) = runQuiz(user2, quiz5, true, false)

        when:
        def user1Res = skillsService.getSkillSummary(user1.userName, proj.projectId, subj.subjectId)
        def user2Res = skillsService.getSkillSummary(user2.userName, proj.projectId, subj.subjectId)
        def user3Res = skillsService.getSkillSummary(user3.userName, proj.projectId, subj.subjectId)
        println( JsonOutput.prettyPrint(JsonOutput.toJson(user2Res)))
        then:
        user1Res.skills[0].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user1Res.skills[0].selfReporting.quizId == quiz1.quizId
        user1Res.skills[0].selfReporting.quizName == quiz1.name
        user1Res.skills[0].selfReporting.numQuizQuestions == 1
        user1Res.skills[0].selfReporting.quizNeedsGrading == false
        user1Res.skills[0].selfReporting.quizOrSurveyPassed == true
        user1Res.skills[0].selfReporting.quizAttemptId == quizAttempt1.id
        Date completed = Date.from(Instant.parse(user1Res.skills[0].selfReporting.quizNeedsGradingAttemptDate))
        completed >= fiveMinutesAgo && completed <= fiveMinutesFromNow

        user1Res.skills[1].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user1Res.skills[1].selfReporting.quizId == quiz2.quizId
        user1Res.skills[1].selfReporting.quizName == quiz2.name
        user1Res.skills[1].selfReporting.numQuizQuestions == 2
        user1Res.skills[1].selfReporting.quizNeedsGrading == false
        user1Res.skills[1].selfReporting.quizOrSurveyPassed == false
        user1Res.skills[1].selfReporting.quizAttemptId == quizAttempt2.id

        user1Res.skills[2].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user1Res.skills[2].selfReporting.quizId == quiz3.quizId
        user1Res.skills[2].selfReporting.quizName == quiz3.name
        user1Res.skills[2].selfReporting.numQuizQuestions == 3
        user1Res.skills[2].selfReporting.quizNeedsGrading == true
        user1Res.skills[2].selfReporting.quizOrSurveyPassed == false
        user1Res.skills[2].selfReporting.quizAttemptId == quizAttempt3.id

        user1Res.skills[3].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user1Res.skills[3].selfReporting.quizId == quiz4.quizId
        user1Res.skills[3].selfReporting.quizName == quiz4.name
        user1Res.skills[3].selfReporting.numQuizQuestions == 4
        user1Res.skills[3].selfReporting.quizNeedsGrading == false
        user1Res.skills[3].selfReporting.quizOrSurveyPassed == false
        user1Res.skills[3].selfReporting.quizAttemptId == null // only loaded for quizzes with InputText questions

        user1Res.skills[4].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user1Res.skills[4].selfReporting.quizId == quiz5.quizId
        user1Res.skills[4].selfReporting.quizName == quiz5.name
        user1Res.skills[4].selfReporting.numQuizQuestions == 1
        user1Res.skills[4].selfReporting.quizNeedsGrading == true
        user1Res.skills[4].selfReporting.quizOrSurveyPassed == false
        user1Res.skills[4].selfReporting.quizAttemptId == quizAttempt5.id

        user1Res.skills[5].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user1Res.skills[5].selfReporting.quizId == quiz1.quizId
        user1Res.skills[5].selfReporting.quizName == quiz1.name
        user1Res.skills[5].selfReporting.numQuizQuestions == 1
        user1Res.skills[5].selfReporting.quizNeedsGrading == false
        user1Res.skills[5].selfReporting.quizOrSurveyPassed == true
        user1Res.skills[5].selfReporting.quizAttemptId == quizAttempt1.id
        user1Res.skills[5].selfReporting.quizNeedsGradingAttemptDate != null

        user1Res.skills[6].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user1Res.skills[6].selfReporting.quizId == quiz2.quizId
        user1Res.skills[6].selfReporting.quizName == quiz2.name
        user1Res.skills[6].selfReporting.numQuizQuestions == 2
        user1Res.skills[6].selfReporting.quizNeedsGrading == false
        user1Res.skills[6].selfReporting.quizOrSurveyPassed == false
        user1Res.skills[6].selfReporting.quizAttemptId == quizAttempt2.id

        user1Res.skills[7].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user1Res.skills[7].selfReporting.quizId == quiz3.quizId
        user1Res.skills[7].selfReporting.quizName == quiz3.name
        user1Res.skills[7].selfReporting.numQuizQuestions == 3
        user1Res.skills[7].selfReporting.quizNeedsGrading == true
        user1Res.skills[7].selfReporting.quizOrSurveyPassed == false
        user1Res.skills[7].selfReporting.quizAttemptId == quizAttempt3.id

        // user 2
        user2Res.skills[0].selfReporting.quizNeedsGrading == false
        user2Res.skills[0].selfReporting.quizOrSurveyPassed == false
        user2Res.skills[0].selfReporting.quizAttemptId == null

        user2Res.skills[1].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user2Res.skills[1].selfReporting.quizId == quiz2.quizId
        user2Res.skills[1].selfReporting.quizName == quiz2.name
        user2Res.skills[1].selfReporting.numQuizQuestions == 2
        user2Res.skills[1].selfReporting.quizNeedsGrading == true
        user2Res.skills[1].selfReporting.quizOrSurveyPassed == false
        user2Res.skills[1].selfReporting.quizAttemptId == quizAttempt6.id

        user2Res.skills[2].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user2Res.skills[2].selfReporting.quizId == quiz3.quizId
        user2Res.skills[2].selfReporting.quizName == quiz3.name
        user2Res.skills[2].selfReporting.numQuizQuestions == 3
        user2Res.skills[2].selfReporting.quizNeedsGrading == false
        user2Res.skills[2].selfReporting.quizOrSurveyPassed == true
        user2Res.skills[2].selfReporting.quizAttemptId == quizAttempt7.id

        user2Res.skills[3].selfReporting.quizNeedsGrading == false
        user2Res.skills[3].selfReporting.quizOrSurveyPassed == false
        user2Res.skills[3].selfReporting.quizAttemptId == null

        user2Res.skills[4].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user2Res.skills[4].selfReporting.quizId == quiz5.quizId
        user2Res.skills[4].selfReporting.quizName == quiz5.name
        user2Res.skills[4].selfReporting.numQuizQuestions == 1
        user2Res.skills[4].selfReporting.quizNeedsGrading == false
        user2Res.skills[4].selfReporting.quizOrSurveyPassed == false
        user2Res.skills[4].selfReporting.quizAttemptId == quizAttempt8.id

        user2Res.skills[5].selfReporting.quizNeedsGrading == false
        user2Res.skills[5].selfReporting.quizOrSurveyPassed == false
        user2Res.skills[5].selfReporting.quizAttemptId == null

        user2Res.skills[6].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user2Res.skills[6].selfReporting.quizId == quiz2.quizId
        user2Res.skills[6].selfReporting.quizName == quiz2.name
        user2Res.skills[6].selfReporting.numQuizQuestions == 2
        user2Res.skills[6].selfReporting.quizNeedsGrading == true
        user2Res.skills[6].selfReporting.quizOrSurveyPassed == false
        user2Res.skills[6].selfReporting.quizAttemptId == quizAttempt6.id

        user2Res.skills[7].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        user2Res.skills[7].selfReporting.quizId == quiz3.quizId
        user2Res.skills[7].selfReporting.quizName == quiz3.name
        user2Res.skills[7].selfReporting.numQuizQuestions == 3
        user2Res.skills[7].selfReporting.quizNeedsGrading == false
        user2Res.skills[7].selfReporting.quizOrSurveyPassed == true
        user2Res.skills[7].selfReporting.quizAttemptId == quizAttempt7.id

        // user 3
        user3Res.skills.each {
            it.selfReporting.quizNeedsGrading == false
            it.selfReporting.quizOrSurveyPassed == false
            it.selfReporting.quizAttemptId == null
        }
    }
}

