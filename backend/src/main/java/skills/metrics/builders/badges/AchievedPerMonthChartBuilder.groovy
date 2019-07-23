package skills.metrics.builders.badges

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.services.AdminUsersService

@Component('badges-AchievedPerMonthChartBuilder')
@CompileStatic
class AchievedPerMonthChartBuilder implements skills.metrics.builders.MetricsChartBuilder {

    static final Integer NUM_MONTHS_DEFAULT = 6

    final Integer displayOrder = 1

    @Autowired
    AdminUsersService adminUsersService

    @Override
    skills.metrics.model.Section getSection() {
        return skills.metrics.model.Section.badges
    }

    @Override
    skills.metrics.model.MetricsChart build(String projectId, Map<String, String> props, boolean loadData=true) {
        Integer numMonths = skills.metrics.ChartParams.getIntValue(props, skills.metrics.ChartParams.NUM_MONTHS, NUM_MONTHS_DEFAULT)
        assert numMonths > 1, "Property [${skills.metrics.ChartParams.NUM_MONTHS}] with value [${numMonths}] must be greater than 1"

        String badgeId = skills.metrics.ChartParams.getValue(props, skills.metrics.ChartParams.SECTION_ID)
        assert badgeId, "badgeId must be specified via ${skills.metrics.ChartParams.SECTION_ID} url param"

        List<skills.controller.result.model.CountItem> dataItems = (loadData ? adminUsersService.getBadgesPerMonth(projectId, badgeId, numMonths) : []) as List<skills.controller.result.model.CountItem>

        skills.metrics.model.MetricsChart metricsChart = new skills.metrics.model.MetricsChart(
                chartType: skills.metrics.model.ChartType.VerticalBar,
                dataItems: dataItems,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<skills.metrics.model.ChartOption, Object> getChartOptions() {
        Map<skills.metrics.model.ChartOption, Object> chartOptions = [
                (skills.metrics.model.ChartOption.title)     : 'Distinct # of Users per Month',
                (skills.metrics.model.ChartOption.yAxisLabel): 'Distinct # of Users',
                (skills.metrics.model.ChartOption.dataLabel) : 'Distinct Users',
        ] as Map<skills.metrics.model.ChartOption, Object>
        return chartOptions
    }
}
