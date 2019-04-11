<template>
  <div style="position: relative">
    <apexchart v-if="chart.hasData" class="skills-bordered-component" height="350" :type="chart.chartType"
               :options="chart.options" :series="chart.series"></apexchart>
<!--    <div v-else class="column is-one-third">-->
<!--      <stat-card :title="chart.options.title" :subtitle="chart.options.subtitle" :icon="chart.options.icon"-->
<!--                 :description="chart.options.description"></stat-card>-->
<!--    </div>-->
<!--    <b-loading :is-full-page="false" :active.sync="isLoading" :can-cancel="false"></b-loading>-->
  </div>
</template>

<script>
  import StatsService from './StatsService';

  export default {
    name: 'SkillsChart',
    props: {
      projectId: {
        type: String,
      },
      chart: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        isLoading: true,
      };
    },
    mounted() {
      // this.loadData();
    },
    methods: {
      loadData() {
        StatsService.numAchievedSkillsPivotedBySubject(this.projectId).then((response) => {
          const seriesPairs = response.map((item) => {
            const seriesItem = { x: item.value, y: item.count };
            return seriesItem;
          });
          const sortedSeries = seriesPairs.sort((a, b) => a.y - b.y);
          this.series = [{ name: 'Achieved Skills', data: sortedSeries }];
          this.isLoading = false;
        });
      },
    },
  };
</script>

<style scoped>
</style>
