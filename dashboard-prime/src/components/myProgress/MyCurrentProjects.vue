<script setup>
import { ref } from 'vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'
import ProjectService from '@/components/projects/ProjectService.js'
import ProjectLinkCard from '@/components/myProgress/ProjectLinkCard.vue'

const myProgressState = useMyProgressState()
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
  if (newIndex >= 0 && (newIndex) < this.myProjects.length) {
    this.loading = true
    ProjectService.moveMyProject(updateInfo.projectId, newIndex)
    // .finally(() => {
    //   this.loadSummaryAndEnableSummary()
    //     .then(() => {
    //       const foundRef = this.$refs[`proj${updateInfo.projectId}`];
    //       this.$nextTick(() => {
    //         foundRef[0].focusSortControl();
    //       });
    //     });
    // });
  }
}
// const animationDelayOptions = [200, 500, 100]
</script>

<template>
  <div class="flex gap-4 flex-wrap" id="projectCards">
    <div v-for="(proj, index) in myProgressState.myProjects"
         :key="proj.projectName"
         :id="proj.projectId"
         class="flex-1">
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