package skills.service.metrics

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import skills.service.metrics.model.ChartOption
import skills.service.metrics.model.MetricsChart
import skills.service.metrics.builders.MetricsChartBuilder
import skills.service.metrics.model.Section

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
            chart.dataLoaded = loadData
            chart.chartOptions.put(ChartOption.chartBuilderId, builder.class.name)
            return chart
        }
    }

    MetricsChart loadChartForSection(String builderId, Section section, String projectId, Map<String, String> props) {
        MetricsChartBuilder chartBuilder = chartBuildersMap.get(section).find { it.class.name == builderId}
        assert chartBuilder, "Unknown chart builder id [$builderId]"
        MetricsChart chart = chartBuilder.build(projectId, props, true)
        chart.dataLoaded = true
        return chart
    }
}
