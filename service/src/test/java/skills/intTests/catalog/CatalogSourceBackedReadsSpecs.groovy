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

import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.beans.factory.annotation.Autowired
import skills.intTests.utils.SkillsService
import skills.intTests.utils.SkillsService.FileAndHeaders
import skills.services.UserEventService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.UserPoints
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserTag
import skills.storage.repos.ProjectParticipationRepo
import skills.storage.repos.UserTagRepo

import static skills.intTests.utils.SkillsFactory.*

/** Source-backed reads against simulated cleaned data, without the Stage 7 endpoint. */
class CatalogSourceBackedReadsSpecs extends CatalogIntSpec {
    @Autowired
    ProjectParticipationRepo participationRepo
    @Autowired
    UserTagRepo userTagRepo
    @Autowired
    UserEventService userEventService

    def "filtered #path pages and counts retain the same event population without multiplying points"() {
        Map fixture = readsFixture()
        List<String> eligible = fixture.users[0..4].sort(false)

        when:
        Map first = usersPage(path, fixture, 1)
        Map second = usersPage(path, fixture, 2)
        Map third = usersPage(path, fixture, 3)
        Map filtered = usersPage(path, fixture, 1, fixture.users[0])

        then:
        [first, second, third].every { Map page -> page.count == 5 && page.totalCount == 5 }
        first.data.size() == 2
        second.data.size() == 2
        third.data.size() == 1
        (first.data + second.data + third.data)*.userId == eligible
        (first.data + second.data + third.data).every { Map row ->
            row.totalPoints == (path == 'skill' ? 200 : path in ['project', 'subject'] && row.userId == fixture.users[4] ? 600 : 400)
        }
        path != 'group' || (first.data + second.data + third.data).every { it.skillsAchieved == 2 }
        filtered.count == 1
        filtered.totalCount == 1
        filtered.data*.userId == [fixture.users[0]]
        skillsService.getProjectUsers(fixture.other.projectId).count == 0
        skillsService.getProjectUsers(fixture.source.projectId).count == 6
        !participationRepo.existsByProjectIdAndUserId(fixture.destination.projectId, fixture.users[4])
        !participationRepo.existsByProjectIdAndUserId(fixture.destination.projectId, fixture.users[5])

        where:
        path << ['project', 'subject', 'skill', 'badge', 'group', 'tag']
    }

    def "streaming Excel export matches paginated project rows and their filters"() {
        Map fixture = readsFixture()

        when:
        FileAndHeaders export = skillsService.getUserProgressExcelExport(fixture.destination.projectId, 'userId', true)
        FileAndHeaders filtered = skillsService.getUserProgressExcelExport(fixture.destination.projectId, 'userId', true, fixture.users[0])

        then:
        exportRows(export.file) == fixture.users[0..4].sort(false).collect { String user ->
            [userAttrsRepo.findByUserIdIgnoreCase(user).userIdForDisplay, user == fixture.users[4] ? 600 : 400]
        }
        exportRows(filtered.file) == [[userAttrsRepo.findByUserIdIgnoreCase(fixture.users[0]).userIdForDisplay, 400]]
    }

    def "admin performed rows filtered counts and statistics represent destination participation in every status"() {
        Map fixture = readsFixture()

        when:
        Map unrelated = skillsService.getPerformedSkills(fixture.users[5], fixture.destination.projectId)
        Map unrelatedStats = skillsService.getUserStats(fixture.destination.projectId, fixture.users[5])

        then:
        unrelated.count == 0
        unrelated.totalCount == 0
        !unrelated.data
        unrelatedStats.numSkills == 0
        unrelatedStats.userTotalPoints == 0
        fixture.users[0..3].every { String user ->
            Map first = skillsService.getPerformedSkills(user, fixture.destination.projectId, '', 'performedOn', 2, 1)
            Map second = skillsService.getPerformedSkills(user, fixture.destination.projectId, '', 'performedOn', 2, 2)
            Map filtered = skillsService.getPerformedSkills(user, fixture.destination.projectId, fixture.imports[0].skillId, 'performedOn', 1, 2)
            Map stats = skillsService.getUserStats(fixture.destination.projectId, user)
            assert first.count == 4 && first.totalCount == 4 && first.data.size() == 2
            assert second.count == 4 && second.totalCount == 4 && second.data.size() == 2
            assert (first.data + second.data)*.id.toSet().size() == 4
            assert (first.data + second.data).every { it.importedSkill }
            assert filtered.count == 2 && filtered.totalCount == 4 && filtered.data.size() == 1
            assert stats.numSkills == 2 && stats.userTotalPoints == 0
            assert skillsService.getUserStats(fixture.other.projectId, user).numSkills == 0
            assert skillsService.getUserStats(fixture.source.projectId, user).userTotalPoints == 400
            true
        }
        skillsService.getPerformedSkills(fixture.users[4], fixture.destination.projectId).totalCount == 6
        skillsService.getUserStats(fixture.destination.projectId, fixture.users[4]).numSkills == 3
        skillsService.getUserStats(fixture.destination.projectId, fixture.users[4]).userTotalPoints == 600
        !participationRepo.existsByProjectIdAndUserId(fixture.destination.projectId, fixture.users[5])

        when: 'new source activity after simulated cleanup still cannot enroll the user'
        String lateUser = getRandomUsers(7)[6]
        skillsService.addSkill(fixture.imports[0], lateUser)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        skillsService.getPerformedSkills(lateUser, fixture.destination.projectId).totalCount == 0
        skillsService.getUserStats(fixture.destination.projectId, lateUser).numSkills == 0
        skillsService.getProjectUsers(fixture.destination.projectId).count == 5

        when: 'an authorized visit establishes eligibility while its real worker is queued'
        queueBackfill()
        SkillsService learner = createService(lateUser)
        Map pending = learner.initializeProjectProgress(fixture.destination.projectId)

        then:
        pending.backfillStatus == 'PENDING'
        !pending.ready
        skillsService.getPerformedSkills(lateUser, fixture.destination.projectId).totalCount == 1
        skillsService.getUserStats(fixture.destination.projectId, lateUser).numSkills == 1
        skillsService.getUserStats(fixture.destination.projectId, lateUser).userTotalPoints == 0
        skillsService.getProjectUsers(fixture.destination.projectId).data.find { it.userId == lateUser }.totalPoints == 100

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        learner.initializeProjectProgress(fixture.destination.projectId).backfillStatus == 'COMPLETED'
        skillsService.getUserStats(fixture.destination.projectId, lateUser).userTotalPoints == 100

        cleanup:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
    }

    def "point history and navigator keep source history only for eligible destination users"() {
        Map fixture = readsFixture()

        expect:
        fixture.users[0..4].every { String user ->
            assert skillsService.getPointHistory(user, fixture.destination.projectId, null, -1, 1).pointsHistory.last().points == (user == fixture.users[4] ? 600 : 400)
            assert skillsService.getPointHistory(user, fixture.destination.projectId, fixture.subject.subjectId, -1, 1).pointsHistory.last().points == (user == fixture.users[4] ? 600 : 400)
            true
        }
        !skillsService.getPointHistory(fixture.users[5], fixture.destination.projectId, null, -1, 1).pointsHistory
        !skillsService.getPointHistory(fixture.users[5], fixture.other.projectId, null, -1, 1).pointsHistory
        skillsService.getPointHistory(fixture.users[5], fixture.source.projectId, null, -1, 1).pointsHistory.last().points == 400
        skillsService.getMetricsData(fixture.destination.projectId, 'skillUsageNavigatorChartBuilder', [:]).skills.findAll {
            it.skillId in fixture.imports*.skillId
        }.every { it.numUserAchieved == 1 && it.numUsersInProgress == 4 }
    }

    def "tag metrics filter both user counts and distinct-tag pagination before grouping"() {
        Map fixture = readsFixture()
        fixture.users.eachWithIndex { String user, int index ->
            userTagRepo.save(new UserTag(userId: user, key: 'readTags', value: "tag${index}"))
            userTagRepo.save(new UserTag(userId: user, key: 'readTags', value: 'shared'))
        }
        Map props = [tagKey: 'readTags', currentPage: 1, pageSize: 2, sortBy: 'tag', sortDesc: false]

        when:
        List<Map> pages = (1..3).collect { int page ->
            skillsService.getMetricsData(fixture.destination.projectId, 'numUsersPerTagBuilder', props + [currentPage: page])
        }
        Map source = skillsService.getMetricsData(fixture.source.projectId, 'numUsersPerTagBuilder', props + [pageSize: 10])
        Map hidden = skillsService.getMetricsData(fixture.destination.projectId, 'numUsersPerTagBuilder', props + [tagFilter: 'tag5'])

        then:
        pages.every { it.totalNumItems == 6 && it.items.size() == 2 }
        pages.collectMany { it.items }.collectEntries { [(it.value): it.count] } == [shared: 5, tag0: 1, tag1: 1, tag2: 1, tag3: 1, tag4: 1]
        source.totalNumItems == 7
        source.items.find { it.value == 'shared' }.count == 6
        hidden.totalNumItems == 0
        !hidden.items

        when:
        createService(fixture.users[5]).initializeProjectProgress(fixture.destination.projectId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        Map qualified = skillsService.getMetricsData(fixture.destination.projectId, 'numUsersPerTagBuilder', props + [tagFilter: 'tag5'])
        skillsService.archiveUsers([fixture.users[5]], fixture.destination.projectId)
        Map archived = skillsService.getMetricsData(fixture.destination.projectId, 'numUsersPerTagBuilder', props + [tagFilter: 'tag5'])

        then:
        qualified.totalNumItems == 1
        qualified.items == [[value: 'tag5', count: 1]]
        archived.totalNumItems == 0
        !archived.items
        skillsService.getMetricsData(fixture.source.projectId, 'numUsersPerTagBuilder', props + [pageSize: 10]).totalNumItems == 7
    }

    def "daily and compacted weekly metrics retain event newness and monthly retains direct-project scope"() {
        Map fixture = readsFixture()
        Date start = new Date().clearTime() - (weekly ? 30 : 2)
        if (weekly) {
            userEventService.compactDailyEvents()
        }
        Map props = [start: start.time]

        when:
        Map destination = skillsService.getMetricsData(fixture.destination.projectId, 'distinctUsersOverTimeForProject', props)
        Map subject = skillsService.getMetricsData(fixture.destination.projectId, 'distinctUsersOverTimeForProject', props + [skillId: fixture.subject.subjectId])
        Map source = skillsService.getMetricsData(fixture.source.projectId, 'distinctUsersOverTimeForProject', props)
        Map other = skillsService.getMetricsData(fixture.other.projectId, 'distinctUsersOverTimeForProject', props)
        Map monthly = skillsService.getMetricsData(fixture.destination.projectId, 'distinctUsersOverTimeForProject', props + [byMonth: true])
        Map monthlySubject = skillsService.getMetricsData(fixture.destination.projectId, 'distinctUsersOverTimeForProject', props + [byMonth: true, skillId: fixture.subject.subjectId])
        Map events = skillsService.getMetricsData(fixture.destination.projectId, 'skillEventsOverTimeChartBuilder', props + [skillId: fixture.imports[0].skillId])

        then:
        destination.users*.count.max() == 5
        source.users*.count.max() == 6
        other.users.every { it.count == 0 }
        destination.newUsers*.count.sum() == 5
        subject.users*.count.max() == 5
        // Preserve the subject query's direct-child newness check, including its imported-ID behavior.
        subject.newUsers*.count.max() == 5
        monthly.users*.count.max() == 1
        monthlySubject.users*.count.max() == 1
        events.countsByDay*.num.sum() == 10
        events.allEvents*.num.sum() == 10

        where:
        weekly << [false, true]
    }

    def "activity newness is not participation creation or positive points for #weekly"() {
        Map fixture = readsFixture()
        // Real zero-point source reports before qualification/window: completed skills still record interactions.
        fixture.users[0..4].each { String user ->
            assert !skillsService.addSkill(fixture.imports[0], user, new Date() - 60).body.skillApplied
        }
        String zeroPointUser = getRandomUsers(7)[6]
        Map blocked = createSkill(2, 1, 4, 0, 1, 0, 100)
        skillsService.createSkill(blocked)
        skillsService.addLearningPathPrerequisite(fixture.destination.projectId, blocked.skillId, 'skill3')
        assert !skillsService.addSkill(blocked, zeroPointUser).body.skillApplied
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        if (weekly) {
            userEventService.compactDailyEvents()
        }
        Map props = [start: (new Date().clearTime() - (weekly ? 30 : 2)).time]

        when:
        Map destination = skillsService.getMetricsData(fixture.destination.projectId, 'distinctUsersOverTimeForProject', props)
        Map source = skillsService.getMetricsData(fixture.source.projectId, 'distinctUsersOverTimeForProject', props)
        Map monthly = skillsService.getMetricsData(fixture.destination.projectId, 'distinctUsersOverTimeForProject', props + [byMonth: true])

        then:
        destination.users*.count.max() == 6
        // Only the native learner and the blocked, zero-point native interaction are new here.
        destination.newUsers*.count.sum() == 2
        source.users*.count.max() == 6
        source.newUsers*.count.sum() == 1
        monthly.users*.count.max() == 2
        monthly.newUsers*.count.sum() == 2
        !userPointsRepo.findByProjectIdAndUserId(fixture.destination.projectId, zeroPointUser)
        !userPerformedSkillRepo.existsLocalEventByProjectIdAndUserId(fixture.destination.projectId, zeroPointUser)
        !participationRepo.existsByProjectIdAndUserId(fixture.destination.projectId, zeroPointUser)

        where:
        weekly << [false, true]
    }

    def "old disabled local evidence qualifies imports in other subjects and same-project reuse is preserved"() {
        Map fixture = readsFixture()
        Map secondSubject = createSubject(2, 2)
        skillsService.createSubject(secondSubject)
        Map local = createSkill(2, 2, 20, 0, 1, 0, 100)
        // Establish local-only evidence before adding imports to this separate fixture project.
        Map reuseProject = createProject(4)
        Map reuseSubject = createSubject(4, 1)
        Map reusedSubject = createSubject(4, 2)
        Map reuseSkill = createSkill(4, 1, 30, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(reuseProject, reuseSubject, [reuseSkill])
        skillsService.createSubject(reusedSubject)
        String user = fixture.users[5]
        skillsService.addSkill(reuseSkill, user, new Date() - 60)
        skillsService.reuseSkills(reuseProject.projectId, [reuseSkill.skillId], reusedSubject.subjectId)

        when:
        Map reuseUsers = skillsService.getSubjectUsers(reuseProject.projectId, reusedSubject.subjectId, 1)
        // The direct evidence is old and its definition is now disabled; membership is still project-wide.
        skillsService.createSkill(local)
        skillsService.addSkill(local, user, new Date() - 60)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        runInTransaction {
            participationRepo.findByProjectIdAndUserId(fixture.destination.projectId, user)?.with { ProjectParticipation row -> participationRepo.delete(row) }
        }
        // Historical disabled definitions cannot be produced through the current API (enablement is one-way).
        runInTransaction {
            SkillDef historicalLocal = skillDefRepo.findByProjectIdAndSkillId(local.projectId, local.skillId)
            historicalLocal.enabled = 'false'
            skillDefRepo.save(historicalLocal)
        }
        Map importedUsers = skillsService.getSkillGroupUsers(fixture.destination.projectId, fixture.group.skillId, 2, 3)

        then:
        reuseUsers.count == 1
        reuseUsers.data*.userId == [user]
        reuseUsers.data*.totalPoints == [100]
        skillsService.getPerformedSkills(user, reuseProject.projectId).totalCount == 1
        !participationRepo.existsByProjectIdAndUserId(reuseProject.projectId, user)
        importedUsers.count == 6
        importedUsers.data*.userId.contains(user)
        importedUsers.data.find { it.userId == user }.totalPoints == 400
        !participationRepo.existsByProjectIdAndUserId(fixture.destination.projectId, user)
    }

    def "repository-seeded achievement-only credit needs participation and does not create event-list progress"() {
        Map fixture = readsFixture()
        String user = getRandomUsers(7)[6]
        SkillDef source = skillDefRepo.findByProjectIdAndSkillId(fixture.source.projectId, fixture.imports[0].skillId)
        // Deliberately not an ordinary reporting flow: no source performed history or points are created.
        userAchievedRepo.save(new UserAchievement(userId: user, projectId: source.projectId,
                skillId: source.skillId, skillRefId: source.id, pointsWhenAchieved: source.totalPoints,
                achievedOn: new Date() - 1))

        expect:
        skillsService.getUserStats(fixture.destination.projectId, user).numSkills == 0
        skillsService.getPerformedSkills(user, fixture.destination.projectId).totalCount == 0
        skillsService.getProjectUsers(fixture.destination.projectId).count == 5
        !participationRepo.existsByProjectIdAndUserId(fixture.destination.projectId, user)

        when:
        createService(user).initializeProjectProgress(fixture.destination.projectId)
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        skillsService.getSkillSummary(user, fixture.destination.projectId).points == 0
        skillsService.getSkillSummary(user, fixture.destination.projectId).skillsAchieved == 1
        skillsService.getPerformedSkills(user, fixture.destination.projectId).totalCount == 0
        skillsService.getUserStats(fixture.destination.projectId, user).numSkills == 0
        skillsService.getProjectUsers(fixture.destination.projectId).count == 6
        skillsService.getProjectUsers(fixture.destination.projectId).data.find { it.userId == user }.totalPoints == 0
        skillsService.getProjectUsers(fixture.destination.projectId).data.find { it.userId == user }.lastUpdated == null
        !userPerformedSkillRepo.existsLocalEventByProjectIdAndUserId(fixture.source.projectId, user)
    }

    private Map readsFixture() {
        List<String> users = getRandomUsers(6)
        Map source = createProject(1)
        Map sourceSubject = createSubject(1, 1)
        List<Map> imports = [createSkill(1, 1, 1, 0, 2, 0, 100), createSkill(1, 1, 2, 0, 2, 0, 100)]
        skillsService.createProjectAndSubjectAndSkills(source, sourceSubject, imports)
        Map destination = createProject(2)
        Map subject = createSubject(2, 1)
        Map nativeSkill = createSkill(2, 1, 3, 0, 2, 0, 100)
        Map group = createSkillsGroup(2, 1, 10)
        skillsService.createProjectAndSubjectAndSkills(destination, subject, [nativeSkill, group])
        Map other = createProject(3)
        Map otherSubject = createSubject(3, 1)
        skillsService.createProjectAndSubjectAndSkills(other, otherSubject, [])

        users.each { String user ->
            imports.each { Map skill ->
                skillsService.addSkill(skill, user, new Date() - 1)
                skillsService.addSkill(skill, user, new Date())
            }
        }
        skillsService.addSkill(nativeSkill, users[4], new Date() - 1)
        skillsService.addSkill(nativeSkill, users[4], new Date())
        users[0..3].each { String user -> createService(user).initializeProjectProgress(destination.projectId) }
        skillsService.bulkExportSkillsToCatalog(source.projectId, imports*.skillId)
        skillsService.bulkImportSkillsIntoGroupFromCatalog(destination.projectId, subject.subjectId, group.skillId, imports)
        skillsService.finalizeSkillsImportFromCatalog(destination.projectId)
        imports.each { Map skill -> skillsService.importSkillFromCatalog(other.projectId, otherSubject.subjectId, source.projectId, skill.skillId) }
        skillsService.finalizeSkillsImportFromCatalog(other.projectId)
        Map badge = createBadge(2, 1)
        skillsService.addBadge(badge)
        imports.each { Map skill -> skillsService.assignSkillToBadge([projectId: destination.projectId, badgeId: badge.badgeId, skillId: skill.skillId]) }
        skillsService.addTagToSkills(destination.projectId, imports*.skillId, 'readTag')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        // Deliberate legacy residue, then scoped simulated cleanup. Source events remain untouched.
        userPointsRepo.save(new UserPoints(projectId: destination.projectId, userId: users[5], points: 400, created: new Date(), updated: new Date()))
        runInTransaction {
            userPointsRepo.deleteAllByProjectIdAndUserId(destination.projectId, users[5])
            userAchievedRepo.deleteAllByProjectIdAndUserId(destination.projectId, users[5])
            users[0..3].eachWithIndex { String user, int index ->
                userPointsRepo.deleteAllByProjectIdAndUserId(destination.projectId, user)
                userAchievedRepo.deleteAllByProjectIdAndUserId(destination.projectId, user)
                ProjectParticipation participation = participationRepo.findByProjectIdAndUserId(destination.projectId, user)
                participation.backfillStatus = BackfillStatus.values()[index]
                participationRepo.save(participation)
            }
        }
        assert !participationRepo.existsByProjectIdAndUserId(destination.projectId, users[4])
        assert !userPointsRepo.findByProjectIdAndUserId(destination.projectId, users[5])
        return [users: users, source: source, destination: destination, other: other, subject: subject, imports: imports, group: group, badge: badge]
    }

    private Map usersPage(String path, Map fixture, int page, String query = '') {
        String projectId = fixture.destination.projectId
        switch (path) {
            case 'project': return skillsService.getProjectUsers(projectId, 2, page, 'userId', true, query)
            case 'subject': return skillsService.getSubjectUsers(projectId, fixture.subject.subjectId, 2, page, 'userId', true, query)
            case 'skill': return skillsService.getSkillUsers(projectId, fixture.imports[0].skillId, 2, page, 'userId', true, query)
            case 'badge': return skillsService.getBadgeUsers(projectId, fixture.badge.badgeId, 2, page, 'userId', true, query)
            case 'group': return skillsService.getSkillGroupUsers(projectId, fixture.group.skillId, 2, page, 'userId', true, query)
            case 'tag': return skillsService.getSkillTagUsers(projectId, 'readtag', 2, page, 'userId', true, query)
        }
        throw new IllegalArgumentException(path)
    }

    private static List<List> exportRows(File file) {
        Workbook workbook = WorkbookFactory.create(file)
        try {
            Sheet sheet = workbook.getSheetAt(0)
            return (2..<sheet.physicalNumberOfRows - 1).collect { int row ->
                [sheet.getRow(row).getCell(0).stringCellValue, sheet.getRow(row).getCell(5).numericCellValue.intValue()]
            }
        } finally {
            workbook.close()
        }
    }
}
