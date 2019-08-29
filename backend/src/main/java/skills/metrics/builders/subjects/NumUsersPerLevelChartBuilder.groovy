package skills.metrics.builders.subjects

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.CountItem
import skills.controller.result.model.LabelCountItem
import skills.metrics.model.ChartOption
import skills.metrics.model.ChartType
import skills.metrics.model.MetricsChart
import skills.metrics.model.Section
import skills.services.AdminUsersService

@Component('subjects-NumUsersPerLevelChartBuilder')
@CompileStatic
class NumUsersPerLevelChartBuilder implements skills.metrics.builders.MetricsChartBuilder {

    final Integer displayOrder = 1

    @Autowired
    AdminUsersService adminUsersService

    @Override
    Section getSection() {
        return Section.subjects
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {

        String subjectId = skills.metrics.ChartParams.getValue(props, skills.metrics.ChartParams.SECTION_ID)
        assert subjectId, "subjectId must be specified via ${skills.metrics.ChartParams.SECTION_ID} url param"

        List<LabelCountItem> dataItems = loadData ? adminUsersService.getUserCountsPerLevel(projectId, subjectId) : []

        MetricsChart metricsChart = new MetricsChart(
                chartType: ChartType.VerticalBar,
                dataItems: dataItems as List<CountItem>,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)         : 'Number Users for each Level',
                (ChartOption.showDataLabels): false,
//                (ChartOption.sort)           : 'asc',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
