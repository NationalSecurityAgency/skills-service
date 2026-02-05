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
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";

const timeUtils = useTimeUtils()

const props = defineProps({
  attemptTimestamp: Number,
})
</script>

<template>
  <Message :closable="false" severity="warn" icon="fas fa-user-clock" data-cy="quizRequiresGradingMsg">
    <div class="pl-3">
      <div class="text-xl font-semibold">Quiz Under Review</div>
      <div class="mt-1">
        <div v-if="attemptTimestamp">
          Quiz completed on <Tag>{{ timeUtils.formatDate(attemptTimestamp) }}</Tag>! Some questions require review.
        </div>
        <div v-else>
          Quiz submitted successfully! Some questions require review.
        </div>
        <div class="mt-1">We'll notify you as soon as grading is complete.</div>
      </div>
    </div>

  </Message>
</template>

<style scoped>

</style>