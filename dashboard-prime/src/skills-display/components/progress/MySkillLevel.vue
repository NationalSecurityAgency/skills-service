<script setup>
import { computed } from 'vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const props = defineProps({
  userProgress: Object,
})
const attributes = useSkillsDisplayAttributesState()

const level = computed(() => props.userProgress.skillsLevel)
const totalLevels = computed(() => props.userProgress.totalLevels)
</script>

<template>
  <div class="progress-circle-wrapper" data-cy="overallLevel">
    <label class="text-2xl" data-cy="overallLevelTitle">My {{ attributes.levelDisplayName }}</label>
    <div class="mt-4">
      <div class="fa-stack skills-icon trophy-stack">
        <i class="fa fa-trophy fa-stack-2x" />
        <i class="fa fa-star fa-stack-1x trophy-star" />
        <strong class="fa-stack-1x trophy-text">{{ level }}</strong>
      </div>
    </div>
    <div data-cy="overallLevelDesc" class="mt-3">
      {{ attributes.levelDisplayName }} <Tag severity="info">{{ level }}</Tag> out of <Tag>{{ totalLevels }}</Tag>
    </div>
    <div class="flex justify-content-center mt-2 overall-progress-stars-icons">
      <Rating v-model="level" :stars="totalLevels" readonly :cancel="false" />
    </div>
  </div>
</template>

<style>
.overall-progress-stars-icons .p-rating-icon {
  width:2rem;
  height:2rem;
}
</style>
<style scoped>

/* Font awesome gives a 2em width which doesnt fit the full trophy at font-size:60px. A bug? */
.trophy-stack.fa-stack {
  width: 3em;
  font-size: 60px;
}

.trophy-text {
  margin-top: -0.65em;
  font-size: 0.5em;
  color: #333;
}

.skills-icon {
  display: inline-block;
  color: #b1b1b1;
  margin: 5px 0;
}

.trophy-star {
  color: #ffffff;
  margin-top: -0.35em;
  font-size: 0.9em;
}

.trophy-text {
  margin-top: -0.65em;
  font-size: 0.5em;
  color: #333;
}
</style>