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
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.createProject

class QuizDescriptionValidatorCommunitySpecs extends DefaultIntSpec {

    String notValidDefault = "has jabberwocky"
    String notValidProtectedCommunity = "has divinedragon"

    def "description validator for community without quizId"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        when:
        def defaultResValid = skillsService.checkCustomDescriptionValidation(notValidProtectedCommunity)
        def defaultResInValid = skillsService.checkCustomDescriptionValidation(notValidDefault)

        then:
        defaultResValid.body.valid
        !defaultResInValid.body.valid
        defaultResInValid.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "description validator for community with quizId"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidationWithQuizId(notValidDefault, q1.quizId)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidationWithQuizId(notValidProtectedCommunity, q1.quizId)

        def communityValidP2 = pristineDragonsUser.checkCustomDescriptionValidationWithQuizId(notValidProtectedCommunity, q2.quizId)
        def communityInvalidValidP2 = pristineDragonsUser.checkCustomDescriptionValidationWithQuizId(notValidDefault, q2.quizId)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == "May not contain divinedragon word"

        communityValidP2.body.valid
        !communityInvalidValidP2.body.valid
        communityInvalidValidP2.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "description validator for community with useProtectedCommunityValidator"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, true, null)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, true, null)

        def defaultResValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, false, null)
        def defaultResInvalid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, false, null)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == "May not contain divinedragon word"

        defaultResValid.body.valid
        !defaultResInvalid.body.valid
        defaultResInvalid.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "description validator for community with quizId overrides useProtectedCommunityValidator"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        pristineDragonsUser.createQuizDef(q2)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidationWithQuizId(notValidDefault, q1.quizId, true)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidationWithQuizId(notValidProtectedCommunity, q1.quizId, true)

        def communityValidP2 = pristineDragonsUser.checkCustomDescriptionValidationWithQuizId(notValidProtectedCommunity, q2.quizId, true)
        def communityInvalidValidP2 = pristineDragonsUser.checkCustomDescriptionValidationWithQuizId(notValidDefault, q2.quizId,true)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == "May not contain divinedragon word"

        communityValidP2.body.valid
        !communityInvalidValidP2.body.valid
        communityInvalidValidP2.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "quiz paragraph custom validation on create - UC quiz fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        q1.description = notValidProtectedCommunity


        def q2 = QuizDefFactory.createQuiz(2)
        q2.enableProtectedUserCommunity = true
        q2.description = notValidDefault
        pristineDragonsUser.createQuizDef(q2) // this fine
        when:
        pristineDragonsUser.createQuizDef(q1)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")
    }

    def "quiz paragraph custom validation on create - non-UC quiz fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.description = notValidProtectedCommunity
        pristineDragonsUser.createQuizDef(q1) // this fine

        def q2 = QuizDefFactory.createQuiz(2)
        q2.description = notValidDefault

        when:
        pristineDragonsUser.createQuizDef(q2)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("paragraphs may not contain jabberwocky")
    }

    def "quiz paragraph custom validation on edit - UC quiz fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        q1.description = notValidDefault
        pristineDragonsUser.createQuizDef(q1)

        def q2 = QuizDefFactory.createQuiz(2)
        q2.enableProtectedUserCommunity = true
        q2.description = "some"
        pristineDragonsUser.createQuizDef(q2)

        q2.description = notValidDefault
        pristineDragonsUser.createQuizDef(q2, q2.quizId) // this is fine

        when:
        q1.description = notValidProtectedCommunity
        pristineDragonsUser.createQuizDef(q1, q1.quizId)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")
    }

    def "quiz paragraph custom validation on edit - non-UC quiz fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.description = "some"
        pristineDragonsUser.createQuizDef(q1) // this fine

        def q2 = QuizDefFactory.createQuiz(2)
        q2.description = "some"
        pristineDragonsUser.createQuizDef(q2)

        q1.description = notValidProtectedCommunity
        pristineDragonsUser.createQuizDef(q1, q1.quizId) // good

        when:
        q2.description = notValidDefault
        pristineDragonsUser.createQuizDef(q2, q2.quizId)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("paragraphs may not contain jabberwocky")
    }

    def "quiz question paragraph custom validation on create - UC quiz fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        q1.description = "fine"
        pristineDragonsUser.createQuizDef(q1)
        def q1Question = QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.SingleChoice)
        q1Question.question = notValidProtectedCommunity

        def q2 = QuizDefFactory.createQuiz(2)
        q2.enableProtectedUserCommunity = true
        q2.description = notValidDefault
        pristineDragonsUser.createQuizDef(q2)
        def q2Question = QuizDefFactory.createChoiceQuestion(2, 1, 4, QuizQuestionType.SingleChoice)
        q2Question.question = notValidDefault
        pristineDragonsUser.createQuizQuestionDef(q2Question) // good

        when:
        pristineDragonsUser.createQuizQuestionDef(q1Question)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")
    }

    def "quiz question paragraph custom validation on create - non-UC quiz fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.description = "fine"
        pristineDragonsUser.createQuizDef(q1)
        def q1Question = QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.SingleChoice)
        q1Question.question = notValidDefault

        def q2 = QuizDefFactory.createQuiz(2)
        q2.description = notValidProtectedCommunity
        pristineDragonsUser.createQuizDef(q2)
        def q2Question = QuizDefFactory.createChoiceQuestion(2, 1, 4, QuizQuestionType.SingleChoice)
        q2Question.question = notValidProtectedCommunity
        pristineDragonsUser.createQuizQuestionDef(q2Question) // good

        when:
        pristineDragonsUser.createQuizQuestionDef(q1Question)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("paragraphs may not contain jabberwocky")
    }

    def "quiz question paragraph custom validation on edit - UC quiz fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        q1.description = "fine"
        pristineDragonsUser.createQuizDef(q1)
        def q1Question = QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.SingleChoice)
        q1Question.question = notValidDefault
        def q1QuestionToUpdate = pristineDragonsUser.createQuizQuestionDef(q1Question).body

        def q2 = QuizDefFactory.createQuiz(2)
        q2.enableProtectedUserCommunity = true
        q2.description = notValidDefault
        pristineDragonsUser.createQuizDef(q2)
        def q2Question = QuizDefFactory.createChoiceQuestion(2, 1, 4, QuizQuestionType.SingleChoice)
        q2Question.question = notValidDefault
        pristineDragonsUser.createQuizQuestionDef(q2Question) // good

        when:
        q1QuestionToUpdate.quizId = q1.quizId
        q1QuestionToUpdate.question = notValidProtectedCommunity
        pristineDragonsUser.updateQuizQuestionDef(q1QuestionToUpdate)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("May not contain divinedragon word")
    }

    def "quiz question paragraph custom validation on edit - non-UC quiz fails"(){
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.description = "fine"
        pristineDragonsUser.createQuizDef(q1)
        def q1Question = QuizDefFactory.createChoiceQuestion(1, 1, 4, QuizQuestionType.SingleChoice)
        q1Question.question = notValidDefault

        def q2 = QuizDefFactory.createQuiz(2)
        q2.description = notValidProtectedCommunity
        pristineDragonsUser.createQuizDef(q2)
        def q2Question = QuizDefFactory.createChoiceQuestion(2, 1, 4, QuizQuestionType.SingleChoice)
        q2Question.question = notValidProtectedCommunity
        pristineDragonsUser.createQuizQuestionDef(q2Question) // good

        when:
        pristineDragonsUser.createQuizQuestionDef(q1Question)

        then:
        def exception = thrown(SkillsClientException)
        exception.message.contains("paragraphs may not contain jabberwocky")
    }

    def "only community member can call description validator for community with quizId that belongs to that community"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def q1 = QuizDefFactory.createQuiz(1)
        q1.enableProtectedUserCommunity = true
        pristineDragonsUser.createQuizDef(q1)

        pristineDragonsUser.checkCustomDescriptionValidationWithQuizId(notValidDefault, q1.quizId) // should not fail
        when:
        skillsService.checkCustomDescriptionValidationWithQuizId(notValidDefault, q1.quizId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("User [${skillsService.userName}] is not allowed to validate using user community validation")
    }

}
