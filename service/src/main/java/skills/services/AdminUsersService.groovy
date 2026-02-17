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
import org.apache.commons.lang3.tuple.Pair
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.*
import skills.skillLoading.RankingLoader
import skills.skillLoading.model.UsersPerLevel
import skills.storage.model.DayCountItem
import skills.storage.model.MonthlyCountItem
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

    List<TimestampCountItem> getUsage(String projectId, String skillId, Date start, Boolean newUsersOnly = false) {
        Date startDate = LocalDateTime.of(start.toLocalDate(), LocalTime.MIN).toDate()
        List<DayCountItem> res

        if(skillId) {
            res = userEventService.getDistinctUserCountForSkillId(projectId, skillId, startDate, newUsersOnly)
        } else {
            res = userEventService.getDistinctUserCountsForProject(projectId, startDate, newUsersOnly)
        }

        List<TimestampCountItem> countsPerDay = []
        res?.each {
            countsPerDay << new TimestampCountItem(value: it.day.time, count: it.count)
        }

        return countsPerDay
    }

    List<TimestampCountItem> getUsagePerMonth(String projectId, String subjectId, Date start, Boolean newUsersOnly = false) {
        Date startDate = LocalDateTime.of(start.toLocalDate(), LocalTime.MIN).toDate()
        List<MonthlyCountItem> users

        if(subjectId) {
            users = userEventService.getDistinctUserCountForSubjectByMonth(projectId, subjectId, startDate, newUsersOnly)
        } else {
            users = userEventService.getDistinctUserCountsForProjectByMonth(projectId, startDate, newUsersOnly)
        }

        List<TimestampCountItem> countsPerMonth = []
        users?.each {
            countsPerMonth << new TimestampCountItem(value: it.month.time, count: it.count)
        }

        return countsPerMonth
    }

    List<LabelCountItem> getUserCountsPerLevel(String projectId, subjectId = null, String tagKey = null, String tagFilter = null) {
        List<UsersPerLevel> levels = rankingLoader.getUserCountsPerLevel(projectId, false, subjectId, tagKey, tagFilter)

        return levels.collect{
            new LabelCountItem(value: "Level ${it.level}", count: it.numUsers)
        }
    }

    @Transactional(readOnly = true)
    TableResultWithTotalPoints loadUsersPageForProject(String projectId, String query, PageRequest pageRequest, int minimumPointsPercent, int maximumPointsPercent) {
        query = query ? query.trim() : ''
        Integer totalPoints = projDefRepo.getTotalPointsByProjectId(projectId) ?: 0
        Pair<Integer, Integer> minMax = calcMinMaxPointsQueryParams(totalPoints, minimumPointsPercent, maximumPointsPercent)
        Page<ProjectUser> usersPage = findDistinctUsersForProject(projectId, query, pageRequest, minMax.left, minMax.right)
        return new TableResultWithTotalPoints(usersPage, totalPoints)
    }

    @Profile
    private Page<ProjectUser> findDistinctUsersForProject(String projectId, String query, PageRequest pageRequest, int minimumPoints, int maximumPoints) {
        return userPointsRepo.findDistinctProjectUsersAndUserIdLike(projectId, usersTableAdditionalUserTagKey, query, minimumPoints, maximumPoints, pageRequest)
    }

    @Profile
    Stream<ProjectUser> streamAllDistinctUsersForProject(String projectId, String query, PageRequest pageRequest, int minimumPoints, int maximumPoints) {
        return userPointsRepo.findDistinctProjectUsersAndUserIdLike(projectId, usersTableAdditionalUserTagKey, query, minimumPoints, maximumPoints, pageRequest)
    }

    @Profile
    long countTotalProjUsers(String projectId) {
        userPointsRepo.countDistinctUserIdByProjectId(projectId)
    }

    TableResultWithTotalPoints loadUsersPageForUserTag(String projectId, String userTagKey, String userTagValue, String query, PageRequest pageRequest) {
        if (!userTagKey || !userTagValue) {
            return TableResultWithTotalPoints.EMPTY
        }
        query = query ? query.trim() : ''
        Integer totalPoints = projDefRepo.getTotalPointsByProjectId(projectId) ?: 0
        Page<ProjectUser> usersPage = userPointsRepo.findDistinctProjectUsersByProjectIdAndUserTagAndUserIdLike(projectId, usersTableAdditionalUserTagKey, userTagKey, userTagValue, query, pageRequest)
        return new TableResultWithTotalPoints(usersPage, totalPoints)
    }

    TableResultWithTotalPoints loadUsersPageForSkills(String projectId, List<String> skillIds, String query, PageRequest pageRequest, int minimumPointsPercent, int maximumPointsPercent) {
        if (!skillIds) {
            return TableResultWithTotalPoints.EMPTY
        }
        query = query ? query.trim() : ''
        Integer totalPoints = skillDefRepo.getTotalPointsSumForSkills(projectId, skillIds) ?: 0
        Pair<Integer, Integer> minMax = calcMinMaxPointsQueryParams(totalPoints, minimumPointsPercent, maximumPointsPercent)
        Page<ProjectUser> usersPage = userPointsRepo.findDistinctProjectUsersByProjectIdAndSkillIdInAndUserIdLike(projectId, usersTableAdditionalUserTagKey, skillIds, query, minMax.left, minMax.right, pageRequest)
        return new TableResultWithTotalPoints(usersPage, totalPoints)
    }

    TableResultWithTotalPoints loadUsersPageForSubject(String projectId, String subjectId, String query, PageRequest pageRequest, int minimumPointsPercent, int maximumPointsPercent) {
        if (!subjectId) {
            return TableResultWithTotalPoints.EMPTY
        }
        query = query ? query.trim() : ''
        Integer totalPoints = skillDefRepo.getTotalPointsByProjectIdAndSkillId(projectId, subjectId) ?: 0
        Pair<Integer, Integer> minMax = calcMinMaxPointsQueryParams(totalPoints, minimumPointsPercent, maximumPointsPercent)
        Integer count = (Integer)PostgresQlNativeRepo.countDistinctUsersByProjectIdAndSubjectIdAndUserIdLike(projectId, subjectId, query, minMax.left, minMax.right)
        List<ProjectUser> usersData = userPointsRepo.findDistinctProjectUsersByProjectIdAndSubjectIdAndUserIdLike(projectId, usersTableAdditionalUserTagKey, subjectId, query, minMax.left, minMax.right, pageRequest)
        return new TableResultWithTotalPoints(usersData, count, totalPoints)
    }

    private static Pair<Integer, Integer> calcMinMaxPointsQueryParams(Integer totalPoints, int minimumPointsPercent, int maximumPointsPercent) {
        int minimumPoints = (int)Math.floor((minimumPointsPercent / 100) * totalPoints)
        int maximumPoints = (int)Math.ceil((maximumPointsPercent / 100) * totalPoints)

        // Because the database query uses "less than" logic, special consideration must be made
        // for when maximum points are not filtered at all so as not to exclude users who have
        // reached 100% completion; thus, a single point is added to the high end of the search
        if(maximumPointsPercent == 100) {
            maximumPoints += 1
        }
        return Pair.of(minimumPoints, maximumPoints)
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
