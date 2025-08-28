/*
Copyright 2025 SkillTree

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
</script>

<template>
  <div class="flex justify-start relative">
    <div class="p-2 bg-blue-100 rounded-2xl pr-4 pl-8">
      <slot />
      <i
          shape="circle"
          size="small"
          class="fa-solid fa-hat-wizard absolute -left-2 -top-2 border-2 border-blue-500 bg-blue-200 text-gray-900 p-2 rounded-full text-lg"
      />
    </div>
  </div>
</template>

<style scoped>

</style>