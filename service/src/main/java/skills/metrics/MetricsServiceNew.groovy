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
package skills.metrics

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import skills.controller.exceptions.SkillException
import skills.metrics.builders.GlobalMetricsBuilder
import skills.metrics.builders.ProjectMetricsBuilder

import javax.annotation.PostConstruct

@Service
@Slf4j
@CompileStatic
class MetricsServiceNew {

    @Autowired
    ApplicationContext appContext

    private final Map<String, ProjectMetricsBuilder> projectBuildersMap = [:]
    private final Map<String, GlobalMetricsBuilder> globalBuildersMap = [:]

    @PostConstruct
    void init() {
        loadProjectBuilders()
        loadGlobalBuilders()
    }

    private void loadProjectBuilders() {
        Collection<ProjectMetricsBuilder> loadedBuilders = appContext.getBeansOfType(ProjectMetricsBuilder).values()
        loadedBuilders.each { ProjectMetricsBuilder builder ->
            assert !projectBuildersMap.get(builder.getId()), "Found more than 1 chart with id [${builder.id}]"
            projectBuildersMap.put(builder.getId(), builder)
        }
        log.info("Loaded [${projectBuildersMap.size()}] builders: ${projectBuildersMap.keySet().toList()}")
    }

    private void loadGlobalBuilders() {
        Collection<GlobalMetricsBuilder> loadedBuilders = appContext.getBeansOfType(GlobalMetricsBuilder).values()
        loadedBuilders.each { GlobalMetricsBuilder builder ->
            assert !globalBuildersMap.get(builder.getId()), "Found more than 1 chart with id [${builder.id}]"
            globalBuildersMap.put(builder.getId(), builder)
        }
        log.info("Loaded [${globalBuildersMap.size()}] builders: ${globalBuildersMap.keySet().toList()}")
    }

    def loadProjectMetrics(String projectId, String metricsId, Map<String, String> props) {
        ProjectMetricsBuilder metricsChartBuilder = projectBuildersMap.get(metricsId)
        if (!metricsChartBuilder) {
            throw new SkillException("Failed to find metric with id [${metricsId}]", projectId)
        }

        return metricsChartBuilder.build(projectId, metricsId, props)
    }

    def loadGlobalMetrics(String metricsId, Map<String, String> props) {
        GlobalMetricsBuilder globalMetricsBuilder = globalBuildersMap.get(metricsId)
        if (!globalMetricsBuilder) {
            throw new SkillException("Failed to find global builder with id [${metricsId}]")
        }

        return globalMetricsBuilder.build(props)
    }

}
