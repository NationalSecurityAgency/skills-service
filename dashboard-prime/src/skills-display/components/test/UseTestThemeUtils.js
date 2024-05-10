import { ref } from 'vue'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useRoute } from 'vue-router'

export const useTestThemeUtils = () => {

  const log = useLog()
  const route = useRoute()

  const isThemed = ref(false)
  const customTheme = {
    maxWidth: '100%',
    breadcrumb: {
      linkColor: '#ffff71',
      currentPageColor: '#fd70d2',
      linkHoverColor: '#000000',
    },
    backgroundColor: '#626d7d',
    landingPageTitle: 'Themed User Skills',
    pageTitleTextColor: '#fdfbfb',
    pageTitleFontSize: '1.5rem',
    backButton: {
      padding: '5px 10px',
      fontSize: '12px',
      lineHeight: '1.5'
    },
    textSecondaryColor: '#ededff',
    textPrimaryColor: '#fdf9f9',
    stars: {
      unearnedColor: '#787886',
      earnedColor: 'gold'
    },
    progressIndicators: {
      beforeTodayColor: '#3e4d44',
      earnedTodayColor: '#667da4',
      completeColor: '#59ad52',
      incompleteColor: '#f6eec7'
    },
    charts: {
      axisLabelColor: '#f9f1f1'
    },
    tiles: {
      backgroundColor: '#152E4d',
      watermarkIconColor: '#a6c5f7'
    },
    buttons: {
      backgroundColor: '#152E4d',
      foregroundColor: '#fdfbfb',
      disabledColor: '#989999'
    },
    badges: {
      backgroundColor: '#6f42c1',
      backgroundColorSecondary: '#a37ee6',
      foregroundColor: '#fdfbfb'
    },
    quiz: {
      incorrectAnswerColor: '#d67070',
      correctAnswerColor: '#93f193',
      selectedAnswerColor: '#2c9a2c'
    },
    prerequisites: {
      achievedColor: '#6df28b',
      skillColor: '#ffe297',
      badgeColor: '#ceb6f4',
      thisSkillColor: '#7fbbfa',
      navButtonsColor: '#cce7f3'
    }
  }

  const constructThemeForTest = () => {
    const isThemeInRoute = route.query.enableTheme && route.query.enableTheme === 'true'
    if (isThemeInRoute || isThemed.value) {
      log.info('Configured custom theme for skills-display in test mode')
      isThemed.value = true
      return customTheme
    }

    const themeParamProvided = route.query.themeParam
    if (themeParamProvided) {
      const theme = {}
      const themeParams = Array.isArray(themeParamProvided) ? themeParamProvided : [themeParamProvided]
      themeParams.forEach((themeParamItem) => {
        const split = themeParamItem.split('|')
        const key = split[0]
        let val = split[1]
        if (val === 'null') {
          delete theme[key]
        } else {
          if (val.includes('{')) {
            val = JSON.parse(val)
          }
          theme[key] = val
        }
      })

      return theme
    }

    return null
  }

  return {
    constructThemeForTest,
    isThemed
  }

}