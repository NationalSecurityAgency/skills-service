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

import skills.intTests.utils.SkillsService
import skills.storage.model.SkillDef.SelfReportingType

import static skills.intTests.utils.SkillsFactory.*

class ProjectUserRemovalQuizSpecs extends QuizSkillAchievementsBaseIntSpec {

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
}
