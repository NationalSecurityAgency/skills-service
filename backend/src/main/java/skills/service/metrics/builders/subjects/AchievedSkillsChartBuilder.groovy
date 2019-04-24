package skills.service.metrics.builders.subjects


import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.service.controller.result.model.CountItem
import skills.service.datastore.services.AdminUsersService
import skills.service.metrics.ChartParams
import skills.service.metrics.builders.MetricsChartBuilder
import skills.service.metrics.model.ChartOption
import skills.service.metrics.model.ChartType
import skills.service.metrics.model.MetricsChart
import skills.service.metrics.model.Section

@Component('subjects-AchievedSkillsChartBuilder')
@CompileStatic
class AchievedSkillsChartBuilder implements MetricsChartBuilder {

    final Integer displayOrder = 2

    @Autowired
    AdminUsersService adminUsersService

    @Override
    Section getSection() {
        return Section.subjects
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {

        String subjectId = ChartParams.getValue(props, ChartParams.SECTION_ID)
        assert subjectId, "subjectId must be specified via ${ChartParams.SECTION_ID} url param"

        List<CountItem> dataItems = (loadData ? adminUsersService.getAchievementCountsPerSkill(projectId, subjectId) : []) as List<CountItem>

        MetricsChart metricsChart = new MetricsChart(
                chartType: ChartType.HorizontalBarChart,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)             : 'Achieved Skills for Subject (for ALL users)',
                (ChartOption.showDataLabels)    : true,
                (ChartOption.dataLabel)         : 'Achieved Skills',
                (ChartOption.distributed)       : true,
                (ChartOption.dataLabelPosition) : 'top',
                (ChartOption.sort)              : 'asc',
                (ChartOption.palette)           : 'palette2',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
