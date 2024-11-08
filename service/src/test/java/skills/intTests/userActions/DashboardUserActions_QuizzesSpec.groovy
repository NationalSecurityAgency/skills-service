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
package skills.intTests.userActions

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.UserAttrs
import skills.storage.model.UserQuizAttempt
import skills.storage.model.auth.RoleName

class DashboardUserActions_QuizzesSpec  extends DefaultIntSpec {

    SkillsService rootService
    def setup() {
        rootService = createRootSkillService()
    }
    private String getDisplayName() {
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        return userAttrs.getUserIdForDisplay()
    }

    def "quiz def CRUD"() {
        def quiz = QuizDefFactory.createQuiz(1)
        String originalName = quiz.name
        when:
        skillsService.createQuizDef(quiz)
        quiz.name = "Cool New Name"
        quiz.description = "Important Update"
        skillsService.createQuizDef(quiz, quiz.quizId)
        skillsService.removeQuizDef(quiz.quizId)

        then:
        def res = rootService.getUserActionsForEverything()
        def createAction = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        then:
        res.count == 3
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Quiz.toString()
        res.data[0].itemId == quiz.quizId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        res.data[0].quizId == quiz.quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.Quiz.toString()
        res.data[1].itemId == quiz.quizId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        !res.data[1].projectId
        res.data[1].quizId == quiz.quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.Quiz.toString()
        res.data[2].itemId == quiz.quizId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        !res.data[2].projectId
        res.data[2].quizId == quiz.quizId

        createAction.name == originalName
        !createAction.description

        editAction.id == res.data[1].itemRefId
        editAction.name == quiz.name
        editAction.description == "Important Update"

        !deleteAction.id
    }

    def "question def CRUD"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        userActionsHistoryRepo.deleteAll()
        when:
        def question1 = QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.SingleChoice)
        def q1Res = skillsService.createQuizQuestionDef(question1).body
        def question2 = QuizDefFactory.createChoiceQuestion(1, 2, 3, QuizQuestionType.MultipleChoice)
        skillsService.createQuizQuestionDef(question2)

        q1Res.question = "New Cool Questions?"
        q1Res.answers[0].answer = "New answer 1"
        q1Res.answers[2].answer = "New answer 3"
        q1Res.answers[0].isCorrect = false
        q1Res.answers[1].isCorrect = true
        q1Res.answers[2].isCorrect = false
        q1Res.answers[3].isCorrect = false
        q1Res.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(q1Res)

        skillsService.deleteQuizQuestionDef(quiz.quizId, q1Res.id)
        then:
        def res = rootService.getUserActionsForEverything()
        def createAction1 = rootService.getUserActionAttributes(res.data[3].id)
        def createAction2 = rootService.getUserActionAttributes(res.data[2].id)
        def editAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        then:
        res.count == 4
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.Question.toString()
        res.data[0].itemId == quiz.quizId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        res.data[0].quizId == quiz.quizId

        res.data[1].action == DashboardAction.Edit.toString()
        res.data[1].item == DashboardItem.Question.toString()
        res.data[1].itemId == quiz.quizId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        !res.data[1].projectId
        res.data[1].quizId == quiz.quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.Question.toString()
        res.data[2].itemId == quiz.quizId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        !res.data[2].projectId
        res.data[2].quizId == quiz.quizId

        res.data[3].action == DashboardAction.Create.toString()
        res.data[3].item == DashboardItem.Question.toString()
        res.data[3].itemId == quiz.quizId
        res.data[3].userId == skillsService.userName
        res.data[3].userIdForDisplay == displayName
        !res.data[3].projectId
        res.data[3].quizId == quiz.quizId

        createAction1.question == "This is questions #1"
        createAction1.questionType == "SingleChoice"
        createAction1["Answer1:text"] == "Answer #1"
        createAction1["Answer1:isCorrectAnswer"] == "true"
        createAction1["Answer2:text"] == "Answer #2"
        createAction1["Answer2:isCorrectAnswer"] == "false"
        createAction1["Answer3:text"] == "Answer #3"
        createAction1["Answer3:isCorrectAnswer"] == "false"
        createAction1["Answer4:text"] == "Answer #4"
        createAction1["Answer4:isCorrectAnswer"] == "false"

        createAction2.question == "This is questions #2"
        createAction2.questionType == "MultipleChoice"
        createAction2["Answer1:text"] == "Answer #1"
        createAction2["Answer1:isCorrectAnswer"] == "true"
        createAction2["Answer2:text"] == "Answer #2"
        createAction2["Answer2:isCorrectAnswer"] == "false"
        createAction2["Answer3:text"] == "Answer #3"
        createAction2["Answer3:isCorrectAnswer"] == "true"

        editAction.question == "New Cool Questions?"
        editAction.questionType == "SingleChoice"
        editAction["Answer1:text"] == "New answer 1"
        editAction["Answer1:isCorrectAnswer"] == "false"
        editAction["Answer2:text"] == "Answer #2"
        editAction["Answer2:isCorrectAnswer"] == "true"
        editAction["Answer3:text"] == "New answer 3"
        editAction["Answer3:isCorrectAnswer"] == "false"
        editAction["Answer4:text"] == "Answer #4"
        editAction["Answer4:isCorrectAnswer"] == "false"

        !deleteAction.id
        deleteAction.question == q1Res.question
        deleteAction.questionType == q1Res.questionType
    }

    def "remove quiz run"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)
        userActionsHistoryRepo.deleteAll()

        String userId = createService(getRandomUsers(1, true)).userName
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[0].id, userId)
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body

        def quizRuns = skillsService.getQuizRuns(quiz.quizId, 10, 1, 'started', true, '')
        when:
        skillsService.deleteQuizRun(quiz.quizId, quizRuns.data[0].attemptId)

        def res = rootService.getUserActionsForEverything()
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        then:
        res.count == 1
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.QuizAttempt.toString()
        res.data[0].itemId == quiz.quizId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        res.data[0].quizId == quiz.quizId

        deleteAction.attemptStarted
        deleteAction.attemptCompleted
        deleteAction.status == UserQuizAttempt.QuizAttemptStatus.PASSED.toString()
    }

    def "quiz user role CRUD"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        userActionsHistoryRepo.deleteAll()
        SkillsService otherUser = createService(getRandomUsers(1))
        when:
        skillsService.addQuizUserRole(quiz.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.deleteQuizUserRole(quiz.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        def res = rootService.getUserActionsForEverything()
        def createAction = rootService.getUserActionAttributes(res.data[1].id)
        def deleteAction = rootService.getUserActionAttributes(res.data[0].id)
        then:
        res.count == 2
        res.data[0].action == DashboardAction.Delete.toString()
        res.data[0].item == DashboardItem.UserRole.toString()
        res.data[0].itemId == userAttrsRepo.findByUserIdIgnoreCase(otherUser.userName).userIdForDisplay
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        res.data[0].quizId == quiz.quizId

        res.data[1].action == DashboardAction.Create.toString()
        res.data[1].item == DashboardItem.UserRole.toString()
        res.data[1].itemId == userAttrsRepo.findByUserIdIgnoreCase(otherUser.userName).userIdForDisplay
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        !res.data[1].projectId
        res.data[1].quizId == quiz.quizId

        createAction.userRole == RoleName.ROLE_QUIZ_ADMIN.toString()

        deleteAction.userRole == RoleName.ROLE_QUIZ_ADMIN.toString()
    }

    def "quiz settings CRUD"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        userActionsHistoryRepo.deleteAll()
        SkillsService otherUser = createService(getRandomUsers(1))
        when:
        String name1 = "set1"
        String name2 = "set2"
        String name3 = "set3"
        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: name1, value: 'a-1'],
                [setting: name2, value: 'a-2'],
                [setting: name3, value: 'a-3'],
        ])

        def res = rootService.getUserActionsForEverything()
        def settingAction3 = rootService.getUserActionAttributes(res.data[2].id)
        def settingAction2 = rootService.getUserActionAttributes(res.data[1].id)
        def settingAction1 = rootService.getUserActionAttributes(res.data[0].id)
        then:
        res.count == 3
        res.data[0].action == DashboardAction.Create.toString()
        res.data[0].item == DashboardItem.Settings.toString()
        res.data[0].itemId == quiz.quizId
        res.data[0].userId == skillsService.userName
        res.data[0].userIdForDisplay == displayName
        !res.data[0].projectId
        res.data[0].quizId == quiz.quizId

        res.data[1].action == DashboardAction.Create.toString()
        res.data[1].item == DashboardItem.Settings.toString()
        res.data[1].itemId == quiz.quizId
        res.data[1].userId == skillsService.userName
        res.data[1].userIdForDisplay == displayName
        !res.data[1].projectId
        res.data[1].quizId == quiz.quizId

        res.data[2].action == DashboardAction.Create.toString()
        res.data[2].item == DashboardItem.Settings.toString()
        res.data[2].itemId == quiz.quizId
        res.data[2].userId == skillsService.userName
        res.data[2].userIdForDisplay == displayName
        !res.data[2].projectId
        res.data[2].quizId == quiz.quizId

        settingAction1.setting == name3
        settingAction1.value == 'a-3'

        settingAction2.setting == name2
        settingAction2.value == 'a-2'

        settingAction3.setting == name1
        settingAction3.value == 'a-1'
    }

}
