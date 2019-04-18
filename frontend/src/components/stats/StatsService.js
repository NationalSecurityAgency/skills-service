import axios from 'axios';

export default {
  getUsage(projectId, numDays) {
    return axios.get(`/admin/projects/${projectId}/usage?numDays=${numDays}`)
      .then(response => response.data);
  },
  numAchievedSkillsPivotedBySubject(projectId) {
    return axios.get(`/admin/projects/${projectId}/stats?type=NUM_ACHIEVED_SKILLS_PIVOTED_BY_SUBJECT`)
      .then(response => response.data);
  },
  numUsersPerSkillLevel(projectId) {
    return axios.get(`/admin/projects/${projectId}/stats?type=NUM_USERS_PER_OVERALL_SKILL_LEVEL`)
      .then(response => response.data);
  },
  getChartsForProjectSection(projectId, numDays, loadDataForFirst = 3) {
    return axios.get(`/admin/projects/${projectId}/metrics?numDays=${numDays}&loadDataForFirst=${loadDataForFirst}`)
      .then(response => Promise.resolve(this.buildCharts(response.data)));
  },
  getChartForProjectSection(projectId, chartBuilderId, numDays) {
    return axios.get(`/admin/projects/${projectId}/metrics/${chartBuilderId}?numDays=${numDays}`)
      .then(response => Promise.resolve(this.buildChart(response.data)));
  },

  buildCharts(data) {
    return data.map(item => this.buildChart(item));
  },

  buildChart(chartData) {
    const chartType = (chartData.chartType === 'LineChart') ? 'line' : 'bar';
    const hasData = Array.isArray(chartData.dataItems) && chartData.dataItems.length;
    const { dataLoaded } = chartData;
    const series = this.buildSeries(chartData);
    const chartMeta = chartData.chartOptions;
    const options = this.buildOptions(chartData.chartType, chartData.chartOptions);
    return {
      chartType,
      hasData,
      dataLoaded,
      options,
      chartMeta,
      series,
    };
  },

  buildSeries(chartData) {
    let seriesPairs = chartData.dataItems.map(dataItem => ({ x: dataItem.value, y: dataItem.count }));
    if (chartData.chartOptions.sort === 'asc') {
      seriesPairs = seriesPairs.sort((a, b) => a.y - b.y);
    } else if (chartData.chartOptions.sort === 'desc') {
      seriesPairs = seriesPairs.sort((a, b) => b.y - a.y);
    }
    return [{ name: (chartData.chartOptions.dataLabel || ''), data: seriesPairs }];
  },

  buildOptions(chartType, chartOptions) {
    const distributed = !Object.prototype.hasOwnProperty.call(chartOptions, 'distributed') || chartOptions.distributed === true;
    const showDataLabels = Object.prototype.hasOwnProperty.call(chartOptions, 'showDataLabels') && chartOptions.showDataLabels === true;
    const options = {
      chart: {
        id: chartOptions.chartBuilderId,
      },
      dataLabels: { enabled: showDataLabels },
      plotOptions: {},
      title: {
        text: chartOptions.title,
        align: chartOptions.titlePosition || 'left',
        style: {
          fontSize: chartOptions.titleSize || '14px',
          color: chartOptions.titleColor || '#008FFB',
        },
      },
      xaxis: {
        type: chartOptions.xAxisType,
        title: { text: chartOptions.xAxisLabel },
      },
      yaxis: {
        type: chartOptions.yAxisType,
        title: { text: chartOptions.yAxisLabel },
      },
      theme: {
        palette: chartOptions.palette,
      },
    };
    if (chartType === 'HorizontalBarChart') {
      options.plotOptions.bar = {
        horizontal: true,
        distributed,
        dataLabels: {
          position: chartOptions.dataLabelPosition || 'center',
        },
      };
    } else if (chartType === 'VerticalBarChart') {
      options.plotOptions.bar = {
        horizontal: false,
        distributed,
        dataLabels: {
          position: chartOptions.dataLabelPosition || 'center',
        },
      };
    }

    return options;
  },
};
