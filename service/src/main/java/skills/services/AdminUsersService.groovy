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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.LabelCountItem
import skills.controller.result.model.ProjectUser
import skills.controller.result.model.TableResult
import skills.controller.result.model.TimestampCountItem
import skills.controller.result.model.UserInfoRes
import skills.skillLoading.RankingLoader
import skills.skillLoading.model.UsersPerLevel
import skills.storage.model.DayCountItem
import skills.storage.model.SkillDef
import skills.storage.model.UserPoints
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo

import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle

@Service
@Slf4j
class AdminUsersService {

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    RankingLoader rankingLoader

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    List<TimestampCountItem> getProjectUsage(String projectId, Integer numDays) {
        Date startDate
        use (TimeCategory) {
            startDate = (numDays-1).days.ago
            startDate.clearTime()
        }

        List<DayCountItem> res = userPointsRepo.findDistinctUserCountsByProject(projectId, startDate)

        List<TimestampCountItem> countsPerDay = []
        if (res) {
            Date earliest = res.min { it.day }.day
            earliest.upto(new Date().clearTime()) { Date theDate ->
                DayCountItem found = res.find({ it.day.clearTime() == theDate })
                countsPerDay << new TimestampCountItem(value: theDate.time, count: found?.count ?: 0)
            }
        }

        return countsPerDay
    }

    List<TimestampCountItem> getSubjectUsage(String projectId, String subjectId, Integer numDays) {
        Date startDate
        use (TimeCategory) {
            startDate = (numDays-1).days.ago
            startDate.clearTime()
        }

        List<DayCountItem> res = userPointsRepo.findDistinctUserCountsBySkillId(projectId, subjectId, startDate)

        List<TimestampCountItem> countsPerDay = []
        startDate.upto(new Date().clearTime()) { Date theDate ->
            DayCountItem found = res.find({it.day.clearTime() == theDate})
            countsPerDay << new TimestampCountItem(value: theDate.time, count: found?.count ?: 0)
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

    List<LabelCountItem> getUserCountsPerLevel(String projectId, subjectId = null) {
        List<UsersPerLevel> levels = rankingLoader.getUserCountsPerLevel(projectId,false, subjectId)

        return levels.collect{
            new LabelCountItem(value: "Level ${it.level}", count: it.numUsers)
        }
    }

    TableResult loadUsersPage(String projectId, String query, PageRequest pageRequest) {
        TableResult result = new TableResult()
        Long totalProjectUsers = countTotalProjUsers(projectId)
        if (totalProjectUsers) {
            query = query ? query.trim() : ''
            result.totalCount = totalProjectUsers
            List<ProjectUser> projectUsers = findDistinctUsers(projectId, query, pageRequest)
            result.data = projectUsers
            if (!projectUsers) {
                result.count = 0
            } else if (query) {
                result.count = userPointsRepo.countDistinctUserIdByProjectIdAndUserIdLike(projectId, query)
            } else {
                result.count = totalProjectUsers
            }
        }
        return result
    }

    @Profile
    private List<ProjectUser> findDistinctUsers(String projectId, String query, PageRequest pageRequest) {
        userPointsRepo.findDistinctProjectUsersAndUserIdLike(projectId, query, pageRequest)
    }

    @Profile
    public long countTotalProjUsers(String projectId) {
        userPointsRepo.countDistinctUserIdByProjectId(projectId)
    }

    TableResult loadUsersPage(String projectId, List<String> skillIds, String query, PageRequest pageRequest) {
        TableResult result = new TableResult()
        if (!skillIds) {
            return result
        }
        Long totalProjectUsersWithSkills = userPointsRepo.countDistinctUserIdByProjectIdAndSkillIdIn(projectId, skillIds)
        if (totalProjectUsersWithSkills) {
            query = query ? query.trim() : ''
            result.totalCount = totalProjectUsersWithSkills
            List<ProjectUser> projectUsers = userPointsRepo.findDistinctProjectUsersByProjectIdAndSkillIdInAndUserIdLike(projectId, skillIds, query, pageRequest)
            result.data = projectUsers
            if (!projectUsers) {
                result.count = 0
            } else if (query) {
                result.count = userPointsRepo.countDistinctUserIdByProjectIdAndSkillIdInAndUserIdLike(projectId, skillIds, query)
            } else {
                result.count = totalProjectUsersWithSkills
            }
        }
        return result
    }

    @Transactional
    UserInfoRes getUserForProject(String projectId, String userId) {
        // check to see if the user actually achieved any points against this project
        UserPoints userPoints = userPointsRepo.findByProjectIdAndUserIdAndSkillIdAndDay(projectId, userId, null, null)
        if (!userPoints) {
            return null
        }
        return accessSettingsStorageService.loadUserInfo(userId)
    }
}
