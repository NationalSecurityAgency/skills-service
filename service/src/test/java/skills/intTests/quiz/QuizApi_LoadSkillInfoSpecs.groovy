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
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class QuizApi_LoadSkillInfoSpecs extends DefaultIntSpec {

    def "return quiz information with the skills"() {
        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createMultipleChoiceQuestions(1, 5, 2)
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
        !skillsRes.skills[1].selfReporting
        !skillsRes.skills[2].selfReporting

        skillRes.selfReporting.type == SkillDef.SelfReportingType.Quiz.toString()
        skillRes.selfReporting.quizId == quiz.quizId
        skillRes.selfReporting.quizName == quiz.name
    }

    def "return survey information with the skills"() {
        def quiz = QuizDefFactory.createQuizSurvey(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = [
                QuizDefFactory.createMultipleChoiceSurveyQuestion(1, 1, 3),
                QuizDefFactory.createSingleChoiceSurveyQuestion(1, 2, 4),
                QuizDefFactory.createTextInputSurveyQuestion(1, 3),
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
        !skillsRes.skills[1].selfReporting
        !skillsRes.skills[2].selfReporting

        skillRes.selfReporting.type == "Survey"
        skillRes.selfReporting.quizId == quiz.quizId
        skillRes.selfReporting.quizName == quiz.name
    }

}
