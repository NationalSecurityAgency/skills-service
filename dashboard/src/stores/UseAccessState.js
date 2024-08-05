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
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import AccessService from '@/components/access/AccessService'

export const useAccessState = defineStore('accessState', () => {
  const isSupervisorState = ref(false)
  const loadIsSupervisor = () => {
    return AccessService.hasRole('ROLE_SUPERVISOR')
      .then((result) => {
        isSupervisorState.value = result
      })
  }
  const isSupervisor = computed(() => isSupervisorState.value)

  const isRootState = ref(false)
  const loadIsRoot = () => {
    return AccessService.hasRole('ROLE_SUPER_DUPER_USER')
      .then((result) => {
        isRootState.value = result
      })
  }
  const isRoot = computed(() => isRootState.value)

  return {
    isSupervisor,
    isRoot,
    loadIsSupervisor,
    loadIsRoot
  }
})