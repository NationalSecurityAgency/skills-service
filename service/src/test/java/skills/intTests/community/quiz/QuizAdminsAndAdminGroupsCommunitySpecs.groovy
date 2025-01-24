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
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.AdminGroupDefFactory.createAdminGroup

class QuizAdminsAndAdminGroupsCommunitySpecs extends DefaultIntSpec {

    def "do not allow to add a non-UC admin to UC quiz"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragons = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def quiz = QuizDefFactory.createQuiz(1)
        quiz.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(quiz)

        String allDragonsUserIdForDisplay = userAttrsRepo.findByUserIdIgnoreCase(allDragons.userName).userIdForDisplay.toLowerCase()

        when:
        pristineDragonsUser.addQuizUserRole(quiz.quizId, allDragons.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${allDragonsUserIdForDisplay}] is not allowed to be assigned [${RoleName.ROLE_QUIZ_ADMIN.displayName}] user role")
    }

    def "add UC admin to UC quiz"() {
        List<String> users = getRandomUsers(2)

        SkillsService anotherPristineDragon = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(anotherPristineDragon.userName, 'dragons', ['DivineDragon'])

        def quiz = QuizDefFactory.createQuiz(1)
        quiz.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(quiz)

        when:
        pristineDragonsUser.addQuizUserRole(quiz.quizId, anotherPristineDragon.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        def allAdmins = pristineDragonsUser.getQuizUserRoles(quiz.quizId)
        then:
        allAdmins.userId.sort() == [pristineDragonsUser.userName, anotherPristineDragon.userName].sort()
    }

    def "do not allow to add a non-UC Admin Group to UC quiz"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def quiz = QuizDefFactory.createQuiz(1)
        quiz.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(quiz)

        def adminGroup = createAdminGroup(1)
        pristineDragonsUser.createAdminGroupDef(adminGroup)

        when:
        pristineDragonsUser.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Admin Group [${adminGroup.name}] is not allowed to be assigned to [${quiz.name}] Quiz as the group does not have Divine Dragon permission")
    }

    def "add UC Admin Group to UC quiz"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def quiz = QuizDefFactory.createQuiz(1)
        quiz.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(quiz)

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = true
        pristineDragonsUser.createAdminGroupDef(adminGroup)

        when:
        pristineDragonsUser.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)
        def adminGroups = pristineDragonsUser.getAdminGroupsForQuiz(quiz.quizId)
        then:
        adminGroups.adminGroupId == [adminGroup.adminGroupId]
    }

    def "add UC Admin Group to non-UC quiz"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def quiz = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(quiz)

        def adminGroup = createAdminGroup(1)
        adminGroup.enableProtectedUserCommunity = true
        pristineDragonsUser.createAdminGroupDef(adminGroup)

        when:
        pristineDragonsUser.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)
        def adminGroups = pristineDragonsUser.getAdminGroupsForQuiz(quiz.quizId)
        then:
        adminGroups.adminGroupId == [adminGroup.adminGroupId]
    }
}
