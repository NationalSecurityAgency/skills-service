import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'

export const useUserProgressSummaryState = defineStore('userProgressSummaryState', () => {

  const skillsDisplayService = useSkillsDisplayService()

  const loadingUserProgressSummary = ref(true)
  const userProgressSummary = ref({})
  const loadingUserSkillsRanking = ref(true)
  const userRanking = ref({})

  const loadUserProgressSummary = () => {
    return skillsDisplayService.loadUserProjectSummary()
      .then((result) => {
        userProgressSummary.value = result
      }).finally(() => {
        loadingUserProgressSummary.value = false
      })
  }

  const loadUserSkillsRanking = (subjectId) => {
    loadingUserSkillsRanking.value = true
    return skillsDisplayService.loadUserSkillsRanking(subjectId)
      .then((result) => {
        userRanking.value = result
      }).finally(() => {
        loadingUserSkillsRanking.value = false
      })
  }

  return {
    userProgressSummary,
    loadUserProgressSummary,
    loadingUserProgressSummary,

    userRanking,
    loadUserSkillsRanking,
    loadingUserSkillsRanking,
  }

})