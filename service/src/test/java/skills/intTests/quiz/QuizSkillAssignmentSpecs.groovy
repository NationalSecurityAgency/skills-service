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

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSubject
import static skills.intTests.utils.SkillsFactory.createSkill

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
        println JsonOutput.prettyPrint(JsonOutput.toJson(skills))
        then:
        skill.selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skill.quizId == quiz.body.quizId
        skill.quizName == quiz.body.name

        skills[0].selfReportingType == SkillDef.SelfReportingType.Quiz.toString()
        skills[0].quizId == quiz.body.quizId
        skills[0].quizName == quiz.body.name
    }

}

