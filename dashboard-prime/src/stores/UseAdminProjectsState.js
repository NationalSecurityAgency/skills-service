import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import ProjectService from '@/components/projects/ProjectService.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

export const useAdminProjectsState = defineStore('adminProjectsState', () => {
  const appConfig = useAppConfig()

  const projects = ref([]);
  const isLoadingProjects = ref(true);
  const loadProjects = () => {
    isLoadingProjects.value = true;
    return ProjectService.getProjects()
      .then((response) => {
        if(response && Object.keys(response).length > 0) {
          projects.value = response.map((p) => ({...p, description: p.description || ''}))
        } else {
          projects.value = []
        }
      })
      .finally(() => {
        isLoadingProjects.value = false;
      });
  }

  const updateOrAddProject = (project) => {
    const existingIndex = projects.value.findIndex((item) => item.projectId === project.originalProjectId)
    if (existingIndex >= 0) {
      projects.value.splice(existingIndex, 1, project)
      return true
    }
    projects.value.push(project)
    return false
  }

  const shouldTileProjectsCards = computed(() => {
    return projects.value.length >= appConfig.numProjectsToStartShowingAsCards
  })

  return {
    loadProjects,
    isLoadingProjects,
    projects,
    updateOrAddProject,
    shouldTileProjectsCards
  }
})