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
import skills.auth.AuthUtils
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.services.quiz.QuizQuestionType

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

        then:
        newQuiz.body.quizId == quiz.quizId
        newQuiz.body.name == quiz.name

        quizDefs.quizId == [quiz.quizId]
        quizDefs.name == [quiz.name]
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
        quizDefs.quizId == [quiz1.quizId, quiz2.quizId]
        quizDefsAfter.quizId == [quiz2.quizId]
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
        def quiz2WithDescAfter = skillsService.getQuizDefs()
        then:
        quizDefs.quizId == [quiz1.quizId, quiz2.quizId]
        quizDefs.name == [quiz1.name, originalQuiz2Name]

        quizDefsAfter.quizId == [quiz1.quizId, quiz2.quizId]
        quizDefsAfter.name == [quiz1.name, "Cool New Name"]
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
        quizDefs.quizId == [quiz1.quizId, originalQuizId]
        quizDefsAfter.quizId == [quiz1.quizId, "newid"]
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

    def "add question to quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        def newQuiz = skillsService.createQuizDef(quiz)

        def question = QuizDefFactory.createMultipleChoiceQuestion(1, 1, 2)

        when:
        def newQuestion = skillsService.createQuizQuestionDef(question)

        then:
        newQuestion.body.id
        newQuestion.body.question == question.question
        newQuestion.body.questionType == QuizQuestionType.MultipleChoice.toString()
        newQuestion.body.answers.size() == 2
        newQuestion.body.answers[0].id
        newQuestion.body.answers[1].id
        newQuestion.body.answers.answer == question.answers.answer
        newQuestion.body.answers.isCorrect == [true, false]
    }

    def "get quiz questions"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question1 = QuizDefFactory.createMultipleChoiceQuestion(1, 1, 2)
        skillsService.createQuizQuestionDef(question1)
        def question2 = QuizDefFactory.createMultipleChoiceQuestion(1, 2, 3)
        skillsService.createQuizQuestionDef(question2)

        when:
        def questions = skillsService.getQuizQuestionDefs(quiz.quizId)

        then:
        questions.size() == 2
        questions[0].id
        questions[1].id
        questions.question == [question1.question, question2.question]
        questions.questionType == [QuizQuestionType.MultipleChoice.toString(), QuizQuestionType.MultipleChoice.toString()]

        questions[0].answers[0].id
        questions[0].answers[1].id
        questions[0].answers.answer == question1.answers.answer
        questions[0].answers.isCorrect == question1.answers.isCorrect
    }



}

