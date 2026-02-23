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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsFactory
import skills.services.quiz.QuizQuestionType

class GlobalUsersProgressSpecs extends DefaultIntSpec {

    def "get empty global users progress" () {
        when:
        def res = skillsService.getGlobalUserProgressMetrics()
        then:
        res.numTotalProjects == 0
        res.numTotalQuizzes == 0
        res.numTotalMetricItems == 0
        res.metricItemsPage == []
    }

    def "users progress" () {
        def p1 = SkillsFactory.createProject(1)
        def p1_sub1 = SkillsFactory.createSubject(1, 1)
        def p1_sks1 = SkillsFactory.createSkills(5, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1_sub1, p1_sks1)
        def p1_sub2 = SkillsFactory.createSubject(1, 2)
        def p1_sks2 = SkillsFactory.createSkills(10, 1, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(null, p1_sub2, p1_sks2)

        def p2 = SkillsFactory.createProject(2)
        def p2_sub1 = SkillsFactory.createSubject(2, 1)
        def p2_sks1 = SkillsFactory.createSkills(5, 2, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p2, p2_sub1, p2_sks1)

        def p3 = SkillsFactory.createProject(3)
        def p3_sub1 = SkillsFactory.createSubject(3, 1)
        def p3_sks1 = SkillsFactory.createSkills(5, 3, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p3, p3_sub1, p3_sks1)

        def p1_badge1 = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(p1_badge1)
        skillsService.assignSkillToBadge(p1.projectId, p1_badge1.badgeId, p1_sks1[0].skillId)
        p1_badge1.enabled = true
        skillsService.updateBadge(p1_badge1)

        def p2_badge1 = SkillsFactory.createBadge(2, 1)
        skillsService.createBadge(p2_badge1)
        skillsService.assignSkillToBadge(p2.projectId, p1_badge1.badgeId, p2_sks1[0].skillId)
        p2_badge1.enabled = true
        skillsService.updateBadge(p2_badge1)

        def globalBadge1 = SkillsFactory.createBadge(3, 10)
        skillsService.createGlobalBadge(globalBadge1)
        skillsService.assignSkillToGlobalBadge([badgeId: globalBadge1.badgeId, projectId: p1.projectId, skillId: p1_sks1[0].skillId])
        skillsService.assignSkillToGlobalBadge([badgeId: globalBadge1.badgeId, projectId: p2.projectId, skillId: p2_sks1[3].skillId])
        globalBadge1.enabled = true
        skillsService.updateGlobalBadge(globalBadge1)

        def globalBadge2 = SkillsFactory.createBadge(3, 11)
        skillsService.createGlobalBadge(globalBadge2)
        skillsService.assignSkillToGlobalBadge([badgeId: globalBadge2.badgeId, projectId: p3.projectId, skillId: p3_sks1[0].skillId])
        skillsService.assignSkillToGlobalBadge([badgeId: globalBadge2.badgeId, projectId: p2.projectId, skillId: p2_sks1[4].skillId])
        globalBadge2.enabled = true
        skillsService.updateGlobalBadge(globalBadge2)

        def globalBadge3 = SkillsFactory.createBadge(3, 12)
        skillsService.createGlobalBadge(globalBadge3)
        skillsService.assignProjectLevelToGlobalBadge(projectId: p1.projectId, badgeId: globalBadge3.badgeId, level: "1")
        skillsService.assignProjectLevelToGlobalBadge(projectId: p2.projectId, badgeId: globalBadge3.badgeId, level: "1")
        globalBadge3.enabled = true
        skillsService.updateGlobalBadge(globalBadge3)

        List<String> users = getRandomUsers(10)

        assert skillsService.addSkill(p1_sks1[0], users[0], new Date() - 4).body.skillApplied
        assert skillsService.addSkill(p1_sks2[1], users[0], new Date() - 4).body.skillApplied
        assert skillsService.addSkill(p1_sks2[2], users[0], new Date() - 4).body.skillApplied

        assert skillsService.addSkill(p2_sks1[3], users[0], new Date() - 4).body.skillApplied

        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1))

        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[0]).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, users[0])
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, users[0]).body

        def quizAttempt1 =  skillsService.startQuizAttemptForUserId(quiz.quizId, users[1]).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt1.id, quizAttempt1.questions[0].answerOptions[0].id, users[1])

        when:
        def res = skillsService.getGlobalUserProgressMetrics("", 10, 1, "numProjects", false)
        then:
        res.numTotalProjects == 3
        res.numTotalQuizzes == 1
        res.numTotalSurveys == 0
        res.numTotalBadges == 2
        res.numTotalGlobalBadges == 3

        res.numTotalMetricItems == 2
        res.metricItemsPage.size() == 2
        res.metricItemsPage[0].userId == users[0]
        res.metricItemsPage[0].numProjects == 2
        res.metricItemsPage[0].numProjectLevelsEarned == 2
        res.metricItemsPage[0].numSubjectLevelsEarned == 3
        res.metricItemsPage[0].numSkillsEarned == 4
        res.metricItemsPage[0].numBadgesEarned == 1
        res.metricItemsPage[0].numGlobalBadgesEarned == 2
        res.metricItemsPage[0].numQuizzesPassed == 1
        res.metricItemsPage[0].numQuizzesFailed == 0
        res.metricItemsPage[0].numQuizzesInProgress == 0
        res.metricItemsPage[0].surveysCompleted == 0

        res.metricItemsPage[1].userId == users[1]
        res.metricItemsPage[1].numProjects == 0
        res.metricItemsPage[1].numProjectLevelsEarned == 0
        res.metricItemsPage[1].numSubjectLevelsEarned == 0
        res.metricItemsPage[1].numSkillsEarned == 0
        res.metricItemsPage[1].numBadgesEarned == 0
        res.metricItemsPage[1].numGlobalBadgesEarned == 0
        res.metricItemsPage[1].numQuizzesPassed == 0
        res.metricItemsPage[1].numQuizzesFailed == 0
        res.metricItemsPage[1].numQuizzesInProgress == 1
        res.metricItemsPage[1].surveysCompleted == 0
    }

}

