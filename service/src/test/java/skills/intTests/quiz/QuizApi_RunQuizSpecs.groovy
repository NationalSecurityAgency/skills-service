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

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.quizLoading.QuizSettings
import skills.services.quiz.QuizQuestionType
import skills.storage.model.SkillDef
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

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        then:
        gradedQuizAttempt.passed == true
        gradedQuizAttempt.numQuestionsGotWrong == 0
        gradedQuizAttempt.gradedQuestions.questionId == quizInfo.questions.id
        gradedQuizAttempt.gradedQuestions.isCorrect == [true, true]
        gradedQuizAttempt.gradedQuestions[0].selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        gradedQuizAttempt.gradedQuestions[1].selectedAnswerIds == [quizInfo.questions[1].answerOptions[0].id]
    }

    def "run quiz - fail quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        then:
        gradedQuizAttempt.passed == false
        gradedQuizAttempt.numQuestionsGotWrong == 1
        !gradedQuizAttempt.gradedQuestions
    }

    def "run quiz - fail quiz - graded questions are returned if there are no more attempts available"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MaxNumAttempts.setting, value: '2'],
        ])

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        // attempt2
        def quizAttempt2 =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt2.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt2.id, quizInfo.questions[1].answerOptions[1].id)
        def gradedQuizAttempt2 = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt2.id).body
        then:
        gradedQuizAttempt.passed == false
        gradedQuizAttempt.numQuestionsGotWrong == 1
        !gradedQuizAttempt.gradedQuestions

        //  more more attempts left
        gradedQuizAttempt2.passed == false
        gradedQuizAttempt2.numQuestionsGotWrong == 1
        gradedQuizAttempt2.gradedQuestions.questionId == quizInfo.questions.id
        gradedQuizAttempt2.gradedQuestions.isCorrect == [true, false]
        gradedQuizAttempt2.gradedQuestions[0].selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        gradedQuizAttempt2.gradedQuestions[1].selectedAnswerIds == [quizInfo.questions[1].answerOptions[1].id]
    }

    def "answer is updated when reporting a different answer for a single-choice answer"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)

        when:
        def quizAttemptBeforeApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id)

        def quizAttemptAfterApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        then:
        quizAttemptBeforeApdate.selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        quizAttemptAfterApdate.selectedAnswerIds == [quizInfo.questions[0].answerOptions[1].id]
    }

    def "answer is added when reporting a different answer for a multiple-choice answer"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 4, QuizQuestionType.MultipleChoice)
        questions[0].answers[2].isCorrect = true
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)

        when:
        def quizAttemptBeforeApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id)

        def quizAttemptAfterApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        then:
        quizAttemptBeforeApdate.selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        quizAttemptAfterApdate.selectedAnswerIds.sort() == [quizInfo.questions[0].answerOptions[0].id, quizInfo.questions[0].answerOptions[1].id].sort()
    }

    def "answer is removed when reporting same answer for a multiple-choice answer with isSelected=false"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 4, QuizQuestionType.MultipleChoice)
        questions[0].answers[2].isCorrect = true
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[2].id)

        when:
        def quizAttemptBeforeApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id, [isSelected: false])

        def quizAttemptAfterApdate =  skillsService.startQuizAttempt(quiz.quizId).body

        then:
        quizAttemptBeforeApdate.selectedAnswerIds.sort() == [quizInfo.questions[0].answerOptions[0].id, quizInfo.questions[0].answerOptions[1].id, quizInfo.questions[0].answerOptions[2].id].sort()
        quizAttemptAfterApdate.selectedAnswerIds.sort() == [quizInfo.questions[0].answerOptions[0].id, quizInfo.questions[0].answerOptions[2].id].sort()
    }

    def "removing quiz definition removes questions and answers definitions and attempts"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[0].id)
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

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[0].id)
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

    def "configured question sort oder is respected"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)
        when:

        def quizInfo0 = skillsService.getQuizInfo(quiz.quizId)
        skillsService.changeQuizQuestionDisplayOrder(quiz.quizId, qRes.questions[2].id, 4)
        def quizInfo1 = skillsService.getQuizInfo(quiz.quizId)
        skillsService.changeQuizQuestionDisplayOrder(quiz.quizId, qRes.questions[1].id, 0)
        def quizInfo2 = skillsService.getQuizInfo(quiz.quizId)

        then:
        quizInfo0.questions.id == [qRes.questions[0].id, qRes.questions[1].id, qRes.questions[2].id, qRes.questions[3].id, qRes.questions[4].id]
        quizInfo1.questions.id == [qRes.questions[0].id, qRes.questions[1].id, qRes.questions[3].id, qRes.questions[4].id, qRes.questions[2].id]
        quizInfo2.questions.id == [qRes.questions[1].id, qRes.questions[0].id, qRes.questions[3].id, qRes.questions[4].id, qRes.questions[2].id]
    }
}
