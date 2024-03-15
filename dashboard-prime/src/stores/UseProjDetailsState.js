import { ref } from 'vue'
import { defineStore } from 'pinia'
import ProjectsService from '@/components/projects/ProjectService'
import { useRoute } from 'vue-router'

export const useProjDetailsState = defineStore('projDetailsState', () => {
  const project = ref(null)
  const isLoading = ref(true)
  const route = useRoute()

  function loadProjectDetailsState(updateLoading = false) {
    if(updateLoading) {
      isLoading.value = true
    }
    ProjectsService.getProjectDetails(route.params.projectId)
      .then((response) => {
        project.value = response
      })
      .finally(() => {
        isLoading.value = false
      })
  }

  return {
    project,
    loadProjectDetailsState,
    isLoading
  }
})