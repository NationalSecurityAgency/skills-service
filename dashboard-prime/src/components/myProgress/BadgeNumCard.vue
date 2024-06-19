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
          class="text-4xl text-orange-500 mr-1"
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
      <div style="font-size: 3.5rem;" class="mt-2">
      <span class="fa-stack">
            <i class="fas fa-circle fa-stack-2x text-bluegray-600"></i>
            <i class="fas fa-trophy fa-stack-1x text-green-500"></i>
          </span>
      </div>
    </template>
    <template #footer>
      <div class="flex">
        <div data-cy="badges-num-footer" class="flex-1">
          Be proud to earn those badges!!
        </div>
        <router-link :to="{ name: 'MyBadges' }" tabindex="-1">
          <SkillsButton
            label="My Badges"
            icon="fas fa-award"
            outlined
            size="small"
            data-cy="viewBadges" />
        </router-link>
      </div>
    </template>
  </my-progress-info-card-util>
</template>

<style scoped>

</style>