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
import { ref, computed } from 'vue'
import MyProgressService from '@/components/myProgress/MyProgressService.js'
import { defineStore } from 'pinia'

export const useMyProgressState = defineStore('myProgressState', () => {
  const myProjects = ref([])
  const myProgress = ref({})
  const isLoadingMyProgressSummary = ref(true)

  const setMyProgress = (incomingProgress) => {
    myProgress.value = incomingProgress
    myProjects.value = incomingProgress.projectSummaries
  }

  const loadMyProgressSummary = (loadInPlace = false) => {
    isLoadingMyProgressSummary.value = !loadInPlace
    return MyProgressService.loadMyProgressSummary()
      .then((response) => {
        setMyProgress(response)
      }).finally(() => {
        isLoadingMyProgressSummary.value = false
      })
  }

  const hasProjects = computed(() => myProjects.value && myProjects.value?.length > 0)

  function afterMyProjectsLoaded() {
    return new Promise((resolve) => {
      (function waitForMyProgress() {
        if (!isLoadingMyProgressSummary.value) return resolve(myProjects.value)
        setTimeout(waitForMyProgress, 100)
        return myProjects.value
      }())
    })
  }

  return {
    isLoadingMyProgressSummary,
    loadMyProgressSummary,
    afterMyProjectsLoaded,
    hasProjects,
    myProjects,
    myProgress,
  }
})