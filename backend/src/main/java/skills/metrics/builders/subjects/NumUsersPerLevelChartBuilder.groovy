package skills.metrics.builders.subjects

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.AdminUsersService

@Component('subjects-NumUsersPerLevelChartBuilder')
@CompileStatic
class NumUsersPerLevelChartBuilder implements skills.metrics.builders.MetricsChartBuilder {

    final Integer displayOrder = 1

    @Autowired
    AdminUsersService adminUsersService

    @Override
    skills.metrics.model.Section getSection() {
        return skills.metrics.model.Section.subjects
    }

    @Override
    skills.metrics.model.MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {

        String subjectId = skills.metrics.ChartParams.getValue(props, skills.metrics.ChartParams.SECTION_ID)
        assert subjectId, "subjectId must be specified via ${skills.metrics.ChartParams.SECTION_ID} url param"

        List<skills.controller.result.model.CountItem> dataItems = (loadData ? adminUsersService.getUserCountsPerLevel(projectId, subjectId) : []) as List<skills.controller.result.model.CountItem>

        skills.metrics.model.MetricsChart metricsChart = new skills.metrics.model.MetricsChart(
                chartType: skills.metrics.model.ChartType.VerticalBar,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<skills.metrics.model.ChartOption, Object> getChartOptions() {
        Map<skills.metrics.model.ChartOption, Object> chartOptions = [
                (skills.metrics.model.ChartOption.title)         : 'Number Users for each Level',
                (skills.metrics.model.ChartOption.showDataLabels): false,
//                (ChartOption.sort)           : 'asc',
        ] as Map<skills.metrics.model.ChartOption, Object>
        return chartOptions
    }
}
