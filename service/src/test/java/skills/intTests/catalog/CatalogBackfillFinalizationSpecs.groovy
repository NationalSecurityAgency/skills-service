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

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.UserAchievement
import skills.storage.model.SkillDef
import spock.util.concurrent.PollingConditions
import javax.sql.DataSource
import java.sql.Connection
import java.time.Instant

import static skills.intTests.utils.SkillsFactory.*

class CatalogBackfillFinalizationSpecs extends CatalogBackfillSpecBase {

    @Autowired DataSource dataSource

    def "finalizing imports credits users who earned local points or visited the project and skips other users"() {
        Map fixture = catalogFixture(false)
        List<SkillsService> learners = users.collect { String userId -> createService(userId) }
        // Full source completion: local reporter, visitor, completed participant, and unrelated user.
        users.take(4).each { String userId -> earnSource(fixture, userId) }
        // Partial source progress: local reporter and unrelated user. One completed source skill: visitor.
        [users[4], users[6]].each { String userId -> skillsService.addSkill(fixture.sourceSkills[0], userId, firstDate) }
        skillsService.addSkill(fixture.sourceSkills[0], users[5], firstDate)
        skillsService.addSkill(fixture.sourceSkills[0], users[5], completionDate)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        // Establish local-only eligibility before the project has any enabled imports.
        skillsService.addSkill(fixture.local, users[0])
        skillsService.addSkill(fixture.local, users[4])
        [1, 5, 7].each { int index -> learners[index].initializeProjectProgress('TestProject2') }

        // A prior finalized import with no earned credit establishes COMPLETED through the real visit flow.
        Map earlierImport = createSkill(1, 1, 90, 0, 1, 0, 100)
        skillsService.createSkill(earlierImport)
        skillsService.exportSkillToCatalog('TestProject1', earlierImport.skillId)
        skillsService.bulkImportSkillsFromCatalogAndFinalize('TestProject2', 'TestSubject1', [earlierImport])
        Map completedVisit = learners[2].initializeProjectProgress('TestProject2')

        importSkills(fixture)
        skillsService.createBadge(fixture.badge + [enabled: 'false'])
        List<Map> participationBefore = [1, 2, 5, 7].collect { int index ->
            ProjectParticipation participant = participationRepo.findByProjectIdAndUserId('TestProject2', users[index])
            [userId: users[index], id: participant.id, created: participant.created, status: participant.backfillStatus]
        }
        List<List<Map>> sourceCreditBefore = users.collect { String userId -> creditSnapshot('TestProject1', userId) }
        List<Map> expected = [
                [sourcePoints: 400, sourceSkills: 2, points: 500, level: 3, skills: 2, badges: 1],
                [sourcePoints: 400, sourceSkills: 2, points: 400, level: 2, skills: 2, badges: 1],
                [sourcePoints: 400, sourceSkills: 2, points: 400, level: 2, skills: 2, badges: 1],
                [sourcePoints: 400, sourceSkills: 2, points: 0, level: 0, skills: 0, badges: 0],
                [sourcePoints: 100, sourceSkills: 0, points: 200, level: 1, skills: 0, badges: 0],
                [sourcePoints: 200, sourceSkills: 1, points: 200, level: 1, skills: 1, badges: 0],
                [sourcePoints: 100, sourceSkills: 0, points: 0, level: 0, skills: 0, badges: 0],
                [sourcePoints: 0, sourceSkills: 0, points: 0, level: 0, skills: 0, badges: 0],
        ]

        expect:
        completedVisit.ready
        completedVisit.backfillStatus == 'COMPLETED'
        [0, 3, 4, 6].every { int index -> !participationRepo.existsByProjectIdAndUserId('TestProject2', users[index]) }
        [1, 5, 7].every { int index -> participationRepo.readStatus('TestProject2', users[index]) == BackfillStatus.NOT_NEEDED }
        participationRepo.readStatus('TestProject2', users[2]) == BackfillStatus.COMPLETED
        [3, 6].every { int index -> !creditSnapshot('TestProject2', users[index]) }
        !backfillTasks()
        learners.eachWithIndex { SkillsService learner, int index ->
            Map sourceSummary = learner.getSkillsSummaryForCurrentUser('TestProject1')
            assert sourceSummary.points == expected[index].sourcePoints
            assert sourceSummary.skillsAchieved == expected[index].sourceSkills
            Map destinationSummary = learner.getSkillsSummaryForCurrentUser('TestProject2')
            assert destinationSummary.points == (index in [0, 4] ? 100 : 0)
            assert destinationSummary.skillsAchieved == 0
            assert destinationSummary.badges.numBadgesCompleted == 0
        }

        when:
        Date finalizationStart = new Date()
        skillsService.finalizeSkillsImportFromCatalog('TestProject2')
        List<Map> summaries = learners.collect { SkillsService learner -> learner.getSkillsSummaryForCurrentUser('TestProject2') }
        then:
        summaries.eachWithIndex { Map summary, int index ->
            Map outcome = expected[index]
            assert summary.points == outcome.points
            assert summary.totalPoints == 1000
            assert summary.skillsLevel == outcome.level
            assert summary.skillsAchieved == outcome.skills
            assert summary.totalSkills == 4
            assert summary.subjects.subjectId == ['TestSubject1']
            Map subjectSummary = summary.subjects[0]
            assert subjectSummary.points == outcome.points
            assert subjectSummary.totalPoints == 1000
            assert subjectSummary.skillsLevel == outcome.level
            assert subjectSummary.skillsAchieved == outcome.skills
            assert subjectSummary.totalSkills == 4
            assert summary.badges.numTotalBadges == 0
            assert summary.badges.numBadgesCompleted == 0
            assert !summary.badges.recentlyAwardedBadges
        }
        [0, 3, 4, 6].every { int index -> !participationRepo.existsByProjectIdAndUserId('TestProject2', users[index]) }
        participationBefore.every { Map before ->
            ProjectParticipation participant = participationRepo.findByProjectIdAndUserId('TestProject2', before.userId)
            participant.id == before.id && participant.created == before.created && participant.backfillStatus == before.status
        }
        users.take(3).every { String userId ->
            fixture.sourceSkills.every { Map skill -> achievement('TestProject2', userId, skill.skillId).achievedOn == completionDate }
        }
        users.take(3).every { String userId ->
            UserAchievement group = achievement('TestProject2', userId, fixture.group.skillId)
            group.achievedOn >= finalizationStart && group.achievedOn <= new Date()
        }
        achievement('TestProject2', users[5], fixture.sourceSkills[0].skillId).achievedOn == completionDate
        [4, 5, 7].every { int index -> !achievement('TestProject2', users[index], fixture.group.skillId) }
        [3, 6].every { int index -> !creditSnapshot('TestProject2', users[index]) }
        users.collect { String userId -> creditSnapshot('TestProject1', userId) } == sourceCreditBefore
        users.every { String userId -> !creditSnapshot('TestProject3', userId) }
        !backfillTasks()

        when:
        // Badge requirements can only be assigned after imported skills are finalized and enabled.
        skillsService.assignSkillsToBadge('TestProject2', fixture.badge.badgeId, fixture.sourceSkills.skillId)
        Date badgePublicationStart = new Date()
        skillsService.createBadge(fixture.badge + [enabled: 'true'])
        List<Map> badgeSummaries = learners.collect { SkillsService learner -> learner.getSkillsSummaryForCurrentUser('TestProject2') }
        then:
        badgeSummaries.eachWithIndex { Map summary, int index ->
            assert summary.points == expected[index].points
            assert summary.badges.numTotalBadges == 1
            assert summary.badges.numBadgesCompleted == expected[index].badges
            assert summary.badges.recentlyAwardedBadges.badgeId == (expected[index].badges ? [fixture.badge.badgeId] : [])
            if (expected[index].badges) {
                long achievedOn = Instant.parse(summary.badges.recentlyAwardedBadges[0].achievedOn).toEpochMilli()
                assert achievedOn >= badgePublicationStart.time
                assert achievedOn <= new Date().time
            }
        }
        [3, 6].every { int index -> !creditSnapshot('TestProject2', users[index]) }
    }


    def "catalog finalization awards badges only to eligible participants - protected community #protectedCommunity"() {
        SkillsService owner = createService(users[7])
        SkillsService root = createRootSkillService()
        List<SkillsService> learners = users.take(5).collect { String userId -> createService(userId) }
        if (protectedCommunity) {
            (users.take(5) + users[7]).each { String userId ->
                root.saveUserTag(userId, 'dragons', ['DivineDragon'])
            }
        }
        Map source = createSkill(1, 1, 11, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [source])
        skillsService.exportSkillToCatalog('TestProject1', source.skillId)
        Map destination = createProject(2) + [enableProtectedUserCommunity: protectedCommunity]
        Map local = createSkill(2, 1, 21, 0, 5, 0, 100)
        owner.createProjectAndSubjectAndSkills(destination, createSubject(2, 1), [local])
        owner.bulkImportSkillsFromCatalogAndFinalize('TestProject2', 'TestSubject1', [source])
        Map badge = createBadge(2, 1)
        owner.createBadge(badge + [enabled: 'false'])
        owner.assignSkillsToBadge('TestProject2', badge.badgeId, [source.skillId])
        users.take(5).each { String userId ->
            skillsService.addSkill(source, userId, completionDate)
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        // Earn imported credit through real reports/visits while all users can access the project.
        [0, 3].each { int index ->
            owner.addSkill(local, users[index], firstDate)
        }
        [1, 2, 4].each { int index -> learners[index].initializeProjectProgress('TestProject2') }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        // Model pre-enrollment credit: retain real credit but remove enrollment for local-only users
        // and the unrelated user. Users 1 and 4 retain visit-only eligibility.
        [0, 2, 3].each { int index ->
            participationRepo.delete(participationRepo.findByProjectIdAndUserId('TestProject2', users[index]))
        }
        if (protectedCommunity) {
            // The root API only adds tags; remove the membership tag through the repository.
            [3, 4].each { int index ->
                userTagRepo.deleteAll(userTagRepo.findAllByUserIdAndKey(users[index], 'dragons'))
            }
        }
        // Publishing through the API awards eligible users immediately. Enable the fixture directly
        // so catalog finalization is the first operation that can award this badge.
        SkillDef badgeDefinition = skillDefRepo.findByProjectIdAndSkillId('TestProject2', badge.badgeId)
        badgeDefinition.enabled = 'true'
        skillDefRepo.save(badgeDefinition)
        // Finalize another import to recalculate the enabled badge against the retained achievements.
        Map additional = createSkill(1, 1, 12, 0, 1, 0, 100)
        skillsService.createSkill(additional)
        skillsService.exportSkillToCatalog('TestProject1', additional.skillId)
        owner.bulkImportSkillsFromCatalog('TestProject2', 'TestSubject1', [additional])

        expect:
        users.take(5).every { String userId -> !achievement('TestProject2', userId, badge.badgeId) }
        users.take(5).every { String userId -> achievement('TestProject2', userId, source.skillId) }
        [0, 2, 3].every { int index -> !participationRepo.existsByProjectIdAndUserId('TestProject2', users[index]) }

        when:
        owner.finalizeSkillsImportFromCatalog('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learners.eachWithIndex { SkillsService learner, int index ->
            if (!protectedCommunity || index < 3) {
                Map summary = learner.getSkillsSummaryForCurrentUser('TestProject2')
                boolean awarded = index != 2
                assert summary.badges.numTotalBadges == 1
                assert summary.badges.numBadgesCompleted == (awarded ? 1 : 0)
                assert summary.badges.recentlyAwardedBadges.badgeId == (awarded ? [badge.badgeId] : [])
            }
        }
        // Nonmembers cannot read the protected project; verify their retained credit and excluded awards directly.
        users.take(5).every { String userId -> achievement('TestProject2', userId, source.skillId).achievedOn == completionDate }
        !achievement('TestProject2', users[2], badge.badgeId)
        !protectedCommunity || [3, 4].every { int index -> !achievement('TestProject2', users[index], badge.badgeId) }

        where:
        protectedCommunity << [false, true]
    }


    def "users who visit or earn local points during finalization receive all catalog credit when it finishes"() {
        Map fixture = catalogFixture()
        users.take(3).each { String userId -> earnSource(fixture, userId) }
        Map newSource = createSkill(1, 1, 19, 0, 2, 0, 100)
        skillsService.createSkill(newSource)
        users.take(3).each { String userId ->
            skillsService.addSkill(newSource, userId, firstDate)
            skillsService.addSkill(newSource, userId, completionDate)
        }
        skillsService.exportSkillToCatalog('TestProject1', newSource.skillId)
        createService(users[0]).initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        skillsService.bulkImportSkillsFromCatalog('TestProject2', 'TestSubject1', [newSource])
        Connection blocker = dataSource.connection
        blocker.createStatement().execute('select pg_advisory_lock(4072002)')
        installTrigger('catalog_test_pause', 'user_points', "if NEW.project_id = 'TestProject2' and NEW.skill_id = '${newSource.skillId}' then perform pg_advisory_xact_lock(4072002); end if; return NEW;")

        when:
        skillsService.finalizeSkillsImportFromCatalog('TestProject2', false)
        new PollingConditions(timeout: 15).eventually {
            assert jdbcTemplate.queryForObject("select count(*) from pg_locks where locktype = 'advisory' and objid = 4072002 and not granted", Integer) > 0
        }
        Map partial = createService(users[1]).initializeProjectProgress('TestProject2')
        skillsService.addSkill(fixture.local, users[2])
        Map empty = createService(users[3]).initializeProjectProgress('TestProject2')
        skillsService.addSkill(fixture.local, users[4])

        then:
        !partial.ready
        !empty.ready
        [users[1], users[2], users[3], users[4]].every { String userId -> participationRepo.readStatus('TestProject2', userId) == BackfillStatus.PENDING }
        !backfillTasks()

        when:
        blocker.createStatement().execute('select pg_advisory_unlock(4072002)')
        blocker.close()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        then:
        points('TestProject2', users[1], null) == 600
        points('TestProject2', users[2], null) == 700
        points('TestProject2', users[3], null) == 0
        points('TestProject2', users[4], null) == 100
        [users[1], users[2], users[3], users[4]].every { String userId -> participationRepo.readStatus('TestProject2', userId) == BackfillStatus.COMPLETED }
        [users[1], users[2]].every { String userId -> achievement('TestProject2', userId, newSource.skillId).achievedOn == completionDate }
        achievement('TestProject2', users[1], fixture.badge.badgeId)

        cleanup:
        if (!blocker.closed) {
            blocker.createStatement().execute('select pg_advisory_unlock(4072002)')
            blocker.close()
        }
    }

}
