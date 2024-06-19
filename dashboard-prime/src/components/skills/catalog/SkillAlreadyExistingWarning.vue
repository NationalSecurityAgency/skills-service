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