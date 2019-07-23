package skills.metrics.builders;

import skills.metrics.model.MetricsChart;
import skills.metrics.model.Section;

import java.util.Map;

public interface MetricsChartBuilder {

    Section getSection();

    Integer getDisplayOrder();

    MetricsChart build(String projectId, Map<String, String> props, boolean loadData);
}
