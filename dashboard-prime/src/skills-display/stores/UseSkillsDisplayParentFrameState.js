import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useSkillsDisplayParentFrameState = defineStore('skillsDisplayParentFrameState', () => {
  const authToken = ref('')
  const isAuthenticating = ref(false)
  const parentFrame = ref(null)
  const serviceUrl = ref('')
  const options = ref ({})
  const isLastViewedScrollSupported = computed(() => {
    if (!parentFrame.value) {
      return true
    }

    return options.value && Object.keys(options.value).length > 0;
  })
  return {
    authToken,
    isAuthenticating,
    parentFrame,
    serviceUrl,
    options,
    isLastViewedScrollSupported
  }
})