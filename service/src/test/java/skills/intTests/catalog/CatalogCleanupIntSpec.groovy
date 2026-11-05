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

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import skills.controller.RootController
import skills.intTests.utils.SkillsService
import skills.services.admin.CatalogParticipationCleanupService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.storage.model.ProjectParticipation
import skills.storage.model.SkillDef
import skills.storage.model.UserAchievement
import skills.storage.model.UserPoints
import skills.storage.model.UserPerformedSkill
import skills.storage.model.ArchivedUser
import skills.storage.repos.ProjectParticipationRepo
import skills.storage.repos.ArchivedUsersRepo

import static skills.intTests.utils.SkillsFactory.*

/** Shared disposable fixtures for temporary pre-5.1 remediation acceptance; retire with endpoints. */
class CatalogCleanupIntSpec extends CatalogIntSpec {
    @Autowired ProjectParticipationRepo participationRepo
    @Autowired ArchivedUsersRepo archiveRepo
    private final List<ListAppender<ILoggingEvent>> cleanupLogAppenders = []
    private final List<ListAppender<ILoggingEvent>> fixLogAppenders = []

    def cleanup() {
        Logger logger = (Logger) LoggerFactory.getLogger(CatalogParticipationCleanupService)
        cleanupLogAppenders.each { ListAppender<ILoggingEvent> appender ->
            logger.detachAppender(appender)
            appender.stop()
        }
        Logger fixLogger = (Logger) LoggerFactory.getLogger(RootController)
        fixLogAppenders.each { ListAppender<ILoggingEvent> appender ->
            fixLogger.detachAppender(appender)
            appender.stop()
        }
    }

    protected ListAppender<ILoggingEvent> captureCleanupLogs() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>()
        appender.start()
        ((Logger) LoggerFactory.getLogger(CatalogParticipationCleanupService)).addAppender(appender)
        cleanupLogAppenders.add(appender)
        appender
    }

    protected List<ILoggingEvent> cleanupOutcomeLogs(ListAppender<ILoggingEvent> appender) {
        appender.list.findAll { ILoggingEvent event ->
            event.formattedMessage.startsWith('CATALOG_PARTICIPATION_CLEANUP_PROJECT_RESULT ')
        }
    }

    protected ListAppender<ILoggingEvent> captureFixLogs() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>()
        appender.start()
        ((Logger) LoggerFactory.getLogger(RootController)).addAppender(appender)
        fixLogAppenders.add(appender)
        appender
    }

    protected List<ILoggingEvent> fixOutcomeLogs(ListAppender<ILoggingEvent> appender) {
        appender.list.findAll { ILoggingEvent event ->
            event.formattedMessage.startsWith('CATALOG_PARTICIPATION_FIX_RESULT ')
        }
    }

    protected Map fixture(int projectCount = 2, List<String> nativeUsers = [],
                          Map<String, List<String>> localUsersByProject = [:], int nativeOccurrences = 1) {
        Map source = createSkill(1, 1, 11, 0, 1, 0, 100)
        skillsService.createProjectAndSubjectAndSkills(createProject(1), createSubject(1, 1), [source])
        skillsService.exportSkillToCatalog(source.projectId, source.skillId)
        List<Map> destinations = (2..(projectCount + 1)).collect { int number ->
            Map nativeSkill = createSkill(number, 1, 21, 0, nativeOccurrences, 0, 100)
            skillsService.createProjectAndSubjectAndSkills(createProject(number), createSubject(number, 1), [nativeSkill])
            // Native evidence precedes imports, so no qualification marker is created.
            (nativeUsers + (localUsersByProject[nativeSkill.projectId] ?: [])).unique().each { String user ->
                skillsService.addSkill(nativeSkill, user, new Date() - 10)
            }
            skillsService.createSubject(createSubject(number, 2))
            skillsService.importSkillFromCatalog(nativeSkill.projectId, 'TestSubject2', source.projectId, source.skillId)
            skillsService.finalizeSkillsImportFromCatalog(nativeSkill.projectId)
            Map badge = createBadge(number, 1)
            skillsService.createBadge(badge)
            skillsService.assignSkillToBadge(nativeSkill.projectId, badge.badgeId, source.skillId)
            skillsService.updateBadge(badge + [enabled: 'true'])
            [projectId: nativeSkill.projectId, nativeSkill: nativeSkill, badge: badge,
             imported: skillDefRepo.findByProjectIdAndSkillId(nativeSkill.projectId, source.skillId),
             subject: skillDefRepo.findByProjectIdAndSkillId(nativeSkill.projectId, 'TestSubject2')]
        }
        Map globalBadge = createBadge(1, 90) + [badgeId: 'legacyGlobal']
        skillsService.createGlobalBadge(globalBadge)
        skillsService.assignSkillToGlobalBadge(source.projectId, globalBadge.badgeId, source.skillId)
        skillsService.assignProjectLevelToGlobalBadge(projectId: destinations[0].projectId, badgeId: globalBadge.badgeId, level: '1')
        skillsService.updateGlobalBadge(globalBadge + [enabled: 'true'])
        [source: source, destinations: destinations]
    }

    // Both destinations contain native reuse, two copies of the imported skill, groups and badges.
    // Local evidence is reported before imports so these learners do not acquire participation rows.
    protected Map reuseFixture(Map<String, List<String>> localUsersByProject = [:],
                               int nativeOccurrences = 1, boolean reservePoints = false) {
        Map setup = fixture(2, [], localUsersByProject, nativeOccurrences)
        setup.destinations.eachWithIndex { Map destination, int index ->
            int number = index + 2
            skillsService.createSubject(createSubject(number, 3))
            skillsService.createSubject(createSubject(number, 4))
            Map nativeGroup = createSkillsGroup(number, 3, 31)
            Map importedGroup = createSkillsGroup(number, 4, 41)
            skillsService.createSkill(nativeGroup)
            skillsService.createSkill(importedGroup)
            skillsService.reuseSkills(destination.projectId, [destination.nativeSkill.skillId], 'TestSubject3', nativeGroup.skillId)
            skillsService.reuseSkillInAnotherSubject(destination.projectId, setup.source.skillId, 'TestSubject1')
            skillsService.reuseSkills(destination.projectId, [setup.source.skillId], 'TestSubject4', importedGroup.skillId)
            Map nativeBadge = createBadge(number, 2)
            skillsService.createBadge(nativeBadge)
            skillsService.assignSkillToBadge(destination.projectId, nativeBadge.badgeId, destination.nativeSkill.skillId)
            skillsService.updateBadge(nativeBadge + [enabled: 'true'])
            if (reservePoints) {
                // Unearned available points distinguish valid lower levels from invalid high levels.
                skillsService.createSkill(createSkill(number, 1, 99, 0, 5, 0, 100))
            }
            destination.nativeGroup = skillDefRepo.findByProjectIdAndSkillId(destination.projectId, nativeGroup.skillId)
            destination.importedGroup = skillDefRepo.findByProjectIdAndSkillId(destination.projectId, importedGroup.skillId)
            destination.nativeBadge = nativeBadge
            destination.nativeCopy = skillDefRepo.findByProjectIdAndSkillId(destination.projectId,
                    SkillReuseIdUtil.addTag(destination.nativeSkill.skillId, 0))
            destination.importedCopies = (0..1).collect { int tag ->
                skillDefRepo.findByProjectIdAndSkillId(destination.projectId, SkillReuseIdUtil.addTag(setup.source.skillId, tag))
            }
            destination.subjects = (1..4).collect { int subject ->
                skillDefRepo.findByProjectIdAndSkillId(destination.projectId, "TestSubject${subject}")
            }
            assert destination.nativeCopy.copiedFromProjectId == destination.projectId
            assert destination.importedCopies.every { SkillDef copy -> copy.copiedFromProjectId == destination.projectId }
        }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        setup
    }

    // Repository-only fixtures intentionally emulate legacy credit without manufacturing local events.
    protected void seedPoints(String projectId, String user, SkillDef definition, int points, Date date = new Date() - 5) {
        UserPoints row = userPointsRepo.findByProjectIdAndUserIdAndSkillId(projectId, user, definition?.skillId)
        if (row) {
            row.points = points
        } else {
            row = new UserPoints(projectId: projectId, userId: user, skillId: definition?.skillId,
                    skillRefId: definition?.id, points: points, created: date, updated: date)
        }
        userPointsRepo.save(row)
    }

    protected void seedAward(String projectId, String user, SkillDef definition, Integer level = null,
                             Date date = new Date() - 5) {
        if (!userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, projectId, definition?.skillId).any { UserAchievement award -> award.level == level }) {
            userAchievedRepo.save(new UserAchievement(projectId: projectId, userId: user,
                    skillId: definition?.skillId, skillRefId: definition?.id, level: level,
                    pointsWhenAchieved: 100, achievedOn: date, notified: 'true'))
        }
    }

    protected void seedLevels(String projectId, String user, SkillDef subject, List<Integer> levels,
                              Date date = new Date() - 5) {
        levels.each { Integer level -> seedAward(projectId, user, subject, level, date) }
    }

    protected void assertLevels(String projectId, String user, String subjectId, int maximumLevel) {
        List<Integer> expected = maximumLevel ? (1..maximumLevel).toList() : []
        assert userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, projectId, subjectId)
                .findAll { UserAchievement award -> award.level != null }*.level.sort() == expected
    }

    // Independent of production catalog eligibility SQL: model old-release residue explicitly.
    protected void seedResidue(Map destination, String user, boolean points = true, boolean projectTotal = true) {
        Date date = new Date() - 5
        List<SkillDef> definitions = [destination.imported, destination.subject,
                                     skillDefRepo.findByProjectIdAndSkillId(destination.projectId, destination.badge.badgeId)]
        definitions.each { SkillDef definition ->
            if (points && definition.type != SkillDef.ContainerType.Badge) {
                userPointsRepo.save(new UserPoints(projectId: destination.projectId, userId: user,
                        skillId: definition.skillId, skillRefId: definition.id, points: 100, created: date, updated: date))
            }
            userAchievedRepo.save(new UserAchievement(projectId: destination.projectId, userId: user,
                    skillId: definition.skillId, skillRefId: definition.id, pointsWhenAchieved: 100,
                    level: definition.type == SkillDef.ContainerType.Subject ? 1 : null, achievedOn: date))
        }
        if (projectTotal) {
            if (points) {
                userPointsRepo.save(new UserPoints(projectId: destination.projectId, userId: user,
                        points: 100, created: date, updated: date))
            }
            userAchievedRepo.save(new UserAchievement(projectId: destination.projectId, userId: user,
                    pointsWhenAchieved: 100, level: 1, achievedOn: date))
        }
    }

    protected void seedReusedProgress(Map destination, String user, int nativePoints, int importedPoints, Date earned) {
        if (nativePoints) {
            SkillDef nativeSkill = skillDefRepo.findByProjectIdAndSkillId(destination.projectId, destination.nativeSkill.skillId)
            [nativeSkill, destination.nativeCopy].each { SkillDef skill ->
                seedPoints(destination.projectId, user, skill, nativePoints, earned)
                if (nativePoints == destination.nativeSkill.pointIncrement * destination.nativeSkill.numPerformToCompletion) {
                    seedAward(destination.projectId, user, skill, null, earned)
                }
            }
            if (nativePoints == destination.nativeSkill.pointIncrement * destination.nativeSkill.numPerformToCompletion) {
                seedAward(destination.projectId, user, destination.nativeGroup, null, earned)
                seedAward(destination.projectId, user, skillDefRepo.findByProjectIdAndSkillId(destination.projectId, destination.nativeBadge.badgeId), null, earned)
            }
        }
        if (importedPoints) {
            ([destination.imported] + destination.importedCopies).each { SkillDef skill ->
                seedPoints(destination.projectId, user, skill, importedPoints, earned)
                seedAward(destination.projectId, user, skill, null, earned)
            }
            seedAward(destination.projectId, user, destination.importedGroup, null, earned)
            seedAward(destination.projectId, user, skillDefRepo.findByProjectIdAndSkillId(destination.projectId, destination.badge.badgeId), null, earned)
        }
        List<Integer> points = [nativePoints + importedPoints, importedPoints, nativePoints, importedPoints]
        destination.subjects.eachWithIndex { SkillDef subject, int index ->
            seedPoints(destination.projectId, user, subject, points[index], earned)
            int maximum = [10, 25, 45, 67, 92].count { int percent -> points[index] > 0 && points[index] * 100 >= subject.totalPoints * percent }
            if (maximum) { seedLevels(destination.projectId, user, subject, (1..maximum).toList(), earned) }
        }
        seedPoints(destination.projectId, user, destination.nativeGroup, nativePoints, earned)
        seedPoints(destination.projectId, user, destination.importedGroup, importedPoints, earned)
        int total = points.sum() as int
        seedPoints(destination.projectId, user, null, total, earned)
        int available = projDefRepo.findByProjectIdIgnoreCase(destination.projectId).totalPoints
        int maximum = [10, 25, 45, 67, 92].count { int percent -> total > 0 && total * 100 >= available * percent }
        if (maximum) { seedLevels(destination.projectId, user, null, (1..maximum).toList(), earned) }
    }

    protected void assertDisplayedProgress(Map destination, String user, List<Integer> subjectPoints, int projectPoints,
                                           List<Integer> subjectLevels, int projectLevel, boolean nativeAchieved, boolean importedAchieved) {
        SkillsService learner = createService(user)
        Map summary = learner.getSkillsSummaryForCurrentUser(destination.projectId)
        assert summary.points == projectPoints
        assert summary.skillsLevel == projectLevel
        destination.subjects.eachWithIndex { SkillDef subject, int index ->
            Map result = learner.getSubjectSummaryForCurrentUser(destination.projectId, subject.skillId)
            assert result.points == subjectPoints[index]
            assert result.skillsLevel == subjectLevels[index]
        }
        assert learner.getSkillsGroupSummary(destination.projectId, destination.nativeGroup.skillId).skillsAchieved == (nativeAchieved ? 1 : 0)
        assert learner.getSkillsGroupSummary(destination.projectId, destination.importedGroup.skillId).skillsAchieved == (importedAchieved ? 1 : 0)
        assert learner.getBadgeSummary(null, destination.projectId, destination.nativeBadge.badgeId).badgeAchieved == nativeAchieved
        assert learner.getBadgeSummary(null, destination.projectId, destination.badge.badgeId).badgeAchieved == importedAchieved
    }

    protected Map snapshot(String projectId, String user) {
        [points: userPointsRepo.findByProjectIdAndUserId(projectId, user).sort { it.id }.collect { UserPoints point ->
            [id: point.id, skillId: point.skillId, skillRefId: point.skillRefId, points: point.points,
             created: point.created, updated: point.updated, contributesToSkillsGroup: point.contributesToSkillsGroup]
        }, awards: userAchievedRepo.findAllByUserAndProjectIds(user, [projectId]).sort { it.id }.collect { UserAchievement award -> awardValues(award) }]
    }

    protected List<Map> globalSnapshot(String user) {
        userAchievedRepo.findAllByUserIdAndProjectId(user, null).sort { it.id }.collect { UserAchievement award -> awardValues(award) }
    }

    protected Map awardValues(UserAchievement award) {
        [id: award.id, skillId: award.skillId, skillRefId: award.skillRefId, level: award.level,
         pointsWhenAchieved: award.pointsWhenAchieved, achievedOn: award.achievedOn, created: award.created,
         updated: award.updated, notified: award.notified, expirationNotificationState: award.expirationNotificationState]
    }

    protected Map membership(String projectId, String user) {
        ProjectParticipation row = participationRepo.findByProjectIdAndUserId(projectId, user)
        row ? [id: row.id, status: row.backfillStatus, created: row.created, updated: row.updated] : null
    }

    protected void seedGlobal(String user) {
        if (!userAchievedRepo.existsByUserIdAndSkillIdAndProjectIdIsNull(user, 'legacyGlobal')) {
            userAchievedRepo.save(new UserAchievement(userId: user, skillId: 'legacyGlobal',
                    skillRefId: skillDefRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(null, 'legacyGlobal', SkillDef.ContainerType.GlobalBadge).id,
                    achievedOn: new Date() - 4, pointsWhenAchieved: 100, notified: 'true'))
        }
    }

    protected Map archiveSnapshot(String projectId, String user) {
        ArchivedUser row = archiveRepo.findByProjectIdAndUserId(projectId, user)
        row ? [id: row.id, created: row.created, updated: row.updated] : null
    }

    protected List<Map> events(Integer skillRefId, String user) {
        userPerformedSkillRepo.findAllBySkillRefIdAndUserId(skillRefId, user).sort { it.id }.collect { UserPerformedSkill event ->
            [id: event.id, userId: event.userId, skillId: event.skillId, skillRefId: event.skillRefId,
             projectId: event.projectId, performedOn: event.performedOn, created: event.created, updated: event.updated]
        }
    }
}
