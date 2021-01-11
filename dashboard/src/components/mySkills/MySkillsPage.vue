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
    <b-row class="my-4">
      <b-col>
        <info-snapshot-card />
      </b-col>
      <b-col>
        <num-skills />
      </b-col>
      <b-col>
        <last-earned-card />
      </b-col>
      <b-col>
        <badges-num-card />
      </b-col>
    </b-row>

      <b-card>
        <div class="text-center">
          <span class="font-weight-bold"><i class="fas fa-chart-bar mr-2 text-secondary"></i>Project Point History</span>
          <apexchart type="line" height="350"
                     :options="chartOptions2"
                     :series="series2">
          </apexchart>
        </div>
      </b-card>

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
  import ProjectLinkCard from './ProjectLinkCard';
  import InfoSnapshotCard from './InfoSnapshotCard';
  import NumSkills from './NumSkills';
  import BadgesNumCard from './BadgesNumCard';
  import LastEarnedCard from './LastEarnedCard';

  export default {
    name: 'MySkillsPage',
    components: {
      LastEarnedCard,
      BadgesNumCard,
      NumSkills,
      InfoSnapshotCard,
      ProjectLinkCard,
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
        series2: [{
                    name: 'DolphinCommute',
                    // data: [45, 52, 38, 24, 33, 26, 21, 20, 6, 8, 15, 10],
                    // eslint-disable-next-line max-len
                    data: [[1607644800000, 15], [1607731200000, 16], [1607817600000, 15], [1607904000000, 19], [1607990400000, 17], [1608076800000, 18], [1608163200000, 14], [1608249600000, 12], [1608336000000, 13], [1608422400000, 10], [1608508800000, 15], [1608595200000, 18], [1608681600000, 16], [1608768000000, 14], [1608854400000, 14], [1608940800000, 17], [1609027200000, 18], [1609113600000, 13], [1609200000000, 17], [1609286400000, 19], [1609372800000, 16], [1609459200000, 15], [1609545600000, 15], [1609632000000, 15], [1609718400000, 16], [1609804800000, 16], [1609891200000, 14], [1609977600000, 16], [1610064000000, 18], [1610150400000, 15], [1610236800000, 12], [1610323200000, 2]],
                  },
                  {
                    name: 'DonkeySquirrel',
                    // data: [35, 41, 62, 42, 13, 18, 29, 37, 36, 51, 32, 35],
                    // eslint-disable-next-line max-len
                    data: [[1607644800000, 5], [1607731200000, 6], [1607817600000, 45], [1607904000000, 29], [1607990400000, 1], [1608076800000, 12], [1608163200000, 4], [1608249600000, 23], [1608336000000, 8], [1608422400000, 19], [1608508800000, 23], [1608595200000, 0], [1608681600000, 6], [1608768000000, 34], [1608854400000, 12], [1608940800000, 11], [1609027200000, 8], [1609113600000, 3], [1609200000000, 27], [1609286400000, 19], [1609372800000, 19], [1609459200000, 5], [1609545600000, 25], [1609632000000, 21], [1609718400000, 9], [1609804800000, 11], [1609891200000, 24], [1609977600000, 36], [1610064000000, 28], [1610150400000, 28], [1610236800000, 17], [1610323200000, 22]],
                  },
                  {
                    name: 'MonkeyPlop',
                    // eslint-disable-next-line max-len
                    data: [[1607644800000, 11], [1607731200000, 42], [1607817600000, 24], [1607904000000, 13], [1607990400000, 22], [1608076800000, 12], [1608163200000, 13], [1608249600000, 11], [1608336000000, 8], [1608422400000, 43], [1608508800000, 36], [1608595200000, 38], [1608681600000, 1], [1608768000000, 4], [1608854400000, 27], [1608940800000, 7], [1609027200000, 38], [1609113600000, 33], [1609200000000, 7], [1609286400000, 9], [1609372800000, 26], [1609459200000, 17], [1609545600000, 18], [1609632000000, 5], [1609718400000, 9], [1609804800000, 33], [1609891200000, 4], [1609977600000, 14], [1610064000000, 28], [1610150400000, 30], [1610236800000, 32], [1610323200000, 35]],
                    // data: [87, 57, 74, 99, 75, 38, 62, 47, 82, 56, 45, 47],
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
          // stroke: {
          //   width: [5, 7, 5],
          //   curve: 'straight',
          //   dashArray: [0, 8, 5],
          // },
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
            // categories: ['01 Jan', '03 Jan', '05 Jan', '07 Jan', '09 Jan', '11 Jan',
            // ],
            type: 'datetime',
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
