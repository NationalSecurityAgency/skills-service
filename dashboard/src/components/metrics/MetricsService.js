/*
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
import axios from 'axios';

export default {

  loadChart(projectId, chartId, params = {}) {
    let url = `/admin/projects/${projectId}/charts/${chartId}`;
    if (params) {
      const paramsEntries = Object.entries(params);
      if (paramsEntries && paramsEntries.length > 0) {
        const paramsStr = paramsEntries.map((entry) => `${entry[0]}=${entry[1]}`)
          .join('&');
        url = `${url}?${paramsStr}`;
      }
    }
    return axios.get(url).then((response) => response.data);
  },
  getChartsForSection(sectionParams) {
    const url = `/admin/projects/${sectionParams.projectId}/${sectionParams.section}/${sectionParams.sectionIdParam}/metrics?numDays=${sectionParams.numDays}&numMonths=${sectionParams.numMonths}&loadDataForFirst=${sectionParams.loadDataForFirst}`;
    return axios.get(url)
      .then((response) => Promise.resolve(this.buildCharts(response.data)));
  },

  getGlobalChartsForSection(sectionParams) {
    const url = `/metrics/${sectionParams.section}?numDays=${sectionParams.numDays}&numMonths=${sectionParams.numMonths}&loadDataForFirst=${sectionParams.loadDataForFirst}`;
    const self = this;
    return axios.get(url)
      .then((response) => Promise.resolve(self.buildCharts(response.data)));
  },

  getGlobalChartForSection(sectionParams) {
    const url = `/metrics/${sectionParams.section}/${sectionParams.sectionIdParam}/metric/${sectionParams.chartBuilderId}?numDays=${sectionParams.numDays}&numMonths=${sectionParams.numMonths}&loadDataForFirst=${sectionParams.loadDataForFirst}`;
    return axios.get(url)
      .then((response) => Promise.resolve(this.buildCharts(response.data)));
  },

  getChartForSection(sectionParams) {
    const url = `/admin/projects/${sectionParams.projectId}/${sectionParams.section}/${sectionParams.sectionIdParam}/metrics/${sectionParams.chartBuilderId}?numDays=${sectionParams.numDays}&numMonths=${sectionParams.numMonths}`;
    return axios.get(url)
      .then((response) => Promise.resolve(this.buildChart(response.data)));
  },

  buildCharts(data) {
    return data.map((item) => this.buildChart(item));
  },

  buildChart(chartData) {
    const chartType = (chartData.chartType === 'HorizontalBar' || chartData.chartType === 'VerticalBar') ? 'bar' : chartData.chartType.toLowerCase();
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
    let seriesData = null;

    if (chartData.chartType.toLowerCase() === 'pie') {
      seriesData = chartData.dataItems.map((dataItem) => dataItem.count);
      return seriesData;
    }

    seriesData = chartData.dataItems.map((dataItem) => ({ x: dataItem.value, y: dataItem.count }));
    const sortAsc = (a, b) => a.y - b.y;
    const sortDsc = (a, b) => b.y - a.y;

    if (chartData.chartOptions.sort === 'asc') {
      seriesData = seriesData.sort(sortAsc);
    } else if (chartData.chartOptions.sort === 'desc') {
      seriesData = seriesData.sort(sortDsc);
    }

    const series = { name: (chartData.chartOptions.dataLabel || ''), data: seriesData };
    return [series];
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
    if (chartType === 'HorizontalBar') {
      options.plotOptions.bar = {
        horizontal: true,
        distributed,
        dataLabels: {
          position: chartOptions.dataLabelPosition || 'center',
        },
      };
    } else if (chartType === 'VerticalBar') {
      options.plotOptions.bar = {
        horizontal: false,
        distributed,
        dataLabels: {
          position: chartOptions.dataLabelPosition || 'center',
        },
      };
    } else if (chartType === 'Pie') {
      options.labels = chartOptions.labels;
    }

    return options;
  },
};
