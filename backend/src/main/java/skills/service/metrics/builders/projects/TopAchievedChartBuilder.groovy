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
class TopAchievedChartBuilder implements MetricsChartBuilder {

    final Integer displayOrder = 3

    @Autowired
    AdminUsersService adminUsersService

    @Override
    Section getSection() {
        return Section.projects
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {
        List<CountItem> dataItems = [] //(loadData ? adminUsersService.getUserCountsPerLevel(projectId) : []) as List<CountItem>

        MetricsChart metricsChart = new MetricsChart(
                chartType: ChartType.VerticalBar,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)       : 'Top Achieved',
                (ChartOption.subtitle)    : 'The Olympians of Skills',
                (ChartOption.icon)        : 'fa fa-trophy',
                (ChartOption.description) : 'This stat depicts most achieved skills. It will display top skills and number of users that achieved those skills.',
                (ChartOption.sort)        : 'asc',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
