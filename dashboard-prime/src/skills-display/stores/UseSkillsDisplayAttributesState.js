import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSkillsDisplayAttributesState = defineStore('skillsDisplayAttributesState', () => {
  const loadingConfig = ref(true)
  const projectId = ref('')
  const serviceUrl = ref('')
  return {
    projectId,
    serviceUrl,
    loadingConfig
  }
})