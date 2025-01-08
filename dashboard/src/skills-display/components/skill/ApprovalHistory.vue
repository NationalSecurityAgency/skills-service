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
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import ApprovalEventMessage from '@/skills-display/components/skill/ApprovalEventMessage.vue';

const props = defineProps({
  events: {
    type: Array,
    required: true,
  },
})

const timeUtils = useTimeUtils()

const REQUESTED = 'Approval Requested'
// const PENDING = 'Pending Approval'
const APPROVED = 'Approved'
const REJECTED = 'Rejected'
const AWAITING_GRADING = "Awaiting Grading"
const PASSED = "Passed"
const FAILED = "Failed"
const COMPLETED = "Completed"

const getIconClass = (item) => {
  const prefix = 'fas fa-'
  if (item.eventStatus === REQUESTED || item.eventStatus === AWAITING_GRADING) {
    return `${prefix}clock`;
  } else if (item.eventStatus === APPROVED || item.eventStatus === PASSED || item.eventStatus === COMPLETED) {
    return `${prefix}check`;
  } else if (item.eventStatus === REJECTED || item.eventStatus === FAILED) {
    return `${prefix}times`;
  }
  return 'fas fa-question'
}

const getIconBackground = (item) => {
  const prefix = 'bg-'
  if (item.eventStatus === REQUESTED || item.eventStatus === AWAITING_GRADING) {
    return `${prefix}yellow-500`;
  } else if (item.eventStatus === APPROVED || item.eventStatus === PASSED || item.eventStatus === COMPLETED) {
    return `${prefix}green-500`;
  } else if (item.eventStatus === REJECTED || item.eventStatus === FAILED) {
    return `${prefix}red-500`;
  }
  return 'bg-gray-500'
}
</script>

<template>
  <div class="pt-2">
    <Timeline :pt="{ opposite: { class: 'p-0 flex-none' } }" :value="props.events" align="left" data-cy="approvalHistoryTimeline">
      <template #marker="slotProps">
        <span class="flex w-2rem h-2rem align-items-center justify-content-center text-white border-circle z-1 shadow-1" :class="getIconBackground(slotProps.item)">
            <i :class="getIconClass(slotProps.item)"></i>
        </span>
      </template>
      <template #content="slotProps">
        <div class="py-2">
          <span class="font-bold">{{ slotProps.item.eventStatus }}</span>
          <i class="fas fa-circle px-2 text-400" style="font-size: .5rem;"></i>
          <small class="text-muted py-2" :title="`${timeUtils.formatDate(slotProps.item.eventTime)}`">{{timeUtils.relativeTime(slotProps.item.eventTime)}}</small>
        </div>
        <div>
          <span class="text-muted text-sm">{{ timeUtils.formatDate(slotProps.item.eventTime) }}</span>
        </div>
        <ApprovalEventMessage class="py-2" v-if="slotProps.item.description" :message="slotProps.item.description" :messageId="slotProps.item.id" />
      </template>
    </Timeline>
  </div>
</template>

<style>

</style>