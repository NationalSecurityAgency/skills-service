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


import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.SkillsFactory.*

class QuizApi_AuthSpecs extends DefaultIntSpec {

    def "start quiz attempt - user must project admin (in case skill-quiz association) OR quiz admin in order to supply user id - not authorized user"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        SkillsService otherUser = super.createService("otherUser")

        when:
        def quizAttempt =  otherUser.startQuizAttempt(quiz.quizId, skillsService.userName).body
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Access Denied")
        skillsClientException.httpStatus == HttpStatus.FORBIDDEN
    }

    def "report question - user must project admin (in case skill-quiz association) OR quiz admin in order to supply user id - not authorized user"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        SkillsService otherUser = super.createService("otherUser")

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId, skillsService.userName).body
        when:
        otherUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected:true, userId: skillsService.userName])
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Access Denied")
        skillsClientException.httpStatus == HttpStatus.FORBIDDEN
    }

    def "complete quiz - user must project admin (in case skill-quiz association) OR quiz admin in order to supply user id - not authorized user"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        SkillsService otherUser = super.createService("otherUser")

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId, skillsService.userName).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected:true, userId: skillsService.userName])
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id, [isSelected:true, userId: skillsService.userName])

        when:
        otherUser.completeQuizAttempt(quiz.quizId, quizAttempt.id, skillsService.userName).body
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Access Denied")
        skillsClientException.httpStatus == HttpStatus.FORBIDDEN
    }

    def "get quiz attempt info - user must project admin (in case skill-quiz association) OR quiz admin in order to supply user id - not authorized user"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        SkillsService otherUser = super.createService("otherUser")
        when:
        otherUser.getQuizInfo(quiz.quizId, skillsService.userName)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.resBody.contains("Access Denied")
        skillsClientException.httpStatus == HttpStatus.FORBIDDEN
    }

    def "user must project admin (in case skill-quiz association) OR quiz admin in order to supply user id - quiz admin"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        SkillsService otherUser = super.createService("otherUser")
        skillsService.addQuizUserRole(quiz.quizId, otherUser.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        when:
        def quizAttempt =  otherUser.startQuizAttempt(quiz.quizId, skillsService.userName).body
        otherUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected:true, userId: skillsService.userName])
        otherUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id, [isSelected:true, userId: skillsService.userName])
        def gradedQuizAttempt = otherUser.completeQuizAttempt(quiz.quizId, quizAttempt.id, skillsService.userName).body
        then:
        gradedQuizAttempt.passed == false
    }

    def "user must project admin (in case skill-quiz association) OR quiz admin in order to supply user id - project admin"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        SkillsService otherUser = super.createService("otherUser")
        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        otherUser.createProjectAndSubjectAndSkills(proj, subj, skills)

        when:
        def quizAttempt =  otherUser.startQuizAttempt(quiz.quizId, skillsService.userName).body
        otherUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected:true, userId: skillsService.userName])
        otherUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id, [isSelected:true, userId: skillsService.userName])
        def gradedQuizAttempt = otherUser.completeQuizAttempt(quiz.quizId, quizAttempt.id, skillsService.userName).body
        then:
        gradedQuizAttempt.passed == false
    }

    def "user must project admin (in case skill-quiz association) OR quiz admin in order to supply user id - project approver"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        SkillsService otherUser = super.createService("otherUser")
        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        otherUser.createProjectAndSubjectAndSkills(proj, subj, skills)

        String approverUserName = getRandomUsers(1).first()
        SkillsService approverUser = super.createService(approverUserName)
        otherUser.addUserRole(approverUser.userName, proj.projectId, RoleName.ROLE_PROJECT_APPROVER.toString())

        when:
        def quizAttempt =  approverUser.startQuizAttempt(quiz.quizId, skillsService.userName).body
        approverUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected:true, userId: skillsService.userName])
        approverUser.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[1].id, [isSelected:true, userId: skillsService.userName])
        def gradedQuizAttempt = approverUser.completeQuizAttempt(quiz.quizId, quizAttempt.id, skillsService.userName).body
        then:
        gradedQuizAttempt.passed == false
    }

}
