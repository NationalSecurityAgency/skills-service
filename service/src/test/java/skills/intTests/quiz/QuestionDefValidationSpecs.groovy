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
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType

@Slf4j
class QuestionDefValidationSpecs extends DefaultIntSpec {

    def "quiz single choice question must have 1 answer marked as correct"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.SingleChoice)
        question.answers[1].isCorrect = true
        when:
        skillsService.createQuizQuestionDefs([question])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("For questionType=[SingleChoice] must provide exactly 1 correct answer")
    }

    def "quiz multiple choice question must have 2 or more questions marked as correct"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.MultipleChoice)
        question.answers[0].isCorrect = true
        question.answers[1].isCorrect = false
        question.answers[2].isCorrect = false
        question.answers[3].isCorrect = false
        when:
        skillsService.createQuizQuestionDefs([question])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("For questionType=[MultipleChoice] must provide >= 2 correct answers")
    }

    def "quiz SingleChoice question must have at least 2 answers"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.SingleChoice)
        when:
        skillsService.createQuizQuestionDefs([question])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Must have at least 2 answers")
    }

    def "quiz MultipleChoice question must have at least 2 answers"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.MultipleChoice)
        when:
        skillsService.createQuizQuestionDefs([question])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Must have at least 2 answers")
    }

    def "survey SingleChoice question must have at least 2 answers"() {
        def survey = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(survey)
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.SingleChoice)
        when:
        skillsService.createQuizQuestionDef(question)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Must have at least 2 answers")
    }

    def "survey MultipleChoice question must have at least 2 answers"() {
        def survey = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(survey)
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.MultipleChoice)
        when:
        skillsService.createQuizQuestionDef(question)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Must have at least 2 answers")
    }

    def "question text custom validation"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question = QuizDefFactory.createChoiceQuestion(1, 1)

        when:
        question.question = "ab jabberwocky kd"
        skillsService.createQuizQuestionDefs([question])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Question: paragraphs may not contain jabberwocky")
    }

    def "question text <= 2000 chars"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question = QuizDefFactory.createChoiceQuestion(1, 1)

        when:
        question.question = (1..2001).collect { "a" }.join("")
        skillsService.createQuizQuestionDefs([question])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[Question] must not exceed [2000] chars")
    }

    def "quiz SingleChoice question must fill in each answer"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.SingleChoice)
        question.answers[0].answer = ''
        when:
        skillsService.createQuizQuestionDefs([question])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("answers.answer was not provided")
    }

    def "quiz MultipleChoice question must fill in each answer"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.MultipleChoice)
        question.answers[1].answer = ''
        when:
        skillsService.createQuizQuestionDefs([question])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("answers.answer was not provided")
    }

    def "quiz may not have more than 500 questions" () {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        500.times {
            def q = QuizDefFactory.createChoiceQuestion(1, it)
            skillsService.createQuizQuestionDef(q)
        }

        when:
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 501))
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[Number of Questions] must be <= [500]")
        skillsClientException.message.contains("quizId:${quiz.quizId}")
    }

    def "answer text <= 2000 chars"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question = QuizDefFactory.createChoiceQuestion(1, 1)

        when:
        question.answers[0].answer = (1..2001).collect { "a" }.join("")
        skillsService.createQuizQuestionDefs([question])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[Answer] must not exceed [2000] chars")
    }

    def "try to retrieve quiz with bad question id"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question = QuizDefFactory.createChoiceQuestion(1, 1)
        skillsService.createQuizQuestionDef(question)

        when:
        skillsService.getQuizQuestionDef(quiz1.quizId, 10000)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.resBody.contains("Provided question id [10000] does not exist")
    }

    def "try to retrieve quiz with question id from another quiz"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question = QuizDefFactory.createChoiceQuestion(1, 1)
        skillsService.createQuizQuestionDef(question)

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)
        def question2 = QuizDefFactory.createChoiceQuestion(2, 1)
        skillsService.createQuizQuestionDef(question2)

        def quiz2Questions = skillsService.getQuizQuestionDefs(quiz2.quizId)
        when:
        skillsService.getQuizQuestionDef(quiz1.quizId, quiz2Questions.questions[0].id)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.resBody.contains("Provided question id [${quiz2Questions.questions[0].id}] does not exist in quiz [${quiz1.quizId}]")
    }

}


