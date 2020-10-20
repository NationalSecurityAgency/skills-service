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
package skills.metricsNew.builders

import skills.controller.exceptions.SkillException

class MetricsParams {
    final static String P_SKILL_ID = "skillId"

    static String getSkillId(String projectId, String chartId, Map<String, String> props) {
        String sortBy = props[P_SKILL_ID]
        if (!sortBy) {
            throw new SkillException("Chart[${chartId}]: Must supply ${P_SKILL_ID} param", projectId)
        }
        return sortBy
    }
}
