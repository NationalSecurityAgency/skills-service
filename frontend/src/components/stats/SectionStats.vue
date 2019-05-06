<template>
  <div>
    <sub-page-header title="Stats"/>

    <div v-if="!isLoading" class="row">
      <div v-for="(chart, index) in loadedCharts" :key="chart.options.chart.id"
           :class="index == 0 ? 'col-12' : 'col-6'" class="mb-3">
        <skills-chart :chart="chart"/>
      </div>
    </div>

    <simple-card v-if="loadableCharts.length > 0">
      <h5>Available Stats</h5>

      <div class="row">
        <div v-for="(chart, index) in loadableCharts" style="min-width: 25rem;" class="col-4 mb-3" :key="chart.options.chart.id">
          <stat-card :title="chart.chartMeta.title" :subtitle="chart.chartMeta.subtitle" :icon="getStatCardColorClass(chart.chartMeta.icon, index)"
                     :description="chart.chartMeta.description" :chart-builder-id="chart.chartMeta.chartBuilderId"
                     @load-chart="loadChart">
          </stat-card>
        </div>
      </div>
    </simple-card>

    <skills-spinner :is-loading="isLoading"/>
  </div>
</template>

<script>
  import SkillsChart from './SkillsChart';
  import StatCard from './StatCard';
  import StatsService from './StatsService';
  import { SectionParams } from './SectionHelper';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SimpleCard from '../utils/cards/SimpleCard';
  import SkillsSpinner from '../utils/SkillsSpinner';

  export default {
    name: 'SectionStats',
    components: {
      SkillsSpinner,
      SimpleCard,
      SubPageHeader,
      SkillsChart,
      StatCard,
    },
    props: {
      projectId: String,
      sectionIdParam: String,
      section: String,
      numDaysToShow: {
        type: Number,
        default: 120,
      },
      numMonthsToShow: {
        type: Number,
        default: 6,
      },
      loadDataForFirst: {
        type: Number,
        default: 3,
      },
    },
    data() {
      return {
        charts: [],
        isLoading: true,
        statCardIconsColors: ['text-warning', 'text-primary', 'text-info', 'text-danger'],
      };
    },
    mounted() {
      this.loadInitialCharts();
    },
    computed: {
      loadedCharts() {
        return this.charts.filter(chart => chart.dataLoaded);
      },
      loadableCharts() {
        return this.charts.filter(chart => !chart.dataLoaded);
      },
    },
    methods: {
      getStatCardColorClass(icon, index) {
        const colorIndex = this.statCardIconsColors.length < index ? index : index % this.statCardIconsColors.length;
        const color = this.statCardIconsColors[colorIndex];
        return `${icon} ${color}`;
      },
      loadInitialCharts() {
        const sectionParams = new SectionParams.Builder(this.section, this.projectId)
          .withSectionIdParam(this.sectionIdParam)
          .withNumMonths(this.numMonthsToShow)
          .withNumDays(this.numDaysToShow)
          .withLoadDataForFirst(this.loadDataForFirst)
          .build();
        StatsService.getChartsForSection(sectionParams)
          .then((response) => {
            this.charts = response;
            this.isLoading = false;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      loadChart(chartBuilderId) {
        this.isLoading = true;
        const sectionParams = new SectionParams.Builder(this.section, this.projectId)
          .withSectionIdParam(this.sectionIdParam)
          .withNumMonths(this.numMonthsToShow)
          .withNumDays(this.numDaysToShow)
          .withChartBuilderId(chartBuilderId)
          .build();
        StatsService.getChartForSection(sectionParams)
          .then((response) => {
            this.charts.splice(this.charts.findIndex(it => it.chartMeta.chartBuilderId === chartBuilderId), 1);
            this.charts.push(response);
            this.isLoading = false;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>
