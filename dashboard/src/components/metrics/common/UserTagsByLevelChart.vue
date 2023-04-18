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
  <metrics-card :title="`Top 20 ${tag.label} Level Breakdown`" :data-cy="`numUsersByTag-${tag.key}`">
    <div style="max-height: 800px; overflow-y: auto; overflow-x: clip;">
      <metrics-overlay :loading="loading" :has-data="series.length > 0" no-data-msg="No users currently">
        <apexchart v-if="!loading" type="bar" :height="chartHeight" :options="chartOptions" :series="series"></apexchart>
      </metrics-overlay>
    </div>
  </metrics-card>
</template>

<script>
  import numberFormatter from '@/filters/NumberFilter';
  import MetricsCard from '../utils/MetricsCard';
  import MetricsService from '../MetricsService';
  import MetricsOverlay from '../utils/MetricsOverlay';

  export default {
    name: 'UserTagsByLevelChart',
    components: { MetricsOverlay, MetricsCard },
    props: ['tag'],
    computed: {
      chartHeight() {
        let height = 350;
        if (this.chartOptions?.xaxis?.categories) {
          const dataSize = this.chartOptions.xaxis.categories.length;
          height = dataSize > 0 ? dataSize * 250 : 350;
        }
        return height;
      },
    },
    data() {
      return {
        series: [],
        loading: true,
        chartOptions: {
          chart: {
            width: 250,
            type: 'bar',
            toolbar: {
              show: true,
              offsetX: 0,
              offsetY: 0,
            },
          },
          plotOptions: {
            bar: {
              horizontal: true,
              dataLabels: {
                position: 'bottom',
              },
            },
          },
          stroke: {
            show: true,
            width: 2,
            colors: ['transparent'],
          },
          xaxis: {
            title: {
              text: '# of Users',
            },
            labels: {
              style: {
                fontSize: '13px',
                fontWeight: 600,
              },
            },
          },
          yaxis: {
            title: {
              text: this.tag.label,
            },
          },
          tooltip: {
            y: {
              formatter(val) {
                return numberFormatter(val);
              },
            },
          },
          dataLabels: {
            enabled: true,
            textAnchor: 'start',
            offsetX: 0,
            style: {
              colors: ['#17a2b8'],
              fontSize: '14px',
              fontFamily: 'Helvetica, Arial, sans-serif',
              fontWeight: 'bold',
            },
            formatter(val, opt) {
              return `${opt.w.globals.seriesNames[opt.seriesIndex]}: ${numberFormatter(val)} users`;
            },
            dropShadow: {
              enabled: true,
            },
            background: {
              enabled: true,
              foreColor: '#ffffff',
              padding: 10,
              borderRadius: 2,
              borderWidth: 1,
              borderColor: '#686565',
              opacity: 1,
              dropShadow: {
                enabled: false,
              },
            },
          },
          legend: {
            show: false,
          },
        },
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        this.loading = true;
        MetricsService.loadChart(this.$route.params.projectId, 'achievementsByTagPerLevelMetricsBuilder', { subjectId: this.$route.params.subjectId, userTagKey: this.tag.key })
          .then((dataFromServer) => {
            if (dataFromServer && Object.keys(dataFromServer.data).length > 0) {
              const userData = dataFromServer.data;
              const tags = Object.keys(userData);

              if (tags) {
                const categories = userData.map((a) => a.tag);
                this.chartOptions.xaxis.categories = categories;
                const numberOfLevels = dataFromServer.totalLevels;
                const series = [];

                for (let level = 1; level <= numberOfLevels; level += 1) {
                  const dataForLevel = [];
                  tags.forEach((tag) => {
                    if (userData[tag].value[level] > 0) {
                      dataForLevel.push(userData[tag].value[level]);
                    } else {
                      dataForLevel.push(0);
                    }
                  });
                  if (dataForLevel.length > 0) {
                    series.push({ name: `Level ${level}`, data: dataForLevel });
                  }
                }
                this.series = series;
              }
            }
            this.loading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
