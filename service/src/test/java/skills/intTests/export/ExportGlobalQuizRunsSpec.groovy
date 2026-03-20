/**
 * Copyright 2026 SkillTree
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
package skills.intTests.export

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.UserQuizAttempt
import skills.storage.repos.UserQuizAttemptRepo

class ExportGlobalQuizRunsSpec extends ExportBaseIntSpec {

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    def "get empty quiz runs progress"() {
        quizDefRepo.deleteAll()
        projDefRepo.deleteAll()
        when:
        def excelExport = skillsService.getGlobalQuizRunsExcelExport()
        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                ["User ID", "Last Name", "First Name", "Org", "Quiz Name", "Type", "Status", "Number Correct", "Number of Questions", "Date Started (UTC)", "Date Completed (UTC)", "Runtime (seconds)"],
                ["For All Dragons Only"],
        ])
    }

    def "export global quiz runs"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)
        def survey1 = createSurvey(3)

        def users = getRandomUsers(3)
        def user1 = users[0]
        def user2 = users[1]
        def user3 = users[2]

        when:
        runQuizOrSurvey(quiz1, user1, true, true)
        runQuizOrSurvey(quiz1, user2, true, false)
        runQuizOrSurvey(quiz2, user3, false, true)
        runQuizOrSurvey(survey1, user1, false, true)

        def excelExport = skillsService.getGlobalQuizRunsExcelExport()

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                [ "User ID", "Last Name", "First Name", "Org", "Quiz Name", "Type", "Status", "Number Correct", "Number of Questions", "Date Started (UTC)", "Date Completed (UTC)", "Runtime (seconds)"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", quiz1.name, "Quiz", "PASSED", "2.0", "2.0", formatDate(today),  formatDate(today), "5.0"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", quiz1.name, "Quiz", "INPROGRESS", "0.0", "2.0", formatDate(today), "", ""],
                [getUserIdForDisplay(user3), getName(user3, false), getName(user3), "", quiz2.name, "Quiz", "FAILED", "1.0", "2.0", formatDate(today),  formatDate(today), "5.0"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", survey1.name, "Survey", "COMPLETED", "2.0", "2.0", formatDate(today),  formatDate(today), "5.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export global quiz runs with date filter"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)

        def users = getRandomUsers(2)
        def user1 = users[0]
        def user2 = users[1]

        when:
        runQuizOrSurvey(quiz1, user1, true, true, fiveDaysAgo)
        runQuizOrSurvey(quiz2, user2, false, true, today)

        def excelExport = skillsService.getGlobalQuizRunsExcelExport('started', true, '', '', '', fiveDaysAgo.format("yyyy-MM-dd HH:mm:ss"), today.format("yyyy-MM-dd HH:mm:ss"))

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                [ "User ID", "Last Name", "First Name", "Org", "Quiz Name", "Type", "Status", "Number Correct", "Number of Questions", "Date Started (UTC)", "Date Completed (UTC)", "Runtime (seconds)"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", quiz1.name, "Quiz", "PASSED", "2.0", "2.0", formatDate(fiveDaysAgo),  formatDate(fiveDaysAgo), "5.0"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", quiz2.name, "Quiz", "FAILED", "1.0", "2.0", formatDate(today),  formatDate(today), "5.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export global quiz runs with name filter"() {
        def quiz1 = createQuiz(1)
        def quiz2 = createQuiz(2)

        def users = getRandomUsers(2)
        def user1 = users[0]
        def user2 = users[1]

        when:
        runQuizOrSurvey(quiz1, user1, true, true)
        runQuizOrSurvey(quiz2, user2, false, true)

        def excelExport = skillsService.getGlobalQuizRunsExcelExport('started', true, '', 'Quiz 2', '', '', '')

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                [ "User ID", "Last Name", "First Name", "Org", "Quiz Name", "Type", "Status", "Number Correct", "Number of Questions", "Date Started (UTC)", "Date Completed (UTC)", "Runtime (seconds)"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", quiz2.name, "Quiz", "FAILED", "1.0", "2.0", formatDate(today),  formatDate(today), "5.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export global quiz runs with user filter"() {
        def quiz1 = createQuiz(1)

        def users = getRandomUsers(2)
        def user1 = users[0]
        def user2 = users[1]

        when:
        runQuizOrSurvey(quiz1, user1, true, true)
        runQuizOrSurvey(quiz1, user2, false, true)

        def excelExport = skillsService.getGlobalQuizRunsExcelExport('started', true, getUserIdForDisplay(user1), '', '', '')

        then:
        validateExport(excelExport.file, [
                ["For All Dragons Only"],
                [ "User ID", "Last Name", "First Name", "Org", "Quiz Name", "Type", "Status", "Number Correct", "Number of Questions", "Date Started (UTC)", "Date Completed (UTC)", "Runtime (seconds)"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", quiz1.name, "Quiz", "PASSED", "2.0", "2.0", formatDate(today),  formatDate(today), "5.0"],
                ["For All Dragons Only"],
        ])
    }

    def "export global quiz runs for UC protected quiz"() {
        def users = getRandomUsers(3)
        def user1 = users[0]
        def user2 = users[1]

        SkillsService pristineDragonsUser = createService(users[2])
        rootSkillsService.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootSkillsService.saveUserTag(rootSkillsService.userName, 'dragons', ['DivineDragon'])

        def quiz = createQuiz(1, pristineDragonsUser)
        quiz.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(quiz, quiz.quizId)

        when:
        runQuizOrSurvey(quiz, user1, true, true, today, pristineDragonsUser)
        runQuizOrSurvey(quiz, user2, false, true, today, pristineDragonsUser)

        def excelExport = pristineDragonsUser.getGlobalQuizRunsExcelExport()

        then:
        validateExport(excelExport.file, [
                ["For Divine Dragon Only"],
                [ "User ID", "Last Name", "First Name", "Org", "Quiz Name", "Type", "Status", "Number Correct", "Number of Questions", "Date Started (UTC)", "Date Completed (UTC)", "Runtime (seconds)"],
                [getUserIdForDisplay(user1), getName(user1, false), getName(user1), "", quiz.name, "Quiz", "PASSED", "2.0", "2.0", formatDate(today),  formatDate(today), "5.0"],
                [getUserIdForDisplay(user2), getName(user2, false), getName(user2), "", quiz.name, "Quiz", "FAILED", "1.0", "2.0", formatDate(today),  formatDate(today), "5.0"],
                ["For Divine Dragon Only"],
        ])
    }

    def runQuizOrSurvey(def quiz, String userId, boolean pass = true, boolean complete = true, Date performedOn = today, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def quizAttempt = serviceToUse.startQuizAttemptForUserId(quiz.quizId, userId).body
        serviceToUse.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, userId)
        serviceToUse.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[pass ? 0 : 1].id, userId)

        UserQuizAttempt userQuizAttempt = userQuizAttemptRepo.findById(quizAttempt.id).get()
        userQuizAttempt.started = performedOn
        userQuizAttemptRepo.save(userQuizAttempt)
        if (complete) {
            // Add 5 seconds between start and completion to test runtime calculation
            Date completionTime = new Date(performedOn.time + 5000)
            serviceToUse.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body
            userQuizAttempt = userQuizAttemptRepo.findById(quizAttempt.id).get()
            userQuizAttempt.completed = completionTime
            userQuizAttemptRepo.save(userQuizAttempt)
        }
    }

    def createQuiz(int num, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def quiz = QuizDefFactory.createQuiz(num)
        quiz.name = "My Quiz ${num}".toString()
        serviceToUse.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(num, 2, 2)
        serviceToUse.createQuizQuestionDefs(questions)
        return quiz
    }

    def createSurvey(int num, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def survey = QuizDefFactory.createQuizSurvey(num)
        survey.name = "My Survey ${num}".toString()
        serviceToUse.createQuizDef(survey)
        def question1 = QuizDefFactory.createSingleChoiceSurveyQuestion(num, 1, 2)
        def question2 = QuizDefFactory.createSingleChoiceSurveyQuestion(num, 2, 2)
        serviceToUse.createQuizQuestionDefs([question1, question2])
        return survey
    }

}
