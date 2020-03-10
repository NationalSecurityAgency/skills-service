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
package skills.metrics.builders.badges

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.result.model.CountItem
import skills.metrics.ChartParams
import skills.metrics.model.ChartOption
import skills.metrics.model.ChartType
import skills.metrics.model.MetricsChart
import skills.metrics.model.Section
import skills.services.AdminUsersService

@Component('badges-AchievedPerMonthChartBuilder')
@CompileStatic
class AchievedPerMonthChartBuilder implements skills.metrics.builders.MetricsChartBuilder {

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
        Integer numMonths = skills.metrics.ChartParams.getIntValue(props, skills.metrics.ChartParams.NUM_MONTHS, NUM_MONTHS_DEFAULT)
        assert numMonths > 1, "Property [${skills.metrics.ChartParams.NUM_MONTHS}] with value [${numMonths}] must be greater than 1"

        String badgeId = ChartParams.getValue(props, ChartParams.SECTION_ID)
        assert badgeId, "badgeId must be specified via ${skills.metrics.ChartParams.SECTION_ID} url param"

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
                (ChartOption.title)     : 'Distinct # of Users per Month',
                (ChartOption.yAxisLabel): 'Distinct # of Users',
                (ChartOption.dataLabel) : 'Distinct Users',
        ] as Map<ChartOption, Object>
        return chartOptions
    }
}
