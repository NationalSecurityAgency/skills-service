package skills.metrics.builders.projects

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.AdminUsersService

@Component
@CompileStatic
class AchievedSkillsChartBuilder implements skills.metrics.builders.MetricsChartBuilder {

    final Integer displayOrder = 2

    @Autowired
    AdminUsersService adminUsersService

    @Override
    skills.metrics.model.Section getSection() {
        return skills.metrics.model.Section.projects
    }

    @Override
    skills.metrics.model.MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {
        List<skills.controller.result.model.CountItem> dataItems = (loadData ? adminUsersService.getAchievementCountsPerSubject(projectId, 5) : []) as List<skills.controller.result.model.CountItem>

        skills.metrics.model.MetricsChart metricsChart = new skills.metrics.model.MetricsChart(
                chartType: skills.metrics.model.ChartType.HorizontalBar,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<skills.metrics.model.ChartOption, Object> getChartOptions() {
        Map<skills.metrics.model.ChartOption, Object> chartOptions = [
                (skills.metrics.model.ChartOption.title)            : 'Achieved Skills By Subject (for ALL users)',
                (skills.metrics.model.ChartOption.showDataLabels)   : true,
                (skills.metrics.model.ChartOption.dataLabel)        : 'Achieved Skills',
                (skills.metrics.model.ChartOption.distributed)      : true,
                (skills.metrics.model.ChartOption.dataLabelPosition): 'top',
                (skills.metrics.model.ChartOption.sort)             : 'asc',
                (skills.metrics.model.ChartOption.palette)          : 'palette2',
        ] as Map<skills.metrics.model.ChartOption, Object>
        return chartOptions
    }
}
