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
import skills.quizLoading.QuizSettings
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.AdminGroupDefFactory.createAdminGroup

class ConfigureCommunityForQuizSpecs extends DefaultIntSpec {

    def "do not allow to enable community for a quiz through settings endpoint"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def quiz = QuizDefFactory.createQuiz(1)
        pristineDragonsUser.createQuizDef(quiz)

        when:
        pristineDragonsUser.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.UserCommunityOnlyQuiz.setting, value: 'true'],
        ])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Not allowed to save [${QuizSettings.UserCommunityOnlyQuiz.setting}] setting using this endpoint")
    }

    def "configure community when creating a quiz"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true

        def q2 = QuizDefFactory.createQuiz(2)

        when:
        def q1CreatedRes = pristineDragonsUser.createQuizDef(q1)
        def q2CreatedRes = pristineDragonsUser.createQuizDef(q2)
        def quizDefsRes = pristineDragonsUser.getQuizDefs()
        def quizSummaryRes1 = pristineDragonsUser.getQuizDefSummary(q1.quizId)
        def quizSummaryRes2 = pristineDragonsUser.getQuizDefSummary(q2.quizId)

        pristineDragonsUser.addQuizUserRole(q2.quizId, allDragonsUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        def quizDefsRes_allDragonsUser = allDragonsUser.getQuizDefs()
        def quizSummaryRes_allDragonsUser = allDragonsUser.getQuizDefSummary(q2.quizId)

        then:
        quizDefsRes.quizId == [q2.quizId, q1.quizId]
        quizDefsRes.userCommunity == ['All Dragons', 'Divine Dragon']
        quizSummaryRes1.quizId == q1.quizId
        quizSummaryRes1.userCommunity == 'Divine Dragon'
        quizSummaryRes2.quizId == q2.quizId
        quizSummaryRes2.userCommunity == 'All Dragons'
        q1CreatedRes.body.userCommunity == 'Divine Dragon'
        q2CreatedRes.body.userCommunity == 'All Dragons'

        quizDefsRes_allDragonsUser.quizId == [q2.quizId]
        quizDefsRes_allDragonsUser.userCommunity == [null]
        quizSummaryRes_allDragonsUser.quizId == q2.quizId
        quizSummaryRes_allDragonsUser.userCommunity == null
    }

    def "community is null for a non-community user"() {
        def q1 = QuizDefFactory.createQuiz(1)
        def q2 = QuizDefFactory.createQuiz(2)

        when:
        def q1CreatedRes = skillsService.createQuizDef(q1)
        def q2CreatedRes = skillsService.createQuizDef(q2)
        def quizDefsRes = skillsService.getQuizDefs()
        def quizSummaryRes1 = skillsService.getQuizDefSummary(q1.quizId)
        def quizSummaryRes2 = skillsService.getQuizDefSummary(q2.quizId)
        then:
        quizDefsRes.quizId == [q2.quizId, q1.quizId]
        quizDefsRes.userCommunity == [null, null]
        q1CreatedRes.body.userCommunity == null
        q2CreatedRes.body.userCommunity == null
        quizSummaryRes1.quizId == q1.quizId
        quizSummaryRes1.userCommunity == null
        quizSummaryRes2.quizId == q2.quizId
        quizSummaryRes2.userCommunity == null
    }

    def "only member of the community can enable quiz "() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])

        def quiz = QuizDefFactory.createQuiz(1)
        quiz.enableProtectedUserCommunity = true
        when:
        allDragonsUser.createQuizDef(quiz)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${allDragonsUser.userName}] is not allowed to set [enableProtectedUserCommunity] to true")
    }

    def "configure community when editing a quiz"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        def q1CreatedRes_t1 = pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        def q2CreatedRes = pristineDragonsUser.createQuizDef(q2)

        when:
        def pristineDragonUserQuizzes_before = pristineDragonsUser.getQuizDefs()
        q1.enableProtectedUserCommunity = true
        def q1CreatedRes_t2 = pristineDragonsUser.createQuizDef(q1, q1.quizId)
        def pristineDragonUserQuizzes_after = pristineDragonsUser.getQuizDefs()
        then:
        pristineDragonUserQuizzes_before.quizId == [q2.quizId, q1.quizId]
        pristineDragonUserQuizzes_before.userCommunity == ['All Dragons', 'All Dragons']

        pristineDragonUserQuizzes_after.quizId == [q2.quizId, q1.quizId]
        pristineDragonUserQuizzes_after.userCommunity == ['All Dragons', 'Divine Dragon']

        q1CreatedRes_t1.body.userCommunity == 'All Dragons'
        q1CreatedRes_t2.body.userCommunity == 'Divine Dragon'
        q2CreatedRes.body.userCommunity == 'All Dragons'
    }

    def "only member of the community can enable community when editing a quiz "() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])

        def quiz = QuizDefFactory.createQuiz(1)
        allDragonsUser.createQuizDef(quiz)

        when:
        quiz.enableProtectedUserCommunity = true
        allDragonsUser.createQuizDef(quiz, quiz.quizId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${allDragonsUser.userName}] is not allowed to set [enableProtectedUserCommunity] to true")
    }

    def "only member of the community can enable community when editing a quiz - quiz admin comes from Admin Group"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])

        def quiz = QuizDefFactory.createQuiz(1)
        allDragonsUser.createQuizDef(quiz)

        def adminGroup = createAdminGroup(1)
        allDragonsUser.createAdminGroupDef(adminGroup)
        allDragonsUser.addAdminGroupOwner(adminGroup.adminGroupId, allDragonsUser.userName)
        allDragonsUser.addQuizToAdminGroup(adminGroup.adminGroupId, quiz.quizId)

        when:
        quiz.enableProtectedUserCommunity = true
        allDragonsUser.createQuizDef(quiz, quiz.quizId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("User [${allDragonsUser.userName}] is not allowed to set [enableProtectedUserCommunity] to true")
    }

    def "once community is enabled it cannot be disabled"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1)
        when:
        q1.enableProtectedUserCommunity = false
        pristineDragonsUser.createQuizDef(q1, q1.quizId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Once quiz [enableProtectedUserCommunity=true] it cannot be flipped to false")
        e.getMessage().contains("quizId:${q1.quizId}")
    }

    def "cannot enable protected community for a quiz that has admin that does not belong to that community"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        allDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        allDragonsUser.createQuizDef(q2)

        allDragonsUser.addQuizUserRole(q1.quizId, pristineDragonsUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        allDragonsUser.addQuizUserRole(q2.quizId, pristineDragonsUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        when:
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1, q1.quizId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Not Allowed to set [enableProtectedUserCommunity] to true")
        e.message.contains("Has existing ${userAttrsRepo.findByUserIdIgnoreCase(allDragonsUser.userName).userIdForDisplay} user that is not authorized")
    }

    def "cannot enable protected community for a quiz that has admin group with a non-UC user"() {
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
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1, q1.quizId)

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.getMessage().contains("Not Allowed to set [enableProtectedUserCommunity] to true")
        e.message.contains("This quiz is part of one or more Admin Groups that do no have Divine Dragon permission")
    }

    def "run community specific paragraph validation for project's description - project creation with community"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        String notAllowedProtectedDesc = "has divinedragon"
        String notAllowedForNonProtectedDesc = "has jabberwocky"
        when:


        def protectedWithNotAllowedDesc  = QuizDefFactory.createQuiz(1)
        protectedWithNotAllowedDesc.enableProtectedUserCommunity = true
        protectedWithNotAllowedDesc.description = notAllowedProtectedDesc

        def protectedWithAllowedDesc  = QuizDefFactory.createQuiz(2)
        protectedWithAllowedDesc.enableProtectedUserCommunity = true
        protectedWithAllowedDesc.description = notAllowedForNonProtectedDesc

        def notProtectedWithNotAllowedDesc  = QuizDefFactory.createQuiz(3)
        notProtectedWithNotAllowedDesc.description = notAllowedForNonProtectedDesc

        def notProtectedWithAllowedDesc  = QuizDefFactory.createQuiz(4)
        notProtectedWithAllowedDesc.description = notAllowedProtectedDesc

        pristineDragonsUser.createQuizDef(protectedWithAllowedDesc)
        pristineDragonsUser.createQuizDef(notProtectedWithAllowedDesc)

        then:
        expectErrWithMsg ({pristineDragonsUser.createQuizDef(protectedWithNotAllowedDesc) }, "May not contain divinedragon word")
        expectErrWithMsg ({pristineDragonsUser.createQuizDef(notProtectedWithNotAllowedDesc) }, "paragraphs may not contain jabberwocky")
    }

    def expectErrWithMsg(Closure c, String msg) {
        boolean res = false
        try {
            c.call()
            res = false // should not get here
        } catch (SkillsClientException e) {
            res = e.message.contains(msg)
        }

        return res
    }
}
