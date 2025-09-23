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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName
import skills.storage.repos.UserTagRepo

class GetQuizzesCommunitySpecs extends DefaultIntSpec {

    @Autowired
    private PlatformTransactionManager transactionManager

    @Autowired
    UserTagRepo userTagRepo

    def "get single quiz - community info is only returned for community members"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        def q1CreatedRes = pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        def q2CreatedRes = pristineDragonsUser.createQuizDef(q2)

        pristineDragonsUser.addQuizUserRole(q2.quizId, allDragonsUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        when:
        def pristineDragonUserQ1 = pristineDragonsUser.getQuizDefSummary(q1.quizId)
        def pristineDragonUserQ2 = pristineDragonsUser.getQuizDefSummary(q2.quizId)
        def pristineDragonUserQ1Def = pristineDragonsUser.getQuizDef(q1.quizId)
        def pristineDragonUserQ2Def = pristineDragonsUser.getQuizDef(q2.quizId)
        def allDragonsUserQ2 = allDragonsUser.getQuizDefSummary(q2.quizId)
        def allDragonsUserQDef2 = allDragonsUser.getQuizDef(q2.quizId)

        then:
        pristineDragonUserQ1.quizId == q1.quizId
        pristineDragonUserQ1.userCommunity == 'Divine Dragon'

        pristineDragonUserQ2.quizId == q2.quizId
        pristineDragonUserQ2.userCommunity == 'All Dragons'

        pristineDragonUserQ1Def.quizId == q1.quizId
        pristineDragonUserQ1Def.userCommunity == 'Divine Dragon'

        pristineDragonUserQ2Def.quizId == q2.quizId
        pristineDragonUserQ2Def.userCommunity == 'All Dragons'

        allDragonsUserQ2.quizId == q2.quizId
        allDragonsUserQ2.userCommunity == null
        allDragonsUserQDef2.quizId == q2.quizId
        allDragonsUserQDef2.userCommunity == null
    }

    def "user community project is not included in admin projects when admin is not a member of the UC"() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
        SkillsService pristineDragonsUser = createService(getRandomUsers(1))
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        def q1CreatedRes = pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        def q2CreatedRes = pristineDragonsUser.createQuizDef(q2)

        when:
        def quizDefsRes = pristineDragonsUser.getQuizDefs()
        transactionTemplate.execute({
            userTagRepo.deleteByUserId(pristineDragonsUser.userName)
        })
        def quizDefsRes_t1 = pristineDragonsUser.getQuizDefs()
        then:
        quizDefsRes.quizId == [q2.quizId, q1.quizId]
        quizDefsRes.userCommunity == ['All Dragons', 'Divine Dragon']

        quizDefsRes_t1.quizId == [q2.quizId]
        quizDefsRes_t1.userCommunity == [null]
    }

    def "user community quiz is not included in admin projects when root is not a member of the UC"() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(rootUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        def q1CreatedRes = rootUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        def q2CreatedRes = rootUser.createQuizDef(q2)

        transactionTemplate.execute({
            userTagRepo.deleteByUserId(rootUser.userName)
        })
        when:

        def q2Summary = rootUser.getQuizDefSummary(q2.quizId)
        def q2Def = rootUser.getQuizDef(q2.quizId)

        then:
        validateForbidden { rootUser.getQuizDefSummary(q1.quizId) }
        validateForbidden { rootUser.getQuizDef(q1.quizId) }

        q2Summary.quizId == q2.quizId
        q2Summary.userCommunity == null

        q2Def.quizId == q2.quizId
        q2Def.userCommunity == null
    }

    def "change quiz name with UC enabled"() {
        SkillsService pristineDragonsUser = createService(getRandomUsers(1))
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        def q1CreatedRes = pristineDragonsUser.createQuizDef(q1)

        when:
        q1.name = 'new name'
        def q1UpdatedRes = pristineDragonsUser.createQuizDef(q1, q1.quizId)

        then:
        q1CreatedRes.body.name == 'Test Quiz #1'
        q1UpdatedRes.body.name == 'new name'
    }

    def "user community is returned in a quiz info"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        def q1CreatedRes = pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        def q2CreatedRes = pristineDragonsUser.createQuizDef(q2)

        pristineDragonsUser.addQuizUserRole(q2.quizId, allDragonsUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        when:
        def q1Info = pristineDragonsUser.getQuizInfo(q1.quizId)
        def q2Info = pristineDragonsUser.getQuizInfo(q2.quizId)

        then:
        q1Info.userCommunity == 'Divine Dragon'
        q2Info.userCommunity == 'All Dragons'
    }

    private static boolean validateForbidden(Closure c) {
        try {
            def res = c.call()
            return false
        } catch (SkillsClientException skillsClientException) {
            return skillsClientException.httpStatus == HttpStatus.FORBIDDEN
        }
    }


}
