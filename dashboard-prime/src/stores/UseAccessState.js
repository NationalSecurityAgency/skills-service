import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import AccessService from '@/components/access/AccessService'

export const useAccessState = defineStore('accessState', () => {
  const isSupervisorState = ref(false)
  const loadIsSupervisor = () => {
    return AccessService.hasRole('ROLE_SUPERVISOR')
      .then((result) => {
        isSupervisorState.value = result
      })
  }
  const isSupervisor = computed(() => isSupervisorState.value)

  const isRootState = ref(false)
  const loadIsRoot = () => {
    return AccessService.hasRole('ROLE_SUPER_DUPER_USER')
      .then((result) => {
        isRootState.value = result
      })
  }
  const isRoot = computed(() => isRootState.value)

  return {
    isSupervisor,
    isRoot,
    loadIsSupervisor,
    loadIsRoot
  }
})