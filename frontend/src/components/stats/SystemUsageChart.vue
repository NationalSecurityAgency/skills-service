<template>
  <div style="position: relative">
    <apexchart class="skills-bordered-component" height="350" type="line"
               :options="options" :series="series"></apexchart>
    <b-loading :is-full-page="false" :active.sync="isLoading" :can-cancel="false"></b-loading>
  </div>
</template>

<script>
  import UsersService from './StatsService';

  export default {
    name: 'SystemUsageChart',
    props: ['projectId'],
    data() {
      return {
        isLoading: true,
        numDaysToShow: 120,
        options: {
          chart: {
            id: 'users-over-time',
            shadow: {
              enabled: true,
              opacity: 1,
            },
          },
          title: {
            text: 'Distinct # of Users over Time',
            align: 'left',
            style: {
              // fontSize: '16px',
              color: '#008FFB',
            },
          },
          xaxis: {
            type: 'datetime',
          },
          yaxis: {
            title: {
              text: 'Distinct # of Users',
            },
          },
          grid: {
            row: {
              colors: ['#f3f3f3', 'transparent'],
              opacity: 0.5,
            },
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
        UsersService.getUsage(this.projectId, this.numDaysToShow).then((response) => {
          const seriesPairs = response.map(item => [item.timestamp, item.count]);
          // this.options = {
          //   series: [{ name: 'Distinct Users', data: seriesPairs }],
          // };
          this.series = [{ name: 'Distinct Users', data: seriesPairs }];
          this.isLoading = false;
        });
      },
    },
  };
</script>

<style scoped>
</style>
