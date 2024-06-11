<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'
import { useElementHelper } from '@/components/utils/inputForm/UseElementHelper.js'
import ProjectService from '@/components/projects/ProjectService.js'
import ProjectLinkCard from '@/components/myProgress/ProjectLinkCard.vue'
import Sortable from 'sortablejs'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'

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

</script>

<template>
  <BlockUI :blocked="sortOrderLoading">
    <skills-spinner :is-loading="true"
                    v-if="sortOrderLoading"
                    aria-label="Updating sort order"
                    class="loading-indicator" />
    <div class="grid" id="projectCards">

      <div v-for="(proj, index) in myProgressState.myProjects"
           :key="proj.projectName"
           :id="proj.projectId"
           class="col">
        <project-link-card
          :ref="`proj${proj.projectId}`"
          :display-order="index"
          @sort-changed-requested="updateSortAndReloadProjects"
          :proj="proj"
          class="fadein animation-duration-500" />
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