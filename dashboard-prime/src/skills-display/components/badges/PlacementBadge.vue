<script setup>
import { useTopPositionsUtils } from '@/skills-display/components/badges/UseTopPositionsUtils.js'

const props = defineProps({
  badge: Object,
})
const topPositions = useTopPositionsUtils()
</script>

<template>
  <div v-if="badge.achievementPosition > 0 && badge.achievementPosition <= 3">
    <Badge
      :aria-label="`You finished in ${topPositions.positionNameShort[badge.achievementPosition - 1]} place`"
    ><i class="fas fa-award"></i> {{ topPositions.positionNameShort[badge.achievementPosition - 1]
      }} place
    </Badge>
  </div>
</template>

<style scoped>

</style>