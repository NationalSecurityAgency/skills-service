package skills.service.metrics.builders.users

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.service.controller.result.model.CountItem
import skills.service.controller.result.model.TimestampCountItem
import skills.service.datastore.services.AdminUsersService
import skills.service.metrics.ChartParams
import skills.service.metrics.builders.MetricsChartBuilder
import skills.service.metrics.model.ChartOption
import skills.service.metrics.model.ChartType
import skills.service.metrics.model.MetricsChart
import skills.service.metrics.model.Section
import skills.service.skillLoading.SkillsLoader

@Component('user-PointHistoryChartBuilder')
@CompileStatic
class PointHistoryChartBuilder implements MetricsChartBuilder {

    static final Integer NUM_DAYS_DEFAULT = 365

    final Integer displayOrder = 0

    @Autowired
    AdminUsersService adminUsersService

    @Autowired
    SkillsLoader skillsLoader

    @Override
    Section getSection() {
        return Section.users
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {
        Integer numDays = ChartParams.getIntValue(props, ChartParams.NUM_DAYS, NUM_DAYS_DEFAULT)
        assert numDays > 1, "Property [${ChartParams.NUM_DAYS}] with value [${numDays}] must be greater than 1"

        String userId = ChartParams.getValue(props, ChartParams.SECTION_ID)
        assert userId, "userId must be specified via ${ChartParams.SECTION_ID} url param"

        List<CountItem> dataItems = []
        if (loadData) {
            dataItems = skillsLoader.loadPointHistorySummary(projectId, userId, numDays).pointsHistory.collect {
                new TimestampCountItem(value: it.dayPerformed.time, count: it.points)
            } as List<CountItem>
        }

        MetricsChart metricsChart = new MetricsChart(
                chartType: ChartType.Area,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)      : 'Point History',
                (ChartOption.xAxisType)  : 'datetime',
//                (ChartOption.yAxisLabel) : '# of Points',
                (ChartOption.dataLabel)  : 'Points',
                (ChartOption.showDataLabels)  : false,
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
