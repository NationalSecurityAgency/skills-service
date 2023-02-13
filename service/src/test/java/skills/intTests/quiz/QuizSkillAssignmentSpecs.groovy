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


import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.quizLoading.QuizSettings
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

@Slf4j
class QuizSkillAssignmentSpecs extends DefaultIntSpec {

    def "assign quiz to skill"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        when:
        skillsService.createSkill(skillWithQuiz)

        def skill = skillsService.getSkill(skillWithQuiz)
        def skills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill.quizId == quiz.body.quizId
        skill.quizName == quiz.body.name
        skill.quizType == QuizDefParent.QuizType.Quiz.toString()

        skills[0].selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skills[0].quizId == quiz.body.quizId
        skills[0].quizName == quiz.body.name
        skills[0].quizType == QuizDefParent.QuizType.Quiz.toString()
    }

    def "assign survey to skill"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuizSurvey(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        when:
        skillsService.createSkill(skillWithQuiz)

        def skill = skillsService.getSkill(skillWithQuiz)
        def skills = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill.quizId == quiz.body.quizId
        skill.quizName == quiz.body.name
        skill.quizType == QuizDefParent.QuizType.Survey.toString()

        skills[0].selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skills[0].quizId == quiz.body.quizId
        skills[0].quizName == quiz.body.name
        skills[0].quizType == QuizDefParent.QuizType.Survey.toString()
    }

    def "update quiz for an existing skill"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))
        def quiz1 = skillsService.createQuizDef(QuizDefFactory.createQuiz(2))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz)

        when:
        def skill_before = skillsService.getSkill(skillWithQuiz)
        def skills_before = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        skillWithQuiz.quizId = quiz1.body.quizId
        skillsService.createSkill(skillWithQuiz)
        def skill_after = skillsService.getSkill(skillWithQuiz)
        def skills_after = skillsService.getSkillsForSubject(proj.projectId, subj.subjectId)
        then:
        skill_before.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill_before.quizId == quiz.body.quizId
        skill_before.quizName == quiz.body.name

        skills_before[0].selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skills_before[0].quizId == quiz.body.quizId
        skills_before[0].quizName == quiz.body.name

        skill_after.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill_after.quizId == quiz1.body.quizId
        skill_after.quizName == quiz1.body.name

        skills_after[0].selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skills_after[0].quizId == quiz1.body.quizId
        skills_after[0].quizName == quiz1.body.name
    }

    def "skill with a quiz must only have 1 occurrence"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 2, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        when:
        skillsService.createSkill(skillWithQuiz)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.BAD_REQUEST
        e.message.contains("When quizId is provided numPerformToCompletion must be equal 1")
        e.message.contains("skillId:${skillWithQuiz.skillId}")
    }

    def "quiz assignment is removed"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz)

        def skill = skillsService.getSkill(skillWithQuiz)
        when:
        skillWithQuiz.quizId = null
        skillWithQuiz.selfReportingType = null

        skillsService.createSkill(skillWithQuiz)
        def skill1 = skillsService.getSkill(skillWithQuiz)

        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill.quizId == quiz.body.quizId
        skill.quizName == quiz.body.name

        !skill1.selfReportingType
        !skill1.quizId
        !skill1.quizName
    }

    def "quiz assignment is changed to another self report type"() {
        def quiz = skillsService.createQuizDef(QuizDefFactory.createQuiz(1))

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skillWithQuiz = createSkill(1, 1, 1, 1, 1, 480, 200)
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Quiz
        skillWithQuiz.quizId = quiz.body.quizId

        skillsService.createSkill(skillWithQuiz)

        def skill = skillsService.getSkill(skillWithQuiz)
        when:
        skillWithQuiz.quizId = null
        skillWithQuiz.selfReportingType = SkillDef.SelfReportingType.Approval

        skillsService.createSkill(skillWithQuiz)
        def skill1 = skillsService.getSkill(skillWithQuiz)

        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill.quizId == quiz.body.quizId
        skill.quizName == quiz.body.name

        skill1.selfReportingType  == SkillDef.SelfReportingType.Approval.toString()
        !skill1.quizId
        !skill1.quizName
    }

}

