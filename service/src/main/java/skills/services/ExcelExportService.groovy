/**
 * Copyright 2024 SkillTree
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

import groovy.util.logging.Slf4j
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.GlobalBadgeLevelRes
import skills.controller.result.model.GlobalBadgeUser
import skills.controller.result.model.ProjectUser
import skills.controller.result.model.SkillDefPartialRes
import skills.controller.result.model.UserProgressExportResult
import skills.metrics.GlobalProgressMetricsService
import skills.metrics.builders.project.UserAchievementsMetricsBuilder
import skills.metrics.builders.project.UserAchievementsMetricsBuilder.QueryParams
import skills.services.admin.SkillsAdminService
import skills.services.admin.UserCommunityService
import skills.services.admin.skillReuse.SkillReuseIdUtil
import skills.services.attributes.ExpirationAttrs
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.repos.GlobalProgressMetricsRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserQuizAttemptRepo
import skills.utils.InputSanitizer

import java.util.stream.Stream

@Service
@Slf4j
class ExcelExportService {

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    UserProgressExportResult userProgressExportResult

    @Value('${skills.config.ui.usersTableAdditionalUserTagKey:}')
    String usersTableAdditionalUserTagKey

    @Value('${skills.config.ui.usersTableAdditionalUserTagLabel:}')
    String userTagLabel

    @Value('${skills.config.ui.exportHeaderAndFooter:}')
    String exportHeaderAndFooter

    @Autowired
    private AdminUsersService adminUsersService

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    GlobalProgressMetricsService globalProgressMetricsService

    @Autowired
    GlobalProgressMetricsRepo globalProgressMetricsRepo

    @Autowired
    UserQuizAttemptRepo userQuizAttemptRepo

    @Autowired
    GlobalBadgesService globalBadgesService

    @Transactional(readOnly = true)
    void exportUsersProgress(Workbook workbook, String projectId, String query, PageRequest pageRequest, int minimumPointsPercent, int maximumPointsPercent, String userTagFilter, boolean includeImported) {
        String projectExportHeaderAndFooter = userCommunityService.replaceProjectDescriptorVar(exportHeaderAndFooter, userCommunityService.getProjectUserCommunity(projectId))
        Sheet sheet = workbook.createSheet()
        List<String> headers
        if (userTagLabel) {
            headers = ["User ID", "Last Name", "First Name", userTagLabel, "Level", "Current Points", "Percent Complete", "Points First Earned (UTC)", "Points Last Earned (UTC)"]
        } else {
            headers = ["User ID", "Last Name", "First Name", "Level", "Current Points", "Percent Complete", "Points First Earned (UTC)", "Points Last Earned (UTC)"]
        }
        Integer columnNumber = 0
        Integer rowNum = initializeSheet(sheet, headers, projectExportHeaderAndFooter)

        CellStyle dateStyle = workbook.createCellStyle()
        dateStyle.setDataFormat((short) 22) // NOTE: 14 = "mm/dd/yyyy"

        CellStyle percentStyle = workbook.createCellStyle()
        percentStyle.setDataFormat((short) 9)

        Cell cell = null

        Integer projectPoints = projDefRepo.getTotalPointsByProjectId(projectId) ?: 0
        int minimumPoints = Math.floor((minimumPointsPercent / 100) * projectPoints)
        int maximumPoints = Math.ceil((maximumPointsPercent / 100) * projectPoints)
        // need to artificially increase the maximum points to include users who have reached 100% completion
        // because the database query uses "less than" logic
        if (maximumPointsPercent == 100) {
            maximumPoints = maximumPoints + 1
        }
        Stream<ProjectUser> projectUsers = adminUsersService.streamAllDistinctUsersForProject(projectId, query, pageRequest, minimumPoints, maximumPoints, userTagFilter, includeImported)
        try {
            projectUsers.each { ProjectUser user ->
                columnNumber = 0
                Row row = sheet.createRow(rowNum++)
                row.createCell(columnNumber++).setCellValue(user.userIdForDisplay)
                row.createCell(columnNumber++).setCellValue(user.lastName ?: '')
                row.createCell(columnNumber++).setCellValue(user.firstName ?: '')
                if (userTagLabel) {
                    row.createCell(columnNumber++).setCellValue(user.userTag)
                }
                row.createCell(columnNumber++).setCellValue(user.userMaxLevel)
                row.createCell(columnNumber++).setCellValue(user.totalPoints)
                Double percentage = (user.totalPoints / projectPoints)
                cell = row.createCell(columnNumber++)
                cell.setCellStyle(percentStyle)
                cell.setCellValue(percentage)

                cell = row.createCell(columnNumber++)
                cell.setCellStyle(dateStyle)
                cell.setCellValue(user.firstUpdated)
                cell = row.createCell(columnNumber++)
                cell.setCellStyle(dateStyle)
                cell.setCellValue(user.lastUpdated)
            }
        } finally {
            projectUsers.close()
        }

        if (projectExportHeaderAndFooter) {
            addDataHeaderOrFooter(sheet, rowNum++, headers.size(), projectExportHeaderAndFooter)
        }
    }

    @Transactional(readOnly = true)
    void exportGlobalBadgeUserProgress(Workbook workbook, String badgeId, Integer id, String query, PageRequest pageRequest, String userTagFilter) {
        String projectExportHeaderAndFooter = userCommunityService.replaceProjectDescriptorVar(exportHeaderAndFooter, userCommunityService.getGlobalBadgeUserCommunity(id))
        Sheet sheet = workbook.createSheet()
        List<String> headers
        if (userTagLabel) {
            headers = ["User ID", "Last Name", "First Name", userTagLabel, "Skills Achieved", "Levels Achieved", "Percent Complete", "Skill Last Earned (UTC)"]
        } else {
            headers = ["User ID", "Last Name", "First Name", "Skills Achieved", "Levels Achieved", "Percent Complete", "Skill Last Earned (UTC)"]
        }
        Integer columnNumber = 0
        Integer rowNum = initializeSheet(sheet, headers, projectExportHeaderAndFooter)

        CellStyle dateStyle = workbook.createCellStyle()
        dateStyle.setDataFormat((short) 22) // NOTE: 14 = "mm/dd/yyyy"

        CellStyle percentStyle = workbook.createCellStyle()
        percentStyle.setDataFormat((short) 9)

        Cell cell = null

        Integer numberOfSkills = globalBadgesService.countSkillsForBadge(badgeId)
        List<GlobalBadgeLevelRes> levels = globalBadgesService.getGlobalBadgeLevels(badgeId)
        Integer totalLevels = levels.sum(0) { level -> level.level } as Integer
        Stream<GlobalBadgeUser> badgeUsers = adminUsersService.streamAllDistinctUsersForGlobalBadge(badgeId, query, pageRequest, userTagFilter)

        try {
            badgeUsers.each { GlobalBadgeUser user ->
                columnNumber = 0
                Row row = sheet.createRow(rowNum++)
                row.createCell(columnNumber++).setCellValue(user.userIdForDisplay)
                row.createCell(columnNumber++).setCellValue(user.lastName ?: '')
                row.createCell(columnNumber++).setCellValue(user.firstName ?: '')
                if (userTagLabel) {
                    row.createCell(columnNumber++).setCellValue(user.userTag)
                }
                row.createCell(columnNumber++).setCellValue(user.skillsAchieved)
                row.createCell(columnNumber++).setCellValue(user.numLevelsAchieved)
                Double percentage = (user.totalProgress / (numberOfSkills + totalLevels))
                cell = row.createCell(columnNumber++)
                cell.setCellStyle(percentStyle)
                cell.setCellValue(percentage)

                cell = row.createCell(columnNumber++)
                cell.setCellStyle(dateStyle)
                cell.setCellValue(user.lastUpdated)
            }
        } finally {
            badgeUsers.close()
        }

        if (projectExportHeaderAndFooter) {
            addDataHeaderOrFooter(sheet, rowNum++, headers.size(), projectExportHeaderAndFooter)
        }
    }


    @Transactional(readOnly = true)
    void exportUsersAchievements(Workbook workbook, String projectId, QueryParams queryParams) {
        String projectExportHeaderAndFooter = userCommunityService.replaceProjectDescriptorVar(exportHeaderAndFooter, userCommunityService.getProjectUserCommunity(projectId))
        Sheet sheet = workbook.createSheet()
        List<String> headers
        if (userTagLabel) {
            headers = ["User ID", "Last Name", "First Name", userTagLabel, "Achievement Type", "Achievement Name", "Level", "Achievement Date (UTC)"]
        } else {
            headers = ["User ID", "Last Name", "First Name", "Achievement Type", "Achievement Name", "Level", "Achievement Date (UTC)"]
        }
        Integer columnNumber = 0
        Integer rowNum = initializeSheet(sheet, headers, projectExportHeaderAndFooter)

        CellStyle dateStyle = workbook.createCellStyle()
        dateStyle.setDataFormat((short) 22) // NOTE: 14 = "mm/dd/yyyy"

        Cell cell = null
        if (queryParams) {
            Stream<UserAchievedLevelRepo.AchievementItem> achievements = userAchievedRepo.findAllForAchievementNavigator(
                    queryParams.projectId, queryParams.usernameFilter, queryParams.from, queryParams.to,
                    queryParams.skillNameFilter, queryParams.minLevel, queryParams.achievementTypesWithoutOverall,
                    queryParams.allNonOverallTypes, queryParams.includeOverallType, usersTableAdditionalUserTagKey, queryParams.userTagFilter, queryParams.pageRequest)
            try {
                achievements?.each { UserAchievedLevelRepo.AchievementItem achievementItem ->
                    UserAchievementsMetricsBuilder.MetricUserAchievement metricUserAchievement = UserAchievementsMetricsBuilder.buildMetricUserAchievement(achievementItem)
                    columnNumber = 0
                    Row row = sheet.createRow(rowNum++)
                    row.createCell(columnNumber++).setCellValue(achievementItem.userIdForDisplay)
                    row.createCell(columnNumber++).setCellValue(achievementItem.lastName ?: '')
                    row.createCell(columnNumber++).setCellValue(achievementItem.firstName ?: '')
                    if (userTagLabel) {
                        row.createCell(columnNumber++).setCellValue(achievementItem.userTag)
                    }
                    row.createCell(columnNumber++).setCellValue(metricUserAchievement.type)
                    row.createCell(columnNumber++).setCellValue(metricUserAchievement.name)
                    row.createCell(columnNumber++).setCellValue(metricUserAchievement.level)

                    cell = row.createCell(columnNumber++)
                    cell.setCellStyle(dateStyle)
                    cell.setCellValue(achievementItem.achievedOn)
                }
            } finally {
                achievements.close()
            }
        }
        if (projectExportHeaderAndFooter) {
            addDataHeaderOrFooter(sheet, rowNum++,  headers.size(), projectExportHeaderAndFooter)
        }
    }

    @Transactional(readOnly = true)
    void exportSkillMetrics(Workbook workbook, String projectId) {
        String projectExportHeaderAndFooter = userCommunityService.replaceProjectDescriptorVar(exportHeaderAndFooter, userCommunityService.getProjectUserCommunity(projectId))
        Sheet sheet = workbook.createSheet()
        List<String> headers = ["Skill Name", "Skill ID", "# Users Achieved", "# Users In Progress", "Date Last Reported (UTC)", "Date Last Achieved (UTC)"]
        Integer columnNumber = 0
        Integer rowNum = initializeSheet(sheet, headers, projectExportHeaderAndFooter)

        CellStyle dateStyle = workbook.createCellStyle()
        dateStyle.setDataFormat((short) 22) // NOTE: 14 = "mm/dd/yyyy"

        Cell cell = null
        List<UserAchievedLevelRepo.SkillUsageItem> skillUsageItems = userAchievedRepo.findAllForSkillsNavigator(projectId)
        skillUsageItems?.each { UserAchievedLevelRepo.SkillUsageItem skillUsageItem ->
            Integer numAchieved = skillUsageItem.getNumUserAchieved() ?: 0
            Integer numProgress = skillUsageItem.getNumUsersInProgress() ?: 0
            columnNumber = 0
            Row row = sheet.createRow(rowNum++)
            row.createCell(columnNumber++).setCellValue(SkillReuseIdUtil.removeTag(InputSanitizer.unsanitizeName(skillUsageItem.skillName)))
            row.createCell(columnNumber++).setCellValue(skillUsageItem.skillId)
            row.createCell(columnNumber++).setCellValue(numAchieved)
            row.createCell(columnNumber++).setCellValue(numProgress - numAchieved)

            cell = row.createCell(columnNumber++)
            cell.setCellStyle(dateStyle)
            cell.setCellValue(skillUsageItem.lastReported)

            cell = row.createCell(columnNumber++)
            cell.setCellStyle(dateStyle)
            cell.setCellValue(skillUsageItem.lastAchieved)
        }
        if (projectExportHeaderAndFooter) {
            addDataHeaderOrFooter(sheet, rowNum++,  headers.size(), projectExportHeaderAndFooter)
        }
    }

    @Transactional(readOnly = true)
    void exportSubjectSkills(Workbook workbook, String projectId, String subjectId) {
        String projectExportHeaderAndFooter = userCommunityService.replaceProjectDescriptorVar(exportHeaderAndFooter, userCommunityService.getProjectUserCommunity(projectId))
        Sheet sheet = workbook.createSheet()
        List<String> headers = ["Skill Name", "Skill ID", "Group Name", "Tags", "Date Created (UTC)",  "Total Points", "Point Increment", "Repetitions", "Self Report", "Catalog", "Expiration", "Time Window", "Version", "Date Last Updated (UTC)",]
        Integer rowNum = initializeSheet(sheet, headers, projectExportHeaderAndFooter)

        CellStyle dateStyle = workbook.createCellStyle()
        dateStyle.setDataFormat((short) 22) // NOTE: 14 = "mm/dd/yyyy"

        List<SkillDefPartialRes> subjectSkills = skillsAdminService.getSkillsForSubjectWithCatalogStatus(projectId, subjectId, false)
        subjectSkills?.each { SkillDefPartialRes skillDef ->
            rowNum = addSkill(sheet, rowNum, dateStyle, skillDef)
        }
        if (projectExportHeaderAndFooter) {
            addDataHeaderOrFooter(sheet, rowNum++,  headers.size(), projectExportHeaderAndFooter)
        }
    }

    @Transactional(readOnly = true)
    void exportGlobalUserProgress(Workbook workbook, String userQuery, PageRequest pageRequest, String userTagValueFilter) {
        GlobalProgressMetricsService.ProjectIdsAndQuizIds projectIdsAndQuizIds = globalProgressMetricsService.getProjectIdsAndQuizIdsForCurrentUser()
        List<String> projectIds = projectIdsAndQuizIds.projectIds
        List<String> quizIds = projectIdsAndQuizIds.quizIds
        Boolean hasAnyUserCommunityOnlyProjectsOrQuizzes = userCommunityService.hasAnyUserCommunityOnlyProjectsOrQuizzes(projectIds, quizIds)
        String userCommunityValue = userCommunityService.getCommunityNameBasedOnConfAndItemStatus(hasAnyUserCommunityOnlyProjectsOrQuizzes)

        String projectExportHeaderAndFooter = userCommunityService.replaceProjectDescriptorVar(exportHeaderAndFooter, userCommunityValue)
        Sheet sheet = workbook.createSheet()
        List<String> baseHeaders = ["User ID", "Last Name", "First Name", "Quiz Attempts", "Quizzes Passed", "Quizzes Failed", "Quizzes In Progress", "Quizzes Needs Grading",
                                   "Surveys", "Surveys Completed", "Surveys In Progress", "Projects", "Project Levels Earned",
                                   "Subject Levels Earned", "Skills Earned", "Project Badges Earned", "Global Badges Earned"]
        List<String> headers = new ArrayList<>(baseHeaders)
        if (userTagLabel) {
            headers.add(3, userTagLabel)
        }
        Integer rowNum = initializeSheet(sheet, headers, projectExportHeaderAndFooter)

        userQuery = userQuery ? userQuery?.trim() : ''
        userTagValueFilter = userTagValueFilter ? userTagValueFilter?.trim() : ''

        Stream<GlobalProgressMetricsRepo.UserProgressMetric> userProgressMetrics = globalProgressMetricsRepo.streamUsersOverallProgress(
                projectIds,
                quizIds,
                userQuery,
                usersTableAdditionalUserTagKey ?: '',
                userTagValueFilter,
                pageRequest)
        try {
            userProgressMetrics.each {userItem ->
                Row row = sheet.createRow(rowNum++)
                Integer colNum = 0

                row.createCell(colNum++).setCellValue(userItem.userIdForDisplay ?: '')
                row.createCell(colNum++).setCellValue(userItem.lastName ?: '')
                row.createCell(colNum++).setCellValue(userItem.firstName ?: '')
                if (userTagLabel) {
                    row.createCell(colNum++).setCellValue(userItem.userTag ?: '')
                }
                row.createCell(colNum++).setCellValue(userItem.numQuizAttempts ?: 0)
                row.createCell(colNum++).setCellValue(userItem.numQuizzesPassed ?: 0)
                row.createCell(colNum++).setCellValue(userItem.numQuizzesFailed ?: 0)
                row.createCell(colNum++).setCellValue(userItem.numQuizzesInProgress ?: 0)
                row.createCell(colNum++).setCellValue(userItem.numQuizzesNeedsGrading ?: 0)
                row.createCell(colNum++).setCellValue(userItem.numSurveys ?: 0)
                row.createCell(colNum++).setCellValue(userItem.numSurveysCompleted ?: 0)
                row.createCell(colNum++).setCellValue(userItem.numSurveysInProgress ?: 0)
                row.createCell(colNum++).setCellValue(userItem.numProjects ?: 0)
                row.createCell(colNum++).setCellValue(userItem.projectLevelsEarned ?: 0)
                row.createCell(colNum++).setCellValue(userItem.subjectLevelsEarned ?: 0)
                row.createCell(colNum++).setCellValue(userItem.numSkillsEarned ?: 0)
                row.createCell(colNum++).setCellValue(userItem.numBadgesEarned ?: 0)
                row.createCell(colNum++).setCellValue(userItem.globalBadgesEarned ?: 0)
            }
            if (projectExportHeaderAndFooter) {
                addDataHeaderOrFooter(sheet, rowNum++,  headers.size(), projectExportHeaderAndFooter)
            }
        } finally {
            userProgressMetrics.close()
        }
    }

    @Transactional(readOnly = true)
    void exportQuizRuns(Workbook workbook, List<String> quizIds, String userQuery, String nameQuery, String userIdFilter, PageRequest pageRequest, Date startDate, Date endDate) {
        Boolean hasAnyUserCommunityOnlyQuizzes = userCommunityService.hasAnyUserCommunityOnlyProjectsOrQuizzes([], quizIds)
        String userCommunityValue = userCommunityService.getCommunityNameBasedOnConfAndItemStatus(hasAnyUserCommunityOnlyQuizzes)

        String projectExportHeaderAndFooter = userCommunityService.replaceProjectDescriptorVar(exportHeaderAndFooter, userCommunityValue)
        Sheet sheet = workbook.createSheet()
        List<String> headers
        if (userTagLabel) {
            headers = ["User ID", "Last Name", "First Name", userTagLabel, "Quiz Name", "Type", "Status", "Number Correct", "Number of Questions", "Date Started (UTC)", "Date Completed (UTC)", "Runtime (seconds)"]
        } else {
            headers = ["User ID", "Last Name", "First Name", "Quiz Name", "Type", "Status", "Number Correct", "Number of Questions", "Date Started (UTC)", "Date Completed (UTC)", "Runtime (seconds)"]
        }
        Integer rowNum = initializeSheet(sheet, headers, projectExportHeaderAndFooter)
        CellStyle dateStyle = workbook.createCellStyle()
        dateStyle.setDataFormat((short) 22) // NOTE: 14 = "mm/dd/yyyy"

        Cell cell = null
        userQuery = userQuery ? userQuery?.trim() : ''
        nameQuery = nameQuery ? nameQuery?.trim() : ''
        userIdFilter = userIdFilter ? userIdFilter?.trim() : ''

        Stream<UserQuizAttemptRepo.QuizRun> quizRunInfoStream = userQuizAttemptRepo.streamQuizRuns(
                quizIds,
                userQuery,
                userIdFilter,
                nameQuery,
                usersTableAdditionalUserTagKey ?: '',
                null,
                startDate,
                endDate,
                pageRequest)
        try {
            quizRunInfoStream.each { quizRunInfo ->
                Row row = sheet.createRow(rowNum++)
                Integer colNum = 0

                row.createCell(colNum++).setCellValue(quizRunInfo.userIdForDisplay ?: '')
                row.createCell(colNum++).setCellValue(quizRunInfo.lastName ?: '')
                row.createCell(colNum++).setCellValue(quizRunInfo.firstName ?: '')
                if (userTagLabel) {
                    row.createCell(colNum++).setCellValue(quizRunInfo.userTag ?: '')
                }
                row.createCell(colNum++).setCellValue(quizRunInfo.quizName ?: '')
                row.createCell(colNum++).setCellValue(quizRunInfo.quizType ?: '')
                row.createCell(colNum++).setCellValue(quizRunInfo.status ?: '')
                row.createCell(colNum++).setCellValue(quizRunInfo.numberCorrect ?: 0)
                row.createCell(colNum++).setCellValue(quizRunInfo.totalAnswers ?: 0)

                cell = row.createCell(colNum++)
                cell.setCellStyle(dateStyle)
                cell.setCellValue(quizRunInfo.started)
                cell = row.createCell(colNum++)
                cell.setCellStyle(dateStyle)
                cell.setCellValue(quizRunInfo.completed)

                Long runtime = quizRunInfo.completed ? (quizRunInfo.completed.time - quizRunInfo.started.time) / 1000 : null
                row.createCell(colNum++).setCellValue(runtime)

            }
            if (projectExportHeaderAndFooter) {
                addDataHeaderOrFooter(sheet, rowNum++, headers.size(), projectExportHeaderAndFooter)
            }
        } finally {
            quizRunInfoStream.close()
        }
    }

    private Integer addSkill(Sheet sheet, Integer rowNum, CellStyle dateStyle, SkillDefPartialRes skillDef) {

        if (skillDef.type == SkillDef.ContainerType.SkillsGroup) {
            skillsAdminService.getSkillsByProjectSkillAndType(skillDef.projectId, skillDef.skillId, SkillDef.ContainerType.SkillsGroup, SkillRelDef.RelationshipType.SkillsGroupRequirement).each {
                it.setGroupId(skillDef.skillId)
                it.setGroupName(skillDef.name)
                rowNum = addSkill(sheet, rowNum, dateStyle, it)
            }
        } else {
            Row row = sheet.createRow(rowNum++)
            Integer columnNumber = 0
            row.createCell(columnNumber++).setCellValue(SkillReuseIdUtil.removeTag(InputSanitizer.unsanitizeName(skillDef.name)))
            row.createCell(columnNumber++).setCellValue(skillDef.skillId)
            row.createCell(columnNumber++).setCellValue(skillDef.groupName)
            row.createCell(columnNumber++).setCellValue(skillDef.tags?.collect {it.tagValue}?.join(','))

            Cell cell = row.createCell(columnNumber++)
            cell.setCellStyle(dateStyle)
            cell.setCellValue(skillDef.created)

            row.createCell(columnNumber++).setCellValue(skillDef.totalPoints)
            row.createCell(columnNumber++).setCellValue(skillDef.pointIncrement)
            row.createCell(columnNumber++).setCellValue(skillDef.numPerformToCompletion)
            row.createCell(columnNumber++).setCellValue(getSelfReportType(skillDef))
            row.createCell(columnNumber++).setCellValue(getCatalogStatus(skillDef))
            row.createCell(columnNumber++).setCellValue(getExpiration(skillDef))
            row.createCell(columnNumber++).setCellValue(getTimeWindow(skillDef))
            row.createCell(columnNumber++).setCellValue(skillDef.version)

            Cell lastUpdatedCell = row.createCell(columnNumber++)
            lastUpdatedCell.setCellStyle(dateStyle)
            lastUpdatedCell.setCellValue(skillDef.updated)
        }

        return rowNum
    }


    private static String getSelfReportType(SkillDefPartialRes skillDef) {
        String selfReportType = ""
        if (skillDef.selfReportingType) {
            if (skillDef.selfReportingType == SkillDef.SelfReportingType.HonorSystem) {
                selfReportType = "Honor System"
            } else {
                selfReportType = skillDef.selfReportingType
            }
        }
        return selfReportType
    }

    private static String getCatalogStatus(SkillDefPartialRes skillDef) {
        String catalogStatus = ""
        if (skillDef.copiedFromProjectId && !skillDef.isReused) {
            catalogStatus = "Imported"
        } else if (skillDef.sharedToCatalog) {
            catalogStatus = "Exported"
        }
        return catalogStatus
    }

    private static String getExpiration(SkillDefPartialRes skillDef) {
        String expiration = ""
        if (skillDef.expirationType && skillDef.expirationType != ExpirationAttrs.NEVER) {
            Boolean plural = skillDef.every > 1
            if (skillDef.expirationType == ExpirationAttrs.YEARLY) {
                expiration = "Every${plural ? " ${skillDef.every}" : ''} ${pluralize(skillDef.every, 'year')} on ${skillDef.nextExpirationDate.format("MM/dd")}"
            } else if (skillDef.expirationType == ExpirationAttrs.MONTHLY) {
                String dayOfMonth = skillDef.monthlyDay == ExpirationAttrs.LAST_DAY_OF_MONTH ? "last day" : getDayOfMonthWithSuffix(skillDef.nextExpirationDate.toMonthDay().dayOfMonth)
                expiration = "Every${plural ? " ${skillDef.every}" : ''} ${pluralize(skillDef.every, 'month')} on the ${dayOfMonth}"
            } else if (skillDef.expirationType == ExpirationAttrs.DAILY) {
                expiration = "After ${skillDef.every} ${pluralize(skillDef.every, 'day')} of inactivity"
            }
        }
        return expiration
    }
    private static String getDayOfMonthWithSuffix(final int n) {
        assert n >= 1 && n <= 31, "illegal day of month: ${n}"
        String dayOfMonth = Integer.toString(n)
        if (n >= 11 && n <= 13) {
            return "${dayOfMonth}th";
        }
        switch (n % 10) {
            case 1:  return "${dayOfMonth}st";
            case 2:  return "${dayOfMonth}nd";
            case 3:  return "${dayOfMonth}rd";
            default: return "${dayOfMonth}th";
        }
    }
    private static String getTimeWindow(SkillDefPartialRes skillDef) {
        String timeWindow = ""
        if (skillDef.type == SkillDef.ContainerType.Skill) {
            Boolean timeWindowEnabled = skillDef.pointIncrementInterval > 0
            if (timeWindowEnabled && skillDef.numPerformToCompletion != 1) {
                Integer hours = Math.floor(skillDef.pointIncrementInterval / 60)
                timeWindow = "${hours} ${pluralize(hours, 'Hour')}"
                Integer minutes = skillDef.pointIncrementInterval % 60
                if (minutes > 0) {
                    timeWindow = "${timeWindow} ${minutes} ${pluralize(minutes, 'Minute')}"
                }

                if (skillDef.numMaxOccurrencesIncrementInterval > 0) {
                    timeWindow = "${timeWindow}, Up to ${skillDef.numMaxOccurrencesIncrementInterval} ${pluralize(skillDef.numMaxOccurrencesIncrementInterval, 'Occurrence')}"
                }
            }
        }
        return timeWindow
    }

    private static String pluralize(int count, String value) {
        return count == 1 ? value : "${value}s"
    }

    private static Integer initializeSheet(Sheet sheet, List<String> headers, String projectExportHeaderAndFooter) {
        Integer rowNum = 0
        Integer columnNumber = 0
        if (projectExportHeaderAndFooter) {
            addDataHeaderOrFooter(sheet, rowNum++, headers.size(), projectExportHeaderAndFooter)
        }
        Row headerRow = sheet.createRow(rowNum++)
        headers.eachWithIndex { String header, int i ->
            headerRow.createCell(columnNumber++).setCellValue(header)
        }
        return rowNum
    }

    private static void addDataHeaderOrFooter(Sheet sheet, Integer rowNum, Integer numCols, String exportHeaderAndFooter) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(0);
        cell.setCellValue(exportHeaderAndFooter)
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numCols - 1))
    }
}
