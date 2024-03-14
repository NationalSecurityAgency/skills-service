<script setup>
import { computed } from 'vue'
const props = defineProps({
  skill: {
    type: Object,
    required: true
  }
})
const itemThatDoesNotExist = computed(() => {
  if (props.skill.skillIdAlreadyExist && props.skill.skillNameAlreadyExist) {
    return 'Skill ID and name';
  }

  if (props.skill.skillIdAlreadyExist) {
    return 'Skill ID';
  }

  return 'Skill name';
})
</script>

<template>
  <InlineMessage v-if="skill.skillIdAlreadyExist || skill.skillNameAlreadyExist" severity="warn"
       :data-cy="`alreadyExistWarning_${skill.projectId}-${skill.skillId}`" class="mb-2">
    <span class="font-semibold mr-1">Cannot import!</span>
    <span class="text-primary">{{ itemThatDoesNotExist }}</span> already
    {{ skill.skillIdAlreadyExist && skill.skillNameAlreadyExist ? 'exist' : 'exists' }} in this project!
  </InlineMessage>
</template>

<style scoped>

</style>