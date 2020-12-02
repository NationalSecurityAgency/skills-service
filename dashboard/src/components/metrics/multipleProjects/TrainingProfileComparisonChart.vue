/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <b-card>
    <div class="text-center" style="width: 100%">
      <span class="font-weight-bold"><i :class="titleIcon" class="mr-2 text-secondary"></i>{{ title }}</span>
      <metrics-overlay :loading="loading" :has-data="hasData" no-data-msg="Selected projects don't have any data">
        <apexchart v-if="!loading"
                   :ref="chartId"
                   type="bar" height="350"
                   :options="options"
                   :series="seriesInternal"/>
      </metrics-overlay>
    </div>
  </b-card>
</template>

<script>
  import numberFormatter from '@//filters/NumberFilter';
  import MetricsOverlay from '../utils/MetricsOverlay';

  export default {
    name: 'TrainingProfileComparisonChart',
    components: { MetricsOverlay },
    props: {
      title: {
        type: String,
        required: true,
      },
      titleIcon: {
        type: String,
        required: true,
      },
      series: {
        type: Array,
        required: true,
      },
      labels: {
        type: Array,
        required: true,
      },
      horizontal: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        chartId: this.title.replace(/\s+/g, ''),
        loading: true,
        hasData: false,
        seriesInternal: [],
        options: {
          chart: {
            type: 'bar',
            height: 350,
            toolbar: {
              show: false,
            },
          },
          legend: {
            show: false,
          },
          plotOptions: {
            bar: {
              horizontal: this.horizontal,
              distributed: true,
              columnWidth: '50%',
              endingShape: 'rounded',
            },
          },
          dataLabels: {
            enabled: false,
          },
          xaxis: {
            categories: this.labels,
          },
          yaxis: {
            min: 0,
            labels: {
              formatter(val) {
                return typeof val === 'number' ? numberFormatter(val) : val;
              },
            },
          },
        },
      };
    },
    watch: {
      series() {
        this.seriesInternal = [{
          name: this.title,
          data: this.series,
        }];
      },
      labels() {
        this.$refs[this.chartId].updateOptions({
          xaxis: {
            categories: this.labels,
          },
        });
      },
    },
    mounted() {
      if (!this.horizontal) {
        this.options.fill = {
          type: 'gradient',
          gradient: {
            shade: 'light',
            type: 'horizontal',
            shadeIntensity: 0.25,
            gradientToColors: undefined,
            inverseColors: true,
            opacityFrom: 0.85,
            opacityTo: 0.85,
            stops: [50, 0, 100],
          },
        };
      }

      this.seriesInternal = [{
        name: this.title,
        data: this.series,
      }];

      this.hasData = this.series && this.series.length > 0 && this.series.find((item) => item > 0) !== undefined;

      this.loading = false;
    },
  };
</script>

<style scoped>

</style>
