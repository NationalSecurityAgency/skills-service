package skills.metricsNew.builders.skills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import skills.metricsNew.builders.MetricsChartBuilder
import skills.storage.repos.UserAchievedLevelRepo

@Component
@Slf4j
class SkillUsageNavigatorChartBuilder implements MetricsChartBuilder {

    @Autowired
    UserAchievedLevelRepo userAchievedRepo

    @Override
    String getId() {
        return "skillUsageNavigatorChartBuilder"
    }

    static class SkillUsageNavigatorItem {
        String skillId
        String skillName
        Integer numUserAchieved
        Integer numUsersInProgress
        Long lastReportedTimestamp
        Long lastAchievedTimestamp
    }

    List<SkillUsageNavigatorItem> build(String projectId, String chartId, Map<String, String> props) {
        def res = userAchievedRepo.findAllForSkillsNavigator(projectId)
        return res.collect {
            Integer numAchieved = it.getNumUserAchieved() ?: 0
            Integer numProgress = it.getNumUsersInProgress() ?: 0
            new SkillUsageNavigatorItem(
                    skillId: it.getSkillId(),
                    skillName: it.getSkillName(),
                    numUserAchieved: numAchieved,
                    numUsersInProgress: numProgress - numAchieved,
                    lastReportedTimestamp: it.getLastReported()?.time,
                    lastAchievedTimestamp: it.getLastAchieved()?.time
            )
        }
    }
}
