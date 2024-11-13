/**
 * Copyright 2020 SkillTree
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
package skills.intTests.quiz

import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.AdminGroupDefFactory.createAdminGroup

@Slf4j
class QuizRoleManagementSpecs extends DefaultIntSpec {

    def "add quiz admin"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions[0..1])

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)

        when:
        def roles_t0 = skillsService.getQuizUserRoles(quiz1.quizId)
        def questions_t0 = skillsService.getQuizQuestionDefs(quiz1.quizId)

        validateForbidden {
            otherUser.createQuizQuestionDefs(questions[2..2])
        }

        skillsService.addQuizUserRole(quiz1.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        def roles_t1 = skillsService.getQuizUserRoles(quiz1.quizId)
        otherUser.createQuizQuestionDefs(questions[2..2])
        def questions_t1 = skillsService.getQuizQuestionDefs(quiz1.quizId)

        then:
        roles_t0.userId == [skillsService.userName]
        roles_t0.roleName == [RoleName.ROLE_QUIZ_ADMIN.toString()]
        roles_t1.userId.sort() ==  [skillsService.userName, otherUser.userName].sort ()
        roles_t1.roleName == [RoleName.ROLE_QUIZ_ADMIN.toString(), RoleName.ROLE_QUIZ_ADMIN.toString()]
        questions_t0.questions.size() == 2
        questions_t1.questions.size() == 3
    }

    def "remove role"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions[0..1])

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)
        skillsService.addQuizUserRole(quiz1.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        when:
        def roles_t0 = skillsService.getQuizUserRoles(quiz1.quizId)
        skillsService.deleteQuizUserRole(quiz1.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        validateForbidden {
            otherUser.createQuizQuestionDefs(questions[2..2])
        }

        def roles_t1 = skillsService.getQuizUserRoles(quiz1.quizId)

        then:
        roles_t0.userId.sort() ==  [skillsService.userName, otherUser.userName].sort ()
        roles_t0.roleName == [RoleName.ROLE_QUIZ_ADMIN.toString(), RoleName.ROLE_QUIZ_ADMIN.toString()]
        roles_t1.userId == [skillsService.userName]
        roles_t1.roleName == [RoleName.ROLE_QUIZ_ADMIN.toString()]
    }

    def "cannot remove myself"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions[0..1])

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)
        skillsService.addQuizUserRole(quiz1.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        when:
        otherUser.deleteQuizUserRole(quiz1.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Cannot remove roles from myself")
    }

    def "cannot add myself"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions[0..1])

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)
        skillsService.addQuizUserRole(quiz1.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        when:
        otherUser.addQuizUserRole(quiz1.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Cannot add roles to myself")
    }

    def "cannot add user that belongs to admin group as local admin for this quiz"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions[0..1])

        def otherUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        createService(otherUserId)
        def adminGroup = createAdminGroup(1)
        skillsService.createAdminGroupDef(adminGroup)
        skillsService.addAdminGroupMember(adminGroup.adminGroupId, otherUserId)
        skillsService.addQuizToAdminGroup(adminGroup.adminGroupId, quiz1.quizId)

        when:
        skillsService.addQuizUserRole(quiz1.quizId, otherUserId, RoleName.ROLE_QUIZ_ADMIN.toString())

        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("User is already part of an Admin Group and cannot be added as a local admin")
    }

    def "user already have the role"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions[0..1])

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)
        skillsService.addQuizUserRole(quiz1.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        when:
        skillsService.addQuizUserRole(quiz1.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("CREATE FAILED -> user-role with quiz id ")
    }

    private boolean validateForbidden(Closure c) {
        try {
            c.call()
            return false
        } catch (SkillsClientException skillsClientException) {
            return skillsClientException.httpStatus == org.springframework.http.HttpStatus.FORBIDDEN
        }
        return false
    }


}

