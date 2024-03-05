import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useProjectInfo = defineStore('useProjectInfo', () => {
  const currentProjectIdVal = ref('')
  const setCurrentProjectId = (newProjId) => {
    currentProjectIdVal.value = newProjId
  }
  const currentProjectId = computed(() => currentProjectIdVal.value)

  return {
    currentProjectId,
    setCurrentProjectId
  }
})