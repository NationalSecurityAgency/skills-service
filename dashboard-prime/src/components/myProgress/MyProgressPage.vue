<script setup>
import { onMounted } from 'vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'
import ProgressAndRankingSplash from '@/components/myProgress/ProgressAndRankingSplash.vue'
import InfoSnapshotCard from '@/components/myProgress/InfoSnapshotCard.vue'
import NumSkills from '@/components/myProgress/NumSkills.vue'
import LastEarnedCard from '@/components/myProgress/LastEarnedCard.vue'
import BadgeNumCard from '@/components/myProgress/BadgeNumCard.vue'

const myProgressState = useMyProgressState()

onMounted(() => {
  myProgressState.loadMyProgressSummary()
})

</script>

<template>
  <div>
    <skills-spinner :is-loading="myProgressState.isLoadingMyProgressSummary" class="mt-8" />
    <div v-if="!myProgressState.isLoadingMyProgressSummary">
      <progress-and-ranking-splash v-if="!myProgressState.hasProjects" />
      <Card v-if="myProgressState.hasProjects">
        <template #header>
          <div class="flex pt-4 px-3">
            <div class="flex-1">
              <span class="text-2xl uppercase">My Projects</span>
            </div>
            <div>
              <router-link :to="{ name: 'DiscoverProjectsPage' }">
                <SkillsButton
                  label="Projects Catalog"
                  outlined
                  icon="fas fa-tasks"
                  data-cy="manageMyProjsBtn"
                  variant="outline-primary" />
              </router-link>
            </div>
          </div>
        </template>
        <template #content>
          <div class="flex gap-3">
            <div class="flex-1">
              <info-snapshot-card :projects="myProgressState.myProjects"
                                  :num-projects-contributed="myProgressState.myProgress.numProjectsContributed" />
            </div>
            <div class="flex-1 h-full">
              <num-skills />
            </div>
            <div class="flex-1">
              <last-earned-card />
            </div>
            <div class="flex-1">
              <badge-num-card />
            </div>
          </div>

        </template>
      </Card>
    </div>

  </div>
</template>

<style scoped></style>
