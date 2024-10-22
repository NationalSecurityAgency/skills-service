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


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class QuizApi_LoadSkillInfoSpecs extends DefaultIntSpec {

    def "return quiz information with the skills"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(1)

        when:
        def skillsRes = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def skillRes = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[0].skillId)
        then:
        skillsRes.skills[0].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillsRes.skills[0].selfReporting.quizId == quiz.quizId
        skillsRes.skills[0].selfReporting.quizName == quiz.name
        skillsRes.skills[0].selfReporting.numQuizQuestions == 5

        !skillsRes.skills[1].selfReporting.enabled
        !skillsRes.skills[2].selfReporting.enabled

        skillRes.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillRes.selfReporting.quizId == quiz.quizId
        skillRes.selfReporting.quizName == quiz.name
        skillRes.selfReporting.numQuizQuestions == 5
    }

    def "return quiz information with the skills - same quiz is associated to multiple skills"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz.quizId
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(1)

        when:
        def skillsRes = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def skillRes = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[0].skillId)
        def skill1Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[1].skillId)
        then:
        skillsRes.skills[0].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillsRes.skills[0].selfReporting.quizId == quiz.quizId
        skillsRes.skills[0].selfReporting.quizName == quiz.name
        skillsRes.skills[0].selfReporting.numQuizQuestions == 5

        skillsRes.skills[1].selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillsRes.skills[1].selfReporting.quizId == quiz.quizId
        skillsRes.skills[1].selfReporting.quizName == quiz.name
        skillsRes.skills[1].selfReporting.numQuizQuestions == 5

        !skillsRes.skills[2].selfReporting.enabled

        skillRes.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillRes.selfReporting.quizId == quiz.quizId
        skillRes.selfReporting.quizName == quiz.name
        skillRes.selfReporting.numQuizQuestions == 5

        skill1Res.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skill1Res.selfReporting.quizId == quiz.quizId
        skill1Res.selfReporting.quizName == quiz.name
        skill1Res.selfReporting.numQuizQuestions == 5
    }

    def "return survey information with the skills"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3, QuizDefParent.QuizType.Survey),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4, QuizDefParent.QuizType.Survey),
                QuizDefFactory.createTextInputQuestion(1, 3),
        ]
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz.quizId
        skillsService.createSkills(skills)

        List<String> users = getRandomUsers(1)

        when:
        def skillsRes = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def skillRes = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[0].skillId)
        then:
        skillsRes.skills[0].selfReporting.type == "Survey"
        skillsRes.skills[0].selfReporting.quizId == quiz.quizId
        skillsRes.skills[0].selfReporting.quizName == quiz.name
        skillsRes.skills[0].selfReporting.numQuizQuestions == 3
        !skillsRes.skills[1].selfReporting.enabled
        !skillsRes.skills[2].selfReporting.enabled

        skillRes.selfReporting.type == "Survey"
        skillRes.selfReporting.quizId == quiz.quizId
        skillRes.selfReporting.quizName == quiz.name
        skillsRes.skills[0].selfReporting.numQuizQuestions == 3
    }

    def "return quiz num questions with the skills"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(1, 1, 2))

        def quiz2 = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(quiz2)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, 2, 2))

        def quiz3 = QuizDefFactory.createQuiz(3)
        skillsService.createQuizDef(quiz3)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(3, 3, 2))

        def quiz4 = QuizDefFactory.createQuiz(4)
        skillsService.createQuizDef(quiz4)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(4, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skills[1].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[1].quizId = quiz2.quizId
        skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[2].quizId = quiz3.quizId
        skills[3].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[3].quizId = quiz4.quizId
        skillsService.createSkills(skills)


        Map badge = [projectId: proj.projectId, badgeId: 'badge1', name: 'Test Badge 1']
        skillsService.addBadge(badge)
        skills.each {
            skillsService.assignSkillToBadge(projectId: proj.projectId, badgeId: badge.badgeId, skillId: it.skillId)
        }
        badge.enabled = 'true'
        skillsService.updateBadge(badge, badge.badgeId)

        List<String> users = getRandomUsers(1)

        when:
        def skillsRes = skillsService.getSkillSummary(users[0], proj.projectId, subj.subjectId)
        def skill1Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[0].skillId)
        def skill2Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[1].skillId)
        def skill3Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[2].skillId)
        def skill4Res = skillsService.getSingleSkillSummary(users[0], proj.projectId, skills[3].skillId)
        def badgeSummary = skillsService.getBadgeSummary(users[0], proj.projectId, badge.badgeId)
        then:
        skillsRes.skills[0].selfReporting.numQuizQuestions == 1
        skillsRes.skills[0].selfReporting.quizName == quiz1.name
        skillsRes.skills[1].selfReporting.numQuizQuestions == 2
        skillsRes.skills[1].selfReporting.quizName == quiz2.name
        skillsRes.skills[2].selfReporting.numQuizQuestions == 3
        skillsRes.skills[2].selfReporting.quizName == quiz3.name
        skillsRes.skills[3].selfReporting.numQuizQuestions == 0
        skillsRes.skills[3].selfReporting.quizName == quiz4.name
        skill1Res.selfReporting.numQuizQuestions == 1
        skill1Res.selfReporting.quizName == quiz1.name
        skill2Res.selfReporting.numQuizQuestions == 2
        skill2Res.selfReporting.quizName == quiz2.name
        skill3Res.selfReporting.numQuizQuestions == 3
        skill3Res.selfReporting.quizName == quiz3.name
        skill4Res.selfReporting.numQuizQuestions == 0
        skill4Res.selfReporting.quizName == quiz4.name

        badgeSummary.skills[0].selfReporting.numQuizQuestions == 1
        badgeSummary.skills[0].selfReporting.quizName == quiz1.name
        badgeSummary.skills[1].selfReporting.numQuizQuestions == 2
        badgeSummary.skills[1].selfReporting.quizName == quiz2.name
        badgeSummary.skills[2].selfReporting.numQuizQuestions == 3
        badgeSummary.skills[2].selfReporting.quizName == quiz3.name
        badgeSummary.skills[3].selfReporting.numQuizQuestions == 0
        badgeSummary.skills[3].selfReporting.quizName == quiz4.name
    }
}


