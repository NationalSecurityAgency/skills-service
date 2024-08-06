/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
      backgroundColor: '#bed4ed',
      foregroundColor: '#3f6893',
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