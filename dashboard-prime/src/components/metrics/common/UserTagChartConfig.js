export function useUserTagChartConfig() {

    const pieChartOptions = {
        chart: {
            // height: 250,
            // width: 250,
            type: 'pie',
            toolbar: {
                show: true,
                offsetX: 0,
                offsetY: -60,
            },
        },
        // colors: ['#17a2b8', '#28a745'],
        labels: [],
        dataLabels: {
            enabled: false,
        },
        legend: {
            position: 'top',
            horizontalAlign: 'left',
        },
    };
    const barChartOptions = {
        chart: {
            type: 'bar',
            // height: 350,
            toolbar: {
                show: true,
                offsetX: 0,
                offsetY: -60,
            },
        },
        plotOptions: {
            bar: {
                barHeight: '90%',
                endingShape: 'rounded',
                distributed: true,
                horizontal: true,
                dataLabels: {
                    position: 'bottom',
                },
            },
        },
        dataLabels: {
            enabled: true,
            textAnchor: 'start',
            style: {
                colors: ['#17a2b8'],
                fontSize: '14px',
                fontFamily: 'Helvetica, Arial, sans-serif',
                fontWeight: 'bold',
            },
            formatter(val, opt) {
                // return `${opt.w.globals.labels[opt.dataPointIndex]}: ${numberFormatter(val)} users`;
            },
            offsetX: 0,
                dropShadow: {
                enabled: true,
            },
            background: {
                enabled: true,
                foreColor: '#ffffff',
                padding: 10,
                borderRadius: 2,
                borderWidth: 1,
                borderColor: '#686565',
                opacity: 1,
                dropShadow: {
                    enabled: false,
                },
            },
        },
        stroke: {
            show: true,
            width: 2,
            colors: ['transparent'],
        },
        xaxis: {
            categories: [],
            labels: {
                formatter(val) {
                    // return numberFormatter(val);
                },
            },
        },
        yaxis: {
            labels: {
                show: false,
            },
        },
        grid: {
            borderColor: '#cfeaf3',
            position: 'front',
        },
        legend: {
            show: false,
        },
        fill: {
            opacity: 1,
        },
        tooltip: {
            y: {
                formatter(val) {
                    // return numberFormatter(val);
                },
            },
        },
    };

    return { pieChartOptions, barChartOptions };
}