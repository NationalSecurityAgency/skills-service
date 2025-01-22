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
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'

const props = defineProps({
  loading: Boolean,
});
</script>

<template>
  <div v-if="loading" class="card h-100 w-100">
    <div class="card-body h-100 text-center">
      <SkillsSpinner variant="info" label="Spinning" style="width: 3rem; height: 3rem;"  class="mt-8"></SkillsSpinner>
    </div>
  </div>
</template>

<style scoped>

</style>
