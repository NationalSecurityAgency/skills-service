package skills.metrics

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.metrics.model.ChartOption
import skills.metrics.model.MetricsChart
import skills.metrics.builders.MetricsChartBuilder
import skills.metrics.model.Section

import javax.annotation.PostConstruct

@Service
@Slf4j
@CompileStatic
class MetricsService {

    @Autowired
    ApplicationContext appContext

    Map<Section, List<MetricsChartBuilder>> chartBuildersMap

    @PostConstruct
    void init() {
        chartBuildersMap = appContext.getBeansOfType(MetricsChartBuilder).values().groupBy { it.section }
        chartBuildersMap.each { section, builders ->
            assert builders.size() == builders.collect { it.displayOrder }.unique().size()
            builders.sort { it.displayOrder }
        }
    }

    List<MetricsChart> loadChartsForSection(Section section, String projectId, Map<String, String> props) {
        int loadFirst = ChartParams.getIntValue(props, ChartParams.LOAD_DATA_FOR_FIRST, -1)
        int i = 0
        return chartBuildersMap.get(section)?.collect { builder ->
            boolean loadData = loadFirst < 0 || ++i <= loadFirst
            MetricsChart chart = builder.build(projectId, props, loadData)
            if(chart != null) {
                chart.dataLoaded = loadData
                if (loadData) {
                    clearDataItemsIfAllValuesZero(chart)
                }
                chart.chartOptions.put(ChartOption.chartBuilderId, builder.class.name)
            }
            return chart
        }?.findAll { it != null }
    }

    MetricsChart loadChartForSection(String builderId, Section section, String projectId, Map<String, String> props) {
        MetricsChartBuilder chartBuilder = chartBuildersMap.get(section).find { it.class.name == builderId}
        if (!chartBuilder) {
            SkillException ex = new SkillException("Unknown chart builder id [$builderId]")
            ex.errorCode = ErrorCode.BadParam
            throw ex
        }
        MetricsChart chart = chartBuilder.build(projectId, props, true)
        if (chart != null) {
            chart.chartOptions.put(ChartOption.chartBuilderId, chartBuilder.class.name)
            chart.dataLoaded = true
            clearDataItemsIfAllValuesZero(chart)
        }
        return chart
    }

    private void clearDataItemsIfAllValuesZero(MetricsChart chart) {
        if (chart.dataItems) {
            if (chart.dataItems?.collect({ it.count })?.unique() == [0]) {
                chart.dataItems.clear()
            }
        }
    }
}
