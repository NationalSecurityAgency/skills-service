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

}

