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
      <div class='col-md-3'>
        <div class='card'>
          <div class='card-body'>
            <h1 class='distribution-icon-text'>{{ rankingDistribution.myLevel | number }}</h1>
            <h4>My Level</h4>
          </div>
        </div>
      </div>
      <div class='col-md-3'>
        <div class='card'>
          <div class='card-body'>
            <h1 class='distribution-icon-text'>{{ rankingDistribution.myPoints | number }}</h1>
            <h4>My Points</h4>
          </div>
        </div>
      </div>
      <div class='col-md-3'>
        <div class='card'>
          <div class='card-body'>
            <h1 class='distribution-icon-text'>{{ rankingDistribution.myPosition | number }}</h1>
            <h4>My Rank</h4>
          </div>
        </div>
      </div>
      <div class='col-md-3'>
        <div class='card'>
          <div class='card-body'>
            <h1 class='distribution-icon-text'>{{ rankingDistribution.totalUsers | number }}</h1>
            <h4>Total Users</h4>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="!loading"
      class='row mt-3'>
      <div class='col-sm-6'>
        <div class='card'>
          <div class='card-body'>
            <apexchart
              v-if="chartSeries"
              :options='chartOptions'
              :series='chartSeries'
              height='250' type='bar' />
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
  import UserSkillsService from '@/userSkills/service/UserSkillsService';

  import Spinner from 'vue-simple-spinner';
  import VueApexCharts from 'vue-apexcharts';

  export default {
    components: {
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

  /*.myrank-container .title {*/
  /*  text-align: left;*/
  /*  width: 40%;*/
  /*  border-bottom: 3px solid #d0d9e8;*/
  /*  padding-left: 1rem;*/
  /*}*/

  /*.myrank-container .title i {*/
  /*  color: #d0d9e8;*/
  /*  font-size: 120%;*/
  /*  vertical-align: middle;*/
  /*}*/

  /*.myrank-container .title span {*/
  /*  font-size: 80%;*/
  /*  color: #698dad;*/
  /*  vertical-align: middle;*/
  /*  padding-left: 1rem;*/
  /*}*/

  /*.myrank-loading-spinner {*/
  /*  padding-top: 15rem;*/
  /*}*/

  /*.point-distribution-info {*/
  /*  width: 100%;*/
  /*  text-align: center;*/
  /*  display: inline-block;*/
  /*}*/

  /*.distribution-tile {*/
  /*  background-color: white;*/
  /*}*/

  /*.distribution-icon-text {*/
  /*  font-size: 60px;*/
  /*}*/

  .point-distribution-wrapper {
    display: block;
    position: relative;
    width: 100%;
    padding: 10px;
    height: 480px;
    background-color: orange;
  }
</style>
