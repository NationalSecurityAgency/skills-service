import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSkillsDisplayPreferencesState = defineStore('skillsDisplayPreferencesStateState', () => {
  const isSummaryOnly = ref(false)
  const internalBackButton = ref(true)

  return {
    isSummaryOnly,
    internalBackButton
  }
})