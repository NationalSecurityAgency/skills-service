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
