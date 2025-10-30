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
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.services.StartDateUtil
import skills.services.WeekNumberUtil
import skills.services.quiz.QuizQuestionType
import skills.storage.model.EventType
import skills.storage.model.QuizAnswerDef
import skills.storage.model.QuizQuestionDef
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserEvent
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints
import skills.storage.model.UserQuizAttempt
import skills.storage.model.UserQuizQuestionAttempt
import skills.storage.repos.*

import static skills.intTests.utils.SkillsFactory.*

class QuizApi_RunQuizSpecs extends DefaultIntSpec {

    @Autowired
    QuizQuestionDefRepo quizQuestionDefRepo

    @Autowired
    QuizAnswerDefRepo quizAnswerDefRepo

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    @Autowired
    UserQuizQuestionAttemptRepo userQuizQuestionAttemptRepo

    @Autowired
    UserQuizAnswerAttemptRepo userQuizAnswerAttemptRepo

    def "run quiz - pass"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        then:
        gradedQuizAttempt.passed == true
        gradedQuizAttempt.numQuestionsGotWrong == 0
        gradedQuizAttempt.gradedQuestions.questionId == quizAttempt.questions.id
        gradedQuizAttempt.gradedQuestions.isCorrect == [true, true]
        gradedQuizAttempt.gradedQuestions[0].selectedAnswerIds == [quizAttempt.questions[0].answerOptions[0].id]
        gradedQuizAttempt.gradedQuestions[1].selectedAnswerIds == [quizAttempt.questions[1].answerOptions[0].id]
    }

    def "run quiz - fail quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        then:
        gradedQuizAttempt.passed == false
        gradedQuizAttempt.numQuestionsGotWrong == 1
        !gradedQuizAttempt.gradedQuestions
    }

    def "run quiz - fail quiz - graded questions are not returned"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: '2'],
        ])

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        // attempt2
        def quizAttempt2 =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt2.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt2.id, quizAttempt.questions[1].answerOptions[1].id)
        def gradedQuizAttempt2 = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt2.id).body
        then:
        gradedQuizAttempt.passed == false
        gradedQuizAttempt.numQuestionsGotWrong == 1
        !gradedQuizAttempt.gradedQuestions

        //  more more attempts left
        gradedQuizAttempt2.passed == false
        gradedQuizAttempt2.numQuestionsGotWrong == 1
        !gradedQuizAttempt2.gradedQuestions
    }

    def "answer is updated when reporting a different answer for a single-choice answer"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)

        when:
        def quizAttemptBeforeApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)

        def quizAttemptAfterApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        then:
        quizAttemptBeforeApdate.selectedAnswerIds == [quizAttempt.questions[0].answerOptions[0].id]
        quizAttemptAfterApdate.selectedAnswerIds == [quizAttempt.questions[0].answerOptions[1].id]
    }

    def "answer is added when reporting a different answer for a multiple-choice answer"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 4, QuizQuestionType.MultipleChoice)
        questions[0].answers[2].isCorrect = true
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)

        when:
        def quizAttemptBeforeApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)

        def quizAttemptAfterApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        then:
        quizAttemptBeforeApdate.selectedAnswerIds == [quizAttempt.questions[0].answerOptions[0].id]
        quizAttemptAfterApdate.selectedAnswerIds.sort() == [quizAttempt.questions[0].answerOptions[0].id, quizAttempt.questions[0].answerOptions[1].id].sort()
    }

    def "answer is removed when reporting same answer for a multiple-choice answer with isSelected=false"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 4, QuizQuestionType.MultipleChoice)
        questions[0].answers[2].isCorrect = true
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[2].id)

        when:
        def quizAttemptBeforeApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id, [isSelected: false])

        def quizAttemptAfterApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        then:
        quizAttemptBeforeApdate.selectedAnswerIds.sort() == [quizAttempt.questions[0].answerOptions[0].id, quizAttempt.questions[0].answerOptions[1].id, quizAttempt.questions[0].answerOptions[2].id].sort()
        quizAttemptAfterApdate.selectedAnswerIds.sort() == [quizAttempt.questions[0].answerOptions[0].id, quizAttempt.questions[0].answerOptions[2].id].sort()
    }

    def "removing quiz definition removes questions and answers definitions and attempts"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert gradedQuizAttempt.gradedQuestions

        when:
        quizDefRepo.deleteAll()
        then:
        quizQuestionDefRepo.findAll() == []
        quizAnswerDefRepo.findAll() == []
        userQuizAttemptRepo.findAll() == []
        userQuizQuestionAttemptRepo.findAll() == []
        userQuizAnswerAttemptRepo.findAll() == []
    }

    def "passing quiz attempt gives skill credit"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.quizId
        skillsService.createSkill(skillWithQuiz)

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        def skillRes = skillsService.getSingleSkillSummary(skillsService.userName, proj.projectId, skillWithQuiz.skillId)
        then:
        gradedQuizAttempt.passed == true
        gradedQuizAttempt.associatedSkillResults.pointsEarned == [skillRes.totalPoints]
        gradedQuizAttempt.associatedSkillResults.skillApplied == [true]

        skillRes.points ==  skillRes.totalPoints
    }

    def "quiz must have at least 1 questions to start"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        when:
        skillsService.startQuizAttempt(quiz.quizId)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Must have at least 1 question declared in order to start.")
    }

    def "configured question sort order is respected"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)

        List<SkillsService> users = getRandomUsers(3).collect { createService(it) }
        when:

        def quizInfo0 = users[0].startQuizAttempt(quiz.quizId).body
        skillsService.changeQuizQuestionDisplayOrder(quiz.quizId, qRes.questions[2].id, 4)
        def quizInfo1 = users[1].startQuizAttempt(quiz.quizId).body
        skillsService.changeQuizQuestionDisplayOrder(quiz.quizId, qRes.questions[1].id, 0)
        def quizInfo2 = users[2].startQuizAttempt(quiz.quizId).body

        then:
        quizInfo0.questions.id == [qRes.questions[0].id, qRes.questions[1].id, qRes.questions[2].id, qRes.questions[3].id, qRes.questions[4].id]
        quizInfo1.questions.id == [qRes.questions[0].id, qRes.questions[1].id, qRes.questions[3].id, qRes.questions[4].id, qRes.questions[2].id]
        quizInfo2.questions.id == [qRes.questions[1].id, qRes.questions[0].id, qRes.questions[3].id, qRes.questions[4].id, qRes.questions[2].id]
    }

    def "Quiz using a subset of the questions in random order persists order"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 100, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.RandomizeQuestions.setting, value: 'true'],
                [setting: QuizSettings.QuizLength.setting, value: 10],
        ])

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        when:
        def quizInfoAfterStart = skillsService.startQuizAttempt(quiz.quizId).body
        def quizInfoSecondStart = skillsService.startQuizAttempt(quiz.quizId).body
        def quizInfoThirdStart = skillsService.startQuizAttempt(quiz.quizId).body

        then:
        quizInfo.quizLength == 10
        quizInfoAfterStart.questions == quizInfoSecondStart.questions
        quizInfoAfterStart.questions == quizInfoThirdStart.questions
    }

    def "in case of QuizLength setting only allow to report answers for the questions of this attempt"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        List questions = (1..2).collect {QuizDefFactory.createChoiceQuestion(1, it) }
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.QuizLength.setting, value: 1],
        ])

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body

        List<String> attemptQuestionIds = quizAttempt.questions.collect { it.id }
        List<QuizQuestionDef> allQuestionDefs = quizQuestionDefRepo.findAllByQuizIdIgnoreCase(quiz.quizId)

        QuizQuestionDef questionNotInAttempt = allQuestionDefs.find { !attemptQuestionIds.contains(it.id) }
        List<QuizAnswerDef> answersNotInAttempt = quizAnswerDefRepo.findAllByQuestionRefId(questionNotInAttempt.id)

        when:
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, answersNotInAttempt[0].id)
        then:
        SkillsClientException quizException = thrown()
        quizException.message.contains("Provided answer id [${answersNotInAttempt[0].id}] does not exist for [${quizAttempt.id}] quiz attempt")
    }

    def "Users have different quiz run attempts"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 100, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.RandomizeQuestions.setting, value: 'true'],
                [setting: QuizSettings.QuizLength.setting, value: 10],
        ])

        def quizInfoUser1 = skillsService.getQuizInfo(quiz.quizId, 'user1')
        def quizInfoUser2 = skillsService.getQuizInfo(quiz.quizId, 'user2')

        when:
        def quizInfoAfterStartUser1 = skillsService.startQuizAttempt(quiz.quizId, 'user1').body
        def quizInfoAfterStartUser2 = skillsService.startQuizAttempt(quiz.quizId, 'user2').body

        then:
        quizInfoUser1.quizLength == 10
        quizInfoUser2.quizLength == 10
        quizInfoAfterStartUser1.questions != quizInfoAfterStartUser2.questions
    }

    def "Can fail a quiz via the endpoint"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 10, 2)
        skillsService.createQuizQuestionDefs(questions)

        def firstQuizAttempt = skillsService.startQuizAttempt(quiz.quizId, 'user1').body
        def initialQuizInfo = skillsService.getQuizInfo(quiz.quizId, 'user1')
        assert initialQuizInfo.isAttemptAlreadyInProgress == true
        assert initialQuizInfo.userNumPreviousQuizAttempts == 0
        assert initialQuizInfo.userQuizPassed == false

        when:
        skillsService.failQuizAttempt(quiz.quizId, firstQuizAttempt.id,'user1')

        then:
        def failedQuizInfo = skillsService.getQuizInfo(quiz.quizId, 'user1')
        assert failedQuizInfo.isAttemptAlreadyInProgress == false
        assert failedQuizInfo.userNumPreviousQuizAttempts == 1
        assert failedQuizInfo.userQuizPassed == false

    }

    def "Can not submit answers to a failed quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 10, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.failQuizAttempt(quiz.quizId, quizAttempt.id)

        when:
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)

        then:
        SkillsClientException quizException = thrown()
        quizException.message.contains("Provided attempt id [${quizAttempt.id}], which corresponds to a failed quiz")
    }

    def "Can not submit a completed attempt to a failed quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 10, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.failQuizAttempt(quiz.quizId, quizAttempt.id)

        when:
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id)

        then:
        SkillsClientException quizException = thrown()
        quizException.message.contains("Provided attempt id [${quizAttempt.id}], which corresponds to a failed quiz")
    }

    def "Can not submit answers to a quiz after the deadline"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 10, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.QuizTimeLimit.setting, value: 1],
        ])

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        Thread.sleep(1500)

        when:
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)

        then:
        SkillsClientException quizException = thrown()
        quizException.message.contains("Deadline for [${quizAttempt.id}] has expired")
    }

    def "Can not submit a completed attempt after the deadline"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 10, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.QuizTimeLimit.setting, value: 1],
        ])

        def quizAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        Thread.sleep(1500)

        when:
        skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id)

        then:
        SkillsClientException quizException = thrown()
        quizException.message.contains("Deadline for [${quizAttempt.id}] has expired")
    }

    def "allow word NULL in as quiz answer"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        questions[0].answers[0].answer = "Null"
        questions[1].answers[0].answer = "NULL"
        skillsService.createQuizQuestionDefs(questions)

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        then:
        gradedQuizAttempt.passed == true
        gradedQuizAttempt.numQuestionsGotWrong == 0
        gradedQuizAttempt.gradedQuestions.questionId == quizAttempt.questions.id
        gradedQuizAttempt.gradedQuestions.isCorrect == [true, true]
        gradedQuizAttempt.gradedQuestions[0].selectedAnswerIds == [quizAttempt.questions[0].answerOptions[0].id]
        gradedQuizAttempt.gradedQuestions[1].selectedAnswerIds == [quizAttempt.questions[1].answerOptions[0].id]
    }

    def "quiz can not be taken if the project does not have enough points"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = createSkills(1, 1, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkill(skills[0])

        when:
        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        then:
        quizInfo
        quizInfo.canStartQuiz == false
        quizInfo.errorMessage == "This Quiz is assigned to a Skill (skill1) that does not have enough points to be completed. The Project (TestProject1) that contains this skill must have at least 100 points."
    }

    def "quiz can not be taken if the subject does not have enough points"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def subj2 = createSubject(1, 2)

        def skills = createSkills(1, 1, 1, 1)
        def otherSkills = createSkills(1, 1, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skillsService.createSubject(subj2)
        skillsService.createSkill(otherSkills[0])

        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkill(skills[0])

        when:
        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        then:
        quizInfo
        quizInfo.canStartQuiz == false
        quizInfo.errorMessage == "This Quiz is assigned to a Skill (skill1) that does not have enough points to be completed. The Subject (TestSubject1) that contains this skill must have at least 100 points."
    }

    def "run quiz with matching question - pass"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createMatchingQuestion(1, 2, 2)
        skillsService.createQuizQuestionDef(question)

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [answerText: 'value1'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id, [answerText: 'value2'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        then:
        gradedQuizAttempt.passed == true
        gradedQuizAttempt.numQuestionsGotWrong == 0
        gradedQuizAttempt.gradedQuestions.questionId == quizAttempt.questions.id
        gradedQuizAttempt.gradedQuestions.isCorrect == [true]
        gradedQuizAttempt.gradedQuestions[0].selectedAnswerIds == [quizAttempt.questions[0].answerOptions[0].id, quizAttempt.questions[0].answerOptions[1].id]
    }

    def "run quiz with matching question - fail"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createMatchingQuestion(1, 2, 2)
        skillsService.createQuizQuestionDef(question)

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [answerText: 'value2'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id, [answerText: 'value1'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        then:
        gradedQuizAttempt.passed == false
        gradedQuizAttempt.numQuestionsGotWrong == 1
        !gradedQuizAttempt.gradedQuestions
    }

    def "run quiz with matching question - fail - return graded question"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createMatchingQuestion(1, 2, 5)
        skillsService.createQuizQuestionDef(question)
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.AlwaysShowCorrectAnswers.setting, value: Boolean.TRUE.toString()],
        ])

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [answerText: 'value2'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id, [answerText: 'value1'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[2].id, [answerText: 'value3'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[3].id, [answerText: 'value4'])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[4].id, [answerText: 'value5'])
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        println JsonOutput.prettyPrint(JsonOutput.toJson(gradedQuizAttempt))

        then:
        gradedQuizAttempt.passed == false
        gradedQuizAttempt.numQuestionsGotWrong == 1
        gradedQuizAttempt.gradedQuestions.size() == 1
        gradedQuizAttempt.gradedQuestions[0].selectedAnswerIds == [
                quizAttempt.questions[0].answerOptions[0].id,
                quizAttempt.questions[0].answerOptions[1].id,
                quizAttempt.questions[0].answerOptions[2].id,
                quizAttempt.questions[0].answerOptions[3].id,
                quizAttempt.questions[0].answerOptions[4].id,]
        gradedQuizAttempt.gradedQuestions[0].isCorrect == false
        gradedQuizAttempt.gradedQuestions[0].correctAnswerIds == [
                quizAttempt.questions[0].answerOptions[2].id,
                quizAttempt.questions[0].answerOptions[3].id,
                quizAttempt.questions[0].answerOptions[4].id
        ]
    }
}
