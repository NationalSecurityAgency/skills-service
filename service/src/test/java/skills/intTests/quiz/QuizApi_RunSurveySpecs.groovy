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

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[2].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'This is user provided answer'])

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

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'This is user provided answer'])

        def restartedQuizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        then:
        restartedQuizAttempt.selectedAnswerIds == [quizInfo.questions[0].answerOptions[1].id]
        restartedQuizAttempt.enteredText.size() == 1
        restartedQuizAttempt.enteredText.find { it.answerId == quizInfo.questions[2].answerOptions[0].id }.answerText == 'This is user provided answer'
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

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        when:
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[0].answerOptions[1].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[1].answerOptions[2].id)
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'ans'])

        def quizAttempt_t1 =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:false, answerText: '   '])
        def quizAttempt_t2 =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'ans1'])
        def quizAttempt_t3 =  skillsService.startQuizAttempt(quiz.quizId).body

        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:false, answerText: null])
        def quizAttempt_t4 =  skillsService.startQuizAttempt(quiz.quizId).body

        then:
        quizAttempt_t1.enteredText.answerId == [quizInfo.questions[2].answerOptions[0].id]
        quizAttempt_t1.enteredText.answerText == ['ans']

        !quizAttempt_t2.enteredText

        quizAttempt_t3.enteredText.answerId == [quizInfo.questions[2].answerOptions[0].id]
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

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        when:
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:true, answerText: null])
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

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        when:
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:true, answerText: '    '])
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

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        when:
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:false, answerText: '  blah   '])
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

        def quizInfo = skillsService.getQuizInfo(quiz.quizId)
        def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
        when:
        skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizInfo.questions[2].answerOptions[0].id, [isSelected:true, answerText: 'Jabberwocky text'])
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
}
