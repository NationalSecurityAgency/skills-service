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
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.services.quiz.QuizQuestionType
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkill
import static skills.intTests.utils.SkillsFactory.createSubject

@Slf4j
class QuizDefManagementSpecs extends DefaultIntSpec {

    def "no quiz definitions"() {
        when:
        def quizDefs = skillsService.getQuizDefs()

        then:
        !quizDefs
    }

    def "create quiz definition"() {
        def quiz = QuizDefFactory.createQuiz(1)

        when:
        def newQuiz = skillsService.createQuizDef(quiz)

        def quizDefs = skillsService.getQuizDefs()
        def quizDef = skillsService.getQuizDef(quiz.quizId)
        def quizDefSummary = skillsService.getQuizDefSummary(quiz.quizId)

        then:
        newQuiz.body.quizId == quiz.quizId
        newQuiz.body.name == quiz.name

        quizDefs.quizId == [quiz.quizId]
        quizDefs.name == [quiz.name]

        quizDef.quizId == quiz.quizId
        quizDef.name == quiz.name
        quizDef.type == quiz.type

        quizDefSummary.quizId == quiz.quizId
        quizDefSummary.name == quiz.name
        quizDefSummary.type == quiz.type
        quizDefSummary.numQuestions == 0
    }

    def "remove quiz definition"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz1)
        skillsService.createQuizDef(quiz2)

        when:
        def quizDefs = skillsService.getQuizDefs()
        skillsService.removeQuizDef(quiz1.quizId)
        def quizDefsAfter = skillsService.getQuizDefs()
        then:
        quizDefs.quizId == [quiz2.quizId, quiz1.quizId]
        quizDefsAfter.quizId == [quiz2.quizId]
    }

    def "removing quiz definition is not allowed if it's associated to a skill"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.quizId
        skillsService.createSkill(skillWithQuiz)

        when:
        skillsService.removeQuizDef(quiz.quizId)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Not allowed to remove quiz when assigned to at least 1 skill")
    }

    def "update quiz definition name and description"() {
        def quiz1 = QuizDefFactory.createQuiz(1, "Import Desc 1")
        def quiz2 = QuizDefFactory.createQuiz(2, "Import Desc 2")
        skillsService.createQuizDef(quiz1)
        skillsService.createQuizDef(quiz2)

        when:
        def quizDefs = skillsService.getQuizDefs()
        String originalQuiz2Name = quiz2.name;
        quiz2.name = "Cool New Name"
        quiz2.description = "Important Update"
        skillsService.createQuizDef(quiz2, quiz2.quizId)
        def quizDefsAfter = skillsService.getQuizDefs()

        then:
        quizDefs.quizId == [quiz2.quizId, quiz1.quizId]
        quizDefs.name == [originalQuiz2Name, quiz1.name]

        quizDefsAfter.quizId == [quiz2.quizId, quiz1.quizId]
        quizDefsAfter.name == ["Cool New Name", quiz1.name]
    }

    def "update quiz definition id"() {
        def quiz1 = QuizDefFactory.createQuiz(1, "Import Desc 1")
        def quiz2 = QuizDefFactory.createQuiz(2, "Import Desc 2")
        skillsService.createQuizDef(quiz1)
        skillsService.createQuizDef(quiz2)

        when:
        def quizDefs = skillsService.getQuizDefs()
        String originalQuizId = quiz2.quizId
        quiz2.quizId = "newid"
        skillsService.createQuizDef(quiz2, originalQuizId)
        def quizDefsAfter = skillsService.getQuizDefs()
        then:
        quizDefs.quizId == [originalQuizId, quiz1.quizId]
        quizDefsAfter.quizId == ["newid", quiz1.quizId]
    }

    def "quiz id exist"() {
        def quiz1 = QuizDefFactory.createQuiz(1, "Import Desc 1")
        def quiz2 = QuizDefFactory.createQuiz(2, "Import Desc 2")
        skillsService.createQuizDef(quiz1)
        skillsService.createQuizDef(quiz2)


        expect:
        Boolean.valueOf(skillsService.quizIdExist(quizId).body) == quizIdExist

        where:
        quizId        | quizIdExist
        "TestQuiz1"   | true
        "tEsTqUiz1"   | true
        " TestQuiz1 " | true
        "TestQuiz"    | false
        "TestQuiz2"   | true
        "TestQuiz3"   | false
    }

    def "quiz name exist"() {
        def quiz1 = QuizDefFactory.createQuiz(1, "Import Desc 1")
        def quiz2 = QuizDefFactory.createQuiz(2, "Import Desc 2")
        skillsService.createQuizDef(quiz1)
        skillsService.createQuizDef(quiz2)


        expect:
        Boolean.valueOf(skillsService.quizNameExist(quizName).body) == quizNmeExist

        where:
        quizName         | quizNmeExist
        "Test Quiz #1"   | true
        "tESt qUiz #1"   | true
        " Test Quiz #1 " | true
        "Test Quiz #"    | false
        "Test Quiz #2"   | true
        "Test Quiz #3"   | false
    }

    def "add MultipleChoice question to quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        def newQuiz = skillsService.createQuizDef(quiz)

        def question = QuizDefFactory.createChoiceQuestion(1, 1, 3, QuizQuestionType.MultipleChoice)

        when:
        def newQuestion = skillsService.createQuizQuestionDef(question)

        then:
        newQuestion.body.id
        newQuestion.body.question == question.question
        newQuestion.body.answerHint == question.answerHint
        newQuestion.body.questionType == QuizQuestionType.MultipleChoice.toString()
        newQuestion.body.answers.size() == 3
        newQuestion.body.answers[0].id
        newQuestion.body.answers[1].id
        newQuestion.body.answers[2].id
        newQuestion.body.answers.answer == question.answers.answer
        newQuestion.body.answers.isCorrect == [true, false, true]
    }

    def "add SingleChoice question to quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        def newQuiz = skillsService.createQuizDef(quiz)

        def question = QuizDefFactory.createChoiceQuestion(1, 1, 2, QuizQuestionType.SingleChoice)

        when:
        def newQuestion = skillsService.createQuizQuestionDef(question)

        then:
        newQuestion.body.id
        newQuestion.body.question == question.question
        newQuestion.body.answerHint == question.answerHint
        newQuestion.body.questionType == QuizQuestionType.SingleChoice.toString()
        newQuestion.body.answers.size() == 2
        newQuestion.body.answers[0].id
        newQuestion.body.answers[1].id
        newQuestion.body.answers.answer == question.answers.answer
        newQuestion.body.answers.isCorrect == [true, false]
    }

    def "get quiz questions"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question1 = QuizDefFactory.createChoiceQuestion(1, 1, 2)
        skillsService.createQuizQuestionDef(question1)
        def question2 = QuizDefFactory.createChoiceQuestion(1, 2, 3, QuizQuestionType.MultipleChoice)
        question2.answers[1].isCorrect = true
        skillsService.createQuizQuestionDef(question2)

        when:
        def res = skillsService.getQuizQuestionDefs(quiz.quizId)
        def questions = res.questions
        then:
        res.quizType == 'Quiz'
        questions.size() == 2
        questions[0].id
        questions[1].id
        questions.question == [question1.question, question2.question]
        questions.answerHint == [question1.answerHint, question2.answerHint]
        questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]

        questions[0].answers[0].id
        questions[0].answers[1].id
        questions[0].answers.answer == question1.answers.answer
        questions[0].answers.isCorrect == question1.answers.isCorrect

        questions[1].answers[0].id
        questions[1].answers[1].id
        questions[1].answers[2].id
        questions[1].answers.answer == question2.answers.answer
        questions[1].answers.isCorrect == question2.answers.isCorrect
    }

    def "get single quiz question"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question1 = QuizDefFactory.createChoiceQuestion(1, 1, 2)
        skillsService.createQuizQuestionDef(question1)
        def question2 = QuizDefFactory.createChoiceQuestion(1, 2, 3, QuizQuestionType.MultipleChoice)
        question2.answers[1].isCorrect = true
        skillsService.createQuizQuestionDef(question2)

        when:
        def res = skillsService.getQuizQuestionDefs(quiz.quizId, )
        def questions = res.questions.collect {
            skillsService.getQuizQuestionDef(quiz.quizId, it.id)
        }
        then:
        res.quizType == 'Quiz'
        questions.size() == 2
        questions[0].id
        questions[1].id
        questions.question == [question1.question, question2.question]
        questions.answerHint == [question1.answerHint, question2.answerHint]
        questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]

        questions[0].answers[0].id
        questions[0].answers[1].id
        questions[0].answers.answer == question1.answers.answer
        questions[0].answers.isCorrect == question1.answers.isCorrect

        questions[1].answers[0].id
        questions[1].answers[1].id
        questions[1].answers[2].id
        questions[1].answers.answer == question2.answers.answer
        questions[1].answers.isCorrect == question2.answers.isCorrect
    }

    def "change quiz questions display order"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        def qRes = skillsService.getQuizQuestionDefs(quiz.quizId)

        skillsService.changeQuizQuestionDisplayOrder(quiz.quizId, qRes.questions[2].id, 4)
        def qRes1 = skillsService.getQuizQuestionDefs(quiz.quizId)

        skillsService.changeQuizQuestionDisplayOrder(quiz.quizId, qRes.questions[1].id, 0)
        def qRes2 = skillsService.getQuizQuestionDefs(quiz.quizId)

        then:
        qRes.questions.displayOrder == [0, 1, 2, 3, 4]

        qRes1.questions.id == [qRes.questions[0].id, qRes.questions[1].id, qRes.questions[3].id, qRes.questions[4].id, qRes.questions[2].id]
        qRes1.questions.displayOrder == [0, 1, 2, 3, 4]

        qRes2.questions.id == [qRes.questions[1].id, qRes.questions[0].id, qRes.questions[3].id, qRes.questions[4].id, qRes.questions[2].id]
        qRes2.questions.displayOrder == [0, 1, 2, 3, 4]
    }

    def "get quiz definition summary"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        when:
        def quizDef = skillsService.getQuizDef(quiz.quizId)
        def quizDefSummary = skillsService.getQuizDefSummary(quiz.quizId)

        then:
        quizDef.quizId == quiz.quizId
        quizDef.name == quiz.name
        quizDef.type == quiz.type

        quizDefSummary.quizId == quiz.quizId
        quizDefSummary.name == quiz.name
        quizDefSummary.type == quiz.type
        quizDefSummary.numQuestions == 5
    }

    def "update question definition - change answers text and correct selection"() {
        def quiz = QuizDefFactory.createQuiz(1)
        def newQuiz = skillsService.createQuizDef(quiz)
        def questionDef = QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice)
        def q1 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 3, QuizQuestionType.SingleChoice)).body
        def q2 = skillsService.createQuizQuestionDef(questionDef).body
        q2.question = "New Cool Questions?"
        q2.answerHint = "New Cool Questions Hint"
        q2.answers[0].answer = "New answer 1"
        q2.answers[2].answer = "New answer 3"
        q2.answers[0].isCorrect = false
        q2.answers[1].isCorrect = true
        q2.answers[2].isCorrect = false
        q2.answers[3].isCorrect = false
        q2.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)
        def updatedQuestions = skillsService.getQuizQuestionDefs(quiz.quizId)

        then:
        updatedQuestion.question == "New Cool Questions?"
        updatedQuestion.answerHint == "New Cool Questions Hint"
        updatedQuestion.questionType == QuizQuestionType.SingleChoice.toString()
        updatedQuestion.answers.answer == ["New answer 1", "Answer #2", "New answer 3", "Answer #4"]
        updatedQuestion.answers.isCorrect == [false, true, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4]

        updatedQuestions.questions.question == ["This is questions #1", "New Cool Questions?"]
        updatedQuestions.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.SingleChoice.toString()]
        updatedQuestions.questions[1].answers.answer == ["New answer 1", "Answer #2", "New answer 3", "Answer #4"]
        updatedQuestions.questions[1].answers.isCorrect == [false, true, false, false]
        updatedQuestions.questions[1].answers.displayOrder == [1, 2, 3, 4]
    }

    def "update question definition - change correct answers and question type - SingleChoice -> MultipleChoice"() {
        def quiz = QuizDefFactory.createQuiz(1)
        def newQuiz = skillsService.createQuizDef(quiz)
        def questionDef = QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice)
        def q1 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 3, QuizQuestionType.SingleChoice)).body
        def q2 = skillsService.createQuizQuestionDef(questionDef).body
        q2.questionType = QuizQuestionType.MultipleChoice.toString()
        q2.answers[0].isCorrect = false
        q2.answers[1].isCorrect = false
        q2.answers[2].isCorrect = true
        q2.answers[3].isCorrect = true
        q2.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)
        def updatedQuestions = skillsService.getQuizQuestionDefs(quiz.quizId)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.answerHint ==  "This is a hint for question #2"
        updatedQuestion.questionType == QuizQuestionType.MultipleChoice.toString()
        updatedQuestion.answers.answer == ["Answer #1", "Answer #2", "Answer #3", "Answer #4"]
        updatedQuestion.answers.isCorrect == [false, false, true, true]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4]

        updatedQuestions.questions.question == ["This is questions #1", "This is questions #2"]
        updatedQuestions.questions.answerHint == ["This is a hint for question #1", "This is a hint for question #2"]
        updatedQuestions.questions.questionType == [QuizQuestionType.SingleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]
        updatedQuestions.questions[1].answers.answer == ["Answer #1", "Answer #2", "Answer #3", "Answer #4"]
        updatedQuestions.questions[1].answers.isCorrect == [false, false, true, true]
        updatedQuestions.questions[1].answers.displayOrder == [1, 2, 3, 4]
    }

    def "update question definition - change correct answers and question type - MultipleChoice -> SingleChoice"() {
        def quiz = QuizDefFactory.createQuiz(1)
        def newQuiz = skillsService.createQuizDef(quiz)
        def q1 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice)).body
        def q2 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.MultipleChoice)).body
        q2.questionType = QuizQuestionType.SingleChoice.toString()
        q2.answers[0].isCorrect = false
        q2.answers[1].isCorrect = false
        q2.answers[2].isCorrect = true
        q2.answers[3].isCorrect = false
        q2.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.answerHint ==  "This is a hint for question #2"
        updatedQuestion.questionType == QuizQuestionType.SingleChoice.toString()
        updatedQuestion.answers.answer == ["Answer #1", "Answer #2", "Answer #3", "Answer #4"]
        updatedQuestion.answers.isCorrect == [false, false, true, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4]
    }

    def "update question definition - one answer was removed"() {
        def quiz = QuizDefFactory.createQuiz(1)
        def newQuiz = skillsService.createQuizDef(quiz)
        def q1 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice)).body
        def q2 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice)).body
        q2.answers.remove(0)
        q2.answers[0].isCorrect = true
        q2.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.answerHint ==  "This is a hint for question #2"
        updatedQuestion.questionType == QuizQuestionType.SingleChoice.toString()
        updatedQuestion.answers.answer == ["Answer #2", "Answer #3", "Answer #4"]
        updatedQuestion.answers.isCorrect == [true, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3]
    }

    def "update question definition - one answer was added at the end"() {
        def quiz = QuizDefFactory.createQuiz(1)
        def newQuiz = skillsService.createQuizDef(quiz)
        def q1 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice)).body
        def q2 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice)).body
        q2.answers.add([
                answer: "New",
                isCorrect: false,
        ])
        q2.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.answerHint ==  "This is a hint for question #2"
        updatedQuestion.questionType == QuizQuestionType.SingleChoice.toString()
        updatedQuestion.answers.answer == ["Answer #1", "Answer #2", "Answer #3", "Answer #4", "New"]
        updatedQuestion.answers.isCorrect == [true, false, false, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4, 5]
    }

    def "update question definition - one answer was added in the middle"() {
        def quiz = QuizDefFactory.createQuiz(1)
        def newQuiz = skillsService.createQuizDef(quiz)
        def q1 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice)).body
        def q2 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice)).body
        q2.answers.add(1, [
                answer: "New",
                isCorrect: false,
        ])
        q2.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.answerHint ==  "This is a hint for question #2"
        updatedQuestion.questionType == QuizQuestionType.SingleChoice.toString()
        updatedQuestion.answers.answer == ["Answer #1", "New", "Answer #2", "Answer #3", "Answer #4"]
        updatedQuestion.answers.isCorrect == [true, false, false, false, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4, 5]
    }

    def "update question definition - one answer was added, one question was removed and type was changed"() {
        def quiz = QuizDefFactory.createQuiz(1)
        def newQuiz = skillsService.createQuizDef(quiz)
        def q1 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice)).body
        def q2 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice)).body
        q2.answers.remove(2)
        q2.answers.add(0, [
                answer: "New",
                isCorrect: false,
        ])
        q2.answers[0].isCorrect = true
        q2.answers[1].isCorrect = true
        q2.answers[2].isCorrect = true
        q2.answers[3].isCorrect = false
        q2.quizId = quiz.quizId
        q2.questionType = QuizQuestionType.MultipleChoice.toString()
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.answerHint ==  "This is a hint for question #2"
        updatedQuestion.questionType == QuizQuestionType.MultipleChoice.toString()
        updatedQuestion.answers.answer == ["New", "Answer #1", "Answer #2", "Answer #4"]
        updatedQuestion.answers.isCorrect == [true, true, true, false]
        updatedQuestion.answers.displayOrder == [1, 2, 3, 4]
    }

    def "update question definition - all answers replaced"() {
        def quiz = QuizDefFactory.createQuiz(1)
        def newQuiz = skillsService.createQuizDef(quiz)
        def q1 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice)).body
        def q2 = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice)).body
        q2.answers = [[
                answer: "n1",
                isCorrect: false,
        ], [
                answer: "n2",
                isCorrect: true,
        ], [
                answer: "n3",
                isCorrect: true,
        ]]
        q2.quizId = quiz.quizId
        q2.questionType = QuizQuestionType.MultipleChoice.toString()
        skillsService.updateQuizQuestionDef(q2)

        when:
        def updatedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, q2.id)

        then:
        updatedQuestion.question ==  "This is questions #2"
        updatedQuestion.answerHint ==  "This is a hint for question #2"
        updatedQuestion.questionType == QuizQuestionType.MultipleChoice.toString()
        updatedQuestion.answers.answer == ["n1", "n2", "n3"]
        updatedQuestion.answers.isCorrect == [false, true, true]
        updatedQuestion.answers.displayOrder == [1, 2, 3]
    }

    def "quiz answers and answer hints are sanitized"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        def question = QuizDefFactory.createChoiceQuestion(1, 1, 2, QuizQuestionType.SingleChoice)
        question.question = "sanitized <script>alert('xss')</script> question"
        question.answerHint = "sanitized <script>alert('xss')</script> hint"
        question.answers[0].answer = "sanitized <script>alert('xss')</script> answer"

        when:
        def newQuestion = skillsService.createQuizQuestionDef(question)
        def res = skillsService.getQuizQuestionDefs(quiz.quizId)

        then:
        res
        res.questions
        res.questions.size() == 1
        res.questions[0].question == 'sanitized  question'
        res.questions[0].answerHint == 'sanitized  hint'
        res.questions[0].answers[0].answer == 'sanitized  answer'
    }
}

