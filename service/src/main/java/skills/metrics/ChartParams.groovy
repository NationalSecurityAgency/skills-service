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

@CompileStatic
class ChartParams {
    public static final String CHART_BUILDER_ID = 'chartBuilderId'
    public static final String SUBJECT_ID = 'subjectId'
    public static final String NUM_DAYS = 'numDays' // limit data to X days
    public static final String NUM_MONTHS = 'numMonths' // limit data to X months
    public static final String LOAD_DATA_FOR_FIRST = 'loadDataForFirst'  // only load the first X charts data for a section
    public static final String SECTION_ID = 'sectionId'

    static String getValue(Map<String, String> props, String key, String defaultVal=null) {
        String value = defaultVal
        if (props.get(key)) {
            value = props.get(key)
        }
        return value
    }

    static Integer getIntValue(Map<String, String> props, String key, Integer defaultVal=null) {
        Integer value = defaultVal
        if (props.get(key)?.isInteger()) {
            value = props.get(key).toInteger()
        }
        return value
    }
}
