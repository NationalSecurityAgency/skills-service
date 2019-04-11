package skills.service.metrics.model

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import skills.service.controller.result.model.CountItem

@CompileStatic
@Canonical
class MetricsChart {

    ChartType chartType
    String icon

    Map<ChartOption, Object> chartOptions = [:]

    // LabelCountItem, TimestampCountItem, others maybe?
    List<CountItem> dataItems
}
