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
import { useSelfReportHelper } from '@/skills-display/UseSelfReportHelper.js';

const props = defineProps({
  events: {
    type: Array,
    required: true,
  },
})

const timeUtils = useTimeUtils()
const selfReportHelper = useSelfReportHelper()

const getIconClass = (item) => {
  const prefix = 'fas fa-'
  if (selfReportHelper.isApprovalRequest(item.eventStatus) || selfReportHelper.isAwaitingGrading(item.eventStatus)) {
    return `${prefix}clock`;
  } else if (selfReportHelper.isApproved(item.eventStatus) || selfReportHelper.isPassed(item.eventStatus) || selfReportHelper.isCompleted(item.eventStatus)) {
    return `${prefix}check`;
  } else if (selfReportHelper.isRejected(item.eventStatus) || selfReportHelper.isFailed(item.eventStatus)) {
    return `${prefix}times`;
  }
  return 'fas fa-question'
}

const getIconBackground = (item) => {
  const prefix = 'bg-'
  if (selfReportHelper.isApprovalRequest(item.eventStatus) || selfReportHelper.isAwaitingGrading(item.eventStatus)) {
    return `${prefix}yellow-500`;
  } else if (selfReportHelper.isApproved(item.eventStatus) || selfReportHelper.isPassed(item.eventStatus) || selfReportHelper.isCompleted(item.eventStatus)) {
    return `${prefix}green-500`;
  } else if (selfReportHelper.isRejected(item.eventStatus) || selfReportHelper.isFailed(item.eventStatus)) {
    return `${prefix}red-500`;
  }
  return 'bg-gray-500'
}

const getApprover = (item) => {
  return item.approverUserIdForDisplay ? item.approverUserIdForDisplay : item.approverUserId
}

</script>

<template>
  <div class="pt-2">
    <Timeline :pt="{ opposite: { class: 'p-0 flex-none' } }" :value="props.events" align="left" data-cy="approvalHistoryTimeline">
      <template #marker="slotProps">
        <span class="flex w-8 h-8 items-center justify-center text-white rounded-full z-10 shadow-sm" :class="getIconBackground(slotProps.item)">
            <i :class="getIconClass(slotProps.item)"></i>
        </span>
      </template>
      <template #content="slotProps">
        <div class="py-2">
          <span class="font-bold">{{ slotProps.item.eventStatus }}</span>
          <span v-if="getApprover(slotProps.item)" class="text-muted text-sm pl-0" data-cy="approver"> (<span class="italic">by</span> {{ getApprover(slotProps.item) }})</span>
          <i class="fas fa-circle px-2 text-surface-400 dark:text-surface-400" style="font-size: .5rem;"></i>
          <small class="text-muted py-2" :title="`${timeUtils.formatDate(slotProps.item.eventTime)}`">{{timeUtils.relativeTime(slotProps.item.eventTime)}}</small>
        </div>
        <div v-if="selfReportHelper.isFailed(slotProps.item.eventStatus)" class="pb-2">
          <span class="text-muted text-sm">
            View <router-link data-cy="myQuizAttemptsLink" :to="{ name:'MyQuizAttemptsPage' }">My Quiz Attempts</router-link> History
          </span>
        </div>
        <div>
          <span class="text-muted text-sm">{{ timeUtils.formatDate(slotProps.item.eventTime) }}</span>
        </div>
        <ApprovalEventMessage class="py-2" v-if="slotProps.item.description" :message="slotProps.item.description" :event-status="slotProps.item.eventStatus" :messageId="slotProps.item.id" />
      </template>
    </Timeline>
  </div>
</template>

<style>

</style>