import { ref, computed } from 'vue'
import MyProgressService from '@/components/myProgress/MyProgressService.js'
import { defineStore } from 'pinia'

export const useMyProgressState = defineStore('myProgressState', () => {
  const myProjects = ref([])
  const myProgress = ref({})
  const isLoadingMyProgressSummary = ref(true)

  const setMyProgress = (incomingProgress) => {
    myProgress.value = incomingProgress
    myProjects.value = incomingProgress.projectSummaries
  }

  const loadMyProgressSummary = () => {
    isLoadingMyProgressSummary.value = true
    MyProgressService.loadMyProgressSummary()
      .then((response) => {
        setMyProgress(response)
      }).finally(() => {
      isLoadingMyProgressSummary.value = false
    })
  }

  const hasProjects = computed(() => myProjects.value.length > 0)

  return {
    isLoadingMyProgressSummary,
    loadMyProgressSummary,
    hasProjects,
    myProjects,
    myProgress,
  }
})