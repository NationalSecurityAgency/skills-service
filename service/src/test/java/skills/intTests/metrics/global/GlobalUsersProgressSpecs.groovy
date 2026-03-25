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


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.metrics.GlobalProgressMetricsService

class GlobalUsersProgressSpecs extends DefaultIntSpec {

    def "get empty global users progress" () {
        when:
        def res = skillsService.getGlobalUserProgressMetrics()
        then:
        res.numTotalProjects == 0
        res.numTotalSkills == 0
        res.numTotalBadges == 0
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 0
        res.numTotalQuizzes == 0
        res.numTotalSurveys == 0
        res.numTotalMetricItems == 0
        res.metricItemsPage == []
    }

    def "do not return disabled skills" () {
        List<SkillsService> users = getRandomUsers(2).collect { createService(it)}
        def p1 = SkillsFactory.createProject(1)
        def p1_sub1 = SkillsFactory.createSubject(1, 1)
        def p1_sks1 = SkillsFactory.createSkills(5, 1, 1, 100)
        p1_sks1[0].enabled = false
        p1_sks1[1].enabled = false
        p1_sks1[2].enabled = false
        users[0].createProjectAndSubjectAndSkills(p1, p1_sub1, p1_sks1)
        when:
        def res = users[0].getGlobalUserProgressMetrics()
        then:
        res.numTotalProjects == 1
        res.numTotalSkills == 2
        res.numTotalBadges == 0
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 0
        res.numTotalQuizzes == 0
        res.numTotalSurveys == 0
    }

    def "do not return disabled badges" () {
        List<SkillsService> users = getRandomUsers(2).collect { createService(it)}
        def p1 = SkillsFactory.createProject(1)
        def p1_sub1 = SkillsFactory.createSubject(1, 1)
        def p1_sks1 = SkillsFactory.createSkills(5, 1, 1, 100)
        users[0].createProjectAndSubjectAndSkills(p1, p1_sub1, p1_sks1)

        def p1_bad1 = SkillsFactory.createBadge(1, 10)
        users[0].createBadge(p1_bad1)
        users[0].assignSkillToBadge([projectId: p1.projectId, badgeId: p1_bad1.badgeId, skillId: p1_sks1[0].skillId])

        when:
        def res = users[0].getGlobalUserProgressMetrics()
        then:
        res.numTotalProjects == 1
        res.numTotalSkills == 5
        res.numTotalBadges == 0
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 0
        res.numTotalQuizzes == 0
        res.numTotalSurveys == 0
    }

    def "do not return disabled global badges" () {
        List<SkillsService> users = getRandomUsers(2).collect { createService(it)}
        def p1 = SkillsFactory.createProject(1)
        def p1_sub1 = SkillsFactory.createSubject(1, 1)
        def p1_sks1 = SkillsFactory.createSkills(5, 1, 1, 100)
        users[0].createProjectAndSubjectAndSkills(p1, p1_sub1, p1_sks1)

        def p1_bad1 = SkillsFactory.createBadge(1, 10)
        users[0].createGlobalBadge(p1_bad1)
        users[0].assignSkillToGlobalBadge([projectId: p1.projectId, badgeId: p1_bad1.badgeId, skillId: p1_sks1[0].skillId])

        when:
        def res = users[0].getGlobalUserProgressMetrics()
        then:
        res.numTotalProjects == 1
        res.numTotalSkills == 5
        res.numTotalBadges == 0
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 0
        res.numTotalQuizzes == 0
        res.numTotalSurveys == 0
    }

    def "filter projects" () {
        List<SkillsService> users = getRandomUsers(2).sort().collect { createService(it)}
        def p1 = SkillsFactory.createProject(1)
        def p1_sub1 = SkillsFactory.createSubject(1, 1)
        def p1_sks1 = SkillsFactory.createSkills(5, 1, 1, 100)
        users[0].createProjectAndSubjectAndSkills(p1, p1_sub1, p1_sks1)

        def p2 = SkillsFactory.createProject(2)
        def p2_sub1 = SkillsFactory.createSubject(2, 1)
        def p2_sks1 = SkillsFactory.createSkills(5, 2, 1, 100)
        users[0].createProjectAndSubjectAndSkills(p2, p2_sub1, p2_sks1)

        assert users[0].addSkill(p1_sks1[0]).body.skillApplied
        assert users[0].addSkill(p1_sks1[1]).body.skillApplied
        assert users[1].addSkill(p2_sks1[0]).body.skillApplied

        when:
        def res_before = users[0].getGlobalUserProgressMetrics()
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [
                        setting  : GlobalProgressMetricsService.USER_PREF_GLOBAL_METRICS_EXCLUSION,
                        value    : true,
                        projectId: p1.projectId,
                        quizId: null
                ]
        ])
        def res = users[0].getGlobalUserProgressMetrics()
        then:
        res_before.numTotalProjects == 2
        res_before.numExcludedProjects == 0
        res_before.numTotalSkills == 10
        res_before.numTotalBadges == 0
        res_before.numTotalProjectBadges == 0
        res_before.numTotalGlobalBadges == 0
        res_before.numExcludedQuizzesAndSurveys == 0
        res_before.numTotalQuizzes == 0
        res_before.numTotalSurveys == 0

        res_before.numTotalMetricItems == 2
        res_before.metricItemsPage.userId == [users[0].userName, users[1].userName]

        res.numTotalProjects == 1
        res.numExcludedProjects == 1
        res.numTotalSkills == 5
        res.numTotalBadges == 0
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 0
        res.numExcludedQuizzesAndSurveys == 0
        res.numTotalQuizzes == 0
        res.numTotalSurveys == 0

        res.numTotalMetricItems == 1
        res.metricItemsPage.userId == [users[1].userName]

    }

    def "filter quizzes" () {
        List<SkillsService> users = getRandomUsers(2).collect { createService(it)}
        def quiz1 = QuizDefFactory.createQuiz(1)
        users[0].createQuizDef(quiz1)
        def question = QuizDefFactory.createChoiceQuestion(1, 1, 2)
        users[0].createQuizQuestionDef(question)

        def quiz2 = QuizDefFactory.createQuiz(2)
        users[0].createQuizDef(quiz2)
        def question2_q2 = QuizDefFactory.createChoiceQuestion(2, 1, 2)
        users[0].createQuizQuestionDef(question2_q2)

        def quizAttempt1 = users[0].startQuizAttempt(quiz1.quizId).body
        users[0].reportQuizAnswer(quiz1.quizId, quizAttempt1.id, quizAttempt1.questions[0].answerOptions[0].id)
        users[0].completeQuizAttempt(quiz1.quizId, quizAttempt1.id).body

        def quizAttempt2 = users[1].startQuizAttempt(quiz2.quizId).body
        users[1].reportQuizAnswer(quiz2.quizId, quizAttempt2.id, quizAttempt2.questions[0].answerOptions[0].id)
        users[1].completeQuizAttempt(quiz2.quizId, quizAttempt2.id).body

        when:
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [
                        setting  : GlobalProgressMetricsService.USER_PREF_GLOBAL_METRICS_EXCLUSION,
                        value    : true,
                        quizId: quiz1.quizId
                ]
        ])
        def res = users[0].getGlobalUserProgressMetrics()
        then:
        res.numTotalProjects == 0
        res.numExcludedProjects == 0
        res.numTotalSkills == 0
        res.numTotalBadges == 0
        res.numTotalProjectBadges == 0
        res.numTotalGlobalBadges == 0
        res.numExcludedQuizzesAndSurveys == 1
        res.numTotalQuizzes == 1
        res.numTotalSurveys == 0

        res.numTotalMetricItems == 1
        res.metricItemsPage.userId == [users[1].userName]
    }

}

