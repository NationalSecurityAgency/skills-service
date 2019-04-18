package skills.service.metrics.builders;

import skills.service.metrics.model.MetricsChart;
import skills.service.metrics.model.Section;

import java.util.Map;

public interface MetricsChartBuilder {

    Section getSection();

    Integer getDisplayOrder();

    MetricsChart build(String projectId, Map<String, String> props, boolean loadData);
}
