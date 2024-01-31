import { ref } from 'vue'
import { defineStore } from 'pinia'
import ProjectsService from '@/components/projects/ProjectService'

export const useProjDetailsState = defineStore('projDetailsState',  () => {
   const project = ref(null);

    function loadProjectDetailsState(payload) {
        return new Promise((resolve, reject) => {
            ProjectsService.getProjectDetails(payload.projectId)
                .then((response) => {
                    project.value = response;
                    resolve(response);
                })
                .catch((error) => reject(error));
        });
    }

    return { project, loadProjectDetailsState }
});