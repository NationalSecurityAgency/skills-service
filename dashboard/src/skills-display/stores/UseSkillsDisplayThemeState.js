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
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import ThemeHelper from '@/skills-display/theme/ThemeHelper.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import UniqueIdGenerator from '@/utils/UniqueIdGenerator.js'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'

export const useSkillsDisplayThemeState = defineStore('skillsDisplayThemeState', () => {

  const log = useLog()
  const themeHelper = useThemesHelper()
  const themeStyleId = UniqueIdGenerator.uniqueId('custom-theme-style-node-')
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
    charts: {}
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
        iconColors && iconColors.length > 3 ? iconColors[3] : themeHelper.isDarkTheme ? '#e46c6c' : colors.danger
      ]
    }
  }
  const initThemeObjInStyleTag = (theme) =>{
    if (theme) {
      const themeResArtifacts = ThemeHelper.build(theme);

      // populate store so JS can subscribe to those values and update styles
      themeResArtifacts.themeModule.forEach((value, key) => {
        setThemeByKey(key, value)
      });

      const style = document.createElement('style');

      style.id = themeStyleId;
      style['data-cy'] = 'skills-display-custom-theme'

      log.trace(`Adding theme css to style tag: ${themeResArtifacts.css}`)
      const cssToAdd = document.createTextNode(themeResArtifacts.css)
      style.appendChild(cssToAdd);


      const { body } = document;
      body.appendChild(style);
    }
  }


  const landingPageTitle = computed(() => theme.value.landingPageTitle || 'User Skills')

  const graphBadgeColor = computed(() => theme?.value?.prerequisites?.badgeColor || 'indigo')
  const graphSkillColor = computed(() => theme?.value?.prerequisites?.skillColor || 'orange')
  const graphAchievedColor = computed(() => theme?.value?.prerequisites?.achievedColor || 'green')
  const graphThisSkillColor = computed(() => theme?.value?.prerequisites?.thisSkillColor || '#00a4e8')
  const graphNavButtonsColor = computed(() => theme?.value?.prerequisites?.navButtonsColor || '')
  const graphTextPrimaryColor = computed(() => theme?.value?.textPrimaryColor || '')
  const textPrimaryColor = computed(() => theme?.value?.textPrimaryColor)
  const circleProgressInteriorTextColor = computed(() => theme?.value?.circleProgressInteriorTextColor)
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
    graphTextPrimaryColor,
    initThemeObjInStyleTag,
    circleProgressInteriorTextColor,
    textPrimaryColor,
    themeStyleId
  }
})