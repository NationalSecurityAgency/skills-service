import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useAppVersionState = defineStore('appVersionState', () => {
  const latestLibVersion = ref('')
  const initialLibVersion = ref('')

  const updateLatestLibVersion = (incomingVersion) => {
    if (!initialLibVersion.value) {
      initialLibVersion.value = incomingVersion
    }
    if (!latestLibVersion.value || incomingVersion.localeCompare(latestLibVersion.value) > 0) {
      latestLibVersion.value = incomingVersion
    }
  }
  const isVersionDifferent = computed(() => {
    return initialLibVersion.value !== latestLibVersion.value
  })

  return {
    updateLatestLibVersion,
    isVersionDifferent
  }
})