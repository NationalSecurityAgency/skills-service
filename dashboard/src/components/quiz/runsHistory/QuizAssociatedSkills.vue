/*
Copyright 2026 SkillTree

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
import {ref} from "vue";

const props = defineProps({
  associatedSkills: {
    type: Array,
    default: null
  },
  verticallyCompact: {
    type: Boolean,
    default: true
  }
})

const associatedSkillsExpanded = ref(false);
</script>

<template>
  <div v-if="associatedSkills && associatedSkills.length > 0" data-cy="associatedSkills" class="flex flex-col gap-1">
    <div v-for="(skill) in (associatedSkillsExpanded ? associatedSkills : associatedSkills.slice(0, 2))"
         :key="`${skill.projectId}-${skill.skillId}`"
         class="flex" :class="{ 'flex-col': !verticallyCompact, 'items-center gap-1': verticallyCompact }"
         :data-cy="`associatedSkill-${skill.projectId}-${skill.skillId}`">
      <router-link :to="{ name:'skillDetailsLocal',
                                   params: { projectId: skill.projectId, skillId: skill.skillId, subjectId: skill.subjectId }}"
                   data-cy="viewSkillLink"
                   class="underline"
                   :aria-label="`View skill ${skill.skillName}`">{{ skill.skillName }}</router-link>
      <div :class="{ 'ml-2': !verticallyCompact }"><span class="text-sm">in</span> <Tag
          severity="secondary"
          :class="{ 'p-0! border-none! bg-transparent!': !verticallyCompact }"
      ><span class="text-gray-600 dark:text-gray-400 italic" data-cy="projectName">{{ skill.projectName }}</span></Tag></div>
    </div>
    <div v-if="associatedSkills.length > 2" class="">
      <SkillsButton
          data-cy="expandOrCollapseSkills"
          @click="associatedSkillsExpanded = !associatedSkillsExpanded"
          link
          class="p-0! text-blue-600!  dark:text-blue-400! underline"
          :aria-label="associatedSkillsExpanded ? 'Show fewer skills' : `Show ${associatedSkills.length - 2} more skills`">
        {{ associatedSkillsExpanded ? 'Show Less' : `View ${associatedSkills.length - 2} More` }}
      </SkillsButton>
    </div>
  </div>
</template>

<style scoped>

</style>