package skills.service.metrics.builders.projects

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.service.controller.result.model.CountItem
import skills.service.datastore.services.AdminUsersService
import skills.service.metrics.builders.MetricsChartBuilder
import skills.service.metrics.model.ChartOption
import skills.service.metrics.model.ChartType
import skills.service.metrics.model.MetricsChart
import skills.service.metrics.model.Section

@Component
@CompileStatic
class ContinuedUsageChartBuilder implements MetricsChartBuilder {

    final Integer displayOrder = 6

    @Autowired
    AdminUsersService adminUsersService

    @Override
    Section getSection() {
        return Section.Projects
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {
        List<CountItem> dataItems = [] //(loadData ? adminUsersService.getUserCountsPerLevel(projectId) : []) as List<CountItem>

        MetricsChart metricsChart = new MetricsChart(
                chartType: ChartType.VerticalBarChart,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)       : 'Continued Usage',
                (ChartOption.subtitle)    : 'Do they stick around?',
                (ChartOption.icon)        : 'fa fa-suitcase',
                (ChartOption.description) : 'Once a particular skill is achieve, do users continue using that skill?',
                (ChartOption.sort)        : 'asc',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
