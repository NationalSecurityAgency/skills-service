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
  badge: Object
})

</script>

<template>
  <div aria-hidden="true">
    <i v-if="badge.gem" class="fas fa-gem text-purple-600 text-2xl"></i>
    <i v-if="badge.global" class="fas fa-globe text-cyan-600 text-2xl"></i>
  </div>
</template>

<style scoped>

</style>