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
import { onMounted, ref } from 'vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'
import ProgressAndRankingSplash from '@/components/myProgress/ProgressAndRankingSplash.vue'
import InfoSnapshotCard from '@/components/myProgress/InfoSnapshotCard.vue'
import NumSkills from '@/components/myProgress/NumSkills.vue'
import LastEarnedCard from '@/components/myProgress/LastEarnedCard.vue'
import BadgeNumCard from '@/components/myProgress/BadgeNumCard.vue'
import MyCurrentProjects from '@/components/myProgress/MyCurrentProjects.vue'
import MyProgressTitle from '@/components/myProgress/MyProgressTitle.vue'
import MyQuizzesCard from "@/components/myProgress/MyQuizzesCard.vue";

const myProgressState = useMyProgressState()
const loading = ref(true)
onMounted(() => {
  myProgressState.loadMyProgressSummary().then(() => {
    loading.value = false
  })
})

</script>

<template>
  <div>
    <skills-spinner :is-loading="myProgressState.isLoadingMyProgressSummary || loading" class="mt-20" />
    <div v-if="!myProgressState.isLoadingMyProgressSummary && !loading">
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

        <div class="flex flex-col sm:flex-row gap-4 flex-wrap mt-4">
          <div class="flex-1">
            <info-snapshot-card />
          </div>
          <div class="flex-1">
            <my-quizzes-card />
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
