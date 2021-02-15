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
  <metrics-card title="Post Achievement Usage" data-cy="binnedNumUsersPostAchievement">
      <b-overlay v-if="!loading" :show="isEmpty" opacity=".5">
        <apexchart v-if="!loading" type="bar" height="350" :options="chartOptions" :series="series"></apexchart>
        <template v-slot:overlay>
          <div class="alert alert-info">
            <i class="fas fa-user-clock"></i> Users have not achieved any skills, yet...</div>
        </template>
      </b-overlay>
    <div class="text-muted small">Number of times this Skill is performed per user after having fully achieved it.</div>
  </metrics-card>
</template>

<script>
  import MetricsService from '../MetricsService';
  import MetricsCard from '../utils/MetricsCard';

  export default {
    name: 'BinnedPostAchievementUsage',
    components: { MetricsCard },
    data() {
      return {
        loading: true,
        isEmpty: true,
        series: [],
        colors: ['#17a2b8', '#28a745'],
        chartOptions: {
          chart: {
            type: 'bar',
            height: 250,
            toolbar: {
              show: true,
              offsetX: 0,
              offsetY: -60,
            },
          },
          plotOptions: {
            bar: {
              horizontal: false,
            },
          },
          dataLabels: {
            enabled: false,
          },
          stroke: {
            show: true,
            width: 2,
            colors: ['transparent'],
          },
          xaxis: {
            categories: [],
            title: {
              text: '# of times Skill performed',
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
              text: '# of distinct users',
            },
          },
          fill: {
            opacity: 1,
          },
          tooltip: {
            y: {
              formatter(val) {
                return `${val}`;
              },
            },
          },
          legend: {
            offsetY: 5,
          },
        },
      };
    },
    mounted() {
      MetricsService.loadChart(this.$route.params.projectId, 'binnedUsagePostAchievementMetricsBuilder', { skillId: this.$route.params.skillId })
        .then((res) => {
          this.updateChart(res);
          this.loading = false;
        });
    },
    methods: {
      updateChart(res) {
        const series = [];
        if (res) {
          this.isEmpty = false;
          this.chartOptions.xaxis.categories = res.map((labeledCount) => labeledCount.label);
          const data = [];
          res.forEach((labeledCount) => {
            data.push(labeledCount.count);
          });
          series.push({
            data,
            name: '# of users',
          });
        }
        this.series = series;
      },
    },
  };
</script>

<style scoped>

</style>
