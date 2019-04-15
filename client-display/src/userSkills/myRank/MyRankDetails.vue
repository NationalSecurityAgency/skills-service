<template>
  <section
    class='myrank-container'>
    <div v-if='loading'>
      <vue-simple-spinner
        class='myrank-loading-spinner'
        size='large'
        message='Loading...'/>
    </div>

    <div
      v-if='!loading'
      class='row text-center mt-2'>

      <my-rank-detail-stat-card
        class="col-md-3"
        icon-class="fas fa-trophy"
        label="My Level"
        :value="rankingDistribution.myLevel" />

      <my-rank-detail-stat-card
        class="col-md-3"
        icon-class="fas fa-user-plus"
        label="My Points"
        :value="rankingDistribution.myPoints" />

      <my-rank-detail-stat-card
        class="col-md-3"
        icon-class="fas fa-running"
        label="My Position"
        :value="rankingDistribution.myPosition" />

      <my-rank-detail-stat-card
        class="col-md-3"
        icon-class="fa fa-users"
        label="Total Users"
        :value="rankingDistribution.totalUsers" />

    </div>

    <div
        v-if="!loading"
        class="row mt-3">
        <div class="col-sm-12 rank-history-chart">
          <div class="card" style="height: 100%;">
            <div class="card-body">
              <h3 class="text-left">Rank History</h3>
              <my-rank-history-chart />
            </div>
          </div>
        </div>
    </div>

    <div
      v-if="!loading"
      class="row mt-3">
      <div class="col-sm-6">
        <div class="card">
          <div class="card-body">
            <apexchart
              v-if="chartSeries"
              :options="chartOptions"
              :series="chartSeries"
              height="250" type="bar" />
          </div>
        </div>
      </div>
      <div class="col-sm-6 summary-card">
        <div class="card" style="height: 100%" >
          <div class="card-body" >
            <h3 class="text-left">Stats</h3>
            <ul>
              <li>
                <i class="fas fa-bolt" style="color: #f3d221" />
                <span class="pl-3">Your rank is <b>18</b> with <b>53</b> points</span>
              </li>
              <li>
                <i class="fas fa-hand-peace" style="color: #f3d221" />
                <span class="pl-3">You are <b>30</b> points passed rank <b>17</b></span>
              </li>
              <li>
                <i class="fas fa-crosshairs" style="color: rgb(251, 118, 118)" />
                <span class="pl-3">You need <b>300</b> more points to pass rank <b>19</b>.</span>
              </li>
              <li>
                <i class="fas fa-flag-checkered" style="color: #44484b" />
                <span class="pl-3">You need <b>1,000</b> more points to reach <b>Level 2</b>.</span>
              </li>
              <li>
                <i class="fas fa-users" style="color: rgb(128, 226, 131)" />
                <span class="pl-3"><b>50</b> users are lower rank to you.</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
  import MyRankDetailStatCard from '@/userSkills/myRank/MyRankDetailStatCard.vue';
  import MyRankHistoryChart from '@/userSkills/myRank/MyRankHistoryChart.vue';

  import UserSkillsService from '@/userSkills/service/UserSkillsService';

  import Spinner from 'vue-simple-spinner';
  import VueApexCharts from 'vue-apexcharts';

  export default {
    components: {
      MyRankDetailStatCard,
      MyRankHistoryChart,
      apexchart: VueApexCharts,
      'vue-simple-spinner': Spinner,
    },
    props: {
      subject: String,
    },
    data() {
      return {
        loading: true,
        rankingDistribution: null,
        chartSeries: {},
        chartOptions: {
          annotations: {
            points: [{
              x: 'Level 0',
              seriesIndex: 0,
              label: {
                borderColor: '#775DD0',
                offsetY: 0,
                style: {
                  color: '#fff',
                  background: '#775DD0',
                },
                text: 'You are Level 0!',
              },
            }],
          },
          plotOptions: {
            bar: {
              columnWidth: '50%',
              endingShape: 'rounded',
            },
          },
          dataLabels: {
            enabled: false,
          },
          title: {
            text: 'My Level Position',
            align: 'left',
            style: {
              color: '#008FFB',
            },
          },
          grid: {
            row: {
              colors: ['#fff', '#f2f2f2'],
            },
          },
          xaxis: {
            labels: {
              rotate: -45,
            },
          },
          yaxis: {
            title: {
              text: '# of Users',
            },
          },
          fill: {
            type: 'gradient',
            gradient: {
              shade: 'dark',
              type: 'horizontal',
              shadeIntensity: 0.25,
              gradientToColors: undefined,
              inverseColors: true,
              opacityFrom: 0.85,
              opacityTo: 0.85,
              stops: [50, 0, 100],
            },
          },
        },
      };
    },
    mounted() {
      this.getData();
    },
    methods: {
      computeRankingDistributionChartSeries() {
        const series = [{
          name: '# of Users',
          data: [{ x: 'Level 0', y: 20 }], // Current end point does not return level 0 count which my user is in. Just mock it right now
        }];
        if (this.rankingDistribution.usersPerLevel) {
          Object.values(this.rankingDistribution.usersPerLevel).forEach((level) => {
            const datum = { x: `Level ${level.level}`, y: level.numUsers };
            series[0].data.push(datum);
            if (level.level === this.rankingDistribution.myLevel) {
              this.chartOptions.annotations.points[0].x = datum.x;
              this.chartOptions.annotations.points[0].text = `You are ${datum.x}!`;
            }
          });
        }
        this.chartOptions = { ...this.chartOptions }; // Trigger reactivity
        this.chartSeries = series;
      },
      getData() {
        this.loading = true;
        const subjectId = this.subject ? this.subject.subjectId : null;
        UserSkillsService.getUserSkillsRankingDistribution(subjectId)
          .then((response) => {
            this.rankingDistribution = response;
            this.loading = false;
            this.computeRankingDistributionChartSeries();
          });
      },
    },
  };
</script>

<style scoped>
  ul {
    list-style: none;
    text-align: left;
    padding-left: 1rem;
  }

  ul li {
    padding-top: 1rem;
  }

  ul li i {
    font-size: 2rem;
    width: 2.5rem;
    text-align: center;
    vertical-align: middle;
  }

  .summary-card {
    height: 305px;
  }

  .rank-history-chart {
    height: 305px;
  }
</style>
