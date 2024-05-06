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
    <div v-for="e in (errorsToShow.length > 5 ? errorsToShow.slice(0, 3) : errorsToShow)" :key="e" class="ml-4">
      - {{ e }}
    </div>
    <div v-if="errorsToShow.length > 5">
      <div v-show="additionalErrorsShown" :class="additionalErrorsShown ? 'fadeindown' : 'fadeoutup'" class="animation-duration-500 ml-4" v-for="e in errorsToShow.slice(3, errorsToShow.length)" :key="e">
        - {{ e }}
      </div>
      <SkillsButton link
              class="mt-4"
              @click="additionalErrorsShown = !additionalErrorsShown"
              :label="!additionalErrorsShown ? `Expand ${errorsToShow.length - 3} more...` : 'Collapse'"
              :icon="!additionalErrorsShown ? 'fas fa-arrow-alt-circle-down' : 'fas fa-arrow-alt-circle-up'"
      />
    </div>
  </Message>
</template>

<style scoped>

</style>