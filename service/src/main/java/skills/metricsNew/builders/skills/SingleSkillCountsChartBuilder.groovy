package skills.metricsNew.builders.skills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.metricsNew.builders.MetricsChartBuilder
import skills.metricsNew.builders.MetricsParams
import skills.storage.repos.UserAchievedLevelRepo
import skills.storage.repos.UserPerformedSkillRepo

@Component
@Slf4j
class SingleSkillCountsChartBuilder  implements MetricsChartBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Autowired
    UserPerformedSkillRepo userPerformedSkillRepo

    static class SingleSkillCounts {
        Integer numUsersAchieved
        Long lastAchieved
        Integer numUsersInProgress
    }

    @Override
    String getId() {
        return "singleSkillCountsChartBuilder"
    }

    SingleSkillCounts build(String projectId, String chartId, Map<String, String> props) {
        String skillId = MetricsParams.getSkillId(projectId, chartId, props);
        UserAchievedLevelRepo.SkillStatsItem skillStatsItem = userAchievedRepo.calculateNumAchievedAndLastAchieved(projectId, skillId)
        Integer numUsersIn = userPerformedSkillRepo.countDistinctUserIdByProjectIdAndSkillId(projectId, skillId)
        int numUsersAchieved = skillStatsItem.getNumUsersAchieved() ?: 0
        return new SingleSkillCounts(
                numUsersAchieved: numUsersAchieved,
                numUsersInProgress: numUsersIn - numUsersAchieved,
                lastAchieved: skillStatsItem.getLastAchieved() ? skillStatsItem.getLastAchieved().time : null)
    }
}
