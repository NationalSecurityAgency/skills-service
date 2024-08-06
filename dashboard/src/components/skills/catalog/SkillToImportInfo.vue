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
import DateCell from '@/components/utils/table/DateCell.vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'

const props = defineProps({
  skill: {
    type: Object,
    required: true,
  }
})
const selfReport = computed(() => {
  if (!props.skill.selfReportingType) {
    return 'N/A';
  }
  if (props.skill.selfReportingType === 'Quiz') {
    return 'Quiz/Survey';
  }
  return (props.skill.selfReportingType === 'Approval') ? 'Requires Approval' : 'Honor System';
})
</script>

<template>
  <div :data-cy="`skillToImportInfo-${skill.projectId}_${skill.skillId}`">
    <div class="md:flex my-1">
      <div class="flex-1">
        <i class="fas fa-laptop skills-color-selfreport" aria-hidden="true" /> <span class="font-italic">Self Report:</span> <span class="text-primary">{{ selfReport }}</span>
      </div>
      <div class="md:flex my-1">
        <div class="align-items-center">
          <i class="fas fa-book text-info"></i> <span class="font-italic">Exported:</span>
        </div>
        <date-cell :value="skill.exportedOn" class="ml-2" />
      </div>
    </div>
    <div class="mb-3">
        <div class="align-items-start flex">
          <span class="font-italic mr-2">Project ID: </span><span class="text-primary" data-cy="projId">{{ skill.projectId }}</span>
        </div>
        <div class="align-items-start flex my-1">
          <span class="font-italic mr-2">Skill ID: </span><span class="text-primary max-wrap" data-cy="skillId">{{ skill.skillId }}</span>
        </div>
        <div>
          <span class="font-italic">Points: </span><span class="text-primary" data-cy="totalPts">{{ skill.totalPoints}}</span><span> ({{ skill.pointIncrement }} Increment x {{ skill.numPerformToCompletion }} Occurrences)</span>
        </div>
    </div>
    <Card>
      <template #title>Description</template>
      <template #content>
        <markdown-text
          v-if="skill.description"
          :text="skill.description"
          :instance-id="`${skill.projectId}_${skill.skillId}`"
          data-cy="importedSkillInfoDescription" />
        <p v-else>
          Not Specified
        </p>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.max-wrap {
  max-width: 30rem;
  word-wrap: break-word;
  display: inline-block;
}
</style>