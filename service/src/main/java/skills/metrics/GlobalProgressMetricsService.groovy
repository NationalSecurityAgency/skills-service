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
package skills.metrics

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.GlobalMetricsSettingsRequest
import skills.controller.request.model.SettingsRequest
import skills.controller.result.model.GlobalMetricsSettingsResult
import skills.controller.result.model.LabelCountItem
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.TableResult
import skills.controller.result.model.globalMetrics.*
import skills.services.admin.ProjAdminService
import skills.services.admin.UserCommunityService
import skills.services.quiz.QuizDefService
import skills.services.quiz.QuizSettingsService
import skills.services.settings.SettingsService
import skills.storage.model.*
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User
import skills.storage.model.auth.UserRole
import skills.storage.repos.*

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

@CompileStatic
@Service
@Slf4j
class GlobalProgressMetricsService {
    static final String USER_PREF_GLOBAL_METRICS_EXCLUSION = "globalMetricsExcludedItem"

    enum GroupingType {
        DAY('day'),
        WEEK('week'),
        MONTH('month')

        final String value

        GroupingType(String value) {
            this.value = value
        }

        static GroupingType fromValue(String value) {
            if (!value) return null
            return values().find { it.value.equalsIgnoreCase(value) }
        }
    }

    @Value('${skills.config.ui.usersTableAdditionalUserTagKey:}')
    String usersTableAdditionalUserTagKey

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    SettingsService settingsService

    @Autowired
    UserRepo userRepo

    @Autowired
    QuizSettingsService quizSettingsService

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    GlobalProgressMetricsRepo globalProgressMetricsRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    LevelDefRepo levelDefRepo

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    MetricsService metricsServiceNew

    @Autowired
    QuizDefService quizDefService

    @Autowired
    UserCommunityService userCommunityService

    @Profile
    UsersOverallProgressResult loadUsersOverallProgress(String userQuery, String userTagValueFilter, PageRequest pageRequest) {
        ProjectIdsAndQuizIds projectIdsAndQuizIds = getProjectIdsAndQuizIdsForCurrentUser()

        userQuery = userQuery ? userQuery?.trim() : ''
        userTagValueFilter = userTagValueFilter ? userTagValueFilter?.trim() : ''

        List<GlobalProgressMetricsRepo.UserProgressMetric> userProgressMetricPage = globalProgressMetricsRepo.findUsersOverallProgress(
                projectIdsAndQuizIds.projectIds,
                projectIdsAndQuizIds.quizIds,
                userQuery,
                usersTableAdditionalUserTagKey ?: '',
                userTagValueFilter,
                pageRequest)

        boolean isFirstAndSmallerThanPageSize = userProgressMetricPage.size() < pageRequest.pageSize && pageRequest.pageNumber == 0
        Long numTotalMetricItems = isFirstAndSmallerThanPageSize ? userProgressMetricPage.size()
                : globalProgressMetricsRepo.countUsersOverallProgress(
                projectIdsAndQuizIds.projectIds,
                projectIdsAndQuizIds.quizIds,
                userQuery,
                usersTableAdditionalUserTagKey ?: '',
                userTagValueFilter)

        List<GlobalMetricsUserItem> metricItemsPage = userProgressMetricPage.collect {
            new GlobalMetricsUserItem(
                    userId: it.userId,
                    userIdForDisplay: it.userIdForDisplay,
                    numProjects: it.numProjects,
                    numProjectLevelsEarned: it.projectLevelsEarned,
                    numSubjectLevelsEarned: it.subjectLevelsEarned,
                    numSkillsEarned: it.numSkillsEarned,
                    numBadgesEarned: it.numBadgesEarned,
                    numGlobalBadgesEarned: it.globalBadgesEarned,
                    numQuizAttempts: it.numQuizAttempts,
                    numQuizzesPassed: it.numQuizzesPassed,
                    numQuizzesFailed: it.numQuizzesFailed,
                    numQuizzesInProgress: it.numQuizzesInProgress,
                    numQuizzesNeedsGrading: it.numQuizzesNeedsGrading,
                    numSurveys: it.numSurveys,
                    numSurveysCompleted: it.numSurveysCompleted,
                    numSurveysInProgress: it.numSurveysInProgress,
                    userTag: it.userTag ?: ''
            )
        }

        UsersOverallProgressResult res = populateGlobalMetricsResult(projectIdsAndQuizIds, new UsersOverallProgressResult(
                numTotalMetricItems: numTotalMetricItems,
                metricItemsPage: metricItemsPage
        ))
        return res
    }

    @Profile
    private <T extends GlobalMetricsResult> T populateGlobalMetricsResult(ProjectIdsAndQuizIds projectIdsAndQuizIds, T globalMetricsResult) {
        GlobalProgressMetricsRepo.ProjDefCounts projDefCounts = globalProgressMetricsRepo.findProjectDefCounts(projectIdsAndQuizIds.projectIds)
        Integer totalGlobalBadgeCount = globalProgressMetricsRepo.getTotalGlobalBadgeCountForProjects(projectIdsAndQuizIds.projectIds)
        int numTotalProjectBadges = projDefCounts?.numBadges ?: 0
        List<GlobalProgressMetricsRepo.QuizInfo> quizIdAndTypes = globalProgressMetricsRepo.getQuizInfo(projectIdsAndQuizIds.quizIds)
        globalMetricsResult.numTotalProjects = projectIdsAndQuizIds.projectIds.size()
        globalMetricsResult.numExcludedProjects = projectIdsAndQuizIds.excludedProjectIds.size()
        globalMetricsResult.numTotalSkills = projDefCounts?.numSkills ?: 0
        globalMetricsResult.numTotalBadges = (numTotalProjectBadges + totalGlobalBadgeCount)
        globalMetricsResult.numTotalProjectBadges = numTotalProjectBadges
        globalMetricsResult.numTotalGlobalBadges = totalGlobalBadgeCount
        globalMetricsResult.numTotalQuizzes = countQuizzesByType(quizIdAndTypes, QuizDefParent.QuizType.Quiz)
        globalMetricsResult.numTotalSurveys = countQuizzesByType(quizIdAndTypes, QuizDefParent.QuizType.Survey)
        globalMetricsResult.numExcludedQuizzesAndSurveys = projectIdsAndQuizIds.excludedQuizIds.size()

        return globalMetricsResult
    }

    @Profile
    SingleUserOverallProgress loadSingleUserProgress(String userId) {
        ProjectIdsAndQuizIds projectIdsAndQuizIds = getProjectIdsAndQuizIdsForCurrentUser()
        List<String> projectIds = projectIdsAndQuizIds.projectIds

        List<UserPoints> projectUserPoints = userPointsRepo.findByUserIdAndProjectIdInAndSkillRefIdIsNull(userId, projectIds)
        List<String> projectsThatUserHasProgressIn = projectUserPoints.collect { it.projectId }.unique()

        List<ProjSummaryResult> projDefInfoList = projDefRepo.getAllSummariesByProjectIdIn(projectsThatUserHasProgressIn)
        Map<String, ProjSummaryResult> projDefInfoMap = projDefInfoList.collectEntries { [it.projectId, it] }

        List<LevelDefRepo.ProjectLevelCount> projectLevelCountList = levelDefRepo.countNumLevelsForProjects(projectsThatUserHasProgressIn)
        Map<String, LevelDefRepo.ProjectLevelCount> projectLevelCountMap = projectLevelCountList.collectEntries { [it.projectId, it] }

        List<GlobalProgressMetricsRepo.SingleUserAchievement> achievements = globalProgressMetricsRepo.findSingleUserAchievements(userId, projectsThatUserHasProgressIn)
        Map<String, GlobalProgressMetricsRepo.SingleUserAchievement> achievementsByProject = achievements.collectEntries { [it.projectId, it] }

        List<GlobalProgressMetricsRepo.SingleUserAchievedBadgeCounts> badgeCounts = globalProgressMetricsRepo.findSingleUserAchievedBadgeCounts(userId, projectsThatUserHasProgressIn)
        Map<String, GlobalProgressMetricsRepo.SingleUserAchievedBadgeCounts> badgeCountsByProject = badgeCounts.collectEntries { [it.projectId, it] }

        List<SingleUserOverallProgress.ProjectProgress> projectsProgressRes = projectUserPoints.collect { UserPoints userPoints->
            GlobalProgressMetricsRepo.SingleUserAchievement projAchievements = achievementsByProject[userPoints.projectId]
            GlobalProgressMetricsRepo.SingleUserAchievedBadgeCounts badgeCount = badgeCountsByProject[userPoints.projectId]
            ProjSummaryResult projDefInfo = projDefInfoMap[userPoints.projectId]
            LevelDefRepo.ProjectLevelCount levelCount = projectLevelCountMap[userPoints.projectId]
            new SingleUserOverallProgress.ProjectProgress(
                    projectId: userPoints.projectId,
                    projectName: projDefInfo.name,
                    numSkills: projDefInfo.numSkills,
                    projectTotalPoints: projDefInfo.totalPoints,
                    numBadges: projDefInfo.numBadges,
                    points: userPoints.points,
                    updated: userPoints.updated,
                    numProjectLevels: levelCount.numberLevels,
                    numAchievedSkills: projAchievements?.numAchievedSkills ?: 0,
                    numAchievedBadges: badgeCount?.numAchievedBadges ?: 0,
                    achievedProjLevel: projAchievements?.achievedProjLevel ?: 0,
            )
        }
        return new SingleUserOverallProgress(projectsProgress: projectsProgressRes)
    }

    @Profile
    TableResult getQuizRuns( String userQuery, String userIdFilter, String nameQuery, UserQuizAttempt.QuizAttemptStatus quizAttemptStatus, PageRequest pageRequest, Date startDate, Date endDate) {
        ProjectIdsAndQuizIds projectIdsAndQuizIds = getProjectIdsAndQuizIdsForCurrentUser()
        List<String> quizIds = projectIdsAndQuizIds.quizIds
        return quizDefService.getQuizRuns(quizIds, userQuery, userIdFilter, nameQuery, quizAttemptStatus, pageRequest, startDate, endDate);
    }

    @Profile
    OverallMetricsResult loadOverallMetrics() {
        ProjectIdsAndQuizIds projectIdsAndQuizIds = getProjectIdsAndQuizIdsForCurrentUser()

        List<GlobalProgressMetricsRepo.QuizInfo> quizIdAndTypes = globalProgressMetricsRepo.getQuizInfo(projectIdsAndQuizIds.quizIds)
        List<GlobalProgressMetricsRepo.ProjectInfo> projectInfo = globalProgressMetricsRepo.getProjectInfo(projectIdsAndQuizIds.projectIds)

        OverallMetricsResult res = populateGlobalMetricsResult(projectIdsAndQuizIds, new OverallMetricsResult(
                projectInfo: projectInfo,
                quizInfo: quizIdAndTypes
        ))
        return res
    }

    @Transactional
    @Profile
    List<DayCountItem> getDistinctUserCountForProjectsAndQuizzes(Date startDate, GroupingType groupingType) {
        ProjectIdsAndQuizIds projectIdsAndQuizIds = getProjectIdsAndQuizIdsForCurrentUser()
        List<String> projectIds = projectIdsAndQuizIds.projectIds
        List<String> quizIds = projectIdsAndQuizIds.quizIds
        String userId = userInfoService.getCurrentUserId()
        log.debug("Retrieving event counts for user [{}], start date [{}], projectIds [{}], quizIds [{}]", userId, startDate, projectIds, quizIds)
        List<DayCountItem> counts = globalProgressMetricsRepo.getDistinctUserCountForProjectsAndQuizzes(projectIds, quizIds, startDate, groupingType.value)

        // Fill in gaps with zero counts
        List<DayCountItem> filledCounts = fillGapsWithZeroCounts(counts, startDate, groupingType)
        return filledCounts
    }

    @Profile
    UsersPerTagRes getUserCountsForTag(String tagKey, Date startDate, Date endDate, int currentPage, int pageSize, Boolean sortDesc, String sortBy, String tagFilter) {
        ProjectIdsAndQuizIds projectIdsAndQuizIds = getProjectIdsAndQuizIdsForCurrentUser()
        List<String> projectIds = projectIdsAndQuizIds.projectIds
        List<String> quizIds = projectIdsAndQuizIds.quizIds
        String userId = userInfoService.getCurrentUserId()
        PageRequest pageRequest = PageRequest.of(currentPage, pageSize, sortDesc ? DESC : ASC, sortBy)
        log.debug("Retrieving user tag counts for user [{}], start date [{}], projectIds [{}], quizIds [{}]", userId, startDate, projectIds, quizIds)
        List<GlobalProgressMetricsRepo.UserTagCount> userTagCounts = globalProgressMetricsRepo.findUserTagCountByProjectIdInAndUserTagFilter(projectIds, quizIds, tagKey, tagFilter ?: null, startDate, endDate, pageRequest)
        Integer numDistinctTags = (pageSize > userTagCounts.size() && currentPage == 0) ? userTagCounts.size() : globalProgressMetricsRepo.countUserTagCountByProjectIdInAndUserTagFilter(projectIds, quizIds, tagKey, tagFilter ?: null, startDate, endDate)

        List<LabelCountItem> items = userTagCounts.collect {
            new LabelCountItem(value: it.tag, count: it.numUsers)
        }
        return new UsersPerTagRes(totalNumItems: numDistinctTags, items: items)
    }

    @Profile
    private static List<DayCountItem> fillGapsWithZeroCounts(List<DayCountItem> existingCounts, Date startDate, GroupingType groupingType) {
        // Sort existing counts by date
        existingCounts.sort { a, b -> a.day <=> b.day }

        Date endDate = new Date()
        List<DayCountItem> result = []

        // Generate all expected dates based on grouping type
        List<Date> expectedDates = generateExpectedDates(startDate, endDate, groupingType)

        // Create a map of existing counts by date for quick lookup
        Map<Date, Long> existingCountsByDate = existingCounts.collectEntries { [(it.day): it.count] }

        // Create DayCountItems for all expected dates
        for (Date expectedDate : expectedDates) {
            Long count = existingCountsByDate.get(expectedDate, 0L)
            result.add(new DayCount(expectedDate, count))
        }

        return result
    }

    @Profile
    private static List<Date> generateExpectedDates(Date startDate, Date endDate, GroupingType groupingType) {
        List<Date> dates = []
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(startDate)

        switch (groupingType) {
            case GroupingType.DAY:
                while (calendar.getTime() <= endDate) {
                    // Set to beginning of day
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    dates.add(calendar.getTime())
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                break

            case GroupingType.WEEK:
                // Find the Sunday of the week containing startDate
                while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                }

                // Set to beginning of Sunday
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                while (calendar.getTime() <= endDate) {
                    dates.add(calendar.getTime())
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }
                break

            case GroupingType.MONTH:
                // Set to first day of month for startDate
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                while (calendar.getTime() <= endDate) {
                    dates.add(calendar.getTime())
                    calendar.add(Calendar.MONTH, 1)
                }
                break

            default:
                throw new IllegalArgumentException("Unsupported grouping type: ${groupingType?.value}")
        }

        return dates
    }

    @Profile
    ProjectIdsAndQuizIds getProjectIdsAndQuizIdsForCurrentUser() {
        String userId = userInfoService.currentUserId

        ProjectsAndQuizzes projectsAndQuizzes = getCurrentUserAdminProjectsAndQuizzes()
        List<String> projectIds = projectsAndQuizzes.projectIds
        List<String> quizIds = projectsAndQuizzes.quizIds

        Boolean isUserCommunityMember = userCommunityService.isUserCommunityMember(userId)
        if (!isUserCommunityMember && projectIds) {
            projectIds.removeAll { userCommunityService.isUserCommunityOnlyProject(it) }
        }
        if (!isUserCommunityMember && quizIds) {
            quizIds.removeAll { userCommunityService.isUserCommunityOnlyQuiz(it) }
        }


        List<GlobalMetricsSettingsResult> exclusionSettings = getGlobalMetricsUserSettings(USER_PREF_GLOBAL_METRICS_EXCLUSION)
        List<String> excludedProjectIds = exclusionSettings ? exclusionSettings.findAll { it.projectId }.collect { it.projectId } : []
        if (excludedProjectIds) {
            projectIds.removeAll { excludedProjectIds.contains(it) }
        }

        List<String> excludedQuizIds = exclusionSettings ? exclusionSettings.findAll { it.quizId }.collect { it.quizId } : []
        if (excludedQuizIds) {
            quizIds.removeAll { excludedQuizIds.contains(it) }
        }

        return new ProjectIdsAndQuizIds(projectIds: projectIds, quizIds: quizIds, excludedProjectIds: excludedProjectIds, excludedQuizIds: excludedQuizIds)
    }

    List<GlobalMetricsSettingsResult> getGlobalMetricsUserSettings(String setting) {
        UserInfo currentUser = userInfoService.currentUser
        ProjectsAndQuizzes projectsAndQuizzes = getCurrentUserAdminProjectsAndQuizzes()

        List<GlobalMetricsSettingsResult> res = []
        List<SettingsResult> settingsResults = settingsService.getUserProjectSettingsForAllProjects(setting, currentUser.username)
        if (settingsResults) {
            settingsResults.findAll { it.projectId in projectsAndQuizzes.projectIds }.each {
                res.add(new GlobalMetricsSettingsResult(
                        projectId: it.projectId,
                        setting: it.setting,
                        value: it.value
                ))
            }
        }

        List<QuizSettingsRepo.SimpleQuizRes> quizSettings = quizSettingsService.getGlobalMetricsUserSettings(setting)
        if (quizSettings) {
            quizSettings.findAll { it.quizId in projectsAndQuizzes.quizIds }.each {
                res.add(new GlobalMetricsSettingsResult(
                        quizId: it.quizId,
                        setting: it.setting,
                        value: it.value
                ))
            }
        }

        return res
    }

    void saveGlobalMetricsUserSettings(List<GlobalMetricsSettingsRequest> values) {
        SkillsValidator.isNotNull(values, "Settings")

        if (!userInfoService.isCurrentUserASuperDuperUser()) {
            ProjectsAndQuizzes projectsAndQuizzes = getCurrentUserAdminProjectsAndQuizzes()
            values.each {
                SkillsValidator.isTrue((it.projectId && !it.quizId) || (!it.projectId && it.quizId), "Exactly one of projectId or quizId must be specified (not both, not neither)")
                if (it.projectId) {
                    SkillsValidator.isTrue(it.projectId in projectsAndQuizzes.projectIds, "User must be an admin in order to save settings for this project", it.projectId)
                }
                if (it.quizId) {
                    QuizValidator.isTrue(it.quizId in projectsAndQuizzes.quizIds, "User must be an admin in order to save settings for this quiz", it.quizId)
                }
            }
        }

        List<GlobalMetricsSettingsRequest> projSettings = values.findAll { it.projectId }

        List<GlobalMetricsSettingsRequest> projSettingToDelete = projSettings.findAll { StringUtils.isBlank(it.value)}
        if (projSettingToDelete) {
            settingsService.deleteGlobalMetricsUserSettings(projSettingToDelete)
        }

        List<? extends SettingsRequest> projSettingsToSave = projSettings.findAll { !StringUtils.isBlank(it.value)}
        if (projSettingsToSave) {
            String userId = userInfoService.currentUser.username
            User user = userRepo.findByUserId(userId.toLowerCase())
            settingsService.saveSettings(projSettingsToSave as List<SettingsRequest>, user, false)
        }

        List<GlobalMetricsSettingsRequest> quizSettings = values.findAll { it.quizId }
        quizSettingsService.updateGlobalMetricsUserSettings(quizSettings)
    }

    static class ProjectsAndQuizzes {
        List<String> projectIds
        List<String> quizIds
    }

    private ProjectsAndQuizzes getCurrentUserAdminProjectsAndQuizzes() {
        String userId = userInfoService.currentUser.username
        boolean isRoot = userInfoService.isCurrentUserASuperDuperUser()

        List<UserRole> allUserRoles = userRoleRepo.findAllByUserId(userId)

        List<String> projectIds = isRoot ?
                settingsService.getUserProjectSettingsForGroup(userId, ProjAdminService.rootUserPinnedProjectGroup)?.collect { it.projectId }
                : allUserRoles?.findAll({ it.roleName == RoleName.ROLE_PROJECT_ADMIN})?.projectId?.unique()
        List<String> quizIds = allUserRoles?.findAll({ it.roleName == RoleName.ROLE_QUIZ_ADMIN})?.quizId?.unique()

        return new ProjectsAndQuizzes(projectIds: projectIds, quizIds: quizIds)
    }

    private static int countQuizzesByType(List<GlobalProgressMetricsRepo.QuizInfo> quizIdAndTypes, QuizDefParent.QuizType quizType) {
        return quizIdAndTypes?.findAll { it.quizType == quizType }?.size() ?: 0
    }

    static final class ProjectIdsAndQuizIds {
        List<String> projectIds
        List<String> quizIds
        List<String> excludedProjectIds
        List<String> excludedQuizIds
    }

    static class UsersPerTagRes {
        Integer totalNumItems
        List<LabelCountItem> items
    }
}
