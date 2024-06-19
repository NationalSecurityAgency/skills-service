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
import { ref } from 'vue'
import UsersService from '@/components/users/UsersService.js'

export const useProjectUserState = defineStore('projectUserState', () => {

  const numSkills = ref(0)
  const userTotalPoints = ref(0)

  const loadUserDetailsState = (projectId, userId) => {
    return UsersService.getUserSkillsMetrics(projectId, userId)
      .then((response) => {
        numSkills.value = response.numSkills
        userTotalPoints.value = response.userTotalPoints
      })
  }

  return {
    numSkills,
    userTotalPoints,
    loadUserDetailsState
  }

})