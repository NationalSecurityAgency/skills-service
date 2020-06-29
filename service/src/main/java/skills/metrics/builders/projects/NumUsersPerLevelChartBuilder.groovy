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
package skills.metrics.builders.projects

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
import skills.services.AdminUsersService

@Component
@CompileStatic
class NumUsersPerLevelChartBuilder implements MetricsChartBuilder {

    final Integer displayOrder = 1

    @Autowired
    AdminUsersService adminUsersService

    @Override
    Section getSection() {
        return Section.projects
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData = true) {
        List<LabelCountItem> dataItems = loadData ? adminUsersService.getUserCountsPerLevel(projectId) : []

        MetricsChart metricsChart = new MetricsChart(
                chartType: ChartType.VerticalBar,
                dataItems: dataItems as List<CountItem>,
                chartOptions: getChartOptions(),
        )
        return metricsChart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)         : 'Number Users for each Level',
                (ChartOption.showDataLabels): false,
                (ChartOption.sort)          : 'asc',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
