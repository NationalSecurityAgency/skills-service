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
defineProps({
  id: String,
})
</script>

<template>
  <div class="p-2 bg-green-100 rounded-2xl relative pr-8 pl-4" :data-cy="`userMsg-${id}`">
    <slot />
    <i
        shape="circle"
        size="small"
        class="fa-solid fa-user absolute -right-2 -top-2 border-2 border-green-700 bg-green-100 text-green-900 p-2 rounded-full text-lg"
    />
  </div>
</template>

<style scoped>

</style>