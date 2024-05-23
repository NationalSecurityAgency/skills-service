<script setup>
import LevelBreakdownMetric from "@/components/metrics/common/LevelBreakdownMetric.vue";
import AchievementsNavigator from "@/components/metrics/projectAchievements/AchievementsNavigator.vue";
</script>

<template>
  <div>
    <div class="flex gap-2">
      <div class="col">
        <level-breakdown-metric />
      </div>
    </div>
    <achievements-navigator />
  </div>
</template>

<style scoped></style>
