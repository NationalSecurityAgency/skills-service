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
import skills.storage.model.QuizDefParent

@Slf4j
class QuizDefCreationValidationSpecs extends DefaultIntSpec {

    def "changing quiz type between Survey and Quiz is not allowed"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        when:
        quiz1.type = QuizDefParent.QuizType.Survey.toString()
        skillsService.createQuizDef(quiz1, quiz1.quizId)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Existing quiz type cannot be changed")
    }

    def "quiz id must be present"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        quiz1.quizId = null
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("QuizId was not provided")
    }

    def "quiz id must not be null string"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        quiz1.quizId = "null"
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("QuizId was not provided")
    }

    def "quiz id must be at least 3 chars"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        quiz1.quizId = "ab"
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[QuizId] must not be less than [3] chars")
    }

    def "quiz id must be <=50 chars"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        quiz1.quizId = (1..51).collect { "a" }.join("")
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[QuizId] must not exceed [50] chars")
    }

    def "duplicate quiz ids are not allowed"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        when:
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Quiz with id [TestQuiz1] already exists")
    }

    def "only 1k quizzes per admin are allowed"() {
        1000.times {
            def quiz = QuizDefFactory.createQuiz(it)
            skillsService.createQuizDef(quiz)
        }

        when:
        skillsService.createQuizDef(QuizDefFactory.createQuiz(1001))

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Each user is limited to [1000] quiz definitions")
    }

    def "quiz id does not allow special chars"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        quiz1.quizId = "with spaces"
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("must be alpha numeric - no spaces or special characters")
    }

    def "quiz name must be present"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        quiz1.name = null
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Quiz Name was not provided")
    }

    def "quiz name must be at least 3 chars"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        quiz1.name = "ab"
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[Quiz Name] must not be less than [3] chars")
    }

    def "quiz name must be <=75 chars"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        quiz1.name = (1..76).collect { "a" }.join("")
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[Quiz Name] must not exceed [75] chars")
    }

    def "duplicate names are not allowed"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)

        def quiz2 = QuizDefFactory.createQuiz(2)
        quiz2.name = quiz1.name
        when:
        skillsService.createQuizDef(quiz2)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("uiz with name [${quiz1.name}] already exists")
    }

    def "description custom validation"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        quiz1.description = "ab jabberwocky kd"
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("paragraphs may not contain jabberwocky")
    }

    def "description <= 2000"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        quiz1.description = (1..2001).collect { "a" }.join("")
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("[Quiz Description] must not exceed [2000] chars")
    }

    def "only quiz and survey types are supported"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        quiz1.type = "Some"
        skillsService.createQuizDef(quiz1)

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Not supported quiz type [Some] please select one from [Survey, Quiz]")
    }

    def "quiz creation returns created date"() {
        def quiz1 = QuizDefFactory.createQuiz(1)

        when:
        def createdQuiz = skillsService.createQuizDef(quiz1).body

        then:
        createdQuiz.quizId == quiz1.quizId
        createdQuiz.created
    }
}

