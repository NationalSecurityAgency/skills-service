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
import { useRoute } from 'vue-router'
import { defineStore } from 'pinia'
import CatalogService from '@/components/skills/catalog/CatalogService.js'

export const useFinalizeInfoState = defineStore('finalizeInfoState', () => {
  const info = ref({
    showFinalizeModal: false,
    finalizeIsRunning: false,
    finalizeSuccessfullyCompleted: false,
    finalizeCompletedAndFailed: false
  })
  const isLoading = ref(true)

  const route = useRoute()
  const loadInfo = () => {
    isLoading.value = true
    CatalogService.getCatalogFinalizeInfo(route.params.projectId)
      .then((finalizeInfoRes) => {
        const finalizeInfoUpdated = {
          showFinalizeModal: false,
          finalizeIsRunning: finalizeInfoRes.isRunning,
          finalizeSuccessfullyCompleted: false,
          finalizeCompletedAndFailed: false,
          ...finalizeInfoRes
        }
        info.value = finalizeInfoUpdated
      }).finally(() => {
      isLoading.value = false
    })
  }

  return {
    info,
    loadInfo,
    isLoading
  }
})