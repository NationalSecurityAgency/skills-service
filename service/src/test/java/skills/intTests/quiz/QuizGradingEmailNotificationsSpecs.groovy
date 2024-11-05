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
package skills.intTests.quiz


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.EmailUtils
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.UserAttrs
import skills.utils.WaitFor

class QuizGradingEmailNotificationsSpecs extends DefaultIntSpec {

    def setup() {
        startEmailServer()
    }

    def "grading information from get quiz endpoint"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputQuestion(1, 1),
        ])

        List<String> users = getRandomUsers(1, true)
        SkillsService testTaker = createService(users[0])
        UserAttrs testTakerUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(testTaker.userName)

        UserAttrs quizAdminUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)

        def quizInfo = testTaker.getQuizInfo(quiz.quizId)
        def quizAttempt = testTaker.startQuizAttempt(quiz.quizId).body
        testTaker.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])

        when:
        def gradedQuizAttempt = testTaker.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 0 }
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)

        then:
        gradedQuizAttempt.needsGrading

        greenMail.getReceivedMessages().length == 1
        emailRes.subj == "SkillTree Quiz Grading Requested"
        emailRes.recipients == [quizAdminUserAttrs.email]
        emailRes.plainText.contains("SkillTree Quiz Grading Request")
        emailRes.html.contains("<h1>SkillTree Quiz Grading Request!</h1>")
    }

}