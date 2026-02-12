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
import {computed} from "vue";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const props = defineProps({
  question: Object,
})

const appConfig = useAppConfig()

const aiGradingInfo = computed(() => {
  if (!appConfig.enableOpenAIIntegration || !props.question || props.question.length === 0) {
    return null
  }
  return props.question.answers[0].aiGradingStatus
})
const gradingResult = computed(() => {
  if (!appConfig.enableOpenAIIntegration || !props.question || props.question.length === 0) {
    return null
  }
  return props.question.answers[0].gradingResult
})
const pendingGradingMsg = computed(() => {
   return !aiGradingInfo?.value?.failed && props.question?.aiGradingConfigured && !gradingResult.value?.gradedOn
})
const hasFailedAttempts = computed(() => {
  return aiGradingInfo.value?.hasFailedAttempts && !aiGradingInfo.value?.failed
})
</script>

<template>
<div v-if="appConfig.enableOpenAIIntegration">
  <Message
      v-if="hasFailedAttempts"
      severity="error"
      data-cy="aiGradingFailedButHasRetriesMsg"
      :closable="false">
    AI Assistant failed to grade this question. It will retry automatically. <Tag>{{ aiGradingInfo.attemptsLeft }}</Tag> attempts remaining out of <Tag severity="secondary">{{
      aiGradingInfo.attemptCount + aiGradingInfo.attemptsLeft
    }}</Tag>. The
    system will continue trying. You can manually grade this question if needed.
  </Message>
  <Message
      v-if="aiGradingInfo?.failed"
      severity="error"
      data-cy="aiGradingFailedMsg"
      :closable="false">
    AI Assistant failed to grade this question after <Tag severity="danger">{{ aiGradingInfo.attemptCount }}</Tag> attempts. Manual grading is now required. The system will
    not attempt further automatic grading.
  </Message>
  <InlineMessage
      v-if="pendingGradingMsg && !hasFailedAttempts && !aiGradingInfo?.failed"
      class="mt-2"
      data-cy="aiManualGradingLongMsg"
      severity="warn">AI grading is enabled for this question. Manual grading is available but not recommended.</InlineMessage>
</div>

</template>

<style scoped>

</style>