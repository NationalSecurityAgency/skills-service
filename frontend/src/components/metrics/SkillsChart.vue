<template>
  <div ref="chartContainer">
    <div v-if="!chart.hasData" class="disabled-overlay"/>
    <div v-if="!chart.hasData" class="text-center user-skills-no-data-icon-text text-danger">
      <div class="row justify-content-center">
        <div class="col-5 text-center border rounded bg-light p-2">
          <div style="font-size: 1rem;"><i class="fa fa-ban"></i> No Data Available</div>
        </div>
      </div>
    </div>
    <simple-card>
      <apexchart v-if="chart.dataLoaded"
                 :class="{'disabled': !chart.hasData}"
                 class="skills-chart"
                 height="350" :type="chart.chartType"
                 :options="chart.options" :series="chart.series">
      </apexchart>
    </simple-card>
  </div>
</template>

<script>
  import Vue from 'vue';
  import VueScrollTo from 'vue-scrollto';
  import SimpleCard from '../utils/cards/SimpleCard';

  Vue.use(VueScrollTo);

  export default {
    name: 'SkillsChart',
    components: { SimpleCard },
    props: {
      chart: {
        type: Object,
        default: () => ({}),
      },
      scrollIntoView: {
        type: Boolean,
        default: false,
      },
    },
    created() {
      // add random data for an empty chart
      if (!this.chart.hasData) {
        this.chart.series[0].data = [
          { x: '0', y: 2 },
          { x: '1', y: 1 },
          { x: '2', y: 0 },
          { x: '3', y: 4 },
          { x: '4', y: 5 },
          { x: '5', y: 3 },
        ];
        this.chart.options.theme = {
          monochrome: {
            enabled: true,
            color: '#efefef',
          },
        };
      }
    },
    mounted() {
      if (this.scrollIntoView) {
        VueScrollTo.scrollTo(this.$refs.chartContainer, 750, {
          y: true,
          x: false,
        });
        this.$emit('scrolledIntoView');
      }
    },
  };
</script>

<style scoped>

  .skills-chart {
    min-width: 350px;
  }

  .disabled {
    opacity: 0.3;
  }

  .disabled-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #666666;
    opacity: 0;
    z-index: 999;
  }

  .user-skills-no-data-icon-text {
    font-weight: 700;
    opacity: 0.8;
    position: absolute;
    left: 0;
    top: 50%;
    z-index: 1000;
    text-align: center;
    width: 100%;
    transform: translateY(-50%);
  }

  .user-skills-no-data-icon-subtext {
    font-size: 0.9em;
    color: grey;
    display: block;
  }
</style>
