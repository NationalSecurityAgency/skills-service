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
import { computed } from 'vue'
import { useRoute } from 'vue-router'

export const usePagePath = () => {
  const route = useRoute()

  const adminHomePage = '/administrator'
  const isAdminPage = computed(() => {
    return route.fullPath.toLowerCase().startsWith(adminHomePage)
  })

  const progressAndRankingHomePage = '/progress-and-rankings'
  const isProgressAndRankingPage = computed(() => {
    return route.fullPath.toLowerCase().startsWith(progressAndRankingHomePage)
  })

  const settingsHomePage = '/settings'
  const isSettingsPage = computed(() => {
    return route.fullPath.toLowerCase().startsWith(settingsHomePage)
  })

  return {
    adminHomePage,
    isAdminPage,
    progressAndRankingHomePage,
    isProgressAndRankingPage,
    settingsHomePage,
    isSettingsPage
  }
}
