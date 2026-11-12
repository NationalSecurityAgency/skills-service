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
package skills.intTests.quiz

import groovy.json.JsonSlurper
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizDefParent.QuizType
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName

@SpringBootTest(properties = [
        'skills.config.ui.projectMetricsTagCharts=[{"key":"projectOnly","type":"table"}]',
        'skills.config.ui.overallMetricsTagCharts=[{"key":"overallOnly","type":"bar"}]',
        'skills.config.ui.usersTableAdditionalUserTagKey=quizOnly',
        'skills.config.ui.userPageTagsToDisplay=pageOnly/Page',
        'skills.config.ui.approvalConfUserTagKey=approvalOnly',
        'skills.config.ui.userCommunityUserTagKey=communityOnly',
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class QuizUserTagMetricsAccessIT extends DefaultIntSpec {

    SkillsService rootUser
    List<SkillsService> learners

    def setup() {
        rootUser = createRootSkillService()
        learners = getRandomUsers(2).collect { String userId -> createService(userId) }
        learners.each { SkillsService learner ->
            rootUser.saveUserTag(learner.userName, 'quizOnly', ['Visible Group'])
            ['hidden', 'projectOnly', 'overallOnly', 'pageOnly', 'approvalOnly', 'communityOnly', 'QuizOnly'].each { String key ->
                rootUser.saveUserTag(learner.userName, key, ['Private Group'])
            }
        }
    }

    def "#quizType tag counts permit only the configured quiz chart key and preserve date filtering"() {
        Map assessment = createAssessment(quizType)

        when:
        List<Map> counts = skillsService.getQuizUserTagCounts(assessment.quizId, 'quizOnly')
        List<Map> filteredCounts = skillsService.getQuizUserTagCounts(assessment.quizId, 'quizOnly',
                '1900-01-01 00:00:00', '1900-01-02 23:59:59')

        then:
        counts == [[value: 'Visible Group', count: 2]]
        filteredCounts == []

        where:
        quizType << [QuizType.Quiz, QuizType.Survey]
    }

    def "#quizType tag counts reject stored keys outside the quiz scope: #tagKey"() {
        Map assessment = createAssessment(quizType)

        when:
        skillsService.getQuizUserTagCounts(assessment.quizId, tagKey)

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.quizId == assessment.quizId
        error.explanation == "Tag key [${tagKey}] is not configured for quiz tag metrics"
        !exception.resBody.contains('Private Group')

        where:
        [quizType, tagKey] << [
                [QuizType.Quiz, QuizType.Survey],
                ['hidden', 'projectOnly', 'overallOnly', 'pageOnly', 'approvalOnly', 'communityOnly', 'QuizOnly'],
        ].combinations()
    }

    def "root access does not bypass #quizType tag key restrictions"() {
        Map assessment = createAssessment(quizType)

        when:
        rootUser.getQuizUserTagCounts(assessment.quizId, 'hidden')

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.quizId == assessment.quizId
        error.explanation == 'Tag key [hidden] is not configured for quiz tag metrics'

        where:
        quizType << [QuizType.Quiz, QuizType.Survey]
    }

    def "#projectRole callers with derived quiz read-only access respect #quizType tag key restrictions"() {
        Map assessment = createAssessment(quizType)
        Map project = SkillsFactory.createProject()
        Map subject = SkillsFactory.createSubject()
        skillsService.createProject(project)
        skillsService.createSubject(subject)
        Map linkedSkill = SkillsFactory.createSkill()
        linkedSkill.selfReportingType = SkillDef.SelfReportingType.Quiz
        linkedSkill.quizId = assessment.quizId
        skillsService.createSkill(linkedSkill)

        List<String> excludedUsers = [skillsService.userName, rootUser.userName] + learners.collect { SkillsService learner -> learner.userName }
        SkillsService readOnlyCaller = createService(getRandomUsers(1, true, excludedUsers).first())
        // Grant only a project role; quiz read-only access must come from the skill association.
        skillsService.addUserRole(readOnlyCaller.userName, project.projectId, projectRole.name())

        when:
        List<Map> settings = readOnlyCaller.getQuizSettings(assessment.quizId)
        List<Map> counts = readOnlyCaller.getQuizUserTagCounts(assessment.quizId, 'quizOnly')

        then:
        settings.find { Map setting -> setting.setting == QuizSettings.QuizUserRole.setting }.value == RoleName.ROLE_QUIZ_READ_ONLY.name()
        counts == [[value: 'Visible Group', count: 2]]

        when:
        readOnlyCaller.getQuizUserTagCounts(assessment.quizId, 'hidden')

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.quizId == assessment.quizId
        error.explanation == 'Tag key [hidden] is not configured for quiz tag metrics'
        !exception.resBody.contains('Private Group')

        where:
        [projectRole, quizType] << [
                [RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER],
                [QuizType.Quiz, QuizType.Survey],
        ].combinations()
    }

    def "missing and blank quiz keys return structured BadParam errors: #tagKey"() {
        Map assessment = createAssessment(QuizType.Quiz)

        when:
        skillsService.getQuizUserTagCounts(assessment.quizId, tagKey)

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.quizId == assessment.quizId
        error.explanation == 'User Tag Key was not provided.'

        where:
        tagKey << [null, '', ' ', 'null']
    }

    def "quiz authorization remains required even for the configured tag key"() {
        Map assessment = createAssessment(QuizType.Quiz)

        when:
        learners.first().getQuizUserTagCounts(assessment.quizId, 'quizOnly')

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.FORBIDDEN
    }

    private Map createAssessment(QuizType quizType) {
        Map assessment = quizType == QuizType.Survey ? QuizDefFactory.createQuizSurvey() : QuizDefFactory.createQuiz()
        skillsService.createQuizDef(assessment)
        List<Map> questions = QuizDefFactory.createChoiceQuestions(1, 1, 2, QuizQuestionType.SingleChoice, quizType)
        skillsService.createQuizQuestionDefs(questions)
        learners.each { SkillsService learner ->
            Map attempt = learner.startQuizAttempt(assessment.quizId).body
            learner.reportQuizAnswer(assessment.quizId, attempt.id, attempt.questions.first().answerOptions.first().id)
            learner.completeQuizAttempt(assessment.quizId, attempt.id)
        }
        return assessment
    }
}
