package skills.service.metrics.builders.badges

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

@Component('badges-AchievedPerMonthChartBuilder')
@CompileStatic
class AchievedPerMonthChartBuilder implements MetricsChartBuilder {

    static final Integer NUM_MONTHS_DEFAULT = 6

    final Integer displayOrder = 1

    @Autowired
    AdminUsersService adminUsersService

    @Override
    Section getSection() {
        return Section.badges
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {
        Integer numMonths = ChartParams.getIntValue(props, ChartParams.NUM_MONTHS, NUM_MONTHS_DEFAULT)
        assert numMonths > 1, "Property [${ChartParams.NUM_MONTHS}] with value [${numMonths}] must be greater than 1"

        String badgeId = ChartParams.getValue(props, ChartParams.SECTION_ID)
        assert badgeId, "badgeId must be specified via ${ChartParams.SECTION_ID} url param"

        List<CountItem> dataItems = (loadData ? adminUsersService.getBadgesPerMonth(projectId, badgeId, numMonths) : []) as List<CountItem>

        MetricsChart metricsChart = new MetricsChart(
                chartType: ChartType.VerticalBar,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)      : 'Distinct # of Users per Month',
                (ChartOption.yAxisLabel) : 'Distinct # of Users',
                (ChartOption.dataLabel)  : 'Distinct Users',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
