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
defineProps({
  title: String,
  titleTag: {
    type: String,
    default: 'h3'
  }
})
</script>

<template>
  <div class="border-b p-4 border-surface bg-surface-100 dark:bg-surface-700 flex" data-cy="card-header">
    <div class="font-bold flex-1 inline"><slot name="headerIcon" /><component :is="titleTag" class="inline">{{ title }}</component></div>
    <slot name="headerContent" />
  </div>
</template>

<style scoped>

</style>