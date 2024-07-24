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
import { computed } from 'vue'
import MyProgressInfoCardUtil from '@/components/myProgress/MyProgressInfoCardUtil.vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'

const myProgressState = useMyProgressState()
const myProgress = computed(() => myProgressState.myProgress)


</script>

<template>
  <my-progress-info-card-util title="Badges">
    <template #left-content>
      <div class="">
        <span
          class="text-4xl text-orange-500"
          data-cy="numAchievedBadges">{{ myProgress.numAchievedBadges }}</span>
        <span
          class="text-secondary"
          data-cy="numBadgesAvailable">/ {{ myProgress.totalBadges
          }}</span>
      </div>
      <div v-if="myProgress.globalBadgeCount > 0">
        <Tag
          severity="secondary"
          class="mt-1"
          data-cy="numAchievedGlobalBadges">Global Badges:
          {{ myProgress.numAchievedGlobalBadges
          }} / {{ myProgress.globalBadgeCount }}
        </Tag>
      </div>
      <div v-if="myProgress.gemCount > 0">
        <Tag
          severity="warning"
          class="mt-1"
          data-cy="numAchievedGemBadges">Gems:
          {{ myProgress.numAchievedGemBadges }} /
          {{ myProgress.gemCount }}
        </Tag>
      </div>
    </template>
    <template #right-content>
      <div class="flex justify-content-center sm:justify-content-end">
        <div class="flex justify-content-center">
          <div class="border-circle w-7rem h-7rem m-2 bg-bluegray-600 font-bold flex align-items-center justify-content-center">
            <i class="text-green-500 text-7xl fas fa-trophy"></i>
          </div>
        </div>
      </div>
    </template>
    <template #footer>
      <div class="flex gap-2 align-items-center flex-column sm:flex-row">
        <div data-cy="badges-num-footer" class="flex-1 w-min-10rem">
          Be proud to earn those badges!!
        </div>
        <div>
        <router-link :to="{ name: 'MyBadges' }" tabindex="-1">
          <SkillsButton
            label="My Badges"
            icon="fas fa-award"
            outlined
            size="small"
            data-cy="viewBadges" />
        </router-link>
        </div>
      </div>
    </template>
  </my-progress-info-card-util>
</template>

<style scoped>

</style>