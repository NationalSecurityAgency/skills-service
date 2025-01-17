/**
 * Copyright 2024 SkillTree
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
package skills.intTests.community.quiz

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.AdminGroupDefFactory.createAdminGroup
import static skills.intTests.utils.SkillsFactory.*

class EnableCommunityForQuizValidationSpecs extends DefaultIntSpec {

    def "validation endpoint - able to enable community"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(q1)
        pristineDragonsUser.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        pristineDragonsUser.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))
        pristineDragonsUser.createQuizQuestionDef(QuizDefFactory.createTextInputQuestion(1, 3))

        when:
        def res = pristineDragonsUser.validateQuizForEnablingCommunity(q1.quizId)
        then:
        res.isAllowed == true
        !res.unmetRequirements
    }

    def "validation endpoint - cannot enable community if quiz has admin that doesn't belong to the community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)

        pristineDragonsUser.addQuizUserRole(q1.quizId, allDragonsUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        when:
        assert pristineDragonsUser.validateQuizForEnablingCommunity(q2.quizId).isAllowed == true
        def res = pristineDragonsUser.validateQuizForEnablingCommunity(q1.quizId)
        then:
        res.isAllowed == false
        res.unmetRequirements == ["Has existing ${userAttrsRepo.findByUserIdIgnoreCase(allDragonsUser.userName).userIdForDisplay} user that is not authorized"]
    }

    def "validation endpoint - cannot enable community if quiz has an admin group with an admin that doesn't belong to the community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)

        def adminGroup = createAdminGroup(1)
        pristineDragonsUser.createAdminGroupDef(adminGroup)
        pristineDragonsUser.addAdminGroupOwner(adminGroup.adminGroupId, allDragonsUser.userName)
        pristineDragonsUser.addQuizToAdminGroup(adminGroup.adminGroupId, q1.quizId)

        when:
        assert pristineDragonsUser.validateQuizForEnablingCommunity(q2.quizId).isAllowed == true
        def res = pristineDragonsUser.validateQuizForEnablingCommunity(q1.quizId)
        then:
        res.isAllowed == false
        res.unmetRequirements == ["This quiz is part of one or more Admin Groups that do no have Divine Dragon permission"]
    }

    def "validation endpoint - cannot enable community if quiz associated to skills of a non-community project"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)

        def p1 = createProject(1)
        def p1Skill = createSkill(1, 1, 1, 1, 1, 480, 200)
        p1Skill.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p1Skill.quizId = q1.quizId
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, createSubject(1, 1), [p1Skill])

        def p2 = createProject(2)
        p2.enableProtectedUserCommunity = true
        def p2Skill = createSkill(2, 1, 1, 1, 1, 480, 200)
        p2Skill.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p2Skill.quizId = q1.quizId
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, createSubject(2, 1), [p2Skill])

        when:
        assert pristineDragonsUser.validateQuizForEnablingCommunity(q2.quizId).isAllowed == true
        def res = pristineDragonsUser.validateQuizForEnablingCommunity(q1.quizId)
        then:
        res.isAllowed == false
        res.unmetRequirements == ["This quiz is linked to the following project(s) that do not have Divine Dragon permission: ${p1.projectId}".toString()]
    }

    def "validation endpoint - cannot enable community if quiz associated to skills of a non-community project - multiple projects case"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)

        def p1 = createProject(1)
        def p1Skill = createSkill(1, 1, 1, 1, 1, 480, 200)
        p1Skill.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p1Skill.quizId = q1.quizId
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, createSubject(1, 1), [p1Skill])

        def p2 = createProject(2)
        p2.enableProtectedUserCommunity = true
        def p2Skill = createSkill(2, 1, 1, 1, 1, 480, 200)
        p2Skill.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p2Skill.quizId = q1.quizId
        def p2Skill2 = createSkill(2, 1, 2, 1, 1, 480, 200)
        p2Skill2.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p2Skill2.quizId = q2.quizId
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, createSubject(2, 1), [p2Skill, p2Skill2])

        def p3 = createProject(3)
        def p3Skill1 = createSkill(3, 1, 1, 1, 1, 480, 200)
        p3Skill1.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p3Skill1.quizId = q1.quizId
        def p3Skill2 = createSkill(3, 1, 2, 1, 1, 480, 200)
        p3Skill2.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p3Skill2.quizId = q1.quizId
        pristineDragonsUser.createProjectAndSubjectAndSkills(p3, createSubject(3, 1), [p3Skill1, p3Skill2])

        def p4 = createProject(4)
        p4.enableProtectedUserCommunity = true
        def p4Skill1 = createSkill(4, 1, 1, 1, 1, 480, 200)
        p4Skill1.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p4Skill1.quizId = q1.quizId
        def p4Skill2 = createSkill(4, 1, 2, 1, 1, 480, 200)
        p4Skill2.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p4Skill2.quizId = q1.quizId
        pristineDragonsUser.createProjectAndSubjectAndSkills(p4, createSubject(4, 1), [p4Skill1, p4Skill2])

        when:
        assert pristineDragonsUser.validateQuizForEnablingCommunity(q2.quizId).isAllowed == true
        def res = pristineDragonsUser.validateQuizForEnablingCommunity(q1.quizId)
        then:
        res.isAllowed == false
        res.unmetRequirements == ["This quiz is linked to the following project(s) that do not have Divine Dragon permission: ${p1.projectId}, ${p3.projectId}".toString()]
    }

    def "validation endpoint - cannot enable community because number of requirements are not met"() {
        List<String> users = getRandomUsers(2)
        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)

        pristineDragonsUser.addQuizUserRole(q1.quizId, allDragonsUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        def p1 = createProject(1)
        def p1Skill = createSkill(1, 1, 1, 1, 1, 480, 200)
        p1Skill.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p1Skill.quizId = q1.quizId
        pristineDragonsUser.createProjectAndSubjectAndSkills(p1, createSubject(1, 1), [p1Skill])

        def p2 = createProject(2)
        p2.enableProtectedUserCommunity = true
        def p2Skill = createSkill(2, 1, 1, 1, 1, 480, 200)
        p2Skill.selfReportingType = SkillDef.SelfReportingType.Quiz.toString()
        p2Skill.quizId = q1.quizId
        pristineDragonsUser.createProjectAndSubjectAndSkills(p2, createSubject(2, 1), [p2Skill])

        when:
        assert pristineDragonsUser.validateQuizForEnablingCommunity(q2.quizId).isAllowed == true
        def res = pristineDragonsUser.validateQuizForEnablingCommunity(q1.quizId)
        then:
        res.isAllowed == false
        res.unmetRequirements.sort() == [
                "Has existing ${allDragonsUser.userName} for display user that is not authorized".toString(),
                "This quiz is linked to the following project(s) that do not have Divine Dragon permission: ${p1.projectId}".toString()
        ].sort()
    }
}
