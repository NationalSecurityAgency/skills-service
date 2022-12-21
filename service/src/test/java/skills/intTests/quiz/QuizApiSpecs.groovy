package skills.intTests.quiz

import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory

class QuizApiSpecs extends DefaultIntSpec {

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
}
