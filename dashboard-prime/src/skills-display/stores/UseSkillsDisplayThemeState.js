import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useSkillsDisplayThemeState = defineStore('skillsDisplayThemeState', () => {
  const theme = ref({
    progressIndicators: {
      beforeTodayColor: '#14a3d2',
      earnedTodayColor: '#7ed6f3',
      completeColor: '#59ad52',
      incompleteColor: '#cdcdcd'
    },
    charts: {
      axisLabelColor: 'black'
    }
  })
  const setThemeByKey = (key, value) => {
    if (typeof value === 'object') {
      theme.value[key] = { ...theme.value[key], ...value }
    } else {
      theme.value[key] = value
    }
  }

  const landingPageTitle = computed(() => theme.value.landingPageTitle || 'User Skills')

  return {
    theme,
    setThemeByKey,
    landingPageTitle
  }
})