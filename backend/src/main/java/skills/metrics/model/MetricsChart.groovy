package skills.metrics.model

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class MetricsChart {

    ChartType chartType
    boolean dataLoaded = false
    String icon

    Map<ChartOption, Object> chartOptions = [:]

    // LabelCountItem, TimestampCountItem, others maybe?
    List<skills.controller.result.model.CountItem> dataItems
}
