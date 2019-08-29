package skills.metrics.builders.subjects


import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import skills.controller.result.model.CountItem
import skills.metrics.model.ChartOption
import skills.metrics.model.ChartType
import skills.metrics.model.MetricsChart
import skills.metrics.model.Section
import skills.services.AdminUsersService

@Component('subjects-AchievedSkillsChartBuilder')
@CompileStatic
class AchievedSkillsChartBuilder implements skills.metrics.builders.MetricsChartBuilder {

    final Integer displayOrder = 2

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

        List<CountItem> dataItems = (loadData ? adminUsersService.getAchievementCountsPerSkill(projectId, subjectId, new PageRequest(0, 5, Sort.Direction.DESC, "countRes")) : []) as List<CountItem>
        // filter empty
        dataItems = dataItems.findAll({it.count > 0})

        MetricsChart metricsChart = new MetricsChart(
                chartType: ChartType.HorizontalBar,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)            : 'Top 5 Achieved Skills',
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
