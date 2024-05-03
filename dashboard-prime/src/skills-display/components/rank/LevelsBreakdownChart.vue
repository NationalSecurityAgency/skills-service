<script setup>
import { onMounted, ref } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const props = defineProps({
  usersPerLevel: Array,
  myLevel: Number,
})
const attributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const animationEnded = ref(false)
const numFormat = useNumberFormat()

onMounted(() => {
  if (props.usersPerLevel) {
    computeChartSeries()
  }
})
const chartSeries = ref([{
  name: '# of Users',
  data: [{ x: `${attributes.levelDisplayName} 1`, y: 0 }, { x: `${attributes.levelDisplayName} 2`, y: 0 }, { x: `${attributes.levelDisplayName} 3`, y: 0 }, { x: `${attributes.levelDisplayName} 4`, y: 0 }, { x: `${attributes.levelDisplayName} 5`, y: 0 }],
}])
const computeChartSeries = () => {
  const series = [{
    name: '# of Users',
    data: [],
  }];
  if (props.usersPerLevel) {
    props.usersPerLevel.forEach((level) => {
      const datum = { x: `${attributes.levelDisplayName} ${level.level}`, y: level.numUsers };
      series[0].data.push(datum);
      if (level.level === props.myLevel) {
        // const label = {
        //   x: datum.x,
        //   text: `You are ${datum.x}!`,
        // };
        // this.chartOptions.annotations.points = [label];
        chartOptions.value.annotations.points[0].x = datum.x;
        chartOptions.value.annotations.points[0].label.text = `You are ${datum.x}!`;
      }
    });
  }
  // chartOptions.value = { ...chartOptions.value }; // Trigger reactivity
  chartSeries.value = series;
}

const chartLabels = () => {
  const chartsModule = themeState.theme?.charts
  return {
    borderColor: chartsModule?.labelBorderColor ? chartsModule.labelBorderColor : themeState.colors.primary,
    backgroundColor: chartsModule?.labelBackgroundColor ? chartsModule.labelBackgroundColor : themeState.colors.success,
    foregroundColor: chartsModule?.labelForegroundColor ? chartsModule.labelForegroundColor : themeState.colors.white
  }
}

const chartOptions = ref({
  chart: {
    toolbar: {
      offsetY: -38,
    },
  },
  annotations: {
    points: [{
      x: `${attributes.levelDisplayName} 1`,
      seriesIndex: 0,
      label: {
        borderColor: chartLabels().borderColor,
        offsetY: 0,
        style: {
          color: chartLabels().foregroundColor,
          background: chartLabels().backgroundColor,
        },
        text: '',
      },
    }],
  },
  plotOptions: {
    bar: {
      columnWidth: '50%',
        borderRadius: 5,
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
        style: {
        colors: themeState.theme.charts.axisLabelColor,
      },
    },
  },
  yaxis: {
    min: 0,
      forceNiceScale: true,
      title: {
      text: '# of Users',
        style: {
        color: themeState.theme.charts.axisLabelColor,
      },
    },
    labels: {
      style: {
        colors: [themeState.theme.charts.axisLabelColor],
      },
      formatter: function format(val) {
        if (val === Infinity) {
          return '0';
        }
        return numFormat.pretty(val);
      },
    },
    axisTicks: {
      show: false,
    },
  },
})
</script>

<template>
<Card data-cy="levelBreakdownChart" :pt="{ content: { class: 'mb-0 pb-0'}}">
  <template #subtitle>
    <div class="flex">
      <div>
        {{ attributes.levelDisplayName }} Breakdown
      </div>
    </div>
  </template>
  <template #content>
    <apexchart
      :options="chartOptions"
      @animationEnd="animationEnded = true"
      :series="chartSeries"
      height="330" type="bar"/>

    <span v-if="animationEnded" data-cy="levelBreakdownChart-animationEnded"></span>
  </template>
</Card>
</template>

<style scoped>

</style>