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
package skills.intTests.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.services.quiz.QuizQuestionType
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName
import skills.storage.repos.QuizQuestionDefRepo

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkill
import static skills.intTests.utils.SkillsFactory.createSubject

class QuizQuestionCopyAuthSpecs extends DefaultIntSpec {

    @Autowired
    QuizQuestionDefRepo quizQuestionDefRepo

    def "non-community #callerRole cannot copy selected questions into a protected destination"() {
        setup:
        List<String> users = getRandomUsers(3)
        SkillsService owner = createService(users[0])
        SkillsService caller = callerRole == 'root' ? createRootSkillService(users[1]) : createService(users[1])
        SkillsService root = callerRole == 'root' ? caller : createRootSkillService(users[2])
        root.saveUserTag(owner.userName, 'dragons', ['DivineDragon'])
        root.saveUserTag(caller.userName, 'dragons', ['DivineDragon'])

        Map sourceQuiz = QuizDefFactory.createQuiz(1)
        Map destinationQuiz = QuizDefFactory.createQuiz(2) + [enableProtectedUserCommunity: true]
        caller.createQuizDef(sourceQuiz)
        owner.createQuizDef(destinationQuiz)
        owner.addQuizUserRole(destinationQuiz.quizId, caller.userName, 'ROLE_QUIZ_ADMIN')
        Map sourceQuestion = QuizDefFactory.createChoiceQuestion(1, 1, 2)
        sourceQuestion.question = 'Selected public source question'
        Map createdSourceQuestion = caller.createQuizQuestionDef(sourceQuestion).body
        Map destinationQuestion = QuizDefFactory.createChoiceQuestion(2, 1, 2)
        destinationQuestion.question = 'Existing protected destination question'
        Map createdDestinationQuestion = owner.createQuizQuestionDef(destinationQuestion).body

        expect: 'the caller initially has destination access and copying is allowed'
        caller.getQuizDefSummary(destinationQuiz.quizId).userCommunity == 'Divine Dragon'
        caller.validateCopyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, 1).body.isAllowed

        when: 'only community membership is revoked, retaining the admin role'
        userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(caller.userName, 'dragons'))
        caller.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [createdSourceQuestion.id])

        then:
        SkillsClientException denied = thrown()
        denied.httpStatus == HttpStatus.FORBIDDEN
        denied.message.contains('errorCode:AccessDenied')
        owner.getQuizUserRoles(destinationQuiz.quizId).any { Map role ->
            role.userId == caller.userName && role.roleName == 'ROLE_QUIZ_ADMIN'
        }

        and: 'the rejected copy did not modify either quiz'
        List<Map> destinationQuestions = owner.getQuizQuestionDefs(destinationQuiz.quizId).questions
        destinationQuestions*.id == [createdDestinationQuestion.id]
        destinationQuestions.first().question == 'Existing protected destination question'
        destinationQuestions.first().answers*.answer == ['Answer #1', 'Answer #2']
        destinationQuestions.first().answers*.isCorrect == [true, false]
        caller.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == [createdSourceQuestion.id]

        when: 'membership is restored without changing either admin role'
        root.saveUserTag(caller.userName, 'dragons', ['DivineDragon'])
        def response = caller.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [createdSourceQuestion.id])
        List<Map> restoredDestinationQuestions = caller.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then:
        response.body.success
        restoredDestinationQuestions*.question == ['Existing protected destination question', 'Selected public source question']
        restoredDestinationQuestions*.displayOrder == [0, 1]
        restoredDestinationQuestions.last().answers*.answer == ['Answer #1', 'Answer #2']
        restoredDestinationQuestions.last().answers*.isCorrect == [true, false]

        where:
        callerRole << ['admin', 'root']
    }

    def "#endpoint requires source admin access when destination admin has #sourceAccess source access"() {
        List<String> users = getRandomUsers(2)
        SkillsService sourceOwner = createService(users[0])
        SkillsService caller = createService(users[1])
        Map sourceQuiz = QuizDefFactory.createQuiz(1)
        Map destinationQuiz = QuizDefFactory.createQuiz(2)
        sourceOwner.createQuizDef(sourceQuiz)
        caller.createQuizDef(destinationQuiz)
        Map sourceQuestion = sourceOwner.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body
        caller.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(2, 1, 2))
        Map sourceBefore = sourceOwner.getQuizQuestionDefs(sourceQuiz.quizId)
        Map destinationBefore = caller.getQuizQuestionDefs(destinationQuiz.quizId)
        if (sourceAccess == 'read-only') {
            Map project = createProject(1)
            Map subject = createSubject(1, 1)
            sourceOwner.createProjectAndSubjectAndSkills(project, subject, [])
            Map skill = createSkill(1, 1, 1)
            skill.selfReportingType = SkillDef.SelfReportingType.Quiz
            skill.quizId = sourceQuiz.quizId
            sourceOwner.createSkill(skill)
            sourceOwner.addProjectAdmin(project.projectId, caller.userName)
            assert caller.getQuizQuestionDefs(sourceQuiz.quizId) == sourceBefore
            assert caller.getQuizSettings(sourceQuiz.quizId).find { Map setting ->
                setting.setting == QuizSettings.QuizUserRole.setting
            }.value == RoleName.ROLE_QUIZ_READ_ONLY.toString()
        }

        when:
        if (endpoint == 'preflight') {
            caller.validateCopyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, 1)
        } else {
            caller.copyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, [sourceQuestion.id])
        }

        then:
        SkillsClientException denied = thrown()
        denied.httpStatus == HttpStatus.FORBIDDEN
        denied.message.contains('errorCode:quiz_permission')
        caller.getQuizQuestionDefs(destinationQuiz.quizId) == destinationBefore
        sourceOwner.getQuizQuestionDefs(sourceQuiz.quizId) == sourceBefore

        where:
        endpoint    | sourceAccess
        'preflight' | 'none'
        'copy'      | 'none'
        'preflight' | 'read-only'
        'copy'      | 'read-only'
    }

    def "validating a copy requires destination quiz admin access"() {
        SkillsService sourceAdmin = createService(getRandomUsers(1)[0])
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        sourceAdmin.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(2, 1, 2))

        when: 'an admin of the source requests validation for a quiz they do not administer'
        sourceAdmin.validateCopyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, 1)

        then:
        SkillsClientException denied = thrown()
        denied.httpStatus == HttpStatus.FORBIDDEN

        when: 'the source admin is granted destination access'
        skillsService.addQuizUserRole(destinationQuiz.quizId, sourceAdmin.userName, 'ROLE_QUIZ_ADMIN')
        def validation = sourceAdmin.validateCopyQuestions(sourceQuiz.quizId, destinationQuiz.quizId, 1)

        then:
        validation.body.isAllowed
        validation.body.validationErrors.empty
    }

    def "copying questions requires destination quiz admin access"() {
        SkillsService sourceAdmin = createService(getRandomUsers(1)[0])
        def sourceQuiz = QuizDefFactory.createQuiz(1)
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        sourceAdmin.createQuizDef(sourceQuiz)
        skillsService.createQuizDef(destinationQuiz)
        def sourceQuestion = sourceAdmin.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body

        when: 'the source quiz admin tries to copy into a quiz they do not administer'
        sourceAdmin.copyQuizQuestion(sourceQuiz.quizId, destinationQuiz.quizId, [sourceQuestion.id])

        then:
        SkillsClientException denied = thrown()
        denied.httpStatus == HttpStatus.FORBIDDEN
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty

        when: 'the source admin is also granted access to the destination'
        skillsService.addQuizUserRole(destinationQuiz.quizId, sourceAdmin.userName, 'ROLE_QUIZ_ADMIN')
        def copied = sourceAdmin.copyQuizQuestion(sourceQuiz.quizId, destinationQuiz.quizId, [sourceQuestion.id])

        then:
        copied.body.success
        skillsService.getQuizQuestionDefs(destinationQuiz.quizId).questions.size() == 1
    }

    def "community-protected questions cannot be copied into an unrestricted quiz"() {
        SkillsService communityAdmin = createService(getRandomUsers(1)[0])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(communityAdmin.userName, 'dragons', ['DivineDragon'])

        def sourceQuiz = QuizDefFactory.createQuiz(1)
        sourceQuiz.enableProtectedUserCommunity = true
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        communityAdmin.createQuizDef(sourceQuiz)
        communityAdmin.createQuizDef(destinationQuiz)
        def sourceQuestion = communityAdmin.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body

        when:
        communityAdmin.copyQuizQuestion(sourceQuiz.quizId, destinationQuiz.quizId, [sourceQuestion.id])

        then:
        SkillsClientException denied = thrown()
        denied.httpStatus == HttpStatus.BAD_REQUEST
        denied.message.contains('errorCode:AccessDenied')
        denied.message.contains('Can not copy questions')
        communityAdmin.getQuizQuestionDefs(destinationQuiz.quizId).questions.empty
        communityAdmin.getQuizQuestionDefs(sourceQuiz.quizId).questions*.id == [sourceQuestion.id]
    }

    def "community-protected questions can be copied into another protected quiz"() {
        SkillsService communityAdmin = createService(getRandomUsers(1)[0])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(communityAdmin.userName, 'dragons', ['DivineDragon'])

        def sourceQuiz = QuizDefFactory.createQuiz(1)
        sourceQuiz.enableProtectedUserCommunity = true
        def destinationQuiz = QuizDefFactory.createQuiz(2)
        destinationQuiz.enableProtectedUserCommunity = true
        communityAdmin.createQuizDef(sourceQuiz)
        communityAdmin.createQuizDef(destinationQuiz)
        def sourceQuestion = communityAdmin.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 2)).body

        when:
        def response = communityAdmin.copyQuizQuestion(sourceQuiz.quizId, destinationQuiz.quizId, [sourceQuestion.id])
        def copiedQuestions = communityAdmin.getQuizQuestionDefs(destinationQuiz.quizId).questions

        then:
        response.body.success
        copiedQuestions.size() == 1
        copiedQuestions[0].id != sourceQuestion.id
        copiedQuestions[0].question == sourceQuestion.question
        copiedQuestions[0].answers*.answer == sourceQuestion.answers*.answer
        communityAdmin.getQuizDefSummary(destinationQuiz.quizId).userCommunity == 'Divine Dragon'
    }

}
