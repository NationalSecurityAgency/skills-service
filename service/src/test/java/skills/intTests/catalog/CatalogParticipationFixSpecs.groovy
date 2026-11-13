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
import org.springframework.test.annotation.DirtiesContext
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.userActions.DashboardItem
import skills.storage.model.ProjectParticipation
import skills.storage.model.ProjectParticipation.BackfillStatus
import skills.storage.model.Setting
import skills.storage.model.UserPoints

/** Temporary pre-5.1 targeted Fix acceptance; retire after remediation. */
// Close this context's scheduler before the dedicated IT context installs worker barriers.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogParticipationFixSpecs extends CatalogCleanupIntSpec {
    def "Fix logs committed and rejected repair without storing user actions or claiming success for rollback"() {
        Map setup = fixture(1)
        List<String> users = getRandomUsers(2)
        SkillsService learner = createService(users[0])
        learner.initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        SkillsService root = createRootSkillService()
        ListAppender<ILoggingEvent> logs = captureFixLogs()

        when:
        root.fixImportedSkillProgress('testproject2', users[0])

        then:
        learner.getSkillsSummaryForCurrentUser('TestProject2').points == 0
        fixOutcomeLogs(logs).size() == 1
        ILoggingEvent committed = fixOutcomeLogs(logs)[0]
        committed.level == Level.INFO
        committed.formattedMessage.contains("actor=[${root.userName.toLowerCase()}]")
        committed.formattedMessage.contains('projectId=[TestProject2]')
        committed.formattedMessage.contains("targetUserId=[${users[0].toLowerCase()}] status=[FIXED]")
        root.getUserActionsForEverything(10, 1, 'created', false, '', DashboardItem.SkillsBatch,
                '', '', 'fixImportedSkillProgress').data.isEmpty()

        when:
        root.fixImportedSkillProgress('TestProject2', users[1])

        then:
        SkillsClientException failure = thrown()
        failure.message.contains('does not enroll')
        !membership('TestProject2', users[1])
        List<ILoggingEvent> outcomes = fixOutcomeLogs(logs)
        outcomes.size() == 2
        outcomes.count { ILoggingEvent event -> event.formattedMessage.contains('status=[FIXED]') } == 1
        ILoggingEvent rejected = outcomes.find { ILoggingEvent event -> event.formattedMessage.contains('status=[FAILED]') }
        rejected.level == Level.ERROR
        rejected.formattedMessage.contains("actor=[${root.userName.toLowerCase()}]")
        rejected.formattedMessage.contains('projectId=[TestProject2]')
        rejected.formattedMessage.contains("targetUserId=[${users[1].toLowerCase()}]")
        rejected.throwableProxy != null
        root.getUserActionsForEverything(10, 1, 'created', false, '', DashboardItem.SkillsBatch,
                '', '', 'fixImportedSkillProgress').data.isEmpty()
    }

    def "Fix enforces current community restrictions while preserving former-member participation and global achievements"() {
        Map setup = fixture(1)
        List<String> users = getRandomUsers(2)
        SkillsService root = createRootSkillService()
        root.saveUserTag(root.userName, 'dragons', ['DivineDragon'])
        root.saveUserTag(skillsService.userName, 'dragons', ['DivineDragon'])
        root.saveUserTag(users[0], 'dragons', ['DivineDragon'])
        skillsService.reuseSkillInAnotherSubject('TestProject2', setup.source.skillId, 'TestSubject1')
        users.each { String user -> skillsService.addSkill(setup.source, user); createService(user).initializeProjectProgress('TestProject2') }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        users.each { String user -> seedGlobal(user) }
        Map memberships = users.collectEntries { String user -> [(user): membership('TestProject2', user)] }
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }
        // Scoped historical policy fixture: emulate credit earned before community restrictions changed.
        settingRepo.save(new Setting(projectId: 'TestProject2', type: Setting.SettingType.Project,
                setting: 'user_community', value: 'true'))

        when:
        users.each { String user -> root.fixImportedSkillProgress('TestProject2', user) }

        then:
        skillsService.getSkillSummary(users[0], 'TestProject2').points == 200
        skillsService.getSkillSummary(users[1], 'TestProject2').points == 0
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId('TestProject2', users[1], setup.source.skillId)
        !userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(users[1], 'TestProject2', setup.source.skillId)
        users.every { String user -> membership('TestProject2', user) == memberships[user] && globalSnapshot(user) == globals[user] &&
                skillsService.getSkillSummary(user, setup.source.projectId).points == 100 }
    }

    def "Fix reconciles positive and empty source state for every participation status without changing membership"() {
        Map setup = fixture()
        List<String> users = getRandomUsers(8)
        Date earned = new Date().clearTime() - 7
        users.take(4).each { String user -> skillsService.addSkill(setup.source, user, earned) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        setup.destinations.each { Map destination ->
            users.eachWithIndex { String user, int index ->
                participationRepo.save(new ProjectParticipation(projectId: destination.projectId, userId: user,
                        backfillStatus: BackfillStatus.values()[index % 4]))
                seedResidue(destination, user)
                UserPoints credit = userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, setup.source.skillId)
                credit.points = 999
                userPointsRepo.save(credit)
                if (index < 4) {
                    userAchievedRepo.deleteAll(userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, destination.badge.badgeId))
                }
                seedGlobal(user)
            }
        }
        Map memberships = [:]
        setup.destinations.each { Map destination -> users.each { String user -> memberships["${destination.projectId}/${user}".toString()] = membership(destination.projectId, user) } }
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }
        SkillsService root = createRootSkillService()

        when:
        setup.destinations.each { Map destination -> users.each { String user -> root.fixImportedSkillProgress(destination.projectId, user) } }

        then:
        setup.destinations.every { Map destination -> users.every { String user ->
            int expectedPoints = user in users.take(4) ? 100 : 0
            skillsService.getSkillSummary(user, destination.projectId).points == expectedPoints &&
                    membership(destination.projectId, user) == memberships["${destination.projectId}/${user}".toString()] &&
                    (userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, setup.source.skillId)?.points ?: 0) == expectedPoints &&
                    userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, setup.source.skillId).size() == (expectedPoints ? 1 : 0) &&
                    userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, destination.badge.badgeId).size() == (expectedPoints ? 1 : 0) &&
                    (!expectedPoints || userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, destination.badge.badgeId).first().achievedOn == earned)
        } }
        users.every { String user -> globalSnapshot(user) == globals[user] }
        !backfillTasks()
    }

    def "later qualification backfills cleaned users while ordinary repeat initialization does not repair"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        skillsService.addSkill(setup.source, user)
        seedResidue(setup.destinations[0], user)
        seedGlobal(user)
        SkillsService root = createRootSkillService()
        SkillsService learner = createService(user)
        List<Map> global = globalSnapshot(user)

        when:
        root.cleanupImportedSkillProgress([projectIds: ['TestProject2']])
        learner.initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        skillsService.getSkillSummary(user, 'TestProject2').points == 100
        membership('TestProject2', user).status == BackfillStatus.COMPLETED

        when:
        userPointsRepo.delete(userPointsRepo.findByProjectIdAndUserIdAndSkillId('TestProject2', user, setup.source.skillId))
        learner.initializeProjectProgress('TestProject2')
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()

        then:
        !userPointsRepo.findByProjectIdAndUserIdAndSkillId('TestProject2', user, setup.source.skillId)
        !backfillTasks()

        when:
        Map participant = membership('TestProject2', user)
        root.fixImportedSkillProgress('TestProject2', user)

        then:
        userPointsRepo.findByProjectIdAndUserIdAndSkillId('TestProject2', user, setup.source.skillId).points == 100
        membership('TestProject2', user) == participant
        globalSnapshot(user) == global
    }

    def "root Fix restores missing and stale credit regardless of status without membership or global side effects"() {
        List<String> users = getRandomUsers(2)
        Map setup = fixture(2, [users[1]])
        Date earned = new Date().clearTime() - 7
        users.each { String user -> skillsService.addSkill(setup.source, user, earned); seedGlobal(user) }
        setup.destinations.each { Map destination -> createService(users[0]).initializeProjectProgress(destination.projectId) }
        waitForAsyncTasksCompletion.waitForAllScheduleTasks()
        SkillsService root = createRootSkillService()
        Map memberships = [:]
        Map globals = users.collectEntries { String user -> [(user): globalSnapshot(user)] }
        Map nativeCredit = [:]
        setup.destinations.each { Map destination ->
            users.each { String user ->
                memberships["${destination.projectId}/${user}".toString()] = membership(destination.projectId, user)
                UserPoints credit = userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, setup.source.skillId)
                if (credit) { userPointsRepo.delete(credit) }
                userAchievedRepo.deleteAll(userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, setup.source.skillId))
            }
            nativeCredit[destination.projectId] = snapshot(destination.projectId, users[1]).points.findAll { it.skillId == destination.nativeSkill.skillId }
            UserPoints total = userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, users[0], null)
            total.points = 17
            userPointsRepo.save(total)
        }

        when:
        setup.destinations.each { Map destination -> users.each { String user -> root.fixImportedSkillProgress(destination.projectId, user) } }

        then:
        setup.destinations.every { Map destination ->
            skillsService.getSkillSummary(users[0], destination.projectId).points == 100 &&
                    skillsService.getSkillSummary(users[1], destination.projectId).points == 200 &&
                    users.every { String user ->
                                membership(destination.projectId, user) == memberships["${destination.projectId}/${user}".toString()] &&
                                userPointsRepo.findByProjectIdAndUserIdAndSkillId(destination.projectId, user, setup.source.skillId).points == 100 &&
                                userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, setup.source.skillId).size() == 1 &&
                                userAchievedRepo.findAllByUserIdAndProjectIdAndSkillId(user, destination.projectId, setup.source.skillId).first().achievedOn == earned
                    } && snapshot(destination.projectId, users[1]).points.findAll { it.skillId == destination.nativeSkill.skillId } == nativeCredit[destination.projectId]
        }
        users.every { String user -> globalSnapshot(user) == globals[user] }
        !membership('TestProject2', users[1])

        when:
        Map repaired = [:]
        setup.destinations.each { Map destination -> users.each { String user -> repaired["${destination.projectId}/${user}".toString()] = snapshot(destination.projectId, user) } }
        setup.destinations.each { Map destination -> users.each { String user -> root.fixImportedSkillProgress(destination.projectId, user) } }

        then:
        users.every { String user -> globalSnapshot(user) == globals[user] }
        skillsService.getSkillSummary(users[0], 'TestProject2').points == 100
        skillsService.getSkillSummary(users[1], 'TestProject3').points == 200
        setup.destinations.every { Map destination -> users.every { String user -> snapshot(destination.projectId, user) == repaired["${destination.projectId}/${user}".toString()] } }
    }

    def "Fix rejects #state without touching participation credit global awards or tasks"() {
        Map setup = fixture(1)
        String user = getRandomUsers(1)[0]
        createService(user).initializeProjectProgress('TestProject2')
        seedGlobal(user)
        Map before = snapshot('TestProject2', user)
        Map participant = membership('TestProject2', user)
        List<Map> global = globalSnapshot(user)
        skillsService.changeSetting('TestProject2', 'catalog.finalize.state', [projectId: 'TestProject2', setting: 'catalog.finalize.state', value: state])

        when:
        createRootSkillService().fixImportedSkillProgress('TestProject2', user)

        then:
        SkillsClientException failure = thrown()
        failure.message.contains('retry Fix')
        snapshot('TestProject2', user) == before
        membership('TestProject2', user) == participant
        globalSnapshot(user) == global
        !backfillTasks()

        where:
        state << ['RUNNING', 'FAILED']
    }
}
