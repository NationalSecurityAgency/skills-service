/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'
import { useElementHelper } from '@/components/utils/inputForm/UseElementHelper.js'
import ProjectService from '@/components/projects/ProjectService.js'
import ProjectLinkCard from '@/components/myProgress/ProjectLinkCard.vue'
import Sortable from 'sortablejs'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import Badge from '@/components/badges/Badge.vue'

const myProgressState = useMyProgressState()
const elementHelper = useElementHelper()
const sortOrderLoading = ref(false)
const sortOrderLoadingProjectId = ref(-1)

const updateSortAndReloadProjects = (updateInfo) => {
  const currentIndex = myProgressState.myProjects.findIndex((item) => item.projectId === updateInfo.projectId)
  const newIndex = updateInfo.direction === 'up' ? currentIndex - 1 : currentIndex + 1
  if (newIndex >= 0 && newIndex < myProgressState.myProjects.length) {
    projectOrderUpdate(updateInfo.projectId, newIndex)
      .finally(() => {
        const idToFocusOn = `sortControlHandle-${updateInfo.projectId}`
        nextTick(() => document.getElementById(idToFocusOn)?.focus())
      })
  }
}

onMounted(() => {
  enableProjectDropAndDrop()
})

const enableProjectDropAndDrop = () => {
  if (myProgressState.hasProjects) {
    nextTick(() => {
      elementHelper.getElementById('projectCards').then((cards) => {
        Sortable.create(cards, {
          handle: '.sort-control',
          animation: 150,
          ghostClass: 'skills-sort-order-ghost-class',
          onUpdate(event) {
            projectOrderUpdate(event.item.id, event.newIndex)
          }
        })
      })
    })
  }
}

const projectOrderUpdate = (projectId, newIndex) => {
  sortOrderLoadingProjectId.value = projectId
  sortOrderLoading.value = true

  const currentProjects = myProgressState.myProjects
  const currentIndex = currentProjects.findIndex((item) => item.projectId === projectId)

  const itemToMove = currentProjects[currentIndex]
  const updatedProjects = [...currentProjects]

  updatedProjects.splice(currentIndex, 1)
  updatedProjects.splice(newIndex, 0, itemToMove)

  return ProjectService.moveMyProject(projectId, newIndex)
    .finally(() => {
      sortOrderLoading.value = false
      myProgressState.myProjects = updatedProjects
    })
}

const removeProject = (projectId) => {
  sortOrderLoading.value = true;

  const currentProjects = myProgressState.myProjects
  const itemToRemove = currentProjects.find(it => it.projectId === projectId);
  const indexToRemove = currentProjects.indexOf(itemToRemove);
  const updatedProjects = [...currentProjects]
  updatedProjects.splice(indexToRemove, 1)

  ProjectService.removeFromMyProjects(projectId).finally(() => {
    sortOrderLoading.value = false
    myProgressState.myProjects = updatedProjects
    myProgressState.loadMyProgressSummary(true)
  })
}
</script>

<template>
  <BlockUI :blocked="sortOrderLoading">
    <skills-spinner :is-loading="true"
                    v-if="sortOrderLoading"
                    aria-label="Updating sort order"
                    class="loading-indicator" />
    <div class="grid grid-cols-12 gap-4" id="projectCards">

      <div v-for="(proj, index) in myProgressState.myProjects"
           :key="proj.projectName"
           :id="proj.projectId"
           class="col-span-12 lg:col-span-6 project-link-container">
        <project-link-card
          :ref="`proj${proj.projectId}`"
          :display-order="index"
          @sort-changed-requested="updateSortAndReloadProjects"
          @remove-project="removeProject"
          :proj="proj"
          class="h-full" />
      </div>
    </div>
  </BlockUI>
</template>

<style scoped>
.loading-indicator {
  position: absolute;
  z-index: 999;
  height: 2em;
  width: 2em;
  overflow: show;
  margin: auto;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
}

</style>