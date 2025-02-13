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
import { ref } from 'vue';

const props = defineProps({
  errorsToShow: Array,
})
const additionalErrorsShown = ref(false)
</script>

<template>
  <Message v-if="errorsToShow" severity="error" :closable="false" data-cy="questionErrors">
    <template #messageicon>
      <i class="fas fa-exclamation-triangle text-2xl" aria-hidden="true"></i>
    </template>
    <span class="mx-2">Please fix the following:</span>
    <div v-for="e in (errorsToShow.length > 5 ? errorsToShow.slice(0, 3) : errorsToShow)" :key="e" class="ml-6">
      - {{ e }}
    </div>
    <div v-if="errorsToShow.length > 5">
      <div v-show="additionalErrorsShown" :class="additionalErrorsShown ? 'fadeindown' : 'fadeoutup'" class="animate-duration-500 ml-6" v-for="e in errorsToShow.slice(3, errorsToShow.length)" :key="e">
        - {{ e }}
      </div>
      <SkillsButton link
              class="mt-6"
              @click="additionalErrorsShown = !additionalErrorsShown"
              :label="!additionalErrorsShown ? `Expand ${errorsToShow.length - 3} more...` : 'Collapse'"
              :icon="!additionalErrorsShown ? 'fas fa-arrow-alt-circle-down' : 'fas fa-arrow-alt-circle-up'"
      />
    </div>
  </Message>
</template>

<style scoped>

</style>