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
<script setup lang="ts">
import {computed} from "vue";

const props = defineProps({
  currentPage: Number,
  totalPages: Number,
})
const emit = defineEmits(['prevPage', 'nextPage'])

const prevPage = () => {
  emit('prevPage')
}
const nextPage = () => {
  emit('nextPage')
}

const progressPercent = computed(() => (props.currentPage / props.totalPages) * 100)
</script>

<template>
  <div class="flex gap-2 items-center">
    <SkillsButton
        aria-label="Previous Slide"
        icon="fa-solid fa-circle-chevron-left"
        class="shadow-md"
        data-cy="prevSlideBtn"
        @click="prevPage"
        :disabled="currentPage <= 1"/>
    <div>
      <div data-cy="currentSlideMsg">Slide {{ currentPage }} of {{ totalPages }}</div>
      <ProgressBar :value="progressPercent" :show-value="false" style="height: 5px"></ProgressBar>
    </div>
    <SkillsButton
        aria-label="Next Slide"
        icon="fa-solid fa-circle-chevron-right"
        class="shadow-md"
        data-cy="nextSlideBtn"
        @click="nextPage"
        :disabled="currentPage >= totalPages"/>
  </div>

</template>

<style scoped>

</style>