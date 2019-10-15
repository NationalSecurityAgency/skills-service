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
class SkillCountPerProjectBuilder implements MetricsChartBuilder{

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
        return 9
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData) {

        def projectResults = projService.getProjects()
        if (projectResults.size() < 2){
            return null
        }

        List<CountItem> chartData = []

        projectResults.each{
            def item = new LabelCountItem(count: (int) projService.countNumberOfSkills(it.projectId), value: it.name)
            chartData.add(item)
        }

        MetricsChart chart = new MetricsChart(
                dataItems: chartData,
                chartType: ChartType.VerticalBar,
                chartOptions: getChartOptions()
        )

        return chart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)         : 'Number of Skills per project',
                (ChartOption.showDataLabels): true,
                (ChartOption.sort)          : 'asc',
                (ChartOption.dataLabel)     : 'Number of Skills'
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
