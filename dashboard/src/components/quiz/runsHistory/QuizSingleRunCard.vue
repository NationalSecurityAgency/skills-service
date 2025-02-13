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
  <Card class="w-full h-full">
    <template #content>
      <div class="uppercase text-muted-color">{{ title }}</div>
      <div class="mt-1">
        <slot></slot>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>