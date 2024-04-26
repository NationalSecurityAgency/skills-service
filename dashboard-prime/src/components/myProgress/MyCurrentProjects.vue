<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'
import { useElementHelper } from '@/components/utils/inputForm/UseElementHelper.js';
import ProjectService from '@/components/projects/ProjectService.js'
import ProjectLinkCard from '@/components/myProgress/ProjectLinkCard.vue'
import Sortable from 'sortablejs'

const myProgressState = useMyProgressState()
const elementHelper = useElementHelper()
const sortOrderLoading = ref(false)
const sortOrderLoadingProjectId = ref(-1)

const updateSortAndReloadProjects = (updateInfo) => {
  const currentIndex = myProgressState.myProjects.sort((a, b) => {
    if (a.displayOrder > b.displayOrder) {
      return 1
    }
    if (b.displayOrder > a.displayOrder) {
      return -1
    }
    return 0
  })
    .findIndex((item) => item.projectId === updateInfo.projectId)
  const newIndex = updateInfo.direction === 'up' ? currentIndex - 1 : currentIndex + 1
  projectOrderUpdate(updateInfo.projectId, newIndex)
    .then(() => {

    })
  // if (newIndex >= 0 && (newIndex) < this.myProjects.length) {
  //   this.loading = true
  //   ProjectService.moveMyProject(updateInfo.projectId, newIndex)
    // .finally(() => {
    //   this.loadSummaryAndEnableSummary()
    //     .then(() => {
    //       const foundRef = this.$refs[`proj${updateInfo.projectId}`];
    //       this.$nextTick(() => {
    //         foundRef[0].focusSortControl();
    //       });
    //     });
    // });
  // }
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
      });
    })
  }
}

const projectOrderUpdate = (projectId, newIndex) => {
  sortOrderLoadingProjectId.value = projectId;
  sortOrderLoading.value = true;
  return ProjectService.moveMyProject(projectId, newIndex)
    .finally(() => {
      sortOrderLoading.value = false;
    });
}

</script>

<template>
  <div class="grid" id="projectCards">
    <div v-for="(proj, index) in myProgressState.myProjects"
         :key="proj.projectName"
         :id="proj.projectId"
         class="col">
      <!--      <b-overlay :show="sortOrderLoading" rounded="sm" opacity="0.4">-->
      <!--        <template #overlay>-->
      <!--          <div class="text-center">-->
      <!--            <div v-if="proj.projectId===sortOrderLoadingProjectId">-->
      <!--              <div class="text-info text-uppercase mb-1">Updating sort order!</div>-->
      <!--              <b-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info" />-->
      <!--            </div>-->
      <!--          </div>-->
      <!--        </template>-->

      <project-link-card

        :ref="`proj${proj.projectId}`"
        :display-order="index"
        @sort-changed-requested="updateSortAndReloadProjects"
        :proj="proj"
        class="fadein animation-duration-500" />
      <!--      </b-overlay>-->
    </div>
  </div>
</template>

<style scoped>

</style>