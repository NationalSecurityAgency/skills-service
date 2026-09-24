/**
 * Copyright 2026 SkillTree
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
package skills.intTests.metrics

import groovy.json.JsonSlurper
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.metrics.builders.MetricsPagingParamsHelper
import skills.metrics.builders.MetricsParams

import static skills.intTests.utils.SkillsFactory.createProject

class MetricsPagingValidationSpec extends DefaultIntSpec {

    String metricsId = "userAchievementsChartBuilder"

    def "malformed numeric metrics paging values return BadParam - #paramId=#invalidValue"() {
        def proj = createProject()
        skillsService.createProject(proj)

        Map props = [
                (MetricsPagingParamsHelper.PROP_CURRENT_PAGE): "1",
                (MetricsPagingParamsHelper.PROP_PAGE_SIZE): "5",
                (MetricsPagingParamsHelper.PROP_SORT_DESC): "false",
                (MetricsPagingParamsHelper.PROP_SORT_BY): "userName",
                (MetricsParams.P_ACHIEVEMENT_TYPES): MetricsParams.ACHIEVEMENT_TYPE_OVERALL
        ]
        props[paramId] = invalidValue

        when:
        skillsService.getMetricsData(proj.projectId, metricsId, props)

        then:
        SkillsClientException e = thrown()
        e.httpStatus == HttpStatus.BAD_REQUEST

        def body = new JsonSlurper().parseText(e.resBody)
        body.errorCode == "BadParam"
        body.explanation == "Metrics[${metricsId}]: ${paramId} must be a valid integer. Provided [${invalidValue}]"

        where:
        paramId                                      | invalidValue
        MetricsPagingParamsHelper.PROP_CURRENT_PAGE | "abc"
        MetricsPagingParamsHelper.PROP_PAGE_SIZE    | "abc"
        MetricsPagingParamsHelper.PROP_CURRENT_PAGE | "2147483648"
        MetricsPagingParamsHelper.PROP_PAGE_SIZE    | "2147483648"
    }
}
