package skills.service.datastore.services

import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import skills.service.controller.result.model.LabelCountItem
import skills.service.controller.result.model.ProjectUser
import skills.service.controller.result.model.TableResult
import skills.service.controller.result.model.TimestampCountItem
import skills.service.skillLoading.RankingLoader
import skills.service.skillLoading.model.UsersPerLevel
import skills.storage.model.SkillDef
import skills.storage.model.UsageItem
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

    List<TimestampCountItem> getUsage(String projectId, Integer numDays) {
        Date startDate
        use (TimeCategory) {
            startDate = (numDays-1).days.ago
            startDate.clearTime()
        }

        List<UsageItem> res = userPointsRepo.findDistinctUserCountsByProject(projectId, startDate)

        List<TimestampCountItem> countsPerDay = []
        startDate.upto(new Date().clearTime()) { Date theDate ->
            UsageItem found = res.find({it.day.clearTime() == theDate})
            countsPerDay << new TimestampCountItem(value: theDate.time, count: found?.numItems ?: 0)
        }

        return countsPerDay
    }

    List<TimestampCountItem> getBadgesPerDay(String projectId, Integer numDays) {
        Date startDate
        use (TimeCategory) {
            startDate = (numDays-1).days.ago
            startDate.clearTime()
        }

        List<UsageItem> res = userAchievedRepo.countAchievementsForProjectPerDay(projectId, SkillDef.ContainerType.Badge, startDate)

        List<TimestampCountItem> countsPerDay = []
        startDate.upto(new Date().clearTime()) { Date theDate ->
            UsageItem found = res.find({
                it.day.clearTime() == theDate
            })
            countsPerDay << new TimestampCountItem(value: theDate.time, count: found?.numItems ?: 0)
        }

        return countsPerDay
    }

    List<LabelCountItem> getBadgesPerMonth(String projectId, Integer numMonths=6) {
        Date startDate
        use (TimeCategory) {
            startDate = (numMonths-1).months.ago
            startDate.clearTime()
        }

        List<UserAchievedLevelRepo.LabelCountInfo> res = userAchievedRepo.countAchievementsForProjectPerMonth(projectId, SkillDef.ContainerType.Badge, startDate)

        List<LabelCountItem> countsPerMonth = []
        Month currentMonth = LocalDate.now().month
        Month startMonth = currentMonth - numMonths

        (1..numMonths).each {
            Month month = startMonth+it
            println "Month: $month"

            UserAchievedLevelRepo.LabelCountInfo found = res.find ({
                it.label == "${month.value}"
            })
            countsPerMonth << new LabelCountItem(value: month.getDisplayName(TextStyle.SHORT, Locale.US), count: found?.count ?: 0)
        }

        return countsPerMonth
    }

    List<LabelCountItem> getAchievementCountsPerSubject(String projectId) {
        List<UserAchievedLevelRepo.LabelCountInfo> res = userAchievedRepo.getUsageFacetedViaSubject(projectId, SkillDef.ContainerType.Subject)

        return res.collect {
            new LabelCountItem(value: it.label, count: it.count)
        }
    }

    List<LabelCountItem> getUserCountsPerLevel(String projectId) {
        List<UsersPerLevel> levels = rankingLoader.getUserCountsPerLevel(projectId, true)

        return levels.collect{
            new LabelCountItem(value: "Level ${it.level}", count: it.numUsers)
        }
    }

    TableResult loadUsersPage(String projectId, String query, PageRequest pageRequest) {
        TableResult result = new TableResult()
        Long totalProjectUsers = userPointsRepo.countDistinctUserIdByProjectIdAndUserIdLike(projectId, query)
        if (totalProjectUsers) {
            List<ProjectUser> projectUsers = userPointsRepo.findDistinctProjectUsersAndUserIdLike(projectId, query, pageRequest)
            result.data = projectUsers
            result.count = totalProjectUsers
        }
        return result
    }

    TableResult loadUsersPage(String projectId, List<String> skillIds, String query, PageRequest pageRequest) {
        TableResult result = new TableResult()
        Long totalProjectUsers = userPointsRepo.countDistinctUserIdByProjectIdAndSkillIdInAndUserIdLike(projectId, skillIds, query)
        if (totalProjectUsers) {
            List<ProjectUser> projectUsers = userPointsRepo.findDistinctProjectUsersByProjectIdAndSkillIdInAndUserIdLike(projectId, skillIds, query, pageRequest)
            result.data = projectUsers
            result.count = totalProjectUsers
        }
        return result
    }
}
