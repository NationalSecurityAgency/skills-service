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
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

@SpringBootTest(properties = [
        'skills.config.ui.projectMetricsTagCharts=[{"key":"projectOnly","type":"table"},{"key":"projectOnly","type":"pie"},{"key":123},{"key":""},{"key":" "},{"title":"missingKey"},null,"hidden"]',
        'skills.config.ui.overallMetricsTagCharts=[{"key":"overallOnly","type":"bar"}]',
        'skills.config.ui.usersTableAdditionalUserTagKey=tableOnly',
        'skills.config.ui.userPageTagsToDisplay=pageOnly/Page|pageOnly/Page|missingLabel|emptyLabel/|/emptyKey|extra/label/part| |',
        'skills.config.ui.approvalConfUserTagKey=approvalOnly',
        'skills.config.ui.userCommunityUserTagKey=communityOnly',
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringBootApp)
class UserTagMetricsAccessIT extends DefaultIntSpec {

    Map project
    Map subject
    Map skill
    SkillsService learner
    SkillsService rootUser

    def setup() {
        learner = createService(getRandomUsers(1).first())
        rootUser = createRootSkillService()
        project = SkillsFactory.createProject()
        subject = SkillsFactory.createSubject()
        skillsService.createProject(project)
        skillsService.createSubject(subject)
        List<Map> projectSkills = SkillsFactory.createSkills(3, 1, 1, 100)
        skillsService.createSkills(projectSkills)
        skill = projectSkills.first()
        projectSkills.each { Map projectSkill -> skillsService.addSkill(projectSkill, learner.userName) }
        ['projectOnly', 'overallOnly', 'tableOnly', 'pageOnly', 'hidden', 'approvalOnly', 'communityOnly',
         'ProjectOnly', '123', 'missingLabel', 'emptyLabel', 'extra'].each { String key ->
            rootUser.saveUserTag(learner.userName, key, ['visible'])
        }
    }

    def "project users-per-tag permits the exact project chart key"() {
        when:
        Map result = skillsService.getMetricsData(project.projectId, 'numUsersPerTagBuilder', pagingProps('projectOnly'))

        then:
        result.totalNumItems == 1
        result.items == [[value: 'visible', count: 1]]
    }

    def "overall users-per-tag permits the overall-only chart key"() {
        when:
        Map result = skillsService.getOverallMetricsData('overallNumUsersPerTagBuilder', pagingProps('overallOnly'))

        then:
        result.totalNumItems == 1
        result.items == [[value: 'visible', count: 1]]
    }

    def "skill and subject tag achievements permit the project chart key"() {
        when:
        Map skillResult = skillsService.getMetricsData(project.projectId, 'skillAchievementsByTagBuilder',
                [skillId: skill.skillId, userTagKey: 'projectOnly'])
        Map subjectResult = skillsService.getMetricsData(project.projectId, 'achievementsByTagPerLevelMetricsBuilder',
                [subjectId: subject.subjectId, userTagKey: 'projectOnly'])

        then:
        skillResult == [visible: [numberAchieved: 1, numberInProgress: 0]]
        subjectResult.totalLevels == 5
        subjectResult.data == [[tag: 'visible', value: ['1': 0, '2': 0, '3': 0, '4': 0, '5': 1], totalUsers: 1]]
    }

    def "project tag builder #metricsId rejects keys from other scopes or no scope: #tagKey"() {
        Map props = pagingProps(tagKey) + [skillId: skill.skillId, subjectId: subject.subjectId, userTagKey: tagKey]

        when:
        skillsService.getMetricsData(project.projectId, metricsId, props)

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.projectId == project.projectId
        error.explanation == "Tag key [${tagKey}] is not configured for project tag metrics"
        if (metricsId == 'skillAchievementsByTagBuilder') {
            assert error.skillId == skill.skillId
        }

        where:
        [metricsId, tagKey] << [
                ['numUsersPerTagBuilder', 'skillAchievementsByTagBuilder', 'achievementsByTagPerLevelMetricsBuilder'],
                ['overallOnly', 'tableOnly', 'pageOnly', 'hidden', 'approvalOnly', 'communityOnly', 'ProjectOnly'],
        ].combinations()
    }

    def "overall metrics reject keys outside overall chart configuration: #tagKey"() {
        when:
        skillsService.getOverallMetricsData('overallNumUsersPerTagBuilder', pagingProps(tagKey))

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.explanation == "Tag key [${tagKey}] is not configured for overall tag metrics"

        where:
        tagKey << ['projectOnly', 'tableOnly', 'pageOnly', 'hidden', 'approvalOnly', 'communityOnly', 'OverallOnly']
    }

    def "an approved alternate parameter cannot authorize the consumed key on #metricsId"() {
        Map props = [currentPage: 1, pageSize: 10, sortDesc: true, skillId: skill.skillId, subjectId: subject.subjectId]
        if (metricsId == 'numUsersPerTagBuilder') {
            props.tagKey = 'hidden'
            props.userTagKey = 'projectOnly'
        } else {
            props.userTagKey = 'hidden'
            props.tagKey = 'projectOnly'
        }

        when:
        skillsService.getMetricsData(project.projectId, metricsId, props)

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.resBody.contains('"errorCode":"BadParam"')
        exception.resBody.contains('Tag key [hidden] is not configured for project tag metrics')

        where:
        metricsId << ['numUsersPerTagBuilder', 'skillAchievementsByTagBuilder', 'achievementsByTagPerLevelMetricsBuilder']
    }

    def "both drill-down endpoints permit keys from #tagKey's configuration"() {
        when:
        List<Map> levels = skillsService.getMetricsData(project.projectId, 'numUsersPerLevelChartBuilder',
                [tagKey: tagKey, tagFilter: 'visible'])
        Map users = skillsService.getUserTagUsers(project.projectId, tagKey, 'visible')

        then:
        levels == [
                [value: 'Level 1', count: 0], [value: 'Level 2', count: 0], [value: 'Level 3', count: 0],
                [value: 'Level 4', count: 0], [value: 'Level 5', count: 1],
        ]
        users.totalCount == 1
        users.totalPoints == 300
        users.data.size() == 1
        users.data.first().userId == learner.userName.toLowerCase()
        users.data.first().totalPoints == 300

        where:
        tagKey << ['projectOnly', 'tableOnly', 'pageOnly']
    }

    def "level drill-down rejects unapproved keys, including malformed configuration entries: #tagKey"() {
        when:
        skillsService.getMetricsData(project.projectId, 'numUsersPerLevelChartBuilder', [tagKey: tagKey, tagFilter: 'visible'])

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.explanation == "Tag key [${tagKey}] is not configured for tag metrics drill-down"

        where:
        tagKey << ['overallOnly', 'hidden', 'approvalOnly', 'communityOnly', 'ProjectOnly', '123', 'missingLabel', 'emptyLabel', 'extra']
    }

    def "user-list drill-down rejects unapproved URL keys: #tagKey"() {
        when:
        skillsService.getUserTagUsers(project.projectId, tagKey, 'visible')

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.projectId == project.projectId
        error.explanation == "Tag key [${tagKey}] is not configured for tag metrics drill-down"

        where:
        tagKey << ['overallOnly', 'hidden', 'approvalOnly', 'communityOnly', 'ProjectOnly', 'missingLabel', 'emptyLabel', 'extra']
    }

    def "omitting the tag value does not bypass level drill-down key validation"() {
        when:
        skillsService.getMetricsData(project.projectId, 'numUsersPerLevelChartBuilder', [tagKey: 'hidden'])

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.resBody.contains('"errorCode":"BadParam"')
    }

    def "tag builders reject missing and blank keys: #metricsId #tagKey"() {
        Map props = [currentPage: 1, pageSize: 10, sortDesc: true, skillId: skill.skillId, subjectId: subject.subjectId]
        if (tagKey != null) {
            props.tagKey = tagKey
            props.userTagKey = tagKey
        }

        when:
        if (metricsId == 'overallNumUsersPerTagBuilder') {
            skillsService.getOverallMetricsData(metricsId, props)
        } else {
            skillsService.getMetricsData(project.projectId, metricsId, props)
        }

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        Map error = new JsonSlurper().parseText(exception.resBody)
        error.errorCode == 'BadParam'
        error.explanation == 'Tag Key was not provided.'

        where:
        [metricsId, tagKey] << [
                ['numUsersPerTagBuilder', 'overallNumUsersPerTagBuilder', 'skillAchievementsByTagBuilder', 'achievementsByTagPerLevelMetricsBuilder'],
                [null, '', ' '],
        ].combinations()
    }

    def "root users cannot bypass the project metrics key scope"() {
        when:
        rootUser.getMetricsData(project.projectId, 'numUsersPerTagBuilder', pagingProps('overallOnly'))

        then:
        SkillsClientException exception = thrown(SkillsClientException)
        exception.httpStatus == HttpStatus.BAD_REQUEST
        exception.resBody.contains('"errorCode":"BadParam"')
    }

    private static Map pagingProps(String tagKey) {
        return [tagKey: tagKey, currentPage: 1, pageSize: 10, sortDesc: true]
    }
}
