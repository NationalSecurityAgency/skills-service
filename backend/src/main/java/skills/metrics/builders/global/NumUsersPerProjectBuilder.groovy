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
package skills.metrics.builders.global

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.CountItem
import skills.controller.result.model.LabelCountItem
import skills.controller.result.model.ProjectResult
import skills.metrics.builders.MetricsChartBuilder
import skills.metrics.model.ChartOption
import skills.metrics.model.ChartType
import skills.metrics.model.MetricsChart
import skills.metrics.model.Section
import skills.services.AdminUsersService
import skills.services.admin.ProjAdminService

@Component
@CompileStatic
class NumUsersPerProjectBuilder implements MetricsChartBuilder{

    @Autowired
    AdminUsersService usersService

    @Autowired
    ProjAdminService projAdminService

    @Override
    Section getSection() {
        return Section.global
    }

    @Override
    Integer getDisplayOrder() {
        return 8
    }

    @Override
    MetricsChart build(String projectId, Map<String, String> props, boolean loadData) {

        List<ProjectResult> projectResults = projAdminService.getProjects()
        if (projectResults.size() < 2){
            return null
        }

        List<CountItem> chartData = []
        projectResults.each {
            LabelCountItem item = new LabelCountItem()
            item.value = it.name
            item.count = (int)usersService.countTotalProjUsers(it.projectId)
            chartData.add(item)
        }

        MetricsChart chart = new MetricsChart(
                dataItems: chartData,
                chartType: ChartType.HorizontalBar,
                chartOptions: getChartOptions()
        )

        return chart
    }

    private Map<ChartOption, Object> getChartOptions() {
        Map<ChartOption, Object> chartOptions = [
                (ChartOption.title)         : 'Number of users per project',
                (ChartOption.showDataLabels): true,
                (ChartOption.sort)          : 'asc',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
