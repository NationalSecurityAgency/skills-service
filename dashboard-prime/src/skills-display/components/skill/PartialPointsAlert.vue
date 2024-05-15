<script setup>
import { computed } from 'vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const props = defineProps({
  skill: Object,
  isLocked: Boolean,
})
const attributes = useSkillsDisplayAttributesState()

const isPartiallyAchievedAndLocked = computed(() => {
  return props.isLocked && props.skill.points > 0 && props.skill.points !== props.skill.totalPoints;
})
const isFullyAchievedAndLocked = computed(() => {
  return props.isLocked && props.skill.points === props.skill.totalPoints;
})

</script>

<template>
  <div v-if="isPartiallyAchievedAndLocked || isFullyAchievedAndLocked" class="mt-2">
    <Message v-if="isPartiallyAchievedAndLocked" :closable="false">
      You were able to earn partial points before the prerequisites were added. Don't worry you get to keep the points!!!
      Accomplish all of the prerequisites to unlock the rest of the {{ attributes.skillDisplayName.toLowerCase() }}'s points!
    </Message>

    <Message v-if="isFullyAchievedAndLocked" :closable="false" severity="success">
      Congrats! You completed this {{ attributes.skillDisplayName.toLowerCase() }} before the prerequisites were added. Don't worry, you get to keep the points!!!
    </Message>
  </div>
</template>

<style scoped>

</style>