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

import groovy.transform.Canonical
import groovy.transform.ToString
import skills.intTests.utils.SkillsClientException
import spock.lang.IgnoreIf

class GlobalUsersProgressReusedDataSpecs extends GlobalReusedDataBaseIntSpec {

    def "users progress from admins[0] point of view" () {
        when:
        def res = admins[0].getGlobalUserProgressMetrics("", 15, 1, "numSkillsEarned", false)
        then:
        res.numTotalProjects == 3
        res.numTotalSkills == 31

        res.numTotalQuizzes == 5
        res.numTotalSurveys == 4

        res.numTotalBadges == 5
        res.numTotalProjectBadges == 2
        res.numTotalGlobalBadges == 3

        res.numTotalMetricItems == 10
        res.metricItemsPage.size() == 10

        def user0 = res.metricItemsPage.find { it.userId == users[0].userName}
        assertMetric(user0, new UserProgressMetric(
                userId: users[0].userName,
                numQuizAttempts: 2, numQuizzesPassed: 1, numQuizzesFailed: 0, numQuizzesInProgress: 1,
                numSurveys: 1, numSurveysCompleted: 0, numSurveysInProgress: 1,
                numProjects: 2, numProjectLevelsEarned: 2, numSubjectLevelsEarned: 3, numSkillsEarned: 4,
                numBadgesEarned: 1, numGlobalBadgesEarned: 2,
                userTag: "ABC1"
        ))

        def user1 = res.metricItemsPage.find { it.userId == users[1].userName}
        assertMetric(user1, new UserProgressMetric(
                userId: users[1].userName,
                numQuizAttempts: 5, numQuizzesPassed: 3, numQuizzesFailed: 2, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 1, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 1, numSkillsEarned: 1,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "ABC1"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[2].userName}, new UserProgressMetric(
                userId: users[2].userName,
                numQuizAttempts: 2, numQuizzesPassed: 1, numQuizzesFailed: 1, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 1, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 1, numSkillsEarned: 1,
                numBadgesEarned: 1, numGlobalBadgesEarned: 0,
                userTag: "KOO4"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[3].userName}, new UserProgressMetric(
                userId: users[3].userName,
                numQuizAttempts: 4, numQuizzesPassed: 3, numQuizzesFailed: 1, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 2, numProjectLevelsEarned: 2, numSubjectLevelsEarned: 2, numSkillsEarned: 2,
                numBadgesEarned: 0, numGlobalBadgesEarned: 1,
                userTag: "KOO5"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[4].userName}, new UserProgressMetric(
                userId: users[4].userName,
                numQuizAttempts: 3, numQuizzesPassed: 2, numQuizzesFailed: 1, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 1, numProjectLevelsEarned: 2, numSubjectLevelsEarned: 3, numSkillsEarned: 4,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "KOO4"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[5].userName}, new UserProgressMetric(
                userId: users[5].userName,
                numQuizAttempts: 1, numQuizzesPassed: 0, numQuizzesFailed: 0, numQuizzesInProgress: 1,
                numSurveys: 1, numSurveysCompleted: 1, numSurveysInProgress: 0,
                numProjects: 3, numProjectLevelsEarned: 15, numSubjectLevelsEarned: 30, numSkillsEarned: 31,
                numBadgesEarned: 2, numGlobalBadgesEarned: 3,
                userTag: ""
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[6].userName}, new UserProgressMetric(
                userId: users[6].userName,
                numQuizAttempts: 2, numQuizzesPassed: 1, numQuizzesFailed: 1, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "CBF2"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[7].userName}, new UserProgressMetric(
                userId: users[7].userName,
                numQuizAttempts: 0, numQuizzesPassed: 0, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 1, numProjectLevelsEarned: 1, numSubjectLevelsEarned: 2, numSkillsEarned: 2,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "XYZ1"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[8].userName}, new UserProgressMetric(
                userId: users[8].userName,
                numQuizAttempts: 1, numQuizzesPassed: 1, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "ABC2"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[9].userName}, new UserProgressMetric(
                userId: users[9].userName,
                numQuizAttempts: 1, numQuizzesPassed: 1, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "ABC1"
        ))
    }

    def "users progress from admins[1] point of view" () {
        when:
        def res = admins[1].getGlobalUserProgressMetrics("", 15, 1, "numSkillsEarned", false)
        then:
        res.numTotalProjects == 2
        res.numTotalSkills == 16

        res.numTotalQuizzes == 4
        res.numTotalSurveys == 3

        res.numTotalBadges == 4
        res.numTotalProjectBadges == 1
        res.numTotalGlobalBadges == 3

        res.numTotalMetricItems == 10
        res.metricItemsPage.size() == 10

        def user0 = res.metricItemsPage.find { it.userId == users[0].userName}
        assertMetric(user0, new UserProgressMetric(
                userId: users[0].userName,
                numQuizAttempts: 2, numQuizzesPassed: 1, numQuizzesFailed: 0, numQuizzesInProgress: 1,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 1, numProjectLevelsEarned: 1, numSubjectLevelsEarned: 1, numSkillsEarned: 1,
                numBadgesEarned: 0, numGlobalBadgesEarned: 2,
                userTag: "ABC1"
        ))

        def user1 = res.metricItemsPage.find { it.userId == users[1].userName}
        assertMetric(user1, new UserProgressMetric(
                userId: users[1].userName,
                numQuizAttempts: 2, numQuizzesPassed: 2, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "ABC1"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[2].userName}, new UserProgressMetric(
                userId: users[2].userName,
                numQuizAttempts: 1, numQuizzesPassed: 1, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "KOO4"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[3].userName}, new UserProgressMetric(
                userId: users[3].userName,
                numQuizAttempts: 2, numQuizzesPassed: 2, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 2, numProjectLevelsEarned: 2, numSubjectLevelsEarned: 2, numSkillsEarned: 2,
                numBadgesEarned: 0, numGlobalBadgesEarned: 1,
                userTag: "KOO5"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[4].userName}, new UserProgressMetric(
                userId: users[4].userName,
                numQuizAttempts: 2, numQuizzesPassed: 1, numQuizzesFailed: 1, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "KOO4"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[5].userName}, new UserProgressMetric(
                userId: users[5].userName,
                numQuizAttempts: 1, numQuizzesPassed: 0, numQuizzesFailed: 0, numQuizzesInProgress: 1,
                numSurveys: 1, numSurveysCompleted: 1, numSurveysInProgress: 0,
                numProjects: 2, numProjectLevelsEarned: 10, numSubjectLevelsEarned: 20, numSkillsEarned: 16,
                numBadgesEarned: 1, numGlobalBadgesEarned: 3,
                userTag: ""
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[6].userName}, new UserProgressMetric(
                userId: users[6].userName,
                numQuizAttempts: 2, numQuizzesPassed: 1, numQuizzesFailed: 1, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "CBF2"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[7].userName}, new UserProgressMetric(
                userId: users[7].userName,
                numQuizAttempts: 0, numQuizzesPassed: 0, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 1, numProjectLevelsEarned: 1, numSubjectLevelsEarned: 2, numSkillsEarned: 2,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "XYZ1"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[8].userName}, new UserProgressMetric(
                userId: users[8].userName,
                numQuizAttempts: 1, numQuizzesPassed: 1, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "ABC2"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[9].userName}, new UserProgressMetric(
                userId: users[9].userName,
                numQuizAttempts: 1, numQuizzesPassed: 1, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "ABC1"
        ))
    }

    def "users progress from admins[2] point of view" () {
        when:
        def res = admins[2].getGlobalUserProgressMetrics("", 15, 1, "numSkillsEarned", false)
        then:
        res.numTotalProjects == 1
        res.numTotalSkills == 9

        res.numTotalQuizzes == 3
        res.numTotalSurveys == 2

        res.numTotalBadges == 1
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 1

        res.numTotalMetricItems == 7
        res.metricItemsPage.size() == 7

        def user0 = res.metricItemsPage.find { it.userId == users[0].userName}
        assertMetric(user0, new UserProgressMetric(
                userId: users[0].userName,
                numQuizAttempts: 1, numQuizzesPassed: 0, numQuizzesFailed: 0, numQuizzesInProgress: 1,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "ABC1"
        ))

        def user1 = res.metricItemsPage.find { it.userId == users[1].userName}
        assertMetric(user1, new UserProgressMetric(
                userId: users[1].userName,
                numQuizAttempts: 1, numQuizzesPassed: 1, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "ABC1"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[3].userName}, new UserProgressMetric(
                userId: users[3].userName,
                numQuizAttempts: 1, numQuizzesPassed: 1, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 1, numProjectLevelsEarned: 1, numSubjectLevelsEarned: 1, numSkillsEarned: 1,
                numBadgesEarned: 0, numGlobalBadgesEarned: 1,
                userTag: "KOO5"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[4].userName}, new UserProgressMetric(
                userId: users[4].userName,
                numQuizAttempts:1, numQuizzesPassed: 0, numQuizzesFailed: 1, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "KOO4"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[5].userName}, new UserProgressMetric(
                userId: users[5].userName,
                numQuizAttempts: 1, numQuizzesPassed: 0, numQuizzesFailed: 0, numQuizzesInProgress: 1,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 1, numProjectLevelsEarned: 5, numSubjectLevelsEarned: 10, numSkillsEarned: 9,
                numBadgesEarned: 0, numGlobalBadgesEarned: 1,
                userTag: ""
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[6].userName}, new UserProgressMetric(
                userId: users[6].userName,
                numQuizAttempts: 1, numQuizzesPassed: 0, numQuizzesFailed: 1, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 0, numProjectLevelsEarned: 0, numSubjectLevelsEarned: 0, numSkillsEarned: 0,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "CBF2"
        ))

        assertMetric(res.metricItemsPage.find { it.userId == users[7].userName}, new UserProgressMetric(
                userId: users[7].userName,
                numQuizAttempts: 0, numQuizzesPassed: 0, numQuizzesFailed: 0, numQuizzesInProgress: 0,
                numSurveys: 0, numSurveysCompleted: 0, numSurveysInProgress: 0,
                numProjects: 1, numProjectLevelsEarned: 1, numSubjectLevelsEarned: 2, numSkillsEarned: 2,
                numBadgesEarned: 0, numGlobalBadgesEarned: 0,
                userTag: "XYZ1"
        ))
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "sort by users and page" () {
        when:
        def resPg1 = admins[0].getGlobalUserProgressMetrics("", 4, 1, "userIdForDisplay", true)
        def resPg2 = admins[0].getGlobalUserProgressMetrics("", 4, 2, "userIdForDisplay", true)
        def resPg3 = admins[0].getGlobalUserProgressMetrics("", 4, 3, "userIdForDisplay", true)

        def resPg1_descending = admins[0].getGlobalUserProgressMetrics("", 4, 1, "userIdForDisplay", false)
        def resPg2_descending = admins[0].getGlobalUserProgressMetrics("", 4, 2, "userIdForDisplay", false)
        def resPg3_descending = admins[0].getGlobalUserProgressMetrics("", 4, 3, "userIdForDisplay", false)

        List<String> userNames =
                users.collect {
                    userAttrsRepo.findByUserIdIgnoreCase(it.userName)
                }.collect { it.userIdForDisplay }.sort()
        List<String> userNamesReversed = userNames.collect { it }.reverse()
        then:
        resPg1.numTotalMetricItems == 10
        resPg1.metricItemsPage.userIdForDisplay == userNames[0..3]

        resPg2.numTotalMetricItems == 10
        resPg2.metricItemsPage.userIdForDisplay == userNames[4..7]

        resPg3.numTotalMetricItems == 10
        resPg3.metricItemsPage.userIdForDisplay == userNames[8..9]

        resPg1_descending.numTotalMetricItems == 10
        resPg1_descending.metricItemsPage.userIdForDisplay == userNamesReversed[0..3]

        resPg2_descending.numTotalMetricItems == 10
        resPg2_descending.metricItemsPage.userIdForDisplay == userNamesReversed[4..7]

        resPg3_descending.numTotalMetricItems == 10
        resPg3_descending.metricItemsPage.userIdForDisplay == userNamesReversed[8..9]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "filter by user id" () {
        when:
        def res = admins[0].getGlobalUserProgressMetrics("SeR1", 4, 1, "userIdForDisplay", true)
        def res_page2 = admins[0].getGlobalUserProgressMetrics("SeR1", 4, 2, "userIdForDisplay", true)
        def res1 = admins[0].getGlobalUserProgressMetrics("uSEr12", 4, 1, "userIdForDisplay", true)
        def res2 = admins[0].getGlobalUserProgressMetrics("USER", 4, 1, "userIdForDisplay", true)
        def res3 = admins[0].getGlobalUserProgressMetrics("!@#%^&*(", 4, 1, "userIdForDisplay", true)

        List<String> userNames =
                users.collect {
                    userAttrsRepo.findByUserIdIgnoreCase(it.userName)
                }.collect { it.userIdForDisplay }.sort()
        then:
        List<String> userIds =  ["user10 for display",
                                 "user11 for display",
                                 "user12 for display",
                                 "user13 for display",
                                 "user14 for display",
                                 "user15 for display"]
        res.metricItemsPage.userIdForDisplay == userIds[0..3]
        res.numTotalMetricItems == 6

        res_page2.metricItemsPage.userIdForDisplay == userIds[4..5]
        res_page2.numTotalMetricItems == 6

        res1.numTotalMetricItems == 1
        res1.metricItemsPage.userIdForDisplay == ["user12 for display"]

        res2.numTotalMetricItems == 10
        res2.metricItemsPage.userIdForDisplay == userNames[0..3]

        res3.numTotalMetricItems == 0
        res3.metricItemsPage == []
    }

    def "filter by user tag" () {
        when:
        def res = admins[0].getGlobalUserProgressMetrics('', 4, 1, "userTag", true, "kOo4")
        def res1 = admins[0].getGlobalUserProgressMetrics('', 4, 1, "userTag", true, "kO")
        def res2 = admins[0].getGlobalUserProgressMetrics('', 4, 1, "userTag", true, "bc1")
        def res3 = admins[0].getGlobalUserProgressMetrics('', 4, 1, "userTag", true, "bc")
        def res4_pg1 = admins[0].getGlobalUserProgressMetrics('', 3, 1, "userTag", true, "bc")
        def res4_pg2 = admins[0].getGlobalUserProgressMetrics('', 3, 2, "userTag", true, "bc")

        then:
        res.metricItemsPage.userTag == ['KOO4', 'KOO4']
        res.numTotalMetricItems == 2

        res1.metricItemsPage.userTag == ['KOO4', 'KOO4', 'KOO5']
        res1.numTotalMetricItems == 3

        res2.metricItemsPage.userTag == ['ABC1', 'ABC1', 'ABC1']
        res2.numTotalMetricItems == 3

        res3.metricItemsPage.userTag == ['ABC1', 'ABC1', 'ABC1', 'ABC2']
        res3.numTotalMetricItems == 4

        res4_pg1.metricItemsPage.userTag == ['ABC1', 'ABC1', 'ABC1']
        res4_pg1.numTotalMetricItems == 4

        res4_pg2.metricItemsPage.userTag == ['ABC2']
        res4_pg2.numTotalMetricItems == 4

    }

    def "sort by user tag" () {
        String orderBy = "userTag"
        when:
        def resPg1 = admins[0].getGlobalUserProgressMetrics("", 4, 1, orderBy, true)
        def resPg2 = admins[0].getGlobalUserProgressMetrics("", 4, 2, orderBy, true)
        def resPg3 = admins[0].getGlobalUserProgressMetrics("", 4, 3, orderBy, true)

        def resPg1_descending = admins[0].getGlobalUserProgressMetrics("", 4, 1, orderBy, false)
        def resPg2_descending = admins[0].getGlobalUserProgressMetrics("", 4, 2, orderBy, false)
        def resPg3_descending = admins[0].getGlobalUserProgressMetrics("", 4, 3, orderBy, false)

        List<String> tags =
                ['ABC1', 'ABC1', 'KOO4', 'KOO5', 'KOO4', '', 'CBF2', 'XYZ1', 'ABC2', 'ABC1'].sort().sort { a, b ->
                    if (a.isEmpty() && b.isEmpty()) return 0
                    if (a.isEmpty()) return 1
                    if (b.isEmpty()) return -1
                    a <=> b
                }
        List<String> tagsReversed = tags.collect { it }.reverse()
        then:
        resPg1.numTotalMetricItems == 10
        resPg1.metricItemsPage.userTag == tags[0..3]

        resPg2.numTotalMetricItems == 10
        resPg2.metricItemsPage.userTag == tags[4..7]

        resPg3.numTotalMetricItems == 10
        resPg3.metricItemsPage.userTag == tags[8..9]

        resPg1_descending.numTotalMetricItems == 10
        resPg1_descending.metricItemsPage.userTag == tagsReversed[0..3]

        resPg2_descending.numTotalMetricItems == 10
        resPg2_descending.metricItemsPage.userTag == tagsReversed[4..7]

        resPg3_descending.numTotalMetricItems == 10
        resPg3_descending.metricItemsPage.userTag == tagsReversed[8..9]
    }

    def "sort by numSkillsEarned" () {
        String orderBy = "numSkillsEarned"
        when:
        def resPg1 = admins[0].getGlobalUserProgressMetrics("", 4, 1, orderBy, true)
        def resPg2 = admins[0].getGlobalUserProgressMetrics("", 4, 2, orderBy, true)
        def resPg3 = admins[0].getGlobalUserProgressMetrics("", 4, 3, orderBy, true)

        def resPg1_descending = admins[0].getGlobalUserProgressMetrics("", 4, 1, orderBy, false)
        def resPg2_descending = admins[0].getGlobalUserProgressMetrics("", 4, 2, orderBy, false)
        def resPg3_descending = admins[0].getGlobalUserProgressMetrics("", 4, 3, orderBy, false)

        List<Integer> numSkillsEarnedValues = [4, 1, 1, 2, 4, 31, 0, 2, 0, 0].sort()
        List<Integer> numSkillsEarnedValuesReversed = numSkillsEarnedValues.collect { it }.reverse()
        then:
        resPg1.numTotalMetricItems == 10
        resPg1.metricItemsPage.numSkillsEarned == numSkillsEarnedValues[0..3]

        resPg2.numTotalMetricItems == 10
        resPg2.metricItemsPage.numSkillsEarned == numSkillsEarnedValues[4..7]

        resPg3.numTotalMetricItems == 10
        resPg3.metricItemsPage.numSkillsEarned == numSkillsEarnedValues[8..9]

        resPg1_descending.numTotalMetricItems == 10
        resPg1_descending.metricItemsPage.numSkillsEarned == numSkillsEarnedValuesReversed[0..3]

        resPg2_descending.numTotalMetricItems == 10
        resPg2_descending.metricItemsPage.numSkillsEarned == numSkillsEarnedValuesReversed[4..7]

        resPg3_descending.numTotalMetricItems == 10
        resPg3_descending.metricItemsPage.numSkillsEarned == numSkillsEarnedValuesReversed[8..9]
    }

    def "sort by numQuizAttempts" () {
        String orderBy = "numQuizAttempts"
        when:
        def resPg1 = admins[0].getGlobalUserProgressMetrics("", 4, 1, orderBy, true)
        def resPg2 = admins[0].getGlobalUserProgressMetrics("", 4, 2, orderBy, true)
        def resPg3 = admins[0].getGlobalUserProgressMetrics("", 4, 3, orderBy, true)

        def resPg1_descending = admins[0].getGlobalUserProgressMetrics("", 4, 1, orderBy, false)
        def resPg2_descending = admins[0].getGlobalUserProgressMetrics("", 4, 2, orderBy, false)
        def resPg3_descending = admins[0].getGlobalUserProgressMetrics("", 4, 3, orderBy, false)

        List<Integer> numQuizAttemptsValues = [2, 5, 2, 4, 3, 1, 2, 0, 1, 1].sort()
        List<Integer> numQuizAttemptsValuesReversed = numQuizAttemptsValues.collect { it }.reverse()
        then:
        resPg1.numTotalMetricItems == 10
        resPg1.metricItemsPage.numQuizAttempts == numQuizAttemptsValues[0..3]

        resPg2.numTotalMetricItems == 10
        resPg2.metricItemsPage.numQuizAttempts == numQuizAttemptsValues[4..7]

        resPg3.numTotalMetricItems == 10
        resPg3.metricItemsPage.numQuizAttempts == numQuizAttemptsValues[8..9]

        resPg1_descending.numTotalMetricItems == 10
        resPg1_descending.metricItemsPage.numQuizAttempts == numQuizAttemptsValuesReversed[0..3]

        resPg2_descending.numTotalMetricItems == 10
        resPg2_descending.metricItemsPage.numQuizAttempts == numQuizAttemptsValuesReversed[4..7]

        resPg3_descending.numTotalMetricItems == 10
        resPg3_descending.metricItemsPage.numQuizAttempts == numQuizAttemptsValuesReversed[8..9]
    }

    def "sort by numSurveys" () {
        String orderBy = "numSurveys"
        when:
        def resPg1 = admins[0].getGlobalUserProgressMetrics("", 4, 1, orderBy, true)
        def resPg2 = admins[0].getGlobalUserProgressMetrics("", 4, 2, orderBy, true)
        def resPg3 = admins[0].getGlobalUserProgressMetrics("", 4, 3, orderBy, true)

        def resPg1_descending = admins[0].getGlobalUserProgressMetrics("", 4, 1, orderBy, false)
        def resPg2_descending = admins[0].getGlobalUserProgressMetrics("", 4, 2, orderBy, false)
        def resPg3_descending = admins[0].getGlobalUserProgressMetrics("", 4, 3, orderBy, false)

        List<Integer> numSurveysValues = [1, 0, 0, 0, 0, 1, 0, 0, 0, 0].sort()
        List<Integer> numSurveysValuesReversed = numSurveysValues.collect { it }.reverse()
        then:
        resPg1.numTotalMetricItems == 10
        resPg1.metricItemsPage.numSurveys == numSurveysValues[0..3]

        resPg2.numTotalMetricItems == 10
        resPg2.metricItemsPage.numSurveys == numSurveysValues[4..7]

        resPg3.numTotalMetricItems == 10
        resPg3.metricItemsPage.numSurveys == numSurveysValues[8..9]

        resPg1_descending.numTotalMetricItems == 10
        resPg1_descending.metricItemsPage.numSurveys == numSurveysValuesReversed[0..3]

        resPg2_descending.numTotalMetricItems == 10
        resPg2_descending.metricItemsPage.numSurveys == numSurveysValuesReversed[4..7]

        resPg3_descending.numTotalMetricItems == 10
        resPg3_descending.metricItemsPage.numSurveys == numSurveysValuesReversed[8..9]
    }

    def "sort by numBadgesEarned" () {
        String orderBy = "numBadgesEarned"
        when:
        def resPg1 = admins[0].getGlobalUserProgressMetrics("", 4, 1, orderBy, true)
        def resPg2 = admins[0].getGlobalUserProgressMetrics("", 4, 2, orderBy, true)
        def resPg3 = admins[0].getGlobalUserProgressMetrics("", 4, 3, orderBy, true)

        def resPg1_descending = admins[0].getGlobalUserProgressMetrics("", 4, 1, orderBy, false)
        def resPg2_descending = admins[0].getGlobalUserProgressMetrics("", 4, 2, orderBy, false)
        def resPg3_descending = admins[0].getGlobalUserProgressMetrics("", 4, 3, orderBy, false)

        List<Integer> numBadgesEarnedValues = [1, 0, 1, 0, 0, 2, 0, 0, 0, 0].sort()
        List<Integer> numBadgesEarnedValuesReversed = numBadgesEarnedValues.collect { it }.reverse()
        then:
        resPg1.numTotalMetricItems == 10
        resPg1.metricItemsPage.numBadgesEarned == numBadgesEarnedValues[0..3]

        resPg2.numTotalMetricItems == 10
        resPg2.metricItemsPage.numBadgesEarned == numBadgesEarnedValues[4..7]

        resPg3.numTotalMetricItems == 10
        resPg3.metricItemsPage.numBadgesEarned == numBadgesEarnedValues[8..9]

        resPg1_descending.numTotalMetricItems == 10
        resPg1_descending.metricItemsPage.numBadgesEarned == numBadgesEarnedValuesReversed[0..3]

        resPg2_descending.numTotalMetricItems == 10
        resPg2_descending.metricItemsPage.numBadgesEarned == numBadgesEarnedValuesReversed[4..7]

        resPg3_descending.numTotalMetricItems == 10
        resPg3_descending.metricItemsPage.numBadgesEarned == numBadgesEarnedValuesReversed[8..9]
    }

    def "must not be able to retrieve more than 200 items"() {
        when:
        admins[0].getGlobalUserProgressMetrics("", 501, 1, "numBadgesEarned", true)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.resBody.contains("Cannot ask for more than 200 items")
    }

    @Canonical
    @ToString(includeNames = true)
    static class UserProgressMetric {
        String userId
        Integer numQuizAttempts
        Integer numQuizzesPassed
        Integer numQuizzesFailed
        Integer numQuizzesInProgress
        Integer numSurveys
        Integer numSurveysCompleted
        Integer numSurveysInProgress
        Integer numProjects
        Integer numProjectLevelsEarned
        Integer numSubjectLevelsEarned
        Integer numSkillsEarned
        Integer numBadgesEarned
        Integer numGlobalBadgesEarned
        String userTag
    }

    private void assertMetric(def row, UserProgressMetric toValidate) {
        String userIdForDisplay = userAttrsRepo.findByUserIdIgnoreCase(toValidate.userId).userIdForDisplay
        assert row.userId == toValidate.userId
        assert row.userIdForDisplay == userIdForDisplay
        assert row.numQuizAttempts == toValidate.numQuizAttempts
        assert row.numQuizzesPassed == toValidate.numQuizzesPassed
        assert row.numQuizzesFailed == toValidate.numQuizzesFailed
        assert row.numQuizzesInProgress == toValidate.numQuizzesInProgress
        assert row.numSurveys == toValidate.numSurveys
        assert row.numSurveysCompleted == toValidate.numSurveysCompleted
        assert row.numSurveysInProgress == toValidate.numSurveysInProgress
        assert row.numProjects == toValidate.numProjects
        assert row.numProjectLevelsEarned == toValidate.numProjectLevelsEarned
        assert row.numSubjectLevelsEarned == toValidate.numSubjectLevelsEarned
        assert row.numSkillsEarned == toValidate.numSkillsEarned
        assert row.numBadgesEarned == toValidate.numBadgesEarned
        assert row.numGlobalBadgesEarned == toValidate.numGlobalBadgesEarned
        assert row.userTag == toValidate.userTag
    }
}

