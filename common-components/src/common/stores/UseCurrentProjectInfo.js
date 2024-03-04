import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useProjectInfo = defineStore('useProjectInfo', () => {
  const currentProjectId = ref('')
  const setCurrentProjectId = (newProjId) => {
    currentProjectId.value = newProjId
  }

  return {
    currentProjectId,
    setCurrentProjectId
  }
})