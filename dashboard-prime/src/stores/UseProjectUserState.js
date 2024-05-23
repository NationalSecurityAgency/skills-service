import { defineStore } from 'pinia'
import { ref } from 'vue'
import UsersService from '@/components/users/UsersService.js'

export const useProjectUserState = defineStore('projectUserState', () => {

  const numSkills = ref(0)
  const userTotalPoints = ref(0)

  const loadUserDetailsState = (projectId, userId) => {
    return UsersService.getUserSkillsMetrics(projectId, userId)
      .then((response) => {
        numSkills.value = response.numSkills
        userTotalPoints.value = response.userTotalPoints
      })
  }

  return {
    numSkills,
    userTotalPoints,
    loadUserDetailsState
  }

})