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
import {computed, ref} from "vue";

const props = defineProps({
  approvers: {
    type: Array,
    required: true
  }
})

const sortedApprovers = computed(() => {
  return props.approvers?.sort((a, b) => a.approverUserId.localeCompare(b.approverUserId))
})
const expanded = ref(false);
</script>

<template>
  <div v-if="sortedApprovers" class="flex flex-col gap-1" data-cy="assignedApprovers">
    <div v-for="(approver, index) in (sortedApprovers.length <= 3 || expanded ? sortedApprovers : sortedApprovers.slice(0, 2))"
         :key="approver.approverUserId"
         :data-cy="`approver-${index}`">
      <div class="flex flex-col">
        <div data-cy="approverId">{{ approver.approverUserId }}</div>
        <div class="flex items-center gap-1 pl-2 font-light text-sm">
          <div>Conf:</div>
          <div data-cy="approverConfTypes">{{ approver.configuredTypes.sort().join(', ') }}</div>
        </div>
      </div>
    </div>
    <div v-if="sortedApprovers?.length > 3" class="">
      <SkillsButton
          data-cy="expandOrCollapse"
          @click="expanded = !expanded"
          link
          class="p-0! text-blue-600!  dark:text-blue-400! underline"
          :aria-label="expanded ? 'Show fewer approvers' : `Show ${approvers.length - 2} more approvers`">
        {{ expanded ? 'Show Less' : `View ${approvers.length - 2} More` }}
      </SkillsButton>
    </div>
  </div>
</template>

<style scoped>

</style>