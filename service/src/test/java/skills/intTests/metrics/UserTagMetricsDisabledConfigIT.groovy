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
package skills.intTests.metrics

import groovy.json.JsonSlurper
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import skills.SpringBootApp
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory

@SpringBootTest(properties = [
        'skills.config.ui.projectMetricsTagCharts=',
        'skills.config.ui.overallMetricsTagCharts=not-json',
        'skills.config.ui.usersTableAdditionalUserTagKey=',
        'skills.config.ui.userPageTagsToDisplay=',
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class UserTagMetricsDisabledConfigIT extends DefaultIntSpec {

    Map project
    Map subject
    Map skill

    def setup() {
        project = SkillsFactory.createProject()
        subject = SkillsFactory.createSubject()
        skill = SkillsFactory.createSkill()
        skillsService.createProject(project)
        skillsService.createSubject(subject)
        skillsService.createSkill(skill)
    }

    def "empty project configuration rejects #metricsId"() {
        when:
        skillsService.getMetricsData(project.projectId, metricsId, [
                tagKey: 'someCoolKey', userTagKey: 'someCoolKey', tagFilter: 'visible',
                currentPage: 1, pageSize: 10, sortDesc: true, skillId: skill.skillId, subjectId: subject.subjectId,
        ])

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.explanation.contains('Tag key [someCoolKey] is not configured')

        where:
        metricsId << ['numUsersPerTagBuilder', 'skillAchievementsByTagBuilder',
                      'achievementsByTagPerLevelMetricsBuilder', 'numUsersPerLevelChartBuilder']
    }

    def "malformed overall chart JSON fails closed"() {
        when:
        skillsService.getOverallMetricsData('overallNumUsersPerTagBuilder', [
                tagKey: 'someCoolKey', currentPage: 1, pageSize: 10, sortDesc: true,
        ])

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.explanation == 'Tag key [someCoolKey] is not configured for overall tag metrics'
    }

    def "empty drill-down configuration rejects URL keys"() {
        when:
        skillsService.getUserTagUsers(project.projectId, 'someCoolKey', 'visible')

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.resBody.contains('"errorCode":"BadParam"')
    }

    def "unfiltered project and subject levels do not require tag configuration"() {
        when:
        List<Map> projectLevels = skillsService.getMetricsData(project.projectId, 'numUsersPerLevelChartBuilder')
        List<Map> subjectLevels = skillsService.getMetricsData(project.projectId, 'numUsersPerLevelChartBuilder', [subjectId: subject.subjectId])

        then:
        projectLevels == (1..5).collect { Integer level -> [value: "Level ${level}", count: 0] }
        subjectLevels == projectLevels
    }

    def "quiz tag counts reject keys when no quiz chart key is configured"() {
        Map quiz = QuizDefFactory.createQuiz()
        skillsService.createQuizDef(quiz)

        when:
        skillsService.getQuizUserTagCounts(quiz.quizId, 'someCoolKey')

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.quizId == quiz.quizId
        error.explanation == 'Tag key [someCoolKey] is not configured for quiz tag metrics'
    }
}
