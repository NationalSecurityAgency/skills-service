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

const props = defineProps({
  label: String,
  inputId: String,
  labelWidthInRem: {
    type: Number,
    default: 16
  }
})
</script>

<template>
  <div class="field flex flex-col lg:flex-row lg:gap-4 mb-6" :data-cy="inputId">
    <label class="text-secondary" :class="`w-min-${labelWidthInRem}rem lg:max-w-${labelWidthInRem}rem`" :id="`labelFor${inputId}`" :for="inputId">
      <span >{{ label }}:</span>
    </label>
    <div class="flex items-center flex-1">
      <slot />
    </div>
  </div>
</template>

<style scoped>

</style>