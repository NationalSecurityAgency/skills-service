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

import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.quizLoading.QuizSettings
import skills.services.attributes.ExpirationAttrs
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class QuizAndSurveyRetakesSpecs extends DefaultIntSpec {

    def "ability to retake passed quizzes when MultipleTakes=true"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(quiz.quizId, [
                [setting: QuizSettings.MultipleTakes.setting, value: true],
        ])

        def runQuiz = {
            def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
            skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
            return skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id)
        }
        when:
        def res1 = runQuiz().body
        def res2 = runQuiz().body
        def res3 = runQuiz().body
        then:
        res1.passed == true
        res2.passed == true
        res3.passed == true
    }

    def "can only take quiz once MultipleTakes=false"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkill(skills[0])

        def runQuiz = {
            def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId).body
            skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
            return skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id)
        }

        def res1 = runQuiz().body
        when:
        runQuiz().body
        then:
        res1.passed == true
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("User [${skillsService.userName}] already took and passed this quiz")
    }

    def "allow multiple quiz attempts if associated skill configured with daily expiration"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 1, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkill(skills[0])

        skillsService.saveSkillExpirationAttributes( proj.projectId, skills[0].skillId, [
                expirationType: ExpirationAttrs.DAILY.toString(),
                every: 7,
        ])

        def runQuiz = {
            def quizAttempt =  skillsService.startQuizAttempt(quiz.quizId, null, [projectId : proj.projectId, skillId: skills[0].skillId]).body
            skillsService.reportQuizAnswer(quiz.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
            return skillsService.completeQuizAttempt(quiz.quizId, quizAttempt.id)
        }

        when:
        def res1 = runQuiz().body
        def res2 = runQuiz().body
        def res3 = runQuiz().body
        then:
        res1.passed == true
        res2.passed == true
        res3.passed == true
    }

    def "ability to retake completed surveys when MultipleTakes=true"() {
        def survey = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(survey)
        def questions = [QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 2)]
        skillsService.createQuizQuestionDefs(questions)

        skillsService.saveQuizSettings(survey.quizId, [
                [setting: QuizSettings.MultipleTakes.setting, value: true],
        ])

        def runQuiz = {
            def quizAttempt =  skillsService.startQuizAttempt(survey.quizId).body
            skillsService.reportQuizAnswer(survey.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
            return skillsService.completeQuizAttempt(survey.quizId, quizAttempt.id)
        }
        when:
        def res1 = runQuiz().body
        def res2 = runQuiz().body
        def res3 = runQuiz().body
        then:
        res1.passed == true
        res2.passed == true
        res3.passed == true
    }

    def "can only take survey once MultipleTakes=false"() {
        def survey = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(survey)
        def questions = [QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 2)]
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = survey.quizId
        skillsService.createSkill(skills[0])

        def runQuiz = {
            def quizAttempt =  skillsService.startQuizAttempt(survey.quizId).body
            skillsService.reportQuizAnswer(survey.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
            return skillsService.completeQuizAttempt(survey.quizId, quizAttempt.id)
        }

        def res1 = runQuiz().body
        when:
        runQuiz().body
        then:
        res1.passed == true
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("User [${skillsService.userName}] has already taken this survey")
    }

    def "allow multiple survey attempts if associated skill configured with daily expiration"() {
        def survey = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(survey)
        def questions = [QuizDefFactory.createSingleChoiceSurveyQuestion(1, 1, 2)]
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        def skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = survey.quizId
        skillsService.createSkill(skills[0])

        skillsService.saveSkillExpirationAttributes( proj.projectId, skills[0].skillId, [
                expirationType: ExpirationAttrs.DAILY.toString(),
                every: 7,
        ])

        def runQuiz = {
            def quizAttempt =  skillsService.startQuizAttempt(survey.quizId, null, [projectId : proj.projectId, skillId: skills[0].skillId]).body
            skillsService.reportQuizAnswer(survey.quizId, quizAttempt.id, quizAttempt.questions[0].answerOptions[0].id)
            return skillsService.completeQuizAttempt(survey.quizId, quizAttempt.id)
        }

        when:
        def res1 = runQuiz().body
        def res2 = runQuiz().body
        def res3 = runQuiz().body
        then:
        res1.passed == true
        res2.passed == true
        res3.passed == true
    }

}


