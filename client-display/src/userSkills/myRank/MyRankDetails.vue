<template>
  <section
    class="myrank-container">
    <div v-if="loading">
      <vue-simple-spinner
        class="myrank-loading-spinner"
        size="large"
        message="Loading..."/>
    </div>

    <div
      v-if="!loading"
      class="point-distribution-info">
      <div class="dist-info-tile col-xs-3">
        <h1 class="distribution-icon-text">{{ rankingDistribution.myLevel | number }}</h1>
        <h4>My Level</h4>
      </div>
      <div class="dist-info-tile col-xs-3">
        <h1 class="distribution-icon-text">{{ rankingDistribution.myPoints | number }}</h1>
        <h4>My Points</h4>
      </div>
      <div class="dist-info-tile col-xs-3">
        <h1 class="distribution-icon-text">{{ rankingDistribution.myPosition | number }}</h1>
        <h4>My Rank</h4>
      </div>
      <div class="dist-info-tile col-xs-3">
        <h1 class="distribution-icon-text">{{ rankingDistribution.totalUsers | number }}</h1>
        <h4>Total Users</h4>
      </div>
    </div>

    <div
      v-show="!loading"
      class="point-distribution-wrapper">
      <canvas :id="chartId"/>
    </div>
  </section>
</template>

<script>
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import UniqueIdGenerator from '@/common/utilities/UniqueIdGenerator';

  import Spinner from 'vue-simple-spinner';
  import Chart from 'chart.js/src/chart';

  export default {
    components: {
      'vue-simple-spinner': Spinner,
    },
    props: {
      subject: String,
    },
    data() {
      return {
        loading: false,
        chartId: UniqueIdGenerator.uniqueId('skills-chart-'),
        rankingDistribution: {},
      };
    },
    computed: {
      dataObject() {
        const labels = [];
        const data = [];
        const colors = [];
        Object.values(this.rankingDistribution.usersPerLevel).forEach((level) => {
          labels.push('Level '.concat(level.level));
          data.push(level.numUsers);
          colors.push(level.level === this.rankingDistribution.myLevel ? '#aed7ac' : '#7cb5ec');
        });
        return {
          labels,
          datasets: [{
            data,
            label: '# Users',
            backgroundColor: colors,
          }],
        };
      },
    },
    mounted() {
      this.getData();
    },
    beforeDestroy() {
      this.chart.destroy();
    },
    methods: {
      getData() {
        this.loading = true;
        const subjectId = this.subject ? this.subject.subjectId : null;
        UserSkillsService.getUserSkillsRankingDistribution(subjectId)
          .then((response) => {
            this.rankingDistribution = response;
            this.loading = false;
            const ctx = document.getElementById(this.chartId);
            setTimeout(() => {
              this.chart = new Chart(ctx, this.getChartConfig());
            }, 250); // Timeout necessary for Firefox 38. I think it is because the modal animation when opening.
          });
      },
      getChartConfig() {
        return {
          type: 'bar',
          data: this.dataObject,
          options: {
            legend: {
              display: false,
            },
            scales: {
              yAxes: [{
                scaleLabel: {
                  display: true,
                  labelString: '# Users',
                },
                ticks: {
                  beginAtZero: true,
                  callback(value) {
                    let result = null;
                    if (Number.isInteger(value)) {
                      result = value;
                    }
                    return result;
                  },
                },
              }],
            },
          },
        };
      },
    },
  };
</script>

<style scoped>
  .myrank-container {
    max-width: 800px;
    margin: 0 auto;
  }

  .myrank-loading-spinner {
    padding-top: 15rem;
  }

  .point-distribution-info {
    width: 100%;
    text-align: center;
    display: inline-block;
  }

  .dist-info-tile:not(:first-child) {
    display: block;
    border-left: 1px solid #ccc;
  }

  .distribution-icon-text {
    font-size: 60px;
  }

  .point-distribution-wrapper {
    display: block;
    width: 100%;
    padding: 10px;
  }
</style>
