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
import org.springframework.security.core.userdetails.UserDetails
import skills.auth.pki.PkiUserDetailsService
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.quizLoading.QuizSettings
import skills.services.quiz.QuizQuestionType
import skills.storage.model.QuizDefParent
import skills.storage.model.SkillDef
import skills.storage.model.auth.RoleName
import spock.lang.Ignore

import java.util.concurrent.atomic.AtomicInteger

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

    def "root users can view all quizzes"() {
        def users = getRandomUsers(5, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])
        def user1 = users[0]
        def root = users[1]
        def user2 = users[2]
        def user3 = users[3]
        SkillsService quizUser1 = createService(user1)
        SkillsService rootUser = createRootSkillService(root)
        SkillsService quizUser2 = createService(user2)
        SkillsService nonQuizUserService = createService(user3)

        def quiz1 = QuizDefFactory.createQuiz(1)
        quizUser1.createQuizDef(quiz1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        quizUser1.createQuizQuestionDefs(questions)
        def proj = createProject(1)
        def subj = createSubject(1, 1)
        quizUser1.createProjectAndSubjectAndSkills(proj, subj, [])
        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        quizUser1.createSkills(skills)
        quizUser1.saveQuizSettings(quiz1.quizId, [
                [setting: "some", value: '2'],
        ])

        def quiz2 = QuizDefFactory.createQuiz(2)
        quizUser2.createQuizDef(quiz2)
        def questions2 = QuizDefFactory.createChoiceQuestions(2, 2, 2)
        quizUser2.createQuizQuestionDefs(questions2)
        def proj2 = createProject(2)
        def subj2 = createSubject(2, 1)
        quizUser2.createProjectAndSubjectAndSkills(proj2, subj2, [])
        def skills2 = createSkills(3, 2, 1, 100, 1)
        skills2[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills2[0].quizId = quiz2.quizId
        quizUser2.createSkills(skills2)
        quizUser2.saveQuizSettings(quiz2.quizId, [
                [setting: "some", value: '2'],
        ])


        when:
        def user1QuizDefRes = quizUser1.getQuizDefs()
        def user2QuizDefRes = quizUser2.getQuizDefs()
        def nonQuizUserQuizDefRes = nonQuizUserService.getQuizDefs()
        def rootQuizDefRes = rootUser.getQuizDefs()

        then:
        rootQuizDefRes.size() == 2
        rootQuizDefRes.find( it -> it.quizId == quiz1.quizId )
        rootQuizDefRes.find( it -> it.quizId == quiz2.quizId )
        user1QuizDefRes.size() == 1
        user1QuizDefRes[0].quizId == quiz1.quizId
        user2QuizDefRes.size() == 1
        user2QuizDefRes[0].quizId == quiz2.quizId
        nonQuizUserQuizDefRes.size() == 0
    }

    def "if quiz is assigned to skill, although any project admin in that project gets a read-only view of the quiz, catalog imported skills do not get the same treatment"() {
        def quiz1 = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz1)
        def questions = QuizDefFactory.createChoiceQuestions(1, 2, 2)
        skillsService.createQuizQuestionDefs(questions)

        def questionDefs = skillsService.getQuizQuestionDefs(quiz1.quizId)

        def proj = createProject(1)
        def subj = createSubject(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [])

        def skills = createSkills(3, 1, 1, 100, 1)
        skills[0].selfReportingType = SkillDef.SelfReportingType.Quiz
        skills[0].quizId = quiz1.quizId
        skillsService.createSkills(skills)
        skills.each { skillsService.exportSkillToCatalog(proj.projectId, it.skillId) }

        def user = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]

        SkillsService otherUser = createService(user)
        def proj2 = createProject(2)
        def proj2_subj = createSubject(2, 1)
        otherUser.createProjectAndSubjectAndSkills(proj2, proj2_subj, [])

        when:
        otherUser.bulkImportSkillsFromCatalog(proj2.projectId, proj2_subj.subjectId, [
                [projectId: proj.projectId, skillId: skills[0].skillId],
                [projectId: proj.projectId, skillId: skills[1].skillId],
                [projectId: proj.projectId, skillId: skills[2].skillId],
        ])
        otherUser.finalizeSkillsImportFromCatalog(proj2.projectId)
        then:

        validateForbidden { otherUser.getQuizDef(quiz1.quizId) }
        validateForbidden { otherUser.getQuizQuestionDefs(quiz1.quizId) }
        validateForbidden { otherUser.getQuizQuestionDef(quiz1.quizId, questionDefs.questions[0].id) }
        validateForbidden { otherUser.getQuizSettings(quiz1.quizId) }
        validateForbidden { otherUser.getQuizMetrics(quiz1.quizId) }
        validateForbidden { otherUser.getQuizDefSummary(quiz1.quizId) }

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

