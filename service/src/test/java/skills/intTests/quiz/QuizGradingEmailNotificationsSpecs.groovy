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
import skills.quizLoading.QuizUserPreferences
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.utils.WaitFor

class QuizGradingEmailNotificationsSpecs extends DefaultIntSpec {

    def setup() {
        startEmailServer()
    }

    def "email notification sent to quiz admin when grading is required"() {
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

        when:
        def gradedQuizAttempt = testTaker.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        assert WaitFor.wait { greenMail.getReceivedMessages().size() > 0 }
        EmailUtils.EmailRes emailRes = EmailUtils.getEmail(greenMail)

        then:
        gradedQuizAttempt.needsGrading

        greenMail.getReceivedMessages().length == 1
        emailRes.subj == "SkillTree Quiz Grading Requested"
        emailRes.recipients == [quizAdminUserAttrs.email]
        emailRes.plainText.contains("SkillTree Quiz Grading Request!")
        emailRes.plainText.contains("User [${testTakerUserAttrs.userIdForDisplay}] has completed the [${quizInfo.name}] quiz which requires manual grading. As a quiz administrator, please review the submitted answers and evaluate them as passed or failed.")
        emailRes.plainText.contains("Grading URL: http://localhost:${localPort}/administrator/quizzes/${quiz.quizId}/grading")

        emailRes.html.contains("<h1>SkillTree Quiz Grading Request!</h1>")
        emailRes.html.contains("<p>User <b>${testTakerUserAttrs.userIdForDisplay}</b> has completed the <b>${quizInfo.name}</b> quiz which requires manual grading. As a quiz administrator, please review the submitted answers and evaluate them as passed or failed.</p>")
        emailRes.html.contains("<a href=\"http://localhost:${localPort}/administrator/quizzes/${quiz.quizId}/grading\" class=\"button\">Review Answers</a>")
    }

    def "email notification sent to multiple quiz admins"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputQuestion(1, 1),
        ])

        List<String> users = getRandomUsers(5, true)

        SkillsService admin2 = createService(users[1])
        SkillsService admin3 = createService(users[2])
        SkillsService admin4 = createService(users[3])
        SkillsService admin5 = createService(users[4])
        skillsService.addQuizUserRole(quiz.quizId, admin2.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.addQuizUserRole(quiz.quizId, admin3.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.addQuizUserRole(quiz.quizId, admin4.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.addQuizUserRole(quiz.quizId, admin5.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        UserAttrs quizAdminUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        UserAttrs quizAdmin2UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin2.userName)
        UserAttrs quizAdmin3UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin3.userName)
        UserAttrs quizAdmin4UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin4.userName)
        UserAttrs quizAdmin5UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin5.userName)

        SkillsService testTaker = createService(users[0])
        UserAttrs testTakerUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(testTaker.userName)

        def quizInfo = testTaker.getQuizInfo(quiz.quizId)
        def quizAttempt = testTaker.startQuizAttempt(quiz.quizId).body
        testTaker.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])

        when:
        def gradedQuizAttempt = testTaker.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 5 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        then:
        gradedQuizAttempt.needsGrading

        emails.size() == 5
        emails.each {
            assert it.subj == "SkillTree Quiz Grading Requested"
            assert it.plainText.contains("SkillTree Quiz Grading Request!")
            assert it.plainText.contains("User [${testTakerUserAttrs.userIdForDisplay}] has completed the [${quizInfo.name}] quiz which requires manual grading. As a quiz administrator, please review the submitted answers and evaluate them as passed or failed.")
            assert it.plainText.contains("Grading URL: http://localhost:${localPort}/administrator/quizzes/${quiz.quizId}/grading")

            assert it.html.contains("<h1>SkillTree Quiz Grading Request!</h1>")
            assert it.html.contains("<p>User <b>${testTakerUserAttrs.userIdForDisplay}</b> has completed the <b>${quizInfo.name}</b> quiz which requires manual grading. As a quiz administrator, please review the submitted answers and evaluate them as passed or failed.</p>")
            assert it.html.contains("<a href=\"http://localhost:${localPort}/administrator/quizzes/${quiz.quizId}/grading\" class=\"button\">Review Answers</a>")
        }
        emails.collect { it. recipients }.flatten().sort() == [quizAdminUserAttrs.email, quizAdmin2UserAttrs.email, quizAdmin3UserAttrs.email, quizAdmin4UserAttrs.email, quizAdmin5UserAttrs.email].sort()
    }

    def "email notification sent to multiple quiz admins - some unsubscribed"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs([
                QuizDefFactory.createTextInputQuestion(1, 1),
        ])

        List<String> users = getRandomUsers(5, true)

        SkillsService admin2 = createService(users[1])
        SkillsService admin3 = createService(users[2])
        SkillsService admin4 = createService(users[3])
        SkillsService admin5 = createService(users[4])
        skillsService.addQuizUserRole(quiz.quizId, admin2.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.addQuizUserRole(quiz.quizId, admin3.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.addQuizUserRole(quiz.quizId, admin4.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.addQuizUserRole(quiz.quizId, admin5.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        admin2.saveQuizUserPreference(quiz.quizId, QuizUserPreferences.DisableGradingRequestNotification.preference, true)
        admin5.saveQuizUserPreference(quiz.quizId, QuizUserPreferences.DisableGradingRequestNotification.preference, true)

        UserAttrs quizAdminUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        UserAttrs quizAdmin2UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin2.userName)
        UserAttrs quizAdmin3UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin3.userName)
        UserAttrs quizAdmin4UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin4.userName)
        UserAttrs quizAdmin5UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin5.userName)

        SkillsService testTaker = createService(users[0])
        UserAttrs testTakerUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(testTaker.userName)

        def quizAttempt = testTaker.startQuizAttempt(quiz.quizId).body
        testTaker.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])

        when:
        def gradedQuizAttempt = testTaker.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 3 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        then:
        gradedQuizAttempt.needsGrading

        emails.size() == 3
        emails.each {
            assert it.subj == "SkillTree Quiz Grading Requested"
            assert it.plainText.contains("SkillTree Quiz Grading Request!")
        }
        emails.collect { it. recipients }.flatten().sort() == [quizAdminUserAttrs.email, quizAdmin3UserAttrs.email, quizAdmin4UserAttrs.email].sort()
    }

    def "header and footer is properly applied to the email"() {
        SkillsService rootSkillsService = createRootSkillService()
        String headerAndFooterValue = 'Attention {{ community.descriptor }} Members'
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

        List<String> users = getRandomUsers(5, true)

        SkillsService admin2 = createService(users[1])
        SkillsService admin3 = createService(users[2])
        SkillsService admin4 = createService(users[3])
        SkillsService admin5 = createService(users[4])
        skillsService.addQuizUserRole(quiz.quizId, admin2.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.addQuizUserRole(quiz.quizId, admin3.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.addQuizUserRole(quiz.quizId, admin4.userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        skillsService.addQuizUserRole(quiz.quizId, admin5.userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        UserAttrs quizAdminUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(skillsService.userName)
        UserAttrs quizAdmin2UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin2.userName)
        UserAttrs quizAdmin3UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin3.userName)
        UserAttrs quizAdmin4UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin4.userName)
        UserAttrs quizAdmin5UserAttrs = userAttrsRepo.findByUserIdIgnoreCase(admin5.userName)

        SkillsService testTaker = createService(users[0])
        UserAttrs testTakerUserAttrs = userAttrsRepo.findByUserIdIgnoreCase(testTaker.userName)

        def quizInfo = testTaker.getQuizInfo(quiz.quizId)
        def quizAttempt = testTaker.startQuizAttempt(quiz.quizId).body
        testTaker.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id, [isSelected: true, answerText: 'This is user provided answer'])

        when:
        def gradedQuizAttempt = testTaker.completeQuizAttempt(quiz.quizId, quizAttempt.id).body

        assert WaitFor.wait { greenMail.getReceivedMessages().size() == 5 }
        List<EmailUtils.EmailRes> emails = EmailUtils.getEmails(greenMail)

        then:
        gradedQuizAttempt.needsGrading

        emails.size() == 5
        emails.each {
            println it.plainText
            println it.html
            assert it.subj == "SkillTree Quiz Grading Requested"
            assert it.plainText.startsWith("Plain Text Header Attention All Dragons Members")
            assert it.plainText.endsWith("Plain Text Footer Attention All Dragons Members")
            assert it.plainText.contains("SkillTree Quiz Grading Request!")
            assert it.plainText.contains("User [${testTakerUserAttrs.userIdForDisplay}] has completed the [${quizInfo.name}] quiz which requires manual grading. As a quiz administrator, please review the submitted answers and evaluate them as passed or failed.")
            assert it.plainText.contains("Grading URL: http://localhost:${localPort}/administrator/quizzes/${quiz.quizId}/grading")

            assert it.html.contains("<body class=\"overall-container\">\r\n<p>Header attention All Dragons Members</p>\r\n<h1>SkillTree Quiz Grading Request!</h1>")
            assert it.html.contains("<p>Footer attention All Dragons Members</p>\r\n</body>")
            assert it.html.contains("<p>User <b>${testTakerUserAttrs.userIdForDisplay}</b> has completed the <b>${quizInfo.name}</b> quiz which requires manual grading. As a quiz administrator, please review the submitted answers and evaluate them as passed or failed.</p>")
            assert it.html.contains("<a href=\"http://localhost:${localPort}/administrator/quizzes/${quiz.quizId}/grading\" class=\"button\">Review Answers</a>")
        }
        emails.collect { it. recipients }.flatten().sort() == [quizAdminUserAttrs.email, quizAdmin2UserAttrs.email, quizAdmin3UserAttrs.email, quizAdmin4UserAttrs.email, quizAdmin5UserAttrs.email].sort()
    }

}