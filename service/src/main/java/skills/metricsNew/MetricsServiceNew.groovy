package skills.metricsNew

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import skills.controller.exceptions.SkillException
import skills.metricsNew.builders.MetricsChartBuilder

import javax.annotation.PostConstruct

@Service
@Slf4j
@CompileStatic
class MetricsServiceNew {

    @Autowired
    ApplicationContext appContext

    final Map<String, MetricsChartBuilder> chartBuildersMap = [:]

    @PostConstruct
    void init() {
        Collection<MetricsChartBuilder> loadedBuilders = appContext.getBeansOfType(MetricsChartBuilder).values()
        loadedBuilders.each { MetricsChartBuilder builder ->
            assert !chartBuildersMap.get(builder.getId()), "Found more than 1 chart with id [${builder.id}]"
            chartBuildersMap.put(builder.getId(), builder)
        }
        log.info("Loaded [${chartBuildersMap.size()}] builders: ${chartBuildersMap.keySet().toList()}")
    }

    def loadChart(String projectId, String chartId, Map<String, String> props) {
        MetricsChartBuilder metricsChartBuilder = chartBuildersMap.get(chartId)
        if (!metricsChartBuilder) {
            throw new SkillException("Failed to find chart with id [${chartId}]", projectId)
        }

        return metricsChartBuilder.build(projectId, chartId, props)
    }

}
