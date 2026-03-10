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
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import spock.lang.IgnoreIf

class GlobalUsersProgressSpecs extends DefaultIntSpec {
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
        users = getRandomUsers(20).collect { createService(it) }
        users = getRandomUsers(10).collect { createService(it) }
        quizzes = (1..5).collect { createQuiz(it) }
        surveys = (6..9).collect { createSurvey(it) }

        attempts = []
        attemptsUsers = []
        attemptsQuizzes = []

        // projects
        p1Skills = createProject(1, 5, 10)
        p2Skills = createProject(2, 5, 2)
        p3Skills = createProject(3, 5, 4)

        def p1_badge1 = createBadge(1, 1, [p1Skills[0]])
        def p2_badge1 = createBadge(2, 1, [p2Skills[0]])

        def globalBadge1 = createGlobalBadge(10, [p1Skills[0], p2Skills[3]])
        def globalBadge2 = createGlobalBadge(11, [p3Skills[0], p2Skills[4]])
        def globalBadge3 = createGlobalBadge(12, [], [
                [projectId: p1Skills[0].projectId, level: 1],
                [projectId: p2Skills[3].projectId, level: 1],
        ])

        // user 0
        assert skillsService.addSkill(p1Skills[0], users[0].userName, new Date() - 4).body.skillApplied
        assert skillsService.addSkill(p1Skills[5], users[0].userName, new Date() - 4).body.skillApplied
        assert skillsService.addSkill(p1Skills[6], users[0].userName, new Date() - 4).body.skillApplied
        assert skillsService.addSkill(p2Skills[3], users[0].userName, new Date() - 4).body.skillApplied

        runQuizOrSurvey(users[0], quizzes[1])
        runQuizOrSurvey(users[0], surveys[0], true, false)
        runQuizOrSurvey(users[0], quizzes[4], true, false)

        rootUser.saveUserTag(users[0].userName, 'dutyOrganization', ['ABC1'])

        // user 1
        assert skillsService.addSkill(p1Skills[2], users[1].userName, new Date() - 1).body.skillApplied

        runQuizOrSurvey(users[1], quizzes[0], false)
        runQuizOrSurvey(users[1], quizzes[4], true)
        runQuizOrSurvey(users[1], quizzes[0], false)
        runQuizOrSurvey(users[1], quizzes[1])
        runQuizOrSurvey(users[1], quizzes[0], true)

        rootUser.saveUserTag(users[1].userName, 'dutyOrganization', ['ABC1'])

        // user 2 - achieved project badge but not a global badge
        assert skillsService.addSkill(p1Skills[0], users[2].userName, new Date()).body.skillApplied
        runQuizOrSurvey(users[2], quizzes[0], false)
        runQuizOrSurvey(users[2], quizzes[1])

        rootUser.saveUserTag(users[2].userName, 'dutyOrganization', ['KOO4'])

        // user 3 - achieved global badge that only has skills but not project badge
        assert skillsService.addSkill(p3Skills[0], users[3].userName, new Date()).body.skillApplied
        assert skillsService.addSkill(p2Skills[4], users[3].userName, new Date()).body.skillApplied
        runQuizOrSurvey(users[3], quizzes[0], false)
        runQuizOrSurvey(users[3], quizzes[2])
        runQuizOrSurvey(users[3], quizzes[0], true)
        runQuizOrSurvey(users[3], quizzes[1])
        rootUser.saveUserTag(users[3].userName, 'dutyOrganization', ['KOO5'])

        // user 4 - achieved global badge with level configs but not project badge
        runQuizOrSurvey(users[4], quizzes[2], false)
        runQuizOrSurvey(users[4], quizzes[0], true)
        runQuizOrSurvey(users[4], quizzes[1])
        assert skillsService.addSkill(p1Skills[4], users[4].userName, new Date()).body.skillApplied
        assert skillsService.addSkill(p1Skills[5], users[4].userName, new Date()).body.skillApplied
        assert skillsService.addSkill(p1Skills[6], users[4].userName, new Date()).body.skillApplied
        assert skillsService.addSkill(p1Skills[7], users[4].userName, new Date()).body.skillApplied
        rootUser.saveUserTag(users[4].userName, 'dutyOrganization', ['KOO4'])

        // user 5 - achieved every project level
        runQuizOrSurvey(users[5], surveys[1])
        runQuizOrSurvey(users[5], quizzes[3], true, false)
        p1Skills.each {
            assert skillsService.addSkill(it, users[5].userName, new Date()).body.skillApplied
        }
        p2Skills.each {
            assert skillsService.addSkill(it, users[5].userName, new Date()).body.skillApplied
        }
        p3Skills.each {
            assert skillsService.addSkill(it, users[5].userName, new Date()).body.skillApplied
        }

        // users 6 - no project achievements
        runQuizOrSurvey(users[6], quizzes[3], false)
        runQuizOrSurvey(users[6], quizzes[1])
        rootUser.saveUserTag(users[6].userName, 'dutyOrganization', ['CBF2'])

        // users 7 - no quiz or surveys
        assert skillsService.addSkill(p3Skills[0], users[7].userName, new Date()).body.skillApplied
        assert skillsService.addSkill(p3Skills[1], users[7].userName, new Date()).body.skillApplied
        rootUser.saveUserTag(users[7].userName, 'dutyOrganization', ['XYZ1'])

        // user 8
        runQuizOrSurvey(users[8], quizzes[1])
        rootUser.saveUserTag(users[8].userName, 'dutyOrganization', ['ABC2'])

        // user 9
        runQuizOrSurvey(users[9], quizzes[1])
        rootUser.saveUserTag(users[9].userName, 'dutyOrganization', ['ABC1'])
    }

    def "get empty global users progress" () {
        quizDefRepo.deleteAll()
        projDefRepo.deleteAll()
        when:
        def res = skillsService.getGlobalUserProgressMetrics()
        then:
        res.numTotalProjects == 0
        res.numTotalQuizzes == 0
        res.numTotalMetricItems == 0
        res.metricItemsPage == []
    }

    def "users progress" () {
        when:
        def res = skillsService.getGlobalUserProgressMetrics("", 15, 1, "numSkillsEarned", false)
        then:
        res.numTotalProjects == 3
        res.numTotalQuizzes == 5
        res.numTotalSurveys == 4
        res.numTotalBadges == 2
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

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "sort by users and page" () {
        when:
        def resPg1 = skillsService.getGlobalUserProgressMetrics("", 4, 1, "userIdForDisplay", true)
        def resPg2 = skillsService.getGlobalUserProgressMetrics("", 4, 2, "userIdForDisplay", true)
        def resPg3 = skillsService.getGlobalUserProgressMetrics("", 4, 3, "userIdForDisplay", true)

        def resPg1_descending = skillsService.getGlobalUserProgressMetrics("", 4, 1, "userIdForDisplay", false)
        def resPg2_descending = skillsService.getGlobalUserProgressMetrics("", 4, 2, "userIdForDisplay", false)
        def resPg3_descending = skillsService.getGlobalUserProgressMetrics("", 4, 3, "userIdForDisplay", false)

        List<String> userNames =
                users.collect {
                    userAttrsRepo.findByUserIdIgnoreCase(it.userName)
                }.collect { it.userIdForDisplay }.sort()
        List<String> userNamesReversed = userNames.collect { it }.reverse()
        then:
        resPg1.numTotalMetricItems == 10
        resPg1.metricItemsPage.size() == 4
        resPg1.metricItemsPage.userIdForDisplay == userNames[0..3]

        resPg2.numTotalMetricItems == 10
        resPg2.metricItemsPage.size() == 4
        resPg2.metricItemsPage.userIdForDisplay == userNames[4..7]

        resPg3.numTotalMetricItems == 10
        resPg3.metricItemsPage.size() == 2
        resPg3.metricItemsPage.userIdForDisplay == userNames[8..9]

        resPg1_descending.numTotalMetricItems == 10
        resPg1_descending.metricItemsPage.size() == 4
        resPg1_descending.metricItemsPage.userIdForDisplay == userNamesReversed[0..3]

        resPg2_descending.numTotalMetricItems == 10
        resPg2_descending.metricItemsPage.size() == 4
        resPg2_descending.metricItemsPage.userIdForDisplay == userNamesReversed[4..7]

        resPg3_descending.numTotalMetricItems == 10
        resPg3_descending.metricItemsPage.size() == 2
        resPg3_descending.metricItemsPage.userIdForDisplay == userNamesReversed[8..9]
    }

    @IgnoreIf({env["SPRING_PROFILES_ACTIVE"] == "pki" })
    def "filter by user id" () {
        when:
        def res = skillsService.getGlobalUserProgressMetrics("SeR1", 4, 1, "userIdForDisplay", true)
        def res1 = skillsService.getGlobalUserProgressMetrics("uSEr12", 4, 1, "userIdForDisplay", true)
        def res2 = skillsService.getGlobalUserProgressMetrics("USER", 4, 1, "userIdForDisplay", true)

        List<String> userNames =
                users.collect {
                    userAttrsRepo.findByUserIdIgnoreCase(it.userName)
                }.collect { it.userIdForDisplay }.sort()
        then:
        res.numTotalMetricItems == 3
        res.metricItemsPage.userIdForDisplay == ["user10 for display",
                                                 "user11 for display",
                                                 "user12 for display"]
        res1.numTotalMetricItems == 1
        res1.metricItemsPage.userIdForDisplay == ["user12 for display"]

        res2.numTotalMetricItems == 10
        res2.metricItemsPage.userIdForDisplay == userNames[0..3]
    }


    @Canonical
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

    private createBadge(int projNum, int badgeNum, List skills) {
        def badgeREs = SkillsFactory.createBadge(projNum, badgeNum)
        skillsService.createBadge(badgeREs)
        skills.each {
            skillsService.assignSkillToBadge(it.projectId, badgeREs.badgeId, it.skillId)
        }
        badgeREs.enabled = true
        skillsService.updateBadge(badgeREs)

        return badgeREs
    }

    private createGlobalBadge(int badgeNum, List skills, List levelsDef = []) {
        def badgeRes = SkillsFactory.createBadge(3, badgeNum)
        skillsService.createGlobalBadge(badgeRes)
        skills.each {
            skillsService.assignSkillToGlobalBadge([badgeId: badgeRes.badgeId, projectId: it.projectId, skillId: it.skillId])
        }

        levelsDef.each {
            skillsService.assignProjectLevelToGlobalBadge(projectId: it.projectId, badgeId: badgeRes.badgeId, level: it.level.toString())
        }

        badgeRes.enabled = true
        skillsService.updateGlobalBadge(badgeRes)

        return badgeRes
    }

    private createProject(int projNum, int numSkillsFirstProj = 5, int numSkillsSecondSubj = 0) {
        List skillsRes = []
        def p1 = SkillsFactory.createProject(projNum)
        def p1_sub1 = SkillsFactory.createSubject(projNum, 1)
        def p1_sks1 = SkillsFactory.createSkills(numSkillsFirstProj, projNum, 1, 100)
        skillsRes.addAll(p1_sks1)
        skillsService.createProjectAndSubjectAndSkills(p1, p1_sub1, p1_sks1)
        if (numSkillsSecondSubj > 0) {
            def p1_sub2 = SkillsFactory.createSubject(projNum, 2)
            def p1_sks2 = SkillsFactory.createSkills(numSkillsSecondSubj, projNum, 2, 100)
            skillsRes.addAll(p1_sks2)
            skillsService.createProjectAndSubjectAndSkills(null, p1_sub2, p1_sks2)
        }
        return skillsRes
    }

    private def createQuiz(int num) {
        def quiz = QuizDefFactory.createQuiz(num)
        quiz.name = "My Quiz ${num}".toString()
        skillsService.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(num, 1, 2)
        skillsService.createQuizQuestionDef(question)
        return quiz
    }

    private def createSurvey(int num) {
        def survey = QuizDefFactory.createQuizSurvey(num)
        survey.name = "My Survey ${num}".toString()
        skillsService.createQuizDef(survey)
        def question = QuizDefFactory.createSingleChoiceSurveyQuestion(num, 1, 2)
        skillsService.createQuizQuestionDef(question)
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

