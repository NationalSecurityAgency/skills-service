<template>
  <div style="position: relative">
    <apexchart class="skills-bordered-component" height="350" type="bar"
               :options="options" :series="series"></apexchart>
    <b-loading :is-full-page="false" :active.sync="isLoading" :can-cancel="false"></b-loading>
  </div>
</template>

<script>
  import UsersService from './StatsService';

  export default {
    name: 'AchievedSkillsChart',
    props: ['projectId'],
    data() {
      return {
        isLoading: true,
        options: {
          plotOptions: {
            bar: {
              horizontal: true,
              distributed: true,
              dataLabels: {
                position: 'top',
              },
            },
          },
          title: {
            text: 'Achieved Skills By Subject (for ALL users)',
            align: 'left',
            style: {
              // fontSize: '16px',
              color: '#008FFB',
            },
          },
          // yaxis: {
          //   title: {
          //     text: 'Number of Achieved Skills',
          //   },
          // },
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
        UsersService.numAchievedSkillsPivotedBySubject(this.projectId).then((response) => {
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
