import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSkillsDisplayPreferencesState = defineStore('skillsDisplayPreferencesStateState', () => {
  const isSummaryOnly = ref(false)
  const internalBackButton = ref(true)

  const projectDisplayName = ref('Project')
  const subjectDisplayName = ref('Subject')
  const groupDisplayName = ref('Group')
  const skillDisplayName = ref('Skill')

  return {
    isSummaryOnly,
    internalBackButton,
    projectDisplayName,
    subjectDisplayName,
    groupDisplayName,
    skillDisplayName,
  }
})