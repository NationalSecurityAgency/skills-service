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


import skills.controller.exceptions.ErrorCode
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSubject

class QuizApi_RunSurveySpecs extends DefaultIntSpec {

    def "report survey attempt"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[2].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'This is user provided answer'])

        def gradedQuizAttempt = skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id).body
        then:
        gradedQuizAttempt.passed == true
        !gradedQuizAttempt.gradedQuestions
        gradedQuizAttempt.started
        gradedQuizAttempt.completed
    }

    def "restart survey attempt"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'This is user provided answer'])

        def restartedQuizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        then:
        restartedQuizAttempt.selectedAnswerIds == [quizAttempt.questions[0].answerOptions[1].id]
        restartedQuizAttempt.enteredText.size() == 1
        restartedQuizAttempt.enteredText.find { it.answerId == quizAttempt.questions[2].answerOptions[0].id }.answerText == 'This is user provided answer'
    }

    def "empty or non-existent text for TextInput question type should remove the answer"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[1].answerOptions[2].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'ans'])

        def quizAttempt_t1 =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:false, answerText: '   '])
        def quizAttempt_t2 =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'ans1'])
        def quizAttempt_t3 =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:false, answerText: null])
        def quizAttempt_t4 =  skillsService.startQuizAttempt(quiz.quizId).body

        then:
        quizAttempt_t1.enteredText.answerId == [quizAttempt.questions[2].answerOptions[0].id]
        quizAttempt_t1.enteredText.answerText == ['ans']

        !quizAttempt_t2.enteredText

        quizAttempt_t3.enteredText.answerId == [quizAttempt.questions[2].answerOptions[0].id]
        quizAttempt_t3.enteredText.answerText == ['ans1']

        !quizAttempt_t4.enteredText
    }

    def "TextInput question validation: selected answers must provide an answer"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        when:
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:true, answerText: null])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("answerText was not provided")
        e.message.contains("quizId:${quiz.quizId}")
        e.message.contains("errorCode:${ErrorCode.BadParam}")
    }

    def "TextInput question validation: selected answers must provide an answer - empty string is not acceptable"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        when:
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:true, answerText: '    '])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("answerText was not provided")
        e.message.contains("quizId:${quiz.quizId}")
        e.message.contains("errorCode:${ErrorCode.BadParam}")
    }

    def "TextInput question validation: not selected answers must NOT provide an answer"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        when:
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:false, answerText: '  blah   '])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("For TextInput type, if isSelected=false then the answer must be null or blank")
        e.message.contains("quizId:${quiz.quizId}")
        e.message.contains("errorCode:${ErrorCode.BadParam}")
    }

    def "TextInput question validation: custom text validation"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        when:
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'Jabberwocky text'])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("answerText is invalid: paragraphs may not contain jabberwocky")
        e.message.contains("quizId:${quiz.quizId}")
        e.message.contains("errorCode:${ErrorCode.BadParam}")
    }

    def "quiz must have at least 1 questions to start"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        when:
        skillsService.startQuizAttempt(quiz.quizId)
        then:
        SkillsClientException skillsClientException = thrown()
        skillsClientException.message.contains("Must have at least 1 question declared in order to start.")
    }

    def "survey can not be taken if the project does not have enough points"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = createSkills(1, 1, 1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkill(skills[0])

        when:
        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        then:
        quizInfo
        quizInfo.canStartQuiz == false
        quizInfo.errorMessage == "This Survey is assigned to a Skill (skill1) that does not have enough points to be completed. The Project (TestProject1) that contains this skill must have at least 100 points."
    }

    def "survey can not be taken if the subject does not have enough points"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def subj2 = createSubject(1, 2)

        def skills = createSkills(1, 1, 1, 1)
        def otherSkills = createSkills(1, 1, 2, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skillsService.createSubject(subj2)
        skillsService.createSkill(otherSkills[0])

        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkill(skills[0])

        when:
        def quizInfo = skillsService.getQuizInfo(quiz.quizId)

        then:
        quizInfo
        quizInfo.canStartQuiz == false
        quizInfo.errorMessage == "This Survey is assigned to a Skill (skill1) that does not have enough points to be completed. The Subject (TestSubject1) that contains this skill must have at least 100 points."
    }

    private Map createTextResponseSurvey(boolean multiple) {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def question = multiple ? QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 4) :
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 4)
        question.answers[2].answer = 'Other reason'
        question.answers[2].allowCustomText = true
        question.answers[3].answer = 'Another reason'
        question.answers[3].allowCustomText = true
        skillsService.createQuizQuestionDefs([question])
        return quiz
    }

    def "survey text responses: ordinary and text options, resume, complete and history - #questionType"() {
        def quiz = createTextResponseSurvey(multiple)

        when:
        def attempt = skillsService.startQuizAttempt(quiz.quizId).body
        def options = attempt.questions[0].answerOptions
        // single choice: each report replaces the previous selection
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, options[0].id)
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, options[2].id, [isSelected: true, answerText: 'First text'])
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, options[3].id, [isSelected: true, answerText: 'Second text'])
        def resumed = skillsService.startQuizAttempt(quiz.quizId).body

        then:
        options.allowCustomText == [false, false, true, true]
        if (multiple) {
            assert resumed.selectedAnswerIds == [options[0].id]
            assert resumed.enteredText.sort { it.answerId }.collect { [it.answerId, it.answerText] } == [[options[2].id, 'First text'], [options[3].id, 'Second text']]
        } else {
            assert resumed.selectedAnswerIds == []
            assert resumed.enteredText.collect { [it.answerId, it.answerText] } == [[options[3].id, 'Second text']]
        }

        when:
        skillsService.completeQuizAttempt(quiz.quizId, attempt.id)
        def learnerHistory = skillsService.getCurrentUserSingleQuizAttempt(attempt.id)
        def adminHistory = skillsService.getQuizAttemptResult(quiz.quizId, attempt.id)
        def answerHistory = skillsService.getUserQuizAnswers(quiz.quizId, options[3].id)

        then:
        [learnerHistory, adminHistory].each { def history ->
            def answers = history.questions[0].answers
            assert answers.collect { it.answer } == (multiple ?
                    ['Answer #1', 'Answer #2', 'Other reason: First text', 'Another reason: Second text'] :
                    ['Answer #1', 'Answer #2', 'Other reason', 'Another reason: Second text'])
            assert answers.collect { it.isSelected } == (multiple ? [true, false, true, true] : [false, false, false, true])
        }
        answerHistory.data.answerTxt == ['Second text']

        where:
        questionType     | multiple
        'SingleChoice'   | false
        'MultipleChoice' | true
    }

    def "survey text responses: a cleared response keeps the selection and blocks completion - #questionType"() {
        def quiz = createTextResponseSurvey(multiple)
        def attempt = skillsService.startQuizAttempt(quiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]

        when:
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Some text'])
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: '   '])
        def resumed = skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.completeQuizAttempt(quiz.quizId, attempt.id)

        then:
        resumed.selectedAnswerIds == [option.id]
        !resumed.enteredText
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Text response is required for each selected answer that allows text")
        e.message.contains("errorCode:${ErrorCode.BadParam}")

        when:
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Final text'])
        def completed = skillsService.completeQuizAttempt(quiz.quizId, attempt.id).body
        def history = skillsService.getQuizAttemptResult(quiz.quizId, attempt.id)

        then:
        completed.passed
        history.questions[0].answers[2].answer == 'Other reason: Final text'

        where:
        questionType     | multiple
        'SingleChoice'   | false
        'MultipleChoice' | true
    }

    def "survey text responses: deselecting removes the saved response - #questionType"() {
        def quiz = createTextResponseSurvey(multiple)
        def attempt = skillsService.startQuizAttempt(quiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]

        when:
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Some text'])
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: false])
        def resumed = skillsService.startQuizAttempt(quiz.quizId).body

        then:
        !resumed.selectedAnswerIds
        !resumed.enteredText

        where:
        questionType     | multiple
        'SingleChoice'   | false
        'MultipleChoice' | true
    }

    def "survey text responses: rejected text keeps the saved response - #questionType, #reason"() {
        def quiz = createTextResponseSurvey(multiple)
        def attempt = skillsService.startQuizAttempt(quiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Saved text'])

        when:
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: text])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains(message)
        e.message.contains("quizId:${quiz.quizId}")
        e.message.contains("errorCode:${ErrorCode.BadParam}")
        skillsService.startQuizAttempt(quiz.quizId).body.enteredText.collect { [it.answerId, it.answerText] } == [[option.id, 'Saved text']]

        where:
        questionType     | multiple | reason       | text               | message
        'SingleChoice'   | false    | 'validation' | 'Jabberwocky text' | 'answerText is invalid: paragraphs may not contain jabberwocky'
        'SingleChoice'   | false    | 'length'     | 'x' * 1001         | 'answerText must not exceed [1000] chars'
        'MultipleChoice' | true     | 'validation' | 'Jabberwocky text' | 'answerText is invalid: paragraphs may not contain jabberwocky'
        'MultipleChoice' | true     | 'length'     | 'x' * 1001         | 'answerText must not exceed [1000] chars'
    }

    def "survey text responses: text is sanitized on first save and on update - #questionType"() {
        def quiz = createTextResponseSurvey(multiple)
        def attempt = skillsService.startQuizAttempt(quiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]

        when:
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'First <script>alert(1)</script><b>bold</b> & more'])
        def afterSave = skillsService.startQuizAttempt(quiz.quizId).body.enteredText[0].answerText
        skillsService.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Second <img src=x onerror=alert(1)>a < b'])
        def afterUpdate = skillsService.startQuizAttempt(quiz.quizId).body.enteredText[0].answerText
        skillsService.completeQuizAttempt(quiz.quizId, attempt.id)

        then:
        afterSave == 'First bold & more'
        afterUpdate == 'Second a < b'
        skillsService.getQuizAttemptResult(quiz.quizId, attempt.id).questions[0].answers[2].answer == 'Other reason: Second a < b'
        skillsService.getUserQuizAnswers(quiz.quizId, option.id).data.answerTxt == ['Second a < b']

        where:
        questionType     | multiple
        'SingleChoice'   | false
        'MultipleChoice' | true
    }

    def "survey text responses: saved responses are kept after the answer no longer allows text - #questionType"() {
        def quiz = createTextResponseSurvey(multiple)
        String user = getRandomUsers(1, true)[0]
        def completedAttempt = skillsService.startQuizAttempt(quiz.quizId).body
        def option = completedAttempt.questions[0].answerOptions[2]
        skillsService.reportQuizAnswer(quiz.quizId, completedAttempt.id, option.id, [isSelected: true, answerText: 'Completed text'])
        skillsService.completeQuizAttempt(quiz.quizId, completedAttempt.id)

        def inProgressAttempt = skillsService.startQuizAttemptForUserId(quiz.quizId, user).body
        skillsService.reportQuizAnswerForUserId(quiz.quizId, inProgressAttempt.id, option.id, user, [isSelected: true, answerText: 'In progress text'])

        when:
        def question = skillsService.getQuizQuestionDefs(quiz.quizId).questions[0]
        question.answers[2].allowCustomText = false
        question.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(question)
        def resumed = skillsService.startQuizAttemptForUserId(quiz.quizId, user).body
        skillsService.completeQuizAttemptForUserId(quiz.quizId, inProgressAttempt.id, user)

        then:
        resumed.questions[0].answerOptions[2].allowCustomText == false
        resumed.enteredText.collect { [it.answerId, it.answerText] } == [[option.id, 'In progress text']]
        skillsService.getQuizAttemptResult(quiz.quizId, completedAttempt.id).questions[0].answers[2].answer == 'Other reason: Completed text'
        skillsService.getQuizAttemptResult(quiz.quizId, inProgressAttempt.id).questions[0].answers[2].answer == 'Other reason: In progress text'

        where:
        questionType     | multiple
        'SingleChoice'   | false
        'MultipleChoice' | true
    }
}
