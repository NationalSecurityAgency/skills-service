<template>
  <div>
    <div class="columns skills-underline-container">
      <div class="column is-full">
        <span class="title is-3">Stats</span>
      </div>
    </div>

    <div class="columns is-multiline">
      <div v-for="(chart, index) in chartsWithData" class="column" :class="index == 0 ? 'is-full' : ''" :key="chart.options.id">
        <div class="column" :class="index == 0 ? 'is-full' : ''" >
          <skills-chart :chart="chart" :project-id="projectId"></skills-chart>
        </div>
      </div>
    </div>

    <div v-if="chartsWithoutData.length > 0" class="columns">
      <div class="column is-full">
        <div class="skills-bordered-component">
          <div class="columns">
            <div class="column is-full">
              <span class="title is-5">Available Stats</span>
            </div>
          </div>
          <div class="columns is-multiline">
            <div v-for="chart in chartsWithoutData" class="column is-one-third" :key="chart.options.id">
              <stat-card :title="chart.chartMeta.title" :subtitle="chart.chartMeta.subtitle" :icon="chart.chartMeta.icon"
                         :description="chart.chartMeta.description"></stat-card>
            </div>
          </div>
        </div>
      </div>
    </div>

    <b-loading :is-full-page="false" :active.sync="isLoading" :can-cancel="false"></b-loading>
  </div>
</template>

<script>
  import SkillsChart from './SkillsChart';
  import StatCard from './StatCard';
  import StatsService from './StatsService';

  export default {
    name: 'ProjectStats',
    components: {
      SkillsChart,
      StatCard,
    },
    props: ['projectId'],
    loadedCharts: [],
    data() {
      return {
        numDaysToShow: 120,
        charts: [],
        isLoading: true,
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      chartsWithData() {
        return this.charts.filter(chart => chart.hasData);
      },
      chartsWithoutData() {
        return this.charts.filter(chart => !chart.hasData);
      },
    },
    methods: {
      loadData() {
        StatsService.getChartsForProjectSection(this.projectId, this.numDaysToShow).then((response) => {
          this.charts = response;
          this.loadedCharts = this.charts.map(chart => chart.options.id);
          this.isLoading = false;
        });
      },
      loading(id) {
        return !this.loadedCharts.includes(id);
      },
    },
  };
</script>

<style scoped>

</style>
