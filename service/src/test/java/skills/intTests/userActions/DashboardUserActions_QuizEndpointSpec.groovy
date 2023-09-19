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


import skills.controller.exceptions.ErrorCode
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.storage.model.auth.RoleName
import spock.lang.IgnoreIf

class DashboardUserActions_QuizEndpointSpec extends DefaultIntSpec {

    def "get quiz user actions"() {
        SkillsService rootService = createRootSkillService()
        def quizzes = (1..3).collect { QuizDefFactory.createQuiz(it) }
        quizzes.each {
            skillsService.createQuizDef(it)
            Thread.sleep(20)
        }
        when:
        def all = rootService.getUserActionsForEverything(10, 1, "projectId", true)
        def quiz1Only = skillsService.getUserActionsForQuiz(quizzes[0].quizId, 10, 1, "created", true)
        def quiz2Only = skillsService.getUserActionsForQuiz(quizzes[1].quizId, 10, 1, "created", true)
        def quiz3Only = skillsService.getUserActionsForQuiz(quizzes[2].quizId, 10, 1, "created", true)
        then:
        all.data.quizId.sort(false) == quizzes.quizId.sort(false)
        all.count == 3
        all.totalCount == 3

        quiz1Only.data.quizId == [quizzes[0].quizId]
        quiz1Only.count == 1
        quiz1Only.totalCount == 1

        quiz2Only.data.quizId == [quizzes[1].quizId]
        quiz2Only.count == 1
        quiz2Only.totalCount == 1

        quiz3Only.data.quizId == [quizzes[2].quizId]
        quiz3Only.count == 1
        quiz3Only.totalCount == 1
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "get quiz's user actions - userId filter"() {
        def q1 = QuizDefFactory.createQuiz(1)

        List<String> users = ["user1", "something", "other"]

        List<SkillsService> services = users.collect { createService(it) }
        services[0].createQuizDef(q1)
        services[0].addQuizUserRole(q1.quizId, services[1].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        services[0].addQuizUserRole(q1.quizId, services[2].userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        userActionsHistoryRepo.deleteAll()

        services.eachWithIndex { it, i ->
            it.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, i, 2, QuizQuestionType.SingleChoice))
        }

        Closure<String> userIdToDisplay = { String userId -> "${userId} for display".toString()}
        when:
        def all = services[0].getUserActionsForQuiz(q1.quizId, 10, 1, "created", true)
        def user2Only =  services[0].getUserActionsForQuiz(q1.quizId,10, 1, "created", true, null,  'SOme')
        then:
        all.data.userId == [services[0].userName, services[1].userName, services[2].userName]
        all.count == 3
        all.totalCount == 3

        user2Only.data.userId == [users[1]]
        user2Only.data.userIdForDisplay == [userIdToDisplay(services[1].userName)]
        user2Only.count == 1
        user2Only.totalCount == 1
    }

    def "get quiz's user actions - item filter"() {
        def q1 = QuizDefFactory.createQuiz(1)
        def questoin1 = QuizDefFactory.createChoiceQuestion(1, 1, 2, QuizQuestionType.SingleChoice)
        skillsService.createQuizDef(q1)
        Thread.sleep(100)
        skillsService.createQuizQuestionDef(questoin1)
        when:
        def all = skillsService.getUserActionsForQuiz(q1.quizId, 10, 1, "created", true)
        def qestionOnly = skillsService.getUserActionsForQuiz(q1.quizId,10, 1, "created", true, DashboardItem.Question)
        then:
        all.data.item == [DashboardItem.Quiz.toString(), DashboardItem.Question.toString()]
        all.count == 2
        all.totalCount == 2

        qestionOnly.data.item == [DashboardItem.Question.toString()]
        qestionOnly.count == 1
        qestionOnly.totalCount == 1
    }

    def "get quiz's user actions - action filter"() {
        def q1 = QuizDefFactory.createQuiz(1)
        def questoin1 = QuizDefFactory.createChoiceQuestion(1, 1, 2, QuizQuestionType.SingleChoice)
        skillsService.createQuizDef(q1)
        Thread.sleep(100)
        skillsService.createQuizQuestionDef(questoin1)
        Thread.sleep(100)
        def questionDefs = skillsService.getQuizQuestionDefs(q1.quizId)
        skillsService.deleteQuizQuestionDef(q1.quizId, questionDefs.questions[0].id)

        when:
        def all = skillsService.getUserActionsForQuiz(q1.quizId, 10, 1, "created", true)
        def deleteOnly = skillsService.getUserActionsForQuiz(q1.quizId,10, 1, "created", true, null, '', '', DashboardAction.Delete)
        then:
        all.count == 3

        deleteOnly.data.action == [DashboardAction.Delete.toString()]
        deleteOnly.count == 1
        deleteOnly.totalCount == 1
    }

    def "get quiz's user actions - paging"() {
        def q1 = QuizDefFactory.createQuiz(1)

        skillsService.createQuizDef(q1)
        userActionsHistoryRepo.deleteAll()

        List questions = (1..7).collect {
            def q = QuizDefFactory.createChoiceQuestion(1, it, 2, QuizQuestionType.SingleChoice)
            skillsService.createQuizQuestionDef(q)
            Thread.sleep(20)
            return q
        }

        when:
        def page1 = skillsService.getUserActionsForQuiz(q1.quizId, 3, 1, "created", true)
        def page2 = skillsService.getUserActionsForQuiz(q1.quizId, 3, 2, "created", true)
        def page3 = skillsService.getUserActionsForQuiz(q1.quizId, 3, 3, "created", true)
        then:
        page1.count == 7
        page1.totalCount == 7
        page1.data.collect {
            skillsService.getQuizUserActionAttributes(q1.quizId, it.id).question
        } == [questions[0].question, questions[1].question, questions[2].question]

        page2.count == 7
        page2.totalCount == 7
        page2.data.collect {
            skillsService.getQuizUserActionAttributes(q1.quizId, it.id).question
        } == [questions[3].question, questions[4].question, questions[5].question]

        page3.count == 7
        page3.totalCount == 7
        page3.data.collect {
            skillsService.getQuizUserActionAttributes(q1.quizId, it.id).question
        } == [questions[6].question]
    }

    def "not allowed to get attributes for another quiz"() {
        def q1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(q1)
        def q2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(q2)

        def q2Activity = skillsService.getUserActionsForQuiz(q2.quizId, 10, 1, "created", true)
        when:
        skillsService.getQuizUserActionAttributes(q1.quizId, q2Activity.data[0].id)
        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        def parsedBody = new groovy.json.JsonSlurper().parseText(skillsClientException.resBody)
        parsedBody.explanation == "UserActionsHistory id [${q2Activity.data[0].id}] does not belong to quiz [${q1.quizId}]"
        parsedBody.errorCode == ErrorCode.AccessDenied.toString()
    }

    def "not allowed to get attributes for project from quiz endpoint"() {
        def q1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(q1)
        def proj = SkillsFactory.createProject()
        skillsService.createProject(proj)

        def projActivity = skillsService.getUserActionsForProject(proj.projectId, 10, 1, "created", true)
        when:
        skillsService.getQuizUserActionAttributes(q1.quizId, projActivity.data[0].id)
        then:
        SkillsClientException skillsClientException = thrown(SkillsClientException)
        def parsedBody = new groovy.json.JsonSlurper().parseText(skillsClientException.resBody)
        parsedBody.explanation == "UserActionsHistory id [${projActivity.data[0].id}] does not belong to quiz [${q1.quizId}]"
        parsedBody.errorCode == ErrorCode.AccessDenied.toString()
    }

    def "get quiz's action filter options"() {
        def q1 = QuizDefFactory.createQuiz(1)
        def questoin1 = QuizDefFactory.createChoiceQuestion(1, 1, 2, QuizQuestionType.SingleChoice)
        skillsService.createQuizDef(q1)
        skillsService.createQuizQuestionDef(questoin1)
        def questionDefs = skillsService.getQuizQuestionDefs(q1.quizId)
        skillsService.deleteQuizQuestionDef(q1.quizId, questionDefs.questions[0].id)

        def q2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(q2)

        when:
        def q1Options = skillsService.getUserActionFilterOptionsForQuiz(q1.quizId)
        def q2Options = skillsService.getUserActionFilterOptionsForQuiz(q2.quizId)
        then:
        q1Options.actionFilterOptions.sort() == [DashboardAction.Delete.toString(), DashboardAction.Create.toString()].sort()
        q1Options.itemFilterOptions.sort() == [DashboardItem.Quiz.toString(), DashboardItem.Question.toString()].sort()

        q2Options.actionFilterOptions.sort() == [DashboardAction.Create.toString()].sort()
        q2Options.itemFilterOptions.sort() == [DashboardItem.Quiz.toString()].sort()
    }

}
