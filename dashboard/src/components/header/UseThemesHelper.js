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
import {computed, ref} from 'vue';
import {defineStore} from 'pinia'
import {useUserPreferences} from "@/stores/UseUserPreferences.js";

export const useThemesHelper = defineStore('useThemesHelper', () => {

  const userPreferences = useUserPreferences()
  const themeOptions =[
    { name: 'Light', value: 'skills-light-green' },
    { name: 'Dark', value: 'skills-dark-green' },
  ];
  const currentTheme = ref(themeOptions[0]);

  const setCurrentTheme = (newTheme) => {
    currentTheme.value = newTheme;
  }

  const setDarkMode = () => {
    setCurrentTheme(themeOptions[1]);
  }

  const setLightMode = () => {
    setCurrentTheme(themeOptions[0]);
  }

  const loadTheme = () => {
    return userPreferences.afterUserPreferencesLoaded().then((response) => {
      const themeSetting = response.enable_dark_mode
      const darkModeEnabled = themeSetting?.toLowerCase() === 'true';
      if(darkModeEnabled) {
        currentTheme.value = themeOptions[1];
      } else {
        currentTheme.value = themeOptions[0];
      }
    });
  }

  const isDarkTheme =  computed(() => currentTheme.value.name === 'Dark')

  return {
    currentTheme,
    setCurrentTheme,
    setDarkMode,
    setLightMode,
    themeOptions,
    loadTheme,
    isDarkTheme
  }
});