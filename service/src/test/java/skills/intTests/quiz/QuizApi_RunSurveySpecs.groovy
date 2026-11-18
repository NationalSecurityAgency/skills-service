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
import skills.controller.exceptions.ErrorCode
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.quiz.QuizQuestionType
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

    private Map createTextResponseSurvey(boolean multiple, int quizNum = 1) {
        def quiz = QuizDefFactory.createQuizSurvey(quizNum)
        skillsService.createQuizDef(quiz)
        def question = multiple ? QuizDefFactory.createMultipleChoiceSurveyQuestion(quizNum, 1, 4) :
                QuizDefFactory.createSingleChoiceSurveyQuestion(quizNum, 1, 4)
        question.answers[2].answer = 'Other reason'
        question.answers[2].allowCustomText = true
        question.answers[3].answer = 'Another reason'
        question.answers[3].allowCustomText = true
        skillsService.createQuizQuestionDefs([question])
        return quiz
    }

    // signed-in learners who are not admins of the survey; one call keeps the users distinct
    private List<SkillsService> createLearners(int num) {
        return getRandomUsers(num).collect { createService(it) }
    }

    private SkillsService createLearner() {
        return createLearners(1)[0]
    }

    private static List enteredText(Map attempt) {
        return attempt.enteredText.sort { it.answerId }.collect { [it.answerId, it.answerText] }
    }

    def "survey text responses: ordinary and text options, resume, complete and history - #questionType"() {
        def quiz = createTextResponseSurvey(multiple)
        SkillsService learner = createLearner()

        when:
        def attempt = learner.startQuizAttempt(quiz.quizId).body
        def options = attempt.questions[0].answerOptions
        // single choice: each report replaces the previous selection
        learner.reportQuizAnswer(quiz.quizId, attempt.id, options[0].id)
        learner.reportQuizAnswer(quiz.quizId, attempt.id, options[2].id, [isSelected: true, answerText: 'First text'])
        learner.reportQuizAnswer(quiz.quizId, attempt.id, options[3].id, [isSelected: true, answerText: 'Second text'])
        def resumed = learner.startQuizAttempt(quiz.quizId).body

        then:
        options.allowCustomText == [false, false, true, true]
        resumed.id == attempt.id
        if (multiple) {
            assert resumed.selectedAnswerIds == [options[0].id]
            assert enteredText(resumed) == [[options[2].id, 'First text'], [options[3].id, 'Second text']]
        } else {
            assert resumed.selectedAnswerIds == []
            assert enteredText(resumed) == [[options[3].id, 'Second text']]
        }

        when:
        learner.completeQuizAttempt(quiz.quizId, attempt.id)
        def learnerHistory = learner.getCurrentUserSingleQuizAttempt(attempt.id)
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
        adminHistory.userId == learner.userName
        answerHistory.data.answerTxt == ['Second text']

        where:
        questionType     | multiple
        'SingleChoice'   | false
        'MultipleChoice' | true
    }

    def "survey text responses: a cleared response keeps the selection and blocks completion - #questionType"() {
        def quiz = createTextResponseSurvey(multiple)
        SkillsService learner = createLearner()
        def attempt = learner.startQuizAttempt(quiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]

        when:
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Some text'])
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: '   '])
        def resumed = learner.startQuizAttempt(quiz.quizId).body
        learner.completeQuizAttempt(quiz.quizId, attempt.id)

        then:
        resumed.selectedAnswerIds == [option.id]
        !resumed.enteredText
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Text response is required for each selected answer that allows text")
        e.message.contains("errorCode:${ErrorCode.BadParam}")

        when:
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Final text'])
        def completed = learner.completeQuizAttempt(quiz.quizId, attempt.id).body
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
        SkillsService learner = createLearner()
        def attempt = learner.startQuizAttempt(quiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]

        when:
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Some text'])
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: false])
        def resumed = learner.startQuizAttempt(quiz.quizId).body

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
        SkillsService learner = createLearner()
        def attempt = learner.startQuizAttempt(quiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Saved text'])

        when:
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: text])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains(message)
        e.message.contains("quizId:${quiz.quizId}")
        e.message.contains("errorCode:${ErrorCode.BadParam}")
        enteredText(learner.startQuizAttempt(quiz.quizId).body) == [[option.id, 'Saved text']]

        where:
        questionType     | multiple | reason       | text               | message
        'SingleChoice'   | false    | 'validation' | 'Jabberwocky text' | 'answerText is invalid: paragraphs may not contain jabberwocky'
        'SingleChoice'   | false    | 'length'     | 'x' * 1001         | 'answerText must not exceed [1000] chars'
        'MultipleChoice' | true     | 'validation' | 'Jabberwocky text' | 'answerText is invalid: paragraphs may not contain jabberwocky'
        'MultipleChoice' | true     | 'length'     | 'x' * 1001         | 'answerText must not exceed [1000] chars'
    }

    def "survey text responses: a rejected first save creates no selection or response - #questionType, #reason"() {
        def quiz = createTextResponseSurvey(multiple)
        SkillsService learner = createLearner()
        def attempt = learner.startQuizAttempt(quiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]

        when:
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: text])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains(message)
        e.message.contains("errorCode:${ErrorCode.BadParam}")

        when:
        def resumed = learner.startQuizAttempt(quiz.quizId).body

        then:
        resumed.id == attempt.id
        !resumed.selectedAnswerIds
        !resumed.enteredText

        where:
        questionType     | multiple | reason       | text               | message
        'SingleChoice'   | false    | 'validation' | 'Jabberwocky text' | 'answerText is invalid: paragraphs may not contain jabberwocky'
        'SingleChoice'   | false    | 'length'     | 'x' * 1001         | 'answerText must not exceed [1000] chars'
        'MultipleChoice' | true     | 'validation' | 'Jabberwocky text' | 'answerText is invalid: paragraphs may not contain jabberwocky'
        'MultipleChoice' | true     | 'length'     | 'x' * 1001         | 'answerText must not exceed [1000] chars'
    }

    def "survey text responses: spaces at either end count toward the length limit - #questionType, #reason"() {
        def quiz = createTextResponseSurvey(multiple)
        SkillsService learner = createLearner()
        def attempt = learner.startQuizAttempt(quiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]

        when:
        def error = null
        try {
            learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: text])
        } catch (SkillsClientException e) {
            error = e
        }

        then:
        text.length() == length
        if (accepted) {
            assert !error
            assert learner.startQuizAttempt(quiz.quizId).body.enteredText.size() == 1
        } else {
            assert error.httpStatus == HttpStatus.BAD_REQUEST
            assert error.message.contains('answerText must not exceed [1000] chars')
            assert !learner.startQuizAttempt(quiz.quizId).body.enteredText
        }

        where:
        questionType     | multiple | reason                | text                     | length | accepted
        'SingleChoice'   | false    | 'at the limit'        | 'x' * 1000               | 1000   | true
        'SingleChoice'   | false    | 'one over'            | 'x' * 1001               | 1001   | false
        'SingleChoice'   | false    | 'leading space at'    | ' ' + 'x' * 999          | 1000   | true
        'SingleChoice'   | false    | 'leading space over'  | ' ' + 'x' * 1000         | 1001   | false
        'SingleChoice'   | false    | 'trailing space at'   | 'x' * 999 + ' '          | 1000   | true
        'SingleChoice'   | false    | 'trailing space over' | 'x' * 1000 + ' '         | 1001   | false
        'MultipleChoice' | true     | 'at the limit'        | 'x' * 1000               | 1000   | true
        'MultipleChoice' | true     | 'one over'            | 'x' * 1001               | 1001   | false
        'MultipleChoice' | true     | 'leading space at'    | ' ' + 'x' * 999          | 1000   | true
        'MultipleChoice' | true     | 'leading space over'  | ' ' + 'x' * 1000         | 1001   | false
        'MultipleChoice' | true     | 'trailing space at'   | 'x' * 999 + ' '          | 1000   | true
        'MultipleChoice' | true     | 'trailing space over' | 'x' * 1000 + ' '         | 1001   | false
    }

    def "survey text responses: text is sanitized on first save and on update - #questionType"() {
        def quiz = createTextResponseSurvey(multiple)
        SkillsService learner = createLearner()
        def attempt = learner.startQuizAttempt(quiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]

        when:
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'First <script>alert(1)</script><b>bold</b> & more'])
        def afterSave = learner.startQuizAttempt(quiz.quizId).body.enteredText[0].answerText
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Second <img src=x onerror=alert(1)>a < b'])
        def afterUpdate = learner.startQuizAttempt(quiz.quizId).body.enteredText[0].answerText
        learner.completeQuizAttempt(quiz.quizId, attempt.id)

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
        def (SkillsService completedLearner, SkillsService inProgressLearner) = createLearners(2)
        def completedAttempt = completedLearner.startQuizAttempt(quiz.quizId).body
        def option = completedAttempt.questions[0].answerOptions[2]
        completedLearner.reportQuizAnswer(quiz.quizId, completedAttempt.id, option.id, [isSelected: true, answerText: 'Completed text'])
        completedLearner.completeQuizAttempt(quiz.quizId, completedAttempt.id)

        def inProgressAttempt = inProgressLearner.startQuizAttempt(quiz.quizId).body
        inProgressLearner.reportQuizAnswer(quiz.quizId, inProgressAttempt.id, option.id, [isSelected: true, answerText: 'In progress text'])

        when:
        def question = skillsService.getQuizQuestionDefs(quiz.quizId).questions[0]
        question.answers[2].allowCustomText = false
        question.quizId = quiz.quizId
        skillsService.updateQuizQuestionDef(question)
        def resumed = inProgressLearner.startQuizAttempt(quiz.quizId).body
        inProgressLearner.completeQuizAttempt(quiz.quizId, inProgressAttempt.id)

        then:
        resumed.questions[0].answerOptions[2].allowCustomText == false
        enteredText(resumed) == [[option.id, 'In progress text']]
        completedLearner.getCurrentUserSingleQuizAttempt(completedAttempt.id).questions[0].answers[2].answer == 'Other reason: Completed text'
        inProgressLearner.getCurrentUserSingleQuizAttempt(inProgressAttempt.id).questions[0].answers[2].answer == 'Other reason: In progress text'
        skillsService.getQuizAttemptResult(quiz.quizId, completedAttempt.id).questions[0].answers[2].answer == 'Other reason: Completed text'
        skillsService.getQuizAttemptResult(quiz.quizId, inProgressAttempt.id).questions[0].answers[2].answer == 'Other reason: In progress text'

        where:
        questionType     | multiple
        'SingleChoice'   | false
        'MultipleChoice' | true
    }

    def "survey text responses: changing a choice question to Input Text turns off text responses - #questionType"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questionDef = multiple ? QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3) : QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 3)
        questionDef.answers[0].allowCustomText = true
        def question = skillsService.createQuizQuestionDef(questionDef).body
        question.questionType = QuizQuestionType.TextInput.toString()
        question.quizId = quiz.quizId
        question.answers = []
        skillsService.updateQuizQuestionDef(question)
        SkillsService learner = createLearner()

        when:
        def updated = skillsService.getQuizQuestionDef(quiz.quizId, question.id)
        def attempt = learner.startQuizAttempt(quiz.quizId).body
        def answerId = attempt.questions[0].answerOptions[0].id
        // the Input Text limit applies, not the 1,000-character limit for choice responses
        // (the test profile sets maxTakeQuizInputTextAnswerLength to 500)
        learner.reportQuizAnswer(quiz.quizId, attempt.id, answerId, [isSelected: true, answerText: 'y' * 1001])

        then:
        updated.questionType == QuizQuestionType.TextInput.toString()
        updated.answers.allowCustomText == [false]
        attempt.questions[0].answerOptions.allowCustomText == [false]
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        !e.message.contains('answerText must not exceed [1000] chars')
        e.message.contains('[Answer] must not exceed [500]')

        when:
        learner.reportQuizAnswer(quiz.quizId, attempt.id, answerId, [isSelected: true, answerText: 'z' * 500])
        def completed = learner.completeQuizAttempt(quiz.quizId, attempt.id).body
        def history = skillsService.getQuizAttemptResult(quiz.quizId, attempt.id)

        then:
        completed.passed
        history.questions[0].answers[0].answer == 'z' * 500

        where:
        questionType     | multiple
        'SingleChoice'   | false
        'MultipleChoice' | true
    }

    def "survey text responses: changing a choice question to Rating turns off text responses - #questionType"() {
        def quiz = QuizDefFactory.createQuizSurvey(1)
        skillsService.createQuizDef(quiz)
        def questionDef = multiple ? QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3) : QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 3)
        questionDef.answers[0].allowCustomText = true
        def question = skillsService.createQuizQuestionDef(questionDef).body
        question.questionType = QuizQuestionType.Rating.toString()
        question.quizId = quiz.quizId
        question.answers = []
        question.questionScale = 5
        skillsService.updateQuizQuestionDef(question)
        SkillsService learner = createLearner()

        when:
        def updated = skillsService.getQuizQuestionDef(quiz.quizId, question.id)
        def attempt = learner.startQuizAttempt(quiz.quizId).body
        def options = attempt.questions[0].answerOptions
        learner.reportQuizAnswer(quiz.quizId, attempt.id, options[3].id)
        def completed = learner.completeQuizAttempt(quiz.quizId, attempt.id).body

        then:
        updated.questionType == QuizQuestionType.Rating.toString()
        updated.answers.allowCustomText == [false] * 5
        options.allowCustomText == [false] * 5
        completed.passed

        where:
        questionType     | multiple
        'SingleChoice'   | false
        'MultipleChoice' | true
    }

    def "survey text responses: a learner cannot save a response to another learner's attempt - #questionType"() {
        def quiz = createTextResponseSurvey(multiple)
        def (SkillsService learner, SkillsService otherLearner) = createLearners(2)
        def attempt = learner.startQuizAttempt(quiz.quizId).body
        def otherAttempt = otherLearner.startQuizAttempt(quiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Learner text'])
        otherLearner.reportQuizAnswer(quiz.quizId, otherAttempt.id, option.id, [isSelected: true, answerText: 'Other learner text'])

        when:
        otherLearner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Overwritten text'])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided attempt id [${attempt.id}] does not exist for [${otherLearner.userName}] user and [${quiz.quizId}] quiz")
        e.message.contains("errorCode:${ErrorCode.BadParam}")
        enteredText(learner.startQuizAttempt(quiz.quizId).body) == [[option.id, 'Learner text']]
        enteredText(otherLearner.startQuizAttempt(quiz.quizId).body) == [[option.id, 'Other learner text']]

        where:
        questionType     | multiple
        'SingleChoice'   | false
        'MultipleChoice' | true
    }

    def "survey text responses: a response cannot be saved for an answer of another quiz - #questionType"() {
        def quiz = createTextResponseSurvey(multiple)
        def otherQuiz = createTextResponseSurvey(multiple, 2)
        SkillsService learner = createLearner()
        def attempt = learner.startQuizAttempt(quiz.quizId).body
        def otherAttempt = learner.startQuizAttempt(otherQuiz.quizId).body
        def option = attempt.questions[0].answerOptions[2]
        def otherOption = otherAttempt.questions[0].answerOptions[2]
        learner.reportQuizAnswer(quiz.quizId, attempt.id, option.id, [isSelected: true, answerText: 'Quiz text'])
        learner.reportQuizAnswer(otherQuiz.quizId, otherAttempt.id, otherOption.id, [isSelected: true, answerText: 'Other quiz text'])

        when:
        learner.reportQuizAnswer(quiz.quizId, attempt.id, otherOption.id, [isSelected: true, answerText: 'Cross quiz text'])

        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("Provided answer id [${otherOption.id}] does not exist for [${attempt.id}] quiz attempt")
        e.message.contains("errorCode:${ErrorCode.BadParam}")
        enteredText(learner.startQuizAttempt(quiz.quizId).body) == [[option.id, 'Quiz text']]
        enteredText(learner.startQuizAttempt(otherQuiz.quizId).body) == [[otherOption.id, 'Other quiz text']]

        where:
        questionType     | multiple
        'SingleChoice'   | false
        'MultipleChoice' | true
    }
}
