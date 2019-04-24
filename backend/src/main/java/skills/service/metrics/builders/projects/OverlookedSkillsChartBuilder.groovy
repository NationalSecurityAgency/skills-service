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
class OverlookedSkillsChartBuilder implements MetricsChartBuilder {

    final Integer displayOrder = 4

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
                chartType: ChartType.VerticalBarChart,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)       : 'Overlooked skills',
                (ChartOption.subtitle)    : 'Users seem to ignore these',
                (ChartOption.icon)        : 'fas fa-sad-tear',
                (ChartOption.description) : 'These skills have low to no usage at all. Explore these skills and see if they need to be improved or removed.',
                (ChartOption.sort)        : 'asc',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
