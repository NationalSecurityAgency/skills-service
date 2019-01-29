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
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPointsRepo

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

        List<UserPointsRepo.UsageItem> res = userPointsRepo.findDistinctUserCountsByProject(projectId, startDate)

        List<TimestampCountItem> countsPerDay = []
        startDate.upto(new Date().clearTime()) { Date theDate ->
            UserPointsRepo.UsageItem found = res.find({it.day.clearTime() == theDate})
            countsPerDay << new TimestampCountItem(timestamp: theDate.time, count: found?.numUsers ?: 0)
        }

        return countsPerDay
    }

    List<LabelCountItem> getAchievementCountsPerSubject(String projectId) {
        List<UserAchievedLevelRepo.LabelCountInfo> res = userAchievedRepo.getUsageFacetedViaSubject(projectId, SkillDef.ContainerType.Subject)

        return res.collect {
            new LabelCountItem(label: it.label, count: it.count)
        }
    }

    List<LabelCountItem> getUserCountsPerLevel(String projectId) {
        List<UsersPerLevel> levels = rankingLoader.getUserCountsPerLevel(projectId, true)

        return levels.collect{
            new LabelCountItem(label: "Level ${it.level}", count: it.numUsers)
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
