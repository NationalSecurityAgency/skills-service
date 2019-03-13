<template>
  <section>
    <modal
      size="lg"
      @dismiss="handleClose">
      <modal-header
        slot="header"
        class="text-left"
        icon-class="fa fa-bar-chart"
        title="ALL RANKINGS"
        @cancel="handleClose"/>

      <div v-if="loading">
        <vue-simple-spinner
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

      <modal-footer
        slot="footer"
        :hide-cancel="true"
        help-url=""
        @ok="handleClose"/>
    </modal>
  </section>
</template>

<script>
  import Modal from '@/common/modal/Modal.vue';
  import ModalHeader from '@/common/modal/ModalHeader.vue';
  import ModalFooter from '@/common/modal/ModalFooter.vue';

  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import UniqueIdGenerator from '@/common/utilities/UniqueIdGenerator';

  import Spinner from 'vue-simple-spinner';
  import Chart from 'chart.js/src/chart';

  export default {
    components: {
      Modal,
      ModalHeader,
      ModalFooter,
      'vue-simple-spinner': Spinner,
    },
    props: {
      subject: String,
      isOpen: Boolean,
    },
    data() {
      return {
        loading: false,
        chartId: UniqueIdGenerator.uniqueId('userskills-chart-'),
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
      handleClose() {
        this.$emit('ok');
      },
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
