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

    <metrics-card title="SkillTree Progress" class="mt-3">
      <b-row class="my-4">
        <b-col>
          <b-card>
            <div class="text-center">
              <span class="font-weight-bold"><i class="fas fa-tasks mr-2 text-secondary"></i>Project Participation</span>
              <apexchart type="radialBar" height="350"
                         :options="chartOptions1"
                         :series="series1">
              </apexchart>
              <span>Participating in 3 or 5 Projects</span>
            </div>
          </b-card>
        </b-col>
        <b-col>
          <b-card>
            <div class="text-center">
              <span class="font-weight-bold"><i class="fas fa-tasks mr-2 text-secondary"></i>Project Point History</span>
<!--              <apexchart type="line" height="350"-->
<!--                         :options="chartOptions2"-->
<!--                         :series="series2">-->
<!--              </apexchart>-->
<!--              <span>Participating in 3 or 5 Projects</span>-->
            </div>
          </b-card>
        </b-col>
      </b-row>
    </metrics-card>

    <metrics-card title="Chart" class="mt-3">
      Chart
    </metrics-card>

    <b-row class="my-4">
      <b-col v-for="proj in projects" :key="proj.name"
             cols="12" lg="6" xl="4"
            class="mb-2">
        <router-link :to="{ name:'MyProjectSkills', params: { projectId: proj.projectId } }" tag="div" class="project-link">
          <project-link-card :proj="proj"/>
        </router-link>
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
          projectId: 'DolphinCommute',
          level: 1,
          totalPts: 34000,
          currentPts: 15000,
          totalUsers: 28399,
          rank: 38,
        }, {
          name: 'DonkeySquirrel',
          projectId: 'DonkeySquirrel',
          level: 0,
          totalPts: 12560,
          currentPts: 15,
          totalUsers: 10,
          rank: 3,
        }, {
          name: 'MonkeyPlop',
          projectId: 'MonkeyPlop',
          level: 3,
          totalPts: 19000,
          currentPts: 16022,
          totalUsers: 59,
          rank: 38,
        }, {
          name: 'Boatfall',
          projectId: 'Boatfall',
          level: 2,
          totalPts: 8525,
          currentPts: 856,
          totalUsers: 379,
          rank: 78,
        }, {
          name: 'SkillTree Dashboard',
          projectId: 'Inception',
          level: 2,
          totalPts: 8525,
          currentPts: 856,
          totalUsers: 379,
          rank: 78,
        }],
        series1: [66],
        chartOptions1: {
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
        series2: [{
                    name: 'Session Duration',
                    data: [45, 52, 38, 24, 33, 26, 21, 20, 6, 8, 15, 10],
                  },
                  {
                    name: 'Page Views',
                    data: [35, 41, 62, 42, 13, 18, 29, 37, 36, 51, 32, 35],
                  },
                  {
                    name: 'Total Visits',
                    data: [87, 57, 74, 99, 75, 38, 62, 47, 82, 56, 45, 47],
                  },
        ],
        chartOptions2: {
          chart: {
            height: 350,
            type: 'line',
            zoom: {
              enabled: false,
            },
          },
          dataLabels: {
            enabled: false,
          },
          stroke: {
            width: [5, 7, 5],
            curve: 'straight',
            dashArray: [0, 8, 5],
          },
          // title: {
          //   text: 'Page Statistics',
          //   align: 'left',
          // },
          legend: {
            tooltipHoverFormatter(val, opts) {
              return `${val} - ${opts.w.globals.series[opts.seriesIndex][opts.dataPointIndex]}`;
            },
          },
          markers: {
            size: 0,
            hover: {
              sizeOffset: 6,
            },
          },
          xaxis: {
            categories: ['01 Jan', '03 Jan', '05 Jan', '07 Jan', '09 Jan', '11 Jan',
            ],
          },
          tooltip: {
            y: [
              {
                title: {
                  formatter(val) {
                    return `${val} (mins)`;
                  },
                },
              },
              {
                title: {
                  formatter(val) {
                    return `${val} per session`;
                  },
                },
              },
              {
                title: {
                  formatter(val) {
                    return val;
                  },
                },
              },
            ],
          },
          grid: {
            borderColor: '#f1f1f1',
          },
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
.project-link :hover {
  cursor: pointer;
}
.charts-content {
  /* this little hack is required to prevent apexcharts from wrapping onto a new line;
  the gist is that they calculate width dynamically and do not work properly with the width of 0*/
  min-width: 1rem !important;
}
</style>
