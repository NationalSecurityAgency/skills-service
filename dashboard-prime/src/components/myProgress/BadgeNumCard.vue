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
      <div class="mt-2 ml-1 text-dark">
        <span style="font-size: 2.5rem;" class="text-warning"
              data-cy="numAchievedBadges">{{ myProgress.numAchievedBadges }}</span> <span class="text-secondary"
                                                                                          style="font-size: 1.2rem;"
                                                                                          data-cy="numBadgesAvailable">/ {{ myProgress.totalBadges
        }}</span>
      </div>
      <div v-if="myProgress.totalGlobalBadges > 0">
        <Tag severity="info" data-cy="numAchievedGlobalBadges">Global Badges: {{ myProgress.numAchievedGlobalBadges
          }} / {{ myProgress.totalGlobalBadges }}
        </Tag>
      </div>
      <div v-if="myProgress.totalGems > 0">
        <Tag severity="info" data-cy="numAchievedGemBadges">Gems: {{ myProgress.numAchievedGemBadges }} /
          {{ myProgress.totalGems }}
        </Tag>
      </div>
    </template>
    <template #right-content>
      <div style="font-size: 4.4rem;">
      <span class="fa-stack">
            <i class="fas fa-circle fa-stack-2x text-secondary"></i>
            <i class="fas fa-trophy fa-stack-1x text-0"></i>
          </span>
      </div>
    </template>
    <template #footer>
      <div class="flex">
        <div data-cy="badges-num-footer" class="flex-1">
          Be proud to earn those badges!!
        </div>
        <!--          :to="{ name: 'MyBadges' }"-->
        <SkillsButton
          label="My Badges"
          icon="fas fa-award"
          outlined
          size="small"
          data-cy="viewBadges" />
      </div>
    </template>
  </my-progress-info-card-util>
</template>

<style scoped>

</style>