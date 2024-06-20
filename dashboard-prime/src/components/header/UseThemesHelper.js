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
import { useStorage } from '@vueuse/core'
import SettingsService from "@/components/settings/SettingsService.js";

export const useThemesHelper = () => {

  const themeOptions =[
    { icon: 'fas fa-sun', name: 'Light', value: 'skills-light-green' },
    { icon: 'fas fa-moon', name: 'Dark', value: 'skills-dark-green' },
  ];
  const currentTheme = useStorage('currentTheme', themeOptions[0])

  const loadTheme = () => {
    SettingsService.getUserSettings().then((response) => {
      const themeSetting = response.find( (it)=> it.setting === 'enable_dark_mode')
      const darkModeEnabled = themeSetting?.value.toLowerCase() === 'true';
      if(darkModeEnabled) {
        currentTheme.value = themeOptions[1];
      } else {
        currentTheme.value = themeOptions[0];
      }

      configureDefaultThemeFileInHeadTag();
    });
  }

  const configureDefaultThemeFileInHeadTag = () =>{
    let file = document.createElement('link')
    file.id=   'theme-link'
    file.rel = 'stylesheet'
    file.href = `/themes/${currentTheme.value.value}/theme.css`
    document.head.appendChild(file)
  }

  return {
    currentTheme,
    themeOptions,
    configureDefaultThemeFileInHeadTag,
    loadTheme
  }
}