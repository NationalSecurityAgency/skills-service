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
import { defineProps } from 'vue'

<script setup>
const props = defineProps({
  noResMsg: {
    type: String,
    default: 'There are no records to show'
  },
  showResetFilter: {
    type: Boolean,
    default: false
  },
  isLoading: {
    type: Boolean,
    default: false
  }
})
const emit = defineEmits(['resetFilter'])
const resetFilter = () => {
  emit('resetFilter')
}
</script>

<template>
  <div class="flex flex-col gap-2 items-center my-6">
    <i v-if="!isLoading" class="fa-solid fa-exclamation-circle fa-3x text-orange-400" aria-hidden="true"></i>
    <div class="text-center" v-if="!isLoading">
      <span class="text-lg">{{ noResMsg }}</span>
      <span v-if="showResetFilter" class="flex gap-2 items-center mt-1">Click
          <SkillsButton
              label="Reset"
              icon="fa-solid fa-eraser"
              size="small"
              @click="resetFilter"
              aria-label="Reset table filters"
              data-cy="tblFilterResetBtn"/> to clear the existing filter.
      </span>
    </div>
    <div v-if="isLoading" class="text-center">Loading...</div>
  </div>
</template>

<style scoped>

</style>