import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useSkillsDisplayThemeState = defineStore('skillsDisplayThemeState', () => {

  const colors = {
    info: '#146c75',
    primary: '#143740',
    secondary: '#60737b',
    warning: '#ffc42b',
    success: '#007c49',
    danger: '#290000',
    white: '#fff',
    pointHistoryGradientStartColor: '#00a4e8'
  }

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

  const infoCards = () => {
    const iconColors = theme.value?.infoCards?.iconColors
    return {
      iconColors: [
        iconColors && iconColors.length > 0 ? iconColors[0] : colors.success,
        iconColors && iconColors.length > 1 ? iconColors[1] : colors.warning,
        iconColors && iconColors.length > 2 ? iconColors[2] : colors.info,
        iconColors && iconColors.length > 3 ? iconColors[3] : colors.danger
      ]
    }
  }

  const landingPageTitle = computed(() => theme.value.landingPageTitle || 'User Skills')

  const graphBadgeColor = computed(() => theme?.value?.prerequisites?.badgeColor || 'indigo')
  const graphSkillColor = computed(() => theme?.value?.prerequisites?.skillColor || 'orange')
  const graphAchievedColor = computed(() => theme?.value?.prerequisites?.achievedColor || 'green')
  const graphThisSkillColor = computed(() => theme?.value?.prerequisites?.thisSkillColor || '#00a4e8')
  const graphNavButtonsColor = computed(() => theme?.value?.prerequisites?.navButtonsColor || '')
  const graphTextPrimaryColor = computed(() => theme?.value?.prerequisites?.textPrimaryColor || '')

  return {
    theme,
    setThemeByKey,
    landingPageTitle,
    colors,
    infoCards,
    graphBadgeColor,
    graphSkillColor,
    graphAchievedColor,
    graphThisSkillColor,
    graphNavButtonsColor,
    graphTextPrimaryColor
  }
})