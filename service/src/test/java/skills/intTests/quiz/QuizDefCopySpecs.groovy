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
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.services.quiz.QuizQuestionType
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkill
import static skills.intTests.utils.SkillsFactory.createSubject

@Slf4j
class QuizDefCopySpecs extends DefaultIntSpec {
    def "copy a quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)
        def originalQuestions = skillsService.getQuizQuestionDefs(quiz.quizId).questions
        def copiedQuestions = skillsService.getQuizQuestionDefs(copiedQuiz.quizId).questions

        then:
        copiedQuiz.quizId == 'newQuizCopy'
        copiedQuiz.name == 'Copy of Quiz'
        quizDefSummary.numQuestions == 2
        quizDefSummary.quizId == 'newQuizCopy'
        quizDefSummary.name == 'Copy of Quiz'
        originalQuestions.size() == copiedQuestions.size()
        for(def index = 0; index < originalQuestions.size(); index++) {
            def originalQuestion = originalQuestions[index]
            def copiedQuestion = copiedQuestions[index]
            assert originalQuestion.id != copiedQuestion.id
            assert originalQuestion.question == copiedQuestion.question
            assert originalQuestion.questionType == copiedQuestion.questionType
            assert originalQuestion.answers.size() == copiedQuestion.answers.size()
            for(def answerIndex = 0; answerIndex < originalQuestion.answers.size(); answerIndex++) {
                def originalAnswer = originalQuestion.answers[answerIndex]
                def copiedAnswer = copiedQuestion.answers[answerIndex]
                assert originalAnswer.id != copiedAnswer.id
                assert originalAnswer.answer == copiedAnswer.answer
                assert originalAnswer.isCorrect == copiedAnswer.isCorrect
                assert originalAnswer.displayOrder == copiedAnswer.displayOrder
            }
        }
    }

    def "changing original quiz does not change copied quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        when:
        String originalQuiz2Name = quiz.name;
        quiz.name = "Cool New Name"
        skillsService.createQuizDef(quiz, quiz.quizId)
        def modifiedQuizDef = skillsService.getQuizDefSummary(quiz.quizId)
        def copiedQuizDef = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        modifiedQuizDef.name == 'Cool New Name'
        copiedQuizDef.name == quizDefSummary.name
    }

    def "changing copied quiz does not change original quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        when:
        skillsService.createQuizDef([quizId: copiedQuiz.quizId, name: 'Cool New Name', type: copiedQuiz.type], copiedQuiz.quizId)
        def modifiedQuizDef = skillsService.getQuizDefSummary(quiz.quizId)
        def copiedQuizDef = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        modifiedQuizDef.name == 'Test Quiz #1'
        copiedQuizDef.name == 'Cool New Name'
    }

    def "deleting original quiz does not impact copied quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        when:
        skillsService.removeQuizDef(quiz.quizId)
        def quizDefAfter = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        quizDefSummary == quizDefAfter
    }

    def "deleting copied quiz does not impact original quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(quiz.quizId)

        when:
        skillsService.removeQuizDef(copiedQuiz.quizId)
        def quizDefAfter = skillsService.getQuizDefSummary(quiz.quizId)

        then:
        quizDefSummary == quizDefAfter
    }

    def "Quiz settings are copied"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.QuizTimeLimit.setting, value: 300],
                [setting: QuizSettings.QuizLength.setting, value: 10],
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '3'],
        ])

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def copiedQuizInfo = skillsService.getQuizInfo(copiedQuiz.quizId)
        def copiedQuizSettings = skillsService.getQuizSettings(copiedQuiz.quizId)

        then:
        copiedQuizInfo.quizTimeLimit == 300
        copiedQuizInfo.quizLength == 10
        copiedQuizInfo.minNumQuestionsToPass == 3
        copiedQuizSettings.find( it -> it.setting == QuizSettings.QuizTimeLimit.setting ).value == '300'
        copiedQuizSettings.find( it -> it.setting == QuizSettings.QuizLength.setting ).value == '10'
        copiedQuizSettings.find( it -> it.setting == QuizSettings.MinNumQuestionsToPass.setting ).value == '3'
    }

    def "copy a survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)

        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4))
        skillsService.createQuizQuestionDef(QuizDefFactory.createTextInputSurveyQuestion(1, 3))
        skillsService.createQuizQuestionDef(QuizDefFactory.createRatingSurveyQuestion(1, 4))

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)
        def originalQuestions = skillsService.getQuizQuestionDefs(quiz.quizId).questions
        def copiedQuestions = skillsService.getQuizQuestionDefs(copiedQuiz.quizId).questions

        then:
        copiedQuiz.quizId == 'newQuizCopy'
        copiedQuiz.name == 'Copy of Quiz'
        quizDefSummary.numQuestions == 4
        quizDefSummary.quizId == 'newQuizCopy'
        quizDefSummary.name == 'Copy of Quiz'
        originalQuestions.size() == copiedQuestions.size()
        for(def index = 0; index < originalQuestions.size(); index++) {
            def originalQuestion = originalQuestions[index]
            def copiedQuestion = copiedQuestions[index]
            assert originalQuestion.id != copiedQuestion.id
            assert originalQuestion.question == copiedQuestion.question
            assert originalQuestion.questionType == copiedQuestion.questionType
            assert originalQuestion.answers.size() == copiedQuestion.answers.size()
            for(def answerIndex = 0; answerIndex < originalQuestion.answers.size(); answerIndex++) {
                def originalAnswer = originalQuestion.answers[answerIndex]
                def copiedAnswer = copiedQuestion.answers[answerIndex]
                assert originalAnswer.id != copiedAnswer.id
                assert originalAnswer.answer == copiedAnswer.answer
                assert originalAnswer.isCorrect == copiedAnswer.isCorrect
                assert originalAnswer.displayOrder == copiedAnswer.displayOrder
            }
        }
    }

    def "changing original Survey does not change copied Survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        when:
        String originalQuiz2Name = quiz.name;
        quiz.name = "Cool New Name"
        skillsService.createQuizDef(quiz, quiz.quizId)
        def modifiedQuizDef = skillsService.getQuizDefSummary(quiz.quizId)
        def copiedQuizDef = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        modifiedQuizDef.name == 'Cool New Name'
        copiedQuizDef.name == quizDefSummary.name
    }

    def "changing copied Survey does not change original Survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        when:
        skillsService.createQuizDef([quizId: copiedQuiz.quizId, name: 'Cool New Name', type: copiedQuiz.type], copiedQuiz.quizId)
        def modifiedQuizDef = skillsService.getQuizDefSummary(quiz.quizId)
        def copiedQuizDef = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        modifiedQuizDef.name == 'Test Quiz #1'
        copiedQuizDef.name == 'Cool New Name'
    }

    def "deleting original Survey does not impact copied Survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        when:
        skillsService.removeQuizDef(quiz.quizId)
        def quizDefAfter = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        quizDefSummary == quizDefAfter
    }

    def "deleting copied Survey does not impact original Survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(quiz.quizId)

        when:
        skillsService.removeQuizDef(copiedQuiz.quizId)
        def quizDefAfter = skillsService.getQuizDefSummary(quiz.quizId)

        then:
        quizDefSummary == quizDefAfter
    }

    def "Survey settings are copied"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 2)]
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MultipleTakes.setting, value: true],
        ])

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def copiedQuizInfo = skillsService.getQuizInfo(copiedQuiz.quizId)
        def copiedQuizSettings = skillsService.getQuizSettings(copiedQuiz.quizId)

        then:
        copiedQuizInfo.multipleTakes == true
        copiedQuizSettings.find( it -> it.setting == QuizSettings.MultipleTakes.setting ).value == 'true'
    }

    def "Quiz admins are copied"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        copiedRoles.size() == 6
        roles == copiedRoles
    }

    def "Survey admins are copied"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        copiedRoles.size() == 6
        roles == copiedRoles
    }

    def "Changing quiz admins does not change admins in copied quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        when:
        users.forEach { user ->
            skillsService.deleteQuizUserRole(quiz.quizId, user, RoleName.ROLE_QUIZ_ADMIN.toString())
        }
        def updatedRoles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        copiedRoles.size() == 6
        roles == copiedRoles
        updatedRoles.size() == 1
    }

    def "Changing survey admins does not change admins in copied survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        when:
        users.forEach { user ->
            skillsService.deleteQuizUserRole(quiz.quizId, user, RoleName.ROLE_QUIZ_ADMIN.toString())
        }
        def updatedRoles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        copiedRoles.size() == 6
        roles == copiedRoles
        updatedRoles.size() == 1
    }

    def "Changing copied quiz admins does not change admins in original quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        when:
        users.forEach { user ->
            skillsService.deleteQuizUserRole(copiedQuiz.quizId, user, RoleName.ROLE_QUIZ_ADMIN.toString())
        }
        def updatedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        copiedRoles.size() == 6
        roles == copiedRoles
        updatedRoles.size() == 1
    }

    def "Changing copied survey admins does not change admins in original survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        when:
        users.forEach { user ->
            skillsService.deleteQuizUserRole(copiedQuiz.quizId, user, RoleName.ROLE_QUIZ_ADMIN.toString())
        }
        def updatedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        copiedRoles.size() == 6
        roles == copiedRoles
        updatedRoles.size() == 1
    }

    def "Copied quiz does not copy activity history"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        quiz.name = "Cool New Name"
        skillsService.createQuizDef(quiz, quiz.quizId)

        when:
        def originalHistory = skillsService.getUserActionsForQuiz(quiz.quizId)
        def copiedHistory = skillsService.getUserActionsForQuiz(copiedQuiz.quizId)

        then:
        originalHistory.data.eachWithIndex { historyItem, index ->
            assert historyItem.id != copiedHistory.data[index].id
            assert historyItem.itemId != copiedHistory.data[index].itemId
            assert historyItem.itemId == quiz.quizId
            assert copiedHistory.data[index].itemId == copiedQuiz.quizId
        }
    }

    def "Copied survey does not copy activity history"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 2)]
        skillsService.createQuizQuestionDefs(questions)

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        quiz.name = "Cool New Name"
        skillsService.createQuizDef(quiz, quiz.quizId)

        when:
        def originalHistory = skillsService.getUserActionsForQuiz(quiz.quizId)
        def copiedHistory = skillsService.getUserActionsForQuiz(copiedQuiz.quizId)

        then:
        originalHistory.data.eachWithIndex { historyItem, index ->
            assert historyItem.id != copiedHistory.data[index].id
            assert historyItem.itemId != copiedHistory.data[index].itemId
            assert historyItem.itemId == quiz.quizId
            assert copiedHistory.data[index].itemId == copiedQuiz.quizId
        }
    }

    def "Copied quiz does not copy associated skills"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        for(def x = 1; x <= 3; x++) {
            def skillWithQuiz = createSkill(1, 1, x, 1, 1, 480, 200)
            skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
            skillWithQuiz.quizId = quiz.quizId
            skillsService.createSkill(skillWithQuiz)
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        when:
        def originalQuizSkillCount = skillsService.countSkillsForQuiz(quiz.quizId)
        def copiedQuizSkillCount = skillsService.countSkillsForQuiz(copiedQuiz.quizId)

        then:
        originalQuizSkillCount == 3
        copiedQuizSkillCount == 0

    }

    def "Copied survey does not copy associated skills"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        for(def x = 1; x <= 3; x++) {
            def skillWithQuiz = createSkill(1, 1, x, 1, 1, 480, 200)
            skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
            skillWithQuiz.quizId = quiz.quizId
            skillsService.createSkill(skillWithQuiz)
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        when:
        def originalQuizSkillCount = skillsService.countSkillsForQuiz(quiz.quizId)
        def copiedQuizSkillCount = skillsService.countSkillsForQuiz(copiedQuiz.quizId)

        then:
        originalQuizSkillCount == 3
        copiedQuizSkillCount == 0

    }

    def "copied quiz does not copy quiz runs"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(10, true)
        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0)
        }

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def originalRuns = skillsService.getQuizRuns(quiz.quizId)
        def copiedRuns = skillsService.getQuizRuns(copiedQuiz.quizId)

        then:
        originalRuns.data.size() == 10
        copiedRuns.data.size() == 0
    }

    def "copied survey does not copy survey runs"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 2),
                         QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 2)]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(10, true)
        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0)
        }

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def originalRuns = skillsService.getQuizRuns(quiz.quizId)
        def copiedRuns = skillsService.getQuizRuns(copiedQuiz.quizId)

        then:
        originalRuns.data.size() == 10
        copiedRuns.data.size() == 0
    }

    void runQuiz(String userId, def quiz, def quizInfo, boolean pass) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body
    }
}
