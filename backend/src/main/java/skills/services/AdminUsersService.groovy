package skills.services

import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import skills.skillLoading.RankingLoader
import skills.skillLoading.model.UsersPerLevel
import skills.storage.model.SkillDef
import skills.storage.model.DayCountItem
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

    List<skills.controller.result.model.TimestampCountItem> getProjectUsage(String projectId, Integer numDays) {
        Date startDate
        use (TimeCategory) {
            startDate = (numDays-1).days.ago
            startDate.clearTime()
        }

        List<DayCountItem> res = userPointsRepo.findDistinctUserCountsByProject(projectId, startDate)

        List<skills.controller.result.model.TimestampCountItem> countsPerDay = []
        startDate.upto(new Date().clearTime()) { Date theDate ->
            DayCountItem found = res.find({it.day.clearTime() == theDate})
            countsPerDay << new skills.controller.result.model.TimestampCountItem(value: theDate.time, count: found?.count ?: 0)
        }

        return countsPerDay
    }

    List<skills.controller.result.model.TimestampCountItem> getSubjectUsage(String projectId, String subjectId, Integer numDays) {
        Date startDate
        use (TimeCategory) {
            startDate = (numDays-1).days.ago
            startDate.clearTime()
        }

        List<DayCountItem> res = userPointsRepo.findDistinctUserCountsBySkillId(projectId, subjectId, startDate)

        List<skills.controller.result.model.TimestampCountItem> countsPerDay = []
        startDate.upto(new Date().clearTime()) { Date theDate ->
            DayCountItem found = res.find({it.day.clearTime() == theDate})
            countsPerDay << new skills.controller.result.model.TimestampCountItem(value: theDate.time, count: found?.count ?: 0)
        }

        return countsPerDay
    }

    List<skills.controller.result.model.TimestampCountItem> getBadgesPerDay(String projectId, String badgeId, Integer numDays) {
        Date startDate
        use (TimeCategory) {
            startDate = (numDays-1).days.ago
            startDate.clearTime()
        }

        List<DayCountItem> res = userAchievedRepo.countAchievementsForProjectPerDay(projectId, badgeId, SkillDef.ContainerType.Badge, startDate)

        List<skills.controller.result.model.TimestampCountItem> countsPerDay = []
        startDate.upto(new Date().clearTime()) { Date theDate ->
            DayCountItem found = res.find({
                it.day.clearTime() == theDate
            })
            countsPerDay << new skills.controller.result.model.TimestampCountItem(value: theDate.time, count: found?.count ?: 0)
        }

        return countsPerDay
    }

    List<skills.controller.result.model.LabelCountItem> getBadgesPerMonth(String projectId, String badgeId, Integer numMonths=6) {
        Date startDate
        use (TimeCategory) {
            startDate = (numMonths-1).months.ago
            startDate.clearTime()
        }

        List<UserAchievedLevelRepo.LabelCountInfo> res = userAchievedRepo.countAchievementsForProjectPerMonth(projectId, badgeId, SkillDef.ContainerType.Badge, startDate)

        List<skills.controller.result.model.LabelCountItem> countsPerMonth = []
        Month currentMonth = LocalDate.now().month
        Month startMonth = currentMonth - numMonths

        (1..numMonths).each {
            Month month = startMonth+it
            println "Month: $month"

            UserAchievedLevelRepo.LabelCountInfo found = res.find ({
                it.label == "${month.value}"
            })
            countsPerMonth << new skills.controller.result.model.LabelCountItem(value: month.getDisplayName(TextStyle.SHORT, Locale.US), count: found?.count ?: 0)
        }

        return countsPerMonth
    }

    List<skills.controller.result.model.LabelCountItem> getAchievementCountsPerSubject(String projectId) {
        List<UserAchievedLevelRepo.LabelCountInfo> res = userAchievedRepo.getUsageFacetedViaSubject(projectId, SkillDef.ContainerType.Subject)

        return res.collect {
            new skills.controller.result.model.LabelCountItem(value: it.label, count: it.count)
        }
    }

    List<skills.controller.result.model.LabelCountItem> getAchievementCountsPerSkill(String projectId, String subjectId) {
        List<UserAchievedLevelRepo.LabelCountInfo> res = userAchievedRepo.getSubjectUsageFacetedViaSkill(projectId, subjectId, SkillDef.ContainerType.Subject)

        return res.collect {
            new skills.controller.result.model.LabelCountItem(value: it.label, count: it.count)
        }
    }

    List<skills.controller.result.model.LabelCountItem> getUserCountsPerLevel(String projectId, subjectId = null) {
        List<UsersPerLevel> levels = rankingLoader.getUserCountsPerLevel(projectId,true, subjectId)

        return levels.collect{
            new skills.controller.result.model.LabelCountItem(value: "Level ${it.level}", count: it.numUsers)
        }
    }

    skills.controller.result.model.TableResult loadUsersPage(String projectId, String query, PageRequest pageRequest) {
        skills.controller.result.model.TableResult result = new skills.controller.result.model.TableResult()
        Long totalProjectUsers = userPointsRepo.countDistinctUserIdByProjectIdAndUserIdLike(projectId, query)
        if (totalProjectUsers) {
            List<skills.controller.result.model.ProjectUser> projectUsers = userPointsRepo.findDistinctProjectUsersAndUserIdLike(projectId, query, pageRequest)
            result.data = projectUsers
            result.count = totalProjectUsers
        }
        return result
    }

    skills.controller.result.model.TableResult loadUsersPage(String projectId, List<String> skillIds, String query, PageRequest pageRequest) {
        skills.controller.result.model.TableResult result = new skills.controller.result.model.TableResult()
        if (!skillIds) {
            return result
        }
        Long totalProjectUsers = userPointsRepo.countDistinctUserIdByProjectIdAndSkillIdInAndUserIdLike(projectId, skillIds, query)
        if (totalProjectUsers) {
            List<skills.controller.result.model.ProjectUser> projectUsers = userPointsRepo.findDistinctProjectUsersByProjectIdAndSkillIdInAndUserIdLike(projectId, skillIds, query, pageRequest)
            result.data = projectUsers
            result.count = totalProjectUsers
        }
        return result
    }
}
