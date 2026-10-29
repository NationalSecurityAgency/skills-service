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
import skills.PublicProps
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType

@Slf4j
class QuestionDefValidationSpecs extends DefaultIntSpec {

    @Autowired
    PublicProps publicProps

    def "matching question rejects identical terms with different values"() {
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        Map question = QuizDefFactory.createMatchingQuestion(1, 1, 2)
        question.answers[0].multiPartAnswer = [term: 'Repeated Term', value: 'First Answer']
        question.answers[1].multiPartAnswer = [term: 'Repeated Term', value: 'Second Answer']

        when:
        skillsService.createQuizQuestionDef(question)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('Answers can not contain duplicate terms or values')
        skillsService.getQuizQuestionDefs(quiz.quizId).questions == []
    }

    def "matching question rejects duplicate terms regardless of case or surrounding whitespace: '#firstTerm' and '#secondTerm'"() {
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        Map question = QuizDefFactory.createMatchingQuestion(1, 1, 2)
        question.answers[0].multiPartAnswer = [term: firstTerm, value: 'b']
        question.answers[1].multiPartAnswer = [term: secondTerm, value: 'c']

        when:
        skillsService.createQuizQuestionDef(question)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('Answers can not contain duplicate terms or values')
        skillsService.getQuizQuestionDefs(quiz.quizId).questions == []

        where:
        firstTerm | secondTerm
        'a'       | 'A'
        'a'       | '  a  '
        '  a  '   | 'a'
        '  A '    | ' a  '
        'a'       | '\u00A0A\u00A0'
        '\u202Fa' | 'A\u202F'
        '\uFEFFa' | 'A\uFEFF'
        'a'       | '\tA\n'
    }

    def "matching question rejects duplicate values regardless of case or surrounding whitespace: '#firstValue' and '#secondValue'"() {
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        Map question = QuizDefFactory.createMatchingQuestion(1, 1, 2)
        question.answers[0].multiPartAnswer = [term: 'First Term', value: firstValue]
        question.answers[1].multiPartAnswer = [term: 'Second Term', value: secondValue]

        when:
        skillsService.createQuizQuestionDef(question)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('Answers can not contain duplicate terms or values')
        skillsService.getQuizQuestionDefs(quiz.quizId).questions == []

        where:
        firstValue   | secondValue
        'answer'     | 'answer'
        'answer'     | 'ANSWER'
        'answer'     | '  answer  '
        '  answer  ' | 'answer'
        '  ANSWER '  | ' answer  '
        'answer'     | '\u00A0ANSWER\u00A0'
        '\u202Fanswer' | 'ANSWER\u202F'
        '\uFEFFanswer' | 'ANSWER\uFEFF'
        'answer'     | '\tANSWER\n'
    }

    def "matching question update rejects duplicate #field and preserves saved answers: '#duplicate'"() {
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        Map question = QuizDefFactory.createMatchingQuestion(1, 1, 2)
        question.answers[0].multiPartAnswer = [term: 'First Term', value: 'First Answer']
        question.answers[1].multiPartAnswer = [term: 'Second Term', value: 'Second Answer']
        question.id = skillsService.createQuizQuestionDef(question).body.id

        Map savedQuestion = skillsService.getQuizQuestionDef(quiz.quizId, question.id)
        List<Integer> originalAnswerIds = savedQuestion.answers.collect { Map answer ->
            answer.id
        }
        question.answers.eachWithIndex { Map answer, int index ->
            answer.id = originalAnswerIds[index]
        }
        question.answers[1].multiPartAnswer[field] = duplicate

        when:
        skillsService.updateQuizQuestionDef(question)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains('Answers can not contain duplicate terms or values')

        Map retrieved = skillsService.getQuizQuestionDef(quiz.quizId, question.id)
        retrieved.answers*.id == originalAnswerIds
        retrieved.answers*.multiPartAnswer == [
                [term: 'First Term', value: 'First Answer'],
                [term: 'Second Term', value: 'Second Answer'],
        ]

        where:
        field   | duplicate
        'term'  | 'First Term'
        'term'  | '  FIRST TERM  '
        'value' | 'First Answer'
        'value' | '  FIRST ANSWER  '
    }

    def "matching question rejects #field that is blank after normalization: '#blank'"() {
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        Map question = QuizDefFactory.createMatchingQuestion(1, 1, 2)
        question.answers[1].multiPartAnswer[field] = blank

        when:
        skillsService.createQuizQuestionDef(question)

        then:
        SkillsClientException exception = thrown()
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.message.contains("answers.multiPartAnswer.${field} was not provided")
        skillsService.getQuizQuestionDefs(quiz.quizId).questions == []

        where:
        [field, blank] << [['term', 'value'], [
                '\u00A0', '\u202F', '\uFEFF', ' \t\u00A0\u202F\uFEFF\n ',
                '&nbsp;', '&#160;&#x202F;', '<b></b>',
        ]].combinations()
    }

    def "question answer count cannot exceed configured maximum for #questionType"() {
        int maxAnswers = publicProps.getInt(PublicProps.UiProp.maxAnswersPerQuizQuestion)
        def quiz = questionType == QuizQuestionType.Rating ? QuizDefFactory.createQuizSurvey(1) : QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = questionWithAnswers(questionType, maxAnswers + 1)

        when:
        skillsService.createQuizQuestionDef(question)

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("[Number of Answers] must be <= [${maxAnswers}]")
        exception.message.contains("quizId:${quiz.quizId}")
        skillsService.getQuizQuestionDefs(quiz.quizId).questions == []

        where:
        questionType << QuizQuestionType.values()
    }

    def "question accepts the configured maximum answers for #questionType"() {
        int maxAnswers = publicProps.getInt(PublicProps.UiProp.maxAnswersPerQuizQuestion)
        def quiz = questionType == QuizQuestionType.Rating ? QuizDefFactory.createQuizSurvey(1) : QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = questionWithAnswers(questionType, maxAnswers)

        when:
        skillsService.createQuizQuestionDef(question)

        then:
        def savedQuestions = skillsService.getQuizQuestionDefs(quiz.quizId).questions
        savedQuestions.size() == 1
        savedQuestions[0].answers.size() == maxAnswers

        where:
        questionType << QuizQuestionType.values().findAll { it != QuizQuestionType.TextInput }
    }

    def "text input question accepts no predefined answers and rejects predefined answers"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createTextInputQuestion(1, 1)
        skillsService.createQuizQuestionDef(question)

        when:
        skillsService.createQuizQuestionDef(questionWithAnswers(QuizQuestionType.TextInput, 1))

        then:
        SkillsClientException exception = thrown()
        exception.message.contains("must not provide an answer")
        skillsService.getQuizQuestionDefs(quiz.quizId).questions.size() == 1
    }

    private Map questionWithAnswers(QuizQuestionType questionType, int numAnswers) {
        if (questionType == QuizQuestionType.Matching) {
            return QuizDefFactory.createMatchingQuestion(1, 1, numAnswers)
        }
        if (questionType == QuizQuestionType.FillInTheBlank) {
            return QuizDefFactory.createFillInTheBlankQuestion(1, 1, numAnswers)
        }
        if (questionType == QuizQuestionType.Rating) {
            def question = QuizDefFactory.createRatingSurveyQuestion(1, 1)
            question.questionScale = numAnswers
            return question
        }
        return QuizDefFactory.createChoiceQuestion(1, 1, numAnswers, questionType)
    }

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

    def "answer hint text <= 2000 chars"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question = QuizDefFactory.createChoiceQuestion(1, 1)

        when:
        question.answerHint = (1..2001).collect { "a" }.join("")
        skillsService.createQuizQuestionDefs([question])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[Answer Hint] must not exceed [2000] chars")
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
        // Generated question IDs are positive; a fixed positive ID can exist as sequences advance.
        int nonexistentQuestionId = -1

        when:
        skillsService.getQuizQuestionDef(quiz1.quizId, nonexistentQuestionId)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.resBody.contains("Provided question id [${nonexistentQuestionId}] does not exist")
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

    def "matching question term must be provided"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question = QuizDefFactory.createMatchingQuestion(1, 1, 2)
        question.answers[0].multiPartAnswer = [
                term: null,
                value: "val"
        ]

        when:
        skillsService.createQuizQuestionDefs([question])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("answers.multiPartAnswer.term was not provided")
    }

    def "matching question term <= 2000 chars"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question = QuizDefFactory.createMatchingQuestion(1, 1, 2)
        question.answers[0].multiPartAnswer = [
                term: (1..2001).collect { "a" }.join(""),
                value: "val"
        ]

        when:
        skillsService.createQuizQuestionDefs([question])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[answers.multiPartAnswer.term] must not exceed [2000] chars")
    }

    def "matching question value must be provided"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question = QuizDefFactory.createMatchingQuestion(1, 1, 2)
        question.answers[0].multiPartAnswer = [
                term: "term",
                value: null,
        ]

        when:
        skillsService.createQuizQuestionDefs([question])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("answers.multiPartAnswer.value was not provided")
    }

    def "matching question value <= 2000 chars"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def question = QuizDefFactory.createMatchingQuestion(1, 1, 2)
        question.answers[0].multiPartAnswer = [
                term: "term",
                value: (1..2001).collect { "a" }.join(""),
        ]

        when:
        skillsService.createQuizQuestionDefs([question])

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[answers.multiPartAnswer.value] must not exceed [2000] chars")
    }

}
