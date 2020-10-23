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

import groovy.time.TimeCategory
import skills.controller.exceptions.SkillException

class MetricsParams {
    final static String P_SKILL_ID = "skillId"
    final static String P_SUBJECT_ID = "subjectId"
    final static String P_START_TIMESTAMP = "start"

    static String getSkillId(String projectId, String chartId, Map<String, String> props) {
        return getParam(props, P_SKILL_ID, chartId, projectId)
    }

    static String getSubjectId(String projectId, String chartId, Map<String, String> props) {
        return getParam(props, P_SUBJECT_ID, chartId, projectId)
    }

    static Date getStart(String projectId, String chartId, Map<String, String> props) {
        String tStr = getParam(props, P_START_TIMESTAMP, chartId, projectId)
        Long timestamp = Long.valueOf(tStr)
        Date date = new Date(timestamp)
        use(TimeCategory) {
            if (date.before(3.years.ago)) {
                throw new SkillException("Chart[${chartId}]: ${P_START_TIMESTAMP} param timestamp must be within last 4 years", projectId)
            }
        }
        return date
    }

    private static String getParam(Map<String, String> props, String paramId, String chartId, String projectId) {
        String sortBy = props[paramId]
        if (!sortBy) {
            throw new SkillException("Chart[${chartId}]: Must supply ${paramId} param", projectId)
        }
        return sortBy
    }
}
