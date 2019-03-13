<template>
  <div style="height: 215px">
    <canvas :id="chartId"/>
  </div>
</template>

<script>
  import UniqueIdGenerator from '@/common/utilities/UniqueIdGenerator';

  import numeral from 'numeral';
  import Chart from 'chart.js';

  export default {
    props: {
      pointsHistory: Array,
    },
    data() {
      return {
        chartId: UniqueIdGenerator.uniqueId('chart-'),
      };
    },
    computed: {
      dataArray() {
        let dataArray = [];

        if (this.pointsHistory.length > 0) {
          dataArray = this.pointsHistory
            .map(value => ({
              t: new Date(parseInt(value.dayPerformed, 10)),
              y: value.points,
            }));
        }

        return dataArray;
      },
    },
    beforeDestroy() {
      this.chart.destroy();
    },
    mounted() {
      const ctx = document.getElementById(this.chartId);
      this.chart = new Chart(ctx, {
        type: 'line',
        data: {
          datasets: [{
            borderColor: '#7CB5EC',
            pointBorderWidth: 0,
            borderWidth: 2,
            data: this.dataArray,
          }],
        },
        options: {
          maintainAspectRatio: false,
          tooltips: {
            callbacks: {
              label(tooltipItem) {
                return `Points: ${numeral(tooltipItem.yLabel).format('0,0')}`;
              },
            },
          },
          elements: {
            point: {
              radius: 0,
              hitRadius: 10,
              hoverRadius: 5,
            },
          },
          title: {
            display: true,
            fontStyle: 'bold',
            padding: 0,
            fontSize: 20,
            text: 'Point Progress',
          },
          legend: {
            display: false,
          },
          scales: {
            xAxes: [{
              gridLines: {
                display: false,
              },
              type: 'time',
              position: 'bottom',
              time: {
                displayFormats: {
                  day: 'MMM YYYY',
                },
              },
            }],
            yAxes: [{
              ticks: {
                callback(unformatted) {
                  return numeral(unformatted).format('0a');
                },
              },
            }],
          },
        },
      });
    },
  };
</script>
