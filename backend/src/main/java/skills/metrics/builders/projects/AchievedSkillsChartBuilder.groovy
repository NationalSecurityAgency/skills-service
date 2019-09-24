package skills.metrics.builders.projects

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.CountItem
import skills.metrics.builders.MetricsChartBuilder
import skills.metrics.model.ChartOption
import skills.metrics.model.ChartType
import skills.metrics.model.MetricsChart
import skills.metrics.model.Section
import skills.services.AdminUsersService

@Component
@CompileStatic
class AchievedSkillsChartBuilder implements MetricsChartBuilder {

    final Integer displayOrder = 2

    @Autowired
    AdminUsersService adminUsersService

    @Override
    Section getSection() {
        return Section.projects
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {
        List<CountItem> dataItems = (loadData ? adminUsersService.getAchievementCountsPerSubject(projectId, 5) : []) as List<CountItem>

        MetricsChart metricsChart = new MetricsChart(
                chartType: ChartType.HorizontalBar,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)                                 : 'Achieved Skills By Subject (for ALL users)',
                (ChartOption.showDataLabels)   : true,
                (ChartOption.dataLabel)        : 'Achieved Skills',
                (ChartOption.distributed)      : true,
                (ChartOption.dataLabelPosition): 'top',
                (ChartOption.sort)             : 'asc',
                (ChartOption.palette)          : 'palette2',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
