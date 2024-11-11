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

class QuizGradedResponseEmailNotificationsSpecs extends DefaultIntSpec {

    def setup() {
        startEmailServer()
    }

    def "quiz graded notification is sent to the quiz taker - quiz passed"() {
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
        testTaker.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])

        def gradedQuizAttempt = testTaker.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        greenMail.purgeEmailFromAllMailboxes()

        def gradedRes = skillsService.gradeAnswer(testTaker.userName, quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, true, "Good answer")

        when:
        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)

        println emailRes.plainText
        println emailRes.html

        then:
        gradedRes.body.doneGradingAttempt

        greenMail.getReceivedMessages().length == 1
        emailRes.subj == "SkillTree Quiz Graded"
        emailRes.recipients == [testTakerUserAttrs.email]
        emailRes.plainText.contains("SkillTree Quiz Graded.")
        emailRes.plainText.contains("Congratulations, you passed the quiz [${quizInfo.name}]!")
        emailRes.plainText.contains("Quiz Url: http://localhost:${localPort}/progress-and-rankings/my-quiz-attempts/${quizAttempt.id}")

        emailRes.html.contains("<h1>SkillTree Quiz Graded</h1>")
        emailRes.html.contains("<p>Congratulations, you passed the quiz <b>${quizInfo.name}</b>!</p>")
        emailRes.html.contains("<a href=\"http://localhost:${localPort}/progress-and-rankings/my-quiz-attempts/${quizAttempt.id}\" class=\"button\">Review Quiz Run</a>")
    }

    def "quiz graded notification is sent to the quiz taker - quiz failed"() {
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
        testTaker.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])

        def gradedQuizAttempt = testTaker.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        greenMail.purgeEmailFromAllMailboxes()

        def gradedRes = skillsService.gradeAnswer(testTaker.userName, quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, false, "Good answer")

        when:
        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)

        println emailRes.plainText
        println emailRes.html

        then:
        gradedRes.body.doneGradingAttempt

        greenMail.getReceivedMessages().length == 1
        emailRes.subj == "SkillTree Quiz Graded"
        emailRes.recipients == [testTakerUserAttrs.email]
        emailRes.plainText.contains("SkillTree Quiz Graded.")
        emailRes.plainText.contains("Unfortunately, you failed the quiz [${quizInfo.name}].")
        emailRes.plainText.contains("Quiz Url: http://localhost:${localPort}/progress-and-rankings/my-quiz-attempts/${quizAttempt.id}")

        emailRes.html.contains("<h1>SkillTree Quiz Graded</h1>")
        emailRes.html.contains("<p>Unfortunately, you failed the quiz <b>${quizInfo.name}</b>.</p>")
        emailRes.html.contains("<a href=\"http://localhost:${localPort}/progress-and-rankings/my-quiz-attempts/${quizAttempt.id}\" class=\"button\">Review Quiz Run</a>")
    }

    def "headers and footer is included in the email when quiz graded notification is sent to the quiz taker"() {
        SkillsService rootSkillsService = createRootSkillService()
        rootSkillsService.saveEmailHeaderAndFooterSettings(
                '<p>Header attention {{ community.descriptor }} Members</p>',
                '<p>Footer attention {{ community.descriptor }} Members</p>',
                'Plain Text Header Attention {{ community.descriptor }} Members',
                'Plain Text Footer Attention {{ community.descriptor }} Members')

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
        testTaker.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])

        def gradedQuizAttempt = testTaker.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        greenMail.purgeEmailFromAllMailboxes()

        skillsService.gradeAnswer(testTaker.userName, quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, false, "Good answer")

        when:
        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 1 }
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)

        then:
        gradedQuizAttempt.needsGrading

        greenMail.getReceivedMessages().length == 1
        emailRes.subj == "SkillTree Quiz Graded"
        emailRes.recipients == [testTakerUserAttrs.email]
        emailRes.plainText.contains("SkillTree Quiz Graded.")
        emailRes.plainText.contains("Unfortunately, you failed the quiz [${quizInfo.name}].")
        emailRes.plainText.contains("Quiz Url: http://localhost:${localPort}/progress-and-rankings/my-quiz-attempts/${quizAttempt.id}")

        emailRes.html.contains("<h1>SkillTree Quiz Graded</h1>")
        emailRes.html.contains("<p>Unfortunately, you failed the quiz <b>${quizInfo.name}</b>.</p>")
        emailRes.html.contains("<a href=\"http://localhost:${localPort}/progress-and-rankings/my-quiz-attempts/${quizAttempt.id}\" class=\"button\">Review Quiz Run</a>")

        emailRes.plainText.startsWith("Plain Text Header Attention All Dragons Members")
        emailRes.plainText.endsWith("Plain Text Footer Attention All Dragons Members")

        emailRes.html.contains("<body class=\"overall-container\">\r\n<p>Header attention All Dragons Members</p>\r\n<h1>SkillTree Quiz Graded</h1>")
        emailRes.html.contains("<p>Footer attention All Dragons Members</p>\r\n</body>")
    }

}