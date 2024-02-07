<script setup>
import { computed } from 'vue'
import { useFieldValue } from 'vee-validate'

const numPerformToCompletion = useFieldValue('numPerformToCompletion')
const pointIncrement = useFieldValue('pointIncrement')

const totalPoints = computed(() => {
  return numPerformToCompletion.value * pointIncrement.value;
})
</script>

<template>
  <div class="field ml-2">
    <label for="totalPoints">Total Points</label>
    <InputGroup>
      <InputGroupAddon><i class="fas fa-equals"></i></InputGroupAddon>
      <InputNumber id="totalPoints" :model-value="totalPoints" :disabled="true" />
    </InputGroup>
  </div>
</template>

<style scoped>

</style>