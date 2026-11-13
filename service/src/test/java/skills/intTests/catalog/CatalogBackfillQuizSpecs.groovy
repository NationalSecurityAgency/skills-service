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
package skills.intTests.catalog

import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.UserAchievement

import static skills.intTests.utils.SkillsFactory.*

class CatalogBackfillQuizSpecs extends CatalogBackfillSpecBase {

    def "passing a shared quiz schedules separate backfill for each project that awards local points"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        skillsService.bulkImportSkillsFromCatalogAndFinalize('TestProject3', 'TestSubject1', fixture.sourceSkills)
        Map quiz = quizFixture()
        Map local2 = fixture.local + [selfReportingType: 'Quiz', quizId: quiz.quizId, numPerformToCompletion: 1]
        Map local3 = createSkill(3, 1, 21, 0, 1, 0, 100) + [selfReportingType: 'Quiz', quizId: quiz.quizId]
        skillsService.createSkill(local2)
        skillsService.createSkill(local3)
        queueBackfill()

        when:
        passQuiz(learner, quiz)
        then:
        backfillTasks().size() == 2
        ['TestProject2', 'TestProject3'].every { String projectId -> participationRepo.readStatus(projectId, users[0]) == BackfillStatus.PENDING }

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary2 = learner.getSkillsSummaryForCurrentUser('TestProject2')
        Map summary3 = learner.getSkillsSummaryForCurrentUser('TestProject3')
        then:
        [summary2, summary3].each { Map summary ->
            assert summary.points == 500
            assert summary.totalPoints == 500
            assert summary.skillsLevel == 5
            assert summary.skillsAchieved == 3
            assert summary.totalSkills == 3
            assert summary.subjects.subjectId == ['TestSubject1']
            assert summary.subjects[0].points == 500
            assert summary.subjects[0].totalPoints == 500
            assert summary.subjects[0].skillsLevel == 5
            assert summary.subjects[0].skillsAchieved == 3
            assert summary.subjects[0].totalSkills == 3
        }
        summary2.badges.numTotalBadges == 1
        summary2.badges.numBadgesCompleted == 1
        summary2.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        summary3.badges.numTotalBadges == 0
        summary3.badges.numBadgesCompleted == 0
        !summary3.badges.recentlyAwardedBadges
    }


    def "assigning a previously passed quiz awards local points and triggers catalog backfill"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        Map quiz = quizFixture()
        passQuiz(learner, quiz)
        Date quizCompleted = jdbcTemplate.queryForObject('select completed from user_quiz_attempt where user_id = ?', Date, users[0])
        queueBackfill()

        when:
        skillsService.createSkill(fixture.local + [selfReportingType: 'Quiz', quizId: quiz.quizId, numPerformToCompletion: 1])
        Map pendingSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        pendingSummary.points == 100
        pendingSummary.skillsLevel == 1
        pendingSummary.skillsAchieved == 1
        pendingSummary.badges.numBadgesCompleted == 0
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.PENDING
        backfillTasks().size() == 1
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], 'TestProject2', null).find { UserAchievement award -> award.level == 1 }.achievedOn == quizCompleted

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        summary.points == 500
        summary.totalPoints == 500
        summary.skillsLevel == 5
        summary.skillsAchieved == 3
        summary.totalSkills == 3
        summary.subjects.subjectId == ['TestSubject1']
        summary.subjects[0].points == 500
        summary.subjects[0].totalPoints == 500
        summary.subjects[0].skillsLevel == 5
        summary.subjects[0].skillsAchieved == 3
        summary.subjects[0].totalSkills == 3
        summary.badges.numTotalBadges == 1
        summary.badges.numBadgesCompleted == 1
        summary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[0], 'TestProject2', null).find { UserAchievement award -> award.level == 1 }.achievedOn == quizCompleted
    }


    def "assigning a quiz backfills multiple previous passers without crediting other quiz passers or restarting completed backfill"() {
        Map fixture = catalogFixture()
        List<SkillsService> learners = users.take(4).collect { String userId -> createService(userId) }
        users.take(4).each { String userId -> earnSource(fixture, userId) }
        Map quiz = quizFixture()
        Map otherQuiz = QuizDefFactory.createQuiz(2)
        skillsService.createQuizDef(otherQuiz)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(2, 1, 2))
        [0, 1, 3].each { int index -> passQuiz(learners[index], quiz) }
        passQuiz(learners[2], otherQuiz)
        // Keep a different quiz assignment in the same project to exercise the skill-reference filter.
        // Its disabled skill must not retroactively award local credit to the other quiz's passer.
        Map otherSkill = createSkill(2, 1, 23, 0, 1, 0, 100) +
                [selfReportingType: 'Quiz', quizId: otherQuiz.quizId, enabled: 'false']
        skillsService.createSkill(otherSkill)
        skillsService.addSkill(fixture.local, users[3], firstDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        ProjectParticipation existing = participationRepo.findByProjectIdAndUserId('TestProject2', users[3])
        Map enrollmentBefore = [id: existing.id, created: existing.created, updated: existing.updated,
                                status: existing.backfillStatus]
        Map quizSkill = createSkill(2, 1, 22, 0, 1, 0, 100) +
                [selfReportingType: 'Quiz', quizId: quiz.quizId]
        queueBackfill()

        expect:
        enrollmentBefore.status == BackfillStatus.COMPLETED
        learners[3].getSkillsSummaryForCurrentUser('TestProject2').points == 500
        users.take(3).every { String userId -> !participationRepo.existsByProjectIdAndUserId('TestProject2', userId) }
        !backfillTasks()

        when:
        skillsService.createSkill(quizSkill)

        then:
        backfillTasks().size() == 2
        [0, 1].each { int index ->
            Map summary = learners[index].getSkillsSummaryForCurrentUser('TestProject2')
            assert summary.points == 100
            assert summary.skillsAchieved == 1
            assert summary.badges.numBadgesCompleted == 0
            assert participationRepo.readStatus('TestProject2', users[index]) == BackfillStatus.PENDING
        }
        learners[2].getSkillsSummaryForCurrentUser('TestProject2').points == 0
        learners[2].getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 0
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[2])
        learners[3].getSkillsSummaryForCurrentUser('TestProject2').points == 600
        learners[3].getSkillsSummaryForCurrentUser('TestProject2').skillsAchieved == 3
        participationRepo.findById(enrollmentBefore.id).get().with { ProjectParticipation participant ->
            participant.created == enrollmentBefore.created && participant.updated == enrollmentBefore.updated &&
                    participant.backfillStatus == enrollmentBefore.status
        }

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        [0, 1, 3].each { int index ->
            Map summary = learners[index].getSkillsSummaryForCurrentUser('TestProject2')
            assert summary.points == (index == 3 ? 600 : 500)
            assert summary.totalPoints == 1000
            assert summary.skillsAchieved == 3
            assert summary.totalSkills == 4
            assert summary.subjects[0].points == summary.points
            assert summary.subjects[0].skillsAchieved == summary.skillsAchieved
            assert summary.badges.numBadgesCompleted == 1
            assert summary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
            assert participationRepo.readStatus('TestProject2', users[index]) == BackfillStatus.COMPLETED
        }
        Map excludedSummary = learners[2].getSkillsSummaryForCurrentUser('TestProject2')
        excludedSummary.points == 0
        excludedSummary.skillsAchieved == 0
        excludedSummary.badges.numBadgesCompleted == 0
        !participationRepo.existsByProjectIdAndUserId('TestProject2', users[2])
        !backfillTasks()
    }


    def "copying a project preserves earned quiz points without enrolling users for catalog backfill"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        Map quiz = quizFixture()
        skillsService.createSkill(fixture.local + [selfReportingType: 'Quiz', quizId: quiz.quizId, numPerformToCompletion: 1])
        passQuiz(learner, quiz)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        skillsService.copyProject('TestProject2', createProject(4))
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject4')
        then:
        summary.points == 100
        summary.totalPoints == 100
        summary.skillsLevel == 5
        summary.skillsAchieved == 1
        summary.totalSkills == 1
        summary.subjects.subjectId == ['TestSubject1']
        summary.subjects[0].points == 100
        summary.subjects[0].totalPoints == 100
        summary.subjects[0].skillsLevel == 5
        summary.subjects[0].skillsAchieved == 1
        summary.subjects[0].totalSkills == 1
        summary.badges.numTotalBadges == 0
        summary.badges.numBadgesCompleted == 0
        points('TestProject4', users[0], fixture.local.skillId) == 100
        !participationRepo.existsByProjectIdAndUserId('TestProject4', users[0])
        !backfillTasks()
    }


    def "completing a quiz prerequisite awards both local skills and schedules only one catalog backfill"() {
        Map fixture = catalogFixture()
        SkillsService learner = createService(users[0])
        earnSource(fixture, users[0])
        Map quiz = quizFixture()
        fixture.local.numPerformToCompletion = 1
        skillsService.createSkill(fixture.local)
        Map quizSkill = createSkill(2, 1, 22, 0, 1, 0, 100) + [selfReportingType: 'Quiz', quizId: quiz.quizId]
        skillsService.createSkill(quizSkill)
        // Passing the quiz alone is insufficient until the native prerequisite is completed.
        skillsService.addLearningPathPrerequisite('TestProject2', quizSkill.skillId, fixture.local.skillId)
        passQuiz(learner, quiz)
        queueBackfill()

        when:
        skillsService.addSkill(fixture.local, users[0])
        Map pendingSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        pendingSummary.points == 200
        pendingSummary.skillsLevel == 2
        pendingSummary.skillsAchieved == 2
        pendingSummary.badges.numBadgesCompleted == 0
        points('TestProject2', users[0], quizSkill.skillId) == 100
        backfillTasks().size() == 1
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.PENDING

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
        then:
        summary.points == 600
        summary.totalPoints == 600
        summary.skillsLevel == 5
        summary.skillsAchieved == 4
        summary.totalSkills == 4
        summary.subjects.subjectId == ['TestSubject1']
        summary.subjects[0].points == 600
        summary.subjects[0].totalPoints == 600
        summary.subjects[0].skillsLevel == 5
        summary.subjects[0].skillsAchieved == 4
        summary.subjects[0].totalSkills == 4
        summary.badges.numTotalBadges == 1
        summary.badges.numBadgesCompleted == 1
        summary.badges.recentlyAwardedBadges.badgeId == [fixture.badge.badgeId]
        points('TestProject2', users[0], quizSkill.skillId) == 100
        participationRepo.readStatus('TestProject2', users[0]) == BackfillStatus.COMPLETED
        !backfillTasks()
    }


    private Map quizFixture() {
        Map quiz = QuizDefFactory.createQuiz(1)
        skillsService.createQuizDef(quiz)
        skillsService.createQuizQuestionDefs(QuizDefFactory.createChoiceQuestions(1, 1, 2))
        return quiz
    }


    private void passQuiz(SkillsService learner, Map quiz) {
        Map attempt = learner.startQuizAttempt(quiz.quizId).body
        learner.reportQuizAnswer(quiz.quizId, attempt.id, attempt.questions[0].answerOptions[0].id)
        assert learner.completeQuizAttempt(quiz.quizId, attempt.id).body.passed
    }

}
