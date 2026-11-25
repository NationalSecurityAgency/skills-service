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
package skills.metrics.builders

import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import spock.lang.Specification

import java.time.LocalDate

class MetricsParamsSpec extends Specification {

    String chartId = "someChart"
    String projectId = "proj1"

    def "malformed fromDayFilter is a BadParam instead of an unexpected error - #value"() {
        when:
        MetricsParams.getFromDayFilter(projectId, chartId, [(MetricsParams.P_FROM_DAY_FILTER): value])

        then:
        SkillException e = thrown()
        e.errorCode == ErrorCode.BadParam
        e.projectId == projectId
        e.message == "Metrics[someChart]: fromDayFilter must be a valid date in the yyyy-MM-dd format. Provided [${value}]"

        where:
        value << ["2024-1-5", "abc", "2024-02-30", "2024-01-05T10:00:00"]
    }

    def "malformed toDayFilter is a BadParam instead of an unexpected error - #value"() {
        when:
        MetricsParams.getToDayFilter(projectId, chartId, [(MetricsParams.P_TO_DAY_FILTER): value])

        then:
        SkillException e = thrown()
        e.errorCode == ErrorCode.BadParam
        e.message == "Metrics[someChart]: toDayFilter must be a valid date in the yyyy-MM-dd format. Provided [${value}]"

        where:
        value << ["2024-1-5", "abc", "2024-13-01"]
    }

    def "malformed minLevel is a BadParam instead of an unexpected error - #value"() {
        when:
        MetricsParams.getMinLevel(projectId, chartId, [(MetricsParams.P_MIN_LEVEL): value])

        then:
        SkillException e = thrown()
        e.errorCode == ErrorCode.BadParam
        e.message == "Metrics[someChart]: minLevel must be a valid integer. Provided [${value}]"

        where:
        value << ["abc", "1.5", "2147483648"]
    }

    def "malformed start timestamp is a BadParam instead of an unexpected error - #value"() {
        when:
        MetricsParams.getStart(projectId, chartId, [(MetricsParams.P_START_TIMESTAMP): value])

        then:
        SkillException e = thrown()
        e.errorCode == ErrorCode.BadParam
        e.message == "Metrics[someChart]: start must be a valid timestamp in epoch milliseconds. Provided [${value}]"

        where:
        value << ["abc", "2024-01-05", "9223372036854775808"]
    }

    def "valid and missing values keep their existing behavior"() {
        expect:
        MetricsParams.getFromDayFilter(projectId, chartId, [(MetricsParams.P_FROM_DAY_FILTER): "2024-01-05"]) == LocalDate.parse("2024-01-05").toDate()
        MetricsParams.getFromDayFilter(projectId, chartId, [:]) == new Date(1)
        MetricsParams.getToDayFilter(projectId, chartId, [(MetricsParams.P_TO_DAY_FILTER): "2024-01-05"]).after(LocalDate.parse("2024-01-05").toDate())
        MetricsParams.getMinLevel(projectId, chartId, [(MetricsParams.P_MIN_LEVEL): "3"]) == 3
        MetricsParams.getMinLevel(projectId, chartId, [:]) == -1
        MetricsParams.getStart(projectId, chartId, [(MetricsParams.P_START_TIMESTAMP): "${System.currentTimeMillis()}".toString()]).time > 0
    }
}
