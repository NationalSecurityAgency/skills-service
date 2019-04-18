<template>
  <div style="position: relative">
    <apexchart class="skills-bordered-component" height="350" type="bar"
               :options="options" :series="series"></apexchart>
    <b-loading :is-full-page="false" :active.sync="isLoading" :can-cancel="false"></b-loading>
  </div>
</template>

<script>
  import StatsService from './StatsService';

  export default {
    name: 'UsersPerLevelChart',
    props: ['projectId'],
    data() {
      return {
        isLoading: true,
        options: {
          plotOptions: {
            bar: {
              distributed: true,
              // endingShape: 'rounded',
            },
          },
          dataLabels: {
            enabled: false,
          },
          title: {
            text: 'Number Users for each Level',
            align: 'left',
            style: {
              // fontSize: '16px',
              color: '#008FFB',
            },
          },
          theme: {
            palette: 'palette2',
          },
        },
        series: [],
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        StatsService.numUsersPerSkillLevel(this.projectId).then((response) => {
          const seriesPairs = response.map((item) => {
            const seriesItem = { x: item.value, y: item.count };
            return seriesItem;
          });
          // const sortedSeries = seriesPairs.sort((a, b) => a.y - b.y);

          // const seriesPairs = response.map(item => item.count);
          // const filtered = seriesPairs.filter(item => item > 0);
          // this.series = seriesPairs;
          this.series = [{ name: 'Achieved Skills', data: seriesPairs }];

          // const labelsDisplay = response.map(item => item.label);
          // this.options = { labels: labelsDisplay };
          this.isLoading = false;
        });
      },
    },
  };
</script>

<style scoped>
</style>
