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
package skills.skillLoading


import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.SerializationUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.SkillExceptionBuilder
import skills.controller.result.model.AvailableProjectResult
import skills.controller.result.model.GlobalBadgeLevelRes
import skills.controller.result.model.SettingsResult
import skills.services.BadgeUtils
import skills.services.DependencyValidator
import skills.services.GlobalBadgesService
import skills.services.LevelDefinitionStorageService
import skills.services.settings.SettingsService
import skills.settings.CommonSettings
import skills.skillLoading.model.*
import skills.storage.model.*
import skills.storage.repos.*
import skills.storage.repos.nativeSql.GraphRelWithAchievement
import skills.storage.repos.nativeSql.NativeQueriesRepo
import skills.utils.InputSanitizer

import static skills.services.LevelDefinitionStorageService.LevelInfo

@Component
@CompileStatic
@Slf4j
class SkillsLoader {

    @Value('#{"${skills.subjects.minimumPoints:20}"}')
    int minimumSubjectPoints

    @Value('#{"${skills.project.minimumPoints:20}"}')
    int minimumProjectPoints

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    SkillDefWithExtraRepo skillDefWithExtraRepo

    @Autowired
    SkillRelDefRepo skillRelDefRepo

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    SkillShareDefRepo skillShareDefRepo

    @Autowired
    UserAchievedLevelRepo achievedLevelRepository

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    @Autowired
    PointsHistoryBuilder pointsHistoryBuilder

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    SubjectDataLoader subjectDataLoader

    @Autowired
    DependencySummaryLoader dependencySummaryLoader

    @Autowired
    NativeQueriesRepo nativeQueriesRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    DependencyValidator dependencyValidator

    @Autowired
    SkillEventsSupportRepo skillEventsSupportRepo

    @Autowired
    SkillsLoader skillsLoader

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    RankingLoader rankingLoader

    @Autowired
    SkillApprovalRepo skillApprovalRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    private static String PROP_HELP_URL_ROOT = CommonSettings.HELP_URL_ROOT

    @Transactional(readOnly = true)
    Integer getUserLevel(String projectId, String userId) {
        ProjDef projDef = projDefRepo.findByProjectId(projectId)
        if (!projDef) {
            // indicates that project doesn't exist at all
            return -1
        }

        Integer res = 0
        List<UserAchievement> levels = achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, null)
        if (levels) {
            res = levels.collect({ it.level }).max()
        }
        return res
    }


    @Profile
    @Transactional(readOnly = true)
    List<AvailableProjectResult> getAvailableForMyProjects(String userId) {

        List<ProjDefRepo.AvailableProjectSummary> projectSummaries = projDefRepo.getAvailableProjectSummariesInProduction(userId)
        List<AvailableProjectResult> res = projectSummaries.collect { ProjDefRepo.AvailableProjectSummary summary ->
            String myProjectId = summary.getMyProjectId();
            new AvailableProjectResult(
                    projectId: summary.getProjectId(),
                    name: summary.getName(),
                    totalPoints: summary.getTotalPoints(),
                    numSubjects: summary.getNumSubjects(),
                    numSkills: summary.getNumSkills(),
                    numBadges: summary.getNumBadges(),
                    created: summary.getCreated(),
                    isMyProject: myProjectId != null,
            )
        }

        return res;
    }

    @Profile
    @Transactional(readOnly = true)
    MyProgressSummary loadMyProgressSummary(String userId) {
        MyProgressSummary myProgressSummary = new MyProgressSummary()
        List<ProjectSummaryResult> projectSummaries = projDefRepo.getProjectSummaries(userId)
        for (ProjectSummaryResult summaryResult : projectSummaries.sort({it.getOrderVal()})) {
            ProjectSummary summary = new ProjectSummary().fromProjectSummaryResult(summaryResult)
            myProgressSummary.projectSummaries << summary
            summary.level = getRealLevelInfo(new ProjDef(id: summary.projectRefId, projectId: summary.projectId, totalPoints: summary.totalPoints), userId, summary.points, summary.totalPoints).level  // SLOW!
            myProgressSummary.numProjectsContributed += summary.points > 0 ? 1 : 0
        }

        BadgeCount badgeCount = skillDefRepo.getProductionBadgesCount(userId)
        myProgressSummary.totalBadges = badgeCount.totalCount ?: 0
        myProgressSummary.globalBadgeCount = badgeCount.globalCount ?: 0
        myProgressSummary.gemCount = badgeCount.gemCount ?: 0

        BadgeCount achievedBadgeCounts = achievedLevelRepository.countAchievedProductionBadgesForUser(userId)
        myProgressSummary.numAchievedBadges = achievedBadgeCounts.totalCount ?: 0
        myProgressSummary.numAchievedGemBadges = achievedBadgeCounts.gemCount ?: 0
        myProgressSummary.numAchievedGlobalBadges = achievedBadgeCounts.globalCount ?: 0

        myProgressSummary.totalSkills = skillDefRepo.countTotalProductionSkills(userId)
        AchievedSkillsCount achievedSkillsCount = achievedLevelRepository.countAchievedProductionSkillsForUserByDayWeekMonth(userId)
        myProgressSummary.numAchievedSkills = achievedSkillsCount.totalCount
        myProgressSummary.numAchievedSkillsLastMonth = achievedSkillsCount.monthCount ?: 0
        myProgressSummary.numAchievedSkillsLastWeek = achievedSkillsCount.weekCount ?: 0
        myProgressSummary.mostRecentAchievedSkill = achievedSkillsCount.lastAchieved
        return myProgressSummary
    }

    @Profile
    @Transactional(readOnly = true)
    OverallSkillSummary loadOverallSummary(String projectId, String userId, Integer version = -1) {
        return loadOverallSummary(getProjDef(userId, projectId), userId, version)
    }

    @Profile
    @Transactional(readOnly = true)
    OverallSkillSummary loadOverallSummary(ProjDef projDef, String userId, Integer version = -1) {
        List<SkillSubjectSummary> subjects = loadSubjectsSummaries(projDef, userId, version)

        int points
        int totalPoints
        int skillLevel
        int levelPoints
        int levelTotalPoints
        LevelInfo levelInfo
        int todaysPoints = 0

        if (subjects) {
            points = (int) subjects.collect({ it.points }).sum()
            totalPoints = (int) subjects.collect({ it.totalPoints }).sum()
            levelInfo = getRealLevelInfo(projDef, userId, points, totalPoints)
            todaysPoints = (Integer) subjects?.collect({ it.todaysPoints })?.sum()

            skillLevel = levelInfo?.level
            levelPoints = levelInfo?.currentPoints
            levelTotalPoints = levelInfo?.nextLevelPoints
        }

        if(totalPoints < minimumProjectPoints){
            skillLevel = 0
        }

        //these probably need to exclude badges where enabled = FALSE
        int numBadgesAchieved = achievedLevelRepository.countAchievedForUser(userId, projDef.projectId, SkillDef.ContainerType.Badge)
        int numTotalBadges = skillDefRepo.countByProjectIdAndTypeWhereEnabled(projDef.projectId, SkillDef.ContainerType.Badge)

        // add in global badge counts
        numBadgesAchieved += achievedLevelRepository.countAchievedGlobalForUser(userId, SkillDef.ContainerType.GlobalBadge)
        numTotalBadges += skillDefRepo.countByProjectIdAndTypeWhereEnabled(null, SkillDef.ContainerType.GlobalBadge)

        OverallSkillSummary res = new OverallSkillSummary(
                projectId: projDef.projectId,
                projectName: projDef.name,
                skillsLevel: skillLevel,
                totalLevels: levelInfo?.totalNumLevels ?: 0,
                points: points,
                totalPoints: totalPoints,
                todaysPoints: todaysPoints,
                levelPoints: levelPoints,
                levelTotalPoints: levelTotalPoints,
                subjects: subjects,
                badges: new OverallSkillSummary.BadgeStats(numTotalBadges: numTotalBadges, numBadgesCompleted: numBadgesAchieved, enabled: numTotalBadges > 0)
        )

        return res
    }

    @Profile
    private LevelInfo getRealLevelInfo(ProjDef projDef, String userId, Integer points, Integer totalPoints) {
        LevelInfo levelInfo = levelDefService.getOverallLevelInfo(projDef, points)
        List<UserAchievement> achievedLevels = getProjectsAchievedLevels(userId, projDef.projectId)
        if (achievedLevels) {
            achievedLevels = achievedLevels.sort({ it.level })
            levelInfo = updateLevelBasedOnLastAchieved(projDef, points, achievedLevels.last(), levelInfo, null)
        }
        if(totalPoints < minimumProjectPoints) {
            levelInfo.level = 0
        }
        return levelInfo
    }

    @Profile
    private List<SkillSubjectSummary> loadSubjectsSummaries(ProjDef projDef, String userId, int version) {
        List<SkillDef> subjectsToConvert = loadSubjectsFromDB(projDef)?.sort({ it.displayOrder })
        List<SkillSubjectSummary> subjects = subjectsToConvert?.collect { SkillDef subjectDefinition ->
            return loadSubjectSummary(projDef, userId, subjectDefinition, version)
        }
        subjects
    }

    @Profile
    private List<UserAchievement> getProjectsAchievedLevels(String userId, String projectId) {
        achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projectId, null)
    }

    @Profile
    private List<SkillDef> loadSubjectsFromDB(ProjDef projDef) {
        return skillDefRepo.findAllByProjectIdAndType(projDef.projectId, SkillDef.ContainerType.Subject)
    }

    @Transactional(readOnly = true)
    List<SkillBadgeSummary> loadBadgeSummaries(String projectId, String userId, Integer version = Integer.MAX_VALUE){
        ProjDef projDef = getProjDef(userId, projectId)
        List<SkillDefWithExtra> badgeDefs = skillDefWithExtraRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Badge)
        if ( version >= 0 ) {
            badgeDefs = badgeDefs.findAll { it.version <= version }
        }
        List<SkillBadgeSummary> badges = badgeDefs.sort({ it.displayOrder }).findAll {
            (it.enabled == null || Boolean.valueOf(it.enabled)) && BadgeUtils.afterStartTime(it)
        }.collect { SkillDefWithExtra badgeDefinition ->
            loadBadgeSummary(projDef, userId, badgeDefinition, version)
        }
        return badges
    }

    @Transactional(readOnly = true)
    List<SkillGlobalBadgeSummary> loadGlobalBadgeSummaries(String userId, String projectId, Integer version = Integer.MAX_VALUE){
        List<SkillDefWithExtra> badgeDefs = skillDefWithExtraRepo.findAllByProjectIdAndType(null, SkillDef.ContainerType.GlobalBadge)
        if ( version >= 0 ) {
            badgeDefs = badgeDefs.findAll { it.version <= version }
        }
        List<SkillGlobalBadgeSummary> globalBadges = badgeDefs.sort({ it.displayOrder }).collect { SkillDefWithExtra badgeDefinition ->
            loadGlobalBadgeSummary(userId, projectId, badgeDefinition, version, true)
        }
        globalBadges = globalBadges.findAll { it.projectLevelsAndSkillsSummaries.find { it.projectId == projectId } }
        return globalBadges
    }

    @Transactional(readOnly = true)
    @Profile
    UserPointHistorySummary loadPointHistorySummary(String projectId, String userId, int showHistoryForNumDays, String skillId = null, Integer version = Integer.MAX_VALUE) {
        List<SkillHistoryPoints> historyPoints = pointsHistoryBuilder.buildHistory(projectId, userId, showHistoryForNumDays, skillId, version)
        List<Achievement> achievements = loadLevelAchievements(userId, projectId, skillId, historyPoints, showHistoryForNumDays)

        return new UserPointHistorySummary (
                pointsHistory: historyPoints,
                achievements: achievements
        )
    }

    @Profile
    private List<Achievement> loadLevelAchievements(String userId, String projectId, String skillId, List<SkillHistoryPoints> historyPoints, Integer numDaysBack=365) {
        List<Achievement> achievements

        List<UserAchievement> userAchievements = achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillIdAndLevelNotNullAndAchievedOnAfter(userId,
                projectId,
                skillId,
                new Date().minus(numDaysBack).clearTime())

        if (userAchievements) {
            // must sort levels in tje ascending order since multiple level achievements are joined in the name attribute
            userAchievements = userAchievements.sort({it.level})
            Map<Date, List<SkillHistoryPoints>> ptsByDay = historyPoints?.groupBy { it.dayPerformed }
            Map<Date, AggregateAchievement> achievementsByDay = [:]
            userAchievements.each {
                Date dayOfAchievement = new Date(it.achievedOn.time).clearTime()
                List<SkillHistoryPoints> foundPts = ptsByDay.get(dayOfAchievement)
                if (!foundPts) {
                    log.warn("Failed to locate pts on [${it.achievedOn}] for userId=[${userId}], projecdtId=[${projectId}], skillId=[${skillId}]. This is likely a bug!")
                } else {
                    AggregateAchievement achievement = achievementsByDay[dayOfAchievement]
                    if (achievement) {
                        achievement.levels.add(it.level)
//                        achievement.name += ", ${it.level}"
                    } else {
                        achievementsByDay[dayOfAchievement] = new AggregateAchievement(achievedOn: dayOfAchievement, points: foundPts.first().points, levels: [it.level])
                    }
                }
            }
            achievements = achievementsByDay.values().collect {
                Achievement combined = new Achievement(achievedOn: it.achievedOn, points: it.points)
                String name = it.levels?.sort()?.join(", ")
                combined.name = "Level${name.contains(",") ? "s" : ""} ${name}"
                return combined
            }
        }
        return achievements
    }

    private static class AggregateAchievement {
        List<Integer> levels = []
        Date achievedOn
        Integer points
    }

    @Transactional(readOnly = true)
    SkillSummary loadSkillSummary(String projectId, String userId, String crossProjectId, String skillId) {
        ProjDef projDef = getProjDef(userId, crossProjectId ?: projectId)
        SkillDefWithExtra skillDef = getSkillDefWithExtra(userId, crossProjectId ?: projectId, skillId, SkillDef.ContainerType.Skill)

        if(crossProjectId) {
            dependencyValidator.validateDependencyEligibility(projectId, skillDef)
        }

        UserPoints points = userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(crossProjectId ?: projectId, userId, skillId, null)
        UserPoints todayPoints = userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(crossProjectId ?: projectId, userId, skillId, new Date().clearTime())
        Date achievedOn = achievedLevelRepository.getAchievedDateByUserIdAndProjectIdAndSkillId(userId, projectId, skillId)

        SkillDependencySummary skillDependencySummary
        if (!crossProjectId) {
            skillDependencySummary = dependencySummaryLoader.loadDependencySummary(userId, projectId, skillId)
        }

        SettingsResult helpUrlRootSetting = settingsService.getProjectSetting(crossProjectId ?: projectId, PROP_HELP_URL_ROOT)

        return new SkillSummary(
                projectId: skillDef.projectId, projectName: projDef.name,
                skillId: skillDef.skillId, skill: skillDef.name,
                points: points?.points ?: 0, todaysPoints: todayPoints?.points ?: 0,
                pointIncrement: skillDef.pointIncrement, pointIncrementInterval: skillDef.pointIncrementInterval,
                maxOccurrencesWithinIncrementInterval: skillDef.numMaxOccurrencesIncrementInterval,
                totalPoints: skillDef.totalPoints,
                description: new SkillDescription(
                        skillId: skillDef.skillId,
                        description: InputSanitizer.unsanitizeForMarkdown(skillDef.description),
                        href: getHelpUrl(helpUrlRootSetting, skillDef.helpUrl)),
                dependencyInfo: skillDependencySummary,
                crossProject: crossProjectId != null,
                achievedOn: achievedOn,
                selfReporting: loadSelfReporting(userId, skillDef),
        )
    }

    @Profile
    private SelfReportingInfo loadSelfReporting(String userId, SkillDefWithExtra skillDef){
        boolean enabled = skillDef.selfReportingType != null
        SkillApproval skillApproval = skillApprovalRepo.findByUserIdAndProjectIdAndSkillRefIdAndRejectionAcknowledgedOnIsNull(userId, skillDef.projectId, skillDef.id)

        SelfReportingInfo selfReportingInfo = new SelfReportingInfo(
                approvalId: skillApproval?.getId(),
                enabled: enabled,
                type: skillDef.selfReportingType,
                requestedOn: skillApproval?.requestedOn?.time,
                rejectedOn: skillApproval?.rejectedOn?.time,
                rejectionMsg: skillApproval?.rejectionMsg
        )

        return selfReportingInfo
    }

    @Transactional(readOnly = true)
    SkillSubjectSummary loadSubject(String projectId, String userId, String subjectId, Integer version = -1) {
        ProjDef projDef = getProjDef(userId, projectId)
        SkillDefWithExtra subjectDef = getSkillDefWithExtra(userId, projectId, subjectId, SkillDef.ContainerType.Subject)

        if (version == -1 || subjectDef.version <= version) {
            return loadSubjectSummary(projDef, userId, subjectDef, version, true)
        } else {
            return null
        }
    }

    @Transactional(readOnly = true)
    List<SkillDescription> loadSubjectDescriptions(String projectId, String subjectId, String userId, Integer version = -1) {
        return loadDescriptions(projectId, subjectId, userId, SkillRelDef.RelationshipType.RuleSetDefinition, version)
    }
    @Transactional(readOnly = true)
    List<SkillDescription> loadBadgeDescriptions(String projectId, String badgeId, String userId, Integer version = -1) {
        return loadDescriptions(projectId, badgeId, userId, SkillRelDef.RelationshipType.BadgeRequirement, version)
    }
    @Transactional(readOnly = true)
    List<SkillDescription> loadGlobalBadgeDescriptions(String badgeId, String userId,Integer version = -1) {
        return loadDescriptions(null, badgeId, userId, SkillRelDef.RelationshipType.BadgeRequirement, version)
    }

    private List<SkillDescription> loadDescriptions(String projectId, String subjectId,  String userId, SkillRelDef.RelationshipType relationshipType, int version) {
        SettingsResult helpUrlRootSetting = settingsService.getProjectSetting(projectId, PROP_HELP_URL_ROOT)

        List<SkillDefWithExtraRepo.SkillDescDBRes> dbRes
        Map<String, List<SkillApprovalRepo.SkillApprovalPlusSkillId>> approvalLookup
        if (projectId) {
            dbRes = skillDefWithExtraRepo.findAllChildSkillsDescriptions(projectId, subjectId, relationshipType, version, userId)
            List<SkillApprovalRepo.SkillApprovalPlusSkillId> approvals = skillApprovalRepo.findSkillApprovalsByProjectIdAndSubjectId(userId, projectId, subjectId)
            approvalLookup = approvals.groupBy { it.getSkillId() }
        } else {
            dbRes = skillDefWithExtraRepo.findAllGlobalChildSkillsDescriptions(subjectId, relationshipType, version, userId)
        }
        List<SkillDescription> res = dbRes.collect {
            SkillApprovalRepo.SkillApprovalPlusSkillId skillApproval = approvalLookup?.get(it.getSkillId())?.get(0)
            new SkillDescription(
                    skillId: it.getSkillId(),
                    description: InputSanitizer.unsanitizeForMarkdown(it.getDescription()),
                    href: getHelpUrl(helpUrlRootSetting, it.getHelpUrl()),
                    achievedOn: it.getAchievedOn(),
                    selfReporting: new SelfReportingInfo(
                            approvalId: skillApproval?.getSkillApproval()?.getId(),
                            type: it.getSelfReportingType(),
                            enabled: it.getSelfReportingType() != null,
                            requestedOn: skillApproval?.getSkillApproval()?.getRequestedOn()?.time,
                            rejectedOn: skillApproval?.getSkillApproval()?.getRejectedOn()?.time,
                            rejectionMsg: skillApproval?.getSkillApproval()?.getRejectionMsg()
                    )
            )
        }
        return res
    }

    @Transactional(readOnly = true)
    SkillBadgeSummary loadBadge(String projectId, String userId, String subjectId, Integer version = Integer.MAX_VALUE) {
        ProjDef projDef = getProjDef(userId, projectId)
        SkillDefWithExtra badgeDef = getSkillDefWithExtra(userId, projectId, subjectId, SkillDef.ContainerType.Badge)

        return loadBadgeSummary(projDef, userId, badgeDef, version,true)
    }


    @Transactional(readOnly = true)
    SkillGlobalBadgeSummary loadGlobalBadge(String userId, String originatingProject, String badgeSkillId, Integer version = Integer.MAX_VALUE) {
        SkillDefWithExtra badgeDef = getSkillDefWithExtra(userId, null, badgeSkillId, SkillDef.ContainerType.GlobalBadge)

        return loadGlobalBadgeSummary(userId, originatingProject, badgeDef, version,true)
    }

    @Transactional(readOnly = true)
    SkillDependencyInfo loadSkillDependencyInfo(String projectId, String userId, String skillId) {
        List<GraphRelWithAchievement> graphDBRes = nativeQueriesRepo.getDependencyGraphWithAchievedIndicator(projectId, skillId, userId)

        List<SkillDependencyInfo.SkillRelationshipItem> deps = graphDBRes.collect {
            new SkillDependencyInfo.SkillRelationship(
                    skill: new SkillDependencyInfo.SkillRelationshipItem(projectId: it.parentProjectId, projectName: it.parentProjectName, skillId: it.parentSkillId, skillName: it.parentName),
                    dependsOn: new SkillDependencyInfo.SkillRelationshipItem(projectId: it.childProjectId, projectName: it.childProjectName, skillId: it.childSkillId, skillName: it.childName),
                    achieved: it.achievementId != null,
                    crossProject: projectId != it.childProjectId
            )
        }?.sort({ a,b ->
            a.skill.skillId <=> b.skill.skillId ?: a.dependsOn.skillId <=> b.dependsOn.skillId
        }) as List<SkillDependencyInfo.SkillRelationshipItem>
        return new SkillDependencyInfo(dependencies: deps)
    }

    @Profile
    private SkillSubjectSummary loadSubjectSummary(ProjDef projDef, String userId, SkillDefParent subjectDefinition, Integer version, boolean loadSkills = false) {
        List<SkillSummary> skillsRes = []

        // must compute total points so the provided version is taken into account
        // subjectDefinition.totalPoints is total overall regardless of the version
        int totalPoints
        if (loadSkills) {
            SubjectDataLoader.SkillsData groupChildrenMeta = subjectDataLoader.loadData(userId, projDef.projectId, subjectDefinition.skillId, version)
            skillsRes = createSkillSummaries(projDef, groupChildrenMeta.childrenWithPoints)
            totalPoints = skillsRes ? skillsRes.collect({it.totalPoints}).sum() as Integer: 0
        } else {
            totalPoints = calculateTotalForSkillDef(projDef, subjectDefinition, version)
        }

        Integer points = calculatePoints(projDef, userId, subjectDefinition, version)
        Integer todaysPoints = calculateTodayPoints(projDef, userId, subjectDefinition, version)

        // convert null result to 0
        points = points ?: 0
        todaysPoints = todaysPoints ?: 0

        LevelInfo levelInfo = levelDefService.getLevelInfo(subjectDefinition, points)

        List<UserAchievement> achievedLevels = locateAchievedLevels(userId, projDef, subjectDefinition)
        if (achievedLevels) {
            achievedLevels = achievedLevels.sort({ it.created })
            levelInfo = updateLevelBasedOnLastAchieved(projDef, points, achievedLevels?.last(), levelInfo, subjectDefinition)
        }

        if(subjectDefinition.totalPoints < minimumSubjectPoints){
            levelInfo.level = 0
        }

        String helpUrl = null
        if(subjectDefinition instanceof SkillDefWithExtra) {
            SettingsResult helpUrlRootSetting = settingsService.getProjectSetting(projDef.projectId, PROP_HELP_URL_ROOT)
            helpUrl = getHelpUrl(helpUrlRootSetting, subjectDefinition.helpUrl)
        }

        String description = subjectDefinition instanceof SkillDefWithExtra ? subjectDefinition.description : null
        description = InputSanitizer.unsanitizeForMarkdown(description)

        return new SkillSubjectSummary(
                subject: subjectDefinition.name,
                subjectId: subjectDefinition.skillId,
                description: description,
                points: points,

                skillsLevel: levelInfo.level,
                totalLevels: levelInfo.totalNumLevels,

                levelPoints: levelInfo.currentPoints,
                levelTotalPoints: levelInfo.nextLevelPoints,

                totalPoints: totalPoints,
                todaysPoints: todaysPoints,

                skills: skillsRes,

                iconClass: subjectDefinition.iconClass,

                helpUrl: helpUrl
        )
    }

    @Profile
    private int calculateTotalForSkillDef(ProjDef projDef, SkillDefParent subjectDefinition, int version) {
        Integer res = skillDefRepo.calculateTotalPointsForSkill(projDef.projectId, subjectDefinition.skillId, SkillRelDef.RelationshipType.RuleSetDefinition, version)
        return res ?: 0
    }

    @Profile
    private List<UserAchievement> locateAchievedLevels(String userId, ProjDef projDef, SkillDefParent subjectDefinition) {
        achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projDef.projectId, subjectDefinition.skillId)
    }

    @Profile
    private Integer calculateTodayPoints(ProjDef projDef, String userId, SkillDefParent subjectDefinition, int version) {
        Integer res = userPointsRepo.getPointsByProjectIdAndUserIdAndSkillRefIdAndDay(projDef.projectId, userId, subjectDefinition.id, new Date().clearTime())
        return res ?: 0
    }

    @Profile
    private Integer calculatePoints(ProjDef projDef, String userId, SkillDefParent subjectDefinition, int version) {
        Integer res = userPointsRepo.getPointsByProjectIdAndUserIdAndSkillRefId(projDef.projectId, userId, subjectDefinition.id)
        return res ?: 0
    }

    @Profile
    private Integer calculatePointsForProject(ProjDef projDef, String userId, int version) {
        Integer res = userPointsRepo.getPointsByProjectIdAndUserId(projDef.projectId, userId)
        return res ?: 0
    }

    @Profile
    private SkillBadgeSummary loadBadgeSummary(ProjDef projDef, String userId, SkillDefWithExtra badgeDefinition, Integer version = Integer.MAX_VALUE, boolean loadSkills = false) {
        List<SkillSummary> skillsRes = []

        if (loadSkills) {
            SubjectDataLoader.SkillsData groupChildrenMeta = subjectDataLoader.loadData(userId, projDef?.projectId, badgeDefinition.skillId, version, SkillRelDef.RelationshipType.BadgeRequirement)
            skillsRes = createSkillSummaries(projDef, groupChildrenMeta.childrenWithPoints)?.sort({ it.skill?.toLowerCase() })
        }

        List<UserAchievement> achievements = achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projDef?.projectId, badgeDefinition.skillId)
        if (achievements) {
            // for badges, there should only be one UserAchievement
            assert achievements.size() == 1
        }

        int numAchievedSkills = achievedLevelRepository.countAchievedChildren(userId, projDef?.projectId, badgeDefinition.skillId, SkillRelDef.RelationshipType.BadgeRequirement)
        int numChildSkills = skillDefRepo.countChildren(projDef?.projectId, badgeDefinition.skillId, SkillRelDef.RelationshipType.BadgeRequirement)

        SettingsResult helpUrlRootSetting = settingsService.getProjectSetting(projDef.projectId, PROP_HELP_URL_ROOT)
        String helpUrl = getHelpUrl(helpUrlRootSetting, badgeDefinition.helpUrl)

        return new SkillBadgeSummary(
                badge: badgeDefinition.name,
                badgeId: badgeDefinition.skillId,
                description: InputSanitizer.unsanitizeForMarkdown(badgeDefinition.description),
                badgeAchieved: achievements?.size() > 0,
                dateAchieved: achievements ? achievements.first().achievedOn : null,
                numSkillsAchieved: numAchievedSkills,
                numTotalSkills: numChildSkills,
                startDate: badgeDefinition.startDate,
                endDate: badgeDefinition.endDate,
                skills: skillsRes,
                iconClass: badgeDefinition.iconClass,
                helpUrl: helpUrl
        )
    }

    @Profile
    private SkillGlobalBadgeSummary loadGlobalBadgeSummary(String userId, String originatingProject, SkillDefWithExtra badgeDefinition, Integer version = Integer.MAX_VALUE, boolean loadSkills = false) {
        List<SkillSummary> skillsRes = []

        if (loadSkills) {
            SubjectDataLoader.SkillsData groupChildrenMeta = subjectDataLoader.loadData(userId, null, badgeDefinition.skillId, version, SkillRelDef.RelationshipType.BadgeRequirement)
            skillsRes = createSkillSummaries(null, groupChildrenMeta.childrenWithPoints)?.sort({ it.skill?.toLowerCase() })
            if (skillsRes) {
                // all the skills are "cross-project" if they don't belong to the project that originated this reqest
                skillsRes.each {
                    if (it.projectId != originatingProject) {
                        it.crossProject = true
                    }
                }
            }
        }

        List<UserAchievement> achievements = achievedLevelRepository.findAllByUserIdAndSkillId(userId, badgeDefinition.skillId)
        if (achievements) {
            // for badges, there should only be one UserAchievement
            assert achievements.size() == 1
        }

        int numAchievedSkills = achievedLevelRepository.countAchievedGlobalSkills(userId, badgeDefinition.skillId, SkillRelDef.RelationshipType.BadgeRequirement)
        int numChildSkills = skillDefRepo.countGlobalChildren(badgeDefinition.skillId, SkillRelDef.RelationshipType.BadgeRequirement)

        List<UserAchievement> achievedLevels = achievedLevelRepository.findAllLevelsByUserId(userId)
        Map<String, Integer> userProjectLevels = (Map<String, Integer>)achievedLevels?.groupBy { it.projectId }
                ?.collectEntries {String key, List<UserAchievement> val -> [key,val.collect{it.level}.max()]}

        List<GlobalBadgeLevelRes> requiredLevels = globalBadgesService.getGlobalBadgeLevels(badgeDefinition.skillId)
        List<ProjectLevelSummary> projectLevels = []
        for (GlobalBadgeLevelRes requiredLevel : requiredLevels) {
            ProjectLevelSummary projectLevelSummary = new ProjectLevelSummary(projectId: requiredLevel.projectId, projectName: requiredLevel.projectName, requiredLevel: requiredLevel.level)
            projectLevels.add(projectLevelSummary)
            if (userProjectLevels.containsKey(requiredLevel.projectId)) {
                Integer achievedProjectLevel = userProjectLevels.get(requiredLevel.projectId)
                projectLevelSummary.achievedLevel = achievedProjectLevel
                if (achievedProjectLevel >= requiredLevel.level) {
                    numAchievedSkills++
                }
            }
        }
        numChildSkills += requiredLevels.size()

        Map<String, ProjectLevelsAndSkillsSummary> byProject = [:]
        skillsRes.groupBy { it.projectId }.each { projectId, skills ->
            byProject[projectId] = new ProjectLevelsAndSkillsSummary(projectId: projectId, projectName: skills.first().projectName, skills: skills)
        }
        projectLevels.groupBy { it.projectId }.each { projectId, levels ->
            assert levels.size() == 1
            ProjectLevelSummary projectLevel = levels.first()
            ProjectLevelsAndSkillsSummary projectLevelsAndSkillsSummary = byProject.get(projectId)
            if (projectLevelsAndSkillsSummary) {
                projectLevelsAndSkillsSummary.projectLevel = projectLevel
            } else {
                byProject[projectId] = new ProjectLevelsAndSkillsSummary(projectId: projectId, projectName: projectLevel.projectName, projectLevel: projectLevel)
            }
        }

        return new SkillGlobalBadgeSummary(
                badge: badgeDefinition.name,
                badgeId: badgeDefinition.skillId,
                description: InputSanitizer.unsanitizeForMarkdown(badgeDefinition.description),
                badgeAchieved: achievements?.size() > 0,
                dateAchieved: achievements ? achievements.first().achievedOn : null,
                numSkillsAchieved: numAchievedSkills,
                numTotalSkills: numChildSkills,
                startDate: badgeDefinition.startDate,
                endDate: badgeDefinition.endDate,
                skills: skillsRes,
                iconClass: badgeDefinition.iconClass,
                projectLevelsAndSkillsSummaries: byProject.values(),
                helpUrl: badgeDefinition.helpUrl
        )
    }

    private String getHelpUrl(SettingsResult helpUrlRootSetting, String helpUrl) {
        String res = helpUrl

        if (helpUrl && helpUrlRootSetting && !helpUrl.toLowerCase().startsWith("http")) {
            String rootUrl = helpUrlRootSetting.value
            if (rootUrl.endsWith("/") && res.startsWith("/")) {
                rootUrl = rootUrl.substring(0, rootUrl.length() - 1)
            }
            res = rootUrl + res
        }

        return res
    }

    @Profile
    private List<SkillSummary> createSkillSummaries(ProjDef thisProjDef, List<SubjectDataLoader.SkillsAndPoints> childrenWithPoints) {
        List<SkillSummary> skillsRes = []

        Map<String,ProjDef> projDefMap = [:]
        childrenWithPoints.each { SubjectDataLoader.SkillsAndPoints skillDefAndUserPoints ->
            SkillDef skillDef = skillDefAndUserPoints.skillDef
            int points = skillDefAndUserPoints.points
            int todayPoints = skillDefAndUserPoints.todaysPoints

            // support skill summaries from other projects
            ProjDef projDef = thisProjDef && thisProjDef.projectId == skillDef.projectId ? thisProjDef : projDefMap[skillDef.projectId]
            if(!projDef){
                projDef = projDefRepo.findByProjectId(skillDef.projectId)
                projDefMap[skillDef.projectId] = projDef
            }

            skillsRes << new SkillSummary(
                    projectId: skillDef.projectId, projectName: projDef.name,
                    skillId: skillDef.skillId, skill: skillDef.name,
                    points: points, todaysPoints: todayPoints,
                    pointIncrement: skillDef.pointIncrement, pointIncrementInterval: skillDef.pointIncrementInterval,
                    maxOccurrencesWithinIncrementInterval: skillDef.numMaxOccurrencesIncrementInterval,
                    totalPoints: skillDef.totalPoints,
                    dependencyInfo: skillDefAndUserPoints.dependencyInfo,
                    selfReporting: skillDef.selfReportingType ? new SelfReportingInfo(enabled: true, type: skillDef.selfReportingType) : null,
            )
        }

        return skillsRes
    }

    /**
     * see if the user already achieved the next level, this can happen if user achieved the next level and then we added new skill with more points
     * which re-balanced how many points required for that level;
     * in that case we'll assign already achieved level and add the points from the previous levels + new level (total number till next level achievement)
     */
    @Profile
    private LevelInfo updateLevelBasedOnLastAchieved(ProjDef projDef, int points, UserAchievement lastAchievedLevel, LevelInfo calculatedLevelInfo, SkillDefParent subjectDef) {
        LevelInfo res = SerializationUtils.clone(calculatedLevelInfo)

        int maxLevel = Integer.MAX_VALUE
        if(subjectDef){
            maxLevel = subjectDef.levelDefinitions.size()
        }else{
            maxLevel = levelDefService.maxProjectLevel(projDef)
        }

        if (lastAchievedLevel && lastAchievedLevel?.level > calculatedLevelInfo.level) {
            if (lastAchievedLevel.level >= maxLevel) {
                res.currentPoints = 0
                res.nextLevelPoints = -1
                res.level = maxLevel
            } else {
                int nextLevelPointsToAchievel

                if (subjectDef) {
                    nextLevelPointsToAchievel = levelDefService.getPointsRequiredForLevel(subjectDef, lastAchievedLevel.level + 1)
                } else {
                    nextLevelPointsToAchievel = levelDefService.getPointsRequiredForOverallLevel(projDef, lastAchievedLevel.level + 1)
                }

                int numPtsLeftInPreviousLevel = nextLevelPointsToAchievel - points

                res.level = lastAchievedLevel?.level
                res.currentPoints = 0
                res.nextLevelPoints = numPtsLeftInPreviousLevel
            }
        }

        return res
    }

    private ProjDef getProjDef(String userId, String projectId) {
        ProjDef projDef = projDefRepo.findByProjectId(projectId)
        if(!projDef){
            throw new SkillExceptionBuilder()
                    .msg("Project definition with id [${projectId}] doesn't exist")
                    .userId(userId)
                    .projectId(projectId)
                    .build()
        }
        return projDef
    }

    private SkillDefWithExtra getSkillDefWithExtra(String userId, String projectId, String skillId, SkillDef.ContainerType containerType) {
        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndType(projectId, skillId, containerType)
        if (!skillDef) {
            throw new SkillExceptionBuilder()
                    .msg("Skill definition with id [${skillId}] doesn't exist")
                    .userId(userId)
                    .projectId(projectId)
                    .skillId(skillId)
                    .build()
        }
        return skillDef
    }
}
