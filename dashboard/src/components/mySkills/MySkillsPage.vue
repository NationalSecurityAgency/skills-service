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
  <div class="container-fluid mt-2" >
    <h2 class="text-center">My Skills</h2>
    <b-row class="my-4">
      <b-col>
        <metrics-card title="card">
          1 of 3
        </metrics-card>
      </b-col>
      <b-col>
        <metrics-card title="card">
          2 of 3
        </metrics-card>
      </b-col>
      <b-col>
        <metrics-card title="card">
          2 of 3
        </metrics-card>
      </b-col>
    </b-row>

    <metrics-card title="Chart" class="mt-3">
      <div class="text-center">
        <span class="font-weight-bold"><i class="fas fa-tasks mr-2 text-secondary"></i>Project Participation</span>
        <apexchart type="radialBar" height="350" :options="chartOptions" :series="series"></apexchart>
        <span>Participating in 3 or 5 Projects</span>
      </div>
    </metrics-card>

    <metrics-card title="Chart" class="mt-3">
      Chart
    </metrics-card>

    <b-row class="my-4">
      <b-col v-for="proj in projects" :key="proj.name"
             cols="4"
            class="mb-2">
        <project-link-card :proj="proj"/>
      </b-col>
    </b-row>

  </div>
</template>

<script>
  import MetricsCard from '../metrics/utils/MetricsCard';
  import ProjectLinkCard from './ProjectLinkCard';

  export default {
    name: 'MySkillsPage',
    components: {
      ProjectLinkCard,
      MetricsCard,
    },
    data() {
      return {
        loading: true,
        projects: [{
          name: 'DolphinCommute',
          level: 1,
          totalPts: 34000,
          currentPts: 15000,
        }, {
          name: 'DonkeySquirrel',
          level: 0,
          totalPts: 12560,
          currentPts: 15,
        }, {
          name: 'MonkeyPlop',
          level: 3,
          totalPts: 19000,
          currentPts: 16022,
        }, {
          name: 'Boatfall',
          level: 2,
          totalPts: 8525,
          currentPts: 856,
        }],
        series: [66],
        chartOptions: {
          chart: {
            height: 350,
            type: 'radialBar',
            toolbar: {
              show: true,
            },
          },
          plotOptions: {
            radialBar: {
              startAngle: -135,
              endAngle: 225,
              hollow: {
                margin: 0,
                size: '70%',
                background: '#fff',
                image: undefined,
                imageOffsetX: 0,
                imageOffsetY: 0,
                position: 'front',
                dropShadow: {
                  enabled: true,
                  top: 3,
                  left: 0,
                  blur: 4,
                  opacity: 0.24,
                },
              },
              track: {
                background: '#fff',
                strokeWidth: '67%',
                margin: 0, // margin is in pixels
                dropShadow: {
                  enabled: true,
                  top: -3,
                  left: 0,
                  blur: 4,
                  opacity: 0.35,
                },
              },

              dataLabels: {
                show: true,
                name: {
                  offsetY: -10,
                  show: true,
                  color: '#888',
                  fontSize: '16px',
                },
                value: {
                  formatter(val) {
                    return `${val} %`;
                  },
                  offsetY: 0,
                  color: '#888',
                  fontSize: '22px',
                  show: true,
                },
              },
            },
          },
          fill: {
            type: 'gradient',
            gradient: {
              shade: 'dark',
              type: 'horizontal',
              shadeIntensity: 0.5,
              gradientToColors: ['#7ED6F3'],
              inverseColors: true,
              opacityFrom: 1,
              opacityTo: 1,
              stops: [0, 100],
            },
          },
          stroke: {
            lineCap: 'round',
          },
          labels: ['Percent'],
        },
      };
    },
    mounted() {
    },
    methods: {
      projectParticipationFormatter(val) {
        console.log(`val is ${val}`);
      },
    },
  };
</script>

<style scoped>

</style>
