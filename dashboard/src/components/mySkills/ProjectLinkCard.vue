<template>
  <div>
    <b-card>
      <b-row class="m-0 p-0">
        <b-col cols="5">
          <apexchart type="radialBar" height="200" :options="chartOptions" :series="series"></apexchart>
        </b-col>
        <b-col class="text-right">
          <div class="h4 text-uppercase">{{ proj.name }}</div>
          <div class="h5 text-secondary">Level {{ proj.level }}</div>
          <div>
            <b-badge>Rank: 25 / 4,303 </b-badge>
          </div>
        </b-col>
      </b-row>
      <b-progress :max="proj.totalPts" :value="proj.currentPts" height="5px" variant="info" class="proj-progress">
      </b-progress>
      <div class="text-center">
        <span class="small text-center">{{ proj.currentPts | number }} / {{ proj.totalPts | number }}</span>
      </div>
    </b-card>
  </div>
</template>

<script>
  export default {
    name: 'ProjectLinkCard',
    props: ['proj'],
    data() {
      return {
        series: [67],
        chartOptions: {
          chart: {
            height: 150,
            type: 'radialBar',
            offsetY: -10,
          },
          plotOptions: {
            radialBar: {
              startAngle: -135,
              endAngle: 135,
              dataLabels: {
                show: true,
                name: {
                  show: false,
                },
                value: {
                  offsetY: 10,
                  fontSize: '22px',
                  color: undefined,
                  formatter(val) {
                    return `${val}%`;
                  },
                },
              },
            },
          },
          fill: {
            type: 'gradient',
            gradient: {
              shade: 'dark',
              shadeIntensity: 0.15,
              inverseColors: false,
              opacityFrom: 1,
              opacityTo: 1,
              stops: [0, 50, 65, 91],
            },
          },
          stroke: {
            dashArray: 4,
          },
          labels: ['Median Ratio'],
        },
      };
    },
  };
</script>

<style lang="scss" scoped>
@import "../../assets/custom";

.proj-progress {
  background-color: #d5d8db !important;
  border-color: $info !important;
}

</style>
