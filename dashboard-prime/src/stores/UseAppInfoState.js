import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import SettingsService from '@/components/settings/SettingsService.js'

export const useAppInfoState = defineStore('useAppInfoState', () => {
  const previousUrlState = ref({})
  const setPreviousUrl = (url) => {
    previousUrlState.value = url
  }
  const previousUrl = computed(() => previousUrlState.value)

  const emailEnabledState = ref(false)
  const loadEmailEnabled = () => {
    return SettingsService.isEmailServiceSupported()
      .then((enabled) => {
        emailEnabledState.value = enabled
      })
  }
  const emailEnabled = computed(() => emailEnabledState.value)

  const showUaState = ref(false)
  const setShowUa = (newVal) => {
    showUaState.value = newVal
  }
  const showUa = computed(() => showUaState.value)

  return {
    previousUrl,
    setPreviousUrl,
    loadEmailEnabled,
    emailEnabled,
    showUa,
    setShowUa
  }

})