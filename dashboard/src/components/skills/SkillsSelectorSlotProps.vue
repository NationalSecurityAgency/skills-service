/*
Copyright 2025 SkillTree

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
import SkillReuseIdUtil from "@/components/utils/SkillReuseIdUtil.js";

const props = defineProps({
  options: Object,
  showType: {
    type: Boolean,
    default: false,
  },
  showProject: {
    type: Boolean,
    default: false,
  },
})
const removeReuseTag = (val) => {
  return SkillReuseIdUtil.removeTag(val);
};
</script>

<template>
  <div :data-cy="`skillsSelectionItem-${options.projectId}-${options.skillId}`">
    <div class="text-xl text-info skills-option-name" data-cy="skillsSelector-skillName"><span
        v-if="showType">{{
        options.type
      }}:</span> {{ options.name }}
      <Tag v-if="options.isReused" variant="success" size="sm" class="uppercase"
           data-cy="reusedBadge"
           style="font-size: 0.85rem !important;"><i class="fas fa-recycle"></i> reused
      </Tag>
    </div>
    <div style="font-size: 0.8rem;">
              <span class="skills-option-id">
                <span v-if="showProject" data-cy="skillsSelectionItem-projectId"><span
                    class="uppercase mr-1 italic">Project ID:</span><span
                    class="font-bold"
                    data-cy="skillsSelector-projectId">{{ options.projectId }}</span></span>
              </span>
      <span v-if="options.type === 'Skill'" class="uppercase mr-1 italic"
            data-cy="skillsSelectionItem-subjectId">Subject:</span>
      <span v-if="options.type === 'Skill'"
            class="font-bold skills-option-subject-name"
            data-cy="skillsSelector-subjectName">{{ options.subjectName }}</span>
      <span v-if="options.type === 'Shared Skill'" class="uppercase mr-1 italic"
            data-cy="skillsSelectionItem-projectName">Project:</span>
      <span v-if="options.type === 'Shared Skill'"
            class="font-bold skills-option-subject-name"
            data-cy="skillsSelector-projectName">{{ options.projectName }}</span>
      <span v-if="options.groupName">
                <span class="mx-2">|</span>
                <span class="uppercase mr-1 italic skills-option-group-name"
                      data-cy="skillsSelectionItem-group">Group:</span><span
          class="font-bold skills-id"
          data-cy="skillsSelector-groupName">{{ options.groupName }}</span>
              </span>
    </div>
  </div>
</template>

<style scoped>

</style>