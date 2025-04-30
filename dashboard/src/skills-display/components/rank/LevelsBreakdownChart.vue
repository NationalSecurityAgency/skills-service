/*
Copyright 2024 SkillTree

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
<script setup>
import { onMounted, ref, computed } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useThemesHelper } from "@/components/header/UseThemesHelper.js";
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useRoute } from 'vue-router'
import ChartOverlayMsg from '@/skills-display/components/utilities/ChartOverlayMsg.vue'

const props = defineProps({
  myLevel: Number
})
const skillsDisplayService = useSkillsDisplayService()
const route = useRoute()
const attributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const themeHelper = useThemesHelper()
const animationEnded = ref(false)
const numFormat = useNumberFormat()

onMounted(() => {
  loadData().then(() => {
    if (usersPerLevel.value) {
      computeChartSeries()
    }
  })
})
const chartAxisColor = () => {
  if (themeState.theme.charts.axisLabelColor) {
    return themeState.theme.charts.axisLabelColor
  }
  return themeHelper.isDarkTheme ? 'white' : undefined
}
const usersPerLevelLoading = ref(true)
const usersPerLevel = ref({})
const loadData = () => {
  return skillsDisplayService.getRankingDistributionUsersPerLevel(route.params.subjectId)
    .then((response) => {
      usersPerLevel.value = response
    })
    .finally(() => {
      usersPerLevelLoading.value = false
    })
}

const hasData = computed(() => {
  const foundMoreThan0 = chartSeries.value.length > 0 && chartSeries.value[0].data.find((item) => item.y > 0)
  return foundMoreThan0
})

const chartSeries = ref([{
  name: '# of Users',
  data: [{ x: `${attributes.levelDisplayName} 1`, y: 0 }, {
    x: `${attributes.levelDisplayName} 2`,
    y: 0
  }, { x: `${attributes.levelDisplayName} 3`, y: 0 }, {
    x: `${attributes.levelDisplayName} 4`,
    y: 0
  }, { x: `${attributes.levelDisplayName} 5`, y: 0 }]
}])

const createPoint = (level) => {
  return {
    x: level,
    seriesIndex: 0,
    label: {
      borderColor: chartLabels().borderColor,
      offsetY: 0,
      style: {
        color: chartLabels().foregroundColor,
        background: chartLabels().backgroundColor
      },
      text: `You are ${level}!`
    }
  }
}
const computeChartSeries = () => {
  const series = [{
    name: '# of Users',
    data: []
  }]
  if (usersPerLevel.value) {
    usersPerLevel.value.forEach((level) => {
      const datum = { x: `${attributes.levelDisplayName} ${level.level}`, y: level.numUsers }
      series[0].data.push(datum)
      if (level.level === props.myLevel) {
        chartOptions.value.annotations.points = [createPoint(datum.x)]
      }
    })
  }
  chartOptions.value = { ...chartOptions.value }; // Trigger reactivity
  chartSeries.value = series
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
      offsetY: -38
    }
  },
  annotations: {
    points: []
  },
  plotOptions: {
    bar: {
      columnWidth: '50%',
      borderRadius: 5,
      distributed: true,
    }
  },
  dataLabels: {
    enabled: false
  },
  legend: {
    show: false
  },
  grid: {
    row: {
      colors: ['#fff', '#f2f2f2']
    }
  },
  xaxis: {
    labels: {
      rotate: -45,
      style: {
        colors: chartAxisColor()
      }
    }
  },
  yaxis: {
    min: 0,
    forceNiceScale: true,
    title: {
      text: '# of Users',
      style: {
        color: themeState.theme.charts.axisLabelColor
      }
    },
    labels: {
      style: {
        colors: chartAxisColor()
      },
      formatter: function format(val) {
        if (val === Infinity) {
          return '0'
        }
        return numFormat.pretty(val)
      }
    },
    axisTicks: {
      show: false
    }
  }
})
</script>

<template>
  <Card data-cy="levelBreakdownChart" :pt="{ content: { class: 'mb-0! pb-0!'}}" class="w-min-15rem h-full">
    <template #subtitle>
      <div class="flex">
        <div>
          {{ attributes.levelDisplayName }} Breakdown
        </div>
      </div>
    </template>
    <template #content>
      <BlockUI :blocked="!hasData || usersPerLevelLoading" :auto-z-index="false">
        <apexchart
          :options="chartOptions"
          @animationEnd="animationEnded = true"
          :series="chartSeries"
          height="330" type="bar" />
        <chart-overlay-msg v-if="!hasData && !usersPerLevelLoading" style="top: 8rem;">
          No one achieved
          <Tag>{{ attributes.levelDisplayName }} 1</Tag>
          yet... You could be the <i><strong>first one</strong></i>!
        </chart-overlay-msg>
        <chart-overlay-msg v-if="usersPerLevelLoading" style="top: 8rem;">
          <skills-spinner
            :is-loading="true"
            size="small"
            message="Loading Chart ..." />
        </chart-overlay-msg>
      </BlockUI>

      <span v-if="animationEnded" data-cy="levelBreakdownChart-animationEnded"></span>
    </template>
  </Card>
</template>

<style scoped>

</style>