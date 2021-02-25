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
package skills.services

import skills.services.events.*
import skills.services.events.pointsAndAchievements.PointsAndAchievementsHandler
import skills.storage.model.SkillDef
import skills.storage.repos.SkillEventsSupportRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo
import skills.utils.MetricsLogger
import spock.lang.Specification

import static skills.storage.repos.SkillEventsSupportRepo.SkillDefMin

class SkillEventServiceUnitSpecs extends Specification {

    String userId = 'userId'
    String projId = 'projId'
    String skillId = 'skillId'

    def "test reportSkill will notify when skills is applied"() {
        SkillEventPublisher mockSkillEventPublisher = Mock()
        SkillEventsSupportRepo mockSkillEventsSupportRepo = Mock()
        UserPerformedSkillRepo mockPerformedSkillRepository = Mock()
        TimeWindowHelper mockTimeWindowHelper = Mock()
        CheckDependenciesHelper mockCheckDependenciesHelper = Mock()
        LockingService mockLockingService = Mock()
        PointsAndAchievementsHandler mockPointsAndAchievementsHandler = Mock()
        UserAchievedLevelRepo mockAchievedLevelRepo = Mock()
        AchievedBadgeHandler mockAchievedBadgeHandler = Mock()
        AchievedGlobalBadgeHandler mockAchievedGlobalBadgeHandler = Mock()
        MetricsLogger mockMetricsLogger = Mock()
        UserEventService mockUserEventService = Mock()

        SkillEventsService skillEventsService = new SkillEventsService(
                skillEventPublisher: mockSkillEventPublisher,
                skillEventsSupportRepo: mockSkillEventsSupportRepo,
                performedSkillRepository: mockPerformedSkillRepository,
                timeWindowHelper: mockTimeWindowHelper,
                checkDependenciesHelper: mockCheckDependenciesHelper,
                lockingService: mockLockingService,
                pointsAndAchievementsHandler: mockPointsAndAchievementsHandler,
                achievedLevelRepo: mockAchievedLevelRepo,
                achievedBadgeHandler: mockAchievedBadgeHandler,
                achievedGlobalBadgeHandler: mockAchievedGlobalBadgeHandler,
                metricsLogger: mockMetricsLogger,
                userEventService: mockUserEventService
        )

        // make it so skill has NOT already reached it's max points, is withing the time window, and has achieved any dependencies
        SkillDefMin skillDefMin = Mock()
        skillDefMin.getPointIncrement() >> 50
        skillDefMin.getTotalPoints() >> 100
        skillDefMin.skillId >> skillId
        skillDefMin.name >> 'Skill ID'
        skillDefMin.projectId >> projId
        mockSkillEventsSupportRepo.findByProjectIdAndSkillIdAndType(projId, skillId, SkillDef.ContainerType.Skill) >> skillDefMin
        mockPerformedSkillRepository.countByUserIdAndProjectIdAndSkillId(userId, projId, skillId) >> 1
        mockTimeWindowHelper.checkTimeWindow(_, _, _) >> new TimeWindowHelper.TimeWindowRes(full: false)
        mockCheckDependenciesHelper.check(_, _, _) >> new CheckDependenciesHelper.DependencyCheckRes(hasNotAchievedDependents: false)

        when:
        Boolean notifyIfNotApplied = false
        skillEventsService.reportSkill(projId, skillId, userId, notifyIfNotApplied, null)

        then:
        true
        1 * mockSkillEventPublisher.publishSkillUpdate(_, userId)
        1 * mockUserEventService.recordEvent(projId, _, userId, _)
        1 * mockMetricsLogger.log(_, { it['selfReported'] == 'false' && it['selfReportType'] == null })
    }

    def "test reportSkill will NOT notify when skills is NOT applied"() {
        SkillEventPublisher mockSkillEventPublisher = Mock()
        SkillEventsSupportRepo mockSkillEventsSupportRepo = Mock()
        UserPerformedSkillRepo mockPerformedSkillRepository = Mock()
        MetricsLogger mockMetricsLogger = Mock()
        UserEventService mockUserEventService = Mock()

        SkillEventsService skillEventsService = new SkillEventsService(
                skillEventPublisher: mockSkillEventPublisher,
                skillEventsSupportRepo: mockSkillEventsSupportRepo,
                performedSkillRepository: mockPerformedSkillRepository,
                metricsLogger: mockMetricsLogger,
                userEventService: mockUserEventService
        )

        // make it so skill has already reached it's max points so result.skillApplied will be false
        SkillDefMin skillDefMin = Mock()
        skillDefMin.getPointIncrement() >> 100
        skillDefMin.getTotalPoints() >> 50
        skillDefMin.getSelfReportingType() >> SkillDef.SelfReportingType.HonorSystem
        mockSkillEventsSupportRepo.findByProjectIdAndSkillIdAndType(projId, skillId, SkillDef.ContainerType.Skill) >> skillDefMin
        mockPerformedSkillRepository.countByUserIdAndProjectIdAndSkillId(userId, projId, skillId) >> 1

        when:
        Boolean notifyIfNotApplied = false
        skillEventsService.reportSkill(projId, skillId, userId, notifyIfNotApplied, null)

        then:
        0 * mockSkillEventPublisher.publishSkillUpdate(_, userId)
        1 * mockUserEventService.recordEvent(projId, _, userId, _)
        1 * mockMetricsLogger.log(_, { it['selfReported'] == 'true' && it['selfReportType'] == 'HonorSystem' })
    }

    def "test reportSkill will notify when skills is NOT applied, but notifyIfNotApplied is true "() {
        SkillEventPublisher mockSkillEventPublisher = Mock()
        SkillEventsSupportRepo mockSkillEventsSupportRepo = Mock()
        UserPerformedSkillRepo mockPerformedSkillRepository = Mock()
        MetricsLogger mockMetricsLogger = Mock()
        UserEventService mockUserEventService = Mock()

        SkillEventsService skillEventsService = new SkillEventsService(
                skillEventPublisher: mockSkillEventPublisher,
                skillEventsSupportRepo: mockSkillEventsSupportRepo,
                performedSkillRepository: mockPerformedSkillRepository,
                metricsLogger: mockMetricsLogger,
                userEventService: mockUserEventService
        )

        // make it so skill has already reached it's max points so result.skillApplied will be false
        SkillDefMin skillDefMin = Mock()
        skillDefMin.getPointIncrement() >> 100
        skillDefMin.getTotalPoints() >> 50
        skillDefMin.getSelfReportingType() >> SkillDef.SelfReportingType.Approval
        mockSkillEventsSupportRepo.findByProjectIdAndSkillIdAndType(projId, skillId, SkillDef.ContainerType.Skill) >> skillDefMin
        mockPerformedSkillRepository.countByUserIdAndProjectIdAndSkillId(userId, projId, skillId) >> 1

        when:
        Boolean notifyIfNotApplied = true // notify even if skill is not applied
        skillEventsService.reportSkill(projId, skillId, userId, notifyIfNotApplied, null)

        then:
        1 * mockSkillEventPublisher.publishSkillUpdate(_, userId)
        1 * mockUserEventService.recordEvent(projId, _, userId, _)
        1 * mockMetricsLogger.log(_, { it['selfReported'] == 'true' && it['selfReportType'] == 'Approval' })
    }
}
