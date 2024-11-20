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
package skills.intTests.copySubject

import groovy.json.JsonOutput
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.*

class CopySubjectToAnotherProjSpecs extends DefaultIntSpec {

    def "copy subject with 1 skill" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(1, 1, 1, 100)
        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def copiedSubject1 = skillsService.getSubject([subjectId: p1subj1.subjectId, projectId: p2.projectId])
        def copiedSubj1Skills = skillsService.getSkillsForSubject(p2.projectId, p1subj1.subjectId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(copiedSubj1Skills))
        then:
        copiedSubject1.name == p1subj1.name
        copiedSubject1.subjectId == p1subj1.subjectId
        copiedSubject1.numGroups == 0
        copiedSubject1.numSkills == 1
        copiedSubject1.totalPoints == (100)
        copiedSubject1.numSkillsReused == 0
        copiedSubject1.totalPointsReused == 0

        copiedSubj1Skills.skillId == p1Subj1Skills.skillId
        copiedSubj1Skills.name == p1Subj1Skills.name
        copiedSubj1Skills[0].projectId == p2.projectId
        copiedSubj1Skills[0].type == "Skill"
        copiedSubj1Skills[0].pointIncrement == 100
        copiedSubj1Skills[0].numMaxOccurrencesIncrementInterval == 1
        copiedSubj1Skills[0].numPerformToCompletion == 1
        copiedSubj1Skills[0].expirationType == "NEVER"
    }

    def "copy subject with multiple skills of various attributes" () {
        def p1 = createProject(1)
        def p1subj1 = createSubject(1, 1)
        def p1Subj1Skills = createSkills(3, 1, 1, 100)
        p1Subj1Skills[0].pointIncrement = 100
        p1Subj1Skills[0].numPerformToCompletion = 1
        p1Subj1Skills[0].pointIncrementInterval = 480
        p1Subj1Skills[0].numMaxOccurrencesIncrementInterval = 1
        p1Subj1Skills[0].description = 'first skill'
        p1Subj1Skills[0].helpUrl = 'https://first.com'

        p1Subj1Skills[1].pointIncrement = 200
        p1Subj1Skills[1].numPerformToCompletion = 4
        p1Subj1Skills[1].pointIncrementInterval = 880
        p1Subj1Skills[1].numMaxOccurrencesIncrementInterval = 2
        p1Subj1Skills[1].description = 'second skill'
        p1Subj1Skills[1].helpUrl = 'https://second.com'


        def quiz = QuizDefFactory.createQuiz(1, "Fancy Description")
        skillsService.createQuizDef(quiz)
        def questions = QuizDefFactory.createChoiceQuestions(1, 5, 2)
        skillsService.createQuizQuestionDefs(questions)

        p1Subj1Skills[2].pointIncrement = 300
        p1Subj1Skills[2].numPerformToCompletion = 1
        p1Subj1Skills[2].pointIncrementInterval = 500
        p1Subj1Skills[2].numMaxOccurrencesIncrementInterval = 1
        p1Subj1Skills[2].description = 'third skill'
        p1Subj1Skills[2].helpUrl = 'https://third.com'
        p1Subj1Skills[2].selfReportingType = SkillDef.SelfReportingType.Quiz
        p1Subj1Skills[2].quizId = quiz.quizId

        skillsService.createProjectAndSubjectAndSkills(p1, p1subj1, p1Subj1Skills)

        def p2 = createProject(2)
        skillsService.createProject(p2)

        when:
        skillsService.copySubjectDefIntoAnotherProject(p1.projectId, p1subj1.subjectId, p2.projectId)

        def copiedSubject1 = skillsService.getSubject([subjectId: p1subj1.subjectId, projectId: p2.projectId])
        def copiedSubj1Skills = skillsService.getSkillsForSubject(p2.projectId, p1subj1.subjectId)
        println JsonOutput.prettyPrint(JsonOutput.toJson(copiedSubj1Skills))
        then:
        copiedSubject1.name == p1subj1.name
        copiedSubject1.subjectId == p1subj1.subjectId
        copiedSubject1.numGroups == 0
        copiedSubject1.numSkills == 3
        copiedSubject1.totalPoints == (100+(200*4)+300)
        copiedSubject1.numSkillsReused == 0
        copiedSubject1.totalPointsReused == 0

        copiedSubj1Skills.size() == 3
        copiedSubj1Skills.skillId == p1Subj1Skills.skillId
        copiedSubj1Skills.name == p1Subj1Skills.name
        copiedSubj1Skills[0].projectId == p2.projectId
        copiedSubj1Skills[0].type == "Skill"
        copiedSubj1Skills[0].pointIncrement == 100
        copiedSubj1Skills[0].numPerformToCompletion == 1
        copiedSubj1Skills[0].numMaxOccurrencesIncrementInterval == 1
        copiedSubj1Skills[0].pointIncrementInterval == 480
        copiedSubj1Skills[0].expirationType == "NEVER"
        copiedSubj1Skills[0].quizType == null
        copiedSubj1Skills[0].quizId == null
        copiedSubj1Skills[0].quizName == null

        copiedSubj1Skills[1].projectId == p2.projectId
        copiedSubj1Skills[1].type == "Skill"
        copiedSubj1Skills[1].pointIncrement == 200
        copiedSubj1Skills[1].numPerformToCompletion == 4
        copiedSubj1Skills[1].numMaxOccurrencesIncrementInterval == 2
        copiedSubj1Skills[1].pointIncrementInterval == 880
        copiedSubj1Skills[1].expirationType == "NEVER"
        copiedSubj1Skills[1].quizType == null
        copiedSubj1Skills[1].quizId == null
        copiedSubj1Skills[1].quizName == null

        copiedSubj1Skills[2].projectId == p2.projectId
        copiedSubj1Skills[2].type == "Skill"
        copiedSubj1Skills[2].pointIncrement == 300
        copiedSubj1Skills[2].numPerformToCompletion == 1
        copiedSubj1Skills[2].numMaxOccurrencesIncrementInterval == 1
        copiedSubj1Skills[2].pointIncrementInterval == 500
        copiedSubj1Skills[2].expirationType == "NEVER"
        copiedSubj1Skills[2].quizType == QuizDefParent.QuizType.Quiz.toString()
        copiedSubj1Skills[2].quizId == quiz.quizId
        copiedSubj1Skills[2].quizName == quiz.name
    }

    // TODO:
    // copy groups
    // copy expiration attributes
    // default to subject levels if subject is not present; keep the levels if it is
        // avoid the oddness if projects declared with different point systems
    // do not copy reused skills
    // do not copy catalog skills
    // copy icons

    // validation tests
    //   admin of the dest project
    //   subject is is actually subject id
    //  dest project exist
    // subject id is for a subject (not a skill)

}
