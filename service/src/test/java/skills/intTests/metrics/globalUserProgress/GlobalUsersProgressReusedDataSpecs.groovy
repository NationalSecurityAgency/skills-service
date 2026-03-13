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
package skills.intTests.metrics.globalUserProgress


import groovy.transform.Canonical
import groovy.transform.ToString
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import skills.storage.model.auth.RoleName
import spock.lang.IgnoreIf

class GlobalUsersProgressReusedDataSpecs extends DefaultIntSpec {
    List<SkillsService> admins
    List<SkillsService> users
    List quizzes
    List surveys

    List attempts
    List attemptsUsers
    List attemptsQuizzes

    List<Map> p1Skills
    List<Map> p2Skills
    List<Map> p3Skills

    def setup() {
        SkillsService rootUser = createRootSkillService()
        List<String> userNames = getRandomUsers(18)
        admins = userNames[0..2].collect { createService(it) }
        users = userNames[3..12].collect { createService(it) }
        List<SkillsService> otherUsers = userNames[13..17].collect { createService(it) }
        quizzes = (1..5).collect { createQuiz(it, admins[0]) }
        quizzes[1..4].each {
            admins[0].addQuizUserRole(it.quizId, admins[1].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }
        quizzes[2..4].each {
            admins[0].addQuizUserRole(it.quizId, admins[2].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        surveys = (6..9).collect { createSurvey(it, admins[0]) }
        surveys[1..3].each {
            admins[0].addQuizUserRole(it.quizId, admins[1].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }
        surveys[2..3].each {
            admins[0].addQuizUserRole(it.quizId, admins[2].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        attempts = []
        attemptsUsers = []
        attemptsQuizzes = []

        // projects
        p1Skills = createProject(1, 5, 10, admins[0])
        p2Skills = createProject(2, 5, 2, admins[0])
        p3Skills = createProject(3, 5, 4, admins[0])

        admins[0].addProjectAdmin(p2Skills[1].projectId, admins[1].userName)
        admins[0].addProjectAdmin(p3Skills[1].projectId, admins[1].userName)

        admins[0].addProjectAdmin(p3Skills[1].projectId, admins[2].userName)

        def p1_badge1 = createBadge(1, 1, [p1Skills[0]], admins[0])
        def p2_badge1 = createBadge(2, 1, [p2Skills[0]], admins[0])

        def globalBadge1 = createGlobalBadge(10, [p1Skills[0], p2Skills[3]], [], admins[0])
        def globalBadge2 = createGlobalBadge(11, [p3Skills[0], p2Skills[4]], [], admins[0])
        def globalBadge3 = createGlobalBadge(12, [], [
                [projectId: p1Skills[0].projectId, level: 1],
                [projectId: p2Skills[3].projectId, level: 1],
        ], admins[0])

        // user 0
        assert users[0].addSkill(p1Skills[0]).body.skillApplied
        assert users[0].addSkill(p1Skills[5]).body.skillApplied
        assert users[0].addSkill(p1Skills[6]).body.skillApplied
        assert users[0].addSkill(p2Skills[3]).body.skillApplied

        runQuizOrSurvey(users[0], quizzes[1])
        runQuizOrSurvey(users[0], surveys[0], true, false)
        runQuizOrSurvey(users[0], quizzes[4], true, false)

        rootUser.saveUserTag(users[0].userName, 'dutyOrganization', ['ABC1'])

        // user 1
        assert users[1].addSkill(p1Skills[2]).body.skillApplied

        runQuizOrSurvey(users[1], quizzes[0], false)
        runQuizOrSurvey(users[1], quizzes[4], true)
        runQuizOrSurvey(users[1], quizzes[0], false)
        runQuizOrSurvey(users[1], quizzes[1])
        runQuizOrSurvey(users[1], quizzes[0], true)

        rootUser.saveUserTag(users[1].userName, 'dutyOrganization', ['ABC1'])

        // user 2 - achieved project badge but not a global badge
        assert users[2].addSkill(p1Skills[0]).body.skillApplied
        runQuizOrSurvey(users[2], quizzes[0], false)
        runQuizOrSurvey(users[2], quizzes[1])

        rootUser.saveUserTag(users[2].userName, 'dutyOrganization', ['KOO4'])

        // user 3 - achieved global badge that only has skills but not project badge
        assert users[3].addSkill(p3Skills[0]).body.skillApplied
        assert users[3].addSkill(p2Skills[4]).body.skillApplied
        runQuizOrSurvey(users[3], quizzes[0], false)
        runQuizOrSurvey(users[3], quizzes[2])
        runQuizOrSurvey(users[3], quizzes[0], true)
        runQuizOrSurvey(users[3], quizzes[1])
        rootUser.saveUserTag(users[3].userName, 'dutyOrganization', ['KOO5'])

        // user 4 - achieved global badge with level configs but not project badge
        runQuizOrSurvey(users[4], quizzes[2], false)
        runQuizOrSurvey(users[4], quizzes[0], true)
        runQuizOrSurvey(users[4], quizzes[1])
        assert users[4].addSkill(p1Skills[4]).body.skillApplied
        assert users[4].addSkill(p1Skills[5]).body.skillApplied
        assert users[4].addSkill(p1Skills[6]).body.skillApplied
        assert users[4].addSkill(p1Skills[7]).body.skillApplied
        rootUser.saveUserTag(users[4].userName, 'dutyOrganization', ['KOO4'])

        // user 5 - achieved every project level
        runQuizOrSurvey(users[5], surveys[1])
        runQuizOrSurvey(users[5], quizzes[3], true, false)
        p1Skills.each {
            assert users[5].addSkill(it).body.skillApplied
        }
        p2Skills.each {
            assert users[5].addSkill(it).body.skillApplied
        }
        p3Skills.each {
            assert users[5].addSkill(it).body.skillApplied
        }

        // users 6 - no project achievements
        runQuizOrSurvey(users[6], quizzes[3], false)
        runQuizOrSurvey(users[6], quizzes[1])
        rootUser.saveUserTag(users[6].userName, 'dutyOrganization', ['CBF2'])

        // users 7 - no quiz or surveys
        assert users[7].addSkill(p3Skills[0]).body.skillApplied
        assert users[7].addSkill(p3Skills[1]).body.skillApplied
        rootUser.saveUserTag(users[7].userName, 'dutyOrganization', ['XYZ1'])

        // user 8
        runQuizOrSurvey(users[8], quizzes[1])
        rootUser.saveUserTag(users[8].userName, 'dutyOrganization', ['ABC2'])

        // user 9
        runQuizOrSurvey(users[9], quizzes[1])
        rootUser.saveUserTag(users[9].userName, 'dutyOrganization', ['ABC1'])

        // other project, quizzes and users that must not effect the result set
        List p4Skills = createProject(4, 5, 4, otherUsers[0])
        List p5Skills = createProject(5, 5, 4, otherUsers[0])
        createGlobalBadge(20, [p4Skills[0], p5Skills[0]], [], otherUsers[0])
        createGlobalBadge(21, [], [[projectId: p4Skills[0].projectId, level: 1]], otherUsers[0])
        assert otherUsers[1].addSkill(p4Skills[0]).body.skillApplied
        assert otherUsers[2].addSkill(p4Skills[1]).body.skillApplied
        assert otherUsers[1].addSkill(p5Skills[0]).body.skillApplied
        assert otherUsers[4].addSkill(p5Skills[1]).body.skillApplied
        def otherQuiz1 = createQuiz(20, otherUsers[1] )
        def otherQuiz2 = createQuiz(21, otherUsers[1] )
        def otherSurvey = createSurvey(22, otherUsers[1] )
        runQuizOrSurvey(otherUsers[1], otherQuiz1, false)
        runQuizOrSurvey(otherUsers[1], otherQuiz1, true)
        runQuizOrSurvey(otherUsers[2], otherQuiz1, true, false)
        runQuizOrSurvey(otherUsers[2], otherQuiz2, true)
        runQuizOrSurvey(otherUsers[3], otherSurvey, true)
    }

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

    private createBadge(int projNum, int badgeNum, List skills, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def badgeREs = SkillsFactory.createBadge(projNum, badgeNum)
        serviceToUse.createBadge(badgeREs)
        skills.each {
            serviceToUse.assignSkillToBadge(it.projectId, badgeREs.badgeId, it.skillId)
        }
        badgeREs.enabled = true
        serviceToUse.updateBadge(badgeREs)

        return badgeREs
    }

    private createGlobalBadge(int badgeNum, List skills, List levelsDef = [], SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def badgeRes = SkillsFactory.createBadge(3, badgeNum)
        serviceToUse.createGlobalBadge(badgeRes)
        skills.each {
            serviceToUse.assignSkillToGlobalBadge([badgeId: badgeRes.badgeId, projectId: it.projectId, skillId: it.skillId])
        }

        levelsDef.each {
            serviceToUse.assignProjectLevelToGlobalBadge(projectId: it.projectId, badgeId: badgeRes.badgeId, level: it.level.toString())
        }

        badgeRes.enabled = true
        serviceToUse.updateGlobalBadge(badgeRes)

        return badgeRes
    }

    private createProject(int projNum, int numSkillsFirstProj = 5, int numSkillsSecondSubj = 0, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        List skillsRes = []
        def p1 = SkillsFactory.createProject(projNum)
        def p1_sub1 = SkillsFactory.createSubject(projNum, 1)
        def p1_sks1 = SkillsFactory.createSkills(numSkillsFirstProj, projNum, 1, 100)
        skillsRes.addAll(p1_sks1)
        serviceToUse.createProjectAndSubjectAndSkills(p1, p1_sub1, p1_sks1)
        if (numSkillsSecondSubj > 0) {
            def p1_sub2 = SkillsFactory.createSubject(projNum, 2)
            def p1_sks2 = SkillsFactory.createSkills(numSkillsSecondSubj, projNum, 2, 100)
            skillsRes.addAll(p1_sks2)
            serviceToUse.createProjectAndSubjectAndSkills(null, p1_sub2, p1_sks2)
        }
        return skillsRes
    }

    private def createQuiz(int num, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def quiz = QuizDefFactory.createQuiz(num)
        quiz.name = "My Quiz ${num}".toString()
        serviceToUse.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(num, 1, 2)
        serviceToUse.createQuizQuestionDef(question)
        return quiz
    }

    private def createSurvey(int num, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def survey = QuizDefFactory.createQuizSurvey(num)
        survey.name = "My Survey ${num}".toString()
        serviceToUse.createQuizDef(survey)
        def question = QuizDefFactory.createSingleChoiceSurveyQuestion(num, 1, 2)
        serviceToUse.createQuizQuestionDef(question)
        return survey
    }


    private def runQuizOrSurvey(SkillsService user, def quiz, boolean pass = true, boolean complete = true) {
        attemptsUsers.push(user)
        attemptsQuizzes.push(quiz)
        def quizInfo = user.getQuizInfo(quiz.quizId)
        def quizAttempt = user.startQuizAttempt(quiz.quizId).body
        quizAttempt.questions.eachWithIndex { it, index ->
            int answerIndex = 0
            if (it.questionType == QuizQuestionType.SingleChoice.toString()) {
                answerIndex = pass ? 0 : 1
            }
            user.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[index].answerOptions[answerIndex].id, [isSelected: true, answerText: 'This is user provided answer'])
        }

        if (complete) {
            user.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        }

        attempts.push(quizAttempt)
    }

}

