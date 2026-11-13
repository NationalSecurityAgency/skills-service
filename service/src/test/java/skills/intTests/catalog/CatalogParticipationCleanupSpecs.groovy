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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Sheet
import org.springframework.test.annotation.DirtiesContext
import skills.intTests.utils.SkillsService
import skills.services.userActions.DashboardItem
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.SkillDef
import skills.storage.model.UserPoints
import skills.storage.model.UserAchievement
import skills.storage.model.UserAttrs

import static skills.intTests.utils.SkillsFactory.*

/** Temporary pre-5.1 cleanup endpoint acceptance; retire after remediation. */
// Close this context's scheduler before the dedicated IT context installs worker barriers.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogParticipationCleanupSpecs extends CatalogCleanupIntSpec {
    def "cleanup logs committed and skipped outcomes with actor and explicit scope without storing user actions"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        seedResidue(setup.destinations[0], user)
        SkillsService root = createRootSkillService()
        ListAppender<ILoggingEvent> logs = captureCleanupLogs()

        when:
        Map cleaned = root.cleanupImportedSkillProgress([projectIds: [' testproject2 ', 'TESTPROJECT2']])
        Map skipped = root.cleanupImportedSkillProgress([allProjects: true])

        then:
        cleaned.deletedRecords == 7
        skipped.deletedRecords == 0
        snapshot('TestProject2', user) == [points: [], awards: []]
        List<ILoggingEvent> outcomes = cleanupOutcomeLogs(logs)
        outcomes.size() == 2
        outcomes.every { ILoggingEvent event ->
            event.formattedMessage.contains("actor=[${root.userName.toLowerCase()}]") &&
                    event.formattedMessage.contains('projectId=[TestProject2]')
        }
        ILoggingEvent committed = outcomes.find { ILoggingEvent event -> event.formattedMessage.contains('status=[CLEANED]') }
        committed.level == Level.INFO
        committed.formattedMessage.endsWith('deletedRecords=[7] deletedPointRows=[3] deletedAchievementRows=[4]')
        ILoggingEvent unchanged = outcomes.find { ILoggingEvent event -> event.formattedMessage.contains('status=[SKIPPED]') }
        unchanged.level == Level.WARN
        unchanged.formattedMessage.endsWith('deletedRecords=[0] deletedPointRows=[0] deletedAchievementRows=[0]')
        logs.list.any { ILoggingEvent event -> event.formattedMessage.contains('scope=[{projectIds=[TestProject2]}]') }
        logs.list.any { ILoggingEvent event -> event.formattedMessage.contains('scope=[{allProjects=true}]') }
        root.getUserActionsForEverything(10, 1, 'created', false, '', DashboardItem.SkillsBatch,
                '', '', 'cleanupImportedSkillProgress').data.isEmpty()
    }

    def "explicit all-project cleanup has no projects to process when there are no enabled catalog imports"() {
        Map nativeSkill = createSkill(1, 1, 11, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [nativeSkill])
        String user = getRandomUsers(1)[0]
        skillsService.addSkill(nativeSkill, user)
        Map before = snapshot(nativeSkill.projectId, user)

        when:
        Map result = createRootSkillService().cleanupImportedSkillProgress([allProjects: true])

        then:
        result.projects == []
        result.deletedRecords == 0
        result.deletedPointRows == 0
        result.deletedAchievementRows == 0
        snapshot(nativeSkill.projectId, user) == before
    }

    def "group points and awards and inactive local badges are removed with exact row preservation"() {
        Map setup = fixture(1)
        Map destination = setup.destinations[0]
        Map source = createSkill(1, 1, 12, 0, 1, 0, 100)
        skillsService.createSkill(source)
        skillsService.exportSkillToCatalog(source.projectId, source.skillId)
        Map group = createSkillsGroup(2, 2, 30)
        skillsService.createSkill(group)
        skillsService.bulkImportSkillsIntoGroupFromCatalog('TestProject2', 'TestSubject2', group.skillId, [[projectId: source.projectId, skillId: source.skillId]])
        skillsService.finalizeSkillsImportFromCatalog('TestProject2')
        List<String> users = getRandomUsers(2)
        participationRepo.save(new ProjectParticipation(projectId: 'TestProject2', userId: users[0], backfillStatus: BackfillStatus.PENDING))
        SkillDef imported = skillDefRepo.findByProjectIdAndSkillId('TestProject2', source.skillId)
        SkillDef groupDef = skillDefRepo.findByProjectIdAndSkillId('TestProject2', group.skillId)
        SkillDef badge = skillDefRepo.findByProjectIdAndSkillId('TestProject2', destination.badge.badgeId)
        badge.enabled = 'false'
        badge.startDate = new Date() - 10
        badge.endDate = new Date() - 5
        skillDefRepo.save(badge)
        users.each { String user ->
            seedResidue(destination, user)
            [imported, groupDef].each { SkillDef definition ->
                Date date = new Date() - 3
                userPointsRepo.save(new UserPoints(projectId: 'TestProject2', userId: user, skillId: definition.skillId,
                        skillRefId: definition.id, points: 100, created: date, updated: date))
                userAchievedRepo.save(new UserAchievement(projectId: 'TestProject2', userId: user, skillId: definition.skillId,
                        skillRefId: definition.id, achievedOn: date, pointsWhenAchieved: 100))
            }
        }
        Map protectedRows = snapshot('TestProject2', users[0])

        when:
        Map result = createRootSkillService().cleanupImportedSkillProgress([allProjects: true])

        then:
        result.deletedRecords == 11
        result.projects*.projectId == ['TestProject2']
        result.deletedPointRows == 5
        result.deletedAchievementRows == 6
        snapshot('TestProject2', users[1]) == [points: [], awards: []]
        snapshot('TestProject2', users[0]) == protectedRows
    }

    def "no-import pending-only and reuse-only projects are skipped without touching residue"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        SkillsService root = createRootSkillService()
        List<Map> nativeSkills = (3..5).collect { int number -> createSkill(number, 1, 21, 0, 1, 0, 100) }
        nativeSkills.eachWithIndex { Map skill, int index ->
            skillsService.createProjectAndSubjectAndSkills(createProject(index + 3), createSubject(index + 3, 1), [skill])
            skillsService.addSkill(skill, user)
        }
        skillsService.importSkillFromCatalog('TestProject4', 'TestSubject1', setup.source.projectId, setup.source.skillId)
        skillsService.createSubject(createSubject(5, 2))
        skillsService.reuseSkillInAnotherSubject('TestProject5', nativeSkills[2].skillId, 'TestSubject2')
        Map before = (3..5).collectEntries { int number -> [("TestProject${number}".toString()): snapshot("TestProject${number}", user)] }
        ListAppender<ILoggingEvent> logs = captureCleanupLogs()

        when:
        Map automatic = root.cleanupImportedSkillProgress([allProjects: true])
        Map result = root.cleanupImportedSkillProgress([projectIds: ['TestProject3', 'TestProject4', 'TestProject5']])

        then:
        automatic.projects*.projectId == ['TestProject2']
        automatic.projects[0].status == 'SKIPPED'
        automatic.projects[0].reason == 'No removable destination credit'
        result.projects*.status == ['SKIPPED', 'SKIPPED', 'SKIPPED']
        result.projects.every { it.reason == 'No enabled cross-project catalog imports' }
        before.every { String project, Map rows -> snapshot(project, user) == rows }
        cleanupOutcomeLogs(logs).size() == 4
        (2..5).every { int number ->
            List<ILoggingEvent> projectLogs = cleanupOutcomeLogs(logs).findAll { ILoggingEvent event -> event.formattedMessage.contains("projectId=[TestProject${number}]") }
            projectLogs.size() == 1 && projectLogs[0].level == Level.WARN &&
                    projectLogs[0].formattedMessage.contains('status=[SKIPPED]') &&
                    projectLogs[0].formattedMessage.contains(number == 2 ? 'reason=[No removable destination credit]' : 'reason=[No enabled cross-project catalog imports]')
        }
    }

    def "old disabled local evidence protects all credit but native residue alone does not enroll a user"() {
        List<String> users = getRandomUsers(2)
        Map setup = fixture(2, [users[0]])
        setup.destinations.each { Map destination ->
            SkillDef local = skillDefRepo.findByProjectIdAndSkillId(destination.projectId, destination.nativeSkill.skillId)
            local.enabled = 'false'
            skillDefRepo.save(local)
            seedResidue(destination, users[0], true, false)
            seedResidue(destination, users[1])
            Date date = new Date() - 30
            // Native credit without local events is protected credit, not membership evidence.
            userPointsRepo.save(new UserPoints(projectId: destination.projectId, userId: users[1], skillId: local.skillId,
                    skillRefId: local.id, points: 40, created: date, updated: date))
        }
        Map protectedRows = setup.destinations.collectEntries { Map destination -> [(destination.projectId): snapshot(destination.projectId, users[0])] }
        Map nativeRows = setup.destinations.collectEntries { Map destination -> [(destination.projectId): snapshot(destination.projectId, users[1]).points.findAll { it.skillId == destination.nativeSkill.skillId }] }

        when:
        Map result = createRootSkillService().cleanupImportedSkillProgress([allProjects: true])

        then:
        result.deletedRecords == 14
        setup.destinations.every { Map destination ->
            snapshot(destination.projectId, users[0]) == protectedRows[destination.projectId] &&
                    snapshot(destination.projectId, users[1]) == [points: nativeRows[destination.projectId], awards: []] &&
                    !membership(destination.projectId, users[0]) && !membership(destination.projectId, users[1])
        }
    }

    def "multi-user multi-project cleanup preserves local and explicit members and deletes only destination residue"() {
        List<String> users = getRandomUsers(9)
        Map setup = fixture(2, users.take(2))
        SkillsService root = createRootSkillService()
        users.each { String user -> skillsService.addSkill(setup.source, user); seedGlobal(user) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        setup.destinations.each { Map destination ->
            assert !membership(destination.projectId, users[0])
            assert !membership(destination.projectId, users[1])
            // Local-event-only users have no explicit row; imports are deliberately in another subject.
            users.take(2).each { String user ->
                if (!userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, setup.source.skillId)) {
                    seedResidue(destination, user, true, false)
                }
            }
            BackfillStatus.values().eachWithIndex { BackfillStatus status, int index ->
                participationRepo.save(new ProjectParticipation(projectId: destination.projectId, userId: users[index + 2], backfillStatus: status))
                seedResidue(destination, users[index + 2])
            }
            seedResidue(destination, users[6])
            seedResidue(destination, users[7], false, false)
            seedResidue(destination, users[8], true, false)
            skillsService.archiveUsers([users[5], users[6]], destination.projectId)
        }
        Map protectedRows = [:]
        Map memberships = [:]
        Map archives = [:]
        Map sources = [:]
        Map sourceEvents = [:]
        Map globals = [:]
        setup.destinations.each { Map destination ->
            [users[5], users[6]].each { String user -> archives["${destination.projectId}/${user}".toString()] = archiveSnapshot(destination.projectId, user) }
            users.take(6).each { String user ->
                protectedRows["${destination.projectId}/${user}".toString()] = snapshot(destination.projectId, user)
                memberships["${destination.projectId}/${user}".toString()] = membership(destination.projectId, user)
            }
        }
        Integer sourceRefId = skillDefRepo.findByProjectIdAndSkillId(setup.source.projectId, setup.source.skillId).id
        users.each { String user ->
            sources[user] = snapshot(setup.source.projectId, user)
            sourceEvents[user] = events(sourceRefId, user)
            globals[user] = globalSnapshot(user)
        }
        Map available = setup.destinations.collectEntries { Map destination -> [(destination.projectId): projDefRepo.findByProjectIdIgnoreCase(destination.projectId).totalPoints] }

        when:
        Map result = root.cleanupImportedSkillProgress([allProjects: true])

        then:
        result.projects.findAll { it.status == 'CLEANED' }*.projectId == setup.destinations*.projectId
        result.deletedRecords == 30
        result.deletedPointRows == 10
        result.deletedAchievementRows == 20
        setup.destinations.every { Map destination ->
            users.drop(6).every { String user -> snapshot(destination.projectId, user) == [points: [], awards: []] } &&
                    users.take(6).every { String user ->
                        snapshot(destination.projectId, user) == protectedRows["${destination.projectId}/${user}".toString()] &&
                                membership(destination.projectId, user) == memberships["${destination.projectId}/${user}".toString()]
                    } && projDefRepo.findByProjectIdIgnoreCase(destination.projectId).totalPoints == available[destination.projectId]
        }
        users.every { String user -> snapshot(setup.source.projectId, user) == sources[user] &&
                events(sourceRefId, user) == sourceEvents[user] && globalSnapshot(user) == globals[user] }
        setup.destinations.every { Map destination ->
            skillsService.getSkillSummary(users[6], destination.projectId).points == 0 &&
                    skillsService.getSkillSummary(users[0], destination.projectId).points == 200
        }
        setup.destinations.every { Map destination ->
            [users[5], users[6]].every { String user -> archiveSnapshot(destination.projectId, user) == archives["${destination.projectId}/${user}".toString()] } &&
                    skillsService.getProjectUsers(destination.projectId).data*.userId.toSet() == users.take(5).toSet() &&
                    skillsService.getLeaderboard(users[0], destination.projectId).rankedUsers*.userId.toSet() == users.take(5).collect { String user -> userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay }.toSet() &&
                    exportedUsers(destination.projectId).toSet() == users.take(5).collect { String user -> userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay }.toSet()
        }
        root.cleanupImportedSkillProgress([allProjects: true]).deletedRecords == 0
        // Archive records survived: restoring an event-free protected learner still finds their row.
        skillsService.restoreArchivedUser(users[5], setup.destinations[0].projectId)
        skillsService.getProjectUsers(setup.destinations[0].projectId).data*.userId.contains(users[5])
    }

    def "cleanup skips observed #state and targeted retry succeeds after recovery"() {
        Map setup = fixture()
        String user = getRandomUsers(1)[0]
        setup.destinations.each { Map destination -> seedResidue(destination, user) }
        skillsService.changeSetting('TestProject2', 'catalog.finalize.state', [projectId: 'TestProject2', setting: 'catalog.finalize.state', value: state])
        Map before = snapshot('TestProject2', user)
        SkillsService root = createRootSkillService()
        ListAppender<ILoggingEvent> logs = captureCleanupLogs()

        when:
        Map result = root.cleanupImportedSkillProgress([projectIds: ['TestProject2', 'TestProject3']])

        then:
        result.projects*.status == ['SKIPPED', 'CLEANED']
        result.projects[0].reason.contains('RUNNING or FAILED')
        snapshot('TestProject2', user) == before
        cleanupOutcomeLogs(logs).size() == 2
        cleanupOutcomeLogs(logs).find { ILoggingEvent event -> event.level == Level.WARN }.formattedMessage.contains('projectId=[TestProject2] status=[SKIPPED] reason=[Catalog finalization is RUNNING or FAILED; retry after recovery]')
        cleanupOutcomeLogs(logs).find { ILoggingEvent event -> event.level == Level.INFO }.formattedMessage.contains('projectId=[TestProject3] status=[CLEANED]')

        when:
        skillsService.changeSetting('TestProject2', 'catalog.finalize.state', [projectId: 'TestProject2', setting: 'catalog.finalize.state', value: 'COMPLETED'])

        then:
        root.cleanupImportedSkillProgress([projectIds: ['TestProject2']]).deletedRecords == 7

        where:
        state << ['RUNNING', 'FAILED']
    }

    def "set-based cleanup removes achievement-only residue for more than one hundred users"() {
        Map setup = fixture(1)
        // These persistence-only legacy identities never authenticate, so they do not need PKI certificates.
        List<String> users = (1..105).collect { int number -> "legacy-cleanup-user-${number}".toString() }
        userAttrsRepo.saveAll(users.collect { String user ->
            new UserAttrs(userId: user, userIdForDisplay: user, userTagsLastUpdated: new Date())
        })
        users.each { String user -> seedResidue(setup.destinations[0], user, false, false) }
        SkillsService root = createRootSkillService()

        when:
        Map result = root.cleanupImportedSkillProgress([projectIds: ['TestProject2']])

        then:
        result.deletedRecords == 315
        result.deletedAchievementRows == 315
        users.every { String user -> snapshot('TestProject2', user) == [points: [], awards: []] }
        root.cleanupImportedSkillProgress([projectIds: ['TestProject2']]).deletedAchievementRows == 0
    }

    private List<String> exportedUsers(String projectId) {
        SkillsService.FileAndHeaders exported = skillsService.getUserProgressExcelExport(projectId)
        Workbook workbook = WorkbookFactory.create(exported.file)
        try {
            Sheet sheet = workbook.getSheetAt(0)
            (2..<sheet.physicalNumberOfRows - 1).collect { int index -> sheet.getRow(index).getCell(0).stringCellValue }
        } finally { workbook.close() }
    }
}
