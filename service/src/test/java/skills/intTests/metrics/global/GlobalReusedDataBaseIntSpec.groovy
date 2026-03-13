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
import skills.services.quiz.QuizQuestionType
import skills.storage.model.auth.RoleName

class GlobalReusedDataBaseIntSpec extends DefaultIntSpec {
    List<SkillsService> admins
    List<SkillsService> users
    List quizzes
    List surveys
    List admin0ProjectIds
    List admin0QuizAndSurveyIds
    List admin1ProjectIds
    List admin1QuizAndSurveyIds
    List admin2ProjectIds
    List admin2QuizAndSurveyIds

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

        admin0ProjectIds = [p1Skills[0].projectId, p2Skills[3].projectId, p3Skills[0].projectId]
        admin0QuizAndSurveyIds = quizzes.quizId + surveys.quizId
        admin1ProjectIds = [p2Skills[1].projectId, p3Skills[1].projectId]
        admin1QuizAndSurveyIds = quizzes[1..4].quizId + surveys[1..3].quizId
        admin2ProjectIds = [p3Skills[1].projectId]
        admin2QuizAndSurveyIds = quizzes[2..4].quizId + surveys[2..3].quizId

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
        rootUser.saveUserTag(users[1].userName, 'adminOrganization', ['XYZ'])

        // user 2 - achieved project badge but not a global badge
        assert users[2].addSkill(p1Skills[0]).body.skillApplied
        runQuizOrSurvey(users[2], quizzes[0], false)
        runQuizOrSurvey(users[2], quizzes[1])

        rootUser.saveUserTag(users[2].userName, 'dutyOrganization', ['KOO4'])
        rootUser.saveUserTag(users[1].userName, 'adminOrganization', ['SKTR'])


        // user 3 - achieved global badge that only has skills but not project badge
        assert users[3].addSkill(p3Skills[0]).body.skillApplied
        assert users[3].addSkill(p2Skills[4]).body.skillApplied
        runQuizOrSurvey(users[3], quizzes[0], false)
        runQuizOrSurvey(users[3], quizzes[2])
        runQuizOrSurvey(users[3], quizzes[0], true)
        runQuizOrSurvey(users[3], quizzes[1])
        rootUser.saveUserTag(users[3].userName, 'dutyOrganization', ['KOO5'])
        rootUser.saveUserTag(users[1].userName, 'adminOrganization', ['SKTR'])

        // user 4 - achieved global badge with level configs but not project badge
        runQuizOrSurvey(users[4], quizzes[2], false)
        runQuizOrSurvey(users[4], quizzes[0], true)
        runQuizOrSurvey(users[4], quizzes[1])
        assert users[4].addSkill(p1Skills[4]).body.skillApplied
        assert users[4].addSkill(p1Skills[5]).body.skillApplied
        assert users[4].addSkill(p1Skills[6]).body.skillApplied
        assert users[4].addSkill(p1Skills[7]).body.skillApplied
        rootUser.saveUserTag(users[4].userName, 'dutyOrganization', ['KOO4'])
        rootUser.saveUserTag(users[1].userName, 'adminOrganization', ['SKTR'])

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
        rootUser.saveUserTag(users[1].userName, 'adminOrganization', ['SKTR'])

        // users 7 - no quiz or surveys
        assert users[7].addSkill(p3Skills[0]).body.skillApplied
        assert users[7].addSkill(p3Skills[1]).body.skillApplied
        rootUser.saveUserTag(users[7].userName, 'dutyOrganization', ['XYZ1'])
        rootUser.saveUserTag(users[1].userName, 'adminOrganization', ['XYZ'])

        // user 8
        runQuizOrSurvey(users[8], quizzes[1])
        rootUser.saveUserTag(users[8].userName, 'dutyOrganization', ['ABC2'])
        rootUser.saveUserTag(users[1].userName, 'adminOrganization', ['XYZ'])


        // user 9
        runQuizOrSurvey(users[9], quizzes[1])
        rootUser.saveUserTag(users[9].userName, 'dutyOrganization', ['ABC1'])
        rootUser.saveUserTag(users[1].userName, 'adminOrganization', ['XYZ'])


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

    def createBadge(int projNum, int badgeNum, List skills, SkillsService serviceToUse = null) {
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

    def createGlobalBadge(int badgeNum, List skills, List levelsDef = [], SkillsService serviceToUse = null) {
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

    def createProject(int projNum, int numSkillsFirstProj = 5, int numSkillsSecondSubj = 0, SkillsService serviceToUse = null) {
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

    def createQuiz(int num, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def quiz = QuizDefFactory.createQuiz(num)
        quiz.name = "My Quiz ${num}".toString()
        serviceToUse.createQuizDef(quiz)
        def question = QuizDefFactory.createChoiceQuestion(num, 1, 2)
        serviceToUse.createQuizQuestionDef(question)
        return quiz
    }

    def createSurvey(int num, SkillsService serviceToUse = null) {
        serviceToUse = serviceToUse ?: skillsService
        def survey = QuizDefFactory.createQuizSurvey(num)
        survey.name = "My Survey ${num}".toString()
        serviceToUse.createQuizDef(survey)
        def question = QuizDefFactory.createSingleChoiceSurveyQuestion(num, 1, 2)
        serviceToUse.createQuizQuestionDef(question)
        return survey
    }

    def runQuizOrSurvey(SkillsService user, def quiz, boolean pass = true, boolean complete = true) {
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
