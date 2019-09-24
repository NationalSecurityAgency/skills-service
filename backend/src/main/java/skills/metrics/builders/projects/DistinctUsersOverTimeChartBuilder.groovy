package skills.metrics.builders.projects

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.CountItem
import skills.metrics.ChartParams
import skills.metrics.builders.MetricsChartBuilder
import skills.metrics.model.ChartOption
import skills.metrics.model.ChartType
import skills.metrics.model.MetricsChart
import skills.metrics.model.Section
import skills.services.AdminUsersService

@Component
@CompileStatic
class DistinctUsersOverTimeChartBuilder implements MetricsChartBuilder {

    static final Integer NUM_DAYS_DEFAULT = 120

    final Integer displayOrder = 0

    @Autowired
    AdminUsersService adminUsersService

    @Override
    Section getSection() {
        return Section.projects
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {
        Integer numDays = ChartParams.getIntValue(props, ChartParams.NUM_DAYS, NUM_DAYS_DEFAULT)
        assert numDays > 1, "Property [${ChartParams.NUM_DAYS}] with value [${numDays}] must be greater than 1"

        List<CountItem> dataItems = (loadData ? adminUsersService.getProjectUsage(projectId, numDays) : []) as List<CountItem>

        MetricsChart metricsChart = new MetricsChart(
                chartType: ChartType.Line,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)     : 'Distinct # of Users over Time',
                (ChartOption.xAxisType) : 'datetime',
                (ChartOption.yAxisLabel): 'Distinct # of Users',
                (ChartOption.dataLabel) : 'Distinct Users',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
