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
package skills.intTests.catalog

import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsService
import skills.services.admin.SkillCatalogFinalizationService
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPerformedSkill
import skills.storage.model.UserPoints

import static skills.intTests.utils.SkillsFactory.*

class ApplyEventsThatWereReportedDuringTheFinalizationSpecs extends CatalogIntSpec {
    @Autowired SkillCatalogFinalizationService skillCatalogFinalizationService

    def "replaying already propagated events must not add their points again"() {
        Map fixture = fixture(10000)
        List<UserPerformedSkill> events = reportSourceEvents(fixture, 4)
        Map window = replayWindow(events[1..2])
        assert readProgress(fixture, true).projectPoints == 40
        assert readProgress(fixture) == [skillPoints: 40, subjectPoints: 40, projectPoints: 40,
                                       skillsAchieved: 0, subjectLevel: 0, projectLevel: 0]

        when: 'two events in the finalization window have already reached the destination'
        replay(fixture, window)

        then:
        readProgress(fixture) == [skillPoints: 40, subjectPoints: 40, projectPoints: 40,
                                 skillsAchieved: 0, subjectLevel: 0, projectLevel: 0]

        when: 'the same window is replayed again'
        replay(fixture, window)

        then:
        readProgress(fixture) == [skillPoints: 40, subjectPoints: 40, projectPoints: 40,
                                 skillsAchieved: 0, subjectLevel: 0, projectLevel: 0]
        readProgress(fixture, true).skillPoints == 40
    }

    def "replay recovers missing credit and a genuine completion without duplicating achievements"() {
        Map fixture = fixture(6, true)
        List<UserPerformedSkill> events = reportSourceEvents(fixture, 6)
        Map window = replayWindow(events[4..5])
        Date completionDate = events.last().performedOn
        assert readProgress(fixture, true).skillPoints == 60
        assert readProgress(fixture, true).skillsAchieved == 1

        // Model a migration snapshot taken after four reports, before the final two
        // reports and completion. Source credit and its genuine award remain intact.
        restoreDestinationSnapshot(fixture, 40)
        assert readProgress(fixture) == [skillPoints: 40, subjectPoints: 40, projectPoints: 40,
                                       skillsAchieved: 0, subjectLevel: 2, projectLevel: 2]

        when:
        replay(fixture, window)

        then:
        readProgress(fixture) == [skillPoints: 60, subjectPoints: 60, projectPoints: 60,
                                 skillsAchieved: 1, subjectLevel: 3, projectLevel: 3]
        assertDestinationAchievements(fixture, completionDate)

        when: 'the recovered events are delivered again'
        replay(fixture, window)

        then:
        readProgress(fixture) == [skillPoints: 60, subjectPoints: 60, projectPoints: 60,
                                 skillsAchieved: 1, subjectLevel: 3, projectLevel: 3]
        assertDestinationAchievements(fixture, completionDate)
        readProgress(fixture, true).skillPoints == 60
    }

    private Map fixture(int occurrences, boolean includeOtherSkills = false) {
        Map sourceProject = createProject(1)
        Map sourceSubject = createSubject(1)
        Map sourceSkill = createSkill(1, 1, 2, 1, occurrences)
        sourceSkill.pointIncrementInterval = 0
        List<Map> sourceSkills = includeOtherSkills ?
                [createSkill(1, 1, 1, 1, occurrences), sourceSkill, createSkill(1, 1, 3, 1, occurrences)] : [sourceSkill]
        skillsService.createProjectAndSubjectAndSkills(sourceProject, sourceSubject, sourceSkills)

        Map destinationProject = createProject(2)
        Map destinationSubject = createSubject(2)
        skillsService.createProjectAndSubjectAndSkills(destinationProject, destinationSubject, [])
        sourceSkills.each { Map skill -> skillsService.exportSkillToCatalog(sourceProject.projectId, skill.skillId) }
        List<Map> imports = includeOtherSkills ? sourceSkills.take(2) : [sourceSkill]
        skillsService.bulkImportSkillsFromCatalogAndFinalize(destinationProject.projectId, destinationSubject.subjectId, imports)

        String userId = getRandomUsers(1)[0]
        participateInProject(destinationProject.projectId, [userId])
        SkillDef imported = skillDefRepo.findByProjectIdAndSkillId(destinationProject.projectId, sourceSkill.skillId)
        [sourceSkill: sourceSkill, projectId: destinationProject.projectId, subjectId: destinationSubject.subjectId,
         importedRefId: imported.id, userId: userId, learner: createService(userId)]
    }

    private List<UserPerformedSkill> reportSourceEvents(Map fixture, int count) {
        Map sourceSkill = fixture.sourceSkill
        Date firstDate = new Date(System.currentTimeMillis() - 3 * 86400000L)
        (0..<count).each { int index ->
            assert skillsService.addSkill(sourceSkill, fixture.userId, new Date(firstDate.time + index * 1000L)).body.skillApplied
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Integer sourceRefId = skillDefRepo.findByProjectIdAndSkillId(sourceSkill.projectId, sourceSkill.skillId).id
        List<UserPerformedSkill> events = userPerformedSkillRepo.findAllBySkillRefIdAndUserId(sourceRefId, fixture.userId)
                .sort { UserPerformedSkill event -> event.performedOn }
        assert events.size() == count
        // Replay selects report creation time. Give the real HTTP-created events explicit,
        // separated creation times so window membership does not depend on execution speed.
        events.each { UserPerformedSkill event -> event.created = event.performedOn }
        userPerformedSkillRepo.saveAll(events)
        return events
    }

    private Map replayWindow(List<UserPerformedSkill> selectedEvents) {
        Date start = new Date(selectedEvents.first().created.time - 1)
        Date end = new Date(selectedEvents.last().created.time + 1)
        List<UserPerformedSkill> found = userPerformedSkillRepo.findAllBySkillRefIdWithinTimeRange(
                selectedEvents.first().skillRefId, start, end)
        assert found.collect { UserPerformedSkill event -> event.id }.sort() ==
                selectedEvents.collect { UserPerformedSkill event -> event.id }.sort()
        [start: start.time, end: end.time]
    }

    private void replay(Map fixture, Map window) {
        skillCatalogFinalizationService.applyEventsThatWereReportedDuringTheFinalizationRun(
                [fixture.importedRefId], window.start, window.end)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
    }

    private Map readProgress(Map fixture, boolean source = false) {
        SkillsService learner = fixture.learner
        Map sourceSkill = fixture.sourceSkill
        String projectId = source ? sourceSkill.projectId : fixture.projectId
        String subjectId = source ? sourceSkill.subjectId : fixture.subjectId
        Map summary = learner.getSkillsSummaryForCurrentUser(projectId)
        Map subject = summary.subjects.find { Map candidate -> candidate.subjectId == subjectId }
        Map skill = learner.getSingleSkillSummaryForCurrentUser(projectId, sourceSkill.skillId)
        [skillPoints: skill.points, subjectPoints: subject.points, projectPoints: summary.points,
         skillsAchieved: summary.skillsAchieved, subjectLevel: subject.skillsLevel, projectLevel: summary.skillsLevel]
    }

    private void restoreDestinationSnapshot(Map fixture, int points) {
        List<UserPoints> rows = [fixture.sourceSkill.skillId, fixture.subjectId, null].collect { String skillId ->
            UserPoints row = userPointsRepo.findByProjectIdAndUserIdAndSkillId(fixture.projectId, fixture.userId, skillId)
            assert row
            row.points = points
            return row
        }
        userPointsRepo.saveAll(rows)
        List<UserAchievement> skillAwards = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(
                fixture.userId, fixture.projectId, fixture.sourceSkill.skillId)
        List<UserAchievement> subjectLevels = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillIdAndLevelNotNull(
                fixture.userId, fixture.projectId, fixture.subjectId)
        List<UserAchievement> projectLevels = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillIdAndLevelNotNull(
                fixture.userId, fixture.projectId, null)
        userAchievedRepo.deleteAll(skillAwards + (subjectLevels + projectLevels).findAll { UserAchievement award -> award.level > 2 })
    }

    private void assertDestinationAchievements(Map fixture, Date completionDate) {
        // The endpoints above expose completion and current levels, but not duplicate rows
        // or all stored achievement dates. Check just those persistence guarantees here.
        List<UserAchievement> skillAwards = userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(
                fixture.userId, fixture.projectId, fixture.sourceSkill.skillId)
        assert skillAwards.size() == 1
        assert skillAwards.first().pointsWhenAchieved == 60
        assert skillAwards.first().achievedOn == completionDate
        assert userAchievedRepo.findAllByUserIdAndProjectIdAndSkillIdAndLevelNotNull(
                fixture.userId, fixture.projectId, fixture.subjectId).collect { UserAchievement award -> award.level }.sort() == [1, 2, 3]
        assert userAchievedRepo.findAllByUserIdAndProjectIdAndSkillIdAndLevelNotNull(
                fixture.userId, fixture.projectId, null).collect { UserAchievement award -> award.level }.sort() == [1, 2, 3]
    }
}
