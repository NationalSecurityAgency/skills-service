package skills.metricsNew.builders.project


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.CountItem
import skills.metrics.ChartParams
import skills.metricsNew.builders.MetricsChartBuilder
import skills.services.AdminUsersService

@Component
class DistinctUsersOverTimeChartBuilderNew implements  MetricsChartBuilder{

    static final Integer NUM_DAYS_DEFAULT = 120

    @Autowired
    AdminUsersService adminUsersService

    @Override
    String getId() {
        return "distinctUsersOverTimeForProject"
    }

    @Override
    def build(String projectId, String chartId, Map<String, String> props) {
        Integer numDays = ChartParams.getIntValue(props, ChartParams.NUM_DAYS, NUM_DAYS_DEFAULT)
        assert numDays > 1, "Property [${ChartParams.NUM_DAYS}] with value [${numDays}] must be greater than 1"

        List<CountItem> dataItems = adminUsersService.getProjectUsage(projectId, numDays)

        return dataItems;
    }
}
