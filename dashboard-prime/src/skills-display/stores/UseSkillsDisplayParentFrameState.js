import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSkillsDisplayParentFrameState = defineStore('skillsDisplayParentFrameState', () => {
  const authToken = ref('')
  const isAuthenticating = ref(false)
  const parentFrame = ref(null)
  const serviceUrl = ref('')
  const options = ref ({})
  return {
    authToken,
    isAuthenticating,
    parentFrame,
    serviceUrl,
    options
  }
})