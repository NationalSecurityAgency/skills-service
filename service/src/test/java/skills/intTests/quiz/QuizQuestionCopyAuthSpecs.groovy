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
import skills.services.quiz.QuizQuestionType
import skills.storage.repos.QuizQuestionDefRepo

class QuizQuestionCopyAuthSpecs extends DefaultIntSpec {

    @Autowired
    QuizQuestionDefRepo quizQuestionDefRepo

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
