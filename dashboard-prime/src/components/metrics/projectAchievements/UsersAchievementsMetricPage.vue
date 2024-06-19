<script setup>
import LevelBreakdownMetric from '@/components/metrics/common/LevelBreakdownMetric.vue'
import AchievementsNavigator from '@/components/metrics/projectAchievements/AchievementsNavigator.vue'
</script>

<template>
  <div>
    <div class="flex flex-column gap-3">
      <level-breakdown-metric />
      <achievements-navigator />
    </div>
  </div>
</template>

<style scoped></style>
