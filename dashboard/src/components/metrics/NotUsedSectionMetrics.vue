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
  <div>
    <sub-page-header title="Metrics1"/>
    <div>
      <metrics-table />
    </div>
    <div v-if="!isLoading" class="row">
      <div v-for="(chart, index) in loadedCharts" :key="chart.options.chart.id"
           :class="index == 0 ? 'col-12' : 'col-md-6'" class="mb-3">
        <skills-chart :chart="chart" :scrollIntoView="chart.scrollIntoView" @scrolledIntoView="chart.scrollIntoView = false"/>
      </div>
    </div>

    <simple-card v-if="canDisplayCharts && loadableCharts.length > 0">
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
    <no-content3 v-if="!isLoading && (!canDisplayCharts)" title="No Metrics Yet" :sub-title="noChartsMsg"/>

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
  import MetricsTable from './MetricsTable';

  export default {
    name: 'SectionMetricsNotUsed',
    components: {
      MetricsTable,
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
      this.section = this.$route.meta.metricsSection;
      if (this.section === SECTION.PROJECTS) {
        this.sectionIdParam = this.$route.params.projectId;
      }

      switch (this.section) {
      case SECTION.PROJECTS:
        this.sectionIdParam = this.$route.params.projectId;
        break;
      case SECTION.SUBJECTS:
        this.sectionIdParam = this.$route.params.subjectId;
        break;
      case SECTION.BADGES:
        this.sectionIdParam = this.$route.params.badgeId;
        break;
      case SECTION.SKILLS:
        this.sectionIdParam = this.$route.params.skillId;
        break;
      case SECTION.USERS:
        this.sectionIdParam = this.$route.params.userId;
        break;
      case SECTION.GLOBAL:
        this.sectionIdParam = 'global';
        break;
      default:
        throw new Error(`Can't handle section type ${this.section}`);
      }

      this.loadInitialCharts();
    },
    computed: {
      loadedCharts() {
        return this.charts.filter((chart) => chart.dataLoaded);
      },
      loadableCharts() {
        return this.charts.filter((chart) => !chart.dataLoaded);
      },
      canDisplayCharts() {
        return this.loadedCharts && this.loadedCharts.length > 0;
      },
      noChartsMsg() {
        if (this.section === SECTION.GLOBAL) {
          return 'Cross project metrics compare projects. These metrics will be available once you have access to at least two projects.';
        }
        return 'Metrics coming soon!';
      },
    },
    methods: {
      getMetricCardColorClass(icon, index) {
        const colorIndex = this.metricCardIconsColors.length < index ? index : index % this.metricCardIconsColors.length;
        const color = this.metricCardIconsColors[colorIndex];
        return `${icon} ${color}`;
      },
      loadInitialCharts() {
        const sectionParams = new SectionParams.Builder(this.section, this.$route.params.projectId)
          .withSectionIdParam(this.sectionIdParam)
          .withNumMonths(this.numMonthsToShow)
          .withNumDays(this.numDaysToShow)
          .withLoadDataForFirst(this.loadDataForFirst)
          .build();

        let promise = null;
        if (this.section === SECTION.GLOBAL) {
          promise = MetricsService.getGlobalChartsForSection(sectionParams);
        } else {
          promise = MetricsService.getChartsForSection(sectionParams);
        }

        promise.then((response) => {
          this.charts = response;
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      loadChart(chartBuilderId) {
        this.isLoading = true;
        const sectionParams = new SectionParams.Builder(this.section, this.$route.params.projectId)
          .withSectionIdParam(this.sectionIdParam)
          .withNumMonths(this.numMonthsToShow)
          .withNumDays(this.numDaysToShow)
          .withChartBuilderId(chartBuilderId)
          .build();

        let promise = null;
        if (this.section === SECTION.GLOBAL) {
          promise = MetricsService.getGlobalChartForSection(sectionParams);
        } else {
          promise = MetricsService.getChartForSection(sectionParams);
        }

        promise.then((response) => {
          this.charts.splice(this.charts.findIndex((it) => it.chartMeta.chartBuilderId === chartBuilderId), 1);
          this.charts.push({ scrollIntoView: true, ...response });
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
