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