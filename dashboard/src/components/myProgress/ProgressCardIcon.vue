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
  icon: String,
})
</script>

<template>
  <div class="border-circle w-7rem h-7rem m-2 bg-blue-100 font-bold flex align-items-center justify-content-center" aria-hidden="true">
    <i class="text-cyan-800 text-7xl" :class="icon"></i>
  </div>
</template>

<style scoped>

</style>