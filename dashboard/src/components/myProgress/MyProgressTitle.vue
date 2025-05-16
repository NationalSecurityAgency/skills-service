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
})
</script>

<template>
  <Card :pt="{ content: { class: 'p-0' }, body: { class: 'p-3' } }">
    <template #content>
      <div class="flex flex-col sm:flex-row gap-2 items-center">
        <div class="flex-1">
          <h1 class="text-2xl font-normal m-0 uppercase" role="heading" aria-level="1" data-cy="myProgressTitle">{{ title }}</h1>
        </div>
        <div>
          <slot name="rightContent"></slot>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>