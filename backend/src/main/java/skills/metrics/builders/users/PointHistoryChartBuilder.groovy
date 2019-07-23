package skills.metrics.builders.users

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.AdminUsersService
import skills.skillLoading.SkillsLoader

@Component('user-PointHistoryChartBuilder')
@CompileStatic
class PointHistoryChartBuilder implements skills.metrics.builders.MetricsChartBuilder {

    static final Integer NUM_DAYS_DEFAULT = 365

    final Integer displayOrder = 0

    @Autowired
    AdminUsersService adminUsersService

    @Autowired
    SkillsLoader skillsLoader

    @Override
    skills.metrics.model.Section getSection() {
        return skills.metrics.model.Section.users
    }

    @Override
    skills.metrics.model.MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {
        Integer numDays = skills.metrics.ChartParams.getIntValue(props, skills.metrics.ChartParams.NUM_DAYS, NUM_DAYS_DEFAULT)
        assert numDays > 1, "Property [${skills.metrics.ChartParams.NUM_DAYS}] with value [${numDays}] must be greater than 1"

        String userId = skills.metrics.ChartParams.getValue(props, skills.metrics.ChartParams.SECTION_ID)
        assert userId, "userId must be specified via ${skills.metrics.ChartParams.SECTION_ID} url param"

        List<skills.controller.result.model.CountItem> dataItems = []
        if (loadData) {
            dataItems = skillsLoader.loadPointHistorySummary(projectId, userId, numDays).pointsHistory.collect {
                new skills.controller.result.model.TimestampCountItem(value: it.dayPerformed.time, count: it.points)
            } as List<skills.controller.result.model.CountItem>
        }

        skills.metrics.model.MetricsChart metricsChart = new skills.metrics.model.MetricsChart(
                chartType: skills.metrics.model.ChartType.Area,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<skills.metrics.model.ChartOption, Object> getChartOptions() {
        Map<skills.metrics.model.ChartOption, Object> chartOptions = [
                (skills.metrics.model.ChartOption.title)         : 'Point History',
                (skills.metrics.model.ChartOption.xAxisType)     : 'datetime',
//                (ChartOption.yAxisLabel) : '# of Points',
                (skills.metrics.model.ChartOption.dataLabel)     : 'Points',
                (skills.metrics.model.ChartOption.showDataLabels): false,
        ] as Map<skills.metrics.model.ChartOption, Object>
        return chartOptions
    }
}
