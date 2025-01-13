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
import { computed, ref } from 'vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue';
import { useSelfReportHelper } from '@/skills-display/UseSelfReportHelper.js';

const props = defineProps({
  message: String,
  messageId: String,
  eventStatus: String,
})

const selfReportHelper = useSelfReportHelper()

const isDescriptionShowing = ref(false)
const showMessageBtnLabel = computed(() => {
  return `${isDescriptionShowing.value ? 'Hide' : 'Show'} ${selfReportHelper.isApprovalRequest(props.eventStatus) ? 'Justification' : 'Message'}`
})
const toggleShowMessage = () => {
  isDescriptionShowing.value = !isDescriptionShowing.value
}

</script>

<template>
  <div v-if="props.message">
    <SkillsButton
        :label="showMessageBtnLabel"
        :icon="isDescriptionShowing ? 'far fa-eye-slash' : 'far fa-eye'"
        class="skills-theme-btn"
        severity="info"
        size="small"
        @click="toggleShowMessage"
        data-cy="toggleShowMessageBtn"/>
  <markdown-text v-if="isDescriptionShowing" :text="props.message" :instance-id="props.messageId" data-cy="approvalEventMessage" />
  </div>
</template>

<style scoped>

</style>