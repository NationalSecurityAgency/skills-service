package skills.intTests.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.PublicProps
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.services.quiz.QuizQuestionType
import skills.storage.repos.QuizQuestionDefRepo

class QuizQuestionCopySpecs extends DefaultIntSpec  {

    @Autowired
    QuizQuestionDefRepo quizQuestionDefRepo

    @Autowired
    PublicProps publicProps

    def "empty selected question ids cannot copy the entire quiz"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2))

        when:
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [])

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains('At least one question id must be provided')
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions.size() == 1
    }

    def "selected question ids must all belong to the source quiz"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        def otherQuiz = QuizDefFactory.createQuiz(3)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        skillsService.createQuizDef(otherQuiz)
        def sourceQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body
        def otherQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(3, 1, 2)).body

        when: 'a request includes an id from another quiz'
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [sourceQuestion.id, otherQuestion.id])

        then:
        SkillsClientException foreignId = thrown()
        foreignId.httpStatus == HttpStatus.BAD_REQUEST
        foreignId.message.contains("Question ids [${otherQuestion.id}] do not belong to quiz [${sourceQuiz.quizId}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty

        when: 'a request includes a nonexistent id'
        Integer nonexistentId = Math.max(sourceQuestion.id, otherQuestion.id) + 1_000_000
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [sourceQuestion.id, nonexistentId])

        then:
        SkillsClientException missingId = thrown()
        missingId.httpStatus == HttpStatus.BAD_REQUEST
        missingId.message.contains("Question ids [${nonexistentId}] do not belong to quiz [${sourceQuiz.quizId}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty
    }

    def "copy refuses questions that would exceed the maximum allowed in the destination quiz"() {
        int maxQuestions = publicProps.getInt(PublicProps.UiProp.maxQuestionsPerQuiz)
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, maxQuestions, 2))
        def existingQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions

        when:
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [sourceQuestion.id])

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains("Questions would exceed the maximum number of questions [${maxQuestions}] in [${destinationQuiz.quizId}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions*.id == existingQuestions*.id
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == [sourceQuestion.id]
    }

    def "copy refuses a batch of questions that would exceed the maximum allowed in the destination quiz"() {
        int maxQuestions = publicProps.getInt(PublicProps.UiProp.maxQuestionsPerQuiz)
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestions = (1..2).collect { int number ->
            skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, number, 2)).body
        }
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, maxQuestions - 1, 2))
        def existingQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions

        when:
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, sourceQuestions*.id)

        then:
        SkillsClientException error = thrown()
        error.httpStatus == HttpStatus.BAD_REQUEST
        error.message.contains("Questions would exceed the maximum number of questions [${maxQuestions}] in [${destinationQuiz.quizId}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions*.id == existingQuestions*.id
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == sourceQuestions*.id
    }

    def "selected questions append in source order after existing destination questions"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestions = (1..4).collect { number ->
            skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, number, 2)).body
        }
        def existingQuestions = (1..2).collect { number ->
            skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(2, number, 2)).body
        }

        when:
        def copied = skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId,
                [sourceQuestions[3].id, sourceQuestions[1].id])
        def destinationQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then:
        copied.body.success
        destinationQuestions*.question == [existingQuestions[0].question, existingQuestions[1].question,
                                           sourceQuestions[1].question, sourceQuestions[3].question]
        destinationQuestions*.displayOrder == [0, 1, 2, 3]
    }

    def "selected questions copied to an empty quiz preserve their definitions and answer order"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestions = [
                QuizDefFactory.createChoiceQuestion(1, 1, 3, QuizQuestionType.SingleChoice),
                QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.MultipleChoice),
                QuizDefFactory.createChoiceQuestion(1, 3, 2, QuizQuestionType.SingleChoice),
        ].collect { skillsService.createQuizQuestionDef(it).body }

        when:
        def copied = skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId,
                [sourceQuestions[2].id, sourceQuestions[0].id])
        def destinationQuestions = skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then:
        copied.body.success
        destinationQuestions.size() == 2
        destinationQuestions*.displayOrder == [0, 1]
        [sourceQuestions[0], sourceQuestions[2]].eachWithIndex { source, index ->
            def copiedQuestion = destinationQuestions[index]
            assert copiedQuestion.id != source.id
            assert copiedQuestion.question == source.question
            assert copiedQuestion.answerHint == source.answerHint
            assert copiedQuestion.questionType == source.questionType
            assert copiedQuestion.answers*.answer == source.answers*.answer
            assert copiedQuestion.answers*.isCorrect == source.answers*.isCorrect
            assert copiedQuestion.answers*.displayOrder == source.answers*.displayOrder
            assert !copiedQuestion.answers*.id.intersect(source.answers*.id)
        }
        skillsService.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == sourceQuestions*.id
    }

    def "validation failure in a selected question prevents all questions from being copied"() {
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def validQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body
        def invalidQuestion = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 2)).body
        // Existing questions can become invalid after the paragraph validation rules change.
        runInTransaction { quizQuestionDefRepo.updateQuestion(invalidQuestion.id, 'jabberwocky') }

        when:
        skillsService.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [validQuestion.id, invalidQuestion.id])

        then:
        SkillsClientException error = thrown()
        error.message.contains("Validation failed for questionId=[${invalidQuestion.id}]")
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty
    }
}
