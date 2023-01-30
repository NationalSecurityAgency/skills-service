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
class QuizDefValidationSpecs extends DefaultIntSpec {

    def "only quiz admin can remove quiz"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)
        // create project where projectId = quizId
        skillsService.createProject([projectId: quiz1.quizId, name: "Some Project Name"])
        when:
        otherUser.removeQuizDef(quiz1.quizId)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("code=403 FORBIDDEN")
    }

    def "only quiz admin can add a question"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)
        // create project where projectId = quizId
        skillsService.createProject([projectId: quiz1.quizId, name: "Some Project Name"])
        when:
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 2)
        otherUser.createQuizQuestionDef(question)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("code=403 FORBIDDEN")
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

    def "TextInput question type is not supported for a quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question =  QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.TextInput)
        when:
        skillsService.createQuizQuestionDefs([question])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("questionType=[TextInput] is not supported for quiz.type of Quiz")
    }

    def "quiz SingleChoice question must have at lest 2 answers"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.SingleChoice)
        when:
        skillsService.createQuizQuestionDefs([question])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Must have at least 2 answers")
    }

    def "quiz MultipleChoice question must have at lest 2 answers"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 1, QuizQuestionType.MultipleChoice)
        when:
        skillsService.createQuizQuestionDefs([question])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Must have at least 2 answers")
    }
}

