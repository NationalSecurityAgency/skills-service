<script setup>
import { onMounted } from 'vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'
import ProgressAndRankingSplash from '@/components/myProgress/ProgressAndRankingSplash.vue'
import InfoSnapshotCard from '@/components/myProgress/InfoSnapshotCard.vue'
import NumSkills from '@/components/myProgress/NumSkills.vue'
import LastEarnedCard from '@/components/myProgress/LastEarnedCard.vue'
import BadgeNumCard from '@/components/myProgress/BadgeNumCard.vue'
import MyCurrentProjects from '@/components/myProgress/MyCurrentProjects.vue'
import MyProgressTitle from '@/components/myProgress/MyProgressTitle.vue'

const myProgressState = useMyProgressState()

onMounted(() => {
  myProgressState.loadMyProgressSummary(true)
})

</script>

<template>
  <div>
    <skills-spinner :is-loading="myProgressState.isLoadingMyProgressSummary" class="mt-8" />
    <div v-if="!myProgressState.isLoadingMyProgressSummary">
      <progress-and-ranking-splash v-if="!myProgressState.hasProjects" />
      <div v-if="myProgressState.hasProjects">
        <my-progress-title title="My Progress">
          <template #rightContent>
            <router-link :to="{ name: 'DiscoverProjectsPage' }" tabindex="-1">
              <SkillsButton
                label="Projects Catalog"
                outlined
                icon="fas fa-tasks"
                data-cy="manageMyProjsBtn"
                variant="outline-primary" />
            </router-link>
          </template>
        </my-progress-title>

        <div class="flex gap-3 flex-wrap mt-3">
          <div class="flex-1">
            <info-snapshot-card />
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

        <Divider />

        <my-current-projects />
      </div>
    </div>

  </div>
</template>

<style scoped></style>
