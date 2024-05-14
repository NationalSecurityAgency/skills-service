import { ref } from 'vue'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useRoute } from 'vue-router'

export const useTestThemeUtils = () => {

  const log = useLog()
  const route = useRoute()

  const isThemed = ref(false)
  const customTheme =  {
    maxWidth: '100%',
    backgroundColor: '#626d7d',
    landingPageTitle: 'Themed User Skills',
    pageTitle: {
      textColor: 'white',
      fontSize: '1.5rem',
    },
    textSecondaryColor: 'white',
    textPrimaryColor: 'white',
    stars: {
      unearnedColor: '#787886',
      earnedColor: 'gold',
    },
    progressIndicators: {
      beforeTodayColor: '#3e4d44',
      earnedTodayColor: '#667da4',
      completeColor: '#59ad52',
      incompleteColor: '#cdcdcd',
    },
    charts: {
      axisLabelColor: 'white',
    },
    tiles: {
      backgroundColor:'#152E4d',
      watermarkIconColor: '#a6c5f7',
    },
    buttons: {
      backgroundColor: '#152E4d',
      foregroundColor: '#59ad52',
    },
    prerequisites: {
      achievedColor: '#6df28b',
      skillColor: '#ffe297',
      badgeColor: '#ceb6f4',
      thisSkillColor: '#7fbbfa',
      navButtonsColor: '#cce7f3',
    },
    infoCards: {
      iconColors: ['#59ad52', '#6df28b', '#7fbbfa', '#ceb6f4', '#ffe297'],
    }
  }

  const constructThemeForTest = () => {
    const isThemeInRoute = route.query.enableTheme && route.query.enableTheme.toLocaleLowerCase() === 'true'
    let themeRes = null
    if (isThemeInRoute || isThemed.value) {
      log.info('Configured custom theme for skills-display in test mode')
      isThemed.value = true
      themeRes = {...customTheme}
    }

    const themeParamProvided = route.query.themeParam
    if (themeParamProvided) {
      if (!themeRes) {
        themeRes = {}
      }
      const themeParams = Array.isArray(themeParamProvided) ? themeParamProvided : [themeParamProvided]
      themeParams.forEach((themeParamItem) => {
        const split = themeParamItem.split('|')
        const key = split[0]
        let val = split[1]
        if (val === 'null') {
          delete themeRes[key]
        } else {
          if (val.includes('{')) {
            val = JSON.parse(val)
          }
          themeRes[key] = val
        }
      })
    }
    return themeRes
  }

  return {
    constructThemeForTest,
    isThemed
  }

}