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
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSkills
import static skills.intTests.utils.SkillsFactory.createSubject

@Slf4j
class QuizDefAuthSpecs extends DefaultIntSpec {

    def "users that do not have Quiz Admin Role cannot mutate and view quiz"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)
        // create project where projectId = quizId
        skillsService.createProject([projectId: quiz1.quizId, name: "Some Project Name"])

        when:
        def questionDefs = skillsService.getQuizQuestionDefs(quiz1.quizId)

        then:
        validateForbidden { otherUser.removeQuizDef(quiz1.quizId) }
        validateForbidden { otherUser.createQuizDef(quiz1, quiz1.quizId) }
        validateForbidden { otherUser.createQuizQuestionDefs(questions) }
        validateForbidden { otherUser.deleteQuizQuestionDef(quiz1.quizId, questionDefs.questions[0].id) }
        validateForbidden {
            otherUser.saveQuizSettings(quiz1.quizId, [
                    [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '2'],
            ])
        }

        validateForbidden { otherUser.getQuizDef(quiz1.quizId) }
        validateForbidden { otherUser.getQuizQuestionDefs(quiz1.quizId) }
        validateForbidden { otherUser.getQuizQuestionDef(quiz1.quizId, questionDefs.questions[0].id) }
        validateForbidden { otherUser.getQuizMetrics(quiz1.quizId) }
        validateForbidden { otherUser.getQuizSettings(quiz1.quizId) }
        validateForbidden { otherUser.getQuizDefSummary(quiz1.quizId) }
    }

    def "if quiz is assigned to skill, any project admin in that project gets a read-only view of the quiz"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createSkills(skills)

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUser = createService(user)
        skillsService.addProjectAdmin(proj.projectId, otherUser.userName)

        skillsService.saveQuizSettings(quiz1.quizId, [
                [setting: "some", value: '2'],
        ])
        when:
        def quizDefRes = otherUser.getQuizDef(quiz1.quizId)
        def questionDefs = otherUser.getQuizQuestionDefs(quiz1.quizId)
        def questionDef = otherUser.getQuizQuestionDef(quiz1.quizId, questionDefs.questions[0].id)
        def quizMetrics = otherUser.getQuizMetrics(quiz1.quizId)
        def quizSettings = otherUser.getQuizSettings(quiz1.quizId)
        def quizSummary = otherUser.getQuizDefSummary(quiz1.quizId)
        then:
        quizDefRes
        questionDefs
        questionDef
        quizMetrics
        quizSettings
        quizSummary

        // must not be able to mutate
        validateForbidden { otherUser.removeQuizDef(quiz1.quizId) }
        validateForbidden { otherUser.createQuizDef(quiz1, quiz1.quizId) }
        validateForbidden { otherUser.createQuizQuestionDefs(questions) }
        validateForbidden { otherUser.deleteQuizQuestionDef(quiz1.quizId, questionDefs.questions[0].id) }
        validateForbidden {
            otherUser.saveQuizSettings(quiz1.quizId, [
                    [setting: QuizSettings.MinNumQuestionsToPass.setting, value: '2'],
            ])
        }
    }

    private boolean validateForbidden(Closure c) {
        try {
            c.call()
            return false
        } catch (SkillsClientException skillsClientException) {
            return skillsClientException.httpStatus == org.springframework.http.HttpStatus.FORBIDDEN
        }
        return false
    }

}

