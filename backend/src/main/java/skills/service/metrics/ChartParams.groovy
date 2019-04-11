package skills.service.metrics

import groovy.transform.CompileStatic

@CompileStatic
class ChartParams {
    public static final String CHART_BUILDER_ID = 'chartBuilderId'
    public static final String NUM_DAYS = 'numDays' // limit data to X days
    public static final String LOAD_DATA_FOR_FIRST = 'loadDataForFirst'  // only load the first X charts data for a section

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
