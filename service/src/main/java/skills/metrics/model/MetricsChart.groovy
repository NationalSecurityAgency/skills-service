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
package skills.metrics.model

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import skills.controller.result.model.CountItem

@CompileStatic
@Canonical
class MetricsChart {

    ChartType chartType
    boolean dataLoaded = false
    String icon

    Map<ChartOption, Object> chartOptions = [:]

    // LabelCountItem, TimestampCountItem, others maybe?
    List<CountItem> dataItems
}
