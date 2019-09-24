package skills.metrics.model;

public enum ChartOption {
    chartBuilderId,
    description,
    title,
    titleSize,  // ex: 16px
    titleColor,  // ex: #008FFB
    titlePosition, // left, center, right
    subtitle,
    xAxisLabel,
    xAxisType, // datetime, ?
    yAxisLabel,
    yAxisType, // datetime, ?
    distributed, // true/false
    dataLabel,
    showDataLabels, // true/false
    dataLabelPosition, // top, center, bottom
    sort, // asc, desc
    palette, // ex. palette2
    icon,
    labels
}
