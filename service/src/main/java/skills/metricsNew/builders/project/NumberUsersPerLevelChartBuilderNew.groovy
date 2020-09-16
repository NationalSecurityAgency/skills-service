package skills.metricsNew.builders.project

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.LabelCountItem
import skills.metricsNew.builders.MetricsChartBuilder
import skills.services.AdminUsersService

@Component
class NumberUsersPerLevelChartBuilderNew implements MetricsChartBuilder {

    @Autowired
    AdminUsersService adminUsersService

    @Override
    String getId() {
        return "numUsersPerLevelChartBuilder"
    }

    @Override
    def build(String projectId, String chartId, Map<String, String> props) {
        List<LabelCountItem> dataItems = adminUsersService.getUserCountsPerLevel(projectId)
        return dataItems
    }
}
