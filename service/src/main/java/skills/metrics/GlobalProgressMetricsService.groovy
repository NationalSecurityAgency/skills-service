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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.result.model.TableResult
import skills.controller.result.model.globalMetrics.GlobalMetricsUserItem
import skills.controller.result.model.globalMetrics.OverallMetricsResult
import skills.controller.result.model.globalMetrics.SingleUserOverallProgress
import skills.controller.result.model.globalMetrics.UsersOverallProgressResult
import skills.services.quiz.QuizDefService
import skills.storage.model.*
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.UserRole
import skills.storage.repos.*

import java.util.stream.Collectors
import java.util.stream.Stream

@CompileStatic
@Service
@Slf4j
class GlobalProgressMetricsService {

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

    UsersOverallProgressResult loadUsersOverallProgress(String userQuery, String userTagValueFilter, PageRequest pageRequest) {
        ProjectIdsAndQuizIds projectIdsAndQuizIds = getProjectIdsAndQuizIdsForCurrentUser()
        List<String> projectIds = projectIdsAndQuizIds.projectIds
        List<String> quizIds = projectIdsAndQuizIds.quizIds

        List<GlobalProgressMetricsRepo.UserProgressMetric> userProgressMetricPage = globalProgressMetricsRepo.findUsersOverallProgress(
                projectIds,
                quizIds,
                userQuery ?: '',
                usersTableAdditionalUserTagKey ?: '',
                userTagValueFilter ?: '',
                pageRequest)

        boolean isFirstAndSmallerThanPageSize = userProgressMetricPage.size() < pageRequest.pageSize && pageRequest.pageNumber == 0
        Long numTotalMetricItems = isFirstAndSmallerThanPageSize ? userProgressMetricPage.size()
                : globalProgressMetricsRepo.countUsersOverallProgress(
                projectIds,
                quizIds,
                userQuery ?: '',
                usersTableAdditionalUserTagKey ?: '',
                userTagValueFilter ?: '')

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
                    numSurveys: it.numSurveys,
                    numSurveysCompleted: it.numSurveyCompleted,
                    numSurveysInProgress: it.numSurveyInProgress,
                    userTag: it.userTag ?: ''
            )
        }

        GlobalProgressMetricsRepo.ProjDefCounts projDefCounts = globalProgressMetricsRepo.findProjectDefCounts(projectIds)
        Integer totalGlobalBadgeCount = globalProgressMetricsRepo.getTotalGlobalBadgeCountForProjects(projectIds)
        List<GlobalProgressMetricsRepo.QuizInfo> quizIdAndTypes = globalProgressMetricsRepo.getQuizInfo(quizIds)
        return new UsersOverallProgressResult(
                numTotalProjects: projectIds.size(),
                numTotalSkills: projDefCounts?.numSkills ?: 0,
                numTotalBadges: projDefCounts?.numBadges ?: 0,
                numTotalGlobalBadges: totalGlobalBadgeCount,
                numTotalQuizzes: countQuizzesByType(quizIdAndTypes, QuizDefParent.QuizType.Quiz),
                numTotalSurveys: countQuizzesByType(quizIdAndTypes, QuizDefParent.QuizType.Survey),
                numTotalMetricItems: numTotalMetricItems,
                metricItemsPage: metricItemsPage
        )
    }

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

    TableResult getQuizRuns( String userQuery, String nameQuery, UserQuizAttempt.QuizAttemptStatus quizAttemptStatus, PageRequest pageRequest, Date startDate, Date endDate) {
        ProjectIdsAndQuizIds projectIdsAndQuizIds = getProjectIdsAndQuizIdsForCurrentUser()
        List<String> quizIds = projectIdsAndQuizIds.quizIds
        return quizDefService.getQuizRuns(quizIds, userQuery, nameQuery, quizAttemptStatus, pageRequest, startDate, endDate);
    }

    OverallMetricsResult loadOverallMetrics(List<String> selectedProjectIds, List<String> selectedQuizIds) {
        ProjectIdsAndQuizIds projectIdsAndQuizIds = getProjectIdsAndQuizIdsForCurrentUser()
        List<String> projectIds = projectIdsAndQuizIds.projectIds
        List<String> quizIds = projectIdsAndQuizIds.quizIds
//        selectedProjectIds = selectedProjectIds ? projectIdsAndQuizIds.projectIds.intersect(selectedProjectIds) : projectIdsAndQuizIds.projectIds
//        selectedQuizIds = selectedQuizIds ? projectIdsAndQuizIds.quizIds.intersect(selectedQuizIds) : projectIdsAndQuizIds.quizIds

        GlobalProgressMetricsRepo.ProjDefCounts projDefCounts = globalProgressMetricsRepo.findProjectDefCounts(projectIds)
        Integer totalGlobalBadgeCount = globalProgressMetricsRepo.getTotalGlobalBadgeCountForProjects(projectIds)
        List<GlobalProgressMetricsRepo.QuizInfo> quizIdAndTypes = globalProgressMetricsRepo.getQuizInfo(quizIds)
        List<GlobalProgressMetricsRepo.ProjectInfo> projectInfo = globalProgressMetricsRepo.getProjectInfo(projectIds)

        return new OverallMetricsResult(
                numTotalProjects: projectIds.size(),
                numTotalSkills: projDefCounts?.numSkills ?: 0,
                numTotalBadges: projDefCounts?.numBadges ?: 0,
                numTotalGlobalBadges: totalGlobalBadgeCount,
                numTotalQuizzes: countQuizzesByType(quizIdAndTypes, QuizDefParent.QuizType.Quiz),
                numTotalSurveys: countQuizzesByType(quizIdAndTypes, QuizDefParent.QuizType.Survey),
                projectInfo: projectInfo,
                quizInfo: quizIdAndTypes
        )
    }

    @Transactional
    List<DayCountItem> getDistinctUserCountForProjectsAndQuizzes(List<String> projectIds, List<String> quizIds, Date startDate, GroupingType groupingType) {
        Stream<DayCountItem> stream = globalProgressMetricsRepo.getDistinctUserCountForProjectsAndQuizzes(projectIds, quizIds, startDate, groupingType.value)
        List<DayCountItem> counts = stream.collect(Collectors.toList())
        stream.close()

        // Fill in gaps with zero counts
        List<DayCountItem> filledCounts = fillGapsWithZeroCounts(counts, startDate, groupingType)
        return filledCounts
    }

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

    private ProjectIdsAndQuizIds getProjectIdsAndQuizIdsForCurrentUser() {
        String userId = userInfoService.currentUser.username
        List<UserRole> allUserRoles = userRoleRepo.findAllByUserId(userId)
        List<String> projectIds = allUserRoles?.findAll({ it.roleName == RoleName.ROLE_PROJECT_ADMIN})?.projectId
        List<String> quizIds = allUserRoles?.findAll({ it.roleName == RoleName.ROLE_QUIZ_ADMIN})?.quizId
        return new ProjectIdsAndQuizIds(projectIds: projectIds, quizIds: quizIds)
    }

    private static int countQuizzesByType(List<GlobalProgressMetricsRepo.QuizInfo> quizIdAndTypes, QuizDefParent.QuizType quizType) {
        return quizIdAndTypes?.findAll { it.quizType == quizType }?.size() ?: 0
    }

    static final class ProjectIdsAndQuizIds {
        List<String> projectIds
        List<String> quizIds
    }
}
