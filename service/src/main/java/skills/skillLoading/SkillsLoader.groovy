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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.util.Pair
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.PublicProps
import skills.auth.SkillsAuthorizationException
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.exceptions.SkillExceptionBuilder
import skills.controller.request.model.UserProjectSettingsRequest
import skills.controller.result.model.AvailableProjectResult
import skills.controller.result.model.GlobalBadgeLevelRes
import skills.controller.result.model.SettingsResult
import skills.services.BadgeUtils
import skills.services.DependencyValidator
import skills.services.GlobalBadgesService
import skills.services.LevelDefinitionStorageService
import skills.services.admin.SkillsGroupAdminService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.settings.ClientPrefKey
import skills.services.settings.ClientPrefService
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.settings.CommonSettings
import skills.skillLoading.model.*
import skills.storage.model.*
import skills.storage.repos.*
import skills.storage.repos.nativeSql.GraphRelWithAchievement
import skills.storage.repos.nativeSql.NativeQueriesRepo
import skills.utils.InputSanitizer

import java.util.stream.Stream

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
    private PublicProps publicProps;

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
    ClientPrefService clientPrefService

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

    @Autowired
    SkillsGroupAdminService skillsGroupAdminService

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
                    name: InputSanitizer.unsanitizeName(summary.getName()),
                    totalPoints: summary.getTotalPoints(),
                    numSubjects: summary.getNumSubjects(),
                    numSkills: summary.getNumSkills(),
                    numBadges: summary.getNumBadges(),
                    created: summary.getCreated(),
                    isMyProject: myProjectId != null,
                    hasDescription: summary.getHasDescription(),
            )
        }

        return res;
    }

    @Profile
    @Transactional(readOnly = true)
    MyProgressSummary loadMyProgressSummary(String userId) {
        MyProgressSummary myProgressSummary = new MyProgressSummary()
        List<ProjectSummaryResult> projectSummaries = projDefRepo.getProjectSummaries(userId)
        List<SettingsResult> customLevelTextForProjects = settingsService.getProjectSettingsForAllProjects('level.displayName')
        for (ProjectSummaryResult summaryResult : projectSummaries.sort({it.getOrderVal()})) {
            ProjectSummary summary = new ProjectSummary().fromProjectSummaryResult(summaryResult)
            myProgressSummary.projectSummaries << summary
            summary.level = getRealLevelInfo(new ProjDef(id: summary.projectRefId, projectId: summary.projectId, totalPoints: summary.totalPoints), userId, summary.points, summary.totalPoints).level  // SLOW!
            myProgressSummary.numProjectsContributed += summary.points > 0 ? 1 : 0
            SettingsResult customLevelText = customLevelTextForProjects.find { it.projectId == summary.projectId }
            if (customLevelText && customLevelText.value) {
                summary.levelDisplayName = customLevelText.value
            }
        }

        BadgeCount badgeCount = skillDefRepo.getProductionMyBadgesCount(userId)
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
    List<? extends SkillBadgeSummary> getBadgesForUserMyProjects(String userId) {
        List<? extends SkillBadgeSummary> badges = []

        skillDefWithExtraRepo.findAllMyBadgesForUser(userId).withCloseable { Stream<SkillDefWithExtra> rawBadges ->
            rawBadges?.forEach({
                if (it.projectId) {
                    ProjDef projDef = projDefRepo.findByProjectId(it.projectId)
                    badges << loadBadgeSummary(projDef, userId, it, Integer.MAX_VALUE, false, true)
                } else {
                    // we have to load badge skills, if no project level is defined then without loading skills,
                    // the client can't identify the project_ids involved in the global bage
                    def badge = loadGlobalBadgeSummary(userId, null, it, Integer.MAX_VALUE, true)
                    if (badge.projectLevelsAndSkillsSummaries) {
                        badges << badge
                    }
                }
            })
        }

        return badges.sort { it.badge }
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
        numBadgesAchieved += achievedLevelRepository.countAchievedGlobalBadgeForUserIntersectingProjectId(userId, projDef.projectId)
        numTotalBadges += skillDefRepo.countGlobalBadgesIntersectingWithProjectIdWhereEnabled(projDef.projectId)

        OverallSkillSummary res = new OverallSkillSummary(
                projectId: projDef.projectId,
                projectName: InputSanitizer.unsanitizeName(projDef.name),
                skillsLevel: skillLevel,
                totalLevels: levelInfo?.totalNumLevels ?: 0,
                points: points,
                totalPoints: totalPoints,
                todaysPoints: todaysPoints,
                levelPoints: levelPoints,
                levelTotalPoints: levelTotalPoints,
                subjects: subjects,
                badges: new OverallSkillSummary.BadgeStats(numTotalBadges: numTotalBadges, numBadgesCompleted: numBadgesAchieved, enabled: numTotalBadges > 0),
                projectDescription: InputSanitizer.unsanitizeForMarkdown(projDef.description)
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
        List<SkillDefWithExtra> badgeDefs = skillDefWithExtraRepo.findAllByProjectIdAndTypeAndEnabled(null, SkillDef.ContainerType.GlobalBadge, Boolean.TRUE.toString())
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
        List<Achievement> achievements = historyPoints ? loadLevelAchievements(userId, projectId, skillId, historyPoints, showHistoryForNumDays) : []

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

            String customLevelName = settingsService.getProjectSetting(projectId, 'level.displayName')?.value ?: 'Level'
            achievements = achievementsByDay.values().collect {
                Achievement combined = new Achievement(achievedOn: it.achievedOn, points: it.points)
                String name = it.levels?.sort()?.join(", ")
                combined.name = "${customLevelName}${name.contains(",") ? "s" : ""} ${name}"
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

    private DisplayOrderRes getNextSkill(List<DisplayOrderRes> skills, Boolean prev, int displayOrder, String groupId) {
        def skill
        // There is no next element - i.e. the start/end of the group has been reached, so exit the group

        def isGroupNext = skills.findAll({ it.groupId != groupId && it.getSkillGroupDisplayOrder() == displayOrder})?.sort({a, b -> a.displayOrder <=> b.displayOrder})
        if(isGroupNext.size() > 0) {
            // Next item in the row is a group, so grab the first / last element
            skill = prev ? isGroupNext.last() : isGroupNext.first()
        }
        else {
            skill = skills.find({it -> it.groupId == null && it.displayOrder == displayOrder})
        }

        return skill
    }

    private String getAdjacentSkillId(Boolean withinSkillGroup, DisplayOrderRes currentSkill, List<DisplayOrderRes> skills, Boolean prev) {
        def skill
        def displayOrder = prev ? currentSkill.displayOrder - 1 : currentSkill.displayOrder + 1;

        if(withinSkillGroup) {
            // Navigating within a group, so try to find the next element
            List <DisplayOrderRes> currentGroup = skills.findAll({it.groupId == currentSkill.groupId});
            skill = currentGroup.find({ it -> it.displayOrder == displayOrder })

            if(!skill) {
                // There is no next element - i.e. the start/end of the group has been reached, so exit the group
                displayOrder = prev ? currentSkill.skillGroupDisplayOrder - 1 : currentSkill.skillGroupDisplayOrder + 1;
                skill = getNextSkill(skills, prev, displayOrder, currentSkill.groupId)
            }
        }
        else {
            // Not within a group, so navigate to the next element
            skill = getNextSkill(skills, prev, displayOrder, currentSkill.groupId)
        }

        return skill?.skillId
    }

    int sortByDisplayOrder(DisplayOrderRes a, DisplayOrderRes b) {
        if( a.groupId != null || b.groupId != null ) {
            if( a.groupId != null && b.groupId != null) {
                if( a.groupId != b.groupId ) {
                    return a.skillGroupDisplayOrder <=> b.skillGroupDisplayOrder
                }
            }
            else {
                if( a.groupId != null && b.groupId == null ) {
                    return a.skillGroupDisplayOrder <=> b.displayOrder
                }
                else {
                    return a.displayOrder <=> b.skillGroupDisplayOrder
                }
            }
        }

        return a.displayOrder <=> b.displayOrder
    }

    @Transactional(readOnly = true)
    SkillSummary loadSkillSummary(String projectId, String userId, String crossProjectId, String skillId, String subjectId) {
        ProjDef projDef = getProjDef(userId, crossProjectId ?: projectId)
        SkillDefWithExtra skillDef = getSkillDefWithExtra(userId, crossProjectId ?: projectId, skillId, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.SkillsGroup])

        String nextSkillId;
        String prevSkillId;
        int totalSkills = 0;
        int orderInGroup = 0;

        boolean isCrossProjectSkill = crossProjectId && crossProjectId != projectId
        if(subjectId && !isCrossProjectSkill) {
            List<DisplayOrderRes> skills = skillDefRepo.findDisplayOrderByProjectIdAndSubjectId(projectId, subjectId)?.sort({a, b -> sortByDisplayOrder(a, b)})
            def currentSkill = skills.find({ it -> it.getSkillId() == skillId })
            def orderedGroup = skills?.sort({a, b -> sortByDisplayOrder(a, b)});
            orderInGroup = orderedGroup.findIndexOf({it -> it.skillId == currentSkill.skillId}) + 1;
            totalSkills = orderedGroup.size();

            if (currentSkill) {
                def withinSkillGroup = currentSkill.groupId != null ? true : false;

                prevSkillId = getAdjacentSkillId(withinSkillGroup, currentSkill, skills, true)
                nextSkillId = getAdjacentSkillId(withinSkillGroup, currentSkill, skills, false)
            }
        }

        if (crossProjectId) {
            dependencyValidator.validateDependencyEligibility(projectId, skillDef)
        }

        UserPoints up = userPointsRepo.findByProjectIdAndUserIdAndSkillId(crossProjectId ?: projectId, userId, skillId)
        Integer points = up ? up.points : 0
        Integer todayPoints = userPointsRepo.calculatePointsForSingleSkillForADay(userId, skillDef.id, new Date().clearTime()) ?: 0
        Date achievedOn = achievedLevelRepository.getAchievedDateByUserIdAndProjectIdAndSkillId(userId, projectId, skillId)


        if (skillDef.copiedFrom != null && skillDef.selfReportingType) {
            // because of the catalog's async nature when self-approval honor skill is submitted todaysPoints and points are not consistent on the imported side
            // this is because todaysPoints are calculated from UserPerformedSkill but points come from UserPoints; UserPerformedSkill
            // is shared in the catalog exported/imported skills but UserPoints are duplicated and asynchronously synced
            if (todayPoints > points) {
                // this will at least account for 1 event that have not been propagated and make it a bit more consistent
                // it mostly likely will account for the first event only unless multiple skill events are submitted in the same day
                points = points + skillDef.pointIncrement
            }
        }

        SkillDependencySummary skillDependencySummary
        if (!crossProjectId) {
            skillDependencySummary = dependencySummaryLoader.loadDependencySummary(userId, projectId, skillId)
        }

        SettingsResult helpUrlRootSetting = settingsService.getProjectSetting(crossProjectId ?: projectId, PROP_HELP_URL_ROOT)
        String copiedFromProjectName = skillDef.copiedFromProjectId ? projDefRepo.getProjectName(skillDef.copiedFromProjectId).getProjectName() : null

        String unsanitizedName = InputSanitizer.unsanitizeName(skillDef.name)
        boolean isReusedSkill = SkillReuseIdUtil.isTagged(unsanitizedName)
        return new SkillSummary(
                projectId: skillDef.projectId,
                projectName: InputSanitizer.unsanitizeName(projDef.name),
                skillId: skillDef.skillId,
                prevSkillId: prevSkillId,
                nextSkillId: nextSkillId,
                orderInGroup: orderInGroup,
                totalSkills: totalSkills,
                skill: isReusedSkill ? SkillReuseIdUtil.removeTag(unsanitizedName) : unsanitizedName,
                points: points, todaysPoints: todayPoints,
                pointIncrement: skillDef.pointIncrement,
                pointIncrementInterval: skillDef.pointIncrementInterval,
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
                type: skillDef.type,
                copiedFromProjectId: isReusedSkill ? null : skillDef.copiedFromProjectId,
                copiedFromProjectName: isReusedSkill ? null : InputSanitizer.unsanitizeName(copiedFromProjectName),
        )
    }

    void documentLastViewedSkillId(String projectId, String skillId) {
        clientPrefService.saveOrUpdateProjPrefForCurrentUser(ClientPrefKey.LastViewedSkill, skillId, projectId)
    }

    @Profile
    private SelfReportingInfo loadSelfReporting(String userId, SkillDefWithExtra skillDef){
        boolean enabled = skillDef.selfReportingType != null
        Pageable oneRowPlease = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "requestedOn"))
        String queryProjId = skillDef.copiedFrom ? skillDef.copiedFromProjectId : skillDef.projectId
        Integer querySkillRefId = skillDef.copiedFrom ? skillDef.copiedFrom : skillDef.id
        List<SkillApproval> skillApprovals = skillApprovalRepo.findApprovalForSkillsDisplay(userId, queryProjId, querySkillRefId, oneRowPlease )
        SkillApproval skillApproval = skillApprovals?.size() > 0 ? skillApprovals.first() : null

        SelfReportingInfo selfReportingInfo = new SelfReportingInfo(
                approvalId: skillApproval?.getId(),
                enabled: enabled,
                type: skillDef.selfReportingType,
                justificationRequired: Boolean.valueOf(skillDef.justificationRequired),
                requestedOn: skillApproval?.requestedOn?.time,
                rejectedOn: skillApproval?.rejectedOn?.time,
                rejectionMsg: skillApproval?.rejectionMsg
        )

        return selfReportingInfo
    }

    @Transactional(readOnly = true)
    SkillSubjectSummary loadSubject(String projectId, String userId, String subjectId, Integer version = -1, Boolean loadSkills = true) {
        ProjDef projDef = getProjDef(userId, projectId)
        SkillDefWithExtra subjectDef = getSkillDefWithExtra(userId, projectId, subjectId, [SkillDef.ContainerType.Subject])

        if (version == -1 || subjectDef.version <= version) {
            return loadSubjectSummary(projDef, userId, subjectDef, version, loadSkills)
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
            List<SkillApprovalRepo.SkillApprovalPlusSkillId> approvals = skillApprovalRepo.findsApprovalWithSkillIdForSkillsDisplay(userId, projectId, subjectId, relationshipType)
            approvalLookup = approvals.groupBy { it.getSkillId() }
        } else {
            dbRes = skillDefWithExtraRepo.findAllGlobalChildSkillsDescriptions(subjectId, relationshipType, version, userId)
        }

        List<String> skillGroupIds = []
        List<SkillDescription> res = []
        dbRes.each {
            SkillApprovalRepo.SkillApprovalPlusSkillId skillApproval = approvalLookup?.get(it.getSkillId())?.sort({ it.skillApproval.requestedOn})?.reverse()?.get(0)
            if (it.type != SkillDef.ContainerType.SkillsGroup || Boolean.valueOf(it.enabled)) {
                SkillDescription skillDescription = createSkillDescription(it, helpUrlRootSetting, skillApproval)
                res << skillDescription
                if (it.type == SkillDef.ContainerType.SkillsGroup && Boolean.valueOf(it.enabled)) {
                    skillGroupIds << skillDescription.skillId
                }
            }
        }
        if (skillGroupIds) {
            dbRes = skillDefWithExtraRepo.findAllChildSkillsDescriptionsForSkillsGroups(projectId, skillGroupIds, SkillRelDef.RelationshipType.SkillsGroupRequirement, version, userId)
            List<SkillApprovalRepo.SkillApprovalPlusSkillId> approvals = skillApprovalRepo.findsApprovalWithSkillIdInForSkillsDisplay(userId, projectId, skillGroupIds)
            approvalLookup = approvals.groupBy { it.getSkillId() }
            dbRes.each {
                SkillApprovalRepo.SkillApprovalPlusSkillId skillApproval = approvalLookup?.get(it.getSkillId())?.sort({ it.skillApproval.requestedOn})?.reverse()?.get(0)
                SkillDescription skillDescription = createSkillDescription(it, helpUrlRootSetting, skillApproval)
                res << skillDescription
            }
        }
        return res
    }

    private SkillDescription createSkillDescription(SkillDefWithExtraRepo.SkillDescDBRes it, SettingsResult helpUrlRootSetting, SkillApprovalRepo.SkillApprovalPlusSkillId skillApproval) {
        SkillDescription skillDescription = new SkillDescription(
                skillId: it.getSkillId(),
                description: InputSanitizer.unsanitizeForMarkdown(it.getDescription()),
                href: getHelpUrl(helpUrlRootSetting, it.getHelpUrl()),
                achievedOn: it.getAchievedOn(),
                type: it.getType(),
                selfReporting: new SelfReportingInfo(
                        approvalId: skillApproval?.getSkillApproval()?.getId(),
                        type: it.getSelfReportingType(),
                        justificationRequired: Boolean.valueOf(it.justificationRequired),
                        enabled: it.getSelfReportingType() != null,
                        requestedOn: skillApproval?.getSkillApproval()?.getRequestedOn()?.time,
                        rejectedOn: skillApproval?.getSkillApproval()?.getRejectedOn()?.time,
                        rejectionMsg: skillApproval?.getSkillApproval()?.getRejectionMsg()
                )
        )
        skillDescription
    }

    @Transactional(readOnly = true)
    SkillBadgeSummary loadBadge(String projectId, String userId, String subjectId, Integer version = Integer.MAX_VALUE, boolean loadSkills=true) {
        ProjDef projDef = getProjDef(userId, projectId)
        SkillDefWithExtra badgeDef = getSkillDefWithExtra(userId, projectId, subjectId, [SkillDef.ContainerType.Badge])

        return loadBadgeSummary(projDef, userId, badgeDef, version,loadSkills)
    }


    @Transactional(readOnly = true)
    SkillGlobalBadgeSummary loadGlobalBadge(String userId, String originatingProject, String badgeSkillId, Integer version = Integer.MAX_VALUE, boolean loadSkills=true) {
        SkillDefWithExtra badgeDef = getSkillDefWithExtra(userId, null, badgeSkillId, [SkillDef.ContainerType.GlobalBadge])

        return loadGlobalBadgeSummary(userId, originatingProject, badgeDef, version,loadSkills)
    }

    @Transactional(readOnly = true)
    SkillDependencyInfo loadSkillDependencyInfo(String projectId, String userId, String skillId) {
        List<GraphRelWithAchievement> graphDBRes = nativeQueriesRepo.getDependencyGraphWithAchievedIndicator(projectId, skillId, userId)

        Map<String, Map<String,String>> subjectIdLookupByProjectIdThenBySkillId = [:]
        Map<String, List<Pair<String,String>>> byProjectIds = graphDBRes.collect { Pair.of(it.childProjectId, it.childSkillId)}.groupBy { it.first }
        byProjectIds.each {
            String projectIdForLookup = it.key
            List<String> skillIds = it.value.collect { it.second }
            List<SkillRelDefRepo.SkillToSubjIds> res = skillRelDefRepo.findAllSubjectIdsByChildSkillId(projectIdForLookup, skillIds)
            Map<String,String> subjectIdLookupBySkillId = [:]
            res.each {
                subjectIdLookupBySkillId[it.skillId] = it.subjectId
            }
            subjectIdLookupByProjectIdThenBySkillId[projectIdForLookup] = subjectIdLookupBySkillId
        }


        List<SkillDependencyInfo.SkillRelationshipItem> deps = graphDBRes.collect {
            Map<String,String> subjectIdLookup = subjectIdLookupByProjectIdThenBySkillId[it.childProjectId]
            String childSubjectId = subjectIdLookup[it.childSkillId]
            new SkillDependencyInfo.SkillRelationship(
                    skill: new SkillDependencyInfo.SkillRelationshipItem(projectId: it.parentProjectId, projectName: InputSanitizer.unsanitizeName(it.parentProjectName), skillId: it.parentSkillId, skillName: InputSanitizer.unsanitizeName(it.parentName)),
                    dependsOn: new SkillDependencyInfo.SkillRelationshipItem(projectId: it.childProjectId, subjectId: childSubjectId, projectName: InputSanitizer.unsanitizeName(it.childProjectName), skillId: it.childSkillId, skillName: InputSanitizer.unsanitizeName(it.childName)),
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
        List<SkillSummaryParent> skillsRes = []

        // must compute total points so the provided version is taken into account
        // subjectDefinition.totalPoints is total overall regardless of the version
        int totalPoints
        Integer points
        Integer todaysPoints
        if (loadSkills) {
            List<SkillRelDef.RelationshipType> relTypes = [
                    SkillRelDef.RelationshipType.RuleSetDefinition, // skills under subject
                    SkillRelDef.RelationshipType.SkillsGroupRequirement, // groups under subject
                    SkillRelDef.RelationshipType.GroupSkillToSubject, // skills under groups
            ]
            SubjectDataLoader.SkillsData groupChildrenMeta = subjectDataLoader.loadData(userId, projDef.projectId, subjectDefinition, version, relTypes)
            skillsRes = createSkillSummaries(projDef, groupChildrenMeta.childrenWithPoints, false, userId, version)
            totalPoints = skillsRes ? skillsRes.collect({it.totalPoints}).sum() as Integer : 0

        } else {
            totalPoints = calculateTotalForSubject(projDef, subjectDefinition, version)
        }

        // pick large enough version = version is not provide;
        // if version is provided then always calculate All points as versions are not respected for user points, at least as of now
        if (skillsRes && version >= 500) {
            points = skillsRes ? skillsRes.collect({ it.points }).sum() as Integer : 0
            todaysPoints = skillsRes ? skillsRes.collect({ it.todaysPoints }).sum() as Integer : 0
        } else {
            points = calculatePointsForSubject(projDef, userId, subjectDefinition)
            todaysPoints= calculateTodayPoints(userId, subjectDefinition)
        }



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
                subject: InputSanitizer.unsanitizeName(subjectDefinition.name),
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
    private int calculateTotalForSubject(ProjDef projDef, SkillDefParent subjectDefinition, int version) {
        Integer res = skillDefRepo.calculateTotalPointsForSubject(projDef.projectId, subjectDefinition.skillId, version)
        return res ?: 0
    }

    @Profile
    private List<UserAchievement> locateAchievedLevels(String userId, ProjDef projDef, SkillDefParent subjectDefinition) {
        achievedLevelRepository.findAllByUserIdAndProjectIdAndSkillId(userId, projDef.projectId, subjectDefinition.skillId)
    }

    @Profile
    private Integer calculateTodayPoints(String userId, SkillDefParent subjectDefinition) {
        Integer res = userPointsRepo.getParentsPointsForAGivenDay(userId, subjectDefinition.id, new Date().clearTime())
        return res ?: 0
    }

    @Profile
    private Integer calculatePointsForSubject(ProjDef projDef, String userId, SkillDefParent subjectDefinition) {
        Integer res = userPointsRepo.getPointsByProjectIdAndUserIdAndSkillRefId(projDef.projectId, userId, subjectDefinition.id)
        return res ?: 0
    }

    @Profile
    private Integer calculatePointsForProject(ProjDef projDef, String userId, int version) {
        Integer res = userPointsRepo.getPointsByProjectIdAndUserId(projDef.projectId, userId)
        return res ?: 0
    }

    @Profile
    private SkillBadgeSummary loadBadgeSummary(ProjDef projDef, String userId, SkillDefWithExtra badgeDefinition, Integer version = Integer.MAX_VALUE, boolean loadSkills = false, boolean loadProjectName = false) {
        List<SkillSummaryParent> skillsRes = []

        if (loadSkills) {
            SubjectDataLoader.SkillsData groupChildrenMeta = subjectDataLoader.loadData(userId, projDef?.projectId, badgeDefinition, version, [SkillRelDef.RelationshipType.BadgeRequirement])
            skillsRes = createSkillSummaries(projDef, groupChildrenMeta.childrenWithPoints, true)?.sort({ it.skill?.toLowerCase() })
        }

        String projectName = "";
        if (loadProjectName) {
            projectName = projDefRepo.findByProjectId(badgeDefinition.projectId)?.name
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
                badge: InputSanitizer.unsanitizeName(badgeDefinition.name),
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
                helpUrl: helpUrl,
                projectId: badgeDefinition.projectId,
                projectName: InputSanitizer.unsanitizeName(projectName)
        )
    }

    @Profile
    private SkillGlobalBadgeSummary loadGlobalBadgeSummary(String userId, String originatingProject, SkillDefWithExtra badgeDefinition, Integer version = Integer.MAX_VALUE, boolean loadSkills = false) {
        List<SkillSummaryParent> skillsRes = []

        if (loadSkills) {
            SubjectDataLoader.SkillsData groupChildrenMeta = subjectDataLoader.loadData(userId, null, badgeDefinition, version, [SkillRelDef.RelationshipType.BadgeRequirement])
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

        List<UserAchievement> achievedLevels = achievedLevelRepository.findAllProjectLevelsByUserId(userId)
        Map<String, Integer> userProjectLevels = (Map<String, Integer>)achievedLevels?.groupBy { it.projectId }
                ?.collectEntries {String key, List<UserAchievement> val -> [key,val.collect{it.level}.max()]}

        List<GlobalBadgeLevelRes> requiredLevels = globalBadgesService.getGlobalBadgeLevels(badgeDefinition.skillId)
        List<ProjectLevelSummary> projectLevels = []
        for (GlobalBadgeLevelRes requiredLevel : requiredLevels) {
            ProjectLevelSummary projectLevelSummary = new ProjectLevelSummary(projectId: requiredLevel.projectId, projectName: InputSanitizer.unsanitizeName(requiredLevel.projectName), requiredLevel: requiredLevel.level)
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
                badge: InputSanitizer.unsanitizeName(badgeDefinition.name),
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
    private List<SkillSummaryParent> createSkillSummaries(ProjDef thisProjDef, List<SubjectDataLoader.SkillsAndPoints> childrenWithPoints, boolean populateSubjectInfo=false) {
        return createSkillSummaries(thisProjDef, childrenWithPoints, populateSubjectInfo, null, null)
    }

    @Profile
    private List<SkillSummaryParent> createSkillSummaries(ProjDef thisProjDef, List<SubjectDataLoader.SkillsAndPoints> childrenWithPoints, boolean populateSubjectInfo, String userId, Integer version) {
        List<SkillSummaryParent> skillsRes = []

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

            String subjectName = "";
            String subjectId = "";
            if (populateSubjectInfo) {
                List<SkillRelDef> subject = skillRelDefRepo.findAllByChildAndType(skillDef, SkillRelDef.RelationshipType.RuleSetDefinition)
                if (subject) {
                    if (subject.size() == 1) {
                        SkillRelDef subj = subject.first()
                        subjectId = subj.parent.skillId
                        subjectName = InputSanitizer.unsanitizeName(subj.parent.name)
                    } else {
                        log.error("Skill [${skillDef.id}] has multiple SkillRelDef parents of type RuleSetDefinition")
                    }
                }
            }

            if (skillDef.type == SkillDef.ContainerType.SkillsGroup && Boolean.valueOf(skillDef.enabled)) {
                SkillsSummaryGroup skillsSummary = new SkillsSummaryGroup(
                        projectId: skillDef.projectId,
                        projectName: InputSanitizer.unsanitizeName(projDef.name),
                        skillId: skillDef.skillId,
                        skill: InputSanitizer.unsanitizeName(skillDef.name),
                        type: skillDef.type,
                        enabled: Boolean.valueOf(skillDef.enabled).toString(),
                        numSkillsRequired: skillDef.numSkillsRequired,
                        totalPoints: skillDef.totalPoints,
                        description: skillDefAndUserPoints.description ? new SkillDescription(
                                skillId: skillDef.skillId,
                                description: InputSanitizer.unsanitizeForMarkdown(skillDefAndUserPoints.description)
                        ) : null,
                )

                List<SubjectDataLoader.SkillsAndPoints> groupChildren = skillDefAndUserPoints.children
                Integer numSkillsRequired = skillDef.numSkillsRequired == - 1 ?  groupChildren.size() : skillDef.numSkillsRequired
                skillsSummary.children = createSkillSummaries(thisProjDef, groupChildren, false, userId, version)

                skillsSummary.points = skillsSummary.children ? skillsSummary.children.collect({it.points}).sort().takeRight(numSkillsRequired).sum() as Integer: 0
                skillsSummary.todaysPoints = skillsSummary.children ? skillsSummary.children.collect({it.todaysPoints}).sort().takeRight(numSkillsRequired).sum() as Integer: 0
                skillsRes << skillsSummary
            } else if (skillDef.type == SkillDef.ContainerType.Skill) {
                boolean isReusedSkill = SkillReuseIdUtil.isTagged(skillDef.skillId)
                String unsanitizedName = InputSanitizer.unsanitizeName(skillDef.name)
                skillsRes << new SkillSummary(
                        projectId: skillDef.projectId,
                        projectName: InputSanitizer.unsanitizeName(projDef.name),
                        skillId: skillDef.skillId,
                        skill: isReusedSkill ? SkillReuseIdUtil.removeTag(unsanitizedName) : unsanitizedName,
                        points: points,
                        todaysPoints: todayPoints,
                        pointIncrement: skillDef.pointIncrement,
                        pointIncrementInterval: skillDef.pointIncrementInterval,
                        maxOccurrencesWithinIncrementInterval: skillDef.numMaxOccurrencesIncrementInterval,
                        totalPoints: skillDef.totalPoints,
                        dependencyInfo: skillDefAndUserPoints.dependencyInfo,
                        selfReporting: skillDef.selfReportingType ? new SelfReportingInfo(enabled: true, type: skillDef.selfReportingType, justificationRequired: Boolean.valueOf(skillDef.justificationRequired)) : null,
                        subjectName: subjectName,
                        subjectId: subjectId,
                        type: skillDef.type,
                        copiedFromProjectId: !isReusedSkill ? skillDef.copiedFromProjectId : null,
                        copiedFromProjectName: !isReusedSkill ? InputSanitizer.unsanitizeName(skillDefAndUserPoints.copiedFromProjectName) : null,
                        isLastViewed: skillDefAndUserPoints.isLastViewed
                )
            }
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
            maxLevel = levelDefService.maxSubjectLevel(subjectDef)
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

    private SkillDefWithExtra getSkillDefWithExtra(String userId, String projectId, String skillId, List<SkillDef.ContainerType> containerTypes) {
        SkillDefWithExtra skillDef = skillDefWithExtraRepo.findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(projectId, skillId, containerTypes)

        if (!skillDef) {
            throw new SkillExceptionBuilder()
                    .msg("Skill definition with id [${skillId}] doesn't exist")
                    .userId(userId)
                    .projectId(projectId)
                    .skillId(skillId)
                    .build()
        }
        if (!skillDef.enabled?.equalsIgnoreCase("true")) {
            throw new SkillExceptionBuilder()
                    .msg("Skill with id [${skillId}] is not enabled")
                    .userId(userId)
                    .projectId(projectId)
                    .skillId(skillId)
                    .build()
        }
        return skillDef
    }

}
