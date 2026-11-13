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

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.UserTag
import skills.storage.repos.ProjectParticipationRepo
import skills.storage.repos.UserTagRepo

import java.time.Instant
import java.sql.Timestamp

import static skills.intTests.utils.SkillsFactory.*

class ProjectParticipantUsersSpecs extends CatalogIntSpec {
    @Autowired
    ProjectParticipationRepo participationRepo
    @Autowired
    UserTagRepo userTagRepo

    def "event-free participants in #status have project rows export and read-only details"() {
        Map fixture = nativeFixture()
        String user = getRandomUsers(1)[0]
        SkillsService learner = createService(user)
        learner.initializeProjectProgress(fixture.project.projectId)
        ProjectParticipation participation = participationRepo.findByProjectIdAndUserId(fixture.project.projectId, user)
        participation.backfillStatus = status
        participation = participationRepo.save(participation)
        Date created = participation.created
        Date updated = participation.updated

        when:
        Map page = skillsService.getProjectUsers(fixture.project.projectId)
        Map details = skillsService.getUserInfoForProject(fixture.project.projectId, user)
        Map summary = skillsService.getSkillSummary(user, fixture.project.projectId)
        List<Map> exported = exportRows(fixture.project.projectId)

        then:
        page.count == 1
        skillsService.getProjectUsersCount(fixture.project.projectId) == 0
        page.data*.userId == [user]
        page.data[0].totalPoints == 0
        page.data[0].userMaxLevel == 0
        Instant.parse(page.data[0].firstUpdated) == created.toInstant()
        page.data[0].lastUpdated == null
        details.userId == user
        summary.points == 0
        exported*.points == [0]
        exported*.last == ['N/A']
        Math.abs(exported[0].first.time - created.time) <= 1
        exported[0].percent == 0
        !userPointsRepo.findByProjectIdAndUserId(fixture.project.projectId, user)
        !userPerformedSkillRepo.existsLocalEventByProjectIdAndUserId(fixture.project.projectId, user)
        participationRepo.findByProjectIdAndUserId(fixture.project.projectId, user).updated == updated
        skillsService.getSubjectUsers(fixture.project.projectId, fixture.subject.subjectId).count == 0
        skillsService.getSkillUsers(fixture.project.projectId, fixture.skill.skillId).count == 0
        skillsService.getPerformedSkills(user, fixture.project.projectId).totalCount == 0
        skillsService.getUserStats(fixture.project.projectId, user).userTotalPoints == 0

        where:
        status << BackfillStatus.values()
    }

    def "project pagination deduplicates activity and participation and puts missing earned dates last"() {
        Map fixture = nativeFixture()
        List<String> users = getRandomUsers(5).sort(false)
        users[0..3].each { String user ->
            createService(user).initializeProjectProgress(fixture.project.projectId)
            setParticipationCreated(fixture.project.projectId, user, new Date().clearTime() - 1)
        }
        Date earned = new Date().clearTime() - 2
        skillsService.addSkill(fixture.skill, users[0], earned)
        skillsService.addSkill(fixture.skill, users[4], earned)

        when:
        List<Map> pages = (1..3).collect { int page -> skillsService.getProjectUsers(fixture.project.projectId, 2, page, sortBy, ascending) }
        List<Map> rows = pages.collectMany { it.data }

        then:
        pages.every { it.count == 5 && it.totalCount == 5 }
        pages.collect { it.data.size() } == [2, 2, 1]
        rows*.userId == (sortBy in ['lastUpdated', 'firstUpdated'] ? [users[0], users[4]] + users[1..3] : users[1..3] + [users[0], users[4]])
        rows.findAll { it.userId in [users[0], users[4]] }.every { it.totalPoints == 100 }
        rows.findAll { it.userId in users[1..3] }.every { it.totalPoints == 0 && !it.lastUpdated }
        exportRows(fixture.project.projectId, sortBy, ascending)*.user == rows*.userIdForDisplay
        !participationRepo.existsByProjectIdAndUserId(fixture.project.projectId, users[4])

        where:
        sortBy        | ascending
        'lastUpdated' | true
        'lastUpdated' | false
        'firstUpdated' | true
        'totalPoints' | true
    }

    def "project name tag and positive progress filters match Excel including small and zero totals"() {
        Map fixture = nativeFixture(withSkill, 10)
        List<String> users = getRandomUsers(2)
        users.each { String user -> createService(user).initializeProjectProgress(fixture.project.projectId) }
        userTagRepo.save(new UserTag(userId: users[0], key: 'dutyOrganization', value: 'participantOrg'))

        expect:
        skillsService.getProjectUsers(fixture.project.projectId, 1, 1, 'userId', true, users[0]).data*.userId == [users[0]]
        skillsService.getProjectUsers(fixture.project.projectId, 1, 1, 'userId', true, '', 0, 100, 'participantOrg').data*.userId == [users[0]]
        exportRows(fixture.project.projectId, 'userId', true, '', 0, 100, 'participantOrg')*.user == [userAttrsRepo.findByUserIdIgnoreCase(users[0]).userIdForDisplay]
        skillsService.getProjectUsers(fixture.project.projectId, 1, 1, 'userId', true, '', 1).count == 0
        !exportRows(fixture.project.projectId, 'userId', true, '', 1)
        exportRows(fixture.project.projectId)*.percent == [0, 0]
        skillsService.getProjectUsers(fixture.project.projectId, 1, 1, 'userId', true, '', 0, 50).count == 2
        exportRows(fixture.project.projectId, 'userId', true, '', 0, 50).size() == 2
        skillsService.getProjectUsers(fixture.project.projectId, 1, 1, 'userId', true, '', 0, 0).count == 0
        !exportRows(fixture.project.projectId, 'userId', true, '', 0, 0)
        skillsService.getProjectUsers(fixture.project.projectId).totalPoints == (withSkill ? 20 : 0)

        where:
        withSkill << [true, false]
    }

    def "archive repeat initialization and restore preserve event-free membership and First Seen"() {
        Map fixture = nativeFixture()
        String user = getRandomUsers(1)[0]
        SkillsService learner = createService(user)
        learner.initializeProjectProgress(fixture.project.projectId)
        String firstSeen = skillsService.getProjectUsers(fixture.project.projectId).data[0].firstUpdated

        when:
        skillsService.archiveUsers([user], fixture.project.projectId)
        learner.initializeProjectProgress(fixture.project.projectId)

        then:
        skillsService.getProjectUsers(fixture.project.projectId, 1).count == 0
        !exportRows(fixture.project.projectId)
        participationRepo.existsByProjectIdAndUserId(fixture.project.projectId, user)

        when:
        skillsService.restoreArchivedUser(user, fixture.project.projectId)

        then:
        skillsService.getProjectUsers(fixture.project.projectId).data[0].firstUpdated == firstSeen
        exportRows(fixture.project.projectId)*.last == ['N/A']
        !userPointsRepo.findByProjectIdAndUserId(fixture.project.projectId, user)
    }

    def "protected community enrollment and project administration checks still guard participant reads"() {
        List<String> users = getRandomUsers(3)
        SkillsService owner = createService(users[0])
        SkillsService learner = createService(users[1])
        SkillsService outsider = createService(users[2])
        SkillsService root = createRootSkillService()
        root.saveUserTag(owner.userName, 'dragons', ['DivineDragon'])
        root.saveUserTag(learner.userName, 'dragons', ['DivineDragon'])
        Map project = createProject(1)
        project.enableProtectedUserCommunity = true
        owner.createProject(project)
        learner.initializeProjectProgress(project.projectId)

        expect:
        owner.getProjectUsers(project.projectId).data*.userId == [learner.userName]
        owner.getUserInfoForProject(project.projectId, learner.userName).userId == learner.userName

        when:
        outsider.initializeProjectProgress(project.projectId)

        then:
        SkillsClientException enrollmentError = thrown()
        enrollmentError.httpStatus == HttpStatus.FORBIDDEN
        !participationRepo.existsByProjectIdAndUserId(project.projectId, outsider.userName)

        when:
        learner.getUserInfoForProject(project.projectId, learner.userName)

        then:
        SkillsClientException detailsError = thrown()
        detailsError.httpStatus == HttpStatus.FORBIDDEN
        owner.getProjectUsers(project.projectId).count == 1
    }

    def "First Seen uses retained earning history or earlier participation and later awards reuse the row"() {
        Map fixture = nativeFixture()
        List<String> users = getRandomUsers(2)
        SkillsService visitor = createService(users[0])
        visitor.initializeProjectProgress(fixture.project.projectId)
        Date earlierVisit = new Date().clearTime() - 4
        setParticipationCreated(fixture.project.projectId, users[0], earlierVisit)
        Date earned = new Date().clearTime() - 2
        skillsService.addSkill(fixture.skill, users[0], earned)
        skillsService.addSkill(fixture.skill, users[1], earned)
        createService(users[1]).initializeProjectProgress(fixture.project.projectId)

        when:
        visitor.initializeProjectProgress(fixture.project.projectId)
        Map page = skillsService.getProjectUsers(fixture.project.projectId)

        then:
        page.count == 2
        Instant.parse(page.data.find { it.userId == users[0] }.firstUpdated) == earlierVisit.toInstant()
        Instant.parse(page.data.find { it.userId == users[1] }.firstUpdated) == earned.toInstant()
        page.data.every { Instant.parse(it.lastUpdated) == earned.toInstant() && it.totalPoints == 100 }
        exportRows(fixture.project.projectId).every { it.last == earned && it.percent == 0.5d }

        when: 'retained history, rather than permanently stored first-ever dates, controls earning dates'
        Date laterEarned = new Date().clearTime() - 1
        skillsService.addSkill(fixture.skill, users[1], laterEarned)
        skillsService.deleteSkillEvent([projectId: fixture.project.projectId, skillId: fixture.skill.skillId, userId: users[1], timestamp: earned.time])
        Map retained = skillsService.getProjectUsers(fixture.project.projectId).data.find { it.userId == users[1] }

        then:
        retained.totalPoints == 100
        Instant.parse(retained.firstUpdated) == laterEarned.toInstant()
        Instant.parse(retained.lastUpdated) == laterEarned.toInstant()

        when:
        skillsService.deleteSkillEvent([projectId: fixture.project.projectId, skillId: fixture.skill.skillId, userId: users[1], timestamp: laterEarned.time])
        Map eventFree = skillsService.getProjectUsers(fixture.project.projectId).data.find { it.userId == users[1] }

        then:
        skillsService.getProjectUsers(fixture.project.projectId).count == 2
        eventFree.totalPoints == 0
        eventFree.lastUpdated == null
        Instant.parse(eventFree.firstUpdated) == participationRepo.findByProjectIdAndUserId(fixture.project.projectId, users[1]).created.toInstant()
        exportRows(fixture.project.projectId).find { it.user == eventFree.userIdForDisplay }.last == 'N/A'
    }

    def "pending participant transitions preserve source history dates and destination isolation"() {
        Map fixture = nativeFixture()
        Map destination = createProject(2)
        Map subject = createSubject(2, 1)
        Map other = createProject(3)
        Map otherSubject = createSubject(3, 1)
        skillsService.createProjectAndSubjectAndSkills(destination, subject, [])
        skillsService.createProjectAndSubjectAndSkills(other, otherSubject, [])
        skillsService.exportSkillToCatalog(fixture.project.projectId, fixture.skill.skillId)
        [destination.projectId, other.projectId].eachWithIndex { String projectId, int index ->
            skillsService.importSkillFromCatalog(projectId, index == 0 ? subject.subjectId : otherSubject.subjectId, fixture.project.projectId, fixture.skill.skillId)
            skillsService.finalizeSkillsImportFromCatalog(projectId)
        }
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress(destination.projectId)
        String visitDate = skillsService.getProjectUsers(destination.projectId).data[0].firstUpdated
        Date earned = new Date().clearTime() - 3
        users.each { String user -> skillsService.addSkill(fixture.skill, user, earned) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        when:
        queueBackfill()
        Map pending = createService(users[1]).initializeProjectProgress(destination.projectId)

        then:
        pending.backfillStatus == 'PENDING'
        skillsService.getSkillSummary(users[1], destination.projectId).points == 0
        skillsService.getProjectUsers(destination.projectId).data.every { it.totalPoints == 100 && Instant.parse(it.firstUpdated) == earned.toInstant() }
        visitDate != skillsService.getProjectUsers(destination.projectId).data.find { it.userId == users[0] }.firstUpdated
        skillsService.getProjectUsers(other.projectId).count == 0
        !exportRows(other.projectId)

        when:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        skillsService.getProjectUsers(destination.projectId).count == 2
        skillsService.getSkillSummary(users[1], destination.projectId).points == 100
        exportRows(destination.projectId).every { it.first == earned && it.last == earned }

        cleanup:
        releaseBackfill()
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
    }

    private Map nativeFixture(boolean withSkill = true, int increment = 100) {
        Map project = createProject(1)
        Map subject = createSubject(1, 1)
        Map skill = createSkill(1, 1, 1, 0, 2, 0, increment)
        skillsService.createProjectAndSubjectAndSkills(project, subject, withSkill ? [skill] : [])
        return [project: project, subject: subject, skill: skill]
    }

    private void setParticipationCreated(String projectId, String userId, Date created) {
        // The audit creation column is intentionally immutable through JPA. Seed historical/tied dates only.
        jdbcTemplate.update('update project_participation set created = ? where project_id = ? and user_id = ?',
                new Timestamp(created.time), projectId, userId)
    }

    private List<Map> exportRows(String projectId, String sortBy = 'userId', boolean ascending = true, String query = '', int minimum = 0, int maximum = 100, String tag = '') {
        SkillsService.FileAndHeaders exported = skillsService.getUserProgressExcelExport(projectId, sortBy, ascending, query, minimum, maximum, tag)
        Workbook workbook = WorkbookFactory.create(exported.file)
        try {
            Sheet sheet = workbook.getSheetAt(0)
            assert sheet.getRow(1).getCell(7).stringCellValue == 'First Seen (UTC)'
            assert sheet.getRow(1).getCell(8).stringCellValue == 'Points Last Earned (UTC)'
            return (2..<sheet.physicalNumberOfRows - 1).collect { int rowIndex ->
                Row row = sheet.getRow(rowIndex)
                assert row.getCell(7).cellType == CellType.NUMERIC
                [user: row.getCell(0).stringCellValue, points: row.getCell(5).numericCellValue.intValue(),
                 percent: row.getCell(6).numericCellValue, first: row.getCell(7).dateCellValue,
                 last: row.getCell(8).cellType == CellType.STRING ? row.getCell(8).stringCellValue : row.getCell(8).dateCellValue]
            }
        } finally {
            workbook.close()
        }
    }
}
