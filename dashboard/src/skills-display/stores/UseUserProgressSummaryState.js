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
import { defineStore } from 'pinia'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'

export const useUserProgressSummaryState = defineStore('userProgressSummaryState', () => {

  const skillsDisplayService = useSkillsDisplayService()
  const parentState = useSkillsDisplayParentFrameState()

  const loadingUserProgressSummary = ref(true)
  const userProgressSummary = ref({})
  const loadingUserSkillsRanking = ref(true)
  const userRanking = ref({})
  // The iframe initializer resets its fixed learner's cache explicitly on recovery.
  // These shared stores do not subscribe to live learner/context switching.
  const reset = () => {
    userProgressSummary.value = {}
    userRanking.value = {}
    loadingUserProgressSummary.value = true
    loadingUserSkillsRanking.value = true
  }

  const loadUserProgressSummary = () => {
    const isCurrent = parentState.captureAuthGeneration()
    loadingUserProgressSummary.value = true
    return skillsDisplayService.loadUserProjectSummary()
      .then((result) => {
        userProgressSummary.value = result
      }).finally(() => {
        if (isCurrent()) loadingUserProgressSummary.value = false
      })
  }

  const loadUserSkillsRanking = (subjectId) => {
    const isCurrent = parentState.captureAuthGeneration()
    loadingUserSkillsRanking.value = true
    return skillsDisplayService.loadUserSkillsRanking(subjectId)
      .then((result) => {
        userRanking.value = result
      }).finally(() => {
        if (isCurrent()) loadingUserSkillsRanking.value = false
      })
  }

  return {
    reset,
    userProgressSummary,
    loadUserProgressSummary,
    loadingUserProgressSummary,

    userRanking,
    loadUserSkillsRanking,
    loadingUserSkillsRanking,
  }

})
