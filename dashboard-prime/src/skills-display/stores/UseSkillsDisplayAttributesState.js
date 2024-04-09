import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSkillsDisplayAttributesState = defineStore('skillsDisplayAttributesState', () => {
  const projectId = ref('')

  return {
    projectId,
  }
})