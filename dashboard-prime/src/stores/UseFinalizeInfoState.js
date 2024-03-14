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