<template>
    <div class="card level-breakdown-container h-100">
        <div class="card-header">
            <h6 class="card-title mb-0 float-left">Level Breakdown</h6>
        </div>
        <div class="card-body m-0 p-0 mr-1 mt-1">
            <apexchart
                    v-if="chartSeries"
                    :options="chartOptions"
                    :series="chartSeries"
                    height="330" type="bar"/>
        </div>
    </div>
</template>

<script>
    import VueApexCharts from 'vue-apexcharts';

    export default {
        name: 'LevelsBreakdownChart',
        components: {
            apexchart: VueApexCharts,
        },
        props: {
            rankingDistribution: Object,
        },
        data() {
            return {
                chartSeries: {},
                chartOptions: {
                    annotations: {
                        points: [{
                            x: 'Level 0',
                            seriesIndex: 0,
                            label: {
                                borderColor: '#775DD0',
                                offsetY: 0,
                                style: {
                                    color: '#fff',
                                    background: '#775DD0',
                                },
                                text: 'You are Level 0!',
                            },
                        }],
                    },
                    plotOptions: {
                        bar: {
                            columnWidth: '50%',
                            endingShape: 'rounded',
                        },
                    },
                    dataLabels: {
                        enabled: false,
                    },
                    grid: {
                        row: {
                            colors: ['#fff', '#f2f2f2'],
                        },
                    },
                    xaxis: {
                        labels: {
                            rotate: -45,
                        },
                    },
                    yaxis: {
                        title: {
                            text: '# of Users',
                        },
                    },
                    fill: {
                        type: 'gradient',
                        gradient: {
                            shade: 'dark',
                            type: 'horizontal',
                            shadeIntensity: 0.25,
                            gradientToColors: undefined,
                            inverseColors: true,
                            opacityFrom: 0.85,
                            opacityTo: 0.85,
                            stops: [50, 0, 100],
                        },
                    },
                },
            };
        },
        mounted() {
            this.computeRankingDistributionChartSeries();
        },
        methods: {
            computeRankingDistributionChartSeries() {
                const series = [{
                    name: '# of Users',
                    data: [{ x: 'Level 0', y: 20 }], // Current end point does not return level 0 count which my user is in. Just mock it right now
                }];
                if (this.rankingDistribution.usersPerLevel) {
                    Object.values(this.rankingDistribution.usersPerLevel)
                        .forEach((level) => {
                            const datum = { x: `Level ${level.level}`, y: level.numUsers };
                            series[0].data.push(datum);
                            if (level.level === this.rankingDistribution.myLevel) {
                                this.chartOptions.annotations.points[0].x = datum.x;
                                this.chartOptions.annotations.points[0].text = `You are ${datum.x}!`;
                            }
                        });
                }
                this.chartOptions = { ...this.chartOptions }; // Trigger reactivity
                this.chartSeries = series;
            },
        },
    };
</script>

<style>
    .level-breakdown-container .apexcharts-menu-icon {
        position: relative !important;
        top: -2.3rem !important;
    }
</style>

<style scoped>

</style>
