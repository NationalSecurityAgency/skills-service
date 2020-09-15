/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
