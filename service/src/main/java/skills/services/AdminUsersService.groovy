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

import callStack.profiler.Profile
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.*
import skills.skillLoading.RankingLoader
import skills.skillLoading.model.UsersPerLevel
import skills.storage.model.DayCountItem
import skills.storage.model.SkillDef
import skills.storage.model.UserPoints
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo
import skills.storage.repos.nativeSql.PostgresQlNativeRepo

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.format.TextStyle
import java.util.stream.Collectors
import java.util.stream.Stream

@Service
@Slf4j
class AdminUsersService {

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    RankingLoader rankingLoader

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    UserEventService userEventService

    @Autowired
    PostgresQlNativeRepo PostgresQlNativeRepo

    @Value('${skills.config.ui.usersTableAdditionalUserTagKey:}')
    String usersTableAdditionalUserTagKey

    List<TimestampCountItem> getUsage(String projectId, String skillId, Date start) {
        Date startDate = LocalDateTime.of(start.toLocalDate(), LocalTime.MIN).toDate()

        List<DayCountItem> res = skillId ?
                userEventService.getDistinctUserCountForSkillId(projectId, skillId, startDate) :
                userEventService.getDistinctUserCountsForProject(projectId, startDate)

        List<TimestampCountItem> countsPerDay = []
        res?.each {
            countsPerDay << new TimestampCountItem(value: it.day.time, count: it.count)
        }

        return countsPerDay
    }

    List<TimestampCountItem> getBadgesPerDay(String projectId, String badgeId, Integer numDays) {
        Date startDate
        use (TimeCategory) {
            startDate = (numDays-1).days.ago
            startDate.clearTime()
        }

        List<DayCountItem> res = userAchievedRepo.countAchievementsForProjectPerDay(projectId, badgeId, SkillDef.ContainerType.Badge, startDate)

        List<TimestampCountItem> countsPerDay = []
        startDate.upto(new Date().clearTime()) { Date theDate ->
            DayCountItem found = res.find({
                it.day.clearTime() == theDate
            })
            countsPerDay << new TimestampCountItem(value: theDate.time, count: found?.count ?: 0)
        }

        return countsPerDay
    }

    List<LabelCountItem> getBadgesPerMonth(String projectId, String badgeId, Integer numMonths=6) {
        Date startDate
        use (TimeCategory) {
            startDate = (numMonths-1).months.ago
            startDate.clearTime()
        }

        List<UserAchievedLevelRepo.LabelCountInfo> res = userAchievedRepo.countAchievementsForProjectPerMonth(projectId, badgeId, SkillDef.ContainerType.Badge, startDate)

        List<LabelCountItem> countsPerMonth = []
        Month currentMonth = LocalDate.now().month
        Month startMonth = currentMonth - numMonths

        (1..numMonths).each {
            Month month = startMonth+it

            UserAchievedLevelRepo.LabelCountInfo found = res.find ({
                Double.parseDouble(it.label).toInteger() == month.value
            })
            countsPerMonth << new LabelCountItem(value: month.getDisplayName(TextStyle.SHORT, Locale.US), count: found?.countRes ?: 0)
        }

        return countsPerMonth
    }

    List<LabelCountItem> getAchievementCountsPerSubject(String projectId, int topNToLoad =5) {
        List<UserAchievedLevelRepo.LabelCountInfo> res = userAchievedRepo.getUsageFacetedViaSubject(projectId, SkillDef.ContainerType.Subject, PageRequest.of(0, topNToLoad, Sort.Direction.DESC, "countRes"))

        return res.collect {
            new LabelCountItem(value: it.label, count: it.countRes)
        }
    }

    List<LabelCountItem> getAchievementCountsPerSkill(String projectId, String subjectId, int topNToLoad =5) {
        List<UserAchievedLevelRepo.LabelCountInfo> res = userAchievedRepo.getSubjectUsageFacetedViaSkill(projectId, subjectId, SkillDef.ContainerType.Subject, PageRequest.of(0, topNToLoad, Sort.Direction.DESC, "countRes"))

        return res.collect {
            new LabelCountItem(value: it.label, count: it.countRes)
        }
    }

    List<LabelCountItem> getUserCountsPerLevel(String projectId, subjectId = null, String tagKey = null, String tagFilter = null) {
        List<UsersPerLevel> levels = rankingLoader.getUserCountsPerLevel(projectId, false, subjectId, tagKey, tagFilter)

        return levels.collect{
            new LabelCountItem(value: "Level ${it.level}", count: it.numUsers)
        }
    }

    @Transactional(readOnly = true)
    TableResultWithTotalPoints loadUsersPageForProject(String projectId, String query, PageRequest pageRequest, int minimumPoints) {
        TableResultWithTotalPoints result = new TableResultWithTotalPoints()
        result.totalPoints = projDefRepo.getTotalPointsByProjectId(projectId) ?: 0
        Long totalProjectUsers = countTotalProjUsers(projectId)
        if (totalProjectUsers) {
            query = query ? query.trim() : ''
            result.totalCount = totalProjectUsers
            List<ProjectUser> projectUsers = findDistinctUsersForProject(projectId, query, pageRequest, minimumPoints)
            result.data = projectUsers
            if (!projectUsers) {
                result.count = 0
            } else if (query || minimumPoints > 0) {
                result.count = userPointsRepo.countDistinctUserIdByProjectIdAndUserIdLike(projectId, query, minimumPoints)
            } else {
                result.count = totalProjectUsers
            }
        }
        return result
    }

    @Profile
    private List<ProjectUser> findDistinctUsersForProject(String projectId, String query, PageRequest pageRequest, int minimumPoints) {
        Stream<ProjectUser> projectUsers = userPointsRepo.findDistinctProjectUsersAndUserIdLike(projectId, usersTableAdditionalUserTagKey, query, minimumPoints, pageRequest)
        try {
            return projectUsers.collect(Collectors.toList());
        } finally {
            projectUsers.close()
        }
    }

    @Profile
    Stream<ProjectUser> streamAllDistinctUsersForProject(String projectId, String query, PageRequest pageRequest, int minimumPoints) {
        return userPointsRepo.findDistinctProjectUsersAndUserIdLike(projectId, usersTableAdditionalUserTagKey, query, minimumPoints, pageRequest)
    }

    @Profile
    public long countTotalProjUsers(String projectId) {
        userPointsRepo.countDistinctUserIdByProjectId(projectId)
    }

    TableResultWithTotalPoints loadUsersPageForUserTag(String projectId, String userTagKey, String userTagValue, String query, PageRequest pageRequest) {
        TableResultWithTotalPoints result = new TableResultWithTotalPoints()
        if (!userTagKey || !userTagValue) {
            return result
        }
        result.totalPoints = projDefRepo.getTotalPointsByProjectId(projectId) ?: 0
        Long totalProjectUsersWithUserTag = userPointsRepo.countDistinctUserIdByProjectIdAndUserTag(projectId, userTagKey, userTagValue)
        if (totalProjectUsersWithUserTag) {
            query = query ? query.trim().toLowerCase() : ''
            result.totalCount = totalProjectUsersWithUserTag
            List<ProjectUser> projectUsers = userPointsRepo.findDistinctProjectUsersByProjectIdAndUserTagAndUserIdLike(projectId, usersTableAdditionalUserTagKey, userTagKey, userTagValue, query, pageRequest)
            result.data = projectUsers
            if (!projectUsers) {
                result.count = 0
            } else if (query) {
                result.count = userPointsRepo.countDistinctUserIdByProjectIdAndUserTagAndUserIdLike(projectId, userTagKey, userTagValue, query)
            } else {
                result.count = totalProjectUsersWithUserTag
            }
        }
        return result
    }

    TableResultWithTotalPoints loadUsersPageForSkills(String projectId, List<String> skillIds, String query, PageRequest pageRequest, int minimumPoints) {
        TableResultWithTotalPoints result = new TableResultWithTotalPoints()
        if (!skillIds) {
            return result
        }
        result.totalPoints = skillDefRepo.getTotalPointsSumForSkills(projectId, skillIds) ?: 0
        Long totalProjectUsersWithSkills = userPointsRepo.countDistinctUserIdByProjectIdAndSkillIdIn(projectId, skillIds)
        if (totalProjectUsersWithSkills) {
            query = query ? query.trim() : ''
            result.totalCount = totalProjectUsersWithSkills
            List<ProjectUser> projectUsers = userPointsRepo.findDistinctProjectUsersByProjectIdAndSkillIdInAndUserIdLike(projectId, usersTableAdditionalUserTagKey, skillIds, query, minimumPoints, pageRequest)
            result.data = projectUsers
            if (!projectUsers) {
                result.count = 0
            } else if (query || minimumPoints > 0) {
                result.count = userPointsRepo.countDistinctUserIdByProjectIdAndSkillIdInAndUserIdLike(projectId, skillIds, query, minimumPoints)
            } else {
                result.count = totalProjectUsersWithSkills
            }
        }
        return result
    }

    TableResultWithTotalPoints loadUsersPageForSubject(String projectId, String subjectId, String query, PageRequest pageRequest, int minimumPoints) {
        TableResultWithTotalPoints result = new TableResultWithTotalPoints()
        if (!subjectId) {
            return result
        }
        result.totalPoints = skillDefRepo.getTotalPointsByProjectIdAndSkillId(projectId, subjectId) ?: 0
        Long totalProjectUsersWithSkills = PostgresQlNativeRepo.countDistinctUsersByProjectIdAndSubjectId(projectId, subjectId)
        if (totalProjectUsersWithSkills) {
            query = query ? query.trim() : ''
            result.totalCount = totalProjectUsersWithSkills
            List<ProjectUser> projectUsers = userPointsRepo.findDistinctProjectUsersByProjectIdAndSubjectIdAndUserIdLike(projectId, usersTableAdditionalUserTagKey, subjectId, query, minimumPoints, pageRequest)
            result.data = projectUsers
            if (!projectUsers) {
                result.count = 0
            } else if (query || minimumPoints > 0) {
                result.count = PostgresQlNativeRepo.countDistinctUsersByProjectIdAndSubjectIdAndUserIdLike(projectId, subjectId, query, minimumPoints)
            } else {
                result.count = totalProjectUsersWithSkills
            }
        }
        return result
    }

    @Transactional
    UserInfoRes getUserForProject(String projectId, String userId) {
        // check to see if the user actually achieved any points against this project
        UserPoints userPoints = userPointsRepo.findByProjectIdAndUserIdAndSkillId(projectId, userId, null)
        if (!userPoints) {
            return null
        }
        return accessSettingsStorageService.loadUserInfo(userId)
    }
}
