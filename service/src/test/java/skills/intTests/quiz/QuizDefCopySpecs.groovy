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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.services.quiz.QuizQuestionType
import skills.storage.model.Attachment
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName
import skills.storage.repos.AttachmentRepo

import java.nio.file.Files

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkill
import static skills.intTests.utils.SkillsFactory.createSubject

@Slf4j
class QuizDefCopySpecs extends DefaultIntSpec {

    @Autowired
    AttachmentRepo attachmentRepo

    def "copy a quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createTextInputQuestion(1, 3))

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)
        def originalQuestions = skillsService.getQuizQuestionDefs(quiz.quizId).questions
        def copiedQuestions = skillsService.getQuizQuestionDefs(copiedQuiz.quizId).questions

        then:
        copiedQuiz.quizId == 'newQuizCopy'
        copiedQuiz.name == 'Copy of Quiz'
        quizDefSummary.numQuestions == 3
        quizDefSummary.quizId == 'newQuizCopy'
        quizDefSummary.name == 'Copy of Quiz'
        originalQuestions.size() == copiedQuestions.size()
        for(def index = 0; index < originalQuestions.size(); index++) {
            def originalQuestion = originalQuestions[index]
            def copiedQuestion = copiedQuestions[index]
            assert originalQuestion.id != copiedQuestion.id
            assert originalQuestion.question == copiedQuestion.question
            assert originalQuestion.questionType == copiedQuestion.questionType
            assert originalQuestion.answers.size() == copiedQuestion.answers.size()
            assert originalQuestion.answerHint == copiedQuestion.answerHint
            for(def answerIndex = 0; answerIndex < originalQuestion.answers.size(); answerIndex++) {
                def originalAnswer = originalQuestion.answers[answerIndex]
                def copiedAnswer = copiedQuestion.answers[answerIndex]
                assert originalAnswer.id != copiedAnswer.id
                assert originalAnswer.answer == copiedAnswer.answer
                assert originalAnswer.isCorrect == copiedAnswer.isCorrect
                assert originalAnswer.displayOrder == copiedAnswer.displayOrder
            }
        }
    }

    def "changing original quiz does not change copied quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        when:
        String originalQuiz2Name = quiz.name;
        quiz.name = "Cool New Name"
        skillsService.createQuizDef(quiz, quiz.quizId)
        def modifiedQuizDef = skillsService.getQuizDefSummary(quiz.quizId)
        def copiedQuizDef = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        modifiedQuizDef.name == 'Cool New Name'
        copiedQuizDef.name == quizDefSummary.name
    }

    def "changing copied quiz does not change original quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        when:
        skillsService.createQuizDef([quizId: copiedQuiz.quizId, name: 'Cool New Name', type: copiedQuiz.type], copiedQuiz.quizId)
        def modifiedQuizDef = skillsService.getQuizDefSummary(quiz.quizId)
        def copiedQuizDef = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        modifiedQuizDef.name == 'Test Quiz #1'
        copiedQuizDef.name == 'Cool New Name'
    }

    def "deleting original quiz does not impact copied quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        when:
        skillsService.removeQuizDef(quiz.quizId)
        def quizDefAfter = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        quizDefSummary == quizDefAfter
    }

    def "deleting copied quiz does not impact original quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(quiz.quizId)

        when:
        skillsService.removeQuizDef(copiedQuiz.quizId)
        def quizDefAfter = skillsService.getQuizDefSummary(quiz.quizId)

        then:
        quizDefSummary == quizDefAfter
    }

    def "Quiz settings are copied"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 3, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.QuizTimeLimit.setting, value: 300],
                [setting: QuizSettings.QuizLength.setting, value: 10],
                [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '3'],
        ])

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def copiedQuizInfo = skillsService.getQuizInfo(copiedQuiz.quizId)
        def copiedQuizSettings = skillsService.getQuizSettings(copiedQuiz.quizId)

        then:
        copiedQuizInfo.quizTimeLimit == 300
        copiedQuizInfo.quizLength == 10
        copiedQuizInfo.minNumQuestionsToPass == 3
        copiedQuizSettings.find( it -> it.setting == QuizSettings.QuizTimeLimit.setting ).value == '300'
        copiedQuizSettings.find( it -> it.setting == QuizSettings.QuizLength.setting ).value == '10'
        copiedQuizSettings.find( it -> it.setting == QuizSettings.MinNumQuestionsToPass.setting ).value == '3'
    }

    def "copy a survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)

        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4))
        skillsService.createQuizQuestionDef(QuizDefFactory.createTextInputQuestion(1, 3))
        skillsService.createQuizQuestionDef(QuizDefFactory.createRatingSurveyQuestion(1, 4))

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)
        def originalQuestions = skillsService.getQuizQuestionDefs(quiz.quizId).questions
        def copiedQuestions = skillsService.getQuizQuestionDefs(copiedQuiz.quizId).questions

        then:
        copiedQuiz.quizId == 'newQuizCopy'
        copiedQuiz.name == 'Copy of Quiz'
        quizDefSummary.numQuestions == 4
        quizDefSummary.quizId == 'newQuizCopy'
        quizDefSummary.name == 'Copy of Quiz'
        originalQuestions.size() == copiedQuestions.size()
        for(def index = 0; index < originalQuestions.size(); index++) {
            def originalQuestion = originalQuestions[index]
            def copiedQuestion = copiedQuestions[index]
            assert originalQuestion.id != copiedQuestion.id
            assert originalQuestion.question == copiedQuestion.question
            assert originalQuestion.questionType == copiedQuestion.questionType
            assert originalQuestion.answers.size() == copiedQuestion.answers.size()
            for(def answerIndex = 0; answerIndex < originalQuestion.answers.size(); answerIndex++) {
                def originalAnswer = originalQuestion.answers[answerIndex]
                def copiedAnswer = copiedQuestion.answers[answerIndex]
                assert originalAnswer.id != copiedAnswer.id
                assert originalAnswer.answer == copiedAnswer.answer
                assert originalAnswer.isCorrect == copiedAnswer.isCorrect
                assert originalAnswer.displayOrder == copiedAnswer.displayOrder
            }
        }
    }

    def "changing original Survey does not change copied Survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        when:
        String originalQuiz2Name = quiz.name;
        quiz.name = "Cool New Name"
        skillsService.createQuizDef(quiz, quiz.quizId)
        def modifiedQuizDef = skillsService.getQuizDefSummary(quiz.quizId)
        def copiedQuizDef = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        modifiedQuizDef.name == 'Cool New Name'
        copiedQuizDef.name == quizDefSummary.name
    }

    def "changing copied Survey does not change original Survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        when:
        skillsService.createQuizDef([quizId: copiedQuiz.quizId, name: 'Cool New Name', type: copiedQuiz.type], copiedQuiz.quizId)
        def modifiedQuizDef = skillsService.getQuizDefSummary(quiz.quizId)
        def copiedQuizDef = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        modifiedQuizDef.name == 'Test Quiz #1'
        copiedQuizDef.name == 'Cool New Name'
    }

    def "deleting original Survey does not impact copied Survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        when:
        skillsService.removeQuizDef(quiz.quizId)
        def quizDefAfter = skillsService.getQuizDefSummary(copiedQuiz.quizId)

        then:
        quizDefSummary == quizDefAfter
    }

    def "deleting copied Survey does not impact original Survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 5))
        skillsService.createQuizQuestionDef(QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def quizDefSummary = skillsService.getQuizDefSummary(quiz.quizId)

        when:
        skillsService.removeQuizDef(copiedQuiz.quizId)
        def quizDefAfter = skillsService.getQuizDefSummary(quiz.quizId)

        then:
        quizDefSummary == quizDefAfter
    }

    def "Survey settings are copied"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 2)]
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MultipleTakes.setting, value: true],
        ])

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def copiedQuizInfo = skillsService.getQuizInfo(copiedQuiz.quizId)
        def copiedQuizSettings = skillsService.getQuizSettings(copiedQuiz.quizId)

        then:
        copiedQuizInfo.multipleTakes == true
        copiedQuizSettings.find( it -> it.setting == QuizSettings.MultipleTakes.setting ).value == 'true'
    }

    def "Quiz admins are not copied"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        copiedRoles.size() == 1
        copiedRoles[0].userId == 'skills@skills.org'
    }

    def "Survey admins are not copied"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        copiedRoles.size() == 1
        copiedRoles[0].userId == 'skills@skills.org'
    }

    def "Changing quiz admins does not change admins in copied quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        when:
        users.forEach { user ->
            skillsService.deleteQuizUserRole(quiz.quizId, user, RoleName.ROLE_QUIZ_ADMIN.toString())
        }
        def updatedRoles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        updatedRoles.size() == 1
        updatedRoles[0].userId == 'skills@skills.org'
    }

    def "Changing survey admins does not change admins in copied survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        when:
        users.forEach { user ->
            skillsService.deleteQuizUserRole(quiz.quizId, user, RoleName.ROLE_QUIZ_ADMIN.toString())
        }
        def updatedRoles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        updatedRoles.size() == 1
        updatedRoles[0].userId == 'skills@skills.org'
    }

    def "Changing copied quiz admins does not change admins in original quiz"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(copiedQuiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        when:
        users.forEach { user ->
            skillsService.deleteQuizUserRole(copiedQuiz.quizId, user, RoleName.ROLE_QUIZ_ADMIN.toString())
        }
        def updatedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        updatedRoles.size() == 1
        updatedRoles[0].userId == 'skills@skills.org'
    }

    def "Changing copied survey admins does not change admins in original survey"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        List<String> users = getRandomUsers(5, true)
        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(quiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        users.forEach{ user ->
            SkillsService newUser = createService(user)
            skillsService.addQuizUserRole(copiedQuiz.quizId, newUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        }

        def roles = skillsService.getQuizUserRoles(quiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }
        def copiedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        when:
        users.forEach { user ->
            skillsService.deleteQuizUserRole(copiedQuiz.quizId, user, RoleName.ROLE_QUIZ_ADMIN.toString())
        }
        def updatedRoles = skillsService.getQuizUserRoles(copiedQuiz.quizId).findAll{ it -> it.roleName == RoleName.ROLE_QUIZ_ADMIN.toString() }

        then:
        roles.size() == 6
        updatedRoles.size() == 1
        updatedRoles[0].userId == 'skills@skills.org'
    }

    def "Copied quiz does not copy activity history"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        quiz.name = "Cool New Name"
        skillsService.createQuizDef(quiz, quiz.quizId)

        when:
        def originalHistory = skillsService.getUserActionsForQuiz(quiz.quizId)
        def copiedHistory = skillsService.getUserActionsForQuiz(copiedQuiz.quizId)

        then:
        originalHistory.data.eachWithIndex { historyItem, index ->
            assert historyItem.id != copiedHistory.data[index].id
            assert historyItem.itemId != copiedHistory.data[index].itemId
            assert historyItem.itemId == quiz.quizId
            assert copiedHistory.data[index].itemId == copiedQuiz.quizId
        }
    }

    def "Copied survey does not copy activity history"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 2)]
        skillsService.createQuizQuestionDefs(questions)

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        quiz.name = "Cool New Name"
        skillsService.createQuizDef(quiz, quiz.quizId)

        when:
        def originalHistory = skillsService.getUserActionsForQuiz(quiz.quizId)
        def copiedHistory = skillsService.getUserActionsForQuiz(copiedQuiz.quizId)

        then:
        originalHistory.data.eachWithIndex { historyItem, index ->
            assert historyItem.id != copiedHistory.data[index].id
            assert historyItem.itemId != copiedHistory.data[index].itemId
            assert historyItem.itemId == quiz.quizId
            assert copiedHistory.data[index].itemId == copiedQuiz.quizId
        }
    }

    def "Copied quiz does not copy associated skills"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        for(def x = 1; x <= 3; x++) {
            def skillWithQuiz = createSkill(1, 1, x, 1, 1, 480, 200)
            skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
            skillWithQuiz.quizId = quiz.quizId
            skillsService.createSkill(skillWithQuiz)
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        when:
        def originalQuizSkillCount = skillsService.countSkillsForQuiz(quiz.quizId)
        def copiedQuizSkillCount = skillsService.countSkillsForQuiz(copiedQuiz.quizId)

        then:
        originalQuizSkillCount == 3
        copiedQuizSkillCount == 0

    }

    def "Copied survey does not copy associated skills"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        for(def x = 1; x <= 3; x++) {
            def skillWithQuiz = createSkill(1, 1, x, 1, 1, 480, 200)
            skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
            skillWithQuiz.quizId = quiz.quizId
            skillsService.createSkill(skillWithQuiz)
        }

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        when:
        def originalQuizSkillCount = skillsService.countSkillsForQuiz(quiz.quizId)
        def copiedQuizSkillCount = skillsService.countSkillsForQuiz(copiedQuiz.quizId)

        then:
        originalQuizSkillCount == 3
        copiedQuizSkillCount == 0

    }

    def "copied quiz does not copy quiz runs"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(10, true)
        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0)
        }

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def originalRuns = skillsService.getQuizRuns(quiz.quizId)
        def copiedRuns = skillsService.getQuizRuns(copiedQuiz.quizId)

        then:
        originalRuns.data.size() == 10
        copiedRuns.data.size() == 0
    }

    def "copied survey does not copy survey runs"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 2),
                         QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 2)]
        skillsService.createQuizQuestionDefs(questions)

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        List<String> users = getRandomUsers(10, true)
        users.eachWithIndex { it, index ->
            runQuiz(it, quiz, quizInfo, index % 2 == 0)
        }

        when:
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body
        def originalRuns = skillsService.getQuizRuns(quiz.quizId)
        def copiedRuns = skillsService.getQuizRuns(copiedQuiz.quizId)

        then:
        originalRuns.data.size() == 10
        copiedRuns.data.size() == 0
    }

    def "Copied quiz creates new activity history"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        quiz.name = "Cool New Name"
        skillsService.createQuizDef(quiz, quiz.quizId)

        when:
        def copiedHistory = skillsService.getUserActionsForQuiz(copiedQuiz.quizId)
        def questionOneAttributes = skillsService.getQuizUserActionAttributes(copiedQuiz.quizId, copiedHistory.data[1].id)

        then:
        copiedHistory.totalCount == 4
        copiedHistory.data[0].action == 'Create'
        copiedHistory.data[0].item == 'Settings'
        copiedHistory.data[0].itemId == 'newQuizCopy'
        copiedHistory.data[0].quizId == copiedQuiz.quizId
        copiedHistory.data[1].action == 'Create'
        copiedHistory.data[1].item == 'Question'
        copiedHistory.data[1].itemId == 'newQuizCopy'
        copiedHistory.data[1].quizId == copiedQuiz.quizId
        copiedHistory.data[2].action == 'Create'
        copiedHistory.data[2].item == 'Question'
        copiedHistory.data[2].itemId == 'newQuizCopy'
        copiedHistory.data[2].quizId == copiedQuiz.quizId
        copiedHistory.data[3].action == 'Create'
        copiedHistory.data[3].item == 'Quiz'
        copiedHistory.data[3].itemId == 'newQuizCopy'
        copiedHistory.data[3].quizId == copiedQuiz.quizId
        questionOneAttributes.question == "This is questions #2"
        questionOneAttributes.questionType == "SingleChoice"
        questionOneAttributes["Answer1:text"] == "Answer #1"
        questionOneAttributes["Answer1:isCorrectAnswer"] == "true"
        questionOneAttributes["Answer2:text"] == "Answer #2"
        questionOneAttributes["Answer2:isCorrectAnswer"] == "false"
        questionOneAttributes["Answer3:text"] == "Answer #3"
        questionOneAttributes["Answer3:isCorrectAnswer"] == "false"
        questionOneAttributes["Answer4:text"] == "Answer #4"
        questionOneAttributes["Answer4:isCorrectAnswer"] == "false"

    }

    def "copy quiz - attachments in quiz description are duplicated"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, null, null, quiz.quizId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test2-pdf.pdf', fileContent2, null, null, quiz.quizId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()

        quiz.description = descriptionWithAttachments
        skillsService.createQuizDef(quiz, quiz.quizId)
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: descriptionWithAttachments, type: quiz.type])
        def copiedQuiz = result.body

        def originalQuiz = skillsService.getQuizDef(quiz.quizId)
        def copiedQuizRes = skillsService.getQuizDef(copiedQuiz.quizId)
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        List<Attachment> newAttachments = attachments_t2.findAll {
            !attachments_t1.find { Attachment inner -> inner.uuid == it.uuid}
        }
        then:
        originalQuiz.description == descriptionWithAttachments
        copiedQuizRes.description != descriptionWithAttachments

        attachments_t1.quizId == [quiz.quizId, quiz.quizId]
        attachments_t1.skillId == [null, null]
        attachments_t1.projectId == [null, null]
        attachments_t1.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf'].sort()

        attachments_t2.quizId.sort() == [quiz.quizId, quiz.quizId, copiedQuizRes.quizId, copiedQuizRes.quizId].sort()
        attachments_t2.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf', 'test1-pdf.pdf', 'test2-pdf.pdf'].sort()

        newAttachments.quizId == [copiedQuizRes.quizId, copiedQuizRes.quizId]
        copiedQuizRes.description.contains(newAttachments[0].uuid)
        copiedQuizRes.description.contains(newAttachments[1].uuid)
    }

    def "copy quiz - attachments in question description are duplicated"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, null, null, quiz.quizId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test2-pdf.pdf', fileContent2, null, null, quiz.quizId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()

        def question = QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice)
        question.question = descriptionWithAttachments
        skillsService.createQuizQuestionDef(question)

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def result = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizCopy', name: 'Copy of Quiz', description: '', type: quiz.type])
        def copiedQuiz = result.body

        def originalQuestions = skillsService.getQuizQuestionDefs(quiz.quizId).questions
        def copiedQuestions = skillsService.getQuizQuestionDefs(copiedQuiz.quizId).questions
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        List<Attachment> newAttachments = attachments_t2.findAll {
            !attachments_t1.find { Attachment inner -> inner.uuid == it.uuid}
        }
        then:
        originalQuestions[0].question == descriptionWithAttachments
        copiedQuestions[0].question != descriptionWithAttachments

        attachments_t1.quizId == [quiz.quizId, quiz.quizId]
        attachments_t1.skillId == [null, null]
        attachments_t1.projectId == [null, null]
        attachments_t1.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf'].sort()

        attachments_t2.quizId.sort() == [quiz.quizId, quiz.quizId, copiedQuiz.quizId, copiedQuiz.quizId].sort()
        attachments_t2.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf', 'test1-pdf.pdf', 'test2-pdf.pdf'].sort()

        newAttachments.quizId == [copiedQuiz.quizId, copiedQuiz.quizId]
        copiedQuestions[0].question.contains(newAttachments[0].uuid)
        copiedQuestions[0].question.contains(newAttachments[1].uuid)
    }

    def "a single question is copied via UI - attachments in question description are duplicated"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)

        String fileContent1 = 'Text in a file1'
        String fileContent2 = 'Text in a file2'
        def uploadedAttachmentRes = skillsService.uploadAttachment('test1-pdf.pdf', fileContent1, null, null, quiz.quizId)
        def uploadedAttachment2Res = skillsService.uploadAttachment('test2-pdf.pdf', fileContent2,null, null,  quiz.quizId)
        String attachmentHref = uploadedAttachmentRes.href
        String attachment2Href = uploadedAttachment2Res.href

        String descriptionWithAttachments =("[File1.pdf](${attachmentHref})\n" +
                "\n" +
                "## some more\n" +
                "\n" +
                "[File2.pdf](${attachment2Href})").toString()

        def question = QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice)
        question.question = descriptionWithAttachments
        skillsService.createQuizQuestionDef(question)

        when:
        List<Attachment> attachments_t1 = attachmentRepo.findAll().toList()
        def questions_t1 = skillsService.getQuizQuestionDefs(quiz.quizId).questions

        skillsService.createQuizQuestionDef(question)

        def questions_t2 = skillsService.getQuizQuestionDefs(quiz.quizId).questions
        List<Attachment> attachments_t2 = attachmentRepo.findAll().toList()
        List<Attachment> newAttachments = attachments_t2.findAll {
            !attachments_t1.find { Attachment inner -> inner.uuid == it.uuid}
        }
        then:
        questions_t1.size() == 1
        questions_t1[0].question == descriptionWithAttachments

        questions_t2.size() == 2
        questions_t2[0].question == descriptionWithAttachments
        questions_t2[1].question != descriptionWithAttachments

        attachments_t1.quizId == [quiz.quizId, quiz.quizId]
        attachments_t1.skillId == [null, null]
        attachments_t1.projectId == [null, null]
        attachments_t1.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf'].sort()

        attachments_t2.quizId.sort() == [quiz.quizId, quiz.quizId, quiz.quizId, quiz.quizId].sort()
        attachments_t2.filename.sort() == ['test1-pdf.pdf', 'test2-pdf.pdf', 'test1-pdf.pdf', 'test2-pdf.pdf'].sort()

        newAttachments.quizId == [quiz.quizId, quiz.quizId]
        questions_t2[1].question.contains(newAttachments[0].uuid)
        questions_t2[1].question.contains(newAttachments[1].uuid)
    }

    def "copy quiz - copy a quiz with a video"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        Integer questionId = question.body.id
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        when:
        skillsService.saveSkillVideoAttributes(quiz.quizId, questionId.toString(), [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
        ], true )
        def result = skillsService.getSkillVideoAttributes(quiz.quizId, questionId.toString(), true)
        assert result
        def copyResult = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizId', name: 'Copy of Quiz', description: '', type: quiz.type]).body
        assert copyResult
        def copiedQuestions = skillsService.getQuizQuestionDefs(copyResult.quizId)
        def copiedQuestion = copiedQuestions.questions[0]
        def copiedQuizVideo = skillsService.getSkillVideoAttributes(copyResult.quizId, copiedQuestion.id.toString(), true)

        then:
        copiedQuizVideo == result
    }

    def "copy quiz - deleting a copied quiz with a video does not impact the original quiz"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        Integer questionId = question.body.id
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        when:
        skillsService.saveSkillVideoAttributes(quiz.quizId, questionId.toString(), [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
        ], true )
        def result = skillsService.getSkillVideoAttributes(quiz.quizId, questionId.toString(), true)
        assert result
        def copyResult = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizId', name: 'Copy of Quiz', description: '', type: quiz.type]).body
        assert copyResult
        def copiedQuestions = skillsService.getQuizQuestionDefs(copyResult.quizId)
        def copiedQuestion = copiedQuestions.questions[0]
        def copiedQuizVideo = skillsService.getSkillVideoAttributes(copyResult.quizId, copiedQuestion.id.toString(), true)
        assert copiedQuizVideo

        skillsService.removeQuizDef(copyResult.quizId)
        def originalResult = skillsService.getSkillVideoAttributes(quiz.quizId, questionId.toString(), true)

        then:
        originalResult == result
    }

    def "copy quiz - deleting a quiz with a video that has been copied does not impact the copy"() {
        def quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        def question = skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 1, 5, QuizQuestionType.MultipleChoice))
        Integer questionId = question.body.id
        skillsService.createQuizQuestionDef(QuizDefFactory.createChoiceQuestion(1, 2, 4, QuizQuestionType.SingleChoice))

        when:
        skillsService.saveSkillVideoAttributes(quiz.quizId, questionId.toString(), [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
        ], true )
        def result = skillsService.getSkillVideoAttributes(quiz.quizId, questionId.toString(), true)
        assert result
        def copyResult = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizId', name: 'Copy of Quiz', description: '', type: quiz.type]).body
        assert copyResult
        def copiedQuestions = skillsService.getQuizQuestionDefs(copyResult.quizId)
        def copiedQuestion = copiedQuestions.questions[0]
        def copiedQuizVideo = skillsService.getSkillVideoAttributes(copyResult.quizId, copiedQuestion.id.toString(), true)
        assert copiedQuizVideo

        skillsService.removeQuizDef(quiz.quizId)
        def copiedQuizVideoAfter = skillsService.getSkillVideoAttributes(copyResult.quizId, copiedQuestion.id.toString(), true)

        then:
        copiedQuizVideo == copiedQuizVideoAfter
    }

    def "copy quiz - slides are copied"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def quiz2 = QuizDefFactory.createQuiz(3, "Fancy Description")
        skillsService.createQuizDef(quiz2)

        Resource pdfSlides = new ClassPathResource("/testSlides/test-slides-1.pdf")
        skillsService.saveQuizSlidesAttributes(quiz.quizId, [file: pdfSlides, width: 111])

        skillsService.saveQuizSlidesAttributes(quiz2.quizId, [ url: "http://some.url", width: 333])

        when:
        def quizInfo_t0 =  skillsService.getQuizInfo(quiz.quizId)
        def quizInfo2_t0 =  skillsService.getQuizInfo(quiz2.quizId)
        def copyResult = skillsService.copyQuiz(quiz.quizId, [quizId: 'newQuizId', name: 'Copy of Quiz', description: '', type: quiz.type]).body
        def copyResult2 = skillsService.copyQuiz(quiz2.quizId, [quizId: 'newQuizId2', name: 'Copy of Quiz2', description: '', type: quiz.type]).body
        def quizInfo =  skillsService.getQuizInfo(copyResult.quizId)
        def quizInfo2 =  skillsService.getQuizInfo(copyResult2.quizId)
        then:
        skillsService.downloadAttachment(quizInfo.slidesSummary.url).file.bytes == Files.readAllBytes(pdfSlides.getFile().toPath())
        quizInfo.slidesSummary.width == 111.0
        quizInfo.slidesSummary.type == "application/pdf"

        quizInfo2.slidesSummary.url == "http://some.url"
        quizInfo2.slidesSummary.width == 333.0


        quizInfo_t0.slidesSummary.url != quizInfo.slidesSummary.url
        quizInfo2_t0.slidesSummary.url == quizInfo2.slidesSummary.url
    }

    def "copy quiz - text input ai grading config is copied"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = [
                QuizDefFactory.createTextInputQuestion(1, 1),
                QuizDefFactory.createTextInputQuestion(1, 2),
                QuizDefFactory.createTextInputQuestion(1, 3)
        ]
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDefs = questions.collect { skillsService.createQuizQuestionDef(it).body}
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDefs[0].id, "Correct answer", 62)
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDefs[2].id, "Ohter answer", 50)

        def newQuiz = [quizId: 'newQuizId', name: 'Copy of Quiz', description: '', type: quiz.type]
        when:
        def copiedQuiz = skillsService.copyQuiz(quiz.quizId, newQuiz).body
        def copiedQuestions = skillsService.getQuizQuestionDefs(copiedQuiz.quizId).questions
        def aiGradingConf1_after = skillsService.getQuizTextInputAiGraderConfigs(copiedQuiz.quizId, copiedQuestions[0].id)
        def aiGradingConf2_after = skillsService.getQuizTextInputAiGraderConfigs(copiedQuiz.quizId, copiedQuestions[1].id)
        def aiGradingConf3_after = skillsService.getQuizTextInputAiGraderConfigs(copiedQuiz.quizId, copiedQuestions[2].id)

        def orig1_after = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDefs[0].id)
        def orig2_after = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDefs[1].id)
        def orig3_after = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDefs[2].id)

        then:
        aiGradingConf1_after.enabled == true
        aiGradingConf1_after.correctAnswer == "Correct answer"
        aiGradingConf1_after.minimumConfidenceLevel == 62

        aiGradingConf2_after.enabled == false
        aiGradingConf2_after.correctAnswer == null
        aiGradingConf2_after.minimumConfidenceLevel == 75 // default

        aiGradingConf3_after.enabled == true
        aiGradingConf3_after.correctAnswer == "Ohter answer"
        aiGradingConf3_after.minimumConfidenceLevel == 50

        orig1_after.enabled == true
        orig1_after.correctAnswer == "Correct answer"
        orig1_after.minimumConfidenceLevel == 62

        orig2_after.enabled == false
        orig2_after.correctAnswer == null
        orig2_after.minimumConfidenceLevel == 75 // default

        orig3_after.enabled == true
        orig3_after.correctAnswer == "Ohter answer"
        orig3_after.minimumConfidenceLevel == 50
    }

    def "copy quiz - text input ai grading config and video configs are copied"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        def questions = [
                QuizDefFactory.createTextInputQuestion(1, 1),
                QuizDefFactory.createTextInputQuestion(1, 2),
                QuizDefFactory.createTextInputQuestion(1, 3)
        ]
        def quizDef = skillsService.createQuizDef(quiz).body
        def questionDefs = questions.collect { skillsService.createQuizQuestionDef(it).body}
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDefs[0].id, "Correct answer", 62)
        skillsService.saveQuizTextInputAiGraderConfigs(quizDef.quizId, questionDefs[2].id, "Ohter answer", 50)

        skillsService.saveSkillVideoAttributes(quizDef.quizId, questionDefs[0].id.toString(), [
                videoUrl: "http://some.url",
                transcript: "transcript",
                captions: "captions",
        ], true )

        def newQuiz = [quizId: 'newQuizId', name: 'Copy of Quiz', description: '', type: quiz.type]
        when:
        def copiedQuiz = skillsService.copyQuiz(quiz.quizId, newQuiz).body
        def copiedQuestions = skillsService.getQuizQuestionDefs(copiedQuiz.quizId).questions
        def aiGradingConf1_after = skillsService.getQuizTextInputAiGraderConfigs(copiedQuiz.quizId, copiedQuestions[0].id)
        def aiGradingConf2_after = skillsService.getQuizTextInputAiGraderConfigs(copiedQuiz.quizId, copiedQuestions[1].id)
        def aiGradingConf3_after = skillsService.getQuizTextInputAiGraderConfigs(copiedQuiz.quizId, copiedQuestions[2].id)

        def copiedVideoConf1 = skillsService.getSkillVideoAttributes(copiedQuiz.quizId, copiedQuestions[0].id.toString(), true)
        boolean noQ2VidConf = false
        boolean noQ3VidConf = false
        try {
            skillsService.getSkillVideoAttributes(copiedQuiz.quizId, copiedQuestions[1].id.toString(), true)
        } catch (SkillsClientException sk) {
            noQ2VidConf = true
        }
        try {
            skillsService.getSkillVideoAttributes(copiedQuiz.quizId, copiedQuestions[2].id.toString(), true)
        } catch (SkillsClientException sk) {
            noQ3VidConf = true
        }

        def orig1_after = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDefs[0].id)
        def orig2_after = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDefs[1].id)
        def orig3_after = skillsService.getQuizTextInputAiGraderConfigs(quizDef.quizId, questionDefs[2].id)

        then:
        aiGradingConf1_after.enabled == true
        aiGradingConf1_after.correctAnswer == "Correct answer"
        aiGradingConf1_after.minimumConfidenceLevel == 62

        aiGradingConf2_after.enabled == false
        aiGradingConf2_after.correctAnswer == null
        aiGradingConf2_after.minimumConfidenceLevel == 75 // default

        aiGradingConf3_after.enabled == true
        aiGradingConf3_after.correctAnswer == "Ohter answer"
        aiGradingConf3_after.minimumConfidenceLevel == 50

        orig1_after.enabled == true
        orig1_after.correctAnswer == "Correct answer"
        orig1_after.minimumConfidenceLevel == 62

        orig2_after.enabled == false
        orig2_after.correctAnswer == null
        orig2_after.minimumConfidenceLevel == 75 // default

        orig3_after.enabled == true
        orig3_after.correctAnswer == "Ohter answer"
        orig3_after.minimumConfidenceLevel == 50

        copiedVideoConf1.videoUrl == "http://some.url"
        copiedVideoConf1.captions == "captions"
        copiedVideoConf1.transcript == "transcript"
        copiedVideoConf1.isInternallyHosted == false
        noQ2VidConf
        noQ3VidConf
    }

    void runQuiz(String userId, def quiz, def quizInfo, boolean pass) {
        def quizAttempt =  skillsService.startQuizAttemptForUserId(quiz.quizId, userId).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, userId)
        skillsService.reportQuizAnswerForUserId(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[pass ? 0 : 1].id, userId)
        skillsService.completeQuizAttemptForUserId(quiz.quizId, quizAttempt.id, userId).body
    }
}
