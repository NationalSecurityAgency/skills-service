package skills.metricsNew.builders.skills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
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

    def build(String projectId, String chartId, Map<String, String> props) {
        def res = userAchievedRepo.calculateSkillUsage1(projectId)
        res.each {
            println it
        }

        return res
    }
}
