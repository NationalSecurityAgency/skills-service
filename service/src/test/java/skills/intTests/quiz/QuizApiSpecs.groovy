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
import skills.storage.repos.QuizAnswerDefRepo
import skills.storage.repos.QuizQuestionDefRepo
import skills.storage.repos.UserQuizAnswerAttemptRepo
import skills.storage.repos.UserQuizAttemptRepo
import skills.storage.repos.UserQuizQuestionAttemptRepo
import spock.lang.IgnoreRest

class QuizApiSpecs extends DefaultIntSpec {

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

    def "change quiz questions display order"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 5, 2)
        questions[1].answers[0].isCorrect = true
        questions[1].answers[1].isCorrect = true
        skillsService.createQuizQuestionDefs(questions)

        when:
        def qRes = skillsService.getQuizInfo(quiz.quizId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(qRes))
        then:
        qRes.name == quiz.name
    }

    def "report quiz attempt - pass quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[0].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        println JsonOutput.prettyPrint(JsonOutput.toJson(gradedQuizAttempt))
        then:
        gradedQuizAttempt.passed == true
        gradedQuizAttempt.gradedQuestions.questionId == quizInfo.questions.id
        gradedQuizAttempt.gradedQuestions.isCorrect == [true, true]
        gradedQuizAttempt.gradedQuestions[0].selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        gradedQuizAttempt.gradedQuestions[1].selectedAnswerIds == [quizInfo.questions[1].answerOptions[0].id]
    }

    def "report quiz attempt - fail quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        then:
        gradedQuizAttempt.passed == false
        gradedQuizAttempt.gradedQuestions.questionId == quizInfo.questions.id
        gradedQuizAttempt.gradedQuestions.isCorrect == [true, false]
        gradedQuizAttempt.gradedQuestions[0].selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        gradedQuizAttempt.gradedQuestions[1].selectedAnswerIds == [quizInfo.questions[1].answerOptions[1].id]
    }

    def "report quiz attempt - failed attempt followed by passed attempt"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        def quizAttempt1 =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt1.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt1.id, quizInfo.questions[1].answerOptions[0].id)
        def gradedQuizAttempt1 = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt1.id).body

        then:
        gradedQuizAttempt.passed == false
        gradedQuizAttempt.gradedQuestions.questionId == quizInfo.questions.id
        gradedQuizAttempt.gradedQuestions.isCorrect == [true, false]
        gradedQuizAttempt.gradedQuestions[0].selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        gradedQuizAttempt.gradedQuestions[1].selectedAnswerIds == [quizInfo.questions[1].answerOptions[1].id]

        gradedQuizAttempt1.passed == true
        gradedQuizAttempt1.gradedQuestions.questionId == quizInfo.questions.id
        gradedQuizAttempt1.gradedQuestions.isCorrect == [true, true]
        gradedQuizAttempt1.gradedQuestions[0].selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        gradedQuizAttempt1.gradedQuestions[1].selectedAnswerIds == [quizInfo.questions[1].answerOptions[0].id]
    }

    def "answer is updated when reporting a different answer for a single-choice answer"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)

        when:
        def quizAttemptBeforeApdate =  skillsService.startQuizAttempt(quiz.quizId).body
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizAttemptBeforeApdate))

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id)

        def quizAttemptAfterApdate =  skillsService.startQuizAttempt(quiz.quizId).body
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizAttemptAfterApdate))

        then:
        quizAttemptBeforeApdate.selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        quizAttemptAfterApdate.selectedAnswerIds == [quizInfo.questions[0].answerOptions[1].id]
    }

    def "answer is added when reporting a different answer for a multiple-choice answer"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 4)
        questions[0].answers[2].isCorrect = true
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)

        when:
        def quizAttemptBeforeApdate =  skillsService.startQuizAttempt(quiz.quizId).body
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizAttemptBeforeApdate))

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id)

        def quizAttemptAfterApdate =  skillsService.startQuizAttempt(quiz.quizId).body
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizAttemptAfterApdate))

        then:
        quizAttemptBeforeApdate.selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id]
        quizAttemptAfterApdate.selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id, quizInfo.questions[0].answerOptions[1].id]
    }

    def "answer is removed when reporting same answer for a multiple-choice answer with isSelected=false"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 4)
        questions[0].answers[2].isCorrect = true
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[2].id)

        when:
        def quizAttemptBeforeApdate =  skillsService.startQuizAttempt(quiz.quizId).body
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizAttemptBeforeApdate))

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id, [isSelected: false])

        def quizAttemptAfterApdate =  skillsService.startQuizAttempt(quiz.quizId).body
        println JsonOutput.prettyPrint(JsonOutput.toJson(quizAttemptAfterApdate))

        then:
        quizAttemptBeforeApdate.selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id, quizInfo.questions[0].answerOptions[1].id, quizInfo.questions[0].answerOptions[2].id]
        quizAttemptAfterApdate.selectedAnswerIds == [quizInfo.questions[0].answerOptions[0].id, quizInfo.questions[0].answerOptions[2].id]
    }

    def "previous quiz attempts do not affect follow-on attempts"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[1].id)
        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        when:
        def quizAttempt1_t0 =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt1_t0.id, quizInfo.questions[0].answerOptions[1].id)
        def quizAttempt1_t1 =  skillsService.startQuizAttempt(quiz.quizId).body
        then:
        quizAttempt1_t0.id != quizAttempt.id
        !quizAttempt1_t0.selectedAnswerIds
        quizAttempt1_t1.selectedAnswerIds == [quizInfo.questions[0].answerOptions[1].id]
        quizAttempt1_t0.id == quizAttempt1_t1.id
        // make sure old runs answer were not removed
        userQuizAnswerAttemptRepo.findAll().findAll({ it.userQuizAttemptRefId == quizAttempt.id }).collect { it.quizAnswerDefinitionRefId } == [quizInfo.questions[0].answerOptions[0].id, quizInfo.questions[1].answerOptions[1].id]
    }

    def "removing quiz definition removes questions and answers definitions and attempts"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 2, 2)
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
}
