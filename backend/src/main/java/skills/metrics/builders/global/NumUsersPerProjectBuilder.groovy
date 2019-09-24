package skills.metrics.builders.global

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.CountItem
import skills.controller.result.model.LabelCountItem
import skills.metrics.builders.MetricsChartBuilder
import skills.metrics.model.ChartOption
import skills.metrics.model.ChartType
import skills.metrics.model.MetricsChart
import skills.metrics.model.Section
import skills.services.AdminProjService
import skills.services.AdminUsersService

@Component
@CompileStatic
class NumUsersPerProjectBuilder implements MetricsChartBuilder{

    @Autowired
    AdminUsersService usersService

    @Autowired
    AdminProjService projService

    @Override
    Section getSection() {
        return Section.global
    }

    @Override
    Integer getDisplayOrder() {
        return 8
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData) {

        def projectResults = projService.getProjects()

        List<CountItem> chartData = []
        projectResults.each{
            LabelCountItem item = new LabelCountItem()
            item.value = it.name
            item.count = (int)usersService.countTotalProjUsers(it.projectId)
            chartData.add(item)
        }

        MetricsChart chart = new MetricsChart(
                dataItems: chartData,
                chartType: ChartType.HorizontalBar,
                chartOptions: getChartOptions()
        )

        return chart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)         : 'Number of users per project',
                (ChartOption.showDataLabels): true,
                (ChartOption.sort)          : 'asc',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
