<template>
  <div>
    <sub-page-header title="Metrics"/>

    <div v-if="!isLoading" class="row">
      <div v-for="(chart, index) in loadedCharts" :key="chart.options.chart.id"
           :class="index == 0 ? 'col-12' : 'col-md-6'" class="mb-3">
        <skills-chart :chart="chart" :scrollIntoView="chart.scrollIntoView" @scrolledIntoView="chart.scrollIntoView = false"/>
      </div>
    </div>

    <simple-card v-if="loadableCharts.length > 0">
      <h5>Available Metrics</h5>

      <div class="row justify-content-center">
        <div v-for="(chart, index) in loadableCharts" style="min-width: 25rem;" class="col-4 mb-3"
             :key="chart.options.chart.id">
          <metrics-card :title="chart.chartMeta.title" :subtitle="chart.chartMeta.subtitle"
                     :icon="getMetricCardColorClass(chart.chartMeta.icon, index)"
                     :description="chart.chartMeta.description" :chart-builder-id="chart.chartMeta.chartBuilderId"
                     @load-chart="loadChart">
          </metrics-card>
        </div>
      </div>
    </simple-card>
    <no-content3 v-if="(!loadedCharts || loadedCharts.length==0 && !loadedCharts || loadedCharts.length==0)" title="No Metrics Yet" sub-title="Metrics coming soon!"/>

    <skills-spinner :is-loading="isLoading"/>
  </div>
</template>

<script>
  import SkillsChart from './SkillsChart';
  import MetricsCard from './MetricsCard';
  import MetricsService from './MetricsService';
  import { SECTION, SectionParams } from './SectionHelper';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SimpleCard from '../utils/cards/SimpleCard';
  import SkillsSpinner from '../utils/SkillsSpinner';
  import NoContent3 from '../utils/NoContent3';

  export default {
    name: 'SectionMetrics',
    components: {
      SkillsSpinner,
      SimpleCard,
      SubPageHeader,
      SkillsChart,
      MetricsCard,
      NoContent3,
    },
    props: {
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
        section: SECTION.GLOBAL,
        sectionIdParam: String,
        metricCardIconsColors: ['text-warning', 'text-primary', 'text-info', 'text-danger'],
      };
    },
    mounted() {
      if (this.$route.params.projectId) {
        this.sectionIdParam = this.$route.params.projectId;
        this.section = SECTION.PROJECTS;
      } else if (this.$route.params.badgeId) {
        this.section = SECTION.BADGES;
        this.sectionIdParam = this.$route.params.badgeId;
      } else if (this.$route.params.subjectId) {
        this.section = SECTION.SUBJECTS;
        this.sectionIdParam = this.$route.params.subjectId;
      } else if (this.$route.params.skillId) {
        this.section = SECTION.SKILLS;
        this.sectionIdParam = this.$route.params.skillId;
      } else if (this.$route.params.userId) {
        this.section = SECTION.USERS;
        this.sectionIdParam = this.$route.params.userId;
      }

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
      getMetricCardColorClass(icon, index) {
        const colorIndex = this.metricCardIconsColors.length < index ? index : index % this.metricCardIconsColors.length;
        const color = this.metricCardIconsColors[colorIndex];
        return `${icon} ${color}`;
      },
      loadInitialCharts() {
        if (this.section !== SECTION.GLOBAL) {
          const sectionParams = new SectionParams.Builder(this.section, this.$route.params.projectId)
            .withSectionIdParam(this.sectionIdParam)
            .withNumMonths(this.numMonthsToShow)
            .withNumDays(this.numDaysToShow)
            .withLoadDataForFirst(this.loadDataForFirst)
            .build();
          MetricsService.getChartsForSection(sectionParams)
            .then((response) => {
              this.charts = response;
            })
            .finally(() => {
              this.isLoading = false;
            });
        }
        this.isLoading = false;
      },
      loadChart(chartBuilderId) {
        this.isLoading = true;
        const sectionParams = new SectionParams.Builder(this.section, this.$route.params.projectId)
          .withSectionIdParam(this.sectionIdParam)
          .withNumMonths(this.numMonthsToShow)
          .withNumDays(this.numDaysToShow)
          .withChartBuilderId(chartBuilderId)
          .build();
        MetricsService.getChartForSection(sectionParams)
          .then((response) => {
            this.charts.splice(this.charts.findIndex(it => it.chartMeta.chartBuilderId === chartBuilderId), 1);
            this.charts.push(Object.assign({ scrollIntoView: true }, response));
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
