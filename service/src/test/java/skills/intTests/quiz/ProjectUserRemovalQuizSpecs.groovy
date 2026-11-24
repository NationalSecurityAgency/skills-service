/**
 * Copyright 2026 SkillTree
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

import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef.SelfReportingType
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.SkillsFactory.*

class ProjectUserRemovalQuizSpecs extends QuizSkillAchievementsBaseIntSpec {

    def "#operation for an absent project user preserves shared quiz attempts and other project progress - archived #archived"() {
        List<String> users = getRandomUsers(4)
        SkillsService projectAdmin = createService(users[0])
        SkillsService otherProjectAdmin = createService(users[1])
        SkillsService quizAdmin = createService(users[2])
        SkillsService learner = createService(users[3])
        Map quiz = QuizDefFactory.createQuiz(1)
        quizAdmin.createQuizDef(quiz)
        quizAdmin.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(1, 1, 2))
        Map disabledSkill = createSkill(1, 1, 1, 0, 1, 0, 100) +
                [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId, enabled: 'false']
        Map sharedSkill = createSkill(2, 1, 2, 0, 1, 0, 100) +
                [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map unrelated = createSkill(2, 1, 3, 0, 1, 0, 100)
        projectAdmin.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [disabledSkill])
        otherProjectAdmin.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [sharedSkill, unrelated])
        Integer passedAttempt = passQuiz(learner, quiz)
        otherProjectAdmin.addSkill(unrelated, learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        if (archived) {
            projectAdmin.archiveUsers([learner.userName], 'TestProject1')
        }
        Long actionsBefore = projectAdmin.getUserActionsForProject('TestProject1').totalCount

        expect:
        otherProjectAdmin.getUserRolesForProjectAndUser('TestProject2', projectAdmin.userName).isEmpty()
        quizAdmin.getQuizUserRoles(quiz.quizId)*.userId == [quizAdmin.userName]
        projectAdmin.getProjectUsers('TestProject1').count == 0
        projectAdmin.getArchivedUsers('TestProject1').data*.userId == (archived ? [learner.userName] : [])
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 200
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 2
        quizAdmin.getQuizRuns(quiz.quizId).data*.attemptId == [passedAttempt]

        when:
        // Only the first removal of archived membership should add an audit action.
        List<Map> results = (1..2).collect { int request ->
            if (removeFromProject) {
                return projectAdmin.removeUserFromProject('TestProject1', learner.userName)
            }
            return projectAdmin.deleteAllSkillEvents([projectId: 'TestProject1', userId: learner.userName])
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        results.every { Map result -> result.success }
        projectAdmin.getProjectUsers('TestProject1').count == 0
        projectAdmin.getUserActionsForProject('TestProject1').totalCount == actionsBefore + (archived && removeFromProject ? 1 : 0)
        projectAdmin.getArchivedUsers('TestProject1').data*.userId == (archived && !removeFromProject ? [learner.userName] : [])
        quizAdmin.getQuizRuns(quiz.quizId).data*.attemptId == [passedAttempt]
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 200
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 2
        otherProjectAdmin.getPerformedSkills(learner.userName, 'TestProject2').data*.skillId.sort() ==
                [sharedSkill.skillId, unrelated.skillId].sort()
        otherProjectAdmin.getProjectUsers('TestProject2').data*.userId == [learner.userName]

        where:
        operation     | removeFromProject | archived
        'Remove User' | true              | false
        'Delete All'  | false             | false
        'Remove User' | true              | true
        'Delete All'  | false             | true
    }

    def "removal clears passed shared quiz attempts and dependent progress but retains failed attempts and unrelated progress"() {
        Map quiz = createQuiz(1)
        Map sourceSkill = createSkill(1, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map sharedSkill = createSkill(2, 1, 2, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: quiz.quizId]
        Map unrelated = createSkill(2, 1, 3, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [sourceSkill])
        skillsService.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [sharedSkill, unrelated])
        SkillsService learner = createService(getRandomUsers(1)[0])
        Integer failedAttempt = failQuiz(learner, quiz)
        passQuiz(learner, quiz)
        skillsService.addSkill(unrelated, learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        expect:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 100
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 200

        when:
        skillsService.removeUserFromProject('TestProject1', learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').skillsAchieved == 0
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 100
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 1
        skillsService.getPerformedSkills(learner.userName, 'TestProject2').data*.skillId == [unrelated.skillId]
        skillsService.getQuizRuns(quiz.quizId).data*.attemptId == [failedAttempt]
        skillsService.getProjectUsers('TestProject1').count == 0
    }

    def "project quiz association authorizes rolling removal without administering the shared quiz or other project"() {
        List<String> users = getRandomUsers(4)
        SkillsService projectAdmin = createService(users[0])
        SkillsService otherProjectAdmin = createService(users[1])
        SkillsService quizAdmin = createService(users[2])
        SkillsService learner = createService(users[3])
        Map sharedQuiz = QuizDefFactory.createQuiz(1)
        Map unrelatedQuiz = QuizDefFactory.createQuiz(2)
        quizAdmin.createQuizDef(sharedQuiz)
        quizAdmin.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(1, 1, 2))
        quizAdmin.createQuizDef(unrelatedQuiz)
        quizAdmin.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, 1, 2))
        Map sourceSkill = createSkill(1, 1, 1, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: sharedQuiz.quizId]
        Map sharedSkill = createSkill(2, 1, 2, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: sharedQuiz.quizId]
        Map unrelatedSkill = createSkill(2, 1, 3, 0, 1, 0, 100)
        Map unrelatedQuizSkill = createSkill(2, 1, 4, 0, 1, 0, 100) + [selfReportingType: SelfReportingType.Quiz, quizId: unrelatedQuiz.quizId]
        projectAdmin.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [sourceSkill])
        otherProjectAdmin.createProjectAndSubjectAndSkills(createProject(2), createSubject(2, 1), [sharedSkill, unrelatedSkill, unrelatedQuizSkill])
        Integer sharedAttempt = passQuiz(learner, sharedQuiz)
        Integer unrelatedAttempt = passQuiz(learner, unrelatedQuiz)
        otherProjectAdmin.addSkill(unrelatedSkill, learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        expect:
        !projectAdmin.isRoot()
        projectAdmin.getUserRolesForProjectAndUser('TestProject1', projectAdmin.userName)*.roleName == [RoleName.ROLE_PROJECT_ADMIN.toString()]
        otherProjectAdmin.getUserRolesForProjectAndUser('TestProject2', projectAdmin.userName).isEmpty()
        quizAdmin.getQuizUserRoles(sharedQuiz.quizId)*.userId == [quizAdmin.userName]
        quizAdmin.getQuizRuns(sharedQuiz.quizId).data*.attemptId == [sharedAttempt]
        quizAdmin.getQuizRuns(unrelatedQuiz.quizId).data*.attemptId == [unrelatedAttempt]
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 100
        learner.getSkillsSummaryForCurrentUser('TestProject1').skillsAchieved == 1
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 300
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 3

        when:
        Map result = projectAdmin.removeUserFromProject('TestProject1', learner.userName)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        result.success
        learner.getSkillsSummaryForCurrentUser('TestProject1').points == 0
        learner.getSkillsSummaryForCurrentUser('TestProject1').skillsAchieved == 0
        projectAdmin.getProjectUsers('TestProject1').count == 0
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 200
        learner.getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 2
        otherProjectAdmin.getPerformedSkills(learner.userName, 'TestProject2').data*.skillId.sort() == [unrelatedSkill.skillId, unrelatedQuizSkill.skillId].sort()
        otherProjectAdmin.getProjectUsers('TestProject2').data*.userId == [learner.userName]
        quizAdmin.getQuizRuns(sharedQuiz.quizId).totalCount == 0
        quizAdmin.getQuizRuns(unrelatedQuiz.quizId).data*.attemptId == [unrelatedAttempt]
    }
}
