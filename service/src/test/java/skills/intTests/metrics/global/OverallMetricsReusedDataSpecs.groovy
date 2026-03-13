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
package skills.intTests.metrics.global

import groovy.time.TimeCategory
import skills.metrics.builders.MetricsParams
import skills.services.StartDateUtil
import skills.storage.model.EventType
import skills.utils.TestDates

class OverallMetricsReusedDataSpecs extends GlobalReusedDataBaseIntSpec {

    List<Date> days

    def setup() {
        TestDates testDates = new TestDates()
        days = [
                testDates.getDateInPreviousWeek().minusDays(21).toDate(),
                testDates.getDateInPreviousWeek().minusDays(14).toDate(),
                testDates.getDateInPreviousWeek().minusDays(7).toDate(),
                testDates.getDateInPreviousWeek().toDate(),
                testDates.getDateWithinCurrentWeek().toDate(),
        ]
    }

    def "get empty global users progress" () {
        quizDefRepo.deleteAll()
        projDefRepo.deleteAll()
        when:
        def res = admins[0].getOverallMetricsSummary()
        then:
        res.numTotalProjects == 0

        res.numTotalSkills == 0
        res.numTotalBadges == 0
        res.numTotalGlobalBadges == 0

        res.numTotalQuizzes == 0
        res.numTotalSurveys == 0
    }

    def "users progress from admins[0] point of view" () {
        when:
        def res = admins[0].getOverallMetricsSummary()

        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = 30.days.ago.time
        }
        def usersPerDay = admins[0].getOverallMetricsData('overallDistinctUsersOverTimeMetricsBuilder', props)

        // users by duty organization
        props = [
                tagKey: 'dutyOrganization',
                currentPage: 1,
                pageSize: 5,
                sortDesc: true
        ]
        def usersByDutyOrg = admins[0].getOverallMetricsData('overallNumUsersPerTagBuilder', props)
        props.currentPage = 2
        def usersByDutyOrgPage2 = admins[0].getOverallMetricsData('overallNumUsersPerTagBuilder', props)

        // users by admin organization
        props.currentPage = 1
        props.tagKey = 'adminOrganization'
        def usersByAdminOrg = admins[0].getOverallMetricsData('overallNumUsersPerTagBuilder', props)

        then:
        res.numTotalProjects == 3

        res.numTotalSkills == 31
        res.numTotalBadges == 5
        res.numTotalProjectBadges == 2
        res.numTotalGlobalBadges == 3

        res.numTotalQuizzes == 5
        res.numTotalSurveys == 4

        res.projectInfo.projectId.size() == admin0ProjectIds.size()
        res.projectInfo.projectId.containsAll(admin0ProjectIds)
        res.quizInfo.quizId.size() == admin0QuizAndSurveyIds.size()
        res.quizInfo.quizId.containsAll(admin0QuizAndSurveyIds)

        usersPerDay.users.size() == 5
        usersPerDay.users.collect {it.count} == [0, 0, 0, 0, 10]
        usersPerDay.users.collect {it.value} == days.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        usersByDutyOrg.totalNumItems == 6
        usersByDutyOrg.items.size() == 5
        usersByDutyOrg.items.sort { it.value } == [[value:'ABC1', count:3], [value:'ABC2', count:1], [value:'CBF2', count:1], [value:'KOO4', count:2], [value:'KOO5', count:1]]
        usersByDutyOrgPage2.totalNumItems == 6
        usersByDutyOrgPage2.items.size() == 1
        usersByDutyOrgPage2.items.sort { it.value } == [[value:'XYZ1', count:1]]

        usersByAdminOrg.totalNumItems == 2
        usersByAdminOrg.items.size() == 2
        usersByAdminOrg.items.sort { it.value } == [[value:'SKTR', count:4], [value:'XYZ', count:4]]
    }

    def "users progress from admins[1] point of view" () {
        when:
        def res = admins[1].getOverallMetricsSummary()

        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = 30.days.ago.time
        }
        def usersPerDay = admins[1].getOverallMetricsData('overallDistinctUsersOverTimeMetricsBuilder', props)

        // users by duty organization
        props = [
                tagKey: 'dutyOrganization',
                currentPage: 1,
                pageSize: 5,
                sortDesc: true
        ]
        def usersByDutyOrg = admins[1].getOverallMetricsData('overallNumUsersPerTagBuilder', props)
        props.currentPage = 2
        def usersByDutyOrgPage2 = admins[1].getOverallMetricsData('overallNumUsersPerTagBuilder', props)

        // users by admin organization
        props.currentPage = 1
        props.tagKey = 'adminOrganization'
        def usersByAdminOrg = admins[1].getOverallMetricsData('overallNumUsersPerTagBuilder', props)

        then:
        res.numTotalProjects == 2

        res.numTotalSkills == 16
        res.numTotalBadges == 4
        res.numTotalProjectBadges == 1
        res.numTotalGlobalBadges == 3

        res.numTotalQuizzes == 4
        res.numTotalSurveys == 3

        res.projectInfo.projectId.size() == admin1ProjectIds.size()
        res.projectInfo.projectId.containsAll(admin1ProjectIds)
        res.quizInfo.quizId.size() == admin1QuizAndSurveyIds.size()
        res.quizInfo.quizId.containsAll(admin1QuizAndSurveyIds)

        usersPerDay.users.size() == 5
        usersPerDay.users.collect {it.count} == [0, 0, 0, 0, 10]
        usersPerDay.users.collect {it.value} == days.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        usersByDutyOrg.totalNumItems == 6
        usersByDutyOrg.items.size() == 5
        usersByDutyOrg.items.sort { it.value } == [[value:'ABC1', count:3], [value:'ABC2', count:1], [value:'CBF2', count:1], [value:'KOO4', count:2], [value:'KOO5', count:1]]
        usersByDutyOrgPage2.totalNumItems == 6
        usersByDutyOrgPage2.items.size() == 1
        usersByDutyOrgPage2.items.sort { it.value } == [[value:'XYZ1', count:1]]

        usersByAdminOrg.totalNumItems == 2
        usersByAdminOrg.items.size() == 2
        usersByAdminOrg.items.sort { it.value } == [[value:'SKTR', count:4], [value:'XYZ', count:4]]
    }

    def "users progress from admins[2] point of view" () {
        when:
        def res = admins[2].getOverallMetricsSummary()

        Map props = [:]
        use(TimeCategory) {
            props[MetricsParams.P_START_TIMESTAMP] = 30.days.ago.time
        }
        def usersPerDay = admins[2].getOverallMetricsData('overallDistinctUsersOverTimeMetricsBuilder', props)

        // users by duty organization
        props = [
                projIds: admin2ProjectIds.join(','),
                quizIds: admin2QuizAndSurveyIds.join(','),
                tagKey: 'dutyOrganization',
                currentPage: 1,
                pageSize: 5,
                sortDesc: true
        ]
        def usersByDutyOrg = admins[2].getOverallMetricsData('overallNumUsersPerTagBuilder', props)

        // users by admin organization
        props.tagKey = 'adminOrganization'
        def usersByAdminOrg = admins[2].getOverallMetricsData('overallNumUsersPerTagBuilder', props)

        then:
        res.numTotalProjects == 1

        res.numTotalSkills == 9
        res.numTotalBadges == 1
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 1

        res.numTotalQuizzes == 3
        res.numTotalSurveys == 2

        res.projectInfo.projectId.size() == admin2ProjectIds.size()
        res.projectInfo.projectId.containsAll(admin2ProjectIds)
        res.quizInfo.quizId.size() == admin2QuizAndSurveyIds.size()
        res.quizInfo.quizId.containsAll(admin2QuizAndSurveyIds)

        usersPerDay.users.size() == 5
        usersPerDay.users.collect {it.count} == [0, 0, 0, 0, 7]
        usersPerDay.users.collect {it.value} == days.collect { StartDateUtil.computeStartDate(it, EventType.WEEKLY).time}

        usersByDutyOrg.totalNumItems == 5
        usersByDutyOrg.items.size() == 5
        usersByDutyOrg.items.sort { it.value } == [[value:'ABC1', count:2], [value:'CBF2', count:1], [value:'KOO4', count:1], [value:'KOO5', count:1], [value:'XYZ1', count:1]]

        usersByAdminOrg.totalNumItems == 2
        usersByAdminOrg.items.size() == 2
        usersByAdminOrg.items.sort { it.value } == [[value:'SKTR', count:3], [value:'XYZ', count:2]]

    }
}



