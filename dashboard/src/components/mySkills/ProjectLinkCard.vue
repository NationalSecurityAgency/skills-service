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
  <div>
    <b-card>
      <b-row class="m-0 p-0">
        <b-col cols="5">
          <apexchart type="radialBar" height="200" :options="chartOptions" :series="series"></apexchart>
        </b-col>
        <b-col class="text-right">
          <div class="h4 text-uppercase">{{ proj.name }}</div>
          <div class="h5 text-secondary">Level {{ proj.level }}</div>
          <div>
            <b-badge :variant="rankVariant">Rank: {{ proj.rank }} / {{ proj.totalUsers | number}} </b-badge>
          </div>
        </b-col>
      </b-row>
      <b-progress :max="proj.totalPts" :value="proj.currentPts" height="5px" variant="info" class="proj-progress">
      </b-progress>
      <div class="text-center">
        <span class="small text-center">{{ proj.currentPts | number }} / {{ proj.totalPts | number }}</span>
      </div>
    </b-card>
  </div>
</template>

<script>
  export default {
    name: 'ProjectLinkCard',
    props: ['proj'],
    data() {
      return {
        series: [0],
        chartOptions: {
          chart: {
            height: 150,
            type: 'radialBar',
            offsetY: -10,
          },
          plotOptions: {
            radialBar: {
              startAngle: -135,
              endAngle: 135,
              dataLabels: {
                show: true,
                name: {
                  show: false,
                },
                value: {
                  offsetY: 10,
                  fontSize: '22px',
                  color: undefined,
                  formatter(val) {
                    return `${val}%`;
                  },
                },
              },
            },
          },
          fill: {
            colors: ['#de0f0f'],
            type: 'solid',
            gradient: {
              shade: 'dark',
              shadeIntensity: 0.15,
              inverseColors: false,
              opacityFrom: 1,
              opacityTo: 1,
              stops: [0, 50, 65, 91],
            },
          },
          stroke: {
            dashArray: 4,
          },
          labels: ['Median Ratio'],
        },
        rankVariant: 'secondary',
      };
    },
    created() {
      if (this.proj.totalPts > 0) {
        const pointsPercent = Math.trunc((this.proj.currentPts / this.proj.totalPts) * 100);
        this.series = [pointsPercent];
        this.chartOptions.fill.colors = [this.getColor(pointsPercent)];
      }
      if (this.proj.totalUsers > 0) {
        const rankPercent = Math.trunc((this.proj.rank / this.proj.totalUsers) * 100);
        console.log(`${rankPercent}`);
        this.rankVariant = this.getVariant(rankPercent);
      }
    },
    methods: {
      getColor(percent) {
        let res = '#007c49';
        if (percent < 15) {
          res = '#e83e8c';
        } else if (percent < 50) {
          res = '#00c3ff';
        }
        return res;
      },
      getVariant(percent) {
        let res = 'secondary';
        if (percent < 15) {
          res = 'secondary';
        } else if (percent < 50) {
          res = 'warning';
        } else if (percent >= 50) {
          res = 'success';
        }
        return res;
      },
    },
  };
</script>

<style lang="scss" scoped>
@import "../../assets/custom";

.proj-progress {
  background-color: #d5d8db !important;
  border-color: $info !important;
}

</style>
