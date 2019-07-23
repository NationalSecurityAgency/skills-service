package skills.metrics.builders.projects

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.AdminUsersService

@Component
@CompileStatic
class DistinctUsersOverTimeChartBuilder implements skills.metrics.builders.MetricsChartBuilder {

    static final Integer NUM_DAYS_DEFAULT = 120

    final Integer displayOrder = 0

    @Autowired
    AdminUsersService adminUsersService

    @Override
    skills.metrics.model.Section getSection() {
        return skills.metrics.model.Section.projects
    }

    @Override
    skills.metrics.model.MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {
        Integer numDays = skills.metrics.ChartParams.getIntValue(props, skills.metrics.ChartParams.NUM_DAYS, NUM_DAYS_DEFAULT)
        assert numDays > 1, "Property [${skills.metrics.ChartParams.NUM_DAYS}] with value [${numDays}] must be greater than 1"

        List<skills.controller.result.model.CountItem> dataItems = (loadData ? adminUsersService.getProjectUsage(projectId, numDays) : []) as List<skills.controller.result.model.CountItem>

        skills.metrics.model.MetricsChart metricsChart = new skills.metrics.model.MetricsChart(
                chartType: skills.metrics.model.ChartType.Line,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<skills.metrics.model.ChartOption, Object> getChartOptions() {
        Map<skills.metrics.model.ChartOption, Object> chartOptions = [
                (skills.metrics.model.ChartOption.title)     : 'Distinct # of Users over Time',
                (skills.metrics.model.ChartOption.xAxisType) : 'datetime',
                (skills.metrics.model.ChartOption.yAxisLabel): 'Distinct # of Users',
                (skills.metrics.model.ChartOption.dataLabel) : 'Distinct Users',
        ] as Map<skills.metrics.model.ChartOption, Object>
        return chartOptions
    }
}
